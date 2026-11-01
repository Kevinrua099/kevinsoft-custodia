<?php
header('Access-Control-Allow-Origin: *');
header('Content-Type: application/json');
header('Access-Control-Allow-Methods: GET, POST');

require_once __DIR__ . '/conexion.php';

try {
    $conexion = conectarBD();
    $metodo = $_SERVER['REQUEST_METHOD'];

    // 1. OBTENER DATOS PARA LOS DESPLEGABLES
    if ($metodo == 'GET') {
        $q_usuarios = $conexion->query("SELECT id_usuario, cedula, nombres_completos, cia_empresa, rol FROM usuarios WHERE estado = 'Activo' ORDER BY nombres_completos ASC");
        $q_armas = $conexion->query("SELECT id_arma, serie, tipo_arma, marca, calibre FROM armamento WHERE estado = 'En Armería' ORDER BY tipo_arma ASC");
        $q_vehiculos = $conexion->query("SELECT id_vehiculo, placa, cia_vehiculo FROM vehiculos WHERE estado = 'Disponible' ORDER BY placa ASC");
        
        echo json_encode([
            "status" => "success", 
            "usuarios" => $q_usuarios->fetchAll(PDO::FETCH_ASSOC),
            "armas" => $q_armas->fetchAll(PDO::FETCH_ASSOC),
            "vehiculos" => $q_vehiculos->fetchAll(PDO::FETCH_ASSOC)
        ]);
    }
    
    // 2. REGISTRAR LA RUTA Y ASIGNAR PERSONAL MÚLTIPLE
    elseif ($metodo == 'POST') {
        $datos = json_decode(file_get_contents("php://input"), true);
        
        $num_registro = "ORD-" . date("Y") . "-" . time();

        // A. Insertamos la Ruta Principal (AHORA INCLUYE CREADO_POR)
        $ruta = $conexion->prepare("INSERT INTO salvoconductos_rutas (numero_registro, fecha_salida, fecha_retorno, tipo_viaje, asunto, origen, destino, id_vehiculo, estado_ruta, creado_por) VALUES (:num, :fsalida, :fretorno, :tipo, :asunto, :origen, :destino, :vehiculo, 'En Tránsito', :creado_por)");
        $ruta->execute([
            ':num' => $num_registro,
            ':fsalida' => $datos['fecha_salida'],
            ':fretorno' => $datos['fecha_retorno'],
            ':tipo' => $datos['tipo_viaje'],
            ':asunto' => trim(strtoupper($datos['asunto'])),
            ':origen' => trim(strtoupper($datos['origen'])),
            ':destino' => trim(strtoupper($datos['destino'])),
            ':vehiculo' => $datos['vehiculo'],
            ':creado_por' => $datos['creado_por'] // <--- Aquí guardamos quién lo hizo
        ]);
        
        $id_ruta = $conexion->lastInsertId();

        // Preparar consultas que usaremos repetidas veces
        $asignarPersonal = $conexion->prepare("INSERT INTO asignacion_personal_armas (id_ruta, id_usuario, rol_en_ruta, id_arma, fecha_hora_entrega) VALUES (?, ?, ?, ?, NOW())");
        $updateArma = $conexion->prepare("UPDATE armamento SET estado = 'Entregada' WHERE id_arma = ?");

        // B. Asignar CHOFER y su arma
        if(!empty($datos['chofer'])) {
            $arma_chofer = !empty($datos['arma_chofer']) ? $datos['arma_chofer'] : null;
            $asignarPersonal->execute([$id_ruta, $datos['chofer'], 'LÍDER/CHOFER', $arma_chofer]);

            if($arma_chofer) {
                $updateArma->execute([$arma_chofer]);
            }
        }

        // C. Asignar TODOS LOS CUSTODIOS EXTRAS (1, 2, 3, 4...)
        if(!empty($datos['custodios_array'])) {
            $numeroCustodio = 1;
            foreach($datos['custodios_array'] as $custodio) {
                $rol_nombre = "CUSTODIO " . $numeroCustodio;
                $arma_id = !empty($custodio['id_arma']) ? $custodio['id_arma'] : null;

                // Asignamos al custodio a la ruta
                $asignarPersonal->execute([$id_ruta, $custodio['id_usuario'], $rol_nombre, $arma_id]);

                // Si se llevó arma, cambiar su estado a "Entregada"
                if($arma_id) {
                    $updateArma->execute([$arma_id]);
                }
                $numeroCustodio++;
            }
        }

        // D. Cambiar el estado del vehículo a "En Ruta"
        if(!empty($datos['vehiculo'])) {
            $conexion->prepare("UPDATE vehiculos SET estado = 'En Ruta' WHERE id_vehiculo = ?")->execute([$datos['vehiculo']]);
        }

        echo json_encode(["status" => "success", "mensaje" => "Salvoconducto $num_registro generado correctamente.", "numero_registro" => $num_registro]);
    }

} catch(PDOException $e) {
    echo json_encode(["status" => "error", "mensaje" => "Error de BD: " . $e->getMessage()]);
}
?>
