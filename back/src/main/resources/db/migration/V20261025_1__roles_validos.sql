-- Los pasos de aprobacion no validaban el rol: un typo dejaba el item esperando a un aprobador inexistente.
ALTER TABLE aprobaciones.regla_paso ADD CONSTRAINT regla_paso_rol_valido
  CHECK (rol IS NULL OR rol IN ('SOLICITANTE', 'ENCARGADO', 'ADMIN', 'SUPERVISOR'));
ALTER TABLE aprobaciones.paso_item ADD CONSTRAINT paso_item_rol_valido
  CHECK (rol IS NULL OR rol IN ('SOLICITANTE', 'ENCARGADO', 'ADMIN', 'SUPERVISOR'));
