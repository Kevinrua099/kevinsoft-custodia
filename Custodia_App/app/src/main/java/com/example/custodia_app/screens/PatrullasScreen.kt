package com.example.custodia_app.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.custodia_app.network.InspeccionPatrulla
import com.example.custodia_app.network.RetrofitClient
import com.example.custodia_app.network.urlFotoPatrulla
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatrullasScreen(navController: NavController) {
    var inspecciones by remember { mutableStateOf<List<InspeccionPatrulla>>(emptyList()) }
    var buscando by rememberSaveable { mutableStateOf("") }
    var cargando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun cargarInspecciones() {
        if (cargando) return
        cargando = true
        error = null
        scope.launch {
            try {
                val respuesta = RetrofitClient.instance.listarInspecciones()
                if (respuesta.status == "success" && respuesta.data != null) {
                    inspecciones = respuesta.data
                } else {
                    error = respuesta.mensaje ?: "No se pudieron cargar las inspecciones."
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = "No se pudo conectar con el servidor. Comprueba tu conexión."
            } finally {
                cargando = false
            }
        }
    }

    LaunchedEffect(Unit) { cargarInspecciones() }
    val filtradas = remember(inspecciones, buscando) {
        val texto = buscando.trim()
        inspecciones.filter { inspeccion ->
            texto.isEmpty() || listOf(inspeccion.placa, inspeccion.disco, inspeccion.usuarioNombre)
                .any { it?.contains(texto, ignoreCase = true) == true }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Control de patrullas") },
                navigationIcon = { TextButton(onClick = { navController.popBackStack() }) { Text("Volver", color = MaterialTheme.colorScheme.onPrimary) } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Recepción de patrullas", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Registra y consulta las inspecciones más recientes.", modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))
                Button(onClick = { navController.navigate("inspeccion") }, modifier = Modifier.fillMaxWidth()) {
                    Text("＋ NUEVA INSPECCIÓN")
                }
            }
            item {
                OutlinedTextField(
                    value = buscando,
                    onValueChange = { buscando = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Buscar inspección") },
                    placeholder = { Text("Placa, disco o responsable") },
                    singleLine = true
                )
            }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Historial (${filtradas.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    TextButton(onClick = { cargarInspecciones() }, enabled = !cargando) { Text("Actualizar") }
                }
                Text("Hasta las 500 inspecciones más recientes.", style = MaterialTheme.typography.bodySmall)
            }
            if (cargando) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 3.dp)
                        Text("Cargando inspecciones...")
                    }
                }
            }
            if (error != null) {
                item {
                    Text(error!!, color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = { cargarInspecciones() }, enabled = !cargando) { Text("Reintentar") }
                }
            } else if (!cargando && filtradas.isEmpty()) {
                item { Text(if (buscando.isBlank()) "Aún no hay inspecciones registradas." else "No hay resultados para esa búsqueda.") }
            }
            items(filtradas, key = { it.id }) { inspeccion ->
                TarjetaPatrulla(inspeccion)
            }
        }
    }
}

@Composable
private fun TarjetaPatrulla(inspeccion: InspeccionPatrulla) {
    var expandida by rememberSaveable(inspeccion.id) { mutableStateOf(false) }
    var fotoSeleccionada by remember { mutableStateOf<String?>(null) }
    val observaciones = inspeccion.observaciones?.trim().orEmpty()
    val fotos = inspeccion.fotos.orEmpty().mapNotNull { urlFotoPatrulla(it) }

    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(inspeccion.placa?.takeIf { it.isNotBlank() } ?: "Sin placa", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Disco: ${inspeccion.disco?.takeIf { it.isNotBlank() } ?: "No indicado"}")
            Text("Recepción: ${fechaPatrulla(inspeccion.fechaRecepcion)}", style = MaterialTheme.typography.bodySmall)
            Text(
                if (observaciones.isEmpty()) "Sin observaciones" else "Con observaciones",
                color = if (observaciones.isEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 8.dp)
            )
            TextButton(onClick = { expandida = !expandida }) { Text(if (expandida) "Ocultar detalle" else "Ver detalle") }
            if (expandida) {
                HorizontalDivider()
                Text("Responsable: ${inspeccion.usuarioNombre?.takeIf { it.isNotBlank() } ?: "No indicado"}", modifier = Modifier.padding(top = 12.dp))
                Text("Kilometraje: ${inspeccion.kilometraje?.let { "$it km" } ?: "No indicado"}")
                Text("Novedades", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp))
                Text(observaciones.ifEmpty { "Sin observaciones registradas." })
                Text("Fotografías (${fotos.size})", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp, bottom = 8.dp))
                if (fotos.isEmpty()) {
                    Text("Sin fotos disponibles.")
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(fotos) { url ->
                            FotoMiniatura(url = url, onClick = { fotoSeleccionada = url })
                        }
                    }
                }
            }
        }
    }

    fotoSeleccionada?.let { url ->
        AlertDialog(
            onDismissRequest = { fotoSeleccionada = null },
            title = { Text("Fotografía de la inspección") },
            text = {
                AsyncImage(model = url, contentDescription = "Fotografía ampliada de la patrulla", modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp, max = 420.dp), contentScale = ContentScale.Fit)
            },
            confirmButton = { TextButton(onClick = { fotoSeleccionada = null }) { Text("Cerrar") } }
        )
    }
}

@Composable
private fun FotoMiniatura(url: String, onClick: () -> Unit) {
    var fallo by remember(url) { mutableStateOf(false) }
    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(110.dp).clickable(onClick = onClick)) {
        Box(contentAlignment = Alignment.Center) {
            if (fallo) {
                Text("Foto no disponible", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(8.dp))
            } else {
                AsyncImage(model = url, contentDescription = "Abrir foto de la patrulla", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop, onError = { fallo = true })
            }
        }
    }
}

private fun fechaPatrulla(valor: String?): String {
    if (valor.isNullOrBlank()) return "No indicada"
    return try {
        val formatoEntrada = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT).apply { isLenient = false }
        val fecha = formatoEntrada.parse(valor) ?: return valor
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.forLanguageTag("es-EC")).format(fecha)
    } catch (_: Exception) {
        valor
    }
}
