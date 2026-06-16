package com.group.talihayat.ui.elderly

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.automirrored.filled.ArrowBack
enum class AppFontSize(
    val label:       String,
    val badge:       String,
    val previewSp:   TextUnit,   // used for the live preview label
) {
    Normal     ("Normal",      "A",   16.sp),
    Large      ("Large",       "A+",  20.sp),
    ExtraLarge ("Extra Large", "A++", 26.sp),
}

// ─────────────────────────────────────────────────────────────────────────────
//  COLOUR PALETTES — standard vs high-contrast (Clean Refactored Version)
// ─────────────────────────────────────────────────────────────────────────────
private data class A11yPalette(
    val background:  Color,
    val surface:     Color,
    val cardBg:      Color,
    val textPrimary: Color,
    val textMuted:   Color,
    val accent:      Color,
    val accentLight: Color,
    val divider:     Color,
    val iconTint:    Color,
    val switchTrack: Color,
)

private val StandardPalette = A11yPalette(
    background  = com.group.talihayat.ui.theme.Background,
    surface     = com.group.talihayat.ui.theme.Surface,
    cardBg      = com.group.talihayat.ui.theme.Surface,
    textPrimary = com.group.talihayat.ui.theme.Navy,
    textMuted   = com.group.talihayat.ui.theme.GrayMuted,
    accent      = com.group.talihayat.ui.theme.Teal,
    accentLight = com.group.talihayat.ui.theme.TealLight,
    divider     = com.group.talihayat.ui.theme.GrayLight,
    iconTint    = com.group.talihayat.ui.theme.Teal,
    switchTrack = com.group.talihayat.ui.theme.Teal,
)

private val HighContrastPalette = A11yPalette(
    background  = com.group.talihayat.ui.theme.ContrastBlack,
    surface     = com.group.talihayat.ui.theme.ContrastBlack,
    cardBg      = com.group.talihayat.ui.theme.ContrastCardBg,
    textPrimary = com.group.talihayat.ui.theme.NeonYellow,   // Maximum luminosity contrast
    textMuted   = com.group.talihayat.ui.theme.Surface,      // Crisp pure white
    accent      = com.group.talihayat.ui.theme.NeonYellow,
    accentLight = com.group.talihayat.ui.theme.ContrastDarkBg,
    divider     = com.group.talihayat.ui.theme.ContrastDivider,
    iconTint    = com.group.talihayat.ui.theme.NeonYellow,
    switchTrack = com.group.talihayat.ui.theme.NeonYellow,
)

// ─────────────────────────────────────────────────────────────────────────────
//  ROOT SCREEN
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccessibilityHubScreen(
    initialFontSize:       AppFontSize = AppFontSize.Normal,
    initialHighContrast:   Boolean     = false,
    onFontSizeChange:      (AppFontSize) -> Unit = {},
    onHighContrastChange:  (Boolean)    -> Unit = {},
    onBackClick:           () -> Unit = {},
) {
    // ── Local UI state (caller should hoist to ViewModel for persistence) ─────
    var selectedFontSize by remember { mutableStateOf(initialFontSize) }
    var highContrast     by remember { mutableStateOf(initialHighContrast) }

    // Derive active palette — recomposes the entire hierarchy when toggled
    val palette = if (highContrast) HighContrastPalette else StandardPalette

    // Smooth background transition so the contrast flip doesn't feel jarring
    val bgColor by animateColorAsState(
        targetValue   = palette.background,
        animationSpec = tween(durationMillis = 380),
        label         = "background_transition",
    )

    Scaffold(
        containerColor = bgColor,
        topBar = {
            AccessibilityTopBar(
                palette     = palette,
                onBackClick = onBackClick,
            )
        },
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {

            // ① Font Size Selector
            FontSizeSection(
                selectedSize  = selectedFontSize,
                onSelect      = {
                    selectedFontSize = it
                    onFontSizeChange(it)
                },
                palette       = palette,
            )

            A11yDivider(palette)

            // ② High Contrast Mode toggle
            HighContrastSection(
                isEnabled  = highContrast,
                onToggle   = {
                    highContrast = it
                    onHighContrastChange(it)
                },
                palette    = palette,
            )

            A11yDivider(palette)

            // ③ Preview strip — shows the combined effect in real time
            LivePreviewCard(
                fontSample   = selectedFontSize,
                highContrast = highContrast,
                palette      = palette,
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  TOP APP BAR
//  Adapts colours to the active palette so contrast-mode flips the bar too.
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccessibilityTopBar(
    palette:     A11yPalette,
    onBackClick: () -> Unit,
) {
    val barBg by animateColorAsState(
        targetValue   = palette.surface,
        animationSpec = tween(380),
        label         = "topbar_bg",
    )

    TopAppBar(
        title = {
            Text(
                text       = "Accessibility Hub",
                fontSize   = 20.sp,
                fontWeight = FontWeight.Bold,
                color      = palette.textPrimary,
            )
        },
        navigationIcon = {
            IconButton(
                onClick  = onBackClick,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to Settings",
                    tint               = palette.iconTint,
                    modifier           = Modifier.size(28.dp),
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = barBg),
    )
}

// ─────────────────────────────────────────────────────────────────────────────
//  FONT SIZE SECTION
//  Three large discrete buttons — no slider to fight with tremor-affected hands.
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun FontSizeSection(
    selectedSize: AppFontSize,
    onSelect:     (AppFontSize) -> Unit,
    palette:      A11yPalette,
) {
    A11yCard(palette = palette) {
        // Section header row
        SectionHeader(
            icon    = Icons.Default.TextFields,
            title   = "Font Size",
            subtitle = "Choose the text size most comfortable for your eyes",
            palette = palette,
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Three-button block — equal weight so all are equally easy to tap
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AppFontSize.entries.forEach { tier ->
                FontSizeTile(
                    tier       = tier,
                    isSelected = tier == selectedSize,
                    onClick    = { onSelect(tier) },
                    palette    = palette,
                    modifier   = Modifier.weight(1f),
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  FONT SIZE TILE
//  Individual selectable block. Animates bg + border when selected.
//  Badge letter scales with the tier so users immediately grasp the hierarchy.
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun FontSizeTile(
    tier:       AppFontSize,
    isSelected: Boolean,
    onClick:    () -> Unit,
    palette:    A11yPalette,
    modifier:   Modifier = Modifier,
) {
    val bgColor by animateColorAsState(
        targetValue   = if (isSelected) palette.accent else palette.cardBg,
        animationSpec = tween(220),
        label         = "font_tile_bg_${tier.name}",
    )
    val textColor = if (isSelected) {
        // In high-contrast mode the accent is yellow — use black text on yellow
        if (palette.textPrimary == HighContrastPalette.textPrimary) Color.Black else Color.White
    } else {
        palette.textPrimary
    }
    val borderColor = if (isSelected) palette.accent
    else if (palette == HighContrastPalette) com.group.talihayat.ui.theme.ContrastBorder
                      else Color(0xFFECEFF1)

    Column(
        modifier = modifier
            .height(100.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(
                width = if (isSelected) 2.5.dp else 1.5.dp,
                color = borderColor,
                shape = RoundedCornerShape(14.dp),
            )
            // Delete the old .clickable block and replace it with this:
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Badge glyph — scaled per tier for immediate visual differentiation
        Text(
            text       = tier.badge,
            fontSize   = tier.previewSp,
            fontWeight = FontWeight.ExtraBold,
            color      = textColor,
            textAlign  = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text       = tier.label,
            fontSize   = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color      = textColor.copy(alpha = 0.75f),
            textAlign  = TextAlign.Center,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  HIGH CONTRAST SECTION
//  A single large ON/OFF switch with rich explanatory copy.
//  When toggled, the entire palette immediately reacts.
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun HighContrastSection(
    isEnabled: Boolean,
    onToggle:  (Boolean) -> Unit,
    palette:   A11yPalette,
) {
    A11yCard(palette = palette) {
        SectionHeader(
            icon     = Icons.Default.Contrast,
            title    = "High Contrast Mode",
            subtitle = "Replaces soft backgrounds with black surfaces and neon text for maximum visibility",
            palette  = palette,
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Toggle row — large tap zone for the switch + tap anywhere on row
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(palette.accentLight)
                .clickable { onToggle(!isEnabled) }
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment           = Alignment.CenterVertically,
            horizontalArrangement       = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = if (isEnabled) "ON — Maximum contrast active" else "OFF — Standard theme active",
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color      = palette.textPrimary,
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text     = if (isEnabled) "Black background · Neon yellow text"
                               else "Tap to enable high-contrast display",
                    fontSize = 12.sp,
                    color    = palette.textMuted,
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Switch — oversized via scale modifier so the thumb is easily grabbable
            Switch(
                checked         = isEnabled,
                onCheckedChange = onToggle,
                modifier        = Modifier.size(width = 56.dp, height = 32.dp),
                colors          = SwitchDefaults.colors(
                    checkedThumbColor       = Color.White,
                    checkedTrackColor       = palette.switchTrack,
                    uncheckedThumbColor     = palette.textMuted,
                    uncheckedTrackColor     = palette.accentLight,
                    uncheckedBorderColor    = palette.textMuted.copy(alpha = 0.4f),
                ),
            )
        }

        // Extra explanatory note when contrast is ON — reinforces what changed
        if (isEnabled) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(text = "⚡", fontSize = 16.sp)
                Text(
                    text       = "High contrast is active. All screens now use black surfaces with neon-yellow or white text to reduce glare.",
                    fontSize   = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color      = palette.textPrimary,
                    lineHeight = 18.sp,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  LIVE PREVIEW CARD
//  Shows the user a concrete sample of the combined font size + contrast
//  settings so they can evaluate before leaving the screen.
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun LivePreviewCard(
    fontSample:   AppFontSize,
    highContrast: Boolean,
    palette:      A11yPalette,
) {
    A11yCard(palette = palette) {
        Text(
            text       = "Preview",
            fontSize   = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color      = palette.textMuted,
            letterSpacing = 1.sp,
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Sample headline
        Text(
            text       = "The quick brown fox",
            fontSize   = fontSample.previewSp,
            fontWeight = FontWeight.Bold,
            color      = palette.textPrimary,
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Sample body copy
        Text(
            text       = "This is how body text will appear across the TaliHayat application with your current settings.",
            fontSize   = (fontSample.previewSp.value * 0.75f).sp,
            color      = palette.textMuted,
            lineHeight  = (fontSample.previewSp.value * 1.1f).sp,
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Status pill showing current settings combination
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(palette.accent.copy(alpha = 0.15f))
                .border(
                    width = 1.dp,
                    color = palette.accent.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(50),
                )
                .padding(horizontal = 14.dp, vertical = 6.dp),
        ) {
            Text(
                text       = "${fontSample.badge} · ${if (highContrast) "High Contrast ON" else "Standard Theme"}",
                fontSize   = 12.sp,
                fontWeight = FontWeight.Bold,
                color      = palette.textPrimary,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  SHARED COMPONENTS
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Palette-aware card shell — consistent shadow + surface colour.
 * Background and border both animate when the contrast palette changes.
 */
@Composable
private fun A11yCard(
    palette:  A11yPalette,
    modifier: Modifier = Modifier,
    content:  @Composable ColumnScope.() -> Unit,
) {
    val cardBg by animateColorAsState(
        targetValue   = palette.cardBg,
        animationSpec = tween(380),
        label         = "card_bg",
    )

    Card(
        modifier  = modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(18.dp),
        colors    = CardDefaults.cardColors(containerColor = cardBg),
        border    = BorderStroke(
            width = 1.dp,
            color = if (palette == HighContrastPalette) Color(0xFF333333) else Color(0xFFF0F2F5),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            content  = content,
        )
    }
}

/**
 * Icon + title + subtitle header row shared by both feature sections.
 */
@Composable
private fun SectionHeader(
    icon:     ImageVector,
    title:    String,
    subtitle: String,
    palette:  A11yPalette,
) {
    Row(
        verticalAlignment     = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Icon container — palette-aware tint
        Box(
            modifier         = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(palette.accentLight),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector        = icon,
                contentDescription = null,
                tint               = palette.iconTint,
                modifier           = Modifier.size(24.dp),
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = title,
                fontSize   = 17.sp,
                fontWeight = FontWeight.Bold,
                color      = palette.textPrimary,
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text       = subtitle,
                fontSize   = 12.sp,
                color      = palette.textMuted,
                lineHeight = 17.sp,
            )
        }
    }
}

/**
 * Thin horizontal rule that adapts to the active palette's divider colour.
 */
@Composable
private fun A11yDivider(palette: A11yPalette) {
    val dividerColor by animateColorAsState(
        targetValue   = palette.divider,
        animationSpec = tween(380),
        label         = "divider_color",
    )
    HorizontalDivider(color = dividerColor, thickness = 1.dp)
}
