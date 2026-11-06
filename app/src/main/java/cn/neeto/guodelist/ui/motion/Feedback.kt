package cn.neeto.guodelist.ui.motion

import android.view.SoundEffectConstants
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView

/** Subtle, consistent touch feedback: one light tick plus the system click sound. */
@Composable
fun rememberClickFeedback(): () -> Unit {
    val haptics = LocalHapticFeedback.current
    val view = LocalView.current
    return remember(haptics, view) {
        {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            view.playSoundEffect(SoundEffectConstants.CLICK)
        }
    }
}

fun Modifier.guoClickable(
    enabled: Boolean = true,
    onClick: () -> Unit,
): Modifier = composed {
    val feedback = rememberClickFeedback()
    clickable(enabled = enabled) {
        feedback()
        onClick()
    }
}

fun Modifier.guoClickable(
    interactionSource: MutableInteractionSource,
    enabled: Boolean = true,
    onClick: () -> Unit,
): Modifier = composed {
    val feedback = rememberClickFeedback()
    clickable(
        interactionSource = interactionSource,
        indication = androidx.compose.foundation.LocalIndication.current,
        enabled = enabled,
    ) {
        feedback()
        onClick()
    }
}
