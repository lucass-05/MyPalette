-- palette: estadisticas de uso anonimas (core/metricas en la app).
-- se ejecuta una vez, despues de 006, en sql editor > new query.
--
-- la app manda eventos ("abrir_app", "buscar", "clic_afiliado"...) con un id inventado por
-- dispositivo. nadie puede leerlos desde la app: solo tu, desde el panel de supabase, con las
-- vistas de abajo (table editor o sql editor: select * from metricas_diarias).

create table public.events (
  id bigint generated always as identity primary key,
  created_at timestamptz not null default now(),
  -- hora del dispositivo en milisegundos (created_at es la del servidor)
  at bigint,
  name text not null,
  props jsonb not null default '{}',
  device text not null,
  visit text,
  platform text,
  version text,
  logged boolean not null default false
);

create index events_created_at on public.events (created_at);
create index events_name_created_at on public.events (name, created_at);

alter table public.events enable row level security;

-- cualquiera (con sesion o sin ella) puede mandar eventos, pero pequeños. no hay politica de
-- select, update ni delete: desde la app no se pueden leer, cambiar ni borrar
create policy "mandar eventos" on public.events for insert to anon, authenticated
  with check (char_length(name) <= 40 and char_length(device) <= 40 and pg_column_size(props) <= 2000);

-- ---- vistas para leer las metricas (solo desde el panel)

-- cada dia: usuarios activos, aperturas, registros, busquedas...
create view public.metricas_diarias as
select
  created_at::date as dia,
  count(distinct device) as usuarios_activos,
  count(distinct device) filter (where logged) as usuarios_con_cuenta,
  count(*) filter (where name = 'abrir_app') as aperturas,
  count(*) filter (where name = 'crear_cuenta') as cuentas_nuevas,
  count(*) filter (where name = 'registrar_contenido') as registros,
  count(*) filter (where name = 'buscar') as busquedas,
  count(*) filter (where name = 'enviar_mensaje') as mensajes,
  count(*) filter (where name = 'clic_recomendacion') as clics_en_descubrir,
  count(*) filter (where name in ('clic_afiliado', 'clic_plataforma')) as clics_hacia_fuera
from public.events
group by 1
order by 1 desc;

-- de donde llega la gente a la web (google, tiktok, instagram...) y con que campaña
create view public.metricas_origen as
select
  created_at::date as dia,
  coalesce(props ->> 'utm_source', nullif(split_part(split_part(props ->> 'origen', '://', 2), '/', 1), ''), 'directo') as origen,
  props ->> 'utm_campaign' as campana,
  count(distinct device) as visitantes
from public.events
where name = 'abrir_app'
group by 1, 2, 3
order by 1 desc, 4 desc;

-- cuantos de los que llegan un dia vuelven al dia siguiente, a la semana y al mes
create view public.metricas_retencion as
with primera as (
  select device, min(created_at)::date as dia0 from public.events group by device
), activo as (
  select distinct device, created_at::date as dia from public.events
)
select
  p.dia0 as cohorte,
  count(*) as nuevos,
  count(*) filter (where exists (select 1 from activo a where a.device = p.device and a.dia = p.dia0 + 1)) as vuelven_dia_1,
  count(*) filter (where exists (select 1 from activo a where a.device = p.device and a.dia between p.dia0 + 7 and p.dia0 + 13)) as vuelven_semana_2,
  count(*) filter (where exists (select 1 from activo a where a.device = p.device and a.dia between p.dia0 + 30 and p.dia0 + 59)) as vuelven_mes_2
from primera p
group by 1
order by 1 desc;

-- anuncios: veces visto, clics y porcentaje de clics (ctr), lo que se enseña a un anunciante
create view public.metricas_anuncios as
select
  props ->> 'anuncio' as anuncio,
  props ->> 'hueco' as hueco,
  (props ->> 'ejemplo')::boolean as ejemplo,
  count(*) filter (where name = 'anuncio_visto') as impresiones,
  count(distinct device) filter (where name = 'anuncio_visto') as personas,
  count(*) filter (where name = 'anuncio_clic') as clics,
  round(100.0 * count(*) filter (where name = 'anuncio_clic') / nullif(count(*) filter (where name = 'anuncio_visto'), 0), 2) as ctr
from public.events
where name in ('anuncio_visto', 'anuncio_clic')
group by 1, 2, 3
order by 4 desc;

-- clics hacia tiendas y plataformas: cuantos llevan id de afiliado (los que pueden cobrar)
create view public.metricas_afiliados as
select
  created_at::date as dia,
  name as evento,
  coalesce(props ->> 'tienda', props ->> 'plataforma') as destino,
  props ->> 'tipo' as tipo,
  count(*) as clics,
  count(*) filter (where (props ->> 'afiliado')::boolean) as clics_con_afiliado
from public.events
where name in ('clic_afiliado', 'clic_plataforma')
group by 1, 2, 3, 4
order by 1 desc, 5 desc;

-- lo que mas se busca (sirve para saber que contenidos y que marcas interesan)
create view public.metricas_busquedas as
select props ->> 'consulta' as consulta, count(*) as veces, count(distinct device) as personas
from public.events
where name = 'buscar' and coalesce(props ->> 'consulta', '') <> ''
group by 1
order by 2 desc;

-- que fila de descubrir funciona mejor
create view public.metricas_descubrir as
select split_part(props ->> 'fila', '_', 1) as fila, count(*) as clics, count(distinct device) as personas
from public.events
where name = 'clic_recomendacion'
group by 1
order by 2 desc;

-- las vistas se saltan las reglas de la tabla, asi que se cierran para la app: solo el panel las lee
revoke all on public.metricas_diarias, public.metricas_origen, public.metricas_retencion,
  public.metricas_anuncios, public.metricas_afiliados, public.metricas_busquedas, public.metricas_descubrir
  from anon, authenticated;
