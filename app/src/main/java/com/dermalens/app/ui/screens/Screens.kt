package com.dermalens.app.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.dermalens.app.data.db.DermaDatabase
import com.dermalens.app.data.model.User
import com.dermalens.app.navigation.Screen
import kotlinx.coroutines.launch
import java.security.MessageDigest

// ── Brand colors ──────────────────────────────────────────────────────────────
val DermaGreen = Color(0xFF1A7A6E)
val DermaGreenLight = Color(0xFFE6F4F2)
val DermaGreenDark = Color(0xFF145F56)

// ── Password hashing ──────────────────────────────────────────────────────────
fun hashPassword(password: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val hashBytes = digest.digest(password.toByteArray())
    return hashBytes.joinToString("") { "%02x".format(it) }
}

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

// ── Bottom Nav Bar ────────────────────────────────────────────────────────────
@Composable
fun DermaBottomNavBar(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 8.dp
    ) {
        bottomNavItems.forEach { item ->
            val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = if (selected) DermaGreen else Color.Gray
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        color = if (selected) DermaGreen else Color.Gray,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                    )
                },
                selected = selected,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = DermaGreenLight
                )
            )
        }
    }
}

// ── Scanning Tips ─────────────────────────────────────────────────────────────
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

// ── Home Screen ───────────────────────────────────────────────────────────────
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
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(DermaGreen, DermaGreenDark)
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
                Column {
                    Text("Welcome back! 👋", fontSize = 14.sp, color = Color.White.copy(alpha = 0.85f))
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("DermaLens", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Your personal skin health companion", fontSize = 12.sp, color = Color.White.copy(alpha = 0.75f))
                }
                Icon(
                    imageVector = Icons.Default.LocalHospital,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.2f),
                    modifier = Modifier
                        .size(80.dp)
                        .align(Alignment.CenterEnd)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Recent Scan Card ──────────────────────────────────────────────
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text("Recent Scan", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1a1a1a))
                Spacer(modifier = Modifier.height(10.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(DermaGreenLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null, tint = DermaGreen, modifier = Modifier.size(28.dp))
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("No scans yet", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1a1a1a))
                            Text("Start your first skin scan today!", fontSize = 13.sp, color = Color.Gray)
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Quick Actions ─────────────────────────────────────────────────
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text("Quick Actions", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1a1a1a))
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionCard(
                        icon = Icons.Default.CameraAlt,
                        label = "Scan Skin",
                        color = DermaGreen,
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate(Screen.Scan.route) }
                    )
                    QuickActionCard(
                        icon = Icons.Default.LocationOn,
                        label = "Find Clinics",
                        color = Color(0xFF2196F3),
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate(Screen.ClinicLocator.route) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionCard(
                        icon = Icons.Default.Timeline,
                        label = "Progress",
                        color = Color(0xFF9C27B0),
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate(Screen.ProgressTracker.route) }
                    )
                    QuickActionCard(
                        icon = Icons.Default.MenuBook,
                        label = "Care Guide",
                        color = Color(0xFFFF9800),
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate(Screen.CareGuide.route) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Scanning Tip ──────────────────────────────────────────────────
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text("Tip of the Day", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1a1a1a))
                Spacer(modifier = Modifier.height(10.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DermaGreenLight),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(DermaGreen),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Lightbulb, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(tip, fontSize = 13.sp, color = DermaGreenDark, lineHeight = 20.sp, modifier = Modifier.weight(1f))
                    }
                }
            }

            // ── Disclaimer ────────────────────────────────────────────────────
            Spacer(modifier = Modifier.height(16.dp))
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Text(
                        text = "⚕️ DermaLens is a diagnostic aid only. Always consult a licensed dermatologist for professional advice.",
                        fontSize = 11.sp,
                        color = Color(0xFFE65100),
                        modifier = Modifier.padding(12.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ── Quick Action Card ─────────────────────────────────────────────────────────
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
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(26.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1a1a1a), textAlign = TextAlign.Center)
        }
    }
}

// ── Reusable placeholder composable ──────────────────────────────────────────
@Composable
private fun PlaceholderScreen(
    title: String,
    subtitle: String,
    icon: ImageVector,
    navController: NavController,
    showBottomNav: Boolean = true,
    content: @Composable ColumnScope.() -> Unit = {}
) {
    Scaffold(
        bottomBar = { if (showBottomNav) DermaBottomNavBar(navController) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(72.dp), tint = DermaGreen)
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = title, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = subtitle, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(32.dp))
            content()
        }
    }
}

// ── Login Screen ──────────────────────────────────────────────────────────────
@Composable
fun LoginScreen(navController: NavController) {
    val context = LocalContext.current
    val db = remember { DermaDatabase.getDatabase(context) }
    val scope = rememberCoroutineScope()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf("") }
    var loginError by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    fun validate(): Boolean {
        var valid = true
        if (email.isBlank() || !email.contains("@")) { emailError = "Please enter a valid email address"; valid = false } else emailError = ""
        if (password.length < 6) { passwordError = "Password must be at least 6 characters"; valid = false } else passwordError = ""
        return valid
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 28.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(48.dp))
            Icon(Icons.Default.LocalHospital, contentDescription = null, modifier = Modifier.size(72.dp), tint = DermaGreen)
            Spacer(modifier = Modifier.height(12.dp))
            Text("DermaLens", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = DermaGreen)
            Text("Skin health in your hands", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(40.dp))

            if (loginError.isNotEmpty()) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(loginError, color = MaterialTheme.colorScheme.error, fontSize = 13.sp, modifier = Modifier.padding(12.dp))
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            OutlinedTextField(
                value = email, onValueChange = { email = it; emailError = ""; loginError = "" },
                label = { Text("Email address") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                isError = emailError.isNotEmpty(),
                supportingText = { if (emailError.isNotEmpty()) Text(emailError, color = MaterialTheme.colorScheme.error) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DermaGreen, focusedLabelColor = DermaGreen, focusedLeadingIconColor = DermaGreen)
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = password, onValueChange = { password = it; passwordError = ""; loginError = "" },
                label = { Text("Password") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                trailingIcon = { IconButton(onClick = { passwordVisible = !passwordVisible }) { Icon(if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null) } },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                isError = passwordError.isNotEmpty(),
                supportingText = { if (passwordError.isNotEmpty()) Text(passwordError, color = MaterialTheme.colorScheme.error) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DermaGreen, focusedLabelColor = DermaGreen, focusedLeadingIconColor = DermaGreen)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    if (validate()) {
                        isLoading = true
                        scope.launch {
                            val user = db.userDao().login(email.trim(), hashPassword(password))
                            isLoading = false
                            if (user != null) {
                                navController.navigate(Screen.Home.route) { popUpTo(Screen.Login.route) { inclusive = true } }
                            } else {
                                loginError = "Incorrect email or password. Please try again."
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DermaGreen),
                enabled = !isLoading
            ) {
                if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                else Text("Login", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Text("Don't have an account? ", color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = { navController.navigate(Screen.Register.route) }) { Text("Register", color = DermaGreen, fontWeight = FontWeight.Bold) }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Card(colors = CardDefaults.cardColors(containerColor = DermaGreenLight), shape = RoundedCornerShape(8.dp)) {
                Text("⚕️ DermaLens is a diagnostic aid only and does not replace professional medical advice. Always consult a licensed dermatologist.", fontSize = 11.sp, color = DermaGreen, modifier = Modifier.padding(12.dp), textAlign = TextAlign.Center)
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ── Register Screen ───────────────────────────────────────────────────────────
@Composable
fun RegisterScreen(navController: NavController) {
    val context = LocalContext.current
    val db = remember { DermaDatabase.getDatabase(context) }
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var nameError by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf("") }
    var confirmPasswordError by remember { mutableStateOf("") }
    var registerError by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    fun validate(): Boolean {
        var valid = true
        if (name.isBlank()) { nameError = "Name is required"; valid = false } else nameError = ""
        if (email.isBlank() || !email.contains("@")) { emailError = "Enter a valid email"; valid = false } else emailError = ""
        if (password.length < 6) { passwordError = "Password must be at least 6 characters"; valid = false } else passwordError = ""
        if (confirmPassword != password) { confirmPasswordError = "Passwords do not match"; valid = false } else confirmPasswordError = ""
        return valid
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 28.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))
            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(64.dp), tint = DermaGreen)
            Spacer(modifier = Modifier.height(12.dp))
            Text("Create Account", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = DermaGreen)
            Text("Join DermaLens today", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(32.dp))

            if (registerError.isNotEmpty()) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(registerError, color = MaterialTheme.colorScheme.error, fontSize = 13.sp, modifier = Modifier.padding(12.dp))
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            OutlinedTextField(value = name, onValueChange = { name = it; nameError = "" }, label = { Text("Full name") }, leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }, isError = nameError.isNotEmpty(), supportingText = { if (nameError.isNotEmpty()) Text(nameError, color = MaterialTheme.colorScheme.error) }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DermaGreen, focusedLabelColor = DermaGreen, focusedLeadingIconColor = DermaGreen))
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(value = email, onValueChange = { email = it; emailError = ""; registerError = "" }, label = { Text("Email address") }, leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) }, isError = emailError.isNotEmpty(), supportingText = { if (emailError.isNotEmpty()) Text(emailError, color = MaterialTheme.colorScheme.error) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DermaGreen, focusedLabelColor = DermaGreen, focusedLeadingIconColor = DermaGreen))
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(value = password, onValueChange = { password = it; passwordError = "" }, label = { Text("Password") }, leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) }, trailingIcon = { IconButton(onClick = { passwordVisible = !passwordVisible }) { Icon(if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null) } }, visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(), isError = passwordError.isNotEmpty(), supportingText = { if (passwordError.isNotEmpty()) Text(passwordError, color = MaterialTheme.colorScheme.error) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DermaGreen, focusedLabelColor = DermaGreen, focusedLeadingIconColor = DermaGreen))
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(value = confirmPassword, onValueChange = { confirmPassword = it; confirmPasswordError = "" }, label = { Text("Confirm password") }, leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) }, trailingIcon = { IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) { Icon(if (confirmPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null) } }, visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(), isError = confirmPasswordError.isNotEmpty(), supportingText = { if (confirmPasswordError.isNotEmpty()) Text(confirmPasswordError, color = MaterialTheme.colorScheme.error) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DermaGreen, focusedLabelColor = DermaGreen, focusedLeadingIconColor = DermaGreen))
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (validate()) {
                        isLoading = true
                        scope.launch {
                            val emailTaken = db.userDao().emailExists(email.trim()) > 0
                            if (emailTaken) { registerError = "An account with this email already exists."; isLoading = false }
                            else {
                                db.userDao().insertUser(User(fullName = name.trim(), email = email.trim(), passwordHash = hashPassword(password)))
                                isLoading = false
                                navController.navigate(Screen.Home.route) { popUpTo(Screen.Register.route) { inclusive = true } }
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DermaGreen),
                enabled = !isLoading
            ) {
                if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                else Text("Create Account", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Text("Already have an account? ", color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = { navController.popBackStack() }) { Text("Login", color = DermaGreen, fontWeight = FontWeight.Bold) }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ── Progress Tracker Screen ───────────────────────────────────────────────────
@Composable
fun ProgressTrackerScreen(navController: NavController) {
    PlaceholderScreen(title = "Progress Tracker", subtitle = "Your scan history and trends", icon = Icons.Default.Timeline, navController = navController) {
        OutlinedButton(onClick = { navController.popBackStack() }, modifier = Modifier.fillMaxWidth()) { Text("Back") }
    }
}

// ── Profile Screen ────────────────────────────────────────────────────────────
@Composable
fun ProfileScreen(navController: NavController) {
    PlaceholderScreen(title = "My Profile", subtitle = "Manage your account", icon = Icons.Default.Person, navController = navController) {
        Button(onClick = { navController.navigate(Screen.EditProfile.route) }, modifier = Modifier.fillMaxWidth()) { Text("Edit Profile") }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(onClick = { navController.navigate(Screen.Login.route) { popUpTo(Screen.Home.route) { inclusive = true } } }, modifier = Modifier.fillMaxWidth()) { Text("Logout") }
    }
}

// ── Edit Profile Screen ───────────────────────────────────────────────────────
@Composable
fun EditProfileScreen(navController: NavController) {
    PlaceholderScreen(title = "Edit Profile", subtitle = "Update your information", icon = Icons.Default.Edit, navController = navController, showBottomNav = false) {
        Button(onClick = { navController.popBackStack() }, modifier = Modifier.fillMaxWidth()) { Text("Save Changes") }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(onClick = { navController.popBackStack() }, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
    }
}