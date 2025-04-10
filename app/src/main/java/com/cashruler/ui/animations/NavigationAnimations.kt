package com.cashruler.ui.animations

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.navigation.NavBackStackEntry

private const val ANIMATION_DURATION = 300

/**
 * Animation d'entrée horizontale pour la navigation
 */
fun AnimatedContentTransitionScope<NavBackStackEntry>.slideInHorizontally(
    towards: AnimatedContentTransitionScope.SlideDirection = AnimatedContentTransitionScope.SlideDirection.Left
): EnterTransition = slideIntoContainer(
    towards = towards,
    animationSpec = tween(ANIMATION_DURATION)
)

/**
 * Animation de sortie horizontale pour la navigation
 */
fun AnimatedContentTransitionScope<NavBackStackEntry>.slideOutHorizontally(
    towards: AnimatedContentTransitionScope.SlideDirection = AnimatedContentTransitionScope.SlideDirection.Left
): ExitTransition = slideOutOfContainer(
    towards = towards,
    animationSpec = tween(ANIMATION_DURATION)
)

/**
 * Animation d'entrée verticale pour la navigation
 */
fun AnimatedContentTransitionScope<NavBackStackEntry>.slideInVertically(
    towards: AnimatedContentTransitionScope.SlideDirection = AnimatedContentTransitionScope.SlideDirection.Up
): EnterTransition = slideIntoContainer(
    towards = towards,
    animationSpec = tween(ANIMATION_DURATION)
)

/**
 * Animation de sortie verticale pour la navigation
 */
fun AnimatedContentTransitionScope<NavBackStackEntry>.slideOutVertically(
    towards: AnimatedContentTransitionScope.SlideDirection = AnimatedContentTransitionScope.SlideDirection.Down
): ExitTransition = slideOutOfContainer(
    towards = towards,
    animationSpec = tween(ANIMATION_DURATION)
)

/**
 * Animation de fondu pour la navigation
 */
@OptIn(ExperimentalAnimationApi::class)
fun AnimatedContentTransitionScope<NavBackStackEntry>.fade(): EnterTransition = fadeIn(
    animationSpec = tween(ANIMATION_DURATION)
)

/**
 * Animation de fondu pour la sortie
 */
@OptIn(ExperimentalAnimationApi::class)
fun AnimatedContentTransitionScope<NavBackStackEntry>.fadeOut(): ExitTransition = fadeOut(
    animationSpec = tween(ANIMATION_DURATION)
)

/**
 * Animation d'échelle pour la navigation
 */
@OptIn(ExperimentalAnimationApi::class)
fun AnimatedContentTransitionScope<NavBackStackEntry>.scale(): EnterTransition = scaleIn(
    initialScale = 0.8f,
    animationSpec = tween(ANIMATION_DURATION)
)

/**
 * Animation d'échelle pour la sortie
 */
@OptIn(ExperimentalAnimationApi::class)
fun AnimatedContentTransitionScope<NavBackStackEntry>.scaleOut(): ExitTransition = scaleOut(
    targetScale = 1.2f,
    animationSpec = tween(ANIMATION_DURATION)
)

/**
 * Animations combinées pour les dialogues
 */
object DialogAnimations {
    fun enter() = fadeIn(animationSpec = tween(220, delayMillis = 90)) +
            scaleIn(initialScale = 0.8f, animationSpec = tween(220, delayMillis = 90))

    fun exit() = fadeOut(animationSpec = tween(90)) +
            scaleOut(targetScale = 0.8f, animationSpec = tween(90))
}

/**
 * Animations pour les transitions de contenu
 */
object ContentTransitions {
    @OptIn(ExperimentalAnimationApi::class)
    fun defaultTransition(
        duration: Int = ANIMATION_DURATION
    ): ContentTransform = fadeIn(animationSpec = tween(duration)) with
            fadeOut(animationSpec = tween(duration))

    @OptIn(ExperimentalAnimationApi::class)
    fun slideTransition(
        duration: Int = ANIMATION_DURATION,
        towards: AnimatedContentTransitionScope.SlideDirection
    ): ContentTransform = slideIntoContainer(
        towards = towards,
        animationSpec = tween(duration)
    ) with slideOutOfContainer(
        towards = towards,
        animationSpec = tween(duration)
    )
}
