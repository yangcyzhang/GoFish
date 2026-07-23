package com.yangcy.gofish.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import kotlin.math.pow
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import com.yangcy.gofish.data.model.FishingSpot
import com.yangcy.gofish.ui.viewmodel.FishingLocation
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreLocation.CLLocationCoordinate2DMake
import platform.MapKit.*
import platform.UIKit.UIGestureRecognizerStateBegan
import platform.UIKit.UILongPressGestureRecognizer
import platform.UIKit.UITapGestureRecognizer
import platform.darwin.NSObject
import platform.objc.sel_registerName
import kotlinx.cinterop.ObjCAction
import kotlinx.cinterop.BetaInteropApi
import platform.CoreGraphics.*
import platform.UIKit.*

@OptIn(ExperimentalForeignApi::class)
private fun createBalloonMarkerImage(colorHex: String): UIImage {
    val size = CGSizeMake(44.0, 52.0)
    UIGraphicsBeginImageContextWithOptions(size, false, 0.0)
    
    val cleanHex = colorHex.removePrefix("#")
    val (r, g, b) = try {
        if (cleanHex == "FF00ADB5" || cleanHex.isEmpty()) {
            Triple(1.0, 0.32, 0.32)
        } else {
            Triple(
                cleanHex.substring(0, 2).toInt(16).toDouble() / 255.0,
                cleanHex.substring(2, 4).toInt(16).toDouble() / 255.0,
                cleanHex.substring(4, 6).toInt(16).toDouble() / 255.0
            )
        }
    } catch (e: Exception) {
        Triple(1.0, 0.32, 0.32)
    }
    val color = UIColor.colorWithRed(r, g, b, 1.0)
    
    val shadowColor = UIColor.colorWithRed(0.0, 0.0, 0.0, 0.25)
    shadowColor.setFill()
    val shadowPath = UIBezierPath.bezierPathWithOvalInRect(CGRectMake(22.0 - 7.0, 45.0, 14.0, 3.5))
    shadowPath.fill()
    
    val path = UIBezierPath()
    path.moveToPoint(CGPointMake(22.0, 46.0))
    path.addCurveToPoint(
        endPoint = CGPointMake(9.0, 18.0),
        controlPoint1 = CGPointMake(15.4, 34.0),
        controlPoint2 = CGPointMake(9.0, 26.0)
    )
    path.addArcWithCenter(
        center = CGPointMake(22.0, 18.0),
        radius = 13.0,
        startAngle = kotlin.math.PI,
        endAngle = 0.0,
        clockwise = true
    )
    path.addCurveToPoint(
        endPoint = CGPointMake(22.0, 46.0),
        controlPoint1 = CGPointMake(35.0, 26.0),
        controlPoint2 = CGPointMake(28.6, 34.0)
    )
    path.closePath()
    
    color.setFill()
    path.fill()
    
    val borderCol = UIColor.colorWithRed(r * 0.8, g * 0.8, b * 0.8, 1.0)
    borderCol.setStroke()
    path.setLineWidth(1.2)
    path.stroke()
    
    UIColor.whiteColor.setFill()
    val whiteDotPath = UIBezierPath.bezierPathWithOvalInRect(CGRectMake(22.0 - 3.8, 18.0 - 3.8, 7.6, 7.6))
    whiteDotPath.fill()
    
    val image = UIGraphicsGetImageFromCurrentImageContext()
    UIGraphicsEndImageContext()
    return image!!
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private class MapGestureHandler(
    private val mapView: MKMapView,
    private val onMapClick: () -> Unit,
    private val onMapLongClick: (Double, Double) -> Unit
) : NSObject() {
    
    @ObjCAction
    fun handleTap() {
        onMapClick()
    }

    @ObjCAction
    fun handleLongPress(gesture: UILongPressGestureRecognizer) {
        if (gesture.state == UIGestureRecognizerStateBegan) {
            val generator = UISelectionFeedbackGenerator()
            generator.selectionChanged()
            
            val point = gesture.locationInView(mapView)
            val coordinate = mapView.convertPoint(point, toCoordinateFromView = mapView)
            coordinate.useContents {
                onMapLongClick(latitude, longitude)
            }
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun MapComponent(
    modifier: Modifier,
    spots: List<FishingSpot>,
    selectedLocation: FishingLocation,
    zoomLevel: Float,
    isSatellite: Boolean,
    pendingLocation: Pair<Double, Double>?,
    onMapClick: () -> Unit,
    onMapLongClick: (lat: Double, lon: Double) -> Unit,
    onMarkerClick: (FishingSpot) -> Unit
) {
    val mapView = remember { MKMapView().apply {
        showsUserLocation = true
    } }

    val coordinator = remember {
        object : NSObject(), MKMapViewDelegateProtocol {
            @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
            override fun mapView(mapView: MKMapView, didSelectAnnotationView: MKAnnotationView) {
                val annotation = didSelectAnnotationView.annotation ?: return
                val spot = spots.find { it.name == annotation.title }
                if (spot != null) onMarkerClick(spot)
                mapView.deselectAnnotation(annotation, true)
                onMapClick()
            }

            @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
            override fun mapView(mapView: MKMapView, viewForAnnotation: MKAnnotationProtocol): MKAnnotationView? {
                if (viewForAnnotation is MKUserLocation) return null
                
                val reuseId = "FishingSpotMarker"
                var annotationView = mapView.dequeueReusableAnnotationViewWithIdentifier(reuseId)
                if (annotationView == null) {
                    annotationView = MKAnnotationView(viewForAnnotation, reuseId)
                    annotationView.canShowCallout = true
                } else {
                    annotationView.annotation = viewForAnnotation
                }
                
                val spot = spots.find { it.name == viewForAnnotation.title }
                val isPending = viewForAnnotation.title == "准备标记点"
                
                if (isPending) {
                    annotationView.image = createBalloonMarkerImage("#FFA726")
                    annotationView.centerOffset = CGPointMake(0.0, -20.0)
                } else if (spot != null) {
                    annotationView.image = createBalloonMarkerImage(spot.iconColor)
                    annotationView.centerOffset = CGPointMake(0.0, -20.0)
                }
                return annotationView
            }
        }
    }

    val gestureHandler = remember { MapGestureHandler(mapView, onMapClick, onMapLongClick) }

    LaunchedEffect(spots, pendingLocation, mapView) {
        mapView.removeAnnotations(mapView.annotations)
        spots.forEach { spot ->
            val annotation = MKPointAnnotation().apply {
                setCoordinate(CLLocationCoordinate2DMake(spot.latitude, spot.longitude))
                setTitle(spot.name)
            }
            mapView.addAnnotation(annotation)
        }

        // 添加临时准备标记点
        pendingLocation?.let { (lat, lon) ->
            val annotation = MKPointAnnotation().apply {
                setCoordinate(CLLocationCoordinate2DMake(lat, lon))
                setTitle("准备标记点")
            }
            mapView.addAnnotation(annotation)
        }
    }

    LaunchedEffect(mapView) {
        mapView.delegate = coordinator
        
        val longPress = UILongPressGestureRecognizer(
            target = gestureHandler,
            action = sel_registerName("handleLongPress:")
        ).apply {
            minimumPressDuration = 0.8 // 800ms
        }
        mapView.addGestureRecognizer(longPress)
        
        val tap = UITapGestureRecognizer(
            target = gestureHandler,
            action = sel_registerName("handleTap")
        )
        tap.setCancelsTouchesInView(false) 
        mapView.addGestureRecognizer(tap)
    }

    LaunchedEffect(isSatellite) {
        mapView.mapType = if (isSatellite) MKMapTypeSatellite else MKMapTypeStandard
    }

    LaunchedEffect(selectedLocation, zoomLevel) {
        val center = CLLocationCoordinate2DMake(selectedLocation.lat, selectedLocation.lon)
        val distance = 2000.0 * 2.0.pow((15.0 - zoomLevel).toDouble())
        val region = MKCoordinateRegionMakeWithDistance(center, distance, distance)
        mapView.setRegion(region, animated = true)
    }

    UIKitView(
        factory = { mapView },
        modifier = modifier.fillMaxSize(),
        update = { }
    )
}
