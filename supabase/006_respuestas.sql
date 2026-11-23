-- palette: responder a un mensaje concreto (deslizandolo en el chat) y responder a historias.
-- se ejecuta una vez, despues de 005, en sql editor > new query.
-- la app funciona sin esto: los mensajes se mandan igual, pero sin la cita y sin la marca de historia.

-- el mensaje al que se contesta: su id, quien lo escribio y un trozo del texto
alter table public.messages add column reply_to jsonb;

-- true si el mensaje es una respuesta a una historia (la historia va en la columna log)
alter table public.messages add column story boolean not null default false;
