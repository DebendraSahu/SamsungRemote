package com.samsung_remote.ui

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * The press behaviour every key shares: a tick the moment the finger lands, and -- for the keys
 * you would hold down on a real remote -- a repeat once the first beat has passed. Sliding off
 * before lifting cancels, which is why a key that doesn't repeat fires on release, not on touch.
 */
@Composable
internal fun holdable(
    repeatable: Boolean,
    onPressedChange: (Boolean) -> Unit,
    onFire: () -> Unit,
): Modifier {
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    // The gesture detector outlives recomposition, so it has to read the latest lambdas rather
    // than the ones it closed over when the press began.
    val fire by rememberUpdatedState(onFire)
    val changed by rememberUpdatedState(onPressedChange)
    // The tap detector below adds no accessibility action of its own, so without this a
    // screen reader would announce a button that double-tapping cannot press.
    return Modifier.semantics {
        onClick {
            fire()
            true
        }
    }.pointerInput(repeatable) {
        detectTapGestures(
            onPress = {
                changed(true)
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                var repeat: Job? = null
                if (repeatable) {
                    fire()
                    repeat = scope.launch {
                        delay(400)
                        while (isActive) {
                            fire()
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            delay(110)
                        }
                    }
                }
                val completed = tryAwaitRelease()
                repeat?.cancel()
                changed(false)
                if (completed && !repeatable) fire()
            }
        )
    }
}

/**
 * One key. [base] is the colour of its plastic; the highlight, the face gradient and the way it
 * collapses under a finger are all derived from it.
 */
@Composable
internal fun Key(
    modifier: Modifier = Modifier,
    base: Color = KeyPlastic,
    shape: Shape = RoundedCornerShape(18.dp),
    elevation: Dp = 6.dp,
    repeatable: Boolean = false,
    desc: String? = null,
    onClick: () -> Unit,
    content: @Composable BoxScope.(pressed: Boolean) -> Unit,
) {
    var pressed by remember { mutableStateOf(false) }
    val lift by animateDpAsState(
        targetValue = if (pressed) 0.dp else elevation,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "lift",
    )
    val squash by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow),
        label = "squash",
    )

    Box(
        modifier = modifier
            .scale(squash)
            .shadow(lift, shape, clip = false, ambientColor = Color.Black, spotColor = Color.Black)
            .moulded(shape, base, pressed = pressed)
            .semantics {
                role = Role.Button
                if (desc != null) contentDescription = desc
            }
            .then(holdable(repeatable, { pressed = it }, onClick)),
        contentAlignment = Alignment.Center,
    ) {
        content(pressed)
    }
}
