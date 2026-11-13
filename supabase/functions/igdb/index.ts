// intermediaria con igdb (videojuegos).
//
// la app manda aqui la consulta y esta funcion la reenvia a igdb con las credenciales de twitch,
// que asi no salen nunca del servidor. solo responde a usuarios con sesion iniciada en palette.
//
// necesita dos secretos en supabase (edge functions > secrets):
//   IGDB_CLIENT_ID y IGDB_CLIENT_SECRET

const cors = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, apikey, content-type, x-client-info",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

const clientId = Deno.env.get("IGDB_CLIENT_ID") ?? "";
const clientSecret = Deno.env.get("IGDB_CLIENT_SECRET") ?? "";
const supabaseUrl = Deno.env.get("SUPABASE_URL") ?? "";

// el token de twitch dura unos dos meses: se guarda mientras la funcion siga en memoria.
let token: { value: string; expiresAt: number } | null = null;

async function twitchToken(): Promise<string> {
  if (token && Date.now() < token.expiresAt) return token.value;
  const res = await fetch(
    `https://id.twitch.tv/oauth2/token?client_id=${clientId}&client_secret=${clientSecret}&grant_type=client_credentials`,
    { method: "POST" },
  );
  if (!res.ok) throw new Error(`Twitch no ha dado token (${res.status})`);
  const data = await res.json();
  token = { value: data.access_token, expiresAt: Date.now() + (data.expires_in - 3600) * 1000 };
  return token.value;
}

// comprueba que quien llama tiene una sesion valida preguntandoselo al propio supabase.
async function signedIn(req: Request): Promise<boolean> {
  const authorization = req.headers.get("Authorization");
  const apikey = req.headers.get("apikey");
  if (!authorization || !apikey) return false;
  const res = await fetch(`${supabaseUrl}/auth/v1/user`, { headers: { Authorization: authorization, apikey } });
  return res.ok;
}

function reply(body: string, status: number): Response {
  return new Response(body, { status, headers: { ...cors, "Content-Type": "application/json" } });
}

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: cors });
  if (req.method !== "POST") return reply('{"message":"Solo POST"}', 405);
  if (!(await signedIn(req))) return reply('{"message":"Hace falta iniciar sesión"}', 401);

  const query = await req.text();
  if (!query || query.length > 2000) return reply('{"message":"Consulta no válida"}', 400);

  try {
    const res = await fetch("https://api.igdb.com/v4/games", {
      method: "POST",
      headers: { "Client-ID": clientId, Authorization: `Bearer ${await twitchToken()}`, Accept: "application/json" },
      body: query,
    });
    if (res.status === 401) token = null;
    return reply(await res.text(), res.status);
  } catch (error) {
    return reply(JSON.stringify({ message: String(error) }), 502);
  }
});
