package com.group.talihayat.ui.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.group.talihayat.ui.theme.*
import kotlinx.coroutines.*
import kotlin.math.*
import androidx.navigation.NavController
import androidx.compose.ui.platform.LocalContext
import android.content.Context
import android.content.Intent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.group.talihayat.ui.navigation.Routes

// ─────────────────────────────────────────────────
//  SPRING SPECS
// ─────────────────────────────────────────────────

private val LogoSpring = spring<Float>(
    dampingRatio = 0.55f,
    stiffness    = Spring.StiffnessMediumLow
)
private val HaloSpring = spring<Float>(
    dampingRatio = 0.65f,
    stiffness    = Spring.StiffnessLow
)
private val TextSpring = spring<Float>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness    = Spring.StiffnessMediumLow
)

@Composable
fun TaliHayatSplashScreen(navController: NavController) {

    val ringScale1   = remember { Animatable(0.60f) }
    val ringScale2   = remember { Animatable(0.40f) }
    val ringAlpha1   = remember { Animatable(0f) }
    val ringAlpha2   = remember { Animatable(0f) }

    val logoOffsetY  = remember { Animatable(80f) }
    val logoAlpha    = remember { Animatable(0f) }
    val logoScale    = remember { Animatable(0.72f) }

    val arcSweep     = remember { Animatable(0f) }

    val haloRadius   = remember { Animatable(0f) }
    val haloAlpha    = remember { Animatable(0f) }

    val textAlpha    = remember { Animatable(0f) }
    val textOffsetY  = remember { Animatable(24f) }
    val letterSpace  = remember { Animatable(6f) }

    val tagAlpha     = remember { Animatable(0f) }
    val tagOffsetY   = remember { Animatable(18f) }

    val heartScale   = remember { Animatable(1f) }

    val density = LocalDensity.current
    val context = LocalContext.current

    LaunchedEffect(key1 = true) {
        launch {
            ringAlpha1.animateTo(1f, tween(600, easing = FastOutSlowInEasing))
        }
        launch {
            delay(180)
            ringAlpha2.animateTo(1f, tween(600, easing = FastOutSlowInEasing))
        }
        launch {
            ringScale1.animateTo(
                1f, spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessVeryLow)
            )
        }
        launch {
            delay(200)
            ringScale2.animateTo(
                1f, spring(dampingRatio = 0.80f, stiffness = Spring.StiffnessVeryLow)
            )
        }

        delay(200)
        launch {
            logoAlpha.animateTo(1f, tween(380, easing = FastOutSlowInEasing))
        }
        launch {
            logoOffsetY.animateTo(0f, LogoSpring)
        }
        launch {
            logoScale.animateTo(1f, LogoSpring)
        }

        delay(550)
        arcSweep.animateTo(
            1f,
            tween(520, easing = FastOutSlowInEasing)
        )

        launch {
            haloAlpha.animateTo(1f, tween(320, easing = FastOutSlowInEasing))
        }
        launch {
            haloRadius.animateTo(1f, HaloSpring)
        }

        delay(200)
        launch {
            textAlpha.animateTo(1f, tween(460, easing = FastOutSlowInEasing))
        }
        launch {
            textOffsetY.animateTo(0f, TextSpring)
        }
        launch {
            letterSpace.animateTo(0f, tween(480, easing = FastOutSlowInEasing))
        }

        delay(350)
        launch {
            tagAlpha.animateTo(1f, tween(420, easing = FastOutSlowInEasing))
        }
        launch {
            tagOffsetY.animateTo(0f, TextSpring)
        }

        delay(300)
        launch {
            while (true) {
                heartScale.animateTo(1.18f, tween(140, easing = FastOutSlowInEasing))
                heartScale.animateTo(1.00f, tween(160, easing = FastOutSlowInEasing))
                delay(80)
                heartScale.animateTo(1.14f, tween(130, easing = FastOutSlowInEasing))
                heartScale.animateTo(1.00f, tween(200, easing = FastOutSlowInEasing))
                delay(900)
            }
        }

        // delay 2000L inside the effect so the user has time to actually see the logo
        delay(2000L)

        // Auth & Role Routing
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            navController.navigate(Routes.Login) {
                popUpTo(Routes.Splash) { inclusive = true }
            }
        } else {
            val prefs = context.getSharedPreferences("TaliHayat_Prefs", Context.MODE_PRIVATE)
            val role = prefs.getString("user_role", null)
            if (role?.trim()?.equals("Caretaker", ignoreCase = true) == true) {
                val serviceIntent = Intent(context, com.group.talihayat.service.FallDetectionService::class.java)
                context.stopService(serviceIntent)
                navController.navigate(Routes.CaretakerDashboard) {
                    popUpTo(Routes.Splash) { inclusive = true }
                }
            } else if (role?.trim()?.equals("Elderly", ignoreCase = true) == true) {
                val serviceIntent = Intent(context, com.group.talihayat.service.FallDetectionService::class.java)
                context.startForegroundService(serviceIntent)
                navController.navigate(Routes.ElderlyDashboard) {
                    popUpTo(Routes.Splash) { inclusive = true }
                }
            } else {
                // If role not found in SharedPreferences, fetch from database as fallback
                val database = FirebaseDatabase.getInstance(
                    "https://talihayat-bfc99-default-rtdb.asia-southeast1.firebasedatabase.app/"
                ).reference
                database.child("users").child(currentUser.uid).addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val roleFromDb = snapshot.child("role").getValue(String::class.java) ?: "Caretaker"
                        val name = snapshot.child("name").getValue(String::class.java) ?: "User"
                        prefs.edit()
                            .putString("user_role", roleFromDb)
                            .putString("saved_user_name", name)
                            .apply()
                        
                        val route = if (roleFromDb.trim().equals("Elderly", ignoreCase = true)) {
                            val serviceIntent = Intent(context, com.group.talihayat.service.FallDetectionService::class.java)
                            context.startForegroundService(serviceIntent)
                            Routes.ElderlyDashboard
                        } else {
                            val serviceIntent = Intent(context, com.group.talihayat.service.FallDetectionService::class.java)
                            context.stopService(serviceIntent)
                            Routes.CaretakerDashboard
                        }
                        navController.navigate(route) {
                            popUpTo(Routes.Splash) { inclusive = true }
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {
                        navController.navigate(Routes.Login) {
                            popUpTo(Routes.Splash) { inclusive = true }
                        }
                    }
                })
            }
        }
    }

    val driftInfinite = rememberInfiniteTransition(label = "AmbientDrift")
    val driftAngle by driftInfinite.animateFloat(
        initialValue  = 0f,
        targetValue   = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation  = tween(18_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "DriftAngle"
    )
    val breathe by driftInfinite.animateFloat(
        initialValue  = 0.92f,
        targetValue   = 1.08f,
        animationSpec = infiniteRepeatable(
            animation  = tween(4_000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Breathe"
    )

    Box(
        modifier         = Modifier
            .fillMaxSize()
            .background(Background),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f

            val driftX = cx + cos(driftAngle) * size.width * 0.08f
            val driftY = cy + sin(driftAngle) * size.height * 0.06f

            drawCircle(
                brush  = Brush.radialGradient(
                    colors = listOf(TealLight.copy(alpha = 0.55f), Color.Transparent),
                    center = Offset(driftX, driftY),
                    radius = size.width * 0.72f * breathe
                ),
                center = Offset(driftX, driftY),
                radius = size.width * 0.72f * breathe
            )

            val dx2 = cx - cos(driftAngle + 1f) * size.width * 0.12f
            val dy2 = cy - sin(driftAngle + 1f) * size.height * 0.10f
            drawCircle(
                brush  = Brush.radialGradient(
                    colors = listOf(Teal.copy(alpha = 0.06f), Color.Transparent),
                    center = Offset(dx2, dy2),
                    radius = size.width * 0.55f
                ),
                center = Offset(dx2, dy2),
                radius = size.width * 0.55f
            )

            val r1 = size.width * 0.38f * ringScale1.value
            val r2 = size.width * 0.58f * ringScale2.value

            drawCircle(
                color  = Teal.copy(alpha = 0.07f * ringAlpha1.value),
                radius = r1,
                center = Offset(cx, cy),
                style  = Stroke(width = 1.5.dp.toPx())
            )
            drawCircle(
                color  = Teal.copy(alpha = 0.04f * ringAlpha2.value),
                radius = r2,
                center = Offset(cx, cy),
                style  = Stroke(width = 1.dp.toPx())
            )
            drawCircle(
                color  = TealLight.copy(alpha = 0.18f * ringAlpha2.value),
                radius = size.width * 0.75f * ringScale2.value,
                center = Offset(cx, cy)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier         = Modifier
                    .graphicsLayer {
                        translationY = with(density) { logoOffsetY.value.dp.toPx() }
                        alpha        = logoAlpha.value
                        scaleX       = logoScale.value
                        scaleY       = logoScale.value
                    }
            ) {
                Canvas(modifier = Modifier.size(160.dp)) {
                    val cx     = size.width / 2f
                    val cy     = size.height / 2f
                    val maxR   = size.width / 2f
                    val r      = (maxR * haloRadius.value).coerceAtLeast(0.1f)
                    val a      = haloAlpha.value

                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Teal.copy(alpha = 0.22f * a),
                                TealLight.copy(alpha = 0.14f * a),
                                Color.Transparent
                            ),
                            center = Offset(cx, cy),
                            radius = r
                        ),
                        radius = r,
                        center = Offset(cx, cy)
                    )

                    drawCircle(
                        color  = TealLight.copy(alpha = 0.60f * a),
                        radius = r * 0.62f,
                        center = Offset(cx, cy)
                    )

                    drawOval(
                        color   = Navy.copy(alpha = 0.07f * a),
                        topLeft = Offset(cx - r * 0.42f, cy + r * 0.50f),
                        size    = Size(r * 0.84f, r * 0.18f)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .shadow(elevation = 16.dp, shape = CircleShape, clip = true)
                        .background(TealLight, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val stroke     = 3.dp.toPx()
                        val inset      = stroke / 2
                        val ovalRect   = Rect(inset, inset, size.width - inset, size.height - inset)

                        drawArc(
                            color      = Teal.copy(alpha = 0.12f),
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter  = false,
                            topLeft    = ovalRect.topLeft,
                            size       = ovalRect.size,
                            style      = Stroke(stroke, cap = StrokeCap.Round)
                        )
                        drawArc(
                            brush      = Brush.sweepGradient(
                                colors = listOf(Teal, Teal.copy(alpha = 0.60f), TealLight, Teal)
                            ),
                            startAngle = -90f,
                            sweepAngle = 360f * arcSweep.value,
                            useCenter  = false,
                            topLeft    = ovalRect.topLeft,
                            size       = ovalRect.size,
                            style      = Stroke(stroke, cap = StrokeCap.Round)
                        )
                    }

                    Icon(
                        imageVector        = Icons.Filled.Favorite,
                        contentDescription = "TaliHayat",
                        tint               = Teal,
                        modifier           = Modifier
                            .size(40.dp)
                            .graphicsLayer {
                                scaleX = heartScale.value
                                scaleY = heartScale.value
                            }
                    )
                }
            }

            Spacer(Modifier.height(36.dp))

            Box(
                modifier = Modifier.graphicsLayer {
                    alpha        = textAlpha.value
                    translationY = with(density) { textOffsetY.value.dp.toPx() }
                }
            ) {
                Text(
                    text          = "TaliHayat",
                    fontSize      = 38.sp,
                    fontWeight    = FontWeight.ExtraBold,
                    color         = Navy,
                    letterSpacing = letterSpace.value.sp,
                    textAlign     = TextAlign.Center
                )
            }

            Spacer(Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .graphicsLayer {
                        alpha        = tagAlpha.value
                        translationY = with(density) { tagOffsetY.value.dp.toPx() }
                    }
                    .padding(horizontal = 32.dp)
            ) {
                Text(
                    text       = "Elderly Fall Monitoring System",
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color      = Navy.copy(alpha = 0.55f),
                    textAlign  = TextAlign.Center,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(Modifier.height(48.dp))

            Box(
                modifier = Modifier.graphicsLayer {
                    alpha = tagAlpha.value * 0.7f
                }
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    repeat(3) { i ->
                        val dotInfinite = rememberInfiniteTransition(label = "Dot$i")
                        val dotAlpha by dotInfinite.animateFloat(
                            initialValue  = 0.25f,
                            targetValue   = (0.85f),
                            animationSpec = infiniteRepeatable(
                                animation  = tween(700, easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse,
                                initialStartOffset = StartOffset(i * 220)
                            ),
                            label = "DotAlpha$i"
                        )
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .background(
                                    color = Teal.copy(alpha = dotAlpha),
                                    shape = CircleShape
                                )
                        )
                    }
                }
            }
        }
    }
}
