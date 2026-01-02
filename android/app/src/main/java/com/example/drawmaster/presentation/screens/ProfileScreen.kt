package com.example.drawmaster.presentation.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.drawmaster.presentation.components.TextButton
import com.example.drawmaster.presentation.viewmodels.AuthViewModel
import com.example.drawmaster.presentation.viewmodels.LocationUiState
import com.example.drawmaster.presentation.viewmodels.ProfileViewModel
import com.example.drawmaster.ui.theme.DrawMasterTheme
import com.example.drawmaster.ui.theme.TealBlue
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProfileScreen(
    navController: NavHostController,
    profileViewModel: ProfileViewModel = viewModel()
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE)

    // Load osmdroid configuration
    LaunchedEffect(Unit) {
        Configuration.getInstance().load(context, prefs)
    }

    val stateValue by profileViewModel.uiState.collectAsState()

    // Permission handling
    val permissionGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) profileViewModel.refreshLocation()
    }

    // Main container to allow bottom alignment
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Top-level layout (center content vertically)
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val authViewModel: AuthViewModel = viewModel()
            val currentUser by authViewModel.currentUser.collectAsState()
            currentUser?.let { user ->
                Text(
                    text = user.displayName ?: user.email ?: "User",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            when (stateValue) {
                is LocationUiState.Idle -> Text(
                    text = "Allow permission to access your location to update it",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                )

                is LocationUiState.Loading -> Text(
                    text = "Obtaining your location...",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                )

                is LocationUiState.Success -> {
                    val s = stateValue as LocationUiState.Success
                    val geoPoint = GeoPoint(s.latitude, s.longitude)

                    Text(
                        text = "Lat: ${s.latitude}, Lng: ${s.longitude}",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(8.dp)
                    )

                    s.accuracy?.let { acc ->
                        Text(
                            text = "Precision: ${"%.1f".format(acc)} m",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    s.timestamp?.let { ts ->
                        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                        Text(text = "Time: ${sdf.format(Date(ts))}", style = MaterialTheme.typography.bodySmall)
                    }

                    // Map container: fixed size, rounded and clipped so it behaves like a canvas
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, ComposeColor(0xFFDDDDDD), RoundedCornerShape(12.dp))
                            .background(ComposeColor.White)
                    ) {
                        AndroidView(
                            factory = { ctx ->
                                MapView(ctx).apply {
                                    setTileSource(TileSourceFactory.MAPNIK)
                                    controller.setZoom(15.0)
                                    // make map non-interactive so it behaves like a fixed canvas
                                    setMultiTouchControls(false)
                                    isClickable = false
                                    setBuiltInZoomControls(false)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(280.dp),
                            update = { mapView ->
                                try {
                                    mapView.controller.setZoom(15.0)
                                    mapView.controller.animateTo(geoPoint)
                                } catch (_: Exception) {
                                    mapView.controller.setCenter(geoPoint)
                                }

                                val toRemove = mapView.overlays.filterIsInstance<Marker>().toList()
                                if (toRemove.isNotEmpty()) mapView.overlays.removeAll(toRemove)

                                val marker = Marker(mapView)
                                marker.position = geoPoint
                                marker.title = null
                                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                val drawable = ContextCompat.getDrawable(mapView.context, com.example.drawmaster.R.drawable.ic_location_pin_red)
                                drawable?.let { marker.icon = it }
                                mapView.overlays.add(marker)
                                mapView.invalidate()
                            }
                        )
                    }
                    Text(
                        text = "Location Updated!",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(8.dp)
                    )
                }

                is LocationUiState.Error -> {
                    val e = stateValue as LocationUiState.Error
                    Text(
                        text = "Error: ${e.message}",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Bottom section for action buttons
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TextButton(
                name = if (permissionGranted) "Update your location" else "Request permission",
                backgroundColor = TealBlue,
                borderColor = TealBlue,
                fontColor = Color.White,
                onClick = {
                    if (!permissionGranted) {
                        launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    } else {
                        profileViewModel.refreshLocation()
                    }
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                name = "Go back to menu",
                backgroundColor = TealBlue,
                borderColor = TealBlue,
                fontColor = Color.White,
                onClick = { navController.popBackStack() }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    val navController = rememberNavController()
    DrawMasterTheme {
        ProfileScreen(navController = navController)
    }
}