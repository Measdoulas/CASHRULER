package com.cashruler.ui.animations

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

private const val STATE_ANIMATION_DURATION = 300

/**
 * Animation de transition entre états de chargement
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun LoadingTransition(
    loading: Boolean,
    content: @Composable AnimatedVisibilityScope.() -> Unit
) {
    AnimatedVisibility(
        visible = loading,
        enter = fadeIn(animationSpec = tween(STATE_ANIMATION_DURATION)) +
                scaleIn(
                    initialScale = 0.8f,
                    animationSpec = tween(STATE_ANIMATION_DURATION)
                ),
        exit = fadeOut(animationSpec = tween(STATE_ANIMATION_DURATION)) +
                scaleOut(
                    targetScale = 0.8f,
                    animationSpec = tween(STATE_ANIMATION_DURATION)
                ),
        content = content
    )
}

/**
 * Animation de transition entre états d'erreur
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ErrorTransition(
    error: Boolean,
    content: @Composable AnimatedVisibilityScope.() -> Unit
) {
    AnimatedVisibility(
        visible = error,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(STATE_ANIMATION_DURATION)
        ) + fadeIn(animationSpec = tween(STATE_ANIMATION_DURATION)),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(STATE_ANIMATION_DURATION)
        ) + fadeOut(animationSpec = tween(STATE_ANIMATION_DURATION)),
        content = content
    )
}

/**
 * Animation de transition pour les changements d'état de succès
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SuccessTransition(
    success: Boolean,
    content: @Composable AnimatedVisibilityScope.() -> Unit
) {
    AnimatedVisibility(
        visible = success,
        enter = expandIn(
            expandFrom = androidx.compose.ui.Alignment.Center,
            animationSpec = tween(STATE_ANIMATION_DURATION)
        ) + fadeIn(animationSpec = tween(STATE_ANIMATION_DURATION)),
        exit = shrinkOut(
            shrinkTowards = androidx.compose.ui.Alignment.Center,
            animationSpec = tween(STATE_ANIMATION_DURATION)
        ) + fadeOut(animationSpec = tween(STATE_ANIMATION_DURATION)),
        content = content
    )
}

/**
 * Animation pour les états de progression
 */
@Composable
fun progressAnimation(
    progress: Float,
    modifier: Modifier = Modifier
): Modifier {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(
            durationMillis = STATE_ANIMATION_DURATION,
            easing = FastOutSlowInEasing
        )
    )

    return modifier.graphicsLayer {
        this.alpha = animatedProgress
    }
}

/**
 * Animation pour les changements d'état de visibilité
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ContentVisibilityTransition(
    visible: Boolean,
    content: @Composable AnimatedVisibilityScope.() -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(STATE_ANIMATION_DURATION)) +
                expandVertically(
                    animationSpec = tween(STATE_ANIMATION_DURATION),
                    expandFrom = androidx.compose.ui.Alignment.Top
                ),
        exit = fadeOut(animationSpec = tween(STATE_ANIMATION_DURATION)) +
                shrinkVertically(
                    animationSpec = tween(STATE_ANIMATION_DURATION),
                    shrinkTowards = androidx.compose.ui.Alignment.Top
                ),
        content = content
    )
}

/**
 * Animation pour les transitions d'état de validation
 */
@Composable
fun validationStateAnimation(
    isValid: Boolean,
    modifier: Modifier = Modifier
): Modifier {
    val shake by animateFloatAsState(
        targetValue = if (!isValid) 10f else 0f,
        animationSpec = repeatable(
            iterations = 3,
            animation = tween(
                durationMillis = 50,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        )
    )

    return modifier.graphicsLayer {
        translationX = shake
    }
}

/**
 * Animation pour les changements d'état de focus
 */
@Composable
fun focusStateAnimation(
    isFocused: Boolean,
    modifier: Modifier = Modifier
): Modifier {
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.02f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    return modifier.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Animation pour les transitions d'état de sélection
 */
@Composable
fun selectionStateAnimation(
    isSelected: Boolean,
    modifier: Modifier = Modifier
): Modifier {
    val elevation by animateDpAsState(
        targetValue = if (isSelected) androidx.compose.ui.unit.dp(8f) else androidx.compose.ui.unit.dp(1f),
        animationSpec = tween(
            durationMillis = STATE_ANIMATION_DURATION,
            easing = FastOutSlowInEasing
        )
    )

    return modifier.graphicsLayer {
        translationY = -elevation.value.value
    }
}
