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
import com.example.custodia_app.screens.ReporteScreen
import com.example.custodia_app.ui.theme.Custodia_AppTheme // Asegúrate de que el nombre del tema coincida con el tuyo

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Custodia_AppTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    val navController = rememberNavController()

                    // Mapa de Navegación de la App
                    NavHost(navController = navController, startDestination = "login") {
                        composable("login") { LoginScreen(navController) }
                        composable("menu") { MenuScreen(navController) }
                        composable("inspeccion") { InspeccionScreen(navController) }
                        composable("reporte") { ReporteScreen(navController) }
                    }
                }
            }
        }
    }
}