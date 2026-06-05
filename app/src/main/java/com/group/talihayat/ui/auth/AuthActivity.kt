package com.group.talihayat.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.*
import com.group.talihayat.ui.caretaker.CaretakerDashboardActivity
import com.group.talihayat.ui.elderly.ElderlyDashboardActivity
import com.group.talihayat.ui.splash.TaliHayatSplashScreen
import com.group.talihayat.ui.theme.*
import kotlin.math.*

enum class AuthScreen { LOGIN, REGISTER }

// ─────────────────────────────────────────────────
//  VALIDATION HELPERS
// ─────────────────────────────────────────────────

private object Validators {
    private val emailRegex = Regex("^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$")
    private val myPhoneRegex = Regex("^(\\+?6?01)[0-46-9]-*[0-9]{7,8}$")

    fun email(value: String): String? = when {
        value.isBlank()            -> "Email cannot be empty"
        !emailRegex.matches(value) -> "Please enter a valid email address"
        else                       -> null
    }

    fun password(value: String): String? = when {
        value.isBlank()   -> "Password cannot be empty"
        value.length < 8  -> "Password must be at least 8 characters"
        else              -> null
    }

    fun loginPassword(value: String): String? =
        if (value.isBlank()) "Password cannot be empty" else null

    fun confirmPassword(password: String, confirm: String): String? = when {
        confirm.isBlank()      -> "Please confirm your password"
        confirm != password    -> "Passwords do not match"
        else                   -> null
    }

    fun phone(value: String): String? = when {
        value.isBlank()              -> "Phone number cannot be empty"
        !myPhoneRegex.matches(value) -> "Enter a valid Malaysian number"
        else                         -> null
    }
}

class AuthActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TaliHayatTheme {
                var showSplash by remember { mutableStateOf(true) }

                AnimatedContent(
                    targetState = showSplash,
                    transitionSpec = { fadeIn(tween(800)) togetherWith fadeOut(tween(800)) },
                    label = "SplashTransition"
                ) { isSplashVisible ->
                    if (isSplashVisible) {
                        TaliHayatSplashScreen(onTimeout = { showSplash = false })
                    } else {
                        TaliHayatAuthHost(
                            onAuthSuccess = { role ->
                                val destination = if (role == "Elderly") {
                                    ElderlyDashboardActivity::class.java
                                } else {
                                    CaretakerDashboardActivity::class.java
                                }
                                startActivity(Intent(this@AuthActivity, destination))
                                finish()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TaliHayatAuthHost(onAuthSuccess: (String) -> Unit = {}) {
    var screen by remember { mutableStateOf(AuthScreen.LOGIN) }

    Box(modifier = Modifier.fillMaxSize().background(Background)) {
        AmbientDecor()

        AnimatedContent(
            targetState  = screen,
            transitionSpec = {
                if (targetState == AuthScreen.REGISTER) {
                    (slideInHorizontally(initialOffsetX = { it }) + fadeIn()) togetherWith
                            (slideOutHorizontally(targetOffsetX = { -it / 3 }) + fadeOut())
                } else {
                    (slideInHorizontally(initialOffsetX = { -it }) + fadeIn()) togetherWith
                            (slideOutHorizontally(targetOffsetX = { it / 3 }) + fadeOut())
                }
            },
            label = "AuthScreenSwitch"
        ) { current ->
            when (current) {
                AuthScreen.LOGIN    -> LoginScreen(
                    onNavigateToRegister = { screen = AuthScreen.REGISTER },
                    onLoginSuccess       = onAuthSuccess
                )
                AuthScreen.REGISTER -> RegisterScreen(
                    onNavigateToLogin  = { screen = AuthScreen.LOGIN },
                    onRegisterSuccess  = onAuthSuccess
                )
            }
        }
    }
}

@Composable
fun AmbientDecor() {
    val infinite = rememberInfiniteTransition(label = "Decor")
    val drift by infinite.animateFloat(
        initialValue  = 0f,
        targetValue   = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(12000, easing = LinearEasing)),
        label = "Drift"
    )
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawCircle(
            color  = Teal.copy(alpha = 0.06f),
            radius = size.width * 0.7f,
            center = Offset(size.width + size.width * 0.1f * sin(drift), -size.height * 0.05f)
        )
        drawCircle(
            color  = Navy.copy(alpha = 0.04f),
            radius = size.width * 0.55f,
            center = Offset(-size.width * 0.15f + 10f * sin(drift + 1f), size.height * 1.05f)
        )
    }
}

@Composable
fun LoginScreen(onNavigateToRegister: () -> Unit, onLoginSuccess: (String) -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(64.dp))
        BrandHero()
        Spacer(Modifier.height(40.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .shadow(elevation = 12.dp, shape = RoundedCornerShape(28.dp))
                .background(Surface, RoundedCornerShape(28.dp))
                .padding(28.dp)
        ) {
            Text("Welcome back", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Navy)
            Text("Sign in to continue", fontSize = 13.sp, color = GrayMuted)
            Spacer(Modifier.height(28.dp))

            TaliTextField(
                value = email,
                onValueChange = { email = it; emailError = null },
                label = "Email Address",
                placeholder = "elderly@tali.com",
                leadingIcon = Icons.Outlined.Email,
                keyboardType = KeyboardType.Email,
                isError = emailError != null,
                errorMessage = emailError
            )
            Spacer(Modifier.height(14.dp))
            TaliTextField(
                value = password,
                onValueChange = { password = it; passwordError = null },
                label = "Password",
                placeholder = "Your password",
                leadingIcon = Icons.Outlined.Lock,
                isPassword = true,
                isError = passwordError != null,
                errorMessage = passwordError
            )
            Spacer(Modifier.height(28.dp))
            TaliPrimaryButton(
                text = "Sign In",
                isLoading = isLoading,
                onClick = {
                    emailError = Validators.email(email)
                    passwordError = Validators.loginPassword(password)
                    if (emailError == null && passwordError == null) {
                        val role = if (email.contains("elderly")) "Elderly" else "Caretaker"
                        if (password == "pass123") { isLoading = true; onLoginSuccess(role) }
                        else { passwordError = "Incorrect password" }
                    }
                }
            )
        }
        Spacer(Modifier.height(28.dp))
        Row {
            Text("Don't have an account? ", fontSize = 13.sp, color = GrayMuted)
            Text("Create one", fontSize = 13.sp, color = Teal, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onNavigateToRegister() })
        }
    }
}

@Composable
fun RegisterScreen(onNavigateToLogin: () -> Unit, onRegisterSuccess: (String) -> Unit) {
    var selectedRole by remember { mutableStateOf("Caregiver / Family") }
    var email by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var emailError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(48.dp))
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).background(Surface, CircleShape).border(1.dp, GrayLight, CircleShape).clickable { onNavigateToLogin() }, contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.ArrowBackIosNew, contentDescription = "Back", tint = Navy, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.width(16.dp))
            Text("Create Account", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Navy)
        }
        Spacer(Modifier.height(20.dp))

        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).shadow(12.dp, RoundedCornerShape(28.dp)).background(Surface, RoundedCornerShape(28.dp)).padding(28.dp)
        ) {
            Text("I am a...", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Navy)
            Spacer(Modifier.height(10.dp))
            RoleSelector(roles = listOf("Caregiver / Family", "Elderly"), selectedRole = selectedRole, onSelect = { selectedRole = it })
            Spacer(Modifier.height(22.dp))
            TaliTextField(value = email, onValueChange = { email = it; emailError = null }, label = "Email Address", placeholder = "your@email.com", leadingIcon = Icons.Outlined.Email, keyboardType = KeyboardType.Email, isError = emailError != null, errorMessage = emailError)
            Spacer(Modifier.height(14.dp))
            TaliTextField(value = phoneNumber, onValueChange = { phoneNumber = it; phoneError = null }, label = "Phone Number", placeholder = "0123456789", leadingIcon = Icons.Outlined.Phone, keyboardType = KeyboardType.Phone, isError = phoneError != null, errorMessage = phoneError)
            Spacer(Modifier.height(14.dp))
            TaliTextField(value = password, onValueChange = { password = it; passwordError = null }, label = "Password", placeholder = "Min. 8 characters", leadingIcon = Icons.Outlined.Lock, isPassword = true, isError = passwordError != null, errorMessage = passwordError)
            Spacer(Modifier.height(14.dp))
            TaliTextField(value = confirmPassword, onValueChange = { confirmPassword = it; confirmPasswordError = null }, label = "Confirm Password", placeholder = "Re-enter password", leadingIcon = Icons.Outlined.LockOpen, isPassword = true, isError = confirmPasswordError != null, errorMessage = confirmPasswordError)
            Spacer(Modifier.height(28.dp))
            TaliPrimaryButton(text = "Create Account", isLoading = false, onClick = {
                emailError = Validators.email(email); phoneError = Validators.phone(phoneNumber); passwordError = Validators.password(password); confirmPasswordError = Validators.confirmPassword(password, confirmPassword)
                if (listOf(emailError, phoneError, passwordError, confirmPasswordError).all { it == null }) onRegisterSuccess(if (selectedRole == "Elderly") "Elderly" else "Caretaker")
            })
        }
        Spacer(Modifier.height(24.dp))
        TextButton(onClick = onNavigateToLogin) { Text("Already have an account? Sign in", color = Teal, fontSize = 13.sp) }
    }
}

@Composable
fun BrandHero() {
    val heartBeat = rememberInfiniteTransition(label = "HB")
    val heartScale by heartBeat.animateFloat(
        initialValue  = 1f, targetValue = 1.18f,
        animationSpec = infiniteRepeatable(animation = keyframes { durationMillis = 1200; 1.0f at 0; 1.18f at 150; 1.0f at 320; 1.18f at 470; 1.0f at 640 }),
        label = "HeartScale"
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(80.dp).background(TealLight, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Favorite, contentDescription = null, tint = Teal, modifier = Modifier.size(38.dp).scale(heartScale))
        }
        Spacer(Modifier.height(16.dp))
        Text("TaliHayat", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = Navy, letterSpacing = (-1).sp)
        Text("Elderly Fall Monitoring System", fontSize = 13.sp, color = GrayMuted)
    }
}

@Composable
fun RoleSelector(roles: List<String>, selectedRole: String, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        roles.forEach { role ->
            val selected = role == selectedRole
            Box(
                modifier = Modifier.fillMaxWidth().border(if (selected) 2.dp else 1.dp, if (selected) Teal else InputBorder, RoundedCornerShape(16.dp))
                    .background(if (selected) TealLight.copy(alpha = 0.3f) else Color.Transparent, RoundedCornerShape(16.dp))
                    .clickable { onSelect(role) }.padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (role == "Elderly") Icons.Filled.Elderly else Icons.Filled.FamilyRestroom, contentDescription = null, tint = if (selected) Teal else GrayMuted)
                    Spacer(Modifier.width(14.dp))
                    Text(role, fontWeight = FontWeight.Bold, color = if (selected) Navy else GrayMuted)
                }
            }
        }
    }
}

@Composable
fun TaliTextField(value: String, onValueChange: (String) -> Unit, label: String, placeholder: String, leadingIcon: ImageVector, keyboardType: KeyboardType = KeyboardType.Text, isPassword: Boolean = false, isError: Boolean = false, errorMessage: String? = null) {
    var showPassword by remember { mutableStateOf(false) }
    Column {
        Text(label, fontSize = 12.sp, color = if (isError) Error else GrayMuted, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 6.dp))
        OutlinedTextField(
            value = value, onValueChange = onValueChange, placeholder = { Text(placeholder, color = GrayMuted, fontSize = 14.sp) },
            leadingIcon = { Icon(leadingIcon, contentDescription = null, tint = if (isError) Error else Teal, modifier = Modifier.size(20.dp)) },
            trailingIcon = if (isPassword) { { IconButton(onClick = { showPassword = !showPassword }) { Icon(if (showPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, contentDescription = null, tint = if (isError) Error else GrayMuted) } } } else if (isError) { { Icon(Icons.Filled.Error, contentDescription = null, tint = Error) } } else null,
            visualTransformation = if (isPassword && !showPassword) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), singleLine = true, isError = isError,
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Teal, unfocusedBorderColor = InputBorder, cursorColor = Teal, errorBorderColor = Error)
        )
        if (isError && !errorMessage.isNullOrBlank()) {
            Text(errorMessage, fontSize = 11.sp, color = Error, modifier = Modifier.padding(top = 4.dp, start = 4.dp))
        }
    }
}

@Composable
fun TaliPrimaryButton(text: String, isLoading: Boolean, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = Teal), enabled = !isLoading) {
        if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
        else Text(text, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
    }
}
