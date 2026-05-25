package com.dermalens.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

val mockClinics = listOf(
    Clinic("Tarlac Dermatology Clinic", "Macabulos Dr, Tarlac City, Tarlac", "0.8 km", 4.8f, true, "Mon-Sat: 8:00 AM - 5:00 PM", "+63 45 982 1234", 15.4755, 120.5963),
    Clinic("SkinCare Specialists Tarlac", "F. Tañedo St, Tarlac City, Tarlac", "1.2 km", 4.6f, true, "Mon-Fri: 9:00 AM - 6:00 PM", "+63 45 982 5678", 15.4780, 120.5940),
    Clinic("Tarlac Provincial Hospital - Derma", "San Vicente St, Tarlac City, Tarlac", "2.1 km", 4.3f, true, "Mon-Sun: 8:00 AM - 8:00 PM", "+63 45 982 9012", 15.4720, 120.5990),
    Clinic("Dr. Santos Skin & Laser Clinic", "Romulo Blvd, Tarlac City, Tarlac", "2.8 km", 4.9f, false, "Tue-Sat: 10:00 AM - 7:00 PM", "+63 45 982 3456", 15.4800, 120.5920),
    Clinic("Capampangan Derma Center", "McArthur Hwy, Tarlac City, Tarlac", "3.5 km", 4.5f, true, "Mon-Sat: 9:00 AM - 5:00 PM", "+63 45 982 7890", 15.4690, 120.6010),
)

private fun haversineKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
    val r = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLng = Math.toRadians(lng2 - lng1)
    val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
            Math.sin(dLng / 2) * Math.sin(dLng / 2)
    return r * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
}

private suspend fun fetchNearbyClinics(lat: Double, lng: Double): List<Clinic> {
    return withContext(Dispatchers.IO) {
        try {
            val query = """
                [out:json][timeout:25];
                (
                  node["amenity"~"clinic|hospital|doctors"](around:10000,$lat,$lng);
                  node["healthcare"~"clinic|hospital|doctor"](around:10000,$lat,$lng);
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
                result.add(Clinic(name, addr, "%.1f km".format(dist), 4.5f, true, hours, phone, elLat, elLng))
            }
            result.sortedBy { haversineKm(lat, lng, it.lat, it.lng) }
        } catch (e: Exception) {
            emptyList()
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
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
        val fetched = fetchNearbyClinics(15.4755, 120.5963)
        if (fetched.isNotEmpty()) clinics = fetched
        isLoading = false
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
                Text("Tarlac City, Tarlac", fontSize = 14.sp, color = Color(0xFF444444), fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
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
                        Configuration.getInstance().userAgentValue = ctx.packageName
                        MapView(ctx).apply {
                            setTileSource(TileSourceFactory.MAPNIK)
                            setMultiTouchControls(true)
                            controller.setZoom(14.5)
                            controller.setCenter(GeoPoint(15.4755, 120.5963))
                        }
                    },
                    update = { mapView ->
                        mapView.overlays.clear()
                        clinics.forEach { clinic ->
                            val marker = Marker(mapView).apply {
                                position = GeoPoint(clinic.lat, clinic.lng)
                                title = clinic.name
                                snippet = clinic.address
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
            shape = RoundedCornerShape(16.dp)
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