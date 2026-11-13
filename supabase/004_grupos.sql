-- palette: grupos en los mensajes.
-- se ejecuta una vez, despues de 003, en sql editor > new query.

create table public.groups (
  id text primary key,
  name text not null check (char_length(name) between 1 and 60),
  photo_url text check (
    photo_url is null
    or photo_url like 'https://unlehgzkissbahwcjedp.supabase.co/storage/v1/object/public/avatars/%'
  ),
  owner_id uuid not null references public.profiles (id) on delete cascade,
  created_at bigint not null default (extract(epoch from now()) * 1000)::bigint
);

create table public.group_members (
  group_id text not null references public.groups (id) on delete cascade,
  user_id uuid not null references public.profiles (id) on delete cascade,
  primary key (group_id, user_id)
);

-- un mensaje va a una persona (to_id) o a un grupo (group_id), nunca a los dos.
alter table public.messages alter column to_id drop not null;
alter table public.messages add column group_id text references public.groups (id) on delete cascade;
alter table public.messages add constraint un_solo_destino check ((to_id is null) <> (group_id is null));
create index messages_group on public.messages (group_id, sent_at);

-- "estoy en este grupo?". va como funcion con permisos propios porque las reglas de group_members
-- no pueden consultarse a si mismas sin entrar en bucle.
create or replace function public.is_group_member(g text)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select exists (select 1 from public.group_members m where m.group_id = g and m.user_id = auth.uid());
$$;
revoke all on function public.is_group_member(text) from public, anon;
grant execute on function public.is_group_member(text) to authenticated;

revoke all on public.groups, public.group_members from anon;
grant select, insert, update, delete on public.groups to authenticated;
grant select, insert, delete on public.group_members to authenticated;

alter table public.groups enable row level security;
alter table public.group_members enable row level security;

-- grupos: los ven sus miembros; los crea, edita y borra quien los creo.
create policy "ver mis grupos" on public.groups for select to authenticated
  using (owner_id = auth.uid() or public.is_group_member(id));
create policy "crear grupo" on public.groups for insert to authenticated with check (owner_id = auth.uid());
create policy "editar mi grupo" on public.groups for update to authenticated
  using (owner_id = auth.uid()) with check (owner_id = auth.uid());
create policy "borrar mi grupo" on public.groups for delete to authenticated using (owner_id = auth.uid());

-- miembros: se ven entre si; los mete quien creo el grupo; cada uno puede salirse.
create policy "ver miembros" on public.group_members for select to authenticated
  using (public.is_group_member(group_id));
create policy "meter miembros" on public.group_members for insert to authenticated
  with check (exists (select 1 from public.groups g where g.id = group_id and g.owner_id = auth.uid()));
create policy "salir del grupo" on public.group_members for delete to authenticated using (user_id = auth.uid());

-- mensajes: ademas de los directos, los de mis grupos.
drop policy "ver mis mensajes" on public.messages;
create policy "ver mis mensajes" on public.messages for select to authenticated using (
  from_id = auth.uid() or to_id = auth.uid() or (group_id is not null and public.is_group_member(group_id))
);
drop policy "mandar mensajes" on public.messages;
create policy "mandar mensajes" on public.messages for insert to authenticated with check (
  from_id = auth.uid() and (group_id is null or public.is_group_member(group_id))
);
