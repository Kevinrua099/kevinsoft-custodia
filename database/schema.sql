-- Estructura vacía, sin usuarios ni datos. Importar solo en una base nueva.
CREATE TABLE usuarios (
  id_usuario int NOT NULL AUTO_INCREMENT PRIMARY KEY,
  cedula varchar(15) NOT NULL UNIQUE,
  nombres_completos varchar(100) NOT NULL,
  cia_empresa varchar(50) DEFAULT 'PRINCIPAL',
  rol enum('Jefe','Sistemas','Consolero','Custodio') NOT NULL,
  password_hash varchar(255) NOT NULL,
  estado enum('Activo','Inactivo') DEFAULT 'Activo'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE vehiculos (
  id_vehiculo int NOT NULL AUTO_INCREMENT PRIMARY KEY,
  placa varchar(20) NOT NULL UNIQUE,
  cia_vehiculo varchar(50) NOT NULL,
  estado enum('Disponible','En Ruta','Mantenimiento') DEFAULT 'Disponible',
  disco varchar(50) DEFAULT 'S/N'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE armamento (
  id_arma int NOT NULL AUTO_INCREMENT PRIMARY KEY,
  serie varchar(50) NOT NULL UNIQUE,
  tipo_arma varchar(50) NOT NULL,
  marca varchar(50) NOT NULL,
  calibre varchar(20) NOT NULL,
  estado enum('En Armería','Entregada','Mantenimiento','Robada','Inactiva') DEFAULT 'En Armería'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE salvoconductos_rutas (
  id_ruta int NOT NULL AUTO_INCREMENT PRIMARY KEY,
  numero_registro varchar(50) DEFAULT NULL UNIQUE,
  fecha_emision datetime DEFAULT CURRENT_TIMESTAMP,
  fecha_salida date DEFAULT NULL,
  fecha_retorno date DEFAULT NULL,
  tipo_viaje varchar(30) DEFAULT NULL,
  asunto varchar(255) DEFAULT NULL,
  origen varchar(100) DEFAULT NULL,
  destino varchar(100) DEFAULT NULL,
  zona varchar(100) DEFAULT NULL,
  id_vehiculo int DEFAULT NULL,
  estado_ruta enum('Activa','En Tránsito','En espera de confirmación','Finalizada') DEFAULT 'En Tránsito',
  creado_por varchar(100) DEFAULT 'Sistema',
  FOREIGN KEY (id_vehiculo) REFERENCES vehiculos (id_vehiculo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE asignacion_personal_armas (
  id_asignacion int NOT NULL AUTO_INCREMENT PRIMARY KEY,
  id_ruta int DEFAULT NULL,
  id_usuario int DEFAULT NULL,
  rol_en_ruta varchar(50) DEFAULT NULL,
  id_arma int DEFAULT NULL,
  municiones_entregadas int DEFAULT 10,
  fecha_hora_entrega datetime DEFAULT NULL,
  fecha_hora_devolucion datetime DEFAULT NULL,
  FOREIGN KEY (id_ruta) REFERENCES salvoconductos_rutas (id_ruta),
  FOREIGN KEY (id_usuario) REFERENCES usuarios (id_usuario),
  FOREIGN KEY (id_arma) REFERENCES armamento (id_arma)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE reportes_pesca (
  id_reporte int NOT NULL AUTO_INCREMENT PRIMARY KEY,
  id_ruta int DEFAULT NULL,
  fecha_hora_reporte datetime DEFAULT NULL,
  origen_camaronera varchar(100) DEFAULT NULL,
  destino_reporte varchar(100) DEFAULT NULL,
  tipo_carga varchar(20) DEFAULT NULL,
  id_usuario_reporte int DEFAULT NULL,
  patrulla_placa varchar(20) DEFAULT NULL,
  patrulla_disco varchar(50) DEFAULT NULL,
  observaciones text,
  tipo_reporte varchar(50) DEFAULT 'Pesca',
  FOREIGN KEY (id_ruta) REFERENCES salvoconductos_rutas (id_ruta),
  FOREIGN KEY (id_usuario_reporte) REFERENCES usuarios (id_usuario)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE guias_plataformas (
  id_guia int NOT NULL AUTO_INCREMENT PRIMARY KEY,
  id_reporte int DEFAULT NULL,
  identificador_plataforma varchar(50) DEFAULT NULL,
  ruta_foto_guia varchar(255) DEFAULT NULL,
  FOREIGN KEY (id_reporte) REFERENCES reportes_pesca (id_reporte) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE detalle_piscinas (
  id_detalle int NOT NULL AUTO_INCREMENT PRIMARY KEY,
  id_guia int DEFAULT NULL,
  numero_piscina varchar(50) DEFAULT NULL,
  libraje decimal(10,2) DEFAULT NULL,
  FOREIGN KEY (id_guia) REFERENCES guias_plataformas (id_guia) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE evidencias_reporte (
  id_evidencia int NOT NULL AUTO_INCREMENT PRIMARY KEY,
  id_reporte int DEFAULT NULL,
  ruta_archivo varchar(255) DEFAULT NULL,
  tipo_archivo varchar(50) DEFAULT NULL,
  FOREIGN KEY (id_reporte) REFERENCES reportes_pesca (id_reporte) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE evidencias_reporte_archivo (
  id_evidencia int NOT NULL PRIMARY KEY,
  tipo_mime varchar(30) NOT NULL,
  contenido LONGBLOB NOT NULL,
  FOREIGN KEY (id_evidencia) REFERENCES evidencias_reporte (id_evidencia) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE inspeccion_vehiculos (
  id_inspeccion int NOT NULL AUTO_INCREMENT PRIMARY KEY,
  id_usuario int DEFAULT NULL,
  placa varchar(20) DEFAULT NULL,
  disco varchar(50) DEFAULT NULL,
  kilometraje int DEFAULT NULL,
  observaciones_estado text,
  fecha_hora_recepcion datetime DEFAULT CURRENT_TIMESTAMP,
  usuario_nombre varchar(100) DEFAULT NULL,
  tipo_inspeccion enum('Recepción','Entrega') NOT NULL DEFAULT 'Recepción',
  FOREIGN KEY (id_usuario) REFERENCES usuarios (id_usuario)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE inspeccion_fotos (
  id_foto int NOT NULL AUTO_INCREMENT PRIMARY KEY,
  id_inspeccion int DEFAULT NULL,
  ruta_foto varchar(255) DEFAULT NULL,
  FOREIGN KEY (id_inspeccion) REFERENCES inspeccion_vehiculos (id_inspeccion) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE inspeccion_fotos_archivo (
  id_foto int NOT NULL PRIMARY KEY,
  tipo_mime varchar(30) NOT NULL,
  contenido LONGBLOB NOT NULL,
  FOREIGN KEY (id_foto) REFERENCES inspeccion_fotos (id_foto) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE registro_bandidas (
  id_registro int NOT NULL AUTO_INCREMENT PRIMARY KEY,
  alias varchar(100) NOT NULL,
  nombre_completo varchar(150) DEFAULT NULL,
  zona_frecuente varchar(150) DEFAULT NULL,
  nivel_riesgo enum('Bajo','Medio','Alto','Crítico') DEFAULT 'Medio',
  observaciones text,
  fecha_registro datetime DEFAULT CURRENT_TIMESTAMP,
  registrado_por varchar(100) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
