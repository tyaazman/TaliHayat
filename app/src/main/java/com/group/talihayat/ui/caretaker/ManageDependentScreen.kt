package com.group.talihayat.ui.caretaker

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.group.talihayat.ui.theme.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageDependentScreen(
    onBackClick:    () -> Unit = {},
    onConnect:      (String) -> Unit = {},
    onScanQr:       () -> Unit = {},
    onSendInvite:   () -> Unit = {},
    onUnlink:       () -> Unit = {},
) {
    var pinValue by remember { mutableStateOf("") }
    var sectionVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(120); sectionVisible = true }

    Scaffold(
        containerColor = Background,
        topBar = { ManageDependentTopBar(onBackClick = onBackClick) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            ManageDependentAnimatedSection(visible = sectionVisible, delayMillis = 0) { CurrentConnectionCard() }
            ManageDependentAnimatedSection(visible = sectionVisible, delayMillis = 80) {
                PairNewDependentSection(pinValue, { if (it.length <= 6 && it.all { c -> c.isDigit() }) pinValue = it }, { onConnect(pinValue) }, onScanQr)
            }
            ManageDependentAnimatedSection(visible = sectionVisible, delayMillis = 160) { RemoteInviteCard(onSend = onSendInvite) }
            ManageDependentAnimatedSection(visible = sectionVisible, delayMillis = 240) { DangerZoneSection(onUnlink = onUnlink) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManageDependentTopBar(onBackClick: () -> Unit) {
    TopAppBar(
        title = { Text("Manage Dependent", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Navy) },
        navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.Default.ArrowBack, "Back", tint = Teal) } },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface),
        modifier = Modifier.shadow(elevation = 2.dp, spotColor = GrayBorder)
    )
}

@Composable
private fun CurrentConnectionCard() {
    ManageDependentCard {
        ManageDependentSectionLabel(text = "Current Connection")
        Spacer(modifier = Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(52.dp).clip(CircleShape).background(TealLight), contentAlignment = Alignment.Center) {
                Text("AR", color = Teal, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
            }
            Column(Modifier.weight(1f)) {
                Text("Dato' Ahmad Razali", color = Navy, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text("Linked since May 2025", color = GrayMuted, fontSize = 12.sp)
            }
            ConnectedBadge()
        }
    }
}

@Composable
private fun ConnectedBadge() {
    Row(
        modifier = Modifier.clip(RoundedCornerShape(50)).background(GreenOnline.copy(alpha = 0.12f)).padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        PulsingDot(color = GreenOnline)
        Text("Connected", color = GreenOnline, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
    }
}

@Composable
private fun PulsingDot(color: Color) {
    val alpha by rememberInfiniteTransition(label = "pulse").animateFloat(1f, 0.3f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "alpha")
    Box(Modifier.size(7.dp).clip(CircleShape).background(color.copy(alpha = alpha)))
}

@Composable
private fun PairNewDependentSection(pin: String, onPinChange: (String) -> Unit, onConnect: () -> Unit, onScanQr: () -> Unit) {
    ManageDependentCard {
        ManageDependentSectionLabel("Pair New Dependent")
        Spacer(Modifier.height(10.dp))
        Text("Enter the 6-digit code displayed on the elderly user's device.", color = GrayMuted, fontSize = 13.sp)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = pin, onValueChange = onPinChange,
            modifier = Modifier.fillMaxWidth().height(64.dp),
            textStyle = TextStyle(textAlign = TextAlign.Center, fontSize = 28.sp, fontWeight = FontWeight.Bold, letterSpacing = 14.sp, color = Navy),
            placeholder = { Text("• • • • • •", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontSize = 22.sp, color = GrayMuted, letterSpacing = 10.sp) },
            singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Teal, unfocusedBorderColor = GrayLight, focusedContainerColor = TealLight.copy(alpha = 0.3f))
        )
        Spacer(Modifier.height(18.dp))
        Button(onClick = onConnect, enabled = pin.length == 6, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = if (pin.length == 6) Teal else GrayLight)) {
            Text("Connect Device", color = Color.White, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onScanQr, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp), border = BorderStroke(1.5.dp, Teal)) {
            Text("Scan QR Code Instead", color = Teal, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun RemoteInviteCard(onSend: () -> Unit) {
    ManageDependentCard {
        ManageDependentSectionLabel("Remote Invite")
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(TealLight), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.PhoneAndroid, null, tint = Teal, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Invite via SMS", color = Navy, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text("Send a setup link to the elderly user's phone", color = GrayMuted, fontSize = 12.sp)
            }
            TextButton(onClick = onSend, modifier = Modifier.clip(RoundedCornerShape(50)).background(TealLight)) {
                Text("Send", fontWeight = FontWeight.Bold, color = Teal)
            }
        }
    }
}

@Composable
private fun DangerZoneSection(onUnlink: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HorizontalDivider(
            color = GrayBorder,
            thickness = 1.dp
        )

        Spacer(Modifier.height(16.dp))

        TextButton(onClick = onUnlink) {
            Text(
                "Unlink Current Dependent",
                fontWeight = FontWeight.SemiBold,
                color = Crimson
            )
        }
    }
}

// Renamed and made private to avoid clashing with CaretakerScreens.kt
@Composable
private fun ManageDependentCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(18.dp), spotColor = Navy.copy(alpha = 0.08f)),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(0.dp)
    ) { Column(Modifier.padding(20.dp), content = content) }
}

// Renamed to avoid clashing
@Composable
private fun ManageDependentSectionLabel(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(3.dp).height(16.dp).clip(RoundedCornerShape(50)).background(Teal))
        Spacer(Modifier.width(8.dp))
        Text(text, color = Navy, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

// Renamed to avoid clashing
@Composable
private fun ManageDependentAnimatedSection(visible: Boolean, delayMillis: Int, content: @Composable () -> Unit) {
    AnimatedVisibility(visible = visible, enter = fadeIn(tween(400, delayMillis)) + slideInVertically(tween(400, delayMillis), initialOffsetY = { it / 5 })) { content() }
}