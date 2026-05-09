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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.dermalens.app.navigation.Screen

// ── Profile Screen ────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController) {
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var notificationsEnabled by remember { mutableStateOf(true) }

    // Mock user data (will be replaced with real Room DB data)
    val userName = "Mark Joseph Garcia"
    val userEmail = "mjgar@tsu.edu.ph"
    val memberSince = "May 2026"
    val totalScans = 6
    val conditions = 3

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Profile", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color(0xFF1a1a1a)
                )
            )
        },
        bottomBar = { DermaBottomNavBar(navController) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .background(Color(0xFFF8F9FA))
        ) {
            // ── Profile Header ────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(DermaGreen, DermaGreenDark)
                        )
                    )
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Avatar
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                            .border(3.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = userName.split(" ").take(2).map { it.first() }.joinToString(""),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(userName, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(userEmail, fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f))
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text("Member since $memberSince", fontSize = 11.sp, color = Color.White)
                    }
                }
            }

            // ── Stats Row ─────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ProfileStatItem(value = "$totalScans", label = "Total Scans", icon = "📷")
                Divider(modifier = Modifier.height(40.dp).width(1.dp), color = Color(0xFFEEEEEE))
                ProfileStatItem(value = "$conditions", label = "Conditions", icon = "🔍")
                Divider(modifier = Modifier.height(40.dp).width(1.dp), color = Color(0xFFEEEEEE))
                ProfileStatItem(value = "12", label = "Days Active", icon = "📅")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Account Section ───────────────────────────────────────────────
            ProfileSectionHeader(title = "Account")

            ProfileMenuItem(
                icon = Icons.Default.Edit,
                iconBg = DermaGreenLight,
                iconTint = DermaGreen,
                title = "Edit Profile",
                subtitle = "Update your name and email",
                onClick = { navController.navigate(Screen.EditProfile.route) }
            )

            ProfileMenuItem(
                icon = Icons.Default.Notifications,
                iconBg = Color(0xFFE3F2FD),
                iconTint = Color(0xFF1565C0),
                title = "Scan Reminders",
                subtitle = if (notificationsEnabled) "Reminders are ON" else "Reminders are OFF",
                trailingContent = {
                    Switch(
                        checked = notificationsEnabled,
                        onCheckedChange = { notificationsEnabled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = DermaGreen, checkedTrackColor = DermaGreenLight)
                    )
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── App Section ───────────────────────────────────────────────────
            ProfileSectionHeader(title = "App")

            ProfileMenuItem(
                icon = Icons.Default.History,
                iconBg = Color(0xFFF3E5F5),
                iconTint = Color(0xFF6A1B9A),
                title = "Scan History",
                subtitle = "View all your past scans",
                onClick = { navController.navigate(Screen.ProgressTracker.route) }
            )

            ProfileMenuItem(
                icon = Icons.Default.MenuBook,
                iconBg = Color(0xFFFFF3E0),
                iconTint = Color(0xFFE65100),
                title = "Care Guide",
                subtitle = "Skincare tips for all conditions",
                onClick = { navController.navigate(Screen.CareGuide.route) }
            )

            ProfileMenuItem(
                icon = Icons.Default.LocationOn,
                iconBg = Color(0xFFE8F5E9),
                iconTint = Color(0xFF2E7D32),
                title = "Find Clinics",
                subtitle = "Locate nearby dermatologists",
                onClick = { navController.navigate(Screen.ClinicLocator.route) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── About Section ─────────────────────────────────────────────────
            ProfileSectionHeader(title = "About")

            ProfileMenuItem(
                icon = Icons.Default.Info,
                iconBg = Color(0xFFE3F2FD),
                iconTint = Color(0xFF1565C0),
                title = "About DermaLens",
                subtitle = "Version 1.0.0 — Capstone Project 2026",
                onClick = { showAboutDialog = true }
            )

            ProfileMenuItem(
                icon = Icons.Default.Shield,
                iconBg = Color(0xFFE8F5E9),
                iconTint = Color(0xFF2E7D32),
                title = "Privacy Policy",
                subtitle = "How we handle your data",
                onClick = { }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Logout Button ─────────────────────────────────────────────────
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clickable { showLogoutDialog = true },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Logout, contentDescription = null, tint = Color(0xFFC62828), modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Logout", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFC62828))
                }
            }

            // ── Disclaimer ────────────────────────────────────────────────────
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "⚕️ DermaLens is a capstone project by Tarlac State University.\nFor educational and research purposes only.",
                fontSize = 11.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                lineHeight = 16.sp
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // ── Logout Dialog ─────────────────────────────────────────────────────────
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            icon = { Icon(Icons.Default.Logout, contentDescription = null, tint = Color(0xFFC62828)) },
            title = { Text("Logout", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to logout from DermaLens?") },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Logout")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showLogoutDialog = false },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    // ── About Dialog ──────────────────────────────────────────────────────────
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            icon = { Icon(Icons.Default.LocalHospital, contentDescription = null, tint = DermaGreen) },
            title = { Text("DermaLens", fontWeight = FontWeight.Bold, color = DermaGreen) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Version 1.0.0", fontSize = 13.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "DermaLens is an Android-based skin disease detection system using YOLOv11 TFLite. Developed as a Capstone Project at Tarlac State University, 2026.",
                        fontSize = 13.sp,
                        color = Color(0xFF444444),
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Developed by:", fontSize = 12.sp, color = Color.Gray)
                    Text("Mark Joseph Garcia", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text("Reynaldo Manio Jr.", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text("Reicee Owen Pastrana", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text("Chrisent Dayniel Tolentino", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            },
            confirmButton = {
                Button(
                    onClick = { showAboutDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = DermaGreen),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Close")
                }
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
                title = { Text("Edit Profile", fontWeight = FontWeight.Bold) },
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
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Avatar
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(DermaGreenLight)
                    .border(3.dp, DermaGreen, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = name.split(" ").take(2).map { it.first() }.joinToString(""),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = DermaGreen
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Name field
            OutlinedTextField(
                value = name,
                onValueChange = { name = it; isSaved = false },
                label = { Text("Full name") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DermaGreen,
                    focusedLabelColor = DermaGreen,
                    focusedLeadingIconColor = DermaGreen
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Email field
            OutlinedTextField(
                value = email,
                onValueChange = { email = it; isSaved = false },
                label = { Text("Email address") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DermaGreen,
                    focusedLabelColor = DermaGreen,
                    focusedLeadingIconColor = DermaGreen
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { isSaved = true },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSaved) Color(0xFF4CAF50) else DermaGreen
                )
            ) {
                Icon(
                    if (isSaved) Icons.Default.Check else Icons.Default.Save,
                    contentDescription = null,
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (isSaved) "Saved!" else "Save Changes",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cancel")
            }
        }
    }
}

// ── Helper Composables ────────────────────────────────────────────────────────
@Composable
fun ProfileSectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = Color.Gray,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
    )
}

@Composable
fun ProfileStatItem(value: String, label: String, icon: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(icon, fontSize = 20.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = DermaGreen)
        Text(label, fontSize = 11.sp, color = Color.Gray)
    }
}

@Composable
fun ProfileMenuItem(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit = {},
    trailingContent: @Composable (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 3.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1a1a1a))
                Text(subtitle, fontSize = 12.sp, color = Color.Gray)
            }
            if (trailingContent != null) {
                trailingContent()
            } else {
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(20.dp))
            }
        }
    }
}