-- ==============================================================================
-- Quran Academy Connect - id-based class matching
-- ==============================================================================
-- WHY: classes.teacher_name / student_name are free text, matched against a
-- profile's display name (public.current_profile_name()). Any drift between the
-- two - a class created before a name is finalized, retyped instead of picked
-- from the dropdown, a name later edited - makes that class invisible under RLS
-- and rejected by the livekit-token / send-call-push edge functions ("You do not
-- have access to this class" / calls that never connect), because those checks
-- require an exact string match.
--
-- This adds nullable teacher_id/student_id columns (the real auth.uid()-based
-- profile ids) alongside the existing name columns. New classes should be
-- created with both id and name; existing rows keep working via the name-match
-- fallback that's already in place. Run this after 0001.
-- ==============================================================================

ALTER TABLE public.classes ADD COLUMN IF NOT EXISTS teacher_id TEXT;
ALTER TABLE public.classes ADD COLUMN IF NOT EXISTS student_id TEXT;
CREATE INDEX IF NOT EXISTS idx_classes_teacher_id ON public.classes(teacher_id);
CREATE INDEX IF NOT EXISTS idx_classes_student_id ON public.classes(student_id);

DROP POLICY IF EXISTS "Participants read own classes" ON public.classes;
DROP POLICY IF EXISTS "Teachers update own class status" ON public.classes;

CREATE POLICY "Participants read own classes"
ON public.classes FOR SELECT
TO authenticated
USING (
    student_id = auth.uid()::text
    OR teacher_id = auth.uid()::text
    OR student_name = public.current_profile_name()
    OR teacher_name = public.current_profile_name()
    OR public.is_admin_or_director()
);

CREATE POLICY "Teachers update own class status"
ON public.classes FOR UPDATE
TO authenticated
USING (
    teacher_id = auth.uid()::text
    OR teacher_name = public.current_profile_name()
    OR public.is_admin_or_director()
)
WITH CHECK (
    teacher_id = auth.uid()::text
    OR teacher_name = public.current_profile_name()
    OR public.is_admin_or_director()
);

-- ==============================================================================
-- END OF MIGRATION
-- ==============================================================================
