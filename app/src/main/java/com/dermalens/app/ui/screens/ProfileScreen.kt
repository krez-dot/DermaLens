package com.dermalens.app.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.dermalens.app.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController) {
    val context = LocalContext.current
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var notificationsEnabled by remember { mutableStateOf(true) }

    val userName = "Mark Joseph Garcia"
    val userEmail = "mjgar@tsu.edu.ph"
    val memberSince = "May 2026"
    val totalScans = 6
    val conditions = 3

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Profile", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White, titleContentColor = Color(0xFF111827))
            )
        },
        bottomBar = { DermaBottomNavBar(navController) }
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).verticalScroll(rememberScrollState()).background(Color(0xFFF8F9FA))
        ) {
            // Header
            Box(
                modifier = Modifier.fillMaxWidth().background(Brush.verticalGradient(colors = listOf(DermaGreen, DermaGreenDark))).padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier.size(88.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)).border(2.5.dp, Color.White.copy(alpha = 0.6f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(userName.split(" ").take(2).map { it.first() }.joinToString(""), fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(userName, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(userEmail, fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f))
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier.background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp)).padding(horizontal = 14.dp, vertical = 5.dp)) {
                        Text("Member since $memberSince", fontSize = 12.sp, color = Color.White)
                    }
                }
            }

            // Stats
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).offset(y = (-1).dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    ProfileStatItem("$totalScans", "Total Scans", "📷")
                    VerticalDivider(modifier = Modifier.height(40.dp), color = Color(0xFFF3F4F6))
                    ProfileStatItem("$conditions", "Conditions", "🔍")
                    VerticalDivider(modifier = Modifier.height(40.dp), color = Color(0xFFF3F4F6))
                    ProfileStatItem("12", "Days Active", "📅")
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Account Section
            ProfileSectionHeader("Account")
            Spacer(modifier = Modifier.height(8.dp))
            ProfileMenuCard {
                ProfileMenuItem(icon = Icons.Default.Edit, iconBg = DermaGreenLight, iconTint = DermaGreen, title = "Edit Profile", subtitle = "Update your name and email", onClick = { navController.navigate(Screen.EditProfile.route) })
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFF3F4F6))
                ProfileMenuItemSwitch(icon = Icons.Default.Notifications, iconBg = Color(0xFFEFF6FF), iconTint = Color(0xFF2563EB), title = "Scan Reminders", subtitle = if (notificationsEnabled) "Reminders are ON" else "Reminders are OFF", checked = notificationsEnabled, onCheckedChange = { notificationsEnabled = it })
            }

            Spacer(modifier = Modifier.height(16.dp))

            // App Section
            ProfileSectionHeader("App")
            Spacer(modifier = Modifier.height(8.dp))
            ProfileMenuCard {
                ProfileMenuItem(icon = Icons.Default.History, iconBg = Color(0xFFF5F3FF), iconTint = Color(0xFF7C3AED), title = "Scan History", subtitle = "View all your past scans", onClick = { navController.navigate(Screen.ProgressTracker.route) })
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFF3F4F6))
                ProfileMenuItem(icon = Icons.Default.MenuBook, iconBg = Color(0xFFFEF3C7), iconTint = Color(0xFFD97706), title = "Care Guide", subtitle = "Skincare tips for all conditions", onClick = { navController.navigate(Screen.CareGuide.route) })
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFF3F4F6))
                ProfileMenuItem(icon = Icons.Default.LocationOn, iconBg = Color(0xFFF0FDF4), iconTint = Color(0xFF16A34A), title = "Find Clinics", subtitle = "Locate nearby dermatologists", onClick = { navController.navigate(Screen.ClinicLocator.route) })
            }

            Spacer(modifier = Modifier.height(16.dp))

            // About Section
            ProfileSectionHeader("About")
            Spacer(modifier = Modifier.height(8.dp))
            ProfileMenuCard {
                ProfileMenuItem(icon = Icons.Default.Info, iconBg = Color(0xFFEFF6FF), iconTint = Color(0xFF2563EB), title = "About DermaLens", subtitle = "Version 1.0.0 — Capstone 2026", onClick = { showAboutDialog = true })
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFF3F4F6))
                ProfileMenuItem(icon = Icons.Default.Shield, iconBg = Color(0xFFF0FDF4), iconTint = Color(0xFF16A34A), title = "Privacy Policy", subtitle = "How we handle your data")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Logout
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).clickable { showLogoutDialog = true },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Icon(Icons.Default.Logout, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Logout", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFDC2626))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("⚕️ DermaLens is a capstone project by Tarlac State University.\nFor educational and research purposes only.", fontSize = 11.sp, color = Color(0xFF9CA3AF), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), lineHeight = 16.sp)
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Logout Dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            icon = { Icon(Icons.Default.Logout, contentDescription = null, tint = Color(0xFFDC2626)) },
            title = { Text("Logout", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to logout from DermaLens?", color = Color(0xFF374151)) },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        context.getSharedPreferences(DermaPrefs.PREFS_NAME, android.content.Context.MODE_PRIVATE)
                            .edit()
                            .putBoolean(DermaPrefs.KEY_IS_LOGGED_IN, false)
                            .apply()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(10.dp)
                ) { Text("Logout") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showLogoutDialog = false }, shape = RoundedCornerShape(10.dp)) { Text("Cancel") }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    // About Dialog
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            icon = { Icon(Icons.Default.LocalHospital, contentDescription = null, tint = DermaGreen) },
            title = { Text("DermaLens", fontWeight = FontWeight.Bold, color = DermaGreen) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Version 1.0.0", fontSize = 12.sp, color = Color(0xFF9CA3AF))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("An Android-based skin disease detection system using YOLOv11 TFLite. Developed as a Capstone Project at Tarlac State University, 2026.", fontSize = 13.sp, color = Color(0xFF374151), textAlign = TextAlign.Center, lineHeight = 20.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Developed by:", fontSize = 12.sp, color = Color(0xFF9CA3AF))
                    Spacer(modifier = Modifier.height(4.dp))
                    listOf("Mark Joseph Garcia", "Reynaldo Manio Jr.", "Reicee Owen Pastrana", "Chrisent Dayniel Tolentino").forEach {
                        Text(it, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF111827))
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showAboutDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = DermaGreen), shape = RoundedCornerShape(10.dp)) { Text("Close") }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}

// ── Edit Profile Screen ───────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(navController: NavController) {
    var name by remember { mutableStateOf("Mark Joseph Garcia") }
    var email by remember { mutableStateOf("mjgar@tsu.edu.ph") }
    var isSaved by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White, titleContentColor = Color(0xFF111827))
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier.size(88.dp).clip(CircleShape).background(DermaGreenLight).border(2.5.dp, DermaGreen, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(name.split(" ").take(2).map { it.first() }.joinToString(""), fontSize = 30.sp, fontWeight = FontWeight.Bold, color = DermaGreen)
            }
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedTextField(value = name, onValueChange = { name = it; isSaved = false }, label = { Text("Full name") }, leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DermaGreen, focusedLabelColor = DermaGreen, unfocusedBorderColor = Color(0xFFE5E7EB), unfocusedLabelColor = Color(0xFF9CA3AF), focusedTextColor = Color(0xFF111827), unfocusedTextColor = Color(0xFF111827)))
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(value = email, onValueChange = { email = it; isSaved = false }, label = { Text("Email address") }, leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DermaGreen, focusedLabelColor = DermaGreen, unfocusedBorderColor = Color(0xFFE5E7EB), unfocusedLabelColor = Color(0xFF9CA3AF), focusedTextColor = Color(0xFF111827), unfocusedTextColor = Color(0xFF111827)))
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = { isSaved = true }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = if (isSaved) Color(0xFF16A34A) else DermaGreen)) {
                Icon(if (isSaved) Icons.Default.Check else Icons.Default.Save, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isSaved) "Saved!" else "Save Changes", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(onClick = { navController.popBackStack() }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp), border = BorderStroke(1.5.dp, Color(0xFFE5E7EB))) {
                Text("Cancel", color = Color(0xFF374151))
            }
        }
    }
}

// ── Helper Composables ────────────────────────────────────────────────────────
@Composable
fun ProfileSectionHeader(title: String) {
    Text(title.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF9CA3AF), letterSpacing = 1.sp, modifier = Modifier.padding(horizontal = 16.dp))
}

@Composable
fun ProfileStatItem(value: String, label: String, icon: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(icon, fontSize = 20.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = DermaGreen)
        Text(label, fontSize = 11.sp, color = Color(0xFF6B7280))
    }
}

@Composable
fun ProfileMenuCard(content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
        Column { content() }
    }
}

@Composable
fun ProfileMenuItem(icon: ImageVector, iconBg: Color, iconTint: Color, title: String, subtitle: String, onClick: () -> Unit = {}) {
    Row(modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(iconBg), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF111827))
            Text(subtitle, fontSize = 12.sp, color = Color(0xFF6B7280))
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFFD1D5DB), modifier = Modifier.size(18.dp))
    }
}

@Composable
fun ProfileMenuItemSwitch(icon: ImageVector, iconBg: Color, iconTint: Color, title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(iconBg), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF111827))
            Text(subtitle, fontSize = 12.sp, color = Color(0xFF6B7280))
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = DermaGreen, uncheckedThumbColor = Color.White, uncheckedTrackColor = Color(0xFFE5E7EB)))
    }
}