package com.example.adrive

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.adrive.data.model.DriveFile
import com.example.adrive.data.network.ApiClient
import com.example.adrive.ui.auth.LoginScreen
import com.example.adrive.ui.drive.DriveScreen
import com.example.adrive.ui.preview.PreviewScreen
import com.example.adrive.ui.theme.AdriveTheme
import com.google.gson.Gson

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AdriveTheme {
                AdriveNavGraph()
            }
        }
    }
}

@Composable
fun AdriveNavGraph() {
    val navController = rememberNavController()
    val gson = remember { Gson() }

    // Decide start destination: if already logged in, go straight to drive
    val startDest = if (ApiClient.getCookieJar().hasAuthCookie()) "drive" else "login"

    NavHost(navController = navController, startDestination = startDest) {

        composable("login") {
            LoginScreen(
                onAuthenticated = {
                    navController.navigate("drive") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        composable("drive") {
            DriveScreen(
                onOpenPreview = { file ->
                    // Serialize the file to JSON to pass via navigation arg
                    val fileJson = java.net.URLEncoder.encode(gson.toJson(file), "UTF-8")
                    navController.navigate("preview/$fileJson")
                },
                onSignOut = {
                    navController.navigate("login") {
                        popUpTo("drive") { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = "preview/{fileJson}",
            arguments = listOf(navArgument("fileJson") { type = NavType.StringType })
        ) { backStack ->
            val fileJson = java.net.URLDecoder.decode(
                backStack.arguments?.getString("fileJson") ?: "",
                "UTF-8"
            )
            val file = remember(fileJson) {
                runCatching { gson.fromJson(fileJson, DriveFile::class.java) }.getOrNull()
            }
            if (file != null) {
                PreviewScreen(file = file, onBack = { navController.popBackStack() })
            }
        }
    }
}

