# JARVIS AI backend setup

This repository includes a provider-neutral Supabase Edge Function template.

## Security design

- The function requires a valid Supabase user JWT (verify_jwt = true).
- Provider credentials are read only from server-side Edge Function secrets.
- Never commit API keys, .env files, Supabase secret keys, or provider credentials.
- The Android APK must never contain the provider API key.
- Configure only an AI provider whose terms allow the account holder and intended users.

## Required server secrets

Set these in the JARVIS Supabase project's Edge Function Secrets:

- AI_API_URL: provider's OpenAI-compatible chat-completions endpoint.
- AI_API_KEY: provider credential.
- AI_MODEL: model identifier allowed by that provider account.

The function expects an OpenAI-compatible request/response format. A provider with a different API format needs a dedicated adapter.

## Deploy

From the repository root with the Supabase CLI authenticated and linked to a dedicated JARVIS Supabase project:

    supabase link --project-ref YOUR_JARVIS_PROJECT_REF
    supabase secrets set AI_API_URL=... AI_API_KEY=... AI_MODEL=...
    supabase functions deploy jarvis-chat

Do not link this project to an unrelated application database. A dedicated JARVIS backend is required.

## Request contract

POST /functions/v1/jarvis-chat

Headers:
- Authorization: Bearer <signed-in-user-access-token>
- apikey: <Supabase publishable key>
- Content-Type: application/json

Body:

    {
      "messages": [
        {"role": "user", "content": "ನಮಸ್ಕಾರ"}
      ]
    }

Success:

    {"reply": "ನಮಸ್ಕಾರ! ನಿಮಗೆ ಹೇಗೆ ಸಹಾಯ ಮಾಡಲಿ?"}

The mobile app must sign the user in and send the access token. Do not expose an unauthenticated public AI proxy.
