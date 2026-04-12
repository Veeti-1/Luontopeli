package com.example.luontopeli.ui.map

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.luontopeli.viewmodel.WalkViewModel
import com.example.luontopeli.viewmodel.formatDistance
import com.example.luontopeli.viewmodel.formatDuration
// 📁 ui/map/MapScreen.kt


import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.luontopeli.viewmodel.MapViewModel
import com.example.luontopeli.viewmodel.formatDuration
import com.example.luontopeli.viewmodel.toFormattedDate
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.io.File

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MapScreen(
    mapViewModel: MapViewModel = viewModel(),
    walkViewModel: WalkViewModel = viewModel()
) {
    val context = LocalContext.current

    val permissionState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    val activityRecognitionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* granted */ }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            activityRecognitionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
        }
    }

    if (!permissionState.allPermissionsGranted) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Sijaintilupa tarvitaan karttaa varten")
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { permissionState.launchMultiplePermissionRequest() }) {
                Text("Myönnä lupa")
            }
        }
        return
    }

    val isWalking by walkViewModel.isWalking.collectAsState()
    val routePoints by mapViewModel.routePoints.collectAsState()
    val currentLocation by mapViewModel.currentLocation.collectAsState()
    val natureSpots by mapViewModel.natureSpots.collectAsState()

    LaunchedEffect(isWalking) {
        if (isWalking) mapViewModel.startTracking()
        else mapViewModel.stopTracking()
    }

    val defaultPosition = GeoPoint(65.0121, 25.4651)

    LaunchedEffect(Unit) {
        Configuration.getInstance().userAgentValue = context.packageName
    }

    Column(modifier = Modifier.fillMaxSize()) {

        Box(modifier = Modifier.weight(1f)) {

            val mapViewState = remember { MapView(context) }

            DisposableEffect(Unit) {
                mapViewState.setTileSource(TileSourceFactory.MAPNIK)
                mapViewState.setMultiTouchControls(true)
                mapViewState.controller.setZoom(15.0)
                mapViewState.controller.setCenter(
                    currentLocation?.let { GeoPoint(it.latitude, it.longitude) }
                        ?: defaultPosition
                )

                onDispose {
                    mapViewState.onDetach()
                }
            }

            AndroidView(
                factory = { mapViewState },
                modifier = Modifier.fillMaxSize(),
                update = { mapView ->
                    mapView.overlays.clear()

                    if (routePoints.size >= 2) {
                        val polyline = Polyline().apply {
                            setPoints(routePoints)
                            outlinePaint.color = 0xFF2E7D32.toInt()  // M3-vihreä
                            outlinePaint.strokeWidth = 8f
                        }
                        mapView.overlays.add(polyline)
                    }

                    natureSpots.forEach { spot ->
                        // Paikallinen tiedosto polusta

                        val marker = Marker(mapView).apply {
                            position = GeoPoint(spot.latitude!!.toDouble(), spot.longitude!!.toDouble())
                            // Näytä kasvin nimi tai kohteen nimi info-ikkunassa
                            title = spot.plantLabel ?: spot.name
                            snippet = spot.timestamp.toFormattedDate()
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        }
                        mapView.overlays.add(marker)

                    }


                    currentLocation?.let { loc ->
                        mapView.controller.animateTo(GeoPoint(loc.latitude, loc.longitude))
                    }

                    mapView.invalidate()  // Piirretään kartta uudelleen
                }
            )
        }
        natureSpots.forEach { spot ->
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(File(spot.imageLocalPath!!))  // Polku tiedostoon
                    .crossfade(true)                    // Pehmeä häivytysanimaatio
                    .build(),
                contentDescription = spot.plantLabel ?: "Luontokuva",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp))
            )

// Firebase Storage URL:sta (viikko 6)
            AsyncImage(
                model = spot.imageFirebaseUrl,
                contentDescription = "Luontokuva pilvestä",
                modifier = Modifier.size(80.dp).clip(CircleShape)
            )
        }

        WalkStatsCard(walkViewModel)
    }
}


@Composable
fun WalkStatsCard(viewModel: WalkViewModel) {
    val session by viewModel.currentSession.collectAsState()
    val isWalking by viewModel.isWalking.collectAsState()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = if (isWalking) "Kävely käynnissä" else "Kävely pysäytetty",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            // Näytä tilastot vain jos sessio on olemassa
            session?.let { s ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${s.stepCount}",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text("askelta", style = MaterialTheme.typography.bodySmall)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = formatDistance(s.distanceMeters),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text("matka", style = MaterialTheme.typography.bodySmall)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = formatDuration(s.startTime),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text("aika", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
            ) {
                if (!isWalking) {
                    Button(
                        onClick = { viewModel.startWalk() },
                        modifier = Modifier.weight(1f)
                    ) { Text("Aloita kävely") }
                } else {
                    OutlinedButton(
                        onClick = { viewModel.stopWalk() },
                        modifier = Modifier.weight(1f)
                    ) { Text("Lopeta") }
                }
            }
        }
    }
}