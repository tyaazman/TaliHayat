package com.group.talihayat.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf

// ─────────────────────────────────────────────────────────────────────────────
//  CORE BASE COLOR PRIMITIVES
// ─────────────────────────────────────────────────────────────────────────────
val Background      = Color(0xFFF8F9FA)
val Surface         = Color(0xFFFFFFFF)
val PrimaryText     = Color(0xFF1A1A1A)
val SecondaryText   = Color(0xFF616161)
val Teal            = Color(0xFF00A8B5)
val TealDark        = Color(0xFF007A85)
val TealLight       = Color(0xFFE0F7FA)
val Crimson         = Color(0xFFFF5757)
val CrimsonLight    = Color(0xFFFFEBEE)
val Navy            = Color(0xFF1E3A5F)
val NavyDark        = Color(0xFF2D5288)
val NavySurface     = Color(0xFFE8EEF6)
val GrayMuted       = Color(0xFF8A9BB0)
val GrayLight       = Color(0xFFECEFF1)
val GrayBorder      = Color(0xFFE0E0E0)
val GlowTeal        = Color(0x3000A8B5)
val CardShadow      = Color(0x14000000)
val InputBorder     = Color(0xFFDDE3EC)
val Error           = Color(0xFFFF5757)
var isHighContrastModeActive by mutableStateOf(false)
// ─────────────────────────────────────────────────────────────────────────────
//  STATUS, SENSOR & CRITICAL ALERT CORES
// ─────────────────────────────────────────────────────────────────────────────
val HeartbeatGreen   = Color(0xFF00C853)
val GreenOnline      = Color(0xFF2ECC71)
val AmberWarning     = Color(0xFFFFA000)
val AmberLight       = Color(0xFFFFF8E1)
val CancelYellow     = Color(0xFFFFD600)
val SentBackground   = Color(0xFF1B5E20)
val DangerBackground = Color(0xFFD32F2F)

// ─────────────────────────────────────────────────────────────────────────────
//  CAREGIVER SIDE / LEGACY TOP-LEVEL BINDINGS
// ─────────────────────────────────────────────────────────────────────────────
val LBackground      = Background
val LSurface         = Surface
val LTealSafe        = Teal
val LTealDark        = TealDark
val LTealLight       = TealLight
val LCrimsonAlert    = Crimson
val LNavy            = Navy
val LNavyLight       = NavySurface
val LGrayMuted       = GrayMuted
val LGrayLight       = GrayLight
val LSuccessGreen    = HeartbeatGreen
val LSuccessLight    = HeartbeatGreen.copy(alpha = 0.12f)
val LCardShadow      = CardShadow

// High Contrast Specific Primitives
val ContrastBlack      = Color(0xFF000000)
val ContrastCardBg     = Color(0xFF111111)
val NeonYellow         = Color(0xFFFFE600)
val ContrastDarkBg     = Color(0xFF1A1700)
val ContrastDivider    = Color(0xFF333333)
val ContrastBorder     = Color(0xFF444444)

// ─────────────────────────────────────────────────────────────────────────────
//  CORE OBJECT INJECTION PALETTES
// ─────────────────────────────────────────────────────────────────────────────
object TaliColors {
    val Background    = com.group.talihayat.ui.theme.Background
    val Surface       = com.group.talihayat.ui.theme.Surface
    val TealSafe      = com.group.talihayat.ui.theme.Teal
    val TealLight     = com.group.talihayat.ui.theme.TealLight
    val TealDark      = com.group.talihayat.ui.theme.TealDark
    val CrimsonAlert  = com.group.talihayat.ui.theme.Crimson
    val CrimsonLight  = com.group.talihayat.ui.theme.CrimsonLight
    val Navy          = com.group.talihayat.ui.theme.Navy
    val NavyLight     = com.group.talihayat.ui.theme.NavySurface
    val GrayMuted     = com.group.talihayat.ui.theme.GrayMuted
    val GrayLight     = com.group.talihayat.ui.theme.GrayLight
    val CardShadow    = com.group.talihayat.ui.theme.CardShadow
    val Divider       = com.group.talihayat.ui.theme.InputBorder
    val GreenOnline   = com.group.talihayat.ui.theme.GreenOnline
    val AmberWarning  = com.group.talihayat.ui.theme.AmberWarning
}

object ElderlyColors {
    val Background: Color       get() = if (isHighContrastModeActive) Color(0xFF000000) else com.group.talihayat.ui.theme.Background
    val Surface: Color          get() = if (isHighContrastModeActive) Color(0xFF000000) else com.group.talihayat.ui.theme.Surface
    val PrimaryText: Color      get() = if (isHighContrastModeActive) Color(0xFFFFE600) else com.group.talihayat.ui.theme.PrimaryText
    val SecondaryText: Color    get() = if (isHighContrastModeActive) Color(0xFFFFFFFF) else com.group.talihayat.ui.theme.SecondaryText
    val Teal: Color             get() = if (isHighContrastModeActive) Color(0xFFFFE600) else com.group.talihayat.ui.theme.Teal
    val TealLight: Color        get() = if (isHighContrastModeActive) Color(0xFF1A1700) else com.group.talihayat.ui.theme.TealLight
    val TealDark: Color         get() = if (isHighContrastModeActive) Color(0xFFFFE600) else com.group.talihayat.ui.theme.TealDark
    val Crimson: Color          get() = if (isHighContrastModeActive) Color(0xFFFFE600) else com.group.talihayat.ui.theme.Crimson
    val CrimsonLight: Color     get() = if (isHighContrastModeActive) Color(0xFF1A1700) else com.group.talihayat.ui.theme.CrimsonLight
    val Navy: Color             get() = if (isHighContrastModeActive) Color(0xFFFFE600) else com.group.talihayat.ui.theme.Navy
    val NavyBlue: Color         get() = if (isHighContrastModeActive) Color(0xFFFFE600) else com.group.talihayat.ui.theme.Navy
    val NavyDark: Color         get() = if (isHighContrastModeActive) Color(0xFFFFE600) else com.group.talihayat.ui.theme.NavyDark
    val NavySurface: Color      get() = if (isHighContrastModeActive) Color(0xFF111111) else com.group.talihayat.ui.theme.NavySurface
    val GrayMuted: Color        get() = if (isHighContrastModeActive) Color(0xFFFFFFFF) else com.group.talihayat.ui.theme.GrayMuted
    val GrayLight: Color        get() = if (isHighContrastModeActive) Color(0xFF111111) else com.group.talihayat.ui.theme.GrayLight
    val GrayBorder: Color       get() = if (isHighContrastModeActive) Color(0xFF333333) else com.group.talihayat.ui.theme.GrayBorder
    val CardShadow: Color       get() = if (isHighContrastModeActive) Color.Transparent else com.group.talihayat.ui.theme.CardShadow
    val GreenOnline: Color      get() = if (isHighContrastModeActive) Color(0xFFFFE600) else com.group.talihayat.ui.theme.GreenOnline
    val AmberWarning: Color     get() = if (isHighContrastModeActive) Color(0xFFFFE600) else com.group.talihayat.ui.theme.AmberWarning
    val AmberLight: Color       get() = if (isHighContrastModeActive) Color(0xFF1A1700) else com.group.talihayat.ui.theme.AmberLight
    val SafeBackground: Color    get() = if (isHighContrastModeActive) Color(0xFF000000) else com.group.talihayat.ui.theme.Background
    val HeartbeatGreen: Color    get() = if (isHighContrastModeActive) Color(0xFFFFE600) else com.group.talihayat.ui.theme.HeartbeatGreen
    val CancelYellow: Color      get() = if (isHighContrastModeActive) Color(0xFFFFE600) else com.group.talihayat.ui.theme.CancelYellow
    val SentBackground: Color    get() = if (isHighContrastModeActive) Color(0xFFFFE600) else com.group.talihayat.ui.theme.SentBackground
    val DangerBackground: Color  get() = if (isHighContrastModeActive) Color(0xFF000000) else com.group.talihayat.ui.theme.DangerBackground
}
