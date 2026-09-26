-- ==============================================================================
-- 🛡️ Supabase Row Level Security (RLS) Policies for Quran Academy Connect
-- ==============================================================================
-- Purpose:
-- 1. Enable Row Level Security (RLS) on all core academy tables.
-- 2. Restrict Students: Only read their own classes and assigned teachers.
-- 3. Restrict Teachers: Only update status and room signals for their own sessions.
-- 4. Restrict Admin / Director: Full management (create, update, delete classes/profiles).
-- 5. Secure active_calls: Allow participants to see ringing calls intended for them.
-- ==============================================================================

-- ------------------------------------------------------------------------------
-- 1. ENABLE ROW LEVEL SECURITY
-- ------------------------------------------------------------------------------
ALTER TABLE IF EXISTS public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.classes ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.active_calls ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.chat_messages ENABLE ROW LEVEL SECURITY;

-- ------------------------------------------------------------------------------
-- 2. HELPER FUNCTIONS FOR ROLE AND USER IDENTIFICATION
-- ------------------------------------------------------------------------------
-- Extract authenticated or token user email/role
CREATE OR REPLACE FUNCTION public.current_user_role()
RETURNS TEXT AS $$
BEGIN
    RETURN COALESCE(
        current_setting('request.jwt.claim.role', true),
        (SELECT role FROM public.profiles WHERE email = auth.email() LIMIT 1),
        'anon'
    );
END;
$$ LANGUAGE plpgsql STABLE SECURITY DEFINER;

CREATE OR REPLACE FUNCTION public.is_admin_or_director()
RETURNS BOOLEAN AS $$
BEGIN
    RETURN (
        SELECT EXISTS (
            SELECT 1 FROM public.profiles
            WHERE (email = auth.email() OR id = auth.uid()::text)
              AND role IN ('ADMIN', 'DIRECTOR')
        )
    );
END;
$$ LANGUAGE plpgsql STABLE SECURITY DEFINER;

-- ------------------------------------------------------------------------------
-- 3. PROFILES TABLE POLICIES
-- ------------------------------------------------------------------------------
DROP POLICY IF EXISTS "Allow users to read profiles" ON public.profiles;
DROP POLICY IF EXISTS "Allow user to update own profile" ON public.profiles;
DROP POLICY IF EXISTS "Admin full control on profiles" ON public.profiles;

-- Anyone authenticated can view teachers and fellow classroom members
CREATE POLICY "Allow users to read profiles"
ON public.profiles
FOR SELECT
TO authenticated, anon
USING (
    -- Admins/Directors see everything
    public.is_admin_or_director()
    -- Teachers see assigned students & other teachers
    OR role = 'TEACHER'
    -- Students see their own profile
    OR email = auth.email()
    OR id = auth.uid()::text
);

-- Users can only modify their own profile data (e.g. avatar, name)
CREATE POLICY "Allow user to update own profile"
ON public.profiles
FOR UPDATE
TO authenticated, anon
USING (
    email = auth.email() OR id = auth.uid()::text OR public.is_admin_or_director()
)
WITH CHECK (
    email = auth.email() OR id = auth.uid()::text OR public.is_admin_or_director()
);

-- Only Admin or Director can create or delete student/teacher profiles
CREATE POLICY "Admin full control on profiles"
ON public.profiles
FOR ALL
TO authenticated, anon
USING (
    public.is_admin_or_director()
)
WITH CHECK (
    public.is_admin_or_director()
);

-- ------------------------------------------------------------------------------
-- 4. CLASSES TABLE POLICIES
-- ------------------------------------------------------------------------------
DROP POLICY IF EXISTS "Students read own classes" ON public.classes;
DROP POLICY IF EXISTS "Teachers read assigned classes" ON public.classes;
DROP POLICY IF EXISTS "Teachers update own class status" ON public.classes;
DROP POLICY IF EXISTS "Admin full control on classes" ON public.classes;

-- Students can only view classes scheduled for them
CREATE POLICY "Students read own classes"
ON public.classes
FOR SELECT
TO authenticated, anon
USING (
    student_name = (SELECT name FROM public.profiles WHERE email = auth.email() LIMIT 1)
    OR public.is_admin_or_director()
);

-- Teachers can view classes they are instructing
CREATE POLICY "Teachers read assigned classes"
ON public.classes
FOR SELECT
TO authenticated, anon
USING (
    teacher_name = (SELECT name FROM public.profiles WHERE email = auth.email() LIMIT 1)
    OR public.is_admin_or_director()
);

-- Teachers can update status of their own sessions (e.g. LIVE_NOW, COMPLETED)
CREATE POLICY "Teachers update own class status"
ON public.classes
FOR UPDATE
TO authenticated, anon
USING (
    teacher_name = (SELECT name FROM public.profiles WHERE email = auth.email() LIMIT 1)
    OR public.is_admin_or_director()
)
WITH CHECK (
    teacher_name = (SELECT name FROM public.profiles WHERE email = auth.email() LIMIT 1)
    OR public.is_admin_or_director()
);

-- Only Admin or Director can insert or delete classes
CREATE POLICY "Admin full control on classes"
ON public.classes
FOR ALL
TO authenticated, anon
USING (
    public.is_admin_or_director()
)
WITH CHECK (
    public.is_admin_or_director()
);

-- ------------------------------------------------------------------------------
-- 5. ACTIVE CALLS (VOIP SIGNALING) POLICIES
-- ------------------------------------------------------------------------------
DROP POLICY IF EXISTS "Participants read active call signals" ON public.active_calls;
DROP POLICY IF EXISTS "Teachers and Admin manage call signals" ON public.active_calls;

-- Students and Teachers can read ringing signals directed to them
CREATE POLICY "Participants read active call signals"
ON public.active_calls
FOR SELECT
TO authenticated, anon
USING (
    student_name = (SELECT name FROM public.profiles WHERE email = auth.email() LIMIT 1)
    OR teacher_name = (SELECT name FROM public.profiles WHERE email = auth.email() LIMIT 1)
    OR public.is_admin_or_director()
);

-- Teachers can initiate calls and stop ringing for their sessions
CREATE POLICY "Teachers and Admin manage call signals"
ON public.active_calls
FOR ALL
TO authenticated, anon
USING (
    teacher_name = (SELECT name FROM public.profiles WHERE email = auth.email() LIMIT 1)
    OR public.is_admin_or_director()
)
WITH CHECK (
    teacher_name = (SELECT name FROM public.profiles WHERE email = auth.email() LIMIT 1)
    OR public.is_admin_or_director()
);

-- ==============================================================================
-- END OF RLS POLICIES
-- ==============================================================================
