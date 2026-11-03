package com.example.custodia_app.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.custodia_app.network.RetrofitClient
import com.example.custodia_app.network.Ruta
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.UUID


private data class PiscinaData(val numero: String = "", val libras: String = "")
private data class PlataformaData(
    val id: String = UUID.randomUUID().toString(),
    val nombre: String = "",
    val piscinas: List<PiscinaData> = listOf(PiscinaData()),
    val foto: String? = null
)

private val plataformasSaver = Saver<List<PlataformaData>, String>(
    save = { Gson().toJson(it) },
    restore = { Gson().fromJson(it, object : TypeToken<List<PlataformaData>>() {}.type) }
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReporteScreen(navController: NavController) {
    val context = LocalContext.current
    val usuarioId = context.getSharedPreferences("CustodiaPrefs", 0).getInt("usuarioId", 0)
    val scope = rememberCoroutineScope()
    var rutas by remember { mutableStateOf<List<Ruta>>(emptyList()) }
    var seleccion by rememberSaveable { mutableStateOf<String?>(null) }
    var tipo by rememberSaveable { mutableStateOf<String?>(null) }
    var origen by rememberSaveable { mutableStateOf("") }
    var destino by rememberSaveable { mutableStateOf("") }
    var numeroContenedor by rememberSaveable { mutableStateOf("") }
    var observaciones by rememberSaveable { mutableStateOf("") }
    var plataformas by rememberSaveable(stateSaver = plataformasSaver) { mutableStateOf(listOf(PlataformaData())) }
    var fotoGeneral by rememberSaveable { mutableStateOf<String?>(null) }
    var fotoDestino by rememberSaveable { mutableStateOf<String?>(null) }
    var cargando by remember { mutableStateOf(false) }
    var enviando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val tomarFoto = rememberCameraCapture { uri ->
        if (fotoDestino == "general") {
            fotoGeneral?.let { deleteCameraPhoto(context, it) }
            fotoGeneral = uri
        } else {
            plataformas = plataformas.map { plataforma ->
                if (plataforma.id == fotoDestino) {
                    plataforma.foto?.let { deleteCameraPhoto(context, it) }
                    plataforma.copy(foto = uri)
                } else plataforma
            }
        }
        fotoDestino = null
    }

    fun cargarRutas() {
        if (usuarioId <= 0 || cargando) return
        cargando = true
        scope.launch {
            try {
                val respuesta = RetrofitClient.instance.listarRutas(usuarioId)
                if (respuesta.status == "success" && respuesta.data != null) {
                    rutas = respuesta.data
                    error = null
                } else error = respuesta.mensaje ?: "No se pudieron consultar las rutas."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = "No se pudieron actualizar las rutas. Comprueba la conexión y vuelve a intentar."
            } finally { cargando = false }
        }
    }

    fun limpiarFormulario() {
        plataformas.mapNotNull { it.foto }.forEach { deleteCameraPhoto(context, it) }
        fotoGeneral?.let { deleteCameraPhoto(context, it) }
        plataformas = listOf(PlataformaData())
        fotoGeneral = null
        observaciones = ""
        origen = ""
        destino = ""
        numeroContenedor = ""
        tipo = null
    }

    BackHandler(enabled = enviando || tipo != null || seleccion != null) {
        if (!enviando) {
            if (tipo != null) limpiarFormulario() else seleccion = null
        }
    }

    LaunchedEffect(usuarioId) { cargarRutas() }
    LaunchedEffect(seleccion) {
        while (seleccion != null) {
            delay(15000)
            cargarRutas()
        }
    }

    val ruta = rutas.find { it.numero == seleccion }
    val activa = ruta?.estado in listOf("Activa", "En Tránsito")

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Custodia en ruta") },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF3B5998), titleContentColor = Color.White, navigationIconContentColor = Color.White),
            navigationIcon = { TextButton(onClick = {
                when {
                    tipo != null -> limpiarFormulario()
                    seleccion != null -> seleccion = null
                    else -> navController.popBackStack()
                }
            }, enabled = !enviando) { Text("Volver", color = Color.White) } }
        )
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (usuarioId <= 0) {
                Text("El servidor aún no entrega el identificador del usuario. Actualiza la API e inicia sesión de nuevo.", color = MaterialTheme.colorScheme.error)
            } else if (seleccion == null) {
                Text("Mis rutas asignadas", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Las rutas se asignan desde Salvoconductos en la página web.")
                OutlinedButton(onClick = { cargarRutas() }, enabled = !cargando) { Text(if (cargando) "Actualizando..." else "Actualizar rutas") }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (!cargando && error == null && rutas.isEmpty()) Text("No tienes rutas asignadas todavía.")
                rutas.forEach { item ->
                    OutlinedCard(onClick = { seleccion = item.numero }, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text(item.numero, fontWeight = FontWeight.Bold)
                            Text("${item.origen ?: "Sin origen"} → ${item.destino ?: "Sin destino"}")
                            Text("Tipo: ${item.tipoViaje ?: "TERRESTRE"}")

                            // Lógica mejorada para detectar patrulla en ambas estructuras
                            val discoPatrulla = item.patrullaRecibida?.disco ?: item.disco
                            val placaPatrulla = item.patrullaRecibida?.placa ?: item.placa
                            val tienePatrulla = !discoPatrulla.isNullOrBlank() || !placaPatrulla.isNullOrBlank()

                            Text(if (tienePatrulla) "Mi patrulla: ${discoPatrulla ?: "S/N"} · ${placaPatrulla ?: "S/N"}" else "Sin recepción de patrulla registrada")
                            Text("Estado: ${item.estado}")
                        }
                    }
                }
            } else if (ruta == null) {
                Text(if (cargando) "Cargando ruta..." else "No se encontró la ruta asignada.")
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                OutlinedButton(onClick = { cargarRutas() }, enabled = !cargando) { Text("Reintentar") }
            } else {
                Text(ruta.numero, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("${ruta.origen ?: "Sin origen"} → ${ruta.destino ?: "Sin destino"}")
                Text("Estado: ${ruta.estado}", fontWeight = FontWeight.SemiBold)
                val esContenedor = ruta.tipoViaje?.trim()?.equals("CONTENEDORES", ignoreCase = true) == true

                // Lógica de Patrulla corregida
                val discoPatrulla = ruta.patrullaRecibida?.disco ?: ruta.disco
                val placaPatrulla = ruta.patrullaRecibida?.placa ?: ruta.placa
                val tienePatrulla = !discoPatrulla.isNullOrBlank() || !placaPatrulla.isNullOrBlank()

                if (tienePatrulla) {
                    Text("Patrulla recibida: disco ${discoPatrulla ?: "S/N"} · placa ${placaPatrulla ?: "S/N"}", fontWeight = FontWeight.SemiBold, color = Color(0xFF2C3E50))
                } else {
                    Text("No tienes una recepción de patrulla vigente. Regístrala en Control de patrullas para incluirla en tus reportes.", color = MaterialTheme.colorScheme.error)
                }

                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

                if (tipo == null) {
                    OutlinedButton(onClick = { cargarRutas() }, enabled = !cargando) { Text(if (cargando) "Actualizando..." else "Actualizar estado") }
                    if (activa) {
                        listOf("Inicio de Ruta", "Pesca", "Novedad en Vía", "Arribo a Destino").forEach { opcion ->
                            Button(
                                onClick = {
                                    origen = if (opcion == "Arribo a Destino") ruta.destino.orEmpty() else ruta.origen.orEmpty();
                                    destino = ruta.destino.orEmpty();
                                    tipo = opcion
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = when(opcion) {
                                        "Inicio de Ruta" -> Color(0xFF27AE60)
                                        "Pesca" -> Color(0xFF2980B9)
                                        "Novedad en Vía" -> Color(0xFFC0392B)
                                        else -> Color(0xFF7D5A7A)
                                    }
                                )
                            ) {
                                Text(if (opcion == "Pesca") { if (esContenedor) "REPORTAR CONTENEDOR" else "REPORTAR PLATAFORMAS" } else opcion.uppercase(), fontWeight = FontWeight.Bold)
                            }
                        }
                    } else if (ruta.estado == "En espera de confirmación") {
                        Text("El arribo fue registrado. Espera la confirmación desde la página web.", color = Color(0xFFF39C12), fontWeight = FontWeight.Bold)
                    } else Text("Esta ruta ya está finalizada.", color = Color.Gray, fontWeight = FontWeight.Bold)
                } else {
                    val tipoActual = tipo!!
                    val esPesca = tipoActual == "Pesca"
                    val esInicio = tipoActual == "Inicio de Ruta"

                    Text(
                        text = if (esPesca) { if (esContenedor) "Guía del contenedor" else "Reporte de plataformas" }
                        else if (esInicio) "Registro de Inicio de Ruta"
                        else tipoActual,
                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = origen, onValueChange = { if (it.length <= 100) origen = it },
                        label = { Text(if (esPesca && esContenedor) "Desde / origen *" else "Ubicación / origen") },
                        modifier = Modifier.fillMaxWidth(), enabled = !enviando
                    )

                    if ((esPesca || esInicio) && esContenedor) {
                        OutlinedTextField(value = destino, onValueChange = { if (it.length <= 100) destino = it }, label = { Text("Hasta / destino *") }, modifier = Modifier.fillMaxWidth(), enabled = !enviando)
                        OutlinedTextField(value = numeroContenedor, onValueChange = { numeroContenedor = it.take(50) }, label = { Text("Número de contenedor o guía (opcional)") }, modifier = Modifier.fillMaxWidth(), enabled = !enviando)
                    }

                    OutlinedTextField(
                        value = observaciones, onValueChange = { observaciones = it },
                        label = { Text(if (tipoActual == "Novedad en Vía") "Describe la novedad *" else "Observaciones") },
                        modifier = Modifier.fillMaxWidth(), enabled = !enviando
                    )

                    // AQUI ESTÁ LA CLAVE: Solo muestra plataformas si es PESCA
                    if (esPesca && !esContenedor) {
                        Text("Plataformas: ${plataformas.size}", fontWeight = FontWeight.Bold)
                        Text("Agrega cada plataforma y sus piscinas. Por ejemplo, 2.000 lb de una piscina y 1.000 lb de otra suman 3.000 lb.", style = MaterialTheme.typography.bodySmall)
                        plataformas.forEachIndexed { index, plataforma ->
                            OutlinedCard(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Plataforma ${index + 1}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    OutlinedTextField(value = plataforma.nombre, onValueChange = { valor -> plataformas = plataformas.map { if (it.id == plataforma.id) it.copy(nombre = valor.take(50)) else it } }, label = { Text("Identificación de plataforma o guía (opcional)") }, modifier = Modifier.fillMaxWidth(), enabled = !enviando)
                                    plataforma.foto?.let { uri -> AsyncImage(model = Uri.parse(uri), contentDescription = "Foto de la guía", modifier = Modifier.size(120.dp)) }
                                    OutlinedButton(onClick = { fotoDestino = plataforma.id; tomarFoto() }, enabled = !enviando) { Text(if (plataforma.foto == null) "Tomar foto de guía" else "Cambiar foto de guía") }
                                    if (plataforma.foto != null) TextButton(onClick = { deleteCameraPhoto(context, plataforma.foto); plataformas = plataformas.map { if (it.id == plataforma.id) it.copy(foto = null) else it } }, enabled = !enviando) { Text("Quitar foto") }
                                    plataforma.piscinas.forEachIndexed { piscinaIndex, piscina ->
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedTextField(value = piscina.numero, onValueChange = { valor ->
                                                val piscinas = plataforma.piscinas.toMutableList(); piscinas[piscinaIndex] = piscina.copy(numero = valor.take(50))
                                                plataformas = plataformas.map { if (it.id == plataforma.id) it.copy(piscinas = piscinas) else it }
                                            }, label = { Text("Piscina *") }, modifier = Modifier.weight(1f), enabled = !enviando)
                                            OutlinedTextField(value = piscina.libras, onValueChange = { valor ->
                                                val piscinas = plataforma.piscinas.toMutableList(); piscinas[piscinaIndex] = piscina.copy(libras = valor.take(12))
                                                plataformas = plataformas.map { if (it.id == plataforma.id) it.copy(piscinas = piscinas) else it }
                                            }, label = { Text("Libras *") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f), enabled = !enviando)
                                        }
                                        if (plataforma.piscinas.size > 1) TextButton(onClick = { plataformas = plataformas.map { if (it.id == plataforma.id) it.copy(piscinas = it.piscinas.filterIndexed { i, _ -> i != piscinaIndex }) else it } }, enabled = !enviando) { Text("Quitar piscina") }
                                    }
                                    Text("Total plataforma: ${String.format(java.util.Locale.US, "%.2f", plataforma.piscinas.sumOf { it.libras.replace(',', '.').toDoubleOrNull() ?: 0.0 })} lb", fontWeight = FontWeight.Bold)
                                    TextButton(onClick = { plataformas = plataformas.map { if (it.id == plataforma.id) it.copy(piscinas = it.piscinas + PiscinaData()) else it } }, enabled = !enviando && plataforma.piscinas.size < 50) { Text("Agregar piscina") }
                                    if (plataformas.size > 1) TextButton(onClick = { plataforma.foto?.let { deleteCameraPhoto(context, it) }; plataformas = plataformas.filterIndexed { i, _ -> i != index } }, enabled = !enviando) { Text("Quitar plataforma") }
                                }
                            }
                        }
                        OutlinedButton(onClick = { plataformas = plataformas + PlataformaData() }, enabled = !enviando && plataformas.size < 8) { Text("Agregar plataforma") }
                        if (plataformas.size == 8) Text("Máximo 8 plataformas por reporte. Puedes enviar otro reporte para las restantes.", style = MaterialTheme.typography.bodySmall)
                        Text("Total de carga: ${String.format(java.util.Locale.US, "%.2f", plataformas.sumOf { plataforma -> plataforma.piscinas.sumOf { it.libras.replace(',', '.').toDoubleOrNull() ?: 0.0 } })} lb", fontWeight = FontWeight.Bold)
                    } else {
                        // Para Inicio, Novedad, Arribo o Contenedor: Solo una foto general
                        fotoGeneral?.let { AsyncImage(model = Uri.parse(it), contentDescription = if (esPesca) "Foto de la guía del contenedor" else "Foto de evidencia", modifier = Modifier.size(120.dp)) }
                        OutlinedButton(onClick = { fotoDestino = "general"; tomarFoto() }, enabled = !enviando) { Text(if (esPesca) { if (fotoGeneral == null) "Tomar foto de la guía *" else "Cambiar foto de la guía" } else if (fotoGeneral == null) "Añadir foto" else "Cambiar foto") }
                        fotoGeneral?.let { uri -> TextButton(onClick = { deleteCameraPhoto(context, uri); fotoGeneral = null }, enabled = !enviando) { Text("Quitar foto") } }
                    }

                    Button(onClick = {
                        val errorFormulario = when {
                            !activa -> "La ruta ya no acepta reportes. Actualiza su estado."
                            tipoActual == "Novedad en Vía" && observaciones.isBlank() -> "Describe la novedad."
                            (esPesca || esInicio) && esContenedor && (origen.isBlank() || destino.isBlank()) -> "Indica desde dónde y hasta dónde se mueve el contenedor."
                            (esPesca || esInicio) && esContenedor && fotoGeneral == null -> "Adjunta la foto de la guía del contenedor."
                            esPesca && !esContenedor && plataformas.any { it.foto == null } -> "Adjunta la foto de la guía de cada plataforma."
                            esPesca && !esContenedor && plataformas.flatMap { it.piscinas }.any { it.numero.isBlank() } -> "Indica el número de cada piscina."
                            esPesca && !esContenedor && plataformas.flatMap { it.piscinas }.any { piscina -> val valor = piscina.libras.trim().replace(',', '.'); !Regex("^\\d{1,8}(?:\\.\\d{1,2})?$").matches(valor) || (valor.toDoubleOrNull() ?: 0.0) <= 0.0 } -> "Indica libras mayores que cero, con hasta dos decimales."
                            else -> null
                        }
                        if (errorFormulario != null) {
                            Toast.makeText(context, errorFormulario, Toast.LENGTH_LONG).show()
                        } else {
                            enviando = true
                            scope.launch {
                                try {
                                    val fotos = if (esPesca && !esContenedor) plataformas.mapIndexedNotNull { i, p -> p.foto?.let { "foto_$i" to it } } else listOfNotNull(fotoGeneral?.let { "foto_0" to it })
                                    val datos = when {
                                        (esPesca || esInicio) && esContenedor -> listOf(mapOf("identificador" to numeroContenedor.trim().ifEmpty { "Contenedor" }, "foto_campo" to "foto_0", "piscinas" to emptyList<Any>()))
                                        esPesca -> plataformas.mapIndexed { i, p -> mapOf("identificador" to p.nombre.trim().ifEmpty { "Plataforma ${i + 1}" }, "foto_campo" to "foto_$i", "piscinas" to p.piscinas.map { mapOf("numero" to it.numero.trim(), "libraje" to it.libras.trim().replace(',', '.')) }) }
                                        else -> emptyList()
                                    }
                                    val partes = withContext(Dispatchers.IO) { photoParts(context, fotos) }
                                    fun texto(valor: String) = valor.toRequestBody("text/plain; charset=utf-8".toMediaType())
                                    val respuesta = RetrofitClient.instance.enviarReporte(texto(ruta.numero), texto(usuarioId.toString()), texto(origen.trim()), texto(destino.trim()), texto(Gson().toJson(datos)), texto(observaciones.trim()), texto(tipoActual), partes)
                                    Toast.makeText(context, respuesta.mensaje, Toast.LENGTH_LONG).show()
                                    if (respuesta.status == "success") { limpiarFormulario(); cargarRutas() }
                                } catch (e: CancellationException) { throw e
                                } catch (e: Exception) {
                                    Toast.makeText(context, "No se pudo enviar el reporte. Revisa la conexión.", Toast.LENGTH_LONG).show()
                                } finally { enviando = false }
                            }
                        }
                    }, enabled = !enviando && activa, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B5998))) {
                        Text(if (enviando) "ENVIANDO..." else "ENVIAR REPORTE", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}