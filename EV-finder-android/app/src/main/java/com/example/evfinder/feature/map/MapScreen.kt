package com.example.evfinder.feature.map

import android.content.Context
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.evfinder.core.model.StationDto
import com.example.evfinder.core.network.OverpassClient
import com.example.evfinder.ui.components.EvCard
import com.example.evfinder.ui.components.EvPrimaryButton
import com.example.evfinder.ui.components.evCircleMarker
import com.example.evfinder.ui.theme.EvColors
import org.osmdroid.events.MapListener
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@Composable
fun MapScreen(
    onStationClick: (String) -> Unit,
    onGetDirections: (lat: Double, lng: Double, name: String) -> Unit = { _, _, _ -> },
    viewModel: MapViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var selectedPoi by remember { mutableStateOf<OverpassClient.Poi?>(null) }

    Box(Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                val map = MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                    isTilesScaledToDpi = true
                    setMinZoomLevel(5.0)
                    setMaxZoomLevel(19.0)
                    controller.setZoom(12.0)
                    controller.setCenter(GeoPoint(23.7810, 90.4150))
                }
                map.addMapListener(object : MapListener {
                    override fun onScroll(event: org.osmdroid.events.ScrollEvent?): Boolean {
                        val bb = map.boundingBox
                        viewModel.onMapMoved(bb.latSouth, bb.lonWest, bb.latNorth, bb.lonEast)
                        return true
                    }
                    override fun onZoom(event: org.osmdroid.events.ZoomEvent?): Boolean {
                        val bb = map.boundingBox
                        viewModel.onMapMoved(bb.latSouth, bb.lonWest, bb.latNorth, bb.lonEast)
                        return true
                    }
                })
                map.post {
                    val bb = map.boundingBox
                    viewModel.onMapMoved(bb.latSouth, bb.lonWest, bb.latNorth, bb.lonEast)
                }
                map
            },
            update = { map ->
                renderMarkers(map, state.stations, state.pois, context, onStationClick) { selectedPoi = it }
            },
            modifier = Modifier.fillMaxSize()
        )

        EvCard(Modifier.align(Alignment.TopCenter).padding(top = 12.dp)) {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                LegendDot(EvColors.Primary, "EV")
                Spacer(Modifier.width(10.dp))
                LegendDot(androidx.compose.ui.graphics.Color(0xFFFFA726), "Fuel")
                Spacer(Modifier.width(10.dp))
                LegendDot(androidx.compose.ui.graphics.Color(0xFF4FC3F7), "LPG")
            }
        }

        if (state.poiLoading) {
            CircularProgressIndicator(
                Modifier.align(Alignment.TopEnd).padding(12.dp).size(18.dp),
                strokeWidth = 2.dp, color = EvColors.Primary
            )
        }

        if (state.loading && state.stations.isEmpty()) {
            EvCard(Modifier.align(Alignment.Center)) {
                Box(Modifier.padding(20.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = EvColors.Primary)
                }
            }
        }
        state.error?.let { error ->
            EvCard(Modifier.align(Alignment.Center).padding(24.dp)) {
                Text(error, color = EvColors.Error, style = MaterialTheme.typography.bodyMedium)
            }
        }

        selectedPoi?.let { poi ->
            Box(Modifier.align(Alignment.BottomCenter).padding(16.dp).fillMaxWidth()) {
                PoiInfoCard(
                    poi = poi,
                    onClose = { selectedPoi = null },
                    onGetDirections = onGetDirections
                )
            }
        }
    }
}

private fun renderMarkers(
    map: MapView,
    stations: List<StationDto>,
    pois: List<OverpassClient.Poi>,
    context: Context,
    onStationClick: (String) -> Unit,
    onPoiClick: (OverpassClient.Poi) -> Unit
) {
    val key = stations.map { it.id }.hashCode() * 31 + pois.size * 17 + pois.hashCode()
    if (map.tag as? Int == key) return
    map.tag = key
    map.overlays.clear()

    stations.forEach { station ->
        map.overlays.add(Marker(map).apply {
            position = GeoPoint(station.latitude, station.longitude)
            title = "⚡ ${station.name}"
            snippet = station.address ?: ""
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            setOnMarkerClickListener { _, _ ->
                onStationClick(station.id)
                true
            }
        })
    }
    pois.forEach { poi ->
        val color = if (poi.lpg) AndroidColor.rgb(79, 195, 247) else AndroidColor.rgb(255, 167, 38)
        map.overlays.add(Marker(map).apply {
            position = GeoPoint(poi.lat, poi.lng)
            title = (if (poi.lpg) "⛽ LPG · " else "⛽ ") + poi.name
            icon = evCircleMarker(context, color)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            setOnMarkerClickListener { _, _ ->
                onPoiClick(poi)
                true
            }
        })
    }
    map.invalidate()
}

@Composable
private fun LegendDot(color: androidx.compose.ui.graphics.Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Spacer(Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = EvColors.OnSurface)
    }
}

@Composable
private fun PoiInfoCard(
    poi: OverpassClient.Poi,
    onClose: () -> Unit,
    onGetDirections: (lat: Double, lng: Double, name: String) -> Unit
) {
    val color = if (poi.lpg) androidx.compose.ui.graphics.Color(0xFF4FC3F7)
    else androidx.compose.ui.graphics.Color(0xFFFFA726)
    EvCard(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.size(10.dp).clip(CircleShape).background(color))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(poi.name, style = MaterialTheme.typography.titleMedium,
                        color = EvColors.OnBackground, fontWeight = FontWeight.SemiBold)
                    Text(
                        if (poi.lpg) "LPG & Fuel Station" else "Fuel Station",
                        style = MaterialTheme.typography.bodySmall, color = EvColors.OnSurfaceVar
                    )
                }
                IconButton(onClick = { onClose() }) {
                    Text("✕", color = EvColors.OnSurfaceVar, style = MaterialTheme.typography.titleMedium)
                }
            }
            Spacer(Modifier.height(6.dp))
            Text("%.5f, %.5f".format(poi.lat, poi.lng),
                style = MaterialTheme.typography.labelSmall, color = EvColors.OnSurfaceVar)
            Spacer(Modifier.height(12.dp))
            EvPrimaryButton(
                text = "Get Directions",
                onClick = { onGetDirections(poi.lat, poi.lng, poi.name) },
                modifier = Modifier.fillMaxWidth(),
                icon = Icons.Filled.Navigation
            )
        }
    }
}
