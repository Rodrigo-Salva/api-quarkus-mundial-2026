-- Foto de perfil del usuario
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS avatar_url VARCHAR(500);

-- Imagen/banner de la sala
ALTER TABLE leagues
    ADD COLUMN IF NOT EXISTS image_url VARCHAR(500);
