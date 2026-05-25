package com.dermalens.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.dermalens.app.data.db.DermaDatabase
import com.dermalens.app.data.model.User
import com.dermalens.app.navigation.Screen
import com.dermalens.app.ui.LocalAppSettings
import kotlinx.coroutines.launch

@Composable
private fun dermaFieldColors(highContrast: Boolean) = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = DermaGreen,
    focusedLabelColor = DermaGreen,
    unfocusedBorderColor = if (highContrast) Color(0xFF000000) else Color(0xFFE5E7EB),
    unfocusedLabelColor = if (highContrast) Color(0xFF1a1a1a) else Color(0xFF9CA3AF),
    focusedTextColor = Color(0xFF111827),
    unfocusedTextColor = Color(0xFF111827)
)

// ── Login Screen ──────────────────────────────────────────────────────────────
@Composable
fun LoginScreen(navController: NavController) {
    val context = LocalContext.current
    val db = remember { DermaDatabase.getDatabase(context) }
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences(DermaPrefs.PREFS_NAME, android.content.Context.MODE_PRIVATE) }
    val settings = LocalAppSettings.current

    var email by remember { mutableStateOf(prefs.getString(DermaPrefs.KEY_REMEMBER_EMAIL, "") ?: "") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf("") }
    var loginError by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(email.isNotEmpty()) }

    val bgColor = settings.background
    val cardBg = if (settings.highContrast) Color(0xFFF0F0F0) else Color(0xFFFEE2E2)

    fun validate(): Boolean {
        var valid = true
        if (email.isBlank() || !email.contains("@")) { emailError = "Please enter a valid email address"; valid = false } else emailError = ""
        if (password.length < 6) { passwordError = "Password must be at least 6 characters"; valid = false } else passwordError = ""
        return valid
    }

    Box(modifier = Modifier.fillMaxSize().background(bgColor)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(64.dp))

            Box(
                modifier = Modifier.size(80.dp).clip(RoundedCornerShape(24.dp))
                    .background(DermaGreenLight)
                    .then(if (settings.highContrast) Modifier.border(2.dp, Color.Black, RoundedCornerShape(24.dp)) else Modifier),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.LocalHospital, contentDescription = "DermaLens logo", tint = DermaGreen, modifier = Modifier.size(44.dp))
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text("Welcome back", fontSize = settings.textDisplay.sp, fontWeight = FontWeight.Bold, color = settings.textPrimary)
            Spacer(modifier = Modifier.height(6.dp))
            Text("Sign in to continue to DermaLens", fontSize = settings.textMd.sp, color = settings.textSecondary, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(40.dp))

            if (loginError.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(cardBg).padding(12.dp).semantics { contentDescription = "Error: $loginError" },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Error, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(loginError, fontSize = settings.textBase.sp, color = Color(0xFFDC2626))
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            OutlinedTextField(
                value = email, onValueChange = { email = it; emailError = ""; loginError = "" },
                label = { Text("Email address") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                isError = emailError.isNotEmpty(),
                supportingText = { if (emailError.isNotEmpty()) Text(emailError, color = Color(0xFFDC2626)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true, modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Email address input field" },
                shape = RoundedCornerShape(14.dp), colors = dermaFieldColors(settings.highContrast)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = password, onValueChange = { password = it; passwordError = ""; loginError = "" },
                label = { Text("Password") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = if (passwordVisible) "Hide password" else "Show password", tint = Color(0xFF9CA3AF))
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                isError = passwordError.isNotEmpty(),
                supportingText = { if (passwordError.isNotEmpty()) Text(passwordError, color = Color(0xFFDC2626)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true, modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Password input field" },
                shape = RoundedCornerShape(14.dp), colors = dermaFieldColors(settings.highContrast)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = rememberMe, onCheckedChange = { rememberMe = it },
                    colors = CheckboxDefaults.colors(checkedColor = DermaGreen, uncheckedColor = if (settings.highContrast) Color.Black else Color(0xFF9CA3AF))
                )
                Text("Remember me", fontSize = settings.textMd.sp, color = settings.textPrimary)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (validate()) {
                        isLoading = true
                        scope.launch {
                            val user = db.userDao().login(email.trim(), hashPassword(password))
                            isLoading = false
                            if (user != null) {
                                prefs.edit().apply {
                                    if (rememberMe) putString(DermaPrefs.KEY_REMEMBER_EMAIL, email.trim()) else remove(DermaPrefs.KEY_REMEMBER_EMAIL)
                                    putBoolean(DermaPrefs.KEY_IS_LOGGED_IN, true)
                                    putString(DermaPrefs.KEY_USER_EMAIL, email.trim())
                                    apply()
                                }
                                navController.navigate(Screen.Home.route) { popUpTo(Screen.Login.route) { inclusive = true } }
                            } else {
                                loginError = "Incorrect email or password. Please try again."
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(54.dp).semantics { contentDescription = "Sign in button" },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DermaGreen),
                enabled = !isLoading
            ) {
                if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                else Text("Sign In", fontSize = settings.textLg.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = if (settings.highContrast) Color.Black else Color(0xFFE5E7EB))
                Text("  or  ", fontSize = settings.textBase.sp, color = settings.textSecondary)
                HorizontalDivider(modifier = Modifier.weight(1f), color = if (settings.highContrast) Color.Black else Color(0xFFE5E7EB))
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Don't have an account?", fontSize = settings.textMd.sp, color = settings.textSecondary)
                TextButton(onClick = { navController.navigate(Screen.Register.route) }) {
                    Text("Sign Up", fontSize = settings.textMd.sp, color = DermaGreen, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            Text("⚕️ For diagnostic reference only. Always consult a licensed dermatologist.", fontSize = settings.textSm.sp, color = settings.textSecondary, textAlign = TextAlign.Center, lineHeight = 16.sp)
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
    val settings = LocalAppSettings.current

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
    var showPrivacyDialog by remember { mutableStateOf(false) }

    fun validate(): Boolean {
        var valid = true
        if (name.isBlank()) { nameError = "Name is required"; valid = false } else nameError = ""
        if (email.isBlank() || !email.contains("@")) { emailError = "Enter a valid email"; valid = false } else emailError = ""
        if (password.length < 6) { passwordError = "Password must be at least 6 characters"; valid = false } else passwordError = ""
        if (confirmPassword != password) { confirmPasswordError = "Passwords do not match"; valid = false } else confirmPasswordError = ""
        return valid
    }

    Box(modifier = Modifier.fillMaxSize().background(settings.background)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(56.dp))

            Box(
                modifier = Modifier.size(80.dp).clip(RoundedCornerShape(24.dp))
                    .background(DermaGreenLight)
                    .then(if (settings.highContrast) Modifier.border(2.dp, Color.Black, RoundedCornerShape(24.dp)) else Modifier),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = "Create account icon", tint = DermaGreen, modifier = Modifier.size(44.dp))
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text("Create account", fontSize = settings.textDisplay.sp, fontWeight = FontWeight.Bold, color = settings.textPrimary)
            Spacer(modifier = Modifier.height(6.dp))
            Text("Join DermaLens today", fontSize = settings.textMd.sp, color = settings.textSecondary)
            Spacer(modifier = Modifier.height(32.dp))

            if (registerError.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0xFFFEE2E2)).padding(12.dp).semantics { contentDescription = "Error: $registerError" },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Error, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(registerError, fontSize = settings.textBase.sp, color = Color(0xFFDC2626))
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            OutlinedTextField(value = name, onValueChange = { name = it; nameError = "" }, label = { Text("Full name") }, leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }, isError = nameError.isNotEmpty(), supportingText = { if (nameError.isNotEmpty()) Text(nameError, color = Color(0xFFDC2626)) }, singleLine = true, modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Full name input field" }, shape = RoundedCornerShape(14.dp), colors = dermaFieldColors(settings.highContrast))
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(value = email, onValueChange = { email = it; emailError = ""; registerError = "" }, label = { Text("Email address") }, leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) }, isError = emailError.isNotEmpty(), supportingText = { if (emailError.isNotEmpty()) Text(emailError, color = Color(0xFFDC2626)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), singleLine = true, modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Email address input field" }, shape = RoundedCornerShape(14.dp), colors = dermaFieldColors(settings.highContrast))
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(value = password, onValueChange = { password = it; passwordError = "" }, label = { Text("Password") }, leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) }, trailingIcon = { IconButton(onClick = { passwordVisible = !passwordVisible }) { Icon(if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = if (passwordVisible) "Hide password" else "Show password", tint = Color(0xFF9CA3AF)) } }, visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(), isError = passwordError.isNotEmpty(), supportingText = { if (passwordError.isNotEmpty()) Text(passwordError, color = Color(0xFFDC2626)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), singleLine = true, modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Password input field" }, shape = RoundedCornerShape(14.dp), colors = dermaFieldColors(settings.highContrast))
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(value = confirmPassword, onValueChange = { confirmPassword = it; confirmPasswordError = "" }, label = { Text("Confirm password") }, leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) }, trailingIcon = { IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) { Icon(if (confirmPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password", tint = Color(0xFF9CA3AF)) } }, visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(), isError = confirmPasswordError.isNotEmpty(), supportingText = { if (confirmPasswordError.isNotEmpty()) Text(confirmPasswordError, color = Color(0xFFDC2626)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), singleLine = true, modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Confirm password input field" }, shape = RoundedCornerShape(14.dp), colors = dermaFieldColors(settings.highContrast))

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
                                val prefs = context.getSharedPreferences(DermaPrefs.PREFS_NAME, android.content.Context.MODE_PRIVATE)
                                prefs.edit().putBoolean(DermaPrefs.KEY_IS_LOGGED_IN, true).putString(DermaPrefs.KEY_USER_EMAIL, email.trim()).apply()
                                isLoading = false
                                navController.navigate(Screen.Home.route) { popUpTo(Screen.Register.route) { inclusive = true } }
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(54.dp).semantics { contentDescription = "Create account button" },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DermaGreen),
                enabled = !isLoading
            ) {
                if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                else Text("Create Account", fontSize = settings.textLg.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Already have an account?", fontSize = settings.textMd.sp, color = settings.textSecondary)
                TextButton(onClick = { navController.popBackStack() }) {
                    Text("Sign In", fontSize = settings.textMd.sp, color = DermaGreen, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(onClick = { showPrivacyDialog = true }, contentPadding = PaddingValues(0.dp)) {
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(color = settings.textSecondary)) { append("By registering, you agree to our ") }
                        withStyle(SpanStyle(color = DermaGreen, fontWeight = FontWeight.SemiBold)) { append("Privacy Policy") }
                    },
                    fontSize = settings.textSm.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text("⚕️ For diagnostic reference only. Always consult a licensed dermatologist.", fontSize = settings.textSm.sp, color = settings.textSecondary, textAlign = TextAlign.Center, lineHeight = 16.sp)
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            containerColor = Color.White,
            titleContentColor = Color(0xFF111827),
            textContentColor = Color(0xFF374151),
            title = { Text("Privacy Policy", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    PrivacySection("Data We Collect", "We collect your name, email address, and skin scan images you choose to submit. Scan results including condition, severity, and confidence scores are stored locally on your device.")
                    PrivacySection("How We Use Your Data", "Your data is used solely to provide personalized skin health tracking within the app. If you enable 'Contribute to Research', anonymized scan data may be used to improve our detection model.")
                    PrivacySection("Data Storage", "All personal data is stored locally on your device using encrypted storage. We do not sell, rent, or share your personal information with third parties.")
                    PrivacySection("Research Contributions", "Contribution is entirely opt-in. You may toggle this off at any time in Profile > Contribute to Research. Contributed data is anonymized before use.")
                    PrivacySection("Your Rights", "You may delete your account and all associated data at any time. Scan records can be individually deleted from the Progress Tracker.")
                    PrivacySection("Medical Disclaimer", "DermaLens is for informational reference only and does not constitute medical advice. Always consult a licensed dermatologist for diagnosis and treatment.")
                    PrivacySection("Contact", "For privacy concerns, contact us through the app's feedback channel.")
                }
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyDialog = false }) {
                    Text("Close", color = DermaGreen, fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }
}