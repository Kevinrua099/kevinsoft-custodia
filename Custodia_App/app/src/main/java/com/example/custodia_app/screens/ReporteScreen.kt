package com.example.custodia_app.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.custodia_app.network.RetrofitClient
import java.util.UUID

enum class EstadoOperacion {
    REGISTRO_INICIAL,
    VIAJE_EN_CURSO,
    ESPERA_CONFIRMACION
}

// Estructuras de datos para guardar lo que escribes dinámicamente
data class PiscinaData(val id: String = UUID.randomUUID().toString(), var numero: String = "", var libras: String = "")
data class PlataformaData(val id: String = UUID.randomUUID().toString(), var nombre: String = "", var piscinas: List<PiscinaData> = listOf(PiscinaData()))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReporteScreen(navController: NavController) {
    var estadoPantalla by remember { mutableStateOf(EstadoOperacion.REGISTRO_INICIAL) }
    val context = LocalContext.current

    // Variables de estado para los campos de texto
    var origenTexto by remember { mutableStateOf("") }

    // Lista reactiva para generar plataformas y piscinas infinitas
    var listaPlataformas by remember { mutableStateOf(listOf(PlataformaData(nombre = "Plataforma 1"))) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Operación Táctica", color = Color.White, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF3B5998))
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {

            when (estadoPantalla) {

                EstadoOperacion.REGISTRO_INICIAL -> {
                    // --- ETAPA 1: REGISTRO INICIAL DE CARGA ---
                    Text(
                        text = "Paso 1: Registro Inicial de Carga",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF3B5998),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // CAMPO: ORIGEN
                    OutlinedTextField(
                        value = origenTexto,
                        onValueChange = { origenTexto = it },
                        label = { Text("Origen (Ej: Muelle Pesalamar)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // BUCLE QUE DIBUJA LAS PLATAFORMAS DINÁMICAMENTE
                    listaPlataformas.forEachIndexed { pIndex, plataforma ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8EAF6)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {

                                // CAMPO: NOMBRE PLATAFORMA
                                OutlinedTextField(
                                    value = plataforma.nombre,
                                    onValueChange = { nuevoTexto ->
                                        val listaActualizada = listaPlataformas.toMutableList()
                                        listaActualizada[pIndex] = plataforma.copy(nombre = nuevoTexto)
                                        listaPlataformas = listaActualizada
                                    },
                                    label = { Text("Plataforma/Guía") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                Button(
                                    onClick = { Toast.makeText(context, "Abriendo cámara...", Toast.LENGTH_SHORT).show() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A6595)),
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("📸 TOMAR FOTO GUÍA", color = Color.White) }

                                Spacer(modifier = Modifier.height(12.dp))

                                // BUCLE QUE DIBUJA LAS PISCINAS DE ESTA PLATAFORMA
                                plataforma.piscinas.forEachIndexed { pscIndex, piscina ->
                                    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {

                                        OutlinedTextField(
                                            value = piscina.numero,
                                            onValueChange = { nuevoNum ->
                                                val piscinasActualizadas = plataforma.piscinas.toMutableList()
                                                piscinasActualizadas[pscIndex] = piscina.copy(numero = nuevoNum)
                                                val listaActualizada = listaPlataformas.toMutableList()
                                                listaActualizada[pIndex] = plataforma.copy(piscinas = piscinasActualizadas)
                                                listaPlataformas = listaActualizada
                                            },
                                            label = { Text("Piscina") },
                                            modifier = Modifier.weight(1f)
                                        )

                                        OutlinedTextField(
                                            value = piscina.libras,
                                            onValueChange = { nuevasLibras ->
                                                val piscinasActualizadas = plataforma.piscinas.toMutableList()
                                                piscinasActualizadas[pscIndex] = piscina.copy(libras = nuevasLibras)
                                                val listaActualizada = listaPlataformas.toMutableList()
                                                listaActualizada[pIndex] = plataforma.copy(piscinas = piscinasActualizadas)
                                                listaPlataformas = listaActualizada
                                            },
                                            label = { Text("Libras") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // BOTÓN: AÑADIR PISCINA DENTRO DE ESTA PLATAFORMA
                                Button(
                                    onClick = {
                                        val piscinasActualizadas = plataforma.piscinas.toMutableList()
                                        piscinasActualizadas.add(PiscinaData())
                                        val listaActualizada = listaPlataformas.toMutableList()
                                        listaActualizada[pIndex] = plataforma.copy(piscinas = piscinasActualizadas)
                                        listaPlataformas = listaActualizada
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD1D5DF)),
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("➕ AÑADIR PISCINA", color = Color.DarkGray, fontWeight = FontWeight.Bold) }
                            }
                        }
                    }

                    // BOTÓN: AGREGAR OTRA PLATAFORMA NUEVA
                    Button(
                        onClick = {
                            val listaActualizada = listaPlataformas.toMutableList()
                            listaActualizada.add(PlataformaData(nombre = "Plataforma ${listaActualizada.size + 1}"))
                            listaPlataformas = listaActualizada
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7D5A7A)),
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) { Text("➕ AGREGAR OTRA PLATAFORMA", color = Color.White) }

                    Spacer(modifier = Modifier.height(16.dp))

                    // BOTÓN: INICIAR RUTA
                    Button(
                        onClick = {
                            // Opcional: Aquí enviarías 'origenTexto' y 'listaPlataformas' a tu API
                            estadoPantalla = EstadoOperacion.VIAJE_EN_CURSO
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A6595)),
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) { Text("INICIAR RUTA Y ENVIAR DATOS", color = Color.White, fontWeight = FontWeight.Bold) }
                }

                EstadoOperacion.VIAJE_EN_CURSO -> {
                    // --- ETAPA 2: VIAJE EN CURSO ---
                    Spacer(modifier = Modifier.height(40.dp))
                    Text("🚚 VIAJE EN CURSO", fontWeight = FontWeight.Bold, fontSize = 24.sp, color = Color(0xFF2C3E50))
                    Spacer(modifier = Modifier.height(30.dp))

                    Button(
                        onClick = { Toast.makeText(context, "Abrir formulario Novedad...", Toast.LENGTH_SHORT).show() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC0392B)),
                        modifier = Modifier.fillMaxWidth(0.85f).height(60.dp), shape = MaterialTheme.shapes.medium
                    ) { Text("🚨 REPORTAR NOVEDAD", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { Toast.makeText(context, "Abrir formulario Pesca...", Toast.LENGTH_SHORT).show() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2980B9)),
                        modifier = Modifier.fillMaxWidth(0.85f).height(60.dp), shape = MaterialTheme.shapes.medium
                    ) { Text("🐟 REPORTAR PESCA", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { estadoPantalla = EstadoOperacion.ESPERA_CONFIRMACION },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7D5A7A)),
                        modifier = Modifier.fillMaxWidth(0.85f).height(60.dp), shape = MaterialTheme.shapes.medium
                    ) { Text("🏁 LLEGADA A PLANTA (ARRIBO)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
                }

                EstadoOperacion.ESPERA_CONFIRMACION -> {
                    // --- ETAPA 3: ESPERANDO CONFIRMACIÓN ---
                    Spacer(modifier = Modifier.height(40.dp))

                    LaunchedEffect(Unit) {
                        while (true) {
                            kotlinx.coroutines.delay(3000)
                            try {
                                val numeroRuta = "ORD-2026-1792104421" // Ojo: Reemplaza por tu variable real
                                val respuesta = RetrofitClient.instance.verificarEstado(numeroRuta)

                                if (respuesta.status == "success") {
                                    val estadoEnBaseDeDatos = respuesta.estado_ruta
                                    if (estadoEnBaseDeDatos == "Finalizada") {
                                        // CUANDO LA WEB APRUEBA: Limpia los campos y regresa al inicio
                                        Toast.makeText(context, "✅ La consola te ha dado de ALTA.", Toast.LENGTH_LONG).show()
                                        origenTexto = ""
                                        listaPlataformas = listOf(PlataformaData(nombre = "Plataforma 1"))
                                        estadoPantalla = EstadoOperacion.REGISTRO_INICIAL
                                        break
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("API_ERROR", "Error: ${e.message}")
                            }
                        }
                    }

                    Text("⏳ ESPERANDO CONFIRMACIÓN", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = Color(0xFFF39C12))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("El arribo ha sido notificado exitosamente.\nEsperando que la Consola Central dé de alta la ruta.", color = Color.DarkGray, textAlign = TextAlign.Center, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(40.dp))

                    Button(
                        onClick = {
                            Toast.makeText(context, "✅ La consola te ha dado de ALTA.", Toast.LENGTH_LONG).show()
                            origenTexto = ""
                            listaPlataformas = listOf(PlataformaData(nombre = "Plataforma 1"))
                            estadoPantalla = EstadoOperacion.REGISTRO_INICIAL
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF27AE60)),
                        modifier = Modifier.fillMaxWidth(0.85f).height(60.dp), shape = MaterialTheme.shapes.medium
                    ) { Text("✅ DAR DE BAJA (NUEVA RUTA)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
                }
            }
        }
    }
}
