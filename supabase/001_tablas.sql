-- palette: tablas y reglas de acceso.
-- se ejecuta una vez en supabase, en sql editor > new query.
-- las fechas se guardan en milisegundos desde 1970, igual que en la app.

create table public.profiles (
  id uuid primary key references auth.users (id) on delete cascade,
  username text not null unique,
  name text not null,
  bio text not null default '',
  favorite bigint not null default 4281162736,  -- 0xFF2D5BF0
  photo_url text,
  watchlist_public boolean not null default true,
  created_at bigint not null default (extract(epoch from now()) * 1000)::bigint
);

create table public.logs (
  id text primary key,
  user_id uuid not null references public.profiles (id) on delete cascade,
  media_id text not null,
  media jsonb not null,
  stars real not null check (stars >= 0.5 and stars <= 5),
  review text not null default '',
  pinned boolean not null default false,
  hidden boolean not null default false,
  created_at bigint not null default (extract(epoch from now()) * 1000)::bigint,
  unique (user_id, media_id)
);
create index logs_created_at on public.logs (created_at desc);

create table public.watchlist (
  user_id uuid not null references public.profiles (id) on delete cascade,
  media_id text not null,
  media jsonb not null,
  created_at bigint not null default (extract(epoch from now()) * 1000)::bigint,
  primary key (user_id, media_id)
);

create table public.follows (
  follower_id uuid not null references public.profiles (id) on delete cascade,
  followed_id uuid not null references public.profiles (id) on delete cascade,
  created_at bigint not null default (extract(epoch from now()) * 1000)::bigint,
  primary key (follower_id, followed_id),
  check (follower_id <> followed_id)
);

create table public.messages (
  id text primary key,
  from_id uuid not null references public.profiles (id) on delete cascade,
  to_id uuid not null references public.profiles (id) on delete cascade,
  text text not null default '',
  media jsonb,
  log jsonb,
  read boolean not null default false,
  sent_at bigint not null default (extract(epoch from now()) * 1000)::bigint
);
create index messages_people on public.messages (from_id, to_id, sent_at);

create table public.likes (
  user_id uuid not null references public.profiles (id) on delete cascade,
  log_id text not null references public.logs (id) on delete cascade,
  primary key (user_id, log_id)
);


-- permisos: solo entra quien ha iniciado sesion. sin sesion no se ve nada.

revoke all on public.profiles, public.logs, public.watchlist, public.follows, public.messages, public.likes from anon;

grant select, insert, update on public.profiles to authenticated;
grant select, insert, update, delete on public.logs to authenticated;
grant select, insert, update, delete on public.watchlist to authenticated;
grant select, insert, update, delete on public.follows to authenticated;
grant select, insert, update, delete on public.likes to authenticated;
-- de un mensaje ya enviado solo se puede cambiar si esta leido
grant select, insert on public.messages to authenticated;
grant update (read) on public.messages to authenticated;

alter table public.profiles enable row level security;
alter table public.logs enable row level security;
alter table public.watchlist enable row level security;
alter table public.follows enable row level security;
alter table public.messages enable row level security;
alter table public.likes enable row level security;


-- perfiles: todos se ven entre si; cada uno edita el suyo.
create policy "ver perfiles" on public.profiles for select to authenticated using (true);
create policy "crear mi perfil" on public.profiles for insert to authenticated with check (id = auth.uid());
create policy "editar mi perfil" on public.profiles for update to authenticated using (id = auth.uid()) with check (id = auth.uid());

-- logs: se ven los mios y los de los demas que no esten ocultos.
create policy "ver logs" on public.logs for select to authenticated using (user_id = auth.uid() or not hidden);
create policy "crear mis logs" on public.logs for insert to authenticated with check (user_id = auth.uid());
create policy "editar mis logs" on public.logs for update to authenticated using (user_id = auth.uid()) with check (user_id = auth.uid());
create policy "borrar mis logs" on public.logs for delete to authenticated using (user_id = auth.uid());

-- pendientes: los mios, y los de quien los tenga publicos.
create policy "ver pendientes" on public.watchlist for select to authenticated using (
  user_id = auth.uid()
  or exists (select 1 from public.profiles p where p.id = watchlist.user_id and p.watchlist_public)
);
create policy "guardar mis pendientes" on public.watchlist for insert to authenticated with check (user_id = auth.uid());
create policy "editar mis pendientes" on public.watchlist for update to authenticated using (user_id = auth.uid()) with check (user_id = auth.uid());
create policy "quitar mis pendientes" on public.watchlist for delete to authenticated using (user_id = auth.uid());

-- seguidos: quien sigue a quien es publico dentro de la app; solo yo decido a quien sigo.
create policy "ver seguidos" on public.follows for select to authenticated using (true);
create policy "seguir" on public.follows for insert to authenticated with check (follower_id = auth.uid());
create policy "mantener seguido" on public.follows for update to authenticated using (follower_id = auth.uid()) with check (follower_id = auth.uid());
create policy "dejar de seguir" on public.follows for delete to authenticated using (follower_id = auth.uid());

-- mensajes: solo los ven quien los manda y quien los recibe. quien recibe puede marcarlos como leidos.
create policy "ver mis mensajes" on public.messages for select to authenticated using (from_id = auth.uid() or to_id = auth.uid());
create policy "mandar mensajes" on public.messages for insert to authenticated with check (from_id = auth.uid());
create policy "marcar como leido" on public.messages for update to authenticated using (to_id = auth.uid()) with check (to_id = auth.uid());

-- me gusta.
create policy "ver me gusta" on public.likes for select to authenticated using (true);
create policy "dar me gusta" on public.likes for insert to authenticated with check (user_id = auth.uid());
create policy "mantener me gusta" on public.likes for update to authenticated using (user_id = auth.uid()) with check (user_id = auth.uid());
create policy "quitar me gusta" on public.likes for delete to authenticated using (user_id = auth.uid());
