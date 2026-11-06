package cn.neeto.guodelist.ui.motion

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Motion language, borrowed from the way openGym moves: short, physical,
 * interruptible. Springs do the heavy lifting for anything the finger touches;
 * cubic-beziers are reserved for content that appears without a gesture.
 *
 * Rules of thumb:
 *  - a finger-driven change settles in 200–320 ms and may overshoot slightly;
 *  - a tap acknowledgement is 90–140 ms and never overshoots;
 *  - lists move with [Springs.gentle]; sheets use Material's own spring;
 *  - nothing animates on first paint except a 120 ms fade of new content.
 */
object GuoMotion {

    const val TAP_MS = 120
    const val QUICK_MS = 180
    const val STANDARD_MS = 240
    const val SLOW_MS = 320

    /** Matches the desktop's `cubic-bezier(.16,1,.3,1)`. */
    val emphasized = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
    val standard = FastOutSlowInEasing
    val decelerate = CubicBezierEasing(0f, 0f, 0.2f, 1f)

    fun <T> gentle(): androidx.compose.animation.core.SpringSpec<T> =
        spring(dampingRatio = 0.86f, stiffness = 380f)

    fun <T> snappy(): androidx.compose.animation.core.SpringSpec<T> =
        spring(dampingRatio = 0.72f, stiffness = 900f)

    fun <T> bouncy(): androidx.compose.animation.core.SpringSpec<T> =
        spring(dampingRatio = 0.55f, stiffness = 520f)

    val tap = tween<Float>(durationMillis = TAP_MS, easing = decelerate)
    val quick = tween<Float>(durationMillis = QUICK_MS, easing = standard)
    val standardTween = tween<Float>(durationMillis = STANDARD_MS, easing = emphasized)

    /** Screen-level push: mostly a fade, with 6% horizontal travel. */
    fun pushIn() = slideInHorizontally(animationSpec = tween(SLOW_MS, easing = emphasized)) { it / 14 } +
        fadeIn(animationSpec = tween(STANDARD_MS, delayMillis = 40))

    fun pushOut() = slideOutHorizontally(animationSpec = tween(QUICK_MS, easing = standard)) { it / 20 } +
        fadeOut(animationSpec = tween(TAP_MS))

    fun sheetIn() = slideInVertically(animationSpec = spring(dampingRatio = 0.9f, stiffness = 420f)) { it / 10 } +
        fadeIn(animationSpec = tween(QUICK_MS))

    fun sheetOut() = slideOutVertically(animationSpec = tween(QUICK_MS, easing = standard)) { it / 12 } +
        fadeOut(animationSpec = tween(TAP_MS))

    fun fadeThrough() = fadeIn(animationSpec = tween(QUICK_MS)) + slideInVertically(
        animationSpec = tween(STANDARD_MS, easing = emphasized),
    ) { it / 24 }

    fun exitFade() = fadeOut(animationSpec = tween(TAP_MS)) + slideOutVertically(
        animationSpec = tween(QUICK_MS, easing = standard),
    ) { it / 30 }

    val springGentle = spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 380f)
    val springSnappy = spring<Float>(dampingRatio = 0.78f, stiffness = 850f)
}

/** Press feedback used by rows, chips and icon buttons. */
@Composable
fun Modifier.pressScale(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.965f,
    stiffness: Int = 1000,
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = stiffness.toFloat()),
        label = "pressScale",
    )
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
