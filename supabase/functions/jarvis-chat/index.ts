import "jsr:@supabase/functions-js/edge-runtime.d.ts";

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

Deno.serve(async (req: Request) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: corsHeaders });
  if (req.method !== "POST") return json({ error: "Method not allowed" }, 405);

  // Supabase validates the signed-in user's JWT because verify_jwt is enabled.
  if (!req.headers.get("Authorization")?.startsWith("Bearer ")) {
    return json({ error: "Sign-in required" }, 401);
  }

  const apiUrl = Deno.env.get("AI_API_URL");
  const apiKey = Deno.env.get("AI_API_KEY");
  const model = Deno.env.get("AI_MODEL");
  if (!apiUrl || !apiKey || !model) {
    return json({ error: "AI backend is not configured yet" }, 503);
  }

  let body: { messages?: Array<{ role: string; content: string }> };
  try {
    body = await req.json();
  } catch {
    return json({ error: "Invalid JSON body" }, 400);
  }

  const messages = body.messages;
  if (!Array.isArray(messages) || messages.length < 1 || messages.length > 30) {
    return json({ error: "Send between 1 and 30 messages" }, 400);
  }

  const safeMessages = messages.filter((m) =>
    m && ["user", "assistant"].includes(m.role) &&
    typeof m.content === "string" &&
    m.content.trim().length > 0 &&
    m.content.length <= 6000
  );
  if (safeMessages.length !== messages.length) {
    return json({ error: "One or more messages are invalid" }, 400);
  }

  try {
    const upstream = await fetch(apiUrl, {
      method: "POST",
      headers: {
        "Authorization": "Bearer " + apiKey,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        model: model,
        messages: [
          {
            role: "system",
            content: "You are JARVIS, a helpful assistant. Reply in the user's language (Kannada, English, or Hindi). Be accurate, admit uncertainty, and do not invent facts.",
          },
          ...safeMessages,
        ],
        temperature: 0.4,
      }),
      signal: AbortSignal.timeout(25000),
    });

    if (!upstream.ok) {
      return json({ error: "AI provider request failed", providerStatus: upstream.status }, 502);
    }

    const data = await upstream.json();
    const reply = data?.choices?.[0]?.message?.content;
    if (typeof reply !== "string" || !reply.trim()) {
      return json({ error: "AI provider returned an empty response" }, 502);
    }
    return json({ reply: reply.trim() });
  } catch {
    return json({ error: "AI service temporarily unavailable" }, 503);
  }
});
