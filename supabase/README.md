# Supabase Edge Functions & Database Deployment Guide

## 1. Prerequisites
Install Supabase CLI:
```bash
npm install -g supabase
```

Login and link to project:
```bash
supabase login
supabase link --project-ref amcdmiioovbhzkjqxdrk
```

## 2. Deploy Database Schema & RLS Policies
Run **only** `supabase/migrations/0001_lockdown_rls_and_auth.sql` (via the SQL Editor, or
`supabase db push` once the CLI project is linked). It creates every table itself, so you do
NOT also need to run `schema.sql` or `supabase_rls_policies.sql` - both of those are kept only
as historical record and are marked superseded at the top of each file; their policies were
either fully open (`USING (true)`) or never actually took effect. See the migration file's
header comment for why, and for the one-time step to make your own account an admin after you
sign up for real through the app.
```bash
supabase db push
# Or paste supabase/migrations/0001_lockdown_rls_and_auth.sql into the Supabase SQL Editor:
# https://supabase.com/dashboard/project/amcdmiioovbhzkjqxdrk/sql
```

## 3. Set Edge Function Secrets
`SUPABASE_URL`, `SUPABASE_ANON_KEY` and `SUPABASE_SERVICE_ROLE_KEY` are injected automatically -
you only need to set these:
```bash
supabase secrets set LIVEKIT_API_KEY="APIHUMTB5KGJXq4"
supabase secrets set LIVEKIT_API_SECRET="your_livekit_api_secret_here"
supabase secrets set FCM_SERVER_KEY="your_firebase_server_key_here"
```

## 4. Deploy Edge Functions
Deploy both edge functions with `--no-verify-jwt` - each one does its own verification by
forwarding the caller's JWT to PostgREST and trusting only what RLS hands back, which is a
better fit here than the gateway's own pass/fail JWT gate:
```bash
supabase functions deploy livekit-token --no-verify-jwt
supabase functions deploy send-call-push --no-verify-jwt
```

## 5. Endpoints Verification
Both endpoints now require the CALLER's own Supabase Auth access token as the `Authorization`
bearer (not the anon key) - get one via `POST /auth/v1/token?grant_type=password` first.

- **LiveKit Token Generation** (only succeeds if the caller is the class's teacher, its
  student, or an admin - checked server-side against the `classes` table):
  `POST https://amcdmiioovbhzkjqxdrk.supabase.co/functions/v1/livekit-token`
  Headers: `Authorization: Bearer <user access token>`, `apikey: <anon key>`
  Body: `{"room": "quran-class-cls_01", "class_id": "cls_01", "identity": "usr_123", "name": "Zaid Ahmed"}`

- **VoIP Call Push** (only the class's teacher/admin can start `INCOMING_CALL`; teacher/student
  can `CANCEL_CALL`):
  `POST https://amcdmiioovbhzkjqxdrk.supabase.co/functions/v1/send-call-push`
  Headers: `Authorization: Bearer <user access token>`, `apikey: <anon key>`
  Body: `{"action": "INCOMING_CALL", "class_id": "cls_01"}`
