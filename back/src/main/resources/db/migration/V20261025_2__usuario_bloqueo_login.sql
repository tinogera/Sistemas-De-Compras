ALTER TABLE auth.usuario
  ADD COLUMN intentos_fallidos INT NOT NULL DEFAULT 0,
  ADD COLUMN bloqueado_hasta TIMESTAMPTZ;
