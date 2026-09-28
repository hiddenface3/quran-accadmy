import { serve } from "https://deno.land/std@0.168.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.39.0";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

function json(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { ...corsHeaders, "Content-Type": "application/json" },
  });
}

serve(async (req: Request) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  try {
    const supabaseUrl = Deno.env.get("SUPABASE_URL") || "";
    const supabaseAnonKey = Deno.env.get("SUPABASE_ANON_KEY") || "";
    const supabaseServiceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") || "";
    const fcmServerKey = Deno.env.get("FCM_SERVER_KEY") || "";

    if (!supabaseUrl || !supabaseAnonKey || !supabaseServiceKey) {
      return json({ error: "Server missing required configuration" }, 500);
    }

    // Must be the CALLER's own Supabase Auth JWT. We use it (via the anon-key client below)
    // purely to ask "what is this person allowed to see", which is how we verify they're
    // actually the teacher (or admin) on the class before we ring anyone or write anything.
    const authHeader = req.headers.get("Authorization");
    if (!authHeader || !authHeader.toLowerCase().startsWith("bearer ")) {
      return json({ error: "Missing Authorization bearer token" }, 401);
    }
    const callerJwt = authHeader.slice(7);

    const { action = "INCOMING_CALL", class_id: classId = "" } = await req.json();
    if (!classId) {
      return json({ error: "Missing required field: 'class_id'" }, 400);
    }

    const callerClient = createClient(supabaseUrl, supabaseAnonKey, {
      global: { headers: { Authorization: `Bearer ${callerJwt}` } },
    });

    const { data: myProfile } = await callerClient
      .from("profiles")
      .select("role,name")
      .single();
    if (!myProfile) {
      return json({ error: "No profile found for the authenticated user" }, 403);
    }

    // RLS on `classes` (via the caller's own JWT) already restricts this to a row the caller
    // is actually allowed to see - i.e. they're the teacher, the student, or an admin.
    const { data: targetClass } = await callerClient
      .from("classes")
      .select("teacher_name,student_name,livekit_room_name")
      .eq("id", classId)
      .maybeSingle();
    if (!targetClass) {
      return json({ error: "You do not have access to this class" }, 403);
    }

    const isAdmin = myProfile.role === "ADMIN" || myProfile.role === "DIRECTOR";
    const isTeacherForClass = isAdmin || (myProfile.role === "TEACHER" && myProfile.name === targetClass.teacher_name);
    const isStudentForClass = myProfile.name === targetClass.student_name;

    if (action === "INCOMING_CALL" && !isTeacherForClass) {
      return json({ error: "Only the assigned teacher (or an admin) can start this call" }, 403);
    }
    if (action !== "INCOMING_CALL" && !isTeacherForClass && !isStudentForClass) {
      return json({ error: "You are not a participant in this class" }, 403);
    }

    const teacherName = targetClass.teacher_name;
    const studentName = targetClass.student_name;
    const roomName = targetClass.livekit_room_name || `quran-room-${classId}`;
    const isRinging = action === "INCOMING_CALL";

    // Privileged writes/lookups only happen server-side, from here on, using values we just
    // verified ourselves - never values the client handed us directly.
    const serviceClient = createClient(supabaseUrl, supabaseServiceKey);

    await serviceClient
      .from("active_calls")
      .upsert(
        {
          id: `call_${classId}`,
          class_id: classId,
          teacher_name: teacherName,
          student_name: studentName,
          room_name: roomName,
          is_ringing: isRinging,
          updated_at: new Date().toISOString(),
        },
        { onConflict: "class_id" },
      );

    let fcmDispatched = false;
    if (isRinging && fcmServerKey) {
      const { data: studentProfile } = await serviceClient
        .from("profiles")
        .select("fcm_token")
        .eq("name", studentName)
        .maybeSingle();
      const recipientToken = studentProfile?.fcm_token;

      if (recipientToken) {
        const fcmResponse = await fetch("https://fcm.googleapis.com/fcm/send", {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            Authorization: `key=${fcmServerKey}`,
          },
          body: JSON.stringify({
            to: recipientToken,
            priority: "high",
            data: { action, class_id: classId, teacher_name: teacherName, student_name: studentName, room_name: roomName },
          }),
        });
        fcmDispatched = fcmResponse.ok;
      }
    }

    return json({ success: true, action, class_id: classId, fcmDispatched, realtimeCallUpdated: true });
  } catch (err: unknown) {
    const message = err instanceof Error ? err.message : String(err);
    return json({ error: message }, 500);
  }
});
