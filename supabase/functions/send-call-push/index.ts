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

// ─── FCM v1 via Service Account OAuth2 ───────────────────────────────────────
// Legacy FCM (key= server key) was deprecated Jun 2023 and disabled for new
// projects. FCM HTTP v1 uses a short-lived OAuth2 access token minted from the
// Firebase Admin service-account credentials stored as a Supabase secret.

/** Base64url-encode a string without padding */
function b64url(str: string): string {
  return btoa(str).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}

/** Encode an ArrayBuffer to base64url */
function ab2b64url(buf: ArrayBuffer): string {
  const bytes = new Uint8Array(buf);
  let str = "";
  bytes.forEach((b) => (str += String.fromCharCode(b)));
  return b64url(str);
}

/** Mint a short-lived (1-hour) OAuth2 access token for FCM v1 */
async function getFcmAccessToken(serviceAccountJson: string): Promise<string> {
  const sa = JSON.parse(serviceAccountJson);
  const now = Math.floor(Date.now() / 1000);

  const header = b64url(JSON.stringify({ alg: "RS256", typ: "JWT" }));
  const payload = b64url(
    JSON.stringify({
      iss: sa.client_email,
      scope: "https://www.googleapis.com/auth/firebase.messaging",
      aud: sa.token_uri,
      iat: now,
      exp: now + 3600,
    }),
  );

  const sigInput = `${header}.${payload}`;

  // Strip PEM headers and decode to binary
  const pemBody = sa.private_key
    .replace(/-----BEGIN PRIVATE KEY-----/g, "")
    .replace(/-----END PRIVATE KEY-----/g, "")
    .replace(/\s/g, "");
  const binaryKey = Uint8Array.from(atob(pemBody), (c) => c.charCodeAt(0));

  const cryptoKey = await crypto.subtle.importKey(
    "pkcs8",
    binaryKey.buffer,
    { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
    false,
    ["sign"],
  );

  const signature = await crypto.subtle.sign(
    "RSASSA-PKCS1-v1_5",
    cryptoKey,
    new TextEncoder().encode(sigInput),
  );

  const jwt = `${sigInput}.${ab2b64url(signature)}`;

  const tokenResp = await fetch(sa.token_uri, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion: jwt,
    }),
  });

  const tokenData = await tokenResp.json();
  if (!tokenData.access_token) {
    throw new Error(`FCM token exchange failed: ${JSON.stringify(tokenData)}`);
  }
  return tokenData.access_token as string;
}

/** Send an FCM v1 data push to a single device token */
async function sendFcmV1Push(
  projectId: string,
  accessToken: string,
  deviceToken: string,
  data: Record<string, string>,
): Promise<boolean> {
  const url = `https://fcm.googleapis.com/v1/projects/${projectId}/messages:send`;
  const resp = await fetch(url, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${accessToken}`,
    },
    body: JSON.stringify({
      message: {
        token: deviceToken,
        // data-only (no notification block) so Android handles it as a
        // high-priority background data message even when the app is killed.
        data,
        android: { priority: "high" },
      },
    }),
  });
  return resp.ok;
}

// ─── Main handler ─────────────────────────────────────────────────────────────

serve(async (req: Request) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  try {
    const supabaseUrl = Deno.env.get("SUPABASE_URL") || "";
    const supabaseAnonKey = Deno.env.get("SUPABASE_ANON_KEY") || "";
    const supabaseServiceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") || "";
    // Full service-account JSON stored as a single Supabase secret.
    const firebaseServiceAccountJson = Deno.env.get("FIREBASE_SERVICE_ACCOUNT_JSON") || "";

    if (!supabaseUrl || !supabaseAnonKey || !supabaseServiceKey) {
      return json({ error: "Server missing required Supabase configuration" }, 500);
    }

    // Caller must supply their own Supabase Auth JWT so we can verify identity
    // via RLS before writing or pushing anything.
    const authHeader = req.headers.get("Authorization");
    if (!authHeader || !authHeader.toLowerCase().startsWith("bearer ")) {
      return json({ error: "Missing Authorization bearer token" }, 401);
    }
    const callerJwt = authHeader.slice(7);

    const { action = "INCOMING_CALL", class_id: classId = "" } = await req.json();
    if (!classId) {
      return json({ error: "Missing required field: 'class_id'" }, 400);
    }

    // Use caller's JWT for RLS-gated reads (profile + class membership check).
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

    const { data: targetClass } = await callerClient
      .from("classes")
      .select("teacher_name,student_name,livekit_room_name")
      .eq("id", classId)
      .maybeSingle();
    if (!targetClass) {
      return json({ error: "You do not have access to this class" }, 403);
    }

    const isAdmin = myProfile.role === "ADMIN" || myProfile.role === "DIRECTOR";
    const isTeacherForClass =
      isAdmin || (myProfile.role === "TEACHER" && myProfile.name === targetClass.teacher_name);
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

    // Privileged writes use the service-role key (bypasses RLS intentionally
    // because we've already verified the caller's rights above).
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

    // FCM v1 push — only attempted when service-account credentials are present.
    let fcmDispatched = false;
    if (isRinging && firebaseServiceAccountJson) {
      try {
        const { data: studentProfile } = await serviceClient
          .from("profiles")
          .select("fcm_token")
          .eq("name", studentName)
          .maybeSingle();

        const recipientToken = studentProfile?.fcm_token;
        if (recipientToken) {
          const sa = JSON.parse(firebaseServiceAccountJson);
          const accessToken = await getFcmAccessToken(firebaseServiceAccountJson);
          fcmDispatched = await sendFcmV1Push(sa.project_id, accessToken, recipientToken, {
            action,
            class_id: classId,
            teacher_name: teacherName,
            student_name: studentName,
            room_name: roomName,
          });
        }
      } catch (fcmErr) {
        // FCM failure is non-fatal — Realtime WebSocket still signals the student.
        console.warn("FCM push failed (non-fatal):", fcmErr);
      }
    }

    return json({ success: true, action, class_id: classId, fcmDispatched, realtimeCallUpdated: true });
  } catch (err: unknown) {
    const message = err instanceof Error ? err.message : String(err);
    return json({ error: message }, 500);
  }
});
