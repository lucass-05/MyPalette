-- palette: fondo de perfil y borrado de cuenta.
-- se ejecuta una vez, despues de 002, en sql editor > new query.

-- color de fondo del perfil (argb). null: cada visitante lo ve con el fondo de su app.
alter table public.profiles add column background bigint;

-- borra la cuenta de quien la llama. al caer la fila de auth.users caen en cascada su perfil,
-- logs, pendientes, seguidos, mensajes y me gusta. solo puede borrarse uno a si mismo.
create or replace function public.delete_my_account()
returns void
language sql
security definer
set search_path = ''
as $$
  delete from auth.users where id = auth.uid();
$$;

revoke all on function public.delete_my_account() from public, anon;
grant execute on function public.delete_my_account() to authenticated;
