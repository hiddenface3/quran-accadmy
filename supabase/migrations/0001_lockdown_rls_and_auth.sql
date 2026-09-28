-- ==============================================================================
-- Quran Academy Connect - Lockdown migration
-- ==============================================================================
-- Run this ONCE in the Supabase SQL Editor (or `supabase db push` once the CLI
-- project is linked) against the project at https://amcdmiioovbhzkjqxdrk.supabase.co
--
-- WHY THIS MIGRATION EXISTS:
-- supabase/schema.sql created every table's RLS policy as `USING (true)` -
-- "Allow public read/upsert/delete". supabase_rls_policies.sql later tried to add
-- role-scoped policies, but never dropped those original ones (different policy
-- names), and Postgres OR-combines multiple permissive policies for the same
-- command - so the wide-open ones were still silently in effect the whole time.
-- On top of that, the app never authenticated with Supabase Auth at all (every
-- request just sent the static anon key), so auth.uid()/auth.email() were always
-- NULL and no per-user policy could have worked anyway.
--
-- This migration:
--   1. Drops every wide-open "Allow public ..." / "Allow all ..." policy.
--   2. Adds a trigger that auto-creates a STUDENT profile row when someone signs
--      up via Supabase Auth (profiles.id == auth.users.id from here on).
--   3. Adds a trigger that blocks a user from changing their OWN role via the API
--      (only an admin, or a direct SQL Editor session, can change a role).
--   4. Rebuilds every policy keyed off auth.uid(), scoped to `authenticated` only
--      (no more `anon`).
--
-- AFTER RUNNING THIS: the app requires a real signed-in Supabase Auth session for
-- everything. Pre-existing demo/profile rows that don't correspond to a real
-- auth.users row become orphaned (harmless - they just won't be reachable by
-- anyone anymore). To make your own account an admin, run once, after you've
-- signed up for real through the app:
--   UPDATE public.profiles SET role = 'ADMIN' WHERE email = 'you@example.com';
-- (Running that as yourself in the SQL Editor works because auth.uid() is NULL
-- in that context, which the anti-escalation trigger below intentionally allows -
-- API requests from a signed-in user's own device are never NULL, so this
-- bootstrap path can't be reached from the app itself.)
-- ==============================================================================

-- ------------------------------------------------------------------------------
-- 0. Bring the real chat table's shape in line with what the app now sends, and
--    make sure every table + its RLS switch exists (idempotent).
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.profiles (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    email TEXT,
    role TEXT NOT NULL DEFAULT 'STUDENT',
    avatar_url TEXT DEFAULT '',
    assigned_teacher_name TEXT DEFAULT '',
    tajweed_level TEXT DEFAULT 'Beginner',
    fcm_token TEXT DEFAULT '',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.classes (
    id TEXT PRIMARY KEY,
    title TEXT NOT NULL,
    teacher_name TEXT NOT NULL,
    teacher_title TEXT DEFAULT 'Certified Qari & Hifz Instructor',
    student_name TEXT NOT NULL,
    date TEXT NOT NULL DEFAULT 'Today',
    start_time TEXT NOT NULL DEFAULT '04:00 PM',
    duration_minutes INTEGER DEFAULT 45,
    status TEXT NOT NULL DEFAULT 'SCHEDULED',
    description TEXT,
    livekit_room_name TEXT,
    surah_topic TEXT DEFAULT 'Surah Al-Mulk (Ayah 1-15)',
    syllabus_notes TEXT DEFAULT '',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.active_calls (
    id TEXT PRIMARY KEY,
    class_id TEXT NOT NULL,
    teacher_name TEXT NOT NULL,
    student_name TEXT NOT NULL,
    room_name TEXT NOT NULL,
    is_ringing BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.messages (
    id TEXT PRIMARY KEY,
    sender_id TEXT NOT NULL,
    sender_name TEXT NOT NULL,
    sender_role TEXT NOT NULL DEFAULT 'STUDENT',
    receiver_id TEXT,
    receiver_name TEXT,
    text TEXT NOT NULL,
    timestamp TEXT NOT NULL,
    is_read BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.classes ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.active_calls ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.messages ENABLE ROW LEVEL SECURITY;

ALTER PUBLICATION supabase_realtime ADD TABLE public.active_calls;
ALTER PUBLICATION supabase_realtime ADD TABLE public.messages;

-- ------------------------------------------------------------------------------
-- 1. Drop every previously-created policy, wide-open or not, so we start clean.
--    (DROP POLICY IF EXISTS is safe to run whether or not each one is present.)
-- ------------------------------------------------------------------------------
DROP POLICY IF EXISTS "Allow public read profiles" ON public.profiles;
DROP POLICY IF EXISTS "Allow public upsert profiles" ON public.profiles;
DROP POLICY IF EXISTS "Allow all profiles" ON public.profiles;
DROP POLICY IF EXISTS "Allow users to read profiles" ON public.profiles;
DROP POLICY IF EXISTS "Allow user to update own profile" ON public.profiles;
DROP POLICY IF EXISTS "Admin full control on profiles" ON public.profiles;

DROP POLICY IF EXISTS "Allow public read classes" ON public.classes;
DROP POLICY IF EXISTS "Allow public upsert classes" ON public.classes;
DROP POLICY IF EXISTS "Allow all classes" ON public.classes;
DROP POLICY IF EXISTS "Students read own classes" ON public.classes;
DROP POLICY IF EXISTS "Teachers read assigned classes" ON public.classes;
DROP POLICY IF EXISTS "Teachers update own class status" ON public.classes;
DROP POLICY IF EXISTS "Admin full control on classes" ON public.classes;

DROP POLICY IF EXISTS "Allow public read active_calls" ON public.active_calls;
DROP POLICY IF EXISTS "Allow public upsert active_calls" ON public.active_calls;
DROP POLICY IF EXISTS "Allow public delete active_calls" ON public.active_calls;
DROP POLICY IF EXISTS "Allow all calls" ON public.active_calls;
DROP POLICY IF EXISTS "Participants read active call signals" ON public.active_calls;
DROP POLICY IF EXISTS "Teachers and Admin manage call signals" ON public.active_calls;
DROP POLICY IF EXISTS "Participants manage own call signals" ON public.active_calls;

DROP POLICY IF EXISTS "Allow public read messages" ON public.messages;
DROP POLICY IF EXISTS "Allow public insert messages" ON public.messages;
DROP POLICY IF EXISTS "Participants read own messages" ON public.messages;
DROP POLICY IF EXISTS "Participants send own messages" ON public.messages;

-- ------------------------------------------------------------------------------
-- 2. Helper functions - now keyed off auth.uid(), which is only populated for a
--    real Supabase Auth session (never for the plain anon key).
-- ------------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.current_profile_name()
RETURNS TEXT AS $$
    SELECT name FROM public.profiles WHERE id = auth.uid()::text LIMIT 1;
$$ LANGUAGE sql STABLE SECURITY DEFINER;

CREATE OR REPLACE FUNCTION public.is_admin_or_director()
RETURNS BOOLEAN AS $$
    SELECT EXISTS (
        SELECT 1 FROM public.profiles
        WHERE id = auth.uid()::text AND role IN ('ADMIN', 'DIRECTOR')
    );
$$ LANGUAGE sql STABLE SECURITY DEFINER;

-- ------------------------------------------------------------------------------
-- 3. Auto-provision a STUDENT profile on sign-up. This is the only way a row is
--    ever inserted into public.profiles - there is deliberately no INSERT policy
--    granting authenticated/anon users direct insert rights.
-- ------------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO public.profiles (id, name, email, role)
    VALUES (
        NEW.id::text,
        COALESCE(NEW.raw_user_meta_data->>'full_name', split_part(NEW.email, '@', 1)),
        NEW.email,
        'STUDENT'
    )
    ON CONFLICT (id) DO NOTHING;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public;

DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();

-- ------------------------------------------------------------------------------
-- 4. Block self role-escalation. auth.uid() IS NULL only for a direct DB session
--    (SQL Editor / migrations) - never for an API request from a signed-in
--    device - so this only ever blocks the app, not the project owner.
-- ------------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.prevent_role_self_escalation()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.role IS DISTINCT FROM OLD.role THEN
        IF auth.uid() IS NOT NULL AND NOT public.is_admin_or_director() THEN
            RAISE EXCEPTION 'Only an administrator can change a user role.';
        END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public;

DROP TRIGGER IF EXISTS trg_prevent_role_self_escalation ON public.profiles;
CREATE TRIGGER trg_prevent_role_self_escalation
    BEFORE UPDATE ON public.profiles
    FOR EACH ROW EXECUTE FUNCTION public.prevent_role_self_escalation();

-- ------------------------------------------------------------------------------
-- 5. PROFILES policies (authenticated only - no more `anon`).
-- ------------------------------------------------------------------------------
CREATE POLICY "Users read own profile or admin reads all"
ON public.profiles FOR SELECT
TO authenticated
USING (
    id = auth.uid()::text
    OR role = 'TEACHER'          -- directory: any signed-in user can see teachers
    OR public.is_admin_or_director()
);

CREATE POLICY "Users update own profile"
ON public.profiles FOR UPDATE
TO authenticated
USING (id = auth.uid()::text OR public.is_admin_or_director())
WITH CHECK (id = auth.uid()::text OR public.is_admin_or_director());
-- Role changes within this UPDATE are further gated by trg_prevent_role_self_escalation.

CREATE POLICY "Admins delete profiles"
ON public.profiles FOR DELETE
TO authenticated
USING (public.is_admin_or_director());

-- ------------------------------------------------------------------------------
-- 6. CLASSES policies.
-- ------------------------------------------------------------------------------
CREATE POLICY "Participants read own classes"
ON public.classes FOR SELECT
TO authenticated
USING (
    student_name = public.current_profile_name()
    OR teacher_name = public.current_profile_name()
    OR public.is_admin_or_director()
);

CREATE POLICY "Teachers update own class status"
ON public.classes FOR UPDATE
TO authenticated
USING (teacher_name = public.current_profile_name() OR public.is_admin_or_director())
WITH CHECK (teacher_name = public.current_profile_name() OR public.is_admin_or_director());

CREATE POLICY "Admins manage classes"
ON public.classes FOR ALL
TO authenticated
USING (public.is_admin_or_director())
WITH CHECK (public.is_admin_or_director());

-- ------------------------------------------------------------------------------
-- 7. ACTIVE_CALLS (VoIP ringing signal) policies.
-- ------------------------------------------------------------------------------
CREATE POLICY "Participants read own call signals"
ON public.active_calls FOR SELECT
TO authenticated
USING (
    student_name = public.current_profile_name()
    OR teacher_name = public.current_profile_name()
    OR public.is_admin_or_director()
);

CREATE POLICY "Participants manage own call signals"
ON public.active_calls FOR ALL
TO authenticated
USING (
    student_name = public.current_profile_name()
    OR teacher_name = public.current_profile_name()
    OR public.is_admin_or_director()
)
WITH CHECK (
    student_name = public.current_profile_name()
    OR teacher_name = public.current_profile_name()
    OR public.is_admin_or_director()
);

-- ------------------------------------------------------------------------------
-- 8. MESSAGES (direct chat) policies - keyed by the caller's own auth.uid(),
--    which is what UserProfile.id now actually is.
-- ------------------------------------------------------------------------------
CREATE POLICY "Participants read own messages"
ON public.messages FOR SELECT
TO authenticated
USING (
    sender_id = auth.uid()::text
    OR receiver_id = auth.uid()::text
    OR public.is_admin_or_director()
);

CREATE POLICY "Participants send own messages"
ON public.messages FOR INSERT
TO authenticated
WITH CHECK (sender_id = auth.uid()::text);

-- ==============================================================================
-- END OF MIGRATION
-- ==============================================================================
