import { serve } from "https://deno.land/std@0.168.0/http/server.ts";
import { AccessToken } from "npm:livekit-server-sdk@2.6.0";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

serve(async (req: Request) => {
  // Handle CORS preflight
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  try {
    const apiKey = Deno.env.get("LIVEKIT_API_KEY") || "APIHUMTB5KGJXq4";
    const apiSecret = Deno.env.get("LIVEKIT_API_SECRET");

    if (!apiSecret) {
      return new Response(
        JSON.stringify({ error: "Server missing LIVEKIT_API_SECRET configuration" }),
        { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    const { room, identity, name, is_teacher } = await req.json();

    if (!room || !identity) {
      return new Response(
        JSON.stringify({ error: "Missing required fields: 'room' and 'identity'" }),
        { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    const participantName = name || identity;
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
      roomAdmin: !!is_teacher,
    });

    const jwt = await token.toJwt();

    return new Response(
      JSON.stringify({
        token: jwt,
        identity,
        room,
        expiresIn: 7200,
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
