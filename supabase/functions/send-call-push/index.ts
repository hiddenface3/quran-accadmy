import { serve } from "https://deno.land/std@0.168.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.39.0";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

serve(async (req: Request) => {
  // CORS Preflight
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  try {
    const supabaseUrl = Deno.env.get("SUPABASE_URL") || "";
    const supabaseServiceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") || "";
    const fcmServerKey = Deno.env.get("FCM_SERVER_KEY") || "";

    const {
      action = "INCOMING_CALL",
      class_id = "",
      teacher_name = "Sheikh Abdullah Al-Mansoor",
      student_name = "Zaid Ahmed",
      room_name = "",
      target_token = "",
    } = await req.json();

    let recipientToken = target_token;

    // If target token not provided directly, lookup recipient token from database
    if (!recipientToken && supabaseUrl && supabaseServiceKey) {
      const supabase = createClient(supabaseUrl, supabaseServiceKey);
      const { data: profile } = await supabase
        .from("profiles")
        .select("fcm_token")
        .ilike("name", `%${student_name.trim()}%`)
        .maybeSingle();

      if (profile?.fcm_token) {
        recipientToken = profile.fcm_token;
      }
    }

    // Also update active_calls table in Supabase so Realtime WebSockets trigger immediately
    if (supabaseUrl && supabaseServiceKey && class_id) {
      const supabase = createClient(supabaseUrl, supabaseServiceKey);
      const isRinging = action === "INCOMING_CALL";
      await supabase
        .from("active_calls")
        .upsert({
          class_id,
          teacher_name,
          student_name,
          room_name: room_name || `quran-room-${class_id}`,
          is_ringing: isRinging,
          updated_at: new Date().toISOString(),
        }, { onConflict: "class_id" });
    }

    // Send FCM high-priority VoIP call notification if recipientToken and fcmServerKey present
    let fcmDispatched = false;
    if (recipientToken && fcmServerKey) {
      const fcmResponse = await fetch("https://fcm.googleapis.com/fcm/send", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `key=${fcmServerKey}`,
        },
        body: JSON.stringify({
          to: recipientToken,
          priority: "high",
          data: {
            action,
            class_id,
            teacher_name,
            student_name,
            room_name,
          },
        }),
      });

      fcmDispatched = fcmResponse.ok;
    }

    return new Response(
      JSON.stringify({
        success: true,
        action,
        class_id,
        fcmDispatched,
        realtimeCallUpdated: true,
      }),
      {
        status: 200,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      }
    );
  } catch (err: unknown) {
    const message = err instanceof Error ? err.message : String(err);
    return new Response(
      JSON.stringify({ error: message }),
      { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  }
});
