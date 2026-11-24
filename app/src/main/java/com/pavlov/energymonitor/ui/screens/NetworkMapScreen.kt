package com.pavlov.energymonitor.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState
import com.pavlov.energymonitor.domain.NodeStatus
import com.pavlov.energymonitor.ui.components.LargeTitle
import com.pavlov.energymonitor.viewmodel.NodesViewModel

private val KYIV_CENTER = LatLng(50.4501, 30.5234)

/**
 * ПЗ 16: мапа (Google Maps Compose) з маркерами вузлів мережі та GPS-позицією користувача.
 * Двостороння взаємодія зі списком (ПЗ 14) — через спільний [NodesViewModel.selectedId]:
 * вибір у списку фокусує мапу на вузлі, клік на маркері підсвічує картку внизу екрана.
 */
@Composable
fun NetworkMapScreen(vm: NodesViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val allNodes = vm.allNodes
    val selectedId by vm.selectedId.collectAsStateWithLifecycle()
    val selectedNode by vm.selectedNode.collectAsStateWithLifecycle()

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasLocationPermission = granted
    }
    LaunchedEffect(Unit) {
        if (!hasLocationPermission) permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(KYIV_CENTER, 11f)
    }

    // ПЗ 16: FusedLocationProviderClient — рекомендований Google спосіб отримати останню
    // відому позицію без постійного опитування GPS (економить заряд батареї).
    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            try {
                LocationServices.getFusedLocationProviderClient(context).lastLocation.addOnSuccessListener { location ->
                    if (location != null && selectedId == null) {
                        cameraPositionState.move(
                            CameraUpdateFactory.newLatLngZoom(LatLng(location.latitude, location.longitude), 12f),
                        )
                    }
                }
            } catch (e: SecurityException) {
                // Користувач відкликав дозвіл між перевіркою і викликом - лишаємось на мапі Києва.
            }
        }
    }

    // Вибір вузла у списку (ПЗ 14) автоматично фокусує мапу на ньому.
    LaunchedEffect(selectedId) {
        val node = allNodes.firstOrNull { it.id == selectedId }
        if (node != null) {
            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(LatLng(node.lat, node.lon), 14f))
        }
    }

    Column(modifier.fillMaxSize()) {
        LargeTitle("Мапа мережі", "${allNodes.size} вузлів · натисніть маркер для деталей")
        Box(Modifier.weight(1f).fillMaxWidth()) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = hasLocationPermission),
                uiSettings = MapUiSettings(myLocationButtonEnabled = hasLocationPermission),
            ) {
                allNodes.forEach { node ->
                    val hue = when (node.status) {
                        NodeStatus.ONLINE -> BitmapDescriptorFactory.HUE_GREEN
                        NodeStatus.WARNING -> BitmapDescriptorFactory.HUE_ORANGE
                        NodeStatus.OFFLINE -> BitmapDescriptorFactory.HUE_RED
                    }
                    Marker(
                        state = rememberUpdatedMarkerState(position = LatLng(node.lat, node.lon)),
                        title = node.name,
                        snippet = "${node.type.label} · %.1f кВт · ${node.status.label}".format(node.powerW / 1000.0),
                        icon = BitmapDescriptorFactory.defaultMarker(hue),
                        onClick = { marker ->
                            vm.select(node.id) // клік на маркері підсвічує картку у списку/знизу (двостороння взаємодія)
                            marker.showInfoWindow()
                            true
                        },
                    )
                }
            }
        }
        selectedNode?.let { node ->
            NodeCard(node, onClick = {}, highlighted = true, modifier = Modifier.fillMaxWidth().padding(16.dp))
        }
    }
}
