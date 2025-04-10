package com.cashruler.ui.animations

import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Modifier

private const val CHART_ANIMATION_DURATION = 800
private const val BAR_STAGGER_DELAY = 50
private const val PIE_SEGMENT_DELAY = 100

/**
 * Animation pour les barres d'un graphique en barres
 */
@Composable
fun barChartAnimation(
    index: Int = 0,
    heightFraction: Float = 1f
): Modifier {
    var currentHeight by remember { mutableStateOf(0f) }
    
    LaunchedEffect(heightFraction) {
        animate(
            initialValue = currentHeight,
            targetValue = heightFraction,
            animationSpec = tween(
                durationMillis = CHART_ANIMATION_DURATION,
                delayMillis = index * BAR_STAGGER_DELAY,
                easing = FastOutSlowInEasing
            )
        ) { value, _ ->
            currentHeight = value
        }
    }

    return Modifier.graphicsLayer(
        scaleY = currentHeight,
        transformOrigin = TransformOrigin(0.5f, 1f)
    )
}

/**
 * Animation pour les segments d'un graphique circulaire
 */
@Composable
fun pieChartAnimation(
    index: Int = 0,
    targetSweepAngle: Float
): Float {
    var currentAngle by remember { mutableStateOf(0f) }
    
    LaunchedEffect(targetSweepAngle) {
        animate(
            initialValue = currentAngle,
            targetValue = targetSweepAngle,
            animationSpec = tween(
                durationMillis = CHART_ANIMATION_DURATION,
                delayMillis = index * PIE_SEGMENT_DELAY,
                easing = FastOutSlowInEasing
            )
        ) { value, _ ->
            currentAngle = value
        }
    }

    return currentAngle
}

/**
 * Animation pour une ligne de graphique linéaire
 */
@Composable
fun lineChartAnimation(
    index: Int = 0,
    points: List<Float>
): List<Float> {
    var currentPoints by remember { mutableStateOf(List(points.size) { 0f }) }
    
    LaunchedEffect(points) {
        points.forEachIndexed { i, targetValue ->
            animate(
                initialValue = currentPoints[i],
                targetValue = targetValue,
                animationSpec = tween(
                    durationMillis = CHART_ANIMATION_DURATION,
                    delayMillis = index * BAR_STAGGER_DELAY,
                    easing = FastOutSlowInEasing
                )
            ) { value, _ ->
                currentPoints = currentPoints.toMutableList().also { it[i] = value }
            }
        }
    }

    return currentPoints
}

/**
 * Animation de progression circulaire
 */
@Composable
fun circularProgressAnimation(
    targetProgress: Float
): Float {
    var currentProgress by remember { mutableStateOf(0f) }
    
    LaunchedEffect(targetProgress) {
        animate(
            initialValue = currentProgress,
            targetValue = targetProgress,
            animationSpec = tween(
                durationMillis = CHART_ANIMATION_DURATION,
                easing = FastOutSlowInEasing
            )
        ) { value, _ ->
            currentProgress = value
        }
    }

    return currentProgress
}

/**
 * Animation d'échelle pour la mise en évidence des points de données
 */
@Composable
fun dataPointAnimation(
    isHighlighted: Boolean
): Modifier {
    val scale by animateFloatAsState(
        targetValue = if (isHighlighted) 1.5f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    return Modifier.graphicsLayer(
        scaleX = scale,
        scaleY = scale
    )
}

/**
 * Animation de fondu pour les légendes et étiquettes
 */
@Composable
fun chartLabelAnimation(
    isVisible: Boolean
): Modifier {
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(
            durationMillis = 300,
            easing = LinearEasing
        )
    )

    return Modifier.graphicsLayer(alpha = alpha)
}

/**
 * Animation de rotation pour les graphiques circulaires
 */
@Composable
fun pieChartRotationAnimation(
    isRotating: Boolean,
    baseAngle: Float = 0f
): Float {
    val angle by animateFloatAsState(
        targetValue = if (isRotating) baseAngle + 360f else baseAngle,
        animationSpec = tween(
            durationMillis = 1000,
            easing = LinearEasing
        )
    )

    return angle
}
