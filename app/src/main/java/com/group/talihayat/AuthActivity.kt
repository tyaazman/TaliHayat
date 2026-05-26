package com.group.talihayat

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
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.*
import kotlin.math.*

// ─────────────────────────────────────────────────
//  DESIGN TOKENS  (unchanged)
// ─────────────────────────────────────────────────

private object C {
    val Background  = Color(0xFFF8F9FA)
    val Surface     = Color(0xFFFFFFFF)
    val Teal        = Color(0xFF00A8B5)
    val TealDark    = Color(0xFF007A85)
    val TealLight   = Color(0xFFE0F7FA)
    val Crimson     = Color(0xFFFF5757)
    val Navy        = Color(0xFF1E3A5F)
    val GrayMuted   = Color(0xFF8A9BB0)
    val GrayLight   = Color(0xFFECEFF1)
    val GlowTeal    = Color(0x3000A8B5)
    val CardShadow  = Color(0x14000000)
    val InputBorder = Color(0xFFDDE3EC)
    val Error       = Color(0xFFFF5757)   // Crimson — validation errors
}

enum class AuthScreen { LOGIN, REGISTER }

// ─────────────────────────────────────────────────
//  VALIDATION HELPERS
// ─────────────────────────────────────────────────

private object Validators {

    /** Basic RFC-5322-lite email check */
    private val emailRegex = Regex(
        "^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$"
    )

    /**
     * Malaysian mobile number.
     * Accepts: 01X-XXXXXXX / 01X-XXXXXXXX / +601X-XXXXXXX / 601X-XXXXXXX
     * Covers 010–019 prefixes (all current Malaysian operators).
     */
    private val myPhoneRegex = Regex(
        "^(\\+?6?01)[0-46-9]-*[0-9]{7,8}$"
    )

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
        !myPhoneRegex.matches(value) ->
            "Enter a valid Malaysian number (e.g. 0123456789 or +60123456789)"
        else                         -> null
    }
}

// ─────────────────────────────────────────────────
//  AUTH ACTIVITY
// ─────────────────────────────────────────────────

class AuthActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TaliHayatTheme {
                TaliHayatAuthHost(
                    onAuthSuccess = { role ->
                        val destination = if (role == "Elderly")
                            ElderlyDashboardActivity::class.java
                        else
                            CaretakerDashboardActivity::class.java
                        startActivity(Intent(this, destination))
                        finish()
                    }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────
//  AUTH HOST  (unchanged — slide transitions kept)
// ─────────────────────────────────────────────────

@Composable
fun TaliHayatAuthHost(onAuthSuccess: (String) -> Unit = {}) {
    var screen by remember { mutableStateOf(AuthScreen.LOGIN) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(C.Background)
    ) {
        AmbientDecor()   // unchanged

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

// ─────────────────────────────────────────────────
//  AMBIENT DECOR  (unchanged)
// ─────────────────────────────────────────────────

@Composable
fun AmbientDecor() {
    val infinite = rememberInfiniteTransition(label = "Decor")
    val drift by infinite.animateFloat(
        initialValue  = 0f,
        targetValue   = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            tween(12000, easing = LinearEasing)
        ),
        label = "Drift"
    )
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawCircle(
            color  = C.Teal.copy(alpha = 0.06f),
            radius = size.width * 0.7f,
            center = Offset(size.width + size.width * 0.1f * sin(drift), -size.height * 0.05f)
        )
        drawCircle(
            color  = C.Navy.copy(alpha = 0.04f),
            radius = size.width * 0.55f,
            center = Offset(-size.width * 0.15f + 10f * sin(drift + 1f), size.height * 1.05f)
        )
    }
}

// ─────────────────────────────────────────────────
//  LOGIN SCREEN  — with validation
// ─────────────────────────────────────────────────

@Composable
fun LoginScreen(
    onNavigateToRegister : () -> Unit,
    onLoginSuccess       : (String) -> Unit
) {
    // Field values
    var email    by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    // Error states (null = no error)
    var emailError    by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding(),
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
                .background(C.Surface, RoundedCornerShape(28.dp))
                .padding(28.dp)
        ) {
            Text(
                text       = "Welcome back",
                fontSize   = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color      = C.Navy
            )
            Text(
                text     = "Sign in to continue",
                fontSize = 13.sp,
                color    = C.GrayMuted
            )

            Spacer(Modifier.height(28.dp))

            // ── Email ──────────────────────────────────────────────────────
            TaliTextField(
                value         = email,
                onValueChange = {
                    email = it
                    if (emailError != null) emailError = null   // clear on type
                },
                label         = "Email Address",
                placeholder   = "elderly@tali.com / care@tali.com",
                leadingIcon   = Icons.Outlined.Email,
                keyboardType  = KeyboardType.Email,
                isError       = emailError != null,
                errorMessage  = emailError
            )

            Spacer(Modifier.height(14.dp))

            // ── Password ───────────────────────────────────────────────────
            TaliTextField(
                value         = password,
                onValueChange = {
                    password = it
                    if (passwordError != null) passwordError = null
                },
                label         = "Password",
                placeholder   = "Your password",
                leadingIcon   = Icons.Outlined.Lock,
                isPassword    = true,
                isError       = passwordError != null,
                errorMessage  = passwordError
            )

            Spacer(Modifier.height(28.dp))

            // ── Sign In ────────────────────────────────────────────────────
            TaliPrimaryButton(
                text      = "Sign In",
                isLoading = isLoading,
                onClick   = {
                    // Run validators
                    emailError    = Validators.email(email)
                    passwordError = Validators.loginPassword(password)

                    if (emailError == null && passwordError == null) {
                        // Dummy routing logic (unchanged)
                        val role = when (email.trim().lowercase()) {
                            "elderly@tali.com" -> "Elderly"
                            "care@tali.com"    -> "Caretaker"
                            else               -> "Caretaker"
                        }
                        if (password == "pass123") {
                            isLoading = true
                            onLoginSuccess(role)
                        } else {
                            passwordError = "Incorrect password"
                        }
                    }
                }
            )
        }

        Spacer(Modifier.height(28.dp))

        Row {
            Text("Don't have an account? ", fontSize = 13.sp, color = C.GrayMuted)
            Text(
                text       = "Create one",
                fontSize   = 13.sp,
                color      = C.Teal,
                fontWeight = FontWeight.Bold,
                modifier   = Modifier.clickable { onNavigateToRegister() }
            )
        }

        Spacer(Modifier.height(40.dp))
    }
}

// ─────────────────────────────────────────────────
//  REGISTER SCREEN  — with validation + confirm password
// ─────────────────────────────────────────────────

@Composable
fun RegisterScreen(
    onNavigateToLogin : () -> Unit,
    onRegisterSuccess : (String) -> Unit
) {
    // Field values
    var selectedRole    by remember { mutableStateOf("Caregiver / Family") }
    var email           by remember { mutableStateOf("") }
    var phoneNumber     by remember { mutableStateOf("") }
    var password        by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    // Error states
    var emailError          by remember { mutableStateOf<String?>(null) }
    var phoneError          by remember { mutableStateOf<String?>(null) }
    var passwordError       by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(48.dp))

        // Back + title
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(C.Surface, CircleShape)
                    .border(1.dp, C.GrayLight, CircleShape)
                    .clickable { onNavigateToLogin() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = Icons.Filled.ArrowBackIosNew,
                    contentDescription = "Back",
                    tint               = C.Navy,
                    modifier           = Modifier.size(16.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Text(
                text       = "Create Account",
                fontSize   = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color      = C.Navy
            )
        }

        Spacer(Modifier.height(20.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .shadow(elevation = 12.dp, shape = RoundedCornerShape(28.dp))
                .background(C.Surface, RoundedCornerShape(28.dp))
                .padding(28.dp)
        ) {
            // ── Role selector (unchanged) ──────────────────────────────────
            Text("I am a...", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = C.Navy)
            Spacer(Modifier.height(10.dp))
            RoleSelector(
                roles        = listOf("Caregiver / Family", "Elderly"),
                selectedRole = selectedRole,
                onSelect     = { selectedRole = it }
            )

            Spacer(Modifier.height(22.dp))

            // ── Email ──────────────────────────────────────────────────────
            TaliTextField(
                value         = email,
                onValueChange = {
                    email = it
                    if (emailError != null) emailError = null
                },
                label         = "Email Address",
                placeholder   = "your@email.com",
                leadingIcon   = Icons.Outlined.Email,
                keyboardType  = KeyboardType.Email,
                isError       = emailError != null,
                errorMessage  = emailError
            )

            Spacer(Modifier.height(14.dp))

            // ── Phone ──────────────────────────────────────────────────────
            TaliTextField(
                value         = phoneNumber,
                onValueChange = {
                    phoneNumber = it
                    if (phoneError != null) phoneError = null
                },
                label        = "Phone Number",
                placeholder  = "e.g. 0123456789 or +60123456789",
                leadingIcon  = Icons.Outlined.Phone,
                keyboardType = KeyboardType.Phone,
                isError      = phoneError != null,
                errorMessage = phoneError
            )

            Spacer(Modifier.height(14.dp))

            // ── Password ───────────────────────────────────────────────────
            TaliTextField(
                value         = password,
                onValueChange = {
                    password = it
                    if (passwordError != null) passwordError = null
                    // Re-check confirm match live once user edits password
                    if (confirmPasswordError != null && confirmPassword.isNotEmpty()) {
                        confirmPasswordError =
                            if (it != confirmPassword) "Passwords do not match" else null
                    }
                },
                label        = "Password",
                placeholder  = "Min. 8 characters",
                leadingIcon  = Icons.Outlined.Lock,
                isPassword   = true,
                isError      = passwordError != null,
                errorMessage = passwordError
            )

            // Password strength bar (appears only when password is non-empty)
            AnimatedVisibility(
                visible = password.isNotEmpty(),
                enter   = expandVertically() + fadeIn(),
                exit    = shrinkVertically() + fadeOut()
            ) {
                Spacer(Modifier.height(8.dp))
                PasswordStrengthBar(password = password)
            }

            Spacer(Modifier.height(14.dp))

            // ── Confirm Password (NEW) ─────────────────────────────────────
            TaliTextField(
                value         = confirmPassword,
                onValueChange = {
                    confirmPassword = it
                    if (confirmPasswordError != null) confirmPasswordError = null
                },
                label        = "Confirm Password",
                placeholder  = "Re-enter your password",
                leadingIcon  = Icons.Outlined.LockOpen,
                isPassword   = true,
                isError      = confirmPasswordError != null,
                errorMessage = confirmPasswordError
            )

            Spacer(Modifier.height(28.dp))

            // ── Create Account ─────────────────────────────────────────────
            TaliPrimaryButton(
                text      = "Create Account",
                isLoading = false,
                onClick   = {
                    // Run all validators
                    emailError           = Validators.email(email)
                    phoneError           = Validators.phone(phoneNumber)
                    passwordError        = Validators.password(password)
                    confirmPasswordError = Validators.confirmPassword(password, confirmPassword)

                    val allValid = listOf(
                        emailError,
                        phoneError,
                        passwordError,
                        confirmPasswordError
                    ).all { it == null }

                    if (allValid) {
                        val role = if (selectedRole == "Elderly") "Elderly" else "Caretaker"
                        onRegisterSuccess(role)
                    }
                }
            )
        }

        Spacer(Modifier.height(24.dp))

        TextButton(onClick = onNavigateToLogin) {
            Text(
                text     = "Already have an account? Sign in",
                color    = C.Teal,
                fontSize = 13.sp
            )
        }

        Spacer(Modifier.height(40.dp))
    }
}

// ─────────────────────────────────────────────────
//  BRAND HERO  (unchanged)
// ─────────────────────────────────────────────────

@Composable
fun BrandHero() {
    val heartBeat = rememberInfiniteTransition(label = "HB")
    val heartScale by heartBeat.animateFloat(
        initialValue  = 1f,
        targetValue   = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1200
                1.0f  at 0   using FastOutSlowInEasing
                1.18f at 150 using FastOutSlowInEasing
                1.0f  at 320 using FastOutSlowInEasing
                1.18f at 470 using FastOutSlowInEasing
                1.0f  at 640
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "HeartScale"
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier         = Modifier
                .size(80.dp)
                .background(C.TealLight, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = Icons.Filled.Favorite,
                contentDescription = null,
                tint               = C.Teal,
                modifier           = Modifier
                    .size(38.dp)
                    .scale(heartScale)
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text          = "TaliHayat",
            fontSize      = 32.sp,
            fontWeight    = FontWeight.ExtraBold,
            color         = C.Navy,
            letterSpacing = (-1).sp
        )
        Text(
            text     = "Elderly Fall Monitoring System",
            fontSize = 13.sp,
            color    = C.GrayMuted
        )
    }
}

// ─────────────────────────────────────────────────
//  ROLE SELECTOR  (unchanged)
// ─────────────────────────────────────────────────

@Composable
fun RoleSelector(
    roles        : List<String>,
    selectedRole : String,
    onSelect     : (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        roles.forEach { role ->
            val selected = role == selectedRole
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = if (selected) 2.dp else 1.dp,
                        color = if (selected) C.Teal else C.InputBorder,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .background(
                        color = if (selected) C.TealLight.copy(alpha = 0.3f) else Color.Transparent,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable { onSelect(role) }
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector        = if (role == "Elderly")
                            Icons.Filled.Elderly else Icons.Filled.FamilyRestroom,
                        contentDescription = null,
                        tint               = if (selected) C.Teal else C.GrayMuted
                    )
                    Spacer(Modifier.width(14.dp))
                    Text(
                        text       = role,
                        fontWeight = FontWeight.Bold,
                        color      = if (selected) C.Navy else C.GrayMuted
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────
//  TALI TEXT FIELD  — refactored with isError + errorMessage
// ─────────────────────────────────────────────────

@Composable
fun TaliTextField(
    value         : String,
    onValueChange : (String) -> Unit,
    label         : String,
    placeholder   : String,
    leadingIcon   : androidx.compose.ui.graphics.vector.ImageVector,
    keyboardType  : KeyboardType = KeyboardType.Text,
    isPassword    : Boolean      = false,
    // ── NEW validation params ──────────────────────────────────────────────
    isError       : Boolean      = false,
    errorMessage  : String?      = null
) {
    var showPassword by remember { mutableStateOf(false) }

    Column {
        // Label — turns red when error
        Text(
            text       = label,
            fontSize   = 12.sp,
            color      = if (isError) C.Error else C.GrayMuted,
            fontWeight = FontWeight.SemiBold,
            modifier   = Modifier.padding(bottom = 6.dp)
        )

        OutlinedTextField(
            value         = value,
            onValueChange = onValueChange,
            placeholder   = {
                Text(placeholder, color = C.GrayMuted, fontSize = 14.sp)
            },
            leadingIcon   = {
                Icon(
                    imageVector        = leadingIcon,
                    contentDescription = null,
                    tint               = if (isError) C.Error else C.Teal,
                    modifier           = Modifier.size(20.dp)
                )
            },
            trailingIcon  = when {
                // Password visibility toggle
                isPassword -> {
                    {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                imageVector        = if (showPassword)
                                    Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                contentDescription = "Toggle password visibility",
                                tint               = if (isError) C.Error else C.GrayMuted
                            )
                        }
                    }
                }
                // Error indicator icon for non-password fields
                isError -> {
                    {
                        Icon(
                            imageVector        = Icons.Filled.Error,
                            contentDescription = "Error",
                            tint               = C.Error,
                            modifier           = Modifier.size(20.dp)
                        )
                    }
                }
                else -> null
            },
            visualTransformation = if (isPassword && !showPassword)
                PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction    = ImeAction.Next
            ),
            modifier  = Modifier.fillMaxWidth(),
            shape     = RoundedCornerShape(14.dp),
            singleLine = true,
            isError   = isError,
            colors    = OutlinedTextFieldDefaults.colors(
                // Normal states
                focusedBorderColor   = C.Teal,
                unfocusedBorderColor = C.InputBorder,
                cursorColor          = C.Teal,
                // Error states — override to Crimson
                errorBorderColor     = C.Error,
                errorCursorColor     = C.Error,
                errorLeadingIconColor  = C.Error,
                errorTrailingIconColor = C.Error,
                errorLabelColor        = C.Error
            )
        )

        // ── Inline error message — slides in/out smoothly ─────────────────
        AnimatedVisibility(
            visible       = isError && !errorMessage.isNullOrBlank(),
            enter         = expandVertically(
                animationSpec = tween(220, easing = FastOutSlowInEasing)
            ) + fadeIn(tween(180)),
            exit          = shrinkVertically(
                animationSpec = tween(180, easing = FastOutSlowInEasing)
            ) + fadeOut(tween(150))
        ) {
            Row(
                modifier              = Modifier.padding(top = 5.dp, start = 4.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    imageVector        = Icons.Filled.ErrorOutline,
                    contentDescription = null,
                    tint               = C.Error,
                    modifier           = Modifier.size(13.dp)
                )
                Text(
                    text       = errorMessage.orEmpty(),
                    fontSize   = 11.sp,
                    color      = C.Error,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────
//  PASSWORD STRENGTH BAR  (new — shown in Register)
// ─────────────────────────────────────────────────

@Composable
private fun PasswordStrengthBar(password: String) {
    val strength = when {
        password.length < 6                                                -> 0  // Weak
        password.length < 8                                                -> 1  // Fair
        password.any { it.isUpperCase() } && password.any { it.isDigit() } -> 3  // Strong
        else                                                               -> 2  // Good
    }
    val labels = listOf("Weak", "Fair", "Good", "Strong")
    val colors = listOf(
        C.Error,
        Color(0xFFFF9800),
        Color(0xFF4CAF50),
        C.Teal
    )

    val barColor by animateColorAsState(
        targetValue   = colors[strength],
        animationSpec = tween(350),
        label         = "StrengthColor"
    )
    val fraction by animateFloatAsState(
        targetValue   = (strength + 1) / 4f,
        animationSpec = tween(350),
        label         = "StrengthFraction"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(C.GrayLight, RoundedCornerShape(2.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(4.dp)
                    .background(barColor, RoundedCornerShape(2.dp))
            )
        }
        Text(
            text       = "Strength: ${labels[strength]}",
            fontSize   = 11.sp,
            color      = barColor,
            fontWeight = FontWeight.Medium,
            modifier   = Modifier.padding(top = 3.dp)
        )
    }
}

// ─────────────────────────────────────────────────
//  PRIMARY BUTTON  (unchanged)
// ─────────────────────────────────────────────────

@Composable
fun TaliPrimaryButton(
    text      : String,
    isLoading : Boolean,
    onClick   : () -> Unit
) {
    Button(
        onClick  = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
        shape    = RoundedCornerShape(16.dp),
        colors   = ButtonDefaults.buttonColors(containerColor = C.Teal),
        enabled  = !isLoading
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color    = Color.White,
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.5.dp
            )
        } else {
            Text(
                text       = text,
                color      = Color.White,
                fontSize   = 15.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

// ─────────────────────────────────────────────────
//  THEME  (unchanged)
// ─────────────────────────────────────────────────

@Composable
fun TaliHayatTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary    = C.Teal,
            secondary  = C.Navy,
            background = C.Background,
            surface    = C.Surface
        ),
        content = content
    )
}