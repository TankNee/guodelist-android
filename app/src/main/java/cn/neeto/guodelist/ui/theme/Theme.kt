package cn.neeto.guodelist.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import cn.neeto.guodelist.core.settings.AppSettings

data class GuoColors(
    val canvas: Color,
    val panel: Color,
    val panel2: Color,
    val line: Color,
    val lineStrong: Color,
    val ink: Color,
    val ink2: Color,
    val ink3: Color,
    val brand: Color,
    val brandInk: Color,
    val brandTint: Color,
    val danger: Color,
    val success: Color,
    val onBrand: Color = Color.White,
)

private val classicLight = GuoColors(
    canvas = Color(0xFFEBE8E3),
    panel = Color(0xFFFFFEFC),
    panel2 = Color(0xFFF5F3EF),
    line = Color(0xFFE7E2DA),
    lineStrong = Color(0xFFD9D3C9),
    ink = Color(0xFF1C1A17),
    ink2 = Color(0xFF5F5A52),
    ink3 = Color(0xFF6D655B),
    brand = Color(0xFFE1543A),
    brandInk = Color(0xFFB83B25),
    brandTint = Color(0xFFFBEAE5),
    danger = Color(0xFFA82A1A),
    success = Color(0xFF4F7259),
)

private val classicDark = GuoColors(
    canvas = Color(0xFF121110),
    panel = Color(0xFF1A1917),
    panel2 = Color(0xFF211F1D),
    line = Color(0xFF2B2825),
    lineStrong = Color(0xFF3A3632),
    ink = Color(0xFFF2EFE9),
    ink2 = Color(0xFFB3ACA2),
    ink3 = Color(0xFF9A938A),
    brand = Color(0xFFF2694C),
    brandInk = Color(0xFFFF9078),
    brandTint = Color(0xFF2E1C17),
    danger = Color(0xFFFF7A66),
    success = Color(0xFF8FC2A4),
)

private val starryLight = GuoColors(
    canvas = Color(0xFFFFF5E6),
    panel = Color(0xFFFFFDF7),
    panel2 = Color(0xFFFFF3E2),
    line = Color(0xFFF0E2CE),
    lineStrong = Color(0xFFE4D2B8),
    ink = Color(0xFF2A211B),
    ink2 = Color(0xFF6B5B4D),
    ink3 = Color(0xFF8A7765),
    brand = Color(0xFFF2915F),
    brandInk = Color(0xFFA4562A),
    brandTint = Color(0xFFFDEBDF),
    danger = Color(0xFFA63A26),
    success = Color(0xFF4F7259),
)

private val starryDark = GuoColors(
    canvas = Color(0xFF14100D),
    panel = Color(0xFF1D1814),
    panel2 = Color(0xFF26201A),
    line = Color(0xFF33291F),
    lineStrong = Color(0xFF443628),
    ink = Color(0xFFF7EFE4),
    ink2 = Color(0xFFC4B5A3),
    ink3 = Color(0xFFA1907D),
    brand = Color(0xFFF6A47B),
    brandInk = Color(0xFFFFC3A3),
    brandTint = Color(0xFF33211A),
    danger = Color(0xFFFF8A72),
    success = Color(0xFF9ACCAF),
)

val LocalGuoColors = staticCompositionLocalOf { classicLight }

val GuoShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(11.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(22.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
)

val GuoTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.9).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.5).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 21.sp,
        lineHeight = 27.sp,
        letterSpacing = (-0.4).sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.1).sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 17.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.1.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        letterSpacing = 0.3.sp,
    ),
)

@Composable
fun GuoTheme(settings: AppSettings, content: @Composable () -> Unit) {
    val dark = when (settings.theme) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }
    val target = when (settings.pack) {
        "starry" -> if (dark) starryDark else starryLight
        else -> if (dark) classicDark else classicLight
    }
    val canvas by animateColorAsState(target.canvas, tween(260), label = "canvas")
    val panel by animateColorAsState(target.panel, tween(260), label = "panel")
    val panel2 by animateColorAsState(target.panel2, tween(260), label = "panel2")
    val line by animateColorAsState(target.line, tween(260), label = "line")
    val lineStrong by animateColorAsState(target.lineStrong, tween(260), label = "lineStrong")
    val ink by animateColorAsState(target.ink, tween(260), label = "ink")
    val ink2 by animateColorAsState(target.ink2, tween(260), label = "ink2")
    val ink3 by animateColorAsState(target.ink3, tween(260), label = "ink3")
    val brand by animateColorAsState(target.brand, tween(260), label = "brand")
    val brandInk by animateColorAsState(target.brandInk, tween(260), label = "brandInk")
    val brandTint by animateColorAsState(target.brandTint, tween(260), label = "brandTint")
    val danger by animateColorAsState(target.danger, tween(260), label = "danger")
    val success by animateColorAsState(target.success, tween(260), label = "success")
    val colors = GuoColors(
        canvas = canvas,
        panel = panel,
        panel2 = panel2,
        line = line,
        lineStrong = lineStrong,
        ink = ink,
        ink2 = ink2,
        ink3 = ink3,
        brand = brand,
        brandInk = brandInk,
        brandTint = brandTint,
        danger = danger,
        success = success,
    )

    val scheme = if (dark) {
        darkColorScheme(
            primary = colors.brand,
            onPrimary = colors.onBrand,
            primaryContainer = colors.brandTint,
            onPrimaryContainer = colors.brandInk,
            secondary = colors.brandInk,
            onSecondary = colors.panel,
            secondaryContainer = colors.brandTint,
            onSecondaryContainer = colors.brandInk,
            tertiary = colors.brandInk,
            onTertiary = colors.panel,
            tertiaryContainer = colors.brandTint,
            onTertiaryContainer = colors.brandInk,
            background = colors.canvas,
            onBackground = colors.ink,
            surface = colors.panel,
            onSurface = colors.ink,
            surfaceVariant = colors.panel2,
            onSurfaceVariant = colors.ink2,
            surfaceTint = colors.brand,
            surfaceContainerLowest = colors.panel,
            surfaceContainerLow = colors.panel,
            surfaceContainer = colors.panel2,
            surfaceContainerHigh = colors.panel2,
            surfaceContainerHighest = colors.panel2,
            inverseSurface = colors.ink,
            inverseOnSurface = colors.panel,
            inversePrimary = colors.brandTint,
            outline = colors.lineStrong,
            outlineVariant = colors.line,
            error = colors.danger,
            onError = Color.White,
            scrim = Color.Black,
        )
    } else {
        lightColorScheme(
            primary = colors.brand,
            onPrimary = colors.onBrand,
            primaryContainer = colors.brandTint,
            onPrimaryContainer = colors.brandInk,
            secondary = colors.brandInk,
            onSecondary = colors.panel,
            secondaryContainer = colors.brandTint,
            onSecondaryContainer = colors.brandInk,
            tertiary = colors.brandInk,
            onTertiary = colors.panel,
            tertiaryContainer = colors.brandTint,
            onTertiaryContainer = colors.brandInk,
            background = colors.canvas,
            onBackground = colors.ink,
            surface = colors.panel,
            onSurface = colors.ink,
            surfaceVariant = colors.panel2,
            onSurfaceVariant = colors.ink2,
            surfaceTint = colors.brand,
            surfaceContainerLowest = colors.panel,
            surfaceContainerLow = colors.panel,
            surfaceContainer = colors.panel2,
            surfaceContainerHigh = colors.panel2,
            surfaceContainerHighest = colors.panel2,
            inverseSurface = colors.ink,
            inverseOnSurface = colors.panel,
            inversePrimary = colors.brandTint,
            outline = colors.lineStrong,
            outlineVariant = colors.line,
            error = colors.danger,
            onError = Color.White,
            scrim = Color.Black,
        )
    }

    val view = LocalView.current
    val activity = LocalContext.current.findActivity()
    SideEffect {
        val window = activity?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        }
    }

    CompositionLocalProvider(LocalGuoColors provides colors) {
        MaterialTheme(
            colorScheme = scheme,
            typography = GuoTypography,
            shapes = GuoShapes,
            content = content,
        )
    }
}

val guoColors: GuoColors
    @Composable
    get() = LocalGuoColors.current

private fun Context.findActivity(): Activity? {
    var current: Context = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
