-- Ejecutar una sola vez en la base existente antes de actualizar Render.
-- Conserva las inspecciones y fotos anteriores.
CREATE TABLE IF NOT EXISTS inspeccion_fotos_archivo (
  id_foto int NOT NULL PRIMARY KEY,
  tipo_mime varchar(30) NOT NULL,
  contenido LONGBLOB NOT NULL,
  FOREIGN KEY (id_foto) REFERENCES inspeccion_fotos (id_foto) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
