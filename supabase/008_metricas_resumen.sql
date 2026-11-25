-- palette: opcional. que la tabla events no crezca sin limite.
-- se ejecuta una vez, despues de 007, en sql editor > new query.
--
-- antes hay que activar la extension pg_cron: database > extensions > pg_cron.
--
-- cada noche se guarda el resumen del dia anterior en metricas_por_dia (una fila por dia y evento,
-- ocupa casi nada) y se borran los eventos de hace mas de 180 dias. las vistas de 007 siguen
-- funcionando con los ultimos 6 meses; para mirar mas atras, metricas_por_dia.

create table public.metricas_por_dia (
  dia date not null,
  name text not null,
  eventos integer not null,
  dispositivos integer not null,
  primary key (dia, name)
);

alter table public.metricas_por_dia enable row level security;
-- sin politicas: solo se lee desde el panel

create or replace function public.resumir_metricas() returns void
language sql security definer set search_path = public as $$
  insert into metricas_por_dia (dia, name, eventos, dispositivos)
  select created_at::date, name, count(*), count(distinct device)
  from events
  where created_at >= current_date - 1 and created_at < current_date
  group by 1, 2
  on conflict (dia, name) do update set eventos = excluded.eventos, dispositivos = excluded.dispositivos;

  delete from events where created_at < now() - interval '180 days';
$$;

revoke all on function public.resumir_metricas() from anon, authenticated;

-- todas las noches a las 3:10 (hora utc)
select cron.schedule('resumir-metricas', '10 3 * * *', 'select public.resumir_metricas()');
