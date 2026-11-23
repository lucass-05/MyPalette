-- palette: comentarios en las reseñas, listas, perfil personalizable y grupos editables.
-- se ejecuta una vez, despues de 004, en sql editor > new query.
-- hay que ejecutarlo antes de publicar la version de la app que lo usa: sin estas columnas
-- el servidor rechaza los perfiles que la app nueva intenta guardar.


-- ---- comentarios
-- un comentario va siempre sobre una reseña (un log). quien puede ver la reseña puede ver
-- y escribir comentarios: la subconsulta a logs ya pasa por las reglas de logs, asi que una
-- reseña oculta tampoco deja ver ni comentar.

create table public.comments (
  id text primary key,
  log_id text not null references public.logs (id) on delete cascade,
  user_id uuid not null references public.profiles (id) on delete cascade,
  text text not null check (char_length(text) between 1 and 1000),
  created_at bigint not null default (extract(epoch from now()) * 1000)::bigint
);
create index comments_log on public.comments (log_id, created_at);

revoke all on public.comments from anon;
grant select, insert, delete on public.comments to authenticated;
alter table public.comments enable row level security;

create policy "ver comentarios" on public.comments for select to authenticated
  using (exists (select 1 from public.logs l where l.id = comments.log_id));
create policy "comentar" on public.comments for insert to authenticated
  with check (user_id = auth.uid() and exists (select 1 from public.logs l where l.id = comments.log_id));
-- cada uno borra los suyos, y el dueño de la reseña puede borrar cualquiera de la suya
create policy "borrar comentario" on public.comments for delete to authenticated
  using (user_id = auth.uid() or exists (select 1 from public.logs l where l.id = comments.log_id and l.user_id = auth.uid()));


-- ---- me gusta con fecha, para poder avisar de ellos
-- los que ya habia se quedan con 0: son viejos y no se avisa de ellos

alter table public.likes add column created_at bigint not null default 0;
alter table public.likes alter column created_at set default (extract(epoch from now()) * 1000)::bigint;


-- ---- listas
-- una lista es de contenido (media guarda los contenidos) o de reseñas (log_ids guarda que logs mios entran).
-- las publicas las ve todo el mundo en el perfil; las privadas solo quien las hizo.

create table public.lists (
  id text primary key,
  user_id uuid not null references public.profiles (id) on delete cascade,
  name text not null check (char_length(name) between 1 and 60),
  kind text not null check (kind in ('media', 'reviews')),
  media jsonb not null default '[]',
  log_ids jsonb not null default '[]',
  public boolean not null default true,
  created_at bigint not null default (extract(epoch from now()) * 1000)::bigint
);

revoke all on public.lists from anon;
grant select, insert, update, delete on public.lists to authenticated;
alter table public.lists enable row level security;

create policy "ver listas" on public.lists for select to authenticated using (user_id = auth.uid() or public);
create policy "crear mis listas" on public.lists for insert to authenticated with check (user_id = auth.uid());
create policy "editar mis listas" on public.lists for update to authenticated using (user_id = auth.uid()) with check (user_id = auth.uid());
create policy "borrar mis listas" on public.lists for delete to authenticated using (user_id = auth.uid());


-- ---- perfil personalizable
-- banner_url: la foto de cabecera, guardada en la misma carpeta que la foto de perfil.
-- layout: los bloques del perfil (textos, portadas, diario) en json, tal como los guarda la app.

alter table public.profiles
  add column banner_url text,
  add column layout jsonb,
  add constraint banner_propio check (
    banner_url is null
    or banner_url like 'https://unlehgzkissbahwcjedp.supabase.co/storage/v1/object/public/avatars/%'
  ),
  add constraint layout_corto check (layout is null or pg_column_size(layout) < 200000);


-- ---- grupos editables
-- cualquier miembro puede cambiar el nombre y la foto y meter gente; quien lo creo puede ademas
-- sacar a otros. el creador no se puede cambiar: solo se deja actualizar el nombre y la foto.

revoke update on public.groups from authenticated;
grant update (name, photo_url) on public.groups to authenticated;

drop policy "editar mi grupo" on public.groups;
create policy "editar mi grupo" on public.groups for update to authenticated
  using (owner_id = auth.uid() or public.is_group_member(id))
  with check (owner_id = auth.uid() or public.is_group_member(id));

drop policy "meter miembros" on public.group_members;
create policy "meter miembros" on public.group_members for insert to authenticated
  with check (
    public.is_group_member(group_id)
    or exists (select 1 from public.groups g where g.id = group_id and g.owner_id = auth.uid())
  );

drop policy "salir del grupo" on public.group_members;
create policy "salir del grupo" on public.group_members for delete to authenticated
  using (
    user_id = auth.uid()
    or exists (select 1 from public.groups g where g.id = group_id and g.owner_id = auth.uid())
  );
