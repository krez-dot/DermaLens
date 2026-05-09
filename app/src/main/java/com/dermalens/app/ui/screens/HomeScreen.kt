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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.dermalens.app.navigation.Screen

// ── Bottom Nav Items ──────────────────────────────────────────────────────────
sealed class BottomNavItem(val route: String, val icon: ImageVector, val label: String) {
    object Home : BottomNavItem(Screen.Home.route, Icons.Default.Home, "Home")
    object Scan : BottomNavItem(Screen.Scan.route, Icons.Default.CameraAlt, "Scan")
    object Progress : BottomNavItem(Screen.ProgressTracker.route, Icons.Default.Timeline, "Progress")
    object Profile : BottomNavItem(Screen.Profile.route, Icons.Default.Person, "Profile")
}

val bottomNavItems = listOf(
    BottomNavItem.Home,
    BottomNavItem.Scan,
    BottomNavItem.Progress,
    BottomNavItem.Profile
)

@Composable
fun DermaBottomNavBar(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar(containerColor = Color.White, tonalElevation = 8.dp) {
        bottomNavItems.forEach { item ->
            val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.label, tint = if (selected) DermaGreen else Color(0xFF9CA3AF)) },
                label = { Text(item.label, color = if (selected) DermaGreen else Color(0xFF9CA3AF), fontSize = 11.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal) },
                selected = selected,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                colors = NavigationBarItemDefaults.colors(indicatorColor = DermaGreenLight)
            )
        }
    }
}

val scanningTips = listOf(
    "💡 Use natural lighting when scanning your skin for best results.",
    "📏 Hold your phone 15–20 cm away from the affected area.",
    "🧴 Always consult a licensed dermatologist for proper diagnosis.",
    "🔍 Clean the camera lens before scanning for clearer images.",
    "☀️ Avoid scanning in direct sunlight — find a well-lit indoor area.",
    "📸 Keep your hand steady while capturing — blurry images reduce accuracy.",
    "🧼 Wash and dry the skin area before scanning for best detection.",
    "🔄 Scan the same area multiple times to get consistent results."
)

@Composable
fun HomeScreen(navController: NavController) {
    val tipIndex = remember { (scanningTips.indices).random() }
    val tip = scanningTips[tipIndex]

    Scaffold(
        bottomBar = { DermaBottomNavBar(navController) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .background(Color(0xFFF8F9FA))
        ) {
            // ── Header ────────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(colors = listOf(DermaGreen, DermaGreenDark)))
                    .padding(horizontal = 20.dp, vertical = 28.dp)
            ) {
                Column {
                    Text("Welcome back! 👋", fontSize = 14.sp, color = Color.White.copy(alpha = 0.85f))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("DermaLens", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Your personal skin health companion", fontSize = 13.sp, color = Color.White.copy(alpha = 0.75f))
                }
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.15f))
                        .align(Alignment.CenterEnd),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.LocalHospital, contentDescription = null, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(40.dp))
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Recent Scan ───────────────────────────────────────────────────
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text("Recent Scan", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111827))
                Spacer(modifier = Modifier.height(10.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(DermaGreenLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.DocumentScanner, contentDescription = null, tint = DermaGreen, modifier = Modifier.size(26.dp))
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("No scans yet", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF111827))
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("Start your first skin scan today!", fontSize = 12.sp, color = Color(0xFF6B7280))
                        }
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(DermaGreenLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = DermaGreen, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Quick Actions ─────────────────────────────────────────────────
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text("Quick Actions", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111827))
                Spacer(modifier = Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    QuickActionCard(icon = Icons.Default.CameraAlt, label = "Scan Skin", color = DermaGreen, modifier = Modifier.weight(1f), onClick = { navController.navigate(Screen.Scan.route) })
                    QuickActionCard(icon = Icons.Default.LocationOn, label = "Find Clinics", color = Color(0xFF0284C7), modifier = Modifier.weight(1f), onClick = { navController.navigate(Screen.ClinicLocator.route) })
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    QuickActionCard(icon = Icons.Default.Timeline, label = "Progress", color = Color(0xFF7C3AED), modifier = Modifier.weight(1f), onClick = { navController.navigate(Screen.ProgressTracker.route) })
                    QuickActionCard(icon = Icons.Default.MenuBook, label = "Care Guide", color = Color(0xFFD97706), modifier = Modifier.weight(1f), onClick = { navController.navigate(Screen.CareGuide.route) })
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Tip of the Day ────────────────────────────────────────────────
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text("Tip of the Day", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111827))
                Spacer(modifier = Modifier.height(10.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DermaGreenLight),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                        Box(
                            modifier = Modifier.size(38.dp).clip(CircleShape).background(DermaGreen),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Lightbulb, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(tip, fontSize = 13.sp, color = DermaGreenDark, lineHeight = 20.sp, modifier = Modifier.weight(1f))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Disclaimer ────────────────────────────────────────────────────
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7ED))
                ) {
                    Text(
                        text = "⚕️ DermaLens is a diagnostic aid only. Always consult a licensed dermatologist for professional advice.",
                        fontSize = 11.sp, color = Color(0xFF92400E),
                        modifier = Modifier.padding(12.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun QuickActionCard(
    icon: ImageVector,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(26.dp))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF111827), textAlign = TextAlign.Center)
        }
    }
}