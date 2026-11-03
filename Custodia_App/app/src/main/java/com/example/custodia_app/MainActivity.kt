package com.example.custodia_app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.custodia_app.screens.InspeccionScreen
import com.example.custodia_app.screens.LoginScreen
import com.example.custodia_app.screens.MenuScreen
import com.example.custodia_app.screens.PatrullasScreen
import com.example.custodia_app.screens.ReporteScreen
import com.example.custodia_app.screens.MovimientoInternoScreen // <-- IMPORTA LA NUEVA PANTALLA
import com.example.custodia_app.ui.theme.Custodia_AppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Custodia_AppTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    val navController = rememberNavController()

                    NavHost(navController = navController, startDestination = "login") {
                        composable("login") { LoginScreen(navController) }
                        composable("menu") { MenuScreen(navController) }
                        composable("patrullas") { PatrullasScreen(navController) }
                        composable("inspeccion") { InspeccionScreen(navController) }
                        composable("reporte") { ReporteScreen(navController) }
                        // NUEVA RUTA PARA MOVIMIENTOS INTERNOS
                        composable("movimientos") { MovimientoInternoScreen(navController) }
                    }
                }
            }
        }
    }
}