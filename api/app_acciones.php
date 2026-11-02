<?php
header('Content-Type: application/json; charset=utf-8');
require_once __DIR__ . '/conexion.php';

try {
    $datos = json_decode(file_get_contents('php://input'), true) ?: [];
    $numero = trim($datos['numero_registro'] ?? '');
    $usuario = filter_var($datos['usuario_id'] ?? null, FILTER_VALIDATE_INT, ['options' => ['min_range' => 1]]);
    $accion = $datos['accion'] ?? '';
    $observacion = trim($datos['observacion'] ?? '');
    if (!$usuario || $numero === '' || !in_array($accion, ['novedad', 'arribo', 'verificar'], true) || ($accion === 'novedad' && $observacion === '')) {
        echo json_encode(['status' => 'error', 'mensaje' => 'Selecciona una ruta asignada y proporciona los datos de la acción.']);
        exit;
    }
    $conexion = conectarBD();
    $conexion->beginTransaction();
    $consulta = $conexion->prepare('SELECT s.id_ruta, s.estado_ruta FROM salvoconductos_rutas s WHERE s.numero_registro = ? AND EXISTS (SELECT 1 FROM asignacion_personal_armas a WHERE a.id_ruta = s.id_ruta AND a.id_usuario = ?) FOR UPDATE');
    $consulta->execute([$numero, $usuario]);
    $ruta = $consulta->fetch(PDO::FETCH_ASSOC);
    if (!$ruta || ($accion !== 'verificar' && !in_array($ruta['estado_ruta'], ['Activa', 'En Tránsito'], true))) {
        $conexion->rollBack();
        echo json_encode(['status' => 'error', 'mensaje' => 'La ruta no está asignada o ya no acepta reportes.']);
        exit;
    }
    if ($accion !== 'verificar') {
        $tipo = $accion === 'novedad' ? 'Novedad en Vía' : 'Arribo a Destino';
        $origen = $accion === 'novedad' ? 'EN RUTA' : 'PLANTA';
        if ($accion === 'arribo' && $observacion === '') $observacion = 'El custodio notificó el arribo a su destino.';
        $conexion->prepare('INSERT INTO reportes_pesca (id_ruta, fecha_hora_reporte, origen_camaronera, observaciones, tipo_reporte) VALUES (?, NOW(), ?, ?, ?)')->execute([$ruta['id_ruta'], $origen, $observacion, $tipo]);
        if ($accion === 'arribo') $conexion->prepare("UPDATE salvoconductos_rutas SET estado_ruta = 'En espera de confirmación' WHERE id_ruta = ?")->execute([$ruta['id_ruta']]);
    }
    $conexion->commit();
    echo json_encode(['status' => 'success', 'mensaje' => 'Acción registrada.', 'estado_ruta' => $accion === 'arribo' ? 'En espera de confirmación' : $ruta['estado_ruta']]);
} catch (Throwable $e) {
    if (isset($conexion) && $conexion->inTransaction()) $conexion->rollBack();
    error_log('Error en acción de ruta: ' . $e->getMessage());
    http_response_code(500);
    echo json_encode(['status' => 'error', 'mensaje' => 'No se pudo procesar la acción.']);
}
