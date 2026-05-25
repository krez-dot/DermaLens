package com.dermalens.app.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

data class Clinic(
    val name: String,
    val address: String,
    val distance: String,
    val rating: Float,
    val openNow: Boolean,
    val hours: String,
    val phone: String,
    val lat: Double,
    val lng: Double
)

private fun isOpenNow(hours: String): Boolean {
    if (hours.isEmpty() || hours.startsWith("Contact")) return true
    return try {
        val cal = java.util.Calendar.getInstance()
        val dow = cal.get(java.util.Calendar.DAY_OF_WEEK)
        val nowMin = cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE)
        val colonIdx = hours.indexOf(':')
        if (colonIdx < 0) return true
        val dayPart = hours.substring(0, colonIdx).trim()
        val timePart = hours.substring(colonIdx + 1).trim()
        val dayMap = mapOf("Mon" to 2, "Tue" to 3, "Wed" to 4, "Thu" to 5, "Fri" to 6, "Sat" to 7, "Sun" to 1)
        val dayTokens = dayPart.split("-")
        val startDay = dayMap[dayTokens[0].trim()] ?: return true
        val endDay = if (dayTokens.size > 1) dayMap[dayTokens[1].trim()] ?: return true else startDay
        val inDay = if (startDay <= endDay) dow in startDay..endDay else dow >= startDay || dow <= endDay
        if (!inDay) return false
        val times = timePart.split(" - ")
        if (times.size < 2) return true
        fun toMin(t: String): Int {
            val pm = "PM" in t
            val clean = t.replace("AM", "").replace("PM", "").trim()
            val hm = clean.split(":")
            var h = hm[0].trim().toInt()
            val m = if (hm.size > 1) hm[1].trim().toInt() else 0
            if (pm && h != 12) h += 12
            if (!pm && h == 12) h = 0
            return h * 60 + m
        }
        nowMin in toMin(times[0])..toMin(times[1])
    } catch (e: Exception) { true }
}

val mockClinics = listOf(
    Clinic("Tarlac Dermatology Clinic", "Macabulos Dr, Tarlac City, Tarlac", "0.8 km", 4.8f, isOpenNow("Mon-Sat: 8:00 AM - 5:00 PM"), "Mon-Sat: 8:00 AM - 5:00 PM", "+63 45 982 1234", 15.4755, 120.5963),
    Clinic("SkinCare Specialists Tarlac", "F. Tañedo St, Tarlac City, Tarlac", "1.2 km", 4.6f, isOpenNow("Mon-Fri: 9:00 AM - 6:00 PM"), "Mon-Fri: 9:00 AM - 6:00 PM", "+63 45 982 5678", 15.4780, 120.5940),
    Clinic("Tarlac Provincial Hospital - Derma", "San Vicente St, Tarlac City, Tarlac", "2.1 km", 4.3f, isOpenNow("Mon-Sun: 8:00 AM - 8:00 PM"), "Mon-Sun: 8:00 AM - 8:00 PM", "+63 45 982 9012", 15.4720, 120.5990),
    Clinic("Dr. Santos Skin & Laser Clinic", "Romulo Blvd, Tarlac City, Tarlac", "2.8 km", 4.9f, isOpenNow("Tue-Sat: 10:00 AM - 7:00 PM"), "Tue-Sat: 10:00 AM - 7:00 PM", "+63 45 982 3456", 15.4800, 120.5920),
    Clinic("Capampangan Derma Center", "McArthur Hwy, Tarlac City, Tarlac", "3.5 km", 4.5f, isOpenNow("Mon-Sat: 9:00 AM - 5:00 PM"), "Mon-Sat: 9:00 AM - 5:00 PM", "+63 45 982 7890", 15.4690, 120.6010),
)

private fun createUserDotDrawable(context: Context): Drawable {
    val dp = context.resources.displayMetrics.density
    val size = (22 * dp).toInt()
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val cx = size / 2f
    paint.color = 0xFFFFFFFF.toInt()
    canvas.drawCircle(cx, cx, cx, paint)
    paint.color = 0xFF7C3AED.toInt()
    canvas.drawCircle(cx, cx, cx * 0.62f, paint)
    return BitmapDrawable(context.resources, bitmap)
}

private fun haversineKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
    val r = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLng = Math.toRadians(lng2 - lng1)
    val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
            Math.sin(dLng / 2) * Math.sin(dLng / 2)
    return r * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
}

private fun createMarkerDrawable(context: Context): Drawable {
    val dp = context.resources.displayMetrics.density
    val w = (36 * dp).toInt()
    val h = (50 * dp).toInt()
    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val cx = w / 2f
    val r = w / 2f
    paint.color = 0xFF7C3AED.toInt()
    paint.style = Paint.Style.FILL
    canvas.drawCircle(cx, r, r, paint)
    val path = Path()
    path.moveTo(cx - r * 0.45f, r + r * 0.55f)
    path.lineTo(cx + r * 0.45f, r + r * 0.55f)
    path.lineTo(cx, h.toFloat())
    path.close()
    canvas.drawPath(path, paint)
    paint.color = 0xFFFFFFFF.toInt()
    canvas.drawCircle(cx, r, r * 0.42f, paint)
    paint.color = 0xFF7C3AED.toInt()
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = r * 0.28f
    paint.strokeCap = Paint.Cap.ROUND
    val arm = r * 0.26f
    canvas.drawLine(cx, r - arm, cx, r + arm, paint)
    canvas.drawLine(cx - arm, r, cx + arm, r, paint)
    return BitmapDrawable(context.resources, bitmap)
}

private suspend fun fetchNearbyClinics(lat: Double, lng: Double): List<Clinic> {
    return withContext(Dispatchers.IO) {
        try {
            val query = """
                [out:json][timeout:25];
                (
                  node["healthcare:speciality"="dermatology"](around:15000,$lat,$lng);
                  node["name"~"derma|skin care|skincare|skin clinic|laser skin|acne|eczema",i](around:15000,$lat,$lng);
                );
                out body;
            """.trimIndent()
            val encoded = java.net.URLEncoder.encode(query, "UTF-8")
            val url = java.net.URL("https://overpass-api.de/api/interpreter")
            val conn = (url.openConnection() as java.net.HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 15000
                readTimeout = 25000
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                outputStream.use { it.write("data=$encoded".toByteArray()) }
            }
            val response = conn.inputStream.bufferedReader().readText()
            val elements = org.json.JSONObject(response).getJSONArray("elements")
            val result = mutableListOf<Clinic>()
            for (i in 0 until elements.length()) {
                val el = elements.getJSONObject(i)
                val tags = el.optJSONObject("tags") ?: continue
                val name = tags.optString("name").takeIf { it.isNotEmpty() } ?: continue
                val elLat = el.optDouble("lat", Double.NaN).takeIf { !it.isNaN() } ?: continue
                val elLng = el.optDouble("lon", Double.NaN).takeIf { !it.isNaN() } ?: continue
                val dist = haversineKm(lat, lng, elLat, elLng)
                val addr = listOf(
                    tags.optString("addr:housenumber"),
                    tags.optString("addr:street"),
                    tags.optString("addr:city")
                ).filter { it.isNotEmpty() }.joinToString(", ").ifEmpty { "Tarlac, Philippines" }
                val phone = tags.optString("phone").ifEmpty { tags.optString("contact:phone") }.ifEmpty { "N/A" }
                val hours = tags.optString("opening_hours").ifEmpty { "Contact clinic for hours" }
                result.add(Clinic(name, addr, "%.1f km".format(dist), 4.5f, isOpenNow(hours), hours, phone, elLat, elLng))
            }
            result.sortedBy { haversineKm(lat, lng, it.lat, it.lng) }
        } catch (e: Exception) {
            emptyList()
        }
    }
}

private suspend fun fetchRoute(fromLat: Double, fromLng: Double, toLat: Double, toLng: Double): List<GeoPoint> {
    return withContext(Dispatchers.IO) {
        try {
            val url = java.net.URL(
                "https://router.project-osrm.org/route/v1/driving/$fromLng,$fromLat;$toLng,$toLat?overview=full&geometries=geojson"
            )
            val conn = (url.openConnection() as java.net.HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10000
                readTimeout = 15000
                setRequestProperty("User-Agent", "DermaLens/1.0")
            }
            val response = conn.inputStream.bufferedReader().readText()
            val coords = org.json.JSONObject(response)
                .getJSONArray("routes")
                .getJSONObject(0)
                .getJSONObject("geometry")
                .getJSONArray("coordinates")
            (0 until coords.length()).map { i ->
                val c = coords.getJSONArray(i)
                GeoPoint(c.getDouble(1), c.getDouble(0))
            }
        } catch (e: Exception) {
            listOf(GeoPoint(fromLat, fromLng), GeoPoint(toLat, toLng))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClinicLocatorScreen(navController: NavController) {
    val context = LocalContext.current
    var showMap by remember { mutableStateOf(false) }
    var selectedClinic by remember { mutableStateOf<Clinic?>(null) }
    var clinics by remember { mutableStateOf(mockClinics) }
    var isLoading by remember { mutableStateOf(true) }
    var locationLabel by remember { mutableStateOf("Locating...") }
    var userLat by remember { mutableStateOf(15.4755) }
    var userLng by remember { mutableStateOf(120.5963) }
    var mapCentered by remember { mutableStateOf(false) }
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    var routes by remember { mutableStateOf<Map<String, List<GeoPoint>>>(emptyMap()) }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    LaunchedEffect(hasLocationPermission) {
        if (!hasLocationPermission) {
            permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            locationLabel = "Tarlac City, Tarlac"
            val fetched = fetchNearbyClinics(15.4755, 120.5963)
            if (fetched.isNotEmpty()) clinics = fetched
            isLoading = false
        } else {
            val location = suspendCancellableCoroutine<android.location.Location?> { cont ->
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { cont.resume(it) }
                    .addOnFailureListener { cont.resume(null) }
            }
            val lat = location?.latitude ?: 15.4755
            val lng = location?.longitude ?: 120.5963
            userLat = lat
            userLng = lng
            val geocoder = android.location.Geocoder(context)
            val addresses = withContext(Dispatchers.IO) {
                try { geocoder.getFromLocation(lat, lng, 1) } catch (e: Exception) { null }
            }
            locationLabel = addresses?.firstOrNull()?.let {
                listOf(it.subLocality, it.locality, it.adminArea)
                    .filter { s -> !s.isNullOrEmpty() }.take(2).joinToString(", ")
            }?.takeIf { it.isNotEmpty() } ?: "Near you"
            val fetched = fetchNearbyClinics(lat, lng)
            if (fetched.isNotEmpty()) clinics = fetched
            isLoading = false
        }
    }

    LaunchedEffect(clinics, userLat, userLng) {
        val fetched = mutableMapOf<String, List<GeoPoint>>()
        clinics.forEach { clinic ->
            fetched[clinic.name] = fetchRoute(userLat, userLng, clinic.lat, clinic.lng)
        }
        routes = fetched
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Clinic Locator", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showMap = !showMap }) {
                        Icon(
                            imageVector = if (showMap) Icons.Default.List else Icons.Default.Map,
                            contentDescription = if (showMap) "List View" else "Map View",
                            tint = DermaGreen
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White, titleContentColor = Color(0xFF1a1a1a))
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).background(Color(0xFFF8F9FA))
        ) {
            // Location permission banner
            if (!hasLocationPermission) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOff, contentDescription = null, tint = Color(0xFFE65100), modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Location access needed", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                            Text("Enable location to find clinics near you", fontSize = 12.sp, color = Color(0xFFE65100))
                        }
                        TextButton(onClick = {
                            permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                        }) {
                            Text("Allow", color = DermaGreen, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Search Bar
            Row(
                modifier = Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = DermaGreen, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(locationLabel, fontSize = 14.sp, color = Color(0xFF444444), fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                Box(modifier = Modifier.background(DermaGreenLight, RoundedCornerShape(20.dp)).padding(horizontal = 12.dp, vertical = 4.dp)) {
                    if (isLoading) {
                        CircularProgressIndicator(color = DermaGreen, modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                    } else {
                        Text("${clinics.size} clinics found", fontSize = 12.sp, color = DermaGreen, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            if (showMap) {
                AndroidView(
                    factory = { ctx ->
                        Configuration.getInstance().apply {
                            userAgentValue = ctx.packageName
                            osmdroidBasePath = ctx.cacheDir
                            osmdroidTileCache = java.io.File(ctx.cacheDir, "osmdroid")
                        }
                        MapView(ctx).apply {
                            val cartoTiles = XYTileSource(
                                "CartoDB",
                                0, 19, 256, ".png",
                                arrayOf(
                                    "https://a.basemaps.cartocdn.com/light_all/",
                                    "https://b.basemaps.cartocdn.com/light_all/",
                                    "https://c.basemaps.cartocdn.com/light_all/"
                                ),
                                "© CartoDB © OpenStreetMap contributors"
                            )
                            setTileSource(cartoTiles)
                            setMultiTouchControls(true)
                            controller.setZoom(14.5)
                            controller.setCenter(GeoPoint(userLat, userLng))
                        }
                    },
                    update = { mapView ->
                        val ctx = mapView.context
                        val dp = ctx.resources.displayMetrics.density
                        val markerIcon = createMarkerDrawable(ctx)
                        val userDot = createUserDotDrawable(ctx)
                        mapView.overlays.clear()
                        if (!mapCentered) {
                            mapView.controller.setCenter(GeoPoint(userLat, userLng))
                            mapCentered = true
                        }
                        // User location dot
                        val userMarker = Marker(mapView).apply {
                            position = GeoPoint(userLat, userLng)
                            title = "You are here"
                            icon = userDot
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        }
                        mapView.overlays.add(userMarker)
                        // Road route + clinic marker for each clinic
                        clinics.forEach { clinic ->
                            val routePoints = routes[clinic.name]
                                ?: listOf(GeoPoint(userLat, userLng), GeoPoint(clinic.lat, clinic.lng))
                            val route = Polyline(mapView).apply {
                                setPoints(routePoints)
                                outlinePaint.color = 0xFF7C3AED.toInt()
                                outlinePaint.strokeWidth = 4f * dp
                                outlinePaint.alpha = 200
                            }
                            mapView.overlays.add(route)
                            val marker = Marker(mapView).apply {
                                position = GeoPoint(clinic.lat, clinic.lng)
                                title = clinic.name
                                snippet = clinic.address
                                icon = markerIcon
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            }
                            mapView.overlays.add(marker)
                        }
                        mapView.invalidate()
                    },
                    modifier = Modifier.fillMaxWidth().height(300.dp)
                )

                // Clinic cards below map
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Text("Nearby Dermatology Clinics", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1a1a1a))
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    items(clinics) { clinic ->
                        CompactClinicCard(clinic = clinic, onClick = { selectedClinic = clinic })
                    }
                }
            } else {
                // List View
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(clinics) { clinic ->
                        FullClinicCard(clinic = clinic, onClick = { selectedClinic = clinic })
                    }
                }
            }
        }
    }

    selectedClinic?.let { clinic ->
        AlertDialog(
            onDismissRequest = { selectedClinic = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(DermaGreenLight), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.LocalHospital, contentDescription = null, tint = DermaGreen, modifier = Modifier.size(22.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(clinic.name, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    DetailRow(icon = Icons.Default.LocationOn, text = clinic.address)
                    DetailRow(icon = Icons.Default.AccessTime, text = clinic.hours)
                    DetailRow(icon = Icons.Default.Phone, text = clinic.phone)
                    DetailRow(icon = Icons.Default.Star, text = "${clinic.rating} / 5.0 rating")
                    DetailRow(icon = Icons.Default.Circle, text = if (clinic.openNow) "Open Now" else "Closed", textColor = if (clinic.openNow) Color(0xFF2E7D32) else Color(0xFFC62828))
                }
            },
            confirmButton = {
                Button(onClick = { selectedClinic = null }, colors = ButtonDefaults.buttonColors(containerColor = DermaGreen), shape = RoundedCornerShape(8.dp)) {
                    Text("Close")
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = Color.White,
            titleContentColor = Color(0xFF111827),
            textContentColor = Color(0xFF374151)
        )
    }
}

@Composable
fun CompactClinicCard(clinic: Clinic, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(DermaGreenLight), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.LocalHospital, contentDescription = null, tint = DermaGreen, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(clinic.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1a1a1a))
                Text(clinic.distance, fontSize = 12.sp, color = Color.Gray)
            }
            Box(modifier = Modifier.background(if (clinic.openNow) Color(0xFFE8F5E9) else Color(0xFFFFEBEE), RoundedCornerShape(20.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                Text(if (clinic.openNow) "Open" else "Closed", fontSize = 11.sp, color = if (clinic.openNow) Color(0xFF2E7D32) else Color(0xFFC62828), fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun FullClinicCard(clinic: Clinic, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(DermaGreenLight), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.LocalHospital, contentDescription = null, tint = DermaGreen, modifier = Modifier.size(26.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(clinic.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1a1a1a))
                    Text(clinic.address, fontSize = 12.sp, color = Color.Gray)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.background(Color(0xFFF0F0F0), RoundedCornerShape(20.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(clinic.distance, fontSize = 12.sp, color = Color.Gray)
                    }
                }
                Box(modifier = Modifier.background(Color(0xFFFFF9C4), RoundedCornerShape(20.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFF9A825), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("${clinic.rating}", fontSize = 12.sp, color = Color(0xFFF9A825), fontWeight = FontWeight.SemiBold)
                    }
                }
                Box(modifier = Modifier.background(if (clinic.openNow) Color(0xFFE8F5E9) else Color(0xFFFFEBEE), RoundedCornerShape(20.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                    Text(if (clinic.openNow) "Open Now" else "Closed", fontSize = 12.sp, color = if (clinic.openNow) Color(0xFF2E7D32) else Color(0xFFC62828), fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AccessTime, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(clinic.hours, fontSize = 12.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
fun DetailRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, textColor: Color = Color(0xFF444444)) {
    Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = DermaGreen, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, fontSize = 13.sp, color = textColor)
    }
}