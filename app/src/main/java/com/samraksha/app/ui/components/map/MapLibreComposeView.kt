package com.samraksha.app.ui.components.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import java.util.concurrent.atomic.AtomicLong

/**
 * Reusable Jetpack Compose wrapper for MapLibre Native SDK [MapView].
 * Properly forwards all Android Activity/Lifecycle events (create, start, resume,
 * pause, stop, destroy) to prevent memory leaks and OpenGL resource leaks.
 *
 * Implements strict style lifecycle management with generation tracking to prevent:
 * 1. Races between sensor updates and asynchronous style transitions.
 * 2. Stale style callbacks modifying destroyed or replaced map styles.
 * 3. Accessing unattached style sources during style transitions.
 */
@Composable
fun MapLibreComposeView(
    modifier: Modifier = Modifier,
    styleUrl: String,
    onStyleWillChange: (() -> Unit)? = null,
    onMapDestroyed: (() -> Unit)? = null,
    onMapReady: (MapLibreMap, Style) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val mapView = remember {
        MapView(context).apply {
            onCreate(null)
        }
    }

    val styleGeneration = remember { AtomicLong(0L) }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> {
                    try {
                        mapView.getMapAsync { maplibreMap ->
                            try {
                                if (maplibreMap.locationComponent.isLocationComponentActivated) {
                                    maplibreMap.locationComponent.isLocationComponentEnabled = false
                                }
                            } catch (_: Exception) {}
                        }
                    } catch (_: Exception) {}
                    mapView.onPause()
                }
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            styleGeneration.incrementAndGet()
            try {
                mapView.getMapAsync { maplibreMap ->
                    try {
                        if (maplibreMap.locationComponent.isLocationComponentActivated) {
                            maplibreMap.locationComponent.isLocationComponentEnabled = false
                        }
                    } catch (_: Exception) {}
                }
            } catch (_: Exception) {}
            onMapDestroyed?.invoke()
            mapView.onDestroy()
        }
    }

    LaunchedEffect(mapView, styleUrl) {
        val currentGen = styleGeneration.incrementAndGet()
        mapView.getMapAsync { maplibreMap ->
            if (currentGen != styleGeneration.get()) return@getMapAsync

            onStyleWillChange?.invoke()
            try {
                if (maplibreMap.locationComponent.isLocationComponentActivated) {
                    maplibreMap.locationComponent.isLocationComponentEnabled = false
                }
            } catch (_: Exception) {}

            maplibreMap.setStyle(styleUrl) { style ->
                if (currentGen == styleGeneration.get() && style.isFullyLoaded) {
                    onMapReady(maplibreMap, style)
                }
            }
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize()
        )
    }
}
