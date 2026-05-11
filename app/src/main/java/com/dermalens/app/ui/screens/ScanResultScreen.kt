package com.dermalens.app.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.dermalens.app.navigation.Screen
import com.dermalens.app.ui.LocalAppSettings
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.dermalens.app.data.db.DermaDatabase
import com.dermalens.app.data.model.ScanRecord
import com.dermalens.app.ui.screens.DermaPrefs
import kotlinx.coroutines.launch

data class DetectionResult(
    val condition: String,
    val confidence: Float,
    val severity: String,
    val description: String,
    val symptoms: List<String>,
    val recommendation: String,
    val color: Color
)

val mockDetectionResults = listOf(
    DetectionResult("Acne Vulgaris", 94.3f, "Moderate", "Acne vulgaris is a common skin condition that occurs when hair follicles become clogged with oil and dead skin cells, causing whiteheads, blackheads, or pimples.", listOf("Whiteheads", "Blackheads", "Papules", "Pustules"), "Consult a dermatologist. Use gentle cleansers and avoid picking or squeezing affected areas.", Color(0xFFE53935)),
    DetectionResult("Atopic Dermatitis", 87.6f, "Mild", "Atopic dermatitis (eczema) is a condition that makes your skin red and itchy. It is common in children but can occur at any age.", listOf("Dry skin", "Itching", "Red patches", "Skin flaking"), "Keep skin moisturized. Avoid known triggers. Consult a dermatologist for topical treatments.", Color(0xFFFF9800)),
    DetectionResult("Melasma", 91.2f, "Mild", "Melasma is a skin condition presenting as brown or blue-gray patches, usually on the face. It is associated with hormonal changes and sun exposure.", listOf("Brown patches", "Facial discoloration", "Symmetrical patches"), "Use broad-spectrum sunscreen daily. Avoid sun exposure. Consult a dermatologist for treatment options.", Color(0xFF795548)),
    DetectionResult("Tinea", 89.5f, "Moderate", "Tinea is a fungal infection of the skin. It can affect different parts of the body and is usually characterized by a ring-shaped rash.", listOf("Ring-shaped rash", "Itching", "Scaly skin", "Redness"), "Use antifungal cream as prescribed. Keep skin dry and clean. Consult a dermatologist.", Color(0xFF4CAF50)),
    DetectionResult("Warts", 96.1f, "Mild", "Warts are small growths caused by the human papillomavirus (HPV). They can appear anywhere on the body and are usually harmless.", listOf("Small flesh-colored bumps", "Rough texture", "Black dots"), "Avoid touching or scratching warts. Consult a dermatologist for removal options.", Color(0xFF9C27B0)),
    DetectionResult("Scabies", 88.4f, "Severe", "Scabies is an itchy skin condition caused by a tiny burrowing mite. The intense itching associated with scabies is an allergic reaction to the mite.", listOf("Intense itching", "Thin burrow tracks", "Rash", "Sores"), "Seek immediate medical attention. Treatment requires prescription medication. Wash all clothing and bedding.", Color(0xFFF44336))
)

fun getSeverityColor(severity: String): Color {
    return when (severity) {
        "Mild" -> Color(0xFF16A34A)
        "Moderate" -> Color(0xFFD97706)
        "Severe" -> Color(0xFFDC2626)
        else -> Color.Gray
    }
}

fun getSeverityBg(severity: String): Color {
    return when (severity) {
        "Mild" -> Color(0xFFDCFCE7)
        "Moderate" -> Color(0xFFFEF3C7)
        "Severe" -> Color(0xFFFEE2E2)
        else -> Color(0xFFF3F4F6)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanResultScreen(navController: NavController) {
    val result = remember { mockDetectionResults.random() }
    var isSaved by remember { mutableStateOf(false) }
    val settings = LocalAppSettings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan Result", fontWeight = FontWeight.Bold, fontSize = settings.textXl.sp) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigate(Screen.Home.route) { popUpTo(Screen.Home.route) { inclusive = false } } }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Go back to home")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White, titleContentColor = Color(0xFF111827))
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF8F9FA))
                .verticalScroll(rememberScrollState())
        ) {
            // Detection Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(colors = listOf(result.color.copy(alpha = 0.9f), result.color)))
                    .padding(24.dp)
                    .semantics { contentDescription = "Detected condition: ${result.condition}, ${result.severity} severity, ${result.confidence}% confidence" }
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier.size(110.dp).clip(RoundedCornerShape(20.dp)).background(Color.White.copy(alpha = 0.15f)).border(1.5.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(20.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.ImageSearch, contentDescription = "Scan image placeholder", tint = Color.White, modifier = Modifier.size(40.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Scan Image", fontSize = settings.textSm.sp, color = Color.White.copy(alpha = 0.8f))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(result.condition, fontSize = settings.textTitle.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Row(
                            modifier = Modifier.background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(20.dp)).padding(horizontal = 12.dp, vertical = 6.dp).semantics { contentDescription = "${result.confidence}% confidence" },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Verified, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("%.1f%%".format(result.confidence), color = Color.White, fontSize = settings.textBase.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Row(
                            modifier = Modifier.background(getSeverityBg(result.severity), RoundedCornerShape(20.dp)).padding(horizontal = 12.dp, vertical = 6.dp).semantics { contentDescription = "${result.severity} severity" },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(getSeverityColor(result.severity)))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(result.severity, fontSize = settings.textBase.sp, color = getSeverityColor(result.severity), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                // About Card
                ResultCard(icon = Icons.Default.Info, iconBg = DermaGreenLight, iconTint = DermaGreen, title = "About this condition") {
                    Text(result.description, fontSize = settings.textMd.sp, color = Color(0xFF374151), lineHeight = 22.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Symptoms Card
                ResultCard(icon = Icons.Default.List, iconBg = Color(0xFFFEF3C7), iconTint = Color(0xFFD97706), title = "Common Symptoms") {
                    result.symptoms.forEach { symptom ->
                        Row(
                            modifier = Modifier.padding(vertical = 4.dp).semantics { contentDescription = "Symptom: $symptom" },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(result.color))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(symptom, fontSize = settings.textMd.sp, color = Color(0xFF374151))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Recommendation Card
                ResultCard(icon = Icons.Default.Lightbulb, iconBg = DermaGreenLight, iconTint = DermaGreen, title = "Recommendation", bgColor = DermaGreenLight) {
                    Text(result.recommendation, fontSize = settings.textMd.sp, color = DermaGreenDark, lineHeight = 22.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Disclaimer
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFFFFF7ED)).padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text("⚕️", fontSize = settings.textMd.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("This result is AI-generated and for reference only. Always consult a licensed dermatologist for accurate diagnosis.", fontSize = settings.textBase.sp, color = Color(0xFF92400E), lineHeight = 18.sp)
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Button(
                    onClick = { navController.navigate(Screen.CareGuide.route) },
                    modifier = Modifier.fillMaxWidth().height(52.dp).semantics { contentDescription = "View care guide for ${result.condition}" },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DermaGreen)
                ) {
                    Icon(Icons.Default.MenuBook, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("View Care Guide", fontSize = settings.textLg.sp, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        if (!isSaved) {
                            scope.launch {
                                val db = DermaDatabase.getDatabase(context)
                                val prefs = context.getSharedPreferences(DermaPrefs.PREFS_NAME, android.content.Context.MODE_PRIVATE)
                                val savedEmail = prefs.getString(DermaPrefs.KEY_USER_EMAIL, "") ?: ""
                                val user = db.userDao().getUserByEmail(savedEmail)
                                if (user != null) {
                                    db.scanRecordDao().insertScan(
                                        ScanRecord(
                                            userId = user.userId,
                                            condition = result.condition,
                                            confidence = result.confidence,
                                            severity = result.severity,
                                            notes = "Scanned on ${java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date())}"
                                        )
                                    )
                                }
                                isSaved = true
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp).semantics { contentDescription = if (isSaved) "Scan saved to history" else "Save scan to history" },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (isSaved) Color(0xFF16A34A) else Color(0xFF0284C7))
                ) {
                    Icon(if (isSaved) Icons.Default.Check else Icons.Default.BookmarkAdd, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isSaved) "Saved to History!" else "Save to History", fontSize = settings.textLg.sp, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = { navController.navigate(Screen.Scan.route) { popUpTo(Screen.Scan.route) { inclusive = true } } },
                    modifier = Modifier.fillMaxWidth().height(52.dp).semantics { contentDescription = "Scan again" },
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.5.dp, Color(0xFFE5E7EB))
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color(0xFF374151), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Scan Again", fontSize = settings.textLg.sp, color = Color(0xFF374151))
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun ResultCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconBg: Color,
    iconTint: Color,
    title: String,
    bgColor: Color = Color.White,
    content: @Composable ColumnScope.() -> Unit
) {
    val settings = LocalAppSettings.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(if (bgColor == Color.White) 2.dp else 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(iconBg), contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(title, fontSize = settings.textLg.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF111827))
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}