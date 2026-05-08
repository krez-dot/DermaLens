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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.dermalens.app.navigation.Screen

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
    DetectionResult(
        condition = "Acne Vulgaris",
        confidence = 94.3f,
        severity = "Moderate",
        description = "Acne vulgaris is a common skin condition that occurs when hair follicles become clogged with oil and dead skin cells, causing whiteheads, blackheads, or pimples.",
        symptoms = listOf("Whiteheads", "Blackheads", "Papules", "Pustules"),
        recommendation = "Consult a dermatologist. Use gentle cleansers and avoid picking or squeezing affected areas.",
        color = Color(0xFFE53935)
    ),
    DetectionResult(
        condition = "Atopic Dermatitis",
        confidence = 87.6f,
        severity = "Mild",
        description = "Atopic dermatitis (eczema) is a condition that makes your skin red and itchy. It is common in children but can occur at any age.",
        symptoms = listOf("Dry skin", "Itching", "Red patches", "Skin flaking"),
        recommendation = "Keep skin moisturized. Avoid known triggers. Consult a dermatologist for topical treatments.",
        color = Color(0xFFFF9800)
    ),
    DetectionResult(
        condition = "Melasma",
        confidence = 91.2f,
        severity = "Mild",
        description = "Melasma is a skin condition presenting as brown or blue-gray patches, usually on the face. It is associated with hormonal changes and sun exposure.",
        symptoms = listOf("Brown patches", "Facial discoloration", "Symmetrical patches"),
        recommendation = "Use broad-spectrum sunscreen daily. Avoid sun exposure. Consult a dermatologist for treatment options.",
        color = Color(0xFF795548)
    ),
    DetectionResult(
        condition = "Tinea",
        confidence = 89.5f,
        severity = "Moderate",
        description = "Tinea is a fungal infection of the skin. It can affect different parts of the body and is usually characterized by a ring-shaped rash.",
        symptoms = listOf("Ring-shaped rash", "Itching", "Scaly skin", "Redness"),
        recommendation = "Use antifungal cream as prescribed. Keep skin dry and clean. Consult a dermatologist.",
        color = Color(0xFF4CAF50)
    ),
    DetectionResult(
        condition = "Warts",
        confidence = 96.1f,
        severity = "Mild",
        description = "Warts are small growths caused by the human papillomavirus (HPV). They can appear anywhere on the body and are usually harmless.",
        symptoms = listOf("Small flesh-colored bumps", "Rough texture", "Black dots"),
        recommendation = "Avoid touching or scratching warts. Consult a dermatologist for removal options.",
        color = Color(0xFF9C27B0)
    ),
    DetectionResult(
        condition = "Scabies",
        confidence = 88.4f,
        severity = "Severe",
        description = "Scabies is an itchy skin condition caused by a tiny burrowing mite. The intense itching associated with scabies is an allergic reaction to the mite.",
        symptoms = listOf("Intense itching", "Thin burrow tracks", "Rash", "Sores"),
        recommendation = "Seek immediate medical attention. Treatment requires prescription medication. Wash all clothing and bedding.",
        color = Color(0xFFF44336)
    )
)

fun getSeverityColor(severity: String): Color {
    return when (severity) {
        "Mild" -> Color(0xFF4CAF50)
        "Moderate" -> Color(0xFFFF9800)
        "Severe" -> Color(0xFFF44336)
        else -> Color.Gray
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanResultScreen(navController: NavController) {
    val result = remember { mockDetectionResults.random() }
    var isSaved by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan Result", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = false }
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color(0xFF1a1a1a)
                )
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
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(result.color.copy(alpha = 0.85f), result.color)
                        )
                    )
                    .padding(24.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.2f))
                            .border(2.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = Color.White, modifier = Modifier.size(56.dp))
                        Text("Scan Image", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp))
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(text = result.condition, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.background(Color.White.copy(alpha = 0.25f), RoundedCornerShape(20.dp)).padding(horizontal = 14.dp, vertical = 6.dp)) {
                            Text(text = "%.1f%% Confidence".format(result.confidence), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Box(modifier = Modifier.background(getSeverityColor(result.severity).copy(alpha = 0.3f), RoundedCornerShape(20.dp)).border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(20.dp)).padding(horizontal = 14.dp, vertical = 6.dp)) {
                            Text(text = result.severity, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(modifier = Modifier.padding(horizontal = 16.dp)) {

                // About Card
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(DermaGreenLight), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = DermaGreen, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("About this condition", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(result.description, fontSize = 14.sp, color = Color(0xFF444444), lineHeight = 22.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Symptoms Card
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0xFFFFF3E0)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.List, contentDescription = null, tint = Color(0xFFE65100), modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Common Symptoms", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        result.symptoms.forEach { symptom ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 3.dp)) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(result.color))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(symptom, fontSize = 14.sp, color = Color(0xFF444444))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Recommendation Card
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = DermaGreenLight), elevation = CardDefaults.cardElevation(0.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(DermaGreen), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Lightbulb, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Recommendation", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = DermaGreenDark)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(result.recommendation, fontSize = 14.sp, color = DermaGreenDark, lineHeight = 22.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Disclaimer
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)), elevation = CardDefaults.cardElevation(0.dp)) {
                    Text(
                        text = "⚕️ This result is AI-generated and for reference only. Always consult a licensed dermatologist for accurate diagnosis and treatment.",
                        fontSize = 11.sp, color = Color(0xFFE65100), modifier = Modifier.padding(12.dp), textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Button(
                    onClick = { navController.navigate(Screen.CareGuide.route) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DermaGreen)
                ) {
                    Icon(Icons.Default.MenuBook, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("View Care Guide", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { isSaved = true },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (isSaved) Color(0xFF4CAF50) else Color(0xFF185FA5))
                ) {
                    Icon(if (isSaved) Icons.Default.Check else Icons.Default.Save, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isSaved) "Saved to History!" else "Save to History", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = { navController.navigate(Screen.Scan.route) { popUpTo(Screen.Scan.route) { inclusive = true } } },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Scan Again", fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}