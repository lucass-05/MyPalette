-- palette: fotos de perfil y limites de tamaño.
-- se ejecuta una vez, despues de 001_tablas.sql, en sql editor > new query.

-- almacen de fotos de perfil. cualquiera puede verlas por su direccion; solo se admiten jpeg de hasta 1 mb.
insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values ('avatars', 'avatars', true, 1048576, array['image/jpeg'])
on conflict (id) do nothing;

-- cada usuario solo puede tocar los ficheros de su carpeta, que se llama como su id.
create policy "ver mi avatar" on storage.objects for select to authenticated
  using (bucket_id = 'avatars' and (storage.foldername(name))[1] = auth.uid()::text);
create policy "subir mi avatar" on storage.objects for insert to authenticated
  with check (bucket_id = 'avatars' and (storage.foldername(name))[1] = auth.uid()::text);
create policy "cambiar mi avatar" on storage.objects for update to authenticated
  using (bucket_id = 'avatars' and (storage.foldername(name))[1] = auth.uid()::text)
  with check (bucket_id = 'avatars' and (storage.foldername(name))[1] = auth.uid()::text);
create policy "borrar mi avatar" on storage.objects for delete to authenticated
  using (bucket_id = 'avatars' and (storage.foldername(name))[1] = auth.uid()::text);

-- limites: la app ya los respeta, pero el servidor no debe fiarse de la app.
alter table public.profiles
  add constraint username_valido check (username ~ '^[a-z0-9._]{2,30}$'),
  add constraint name_corto check (char_length(name) between 1 and 60),
  add constraint bio_corta check (char_length(bio) <= 300),
  -- la foto solo puede ser una de nuestro almacen, no un enlace a cualquier sitio
  add constraint foto_propia check (
    photo_url is null
    or photo_url like 'https://unlehgzkissbahwcjedp.supabase.co/storage/v1/object/public/avatars/%'
  );

alter table public.logs add constraint review_corta check (char_length(review) <= 4000);
alter table public.messages add constraint texto_corto check (char_length(text) <= 4000);
