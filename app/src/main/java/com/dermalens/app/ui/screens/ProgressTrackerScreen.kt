package com.dermalens.app.ui.screens

import androidx.compose.foundation.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.dermalens.app.navigation.Screen

// ── Data Models ───────────────────────────────────────────────────────────────
data class ScanEntry(
    val date: String,
    val severity: String,
    val confidence: Float,
    val notes: String
)

data class ConditionTrack(
    val condition: String,
    val color: Color,
    val emoji: String,
    val scans: List<ScanEntry>
)

// ── Mock Progress Data ────────────────────────────────────────────────────────
val mockProgressData = listOf(
    ConditionTrack(
        condition = "Acne Vulgaris",
        color = Color(0xFFE53935),
        emoji = "🔴",
        scans = listOf(
            ScanEntry("May 1, 2026", "Severe", 94.3f, "Initial scan — widespread breakout"),
            ScanEntry("May 5, 2026", "Moderate", 89.2f, "Slight improvement after treatment"),
            ScanEntry("May 10, 2026", "Mild", 91.5f, "Significant improvement noted"),
        )
    ),
    ConditionTrack(
        condition = "Atopic Dermatitis",
        color = Color(0xFFFF9800),
        emoji = "🟠",
        scans = listOf(
            ScanEntry("Apr 20, 2026", "Moderate", 87.6f, "Flare-up detected on forearm"),
            ScanEntry("Apr 28, 2026", "Mild", 85.1f, "Moisturizer routine helping"),
        )
    ),
    ConditionTrack(
        condition = "Melasma",
        color = Color(0xFF795548),
        emoji = "🟤",
        scans = listOf(
            ScanEntry("May 3, 2026", "Mild", 91.2f, "Brown patches on cheeks detected"),
        )
    )
)

fun getSeverityLevel(severity: String): Int {
    return when (severity) {
        "Mild" -> 1
        "Moderate" -> 2
        "Severe" -> 3
        else -> 0
    }
}

fun getTrendIcon(scans: List<ScanEntry>): String {
    if (scans.size < 2) return "➡️"
    val last = getSeverityLevel(scans.last().severity)
    val prev = getSeverityLevel(scans[scans.size - 2].severity)
    return when {
        last < prev -> "📈 Improving"
        last > prev -> "📉 Worsening"
        else -> "➡️ Stable"
    }
}

fun getTrendColor(scans: List<ScanEntry>): Color {
    if (scans.size < 2) return Color.Gray
    val last = getSeverityLevel(scans.last().severity)
    val prev = getSeverityLevel(scans[scans.size - 2].severity)
    return when {
        last < prev -> Color(0xFF2E7D32)
        last > prev -> Color(0xFFC62828)
        else -> Color.Gray
    }
}

// ── Progress Tracker Screen ───────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressTrackerScreen(navController: NavController) {
    val totalScans = mockProgressData.sumOf { it.scans.size }
    val totalConditions = mockProgressData.size

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Progress Tracker", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color(0xFF1a1a1a)
                )
            )
        },
        bottomBar = { DermaBottomNavBar(navController) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF8F9FA)),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // ── Header Stats ──────────────────────────────────────────────────
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(DermaGreen, DermaGreenDark)
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Text("Your Skin Journey", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Track your progress over time", fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f))
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            StatCard(value = "$totalScans", label = "Total Scans", modifier = Modifier.weight(1f))
                            StatCard(value = "$totalConditions", label = "Conditions", modifier = Modifier.weight(1f))
                            StatCard(value = "12", label = "Days Tracked", modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            // ── Tip Card ──────────────────────────────────────────────────────
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DermaGreenLight)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("💡", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "Scan regularly to track your skin's progress. Consistent monitoring helps you see improvements over time!",
                            fontSize = 12.sp,
                            color = DermaGreenDark,
                            lineHeight = 18.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // ── Condition Tracks ──────────────────────────────────────────────
            item {
                Text(
                    "Condition Timelines",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1a1a1a),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            items(mockProgressData) { track ->
                ConditionTrackCard(
                    track = track,
                    onScanAgain = { navController.navigate(Screen.Scan.route) },
                    onViewGuide = { navController.navigate(Screen.CareGuide.route) }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // ── Start New Scan ────────────────────────────────────────────────
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clickable { navController.navigate(Screen.Scan.route) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DermaGreen),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Start New Scan", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

// ── Stat Card ─────────────────────────────────────────────────────────────────
@Composable
fun StatCard(value: String, label: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(label, fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f), textAlign = TextAlign.Center)
        }
    }
}

// ── Condition Track Card ──────────────────────────────────────────────────────
@Composable
fun ConditionTrackCard(
    track: ConditionTrack,
    onScanAgain: () -> Unit,
    onViewGuide: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(true) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // ── Condition Header ──────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(track.color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(track.emoji, fontSize = 20.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(track.condition, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1a1a1a))
                    Text(
                        getTrendIcon(track.scans),
                        fontSize = 12.sp,
                        color = getTrendColor(track.scans),
                        fontWeight = FontWeight.Medium
                    )
                }
                // Scan count badge
                Box(
                    modifier = Modifier
                        .background(track.color.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("${track.scans.size} scans", fontSize = 11.sp, color = track.color, fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = Color(0xFFEEEEEE))
                Spacer(modifier = Modifier.height(16.dp))

                // ── Timeline ──────────────────────────────────────────────────
                track.scans.forEachIndexed { index, scan ->
                    TimelineNode(
                        scan = scan,
                        isFirst = index == 0,
                        isLast = index == track.scans.size - 1,
                        color = track.color,
                        previousScan = if (index > 0) track.scans[index - 1] else null
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ── Action Buttons ────────────────────────────────────────────
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onScanAgain,
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = track.color)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Scan Again", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    OutlinedButton(
                        onClick = onViewGuide,
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, track.color)
                    ) {
                        Icon(Icons.Default.MenuBook, contentDescription = null, tint = track.color, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Care Guide", fontSize = 13.sp, color = track.color, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

// ── Timeline Node ─────────────────────────────────────────────────────────────
@Composable
fun TimelineNode(
    scan: ScanEntry,
    isFirst: Boolean,
    isLast: Boolean,
    color: Color,
    previousScan: ScanEntry?
) {
    val severityColor = getSeverityColor(scan.severity)
    val improved = previousScan != null &&
            getSeverityLevel(scan.severity) < getSeverityLevel(previousScan.severity)
    val worsened = previousScan != null &&
            getSeverityLevel(scan.severity) > getSeverityLevel(previousScan.severity)

    Row(modifier = Modifier.fillMaxWidth()) {
        // Timeline line + dot
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Top line
            if (!isFirst) {
                Box(modifier = Modifier.width(2.dp).height(8.dp).background(color.copy(alpha = 0.3f)))
            } else {
                Spacer(modifier = Modifier.height(8.dp))
            }
            // Dot
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(if (isLast) color else color.copy(alpha = 0.5f))
                    .border(2.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (isLast) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color.White))
                }
            }
            // Bottom line
            if (!isLast) {
                Box(modifier = Modifier.width(2.dp).height(40.dp).background(color.copy(alpha = 0.3f)))
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Scan info
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = if (!isLast) 4.dp else 0.dp),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isLast) color.copy(alpha = 0.08f) else Color(0xFFF8F9FA)
            ),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(scan.date, fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        // Improvement indicator
                        if (improved) {
                            Text("▲ Better", fontSize = 10.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                        } else if (worsened) {
                            Text("▼ Worse", fontSize = 10.sp, color = Color(0xFFC62828), fontWeight = FontWeight.Bold)
                        }
                        // Severity badge
                        Box(
                            modifier = Modifier
                                .background(severityColor.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(scan.severity, fontSize = 11.sp, color = severityColor, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(scan.notes, fontSize = 12.sp, color = Color(0xFF666666), lineHeight = 16.sp)
                Text("${scan.confidence}% confidence", fontSize = 11.sp, color = Color.Gray)
                if (isLast) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("← Latest scan", fontSize = 11.sp, color = color, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}