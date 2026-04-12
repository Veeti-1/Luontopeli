package com.example.luontopeli

import LuontopeliTheme
import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import com.example.luontopeli.ml.ClassificationResult
import com.example.luontopeli.ui.navigation.LuontopeliBottomBar
import com.example.luontopeli.ui.navigation.LuontopeliNavHost
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // Sijaintiluvat Accompanist Permissionsilla
            val permissionState = rememberMultiplePermissionsState(
                permissions = listOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )

// ACTIVITY_RECOGNITION tarvitaan Android 10+ askelmittarille
            val activityRecognitionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { /* granted */  }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    activityRecognitionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                }
            }

// Näytä lupapyyntö-UI jos luvat puuttuu
            if (!permissionState.allPermissionsGranted) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Sijaintilupa tarvitaan karttaa varten")
                    Button(onClick = { permissionState.launchMultiplePermissionRequest() }) {
                        Text("Myönnä lupa")
                    }
                }
                return@setContent
            }
            LuontopeliTheme {

                LuontoPeli()

            }
        }
    }
}
@Composable
fun RequestActivityRecognitionPermission(
    onGranted: () -> Unit
) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) onGranted()
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            launcher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
        } else {
            onGranted()  // Vanhemmilla laitteilla ei tarvita lupaa
        }
    }
}
@Composable
fun LuontoPeli() {

    val navController = rememberNavController()

    Scaffold(

        bottomBar = {
            LuontopeliBottomBar(navController = navController)
        }
    ) { innerPadding ->

        LuontopeliNavHost(
            navController = navController,
            modifier = Modifier.padding(innerPadding)  // vältetään sisällön piiloutuminen BottomBarin alle
        )
    }
}
@Composable
fun ClassificationResultCard(result: ClassificationResult) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (result) {
                is ClassificationResult.Success ->
                    if (result.confidence > 0.8f)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.secondaryContainer
                else -> MaterialTheme.colorScheme.errorContainer
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            when (result) {
                is ClassificationResult.Success -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Tunnistettu:",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(Modifier.width(8.dp))
                        // Varmuustaso-badge
                        Badge(
                            containerColor = when {
                                result.confidence > 0.8f -> Color(0xFF2E7D32)
                                result.confidence > 0.6f -> Color(0xFFF57C00)
                                else -> Color(0xFFD32F2F)
                            }
                        ) {
                            Text("${"%.0f".format(result.confidence * 100)}%")
                        }
                    }

                    Text(
                        text = result.label,
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    // Varmuuspalkki
                    LinearProgressIndicator(
                        progress = result.confidence,
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                        color = when {
                            result.confidence > 0.8f -> Color(0xFF2E7D32)
                            result.confidence > 0.6f -> Color(0xFFF57C00)
                            else -> Color(0xFFD32F2F)
                        }
                    )
                }

                is ClassificationResult.NotNature -> {
                    Text("Ei luontokohde", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Kuvassa tunnistettiin: ${result.allLabels.joinToString { it.text }}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                is ClassificationResult.Error -> {
                    Text("Tunnistus epäonnistui: ${result.message}",
                        style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}


