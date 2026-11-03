package com.example.custodia_app.screens

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovimientoInternoScreen(navController: NavController) {
    val context = LocalContext.current
    val usuarioId = context.getSharedPreferences("CustodiaPrefs", 0).getInt("usuarioId", 0)

    var tipoMovimiento by rememberSaveable { mutableStateOf("Traslado de Ida") }
    var origen by rememberSaveable { mutableStateOf("") }
    var destino by rememberSaveable { mutableStateOf("") }
    var numeroContenedor by rememberSaveable { mutableStateOf("") }
    var observaciones by rememberSaveable { mutableStateOf("") }
    var fotoEvidencia by rememberSaveable { mutableStateOf<String?>(null) }
    var enviando by remember { mutableStateOf(false) }

    val tomarFoto = rememberCameraCapture { uri ->
        fotoEvidencia?.let { deleteCameraPhoto(context, it) }
        fotoEvidencia = uri
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Movimiento de Contenedores") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF2C3E50),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                ),
                navigationIcon = {
                    TextButton(onClick = { navController.popBackStack() }, enabled = !enviando) {
                        Text("Volver", color = Color.White)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Auditoría de Movimientos",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Registra traslados de contenedores, retornos o movimientos entre patios que no requieran una ruta formal.",
                color = Color.Gray,
                style = MaterialTheme.typography.bodyMedium
            )

            // Selector de Tipo de Movimiento
            Text("Tipo de Movimiento *", fontWeight = FontWeight.Bold)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Traslado de Ida", "Traslado de Retorno").forEach { opcion ->
                    FilterChip(
                        selected = tipoMovimiento == opcion,
                        onClick = { tipoMovimiento = opcion },
                        label = { Text(opcion) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Campos del formulario
            OutlinedTextField(
                value = origen,
                onValueChange = { origen = it },
                label = { Text("Desde / Origen *") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !enviando
            )

            OutlinedTextField(
                value = destino,
                onValueChange = { destino = it },
                label = { Text("Hasta / Destino *") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !enviando
            )

            OutlinedTextField(
                value = numeroContenedor,
                onValueChange = { numeroContenedor = it },
                label = { Text("N° de Contenedor o Guía *") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !enviando
            )

            OutlinedTextField(
                value = observaciones,
                onValueChange = { observaciones = it },
                label = { Text("Observaciones (opcional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                enabled = !enviando
            )

            // Sección de Fotografía
            Text("Evidencia Fotográfica *", fontWeight = FontWeight.Bold)
            if (fotoEvidencia != null) {
                AsyncImage(
                    model = Uri.parse(fotoEvidencia),
                    contentDescription = "Evidencia",
                    modifier = Modifier.fillMaxWidth().height(200.dp)
                )
                TextButton(onClick = {
                    deleteCameraPhoto(context, fotoEvidencia!!)
                    fotoEvidencia = null
                }, enabled = !enviando) { Text("Quitar foto", color = MaterialTheme.colorScheme.error) }
            } else {
                OutlinedButton(
                    onClick = { tomarFoto() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !enviando
                ) {
                    Text("Tomar Foto del Contenedor/Guía")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Botón Enviar
            Button(
                onClick = {
                    val errorFormulario = when {
                        origen.isBlank() -> "Debes indicar el origen."
                        destino.isBlank() -> "Debes indicar el destino."
                        numeroContenedor.isBlank() -> "Ingresa el número de contenedor."
                        fotoEvidencia == null -> "Se requiere una foto de evidencia."
                        else -> null
                    }

                    if (errorFormulario != null) {
                        Toast.makeText(context, errorFormulario, Toast.LENGTH_LONG).show()
                    } else {
                        enviando = true
                        /*
                         * AQUÍ IRÁ LA LÓGICA DE ENVÍO A RENDER.
                         * Se necesita un endpoint (ej: movimientos.php) en el backend.
                         */
                        Toast.makeText(context, "Módulo UI listo. Falta conectar endpoint.", Toast.LENGTH_LONG).show()
                        enviando = false
                    }
                },
                enabled = !enviando,
                modifier = Modifier.fillMaxWidth().height(55.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C3E50))
            ) {
                Text(if (enviando) "GUARDANDO..." else "REGISTRAR MOVIMIENTO", fontWeight = FontWeight.Bold)
            }
        }
    }
}