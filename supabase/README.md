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
Run the SQL migration in `supabase/schema.sql`:
```bash
supabase db push
# Or copy and paste `supabase/schema.sql` into the Supabase SQL Editor:
# https://supabase.com/dashboard/project/amcdmiioovbhzkjqxdrk/sql
```

## 3. Set Edge Function Secrets
Set the required environment secrets for LiveKit JWT generation and FCM VoIP pushing:
```bash
supabase secrets set LIVEKIT_API_KEY="APIHUMTB5KGJXq4"
supabase secrets set LIVEKIT_API_SECRET="your_livekit_api_secret_here"
supabase secrets set FCM_SERVER_KEY="your_firebase_server_key_here"
```

## 4. Deploy Edge Functions
Deploy both edge functions with `--no-verify-jwt` so client apps can call them via standard anon key:
```bash
supabase functions deploy livekit-token --no-verify-jwt
supabase functions deploy send-call-push --no-verify-jwt
```

## 5. Endpoints Verification
- **LiveKit Token Generation**:
  `POST https://amcdmiioovbhzkjqxdrk.supabase.co/functions/v1/livekit-token`
  Body: `{"room": "test-room", "identity": "student_01", "name": "Zaid Ahmed"}`

- **VoIP Call Push**:
  `POST https://amcdmiioovbhzkjqxdrk.supabase.co/functions/v1/send-call-push`
  Body: `{"action": "INCOMING_CALL", "class_id": "class_01", "teacher_name": "Sheikh Abdullah", "student_name": "Zaid Ahmed", "room_name": "quran-room-01"}`
