<?php
// La recepción más reciente que no ha sido reemplazada ni entregada para esa placa.
function patrullaActual(PDO $conexion, int $usuario): ?array {
    $consulta = $conexion->prepare("SELECT i.placa, i.disco, i.id_inspeccion
        FROM inspeccion_vehiculos i
        WHERE i.id_usuario = ? AND i.tipo_inspeccion = 'Recepción'
          AND i.id_inspeccion = (SELECT MAX(recepcion.id_inspeccion) FROM inspeccion_vehiculos recepcion WHERE recepcion.id_usuario = i.id_usuario AND recepcion.tipo_inspeccion = 'Recepción')
          AND i.id_inspeccion = (SELECT MAX(ultima.id_inspeccion) FROM inspeccion_vehiculos ultima WHERE ultima.placa = i.placa)
        ORDER BY i.id_inspeccion DESC LIMIT 1");
    $consulta->execute([$usuario]);
    $patrulla = $consulta->fetch(PDO::FETCH_ASSOC);
    return $patrulla ?: null;
}
