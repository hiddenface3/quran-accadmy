import { serve } from "https://deno.land/std@0.168.0/http/server.ts";
import { AccessToken } from "npm:livekit-server-sdk@2.6.0";

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
    const apiKey = Deno.env.get("LIVEKIT_API_KEY") || "APIHUMTB5KGJXq4";
    const apiSecret = Deno.env.get("LIVEKIT_API_SECRET");
    const supabaseUrl = Deno.env.get("SUPABASE_URL");
    const supabaseAnonKey = Deno.env.get("SUPABASE_ANON_KEY");

    if (!apiSecret || !supabaseUrl || !supabaseAnonKey) {
      return json({ error: "Server missing required configuration" }, 500);
    }

    // This must be the CALLER's own Supabase Auth JWT, forwarded by the client - never the
    // anon key. Everything below is verified server-side against Postgres RLS using this
    // token, so a client cannot simply claim "I'm the teacher" and get admin grants.
    const authHeader = req.headers.get("Authorization");
    if (!authHeader || !authHeader.toLowerCase().startsWith("bearer ")) {
      return json({ error: "Missing Authorization bearer token" }, 401);
    }
    const callerJwt = authHeader.slice(7);

    const { room, class_id: classId, identity, name } = await req.json();
    if (!room || !identity || !classId) {
      return json({ error: "Missing required fields: 'room', 'class_id' and 'identity'" }, 400);
    }

    const restHeaders = {
      apikey: supabaseAnonKey,
      Authorization: `Bearer ${callerJwt}`,
    };

    // RLS scopes this to exactly the caller's own row (see "Users read own profile" policy).
    // If it comes back empty, the JWT didn't resolve to a real, provisioned profile.
    const profileResp = await fetch(`${supabaseUrl}/rest/v1/profiles?select=role,name`, {
      headers: restHeaders,
    });
    if (!profileResp.ok) {
      return json({ error: "Could not verify caller identity" }, 401);
    }
    const profiles = await profileResp.json();
    const profile = Array.isArray(profiles) ? profiles[0] : null;
    if (!profile) {
      return json({ error: "No profile found for the authenticated user" }, 403);
    }

    // RLS scopes this to classes the caller is actually allowed to see (their own, as
    // student or teacher, or all of them if they're an admin/director). An empty result here
    // means the caller has no legitimate relationship to this class at all.
    const classResp = await fetch(
      `${supabaseUrl}/rest/v1/classes?id=eq.${encodeURIComponent(classId)}&select=teacher_name,student_name`,
      { headers: restHeaders },
    );
    if (!classResp.ok) {
      return json({ error: "Could not verify class access" }, 403);
    }
    const classes = await classResp.json();
    const targetClass = Array.isArray(classes) ? classes[0] : null;
    if (!targetClass) {
      return json({ error: "You do not have access to this class" }, 403);
    }

    const isAdmin = profile.role === "ADMIN" || profile.role === "DIRECTOR";
    const isTeacherForClass =
      isAdmin || (profile.role === "TEACHER" && profile.name === targetClass.teacher_name);
    const isStudentForClass = profile.name === targetClass.student_name;

    if (!isTeacherForClass && !isStudentForClass) {
      return json({ error: "You are not a participant in this class" }, 403);
    }

    const participantName = name || profile.name || identity;
    const token = new AccessToken(apiKey, apiSecret, {
      identity,
      name: participantName,
      ttl: "2h",
    });

    token.addGrant({
      roomJoin: true,
      room,
      canPublish: true,
      canSubscribe: true,
      canPublishData: true,
      // Only ever granted based on the server-verified role/relationship above - never from a
      // client-supplied flag.
      roomAdmin: isTeacherForClass,
      roomRecord: isTeacherForClass,
    });

    const jwt = await token.toJwt();

    return json({
      token: jwt,
      identity,
      room,
      expiresIn: 7200,
    });
  } catch (err: unknown) {
    const message = err instanceof Error ? err.message : String(err);
    return json({ error: message }, 500);
  }
});
