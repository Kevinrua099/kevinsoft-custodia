-- Ejecutar una vez en una base existente, después de patrullas_entrega_migration.sql.
-- Conserva los reportes anteriores; sus nuevos campos quedan vacíos.
ALTER TABLE reportes_pesca
  ADD COLUMN destino_reporte varchar(100) DEFAULT NULL,
  ADD COLUMN tipo_carga varchar(20) DEFAULT NULL,
  ADD COLUMN id_usuario_reporte int DEFAULT NULL,
  ADD COLUMN patrulla_placa varchar(20) DEFAULT NULL,
  ADD COLUMN patrulla_disco varchar(50) DEFAULT NULL,
  ADD CONSTRAINT fk_reportes_usuario_reporte FOREIGN KEY (id_usuario_reporte) REFERENCES usuarios (id_usuario);
