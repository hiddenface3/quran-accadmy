-- Quran Academy Connect - Supabase Production Schema & Security Policies (RLS)

-- 1. Profiles Table
CREATE TABLE IF NOT EXISTS public.profiles (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    email TEXT,
    role TEXT NOT NULL DEFAULT 'STUDENT',
    avatar_url TEXT,
    fcm_token TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 2. Classes Table
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
    syllabus_notes TEXT DEFAULT 'Makharij Al-Huroof and Ghunnah rules recitation practice.',
    is_next_class BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 3. Messages Table (Chat)
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

-- 4. Active Calls Table (VoIP Signaling & Ringing State)
CREATE TABLE IF NOT EXISTS public.active_calls (
    class_id TEXT PRIMARY KEY,
    teacher_name TEXT NOT NULL,
    student_name TEXT NOT NULL,
    room_name TEXT NOT NULL,
    is_ringing BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Enable Row Level Security (RLS)
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.classes ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.messages ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.active_calls ENABLE ROW LEVEL SECURITY;

-- Permissive public policies for authenticated and anon client apps
CREATE POLICY "Allow public read profiles" ON public.profiles FOR SELECT USING (true);
CREATE POLICY "Allow public upsert profiles" ON public.profiles FOR ALL USING (true) WITH CHECK (true);

CREATE POLICY "Allow public read classes" ON public.classes FOR SELECT USING (true);
CREATE POLICY "Allow public upsert classes" ON public.classes FOR ALL USING (true) WITH CHECK (true);

CREATE POLICY "Allow public read messages" ON public.messages FOR SELECT USING (true);
CREATE POLICY "Allow public insert messages" ON public.messages FOR INSERT WITH CHECK (true);

CREATE POLICY "Allow public read active_calls" ON public.active_calls FOR SELECT USING (true);
CREATE POLICY "Allow public upsert active_calls" ON public.active_calls FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "Allow public delete active_calls" ON public.active_calls FOR DELETE USING (true);

-- Enable Realtime Replication for instant live subscriptions (<50ms latency)
ALTER PUBLICATION supabase_realtime ADD TABLE public.active_calls;
ALTER PUBLICATION supabase_realtime ADD TABLE public.messages;
ALTER PUBLICATION supabase_realtime ADD TABLE public.classes;
