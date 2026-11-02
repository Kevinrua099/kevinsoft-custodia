-- Ejecutar una vez en una base existente. Las inspecciones anteriores se conservan como recepciones.
ALTER TABLE inspeccion_vehiculos
  ADD COLUMN tipo_inspeccion enum('Recepción','Entrega') NOT NULL DEFAULT 'Recepción';
