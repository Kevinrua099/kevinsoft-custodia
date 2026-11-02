-- Ejecutar en la base existente antes de desplegar la API actualizada.
-- Conserva los reportes y evidencias anteriores.
CREATE TABLE IF NOT EXISTS evidencias_reporte_archivo (
  id_evidencia int NOT NULL PRIMARY KEY,
  tipo_mime varchar(30) NOT NULL,
  contenido LONGBLOB NOT NULL,
  FOREIGN KEY (id_evidencia) REFERENCES evidencias_reporte (id_evidencia) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
