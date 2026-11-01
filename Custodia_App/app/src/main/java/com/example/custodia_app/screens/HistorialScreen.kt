package com.example.custodia_app.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorialScreen(navController: NavController) {

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mi Historial de Viajes", color = Color.White, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF3B5998))
            )
        }
    ) { paddingValues ->
        // Aquí luego conectarás Retrofit para descargar los datos reales.
        // Por ahora, pondremos datos visuales de ejemplo:
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(3) { index ->
                TarjetaHistorial(
                    fecha = "16 Oct 2026",
                    ruta = "ORD-2026-179210${index}421",
                    vehiculo = "Disco: 102 | Placa: GBA-1542",
                    origen = "Planta Songa",
                    destino = "Camaronera XYZ",
                    estado = if (index == 0) "En Tránsito" else "Finalizada"
                )
            }
        }
    }
}

@Composable
fun TarjetaHistorial(fecha: String, ruta: String, vehiculo: String, origen: String, destino: String, estado: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = ruta, fontWeight = FontWeight.Bold, color = Color(0xFF2C3E50), fontSize = 16.sp)
                Text(text = fecha, color = Color.Gray, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "\uD83D\uDE93 Vehículo: $vehiculo", color = Color.DarkGray, fontSize = 14.sp)
            Text(text = "\uD83D\uDCCD Origen: $origen", color = Color.DarkGray, fontSize = 14.sp)
            Text(text = "\uD83C\uDFC1 Destino: $destino", color = Color.DarkGray, fontSize = 14.sp)

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = estado,
                color = if (estado == "Finalizada") Color(0xFF27AE60) else Color(0xFFF39C12),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}