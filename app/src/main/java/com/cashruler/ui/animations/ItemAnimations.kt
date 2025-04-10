package com.cashruler.ui.animations

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt

private const val ANIMATION_DURATION = 300
private const val STAGGER_DELAY = 50

/**
 * Animation pour l'entrée d'un élément dans une liste
 */
@OptIn(ExperimentalAnimationApi::class)
fun listItemEnterTransition(
    index: Int = 0,
    initialOffsetX: Int = 300,
    delayPerItem: Int = STAGGER_DELAY
): EnterTransition = slideInHorizontally(
    animationSpec = tween(
        durationMillis = ANIMATION_DURATION,
        delayMillis = index * delayPerItem,
        easing = FastOutSlowInEasing
    ),
    initialOffsetX = { initialOffsetX }
) + fadeIn(
    animationSpec = tween(
        durationMillis = ANIMATION_DURATION,
        delayMillis = index * delayPerItem,
        easing = FastOutSlowInEasing
    )
)

/**
 * Animation pour la sortie d'un élément d'une liste
 */
@OptIn(ExperimentalAnimationApi::class)
fun listItemExitTransition(
    targetOffsetX: Int = -300
): ExitTransition = slideOutHorizontally(
    animationSpec = tween(
        durationMillis = ANIMATION_DURATION,
        easing = FastOutSlowInEasing
    ),
    targetOffsetX = { targetOffsetX }
) + fadeOut(
    animationSpec = tween(
        durationMillis = ANIMATION_DURATION,
        easing = FastOutSlowInEasing
    )
)

/**
 * Animation de déplacement d'un élément lors du glissement
 */
@Composable
fun swipeToDismissAnimation(
    isActive: Boolean,
    maxOffset: Float
): Modifier {
    val offsetX by animateFloatAsState(
        targetValue = if (isActive) maxOffset else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    return Modifier.offset { IntOffset(offsetX.roundToInt(), 0) }
}

/**
 * Animation de pulsation pour mettre en évidence un élément
 */
@Composable
fun pulseAnimation(
    trigger: Boolean = true,
    pulseScale: Float = 1.2f
): Modifier {
    val scale by animateFloatAsState(
        targetValue = if (trigger) pulseScale else 1f,
        animationSpec = repeatable(
            iterations = 1,
            animation = tween(
                durationMillis = 200,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        )
    )

    return Modifier.animateContentSize()
}

/**
 * Animation d'expansion pour un élément dépliable
 */
@Composable
fun expandableItemAnimation(
    expanded: Boolean,
    maxHeight: Int
): Modifier {
    val expandProgress by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        )
    )

    return Modifier.animateContentSize(
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        )
    )
}

/**
 * Animation de secouement pour indiquer une erreur
 */
@Composable
fun shakeAnimation(
    trigger: Boolean = false,
    shakeIntensity: Float = 10f
): Modifier {
    val offsetX by animateFloatAsState(
        targetValue = if (trigger) shakeIntensity else 0f,
        animationSpec = repeatable(
            iterations = 3,
            animation = tween(
                durationMillis = 50,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        )
    )

    return Modifier.offset { IntOffset(offsetX.roundToInt(), 0) }
}

/**
 * Animation de rebond pour un élément
 */
@Composable
fun bounceAnimation(
    trigger: Boolean = false,
    bounceHeight: Float = 20f
): Modifier {
    val offsetY by animateFloatAsState(
        targetValue = if (trigger) -bounceHeight else 0f,
        animationSpec = repeatable(
            iterations = 1,
            animation = tween(
                durationMillis = 300,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        )
    )

    return Modifier.offset { IntOffset(0, offsetY.roundToInt()) }
}
