package com.cashruler.benchmark

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.filters.LargeTest
import com.cashruler.ui.animations.ChartAnimations
import com.cashruler.ui.animations.NavigationAnimations
import com.cashruler.ui.animations.StateAnimations
import kotlinx.coroutines.delay
import org.junit.Rule
import org.junit.Test
import kotlin.system.measureTimeMillis

@LargeTest
class AnimationPerformanceTest : BaseBenchmark() {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun chartAnimationPerformance() {
        runBenchmark(
            warmupIterations = BenchmarkConfig.WARMUP_ITERATIONS,
            measurementIterations = BenchmarkConfig.MEASUREMENT_ITERATIONS
        ) {
            composeTestRule.setContent {
                AnimatedChart()
            }

            composeTestRule.waitForIdle()
            val frameTime = measureTimeMillis {
                composeTestRule
                    .onNodeWithTag("animated_chart")
                    .performTouchInput {
                        down(0f, 0f)
                        moveBy(100f, 0f)
                        up()
                    }
                composeTestRule.waitForIdle()
            }

            // Vérifier que l'animation est fluide (60 FPS)
            assert(frameTime <= BenchmarkConfig.TARGET_FRAME_TIME_MS) {
                "Animation frame time exceeded target: $frameTime ms"
            }
        }
    }

    @Test
    fun navigationTransitionPerformance() {
        runBenchmark {
            composeTestRule.setContent {
                NavigationTransitionTest()
            }

            composeTestRule.waitForIdle()
            val transitionTime = measureTimeMillis {
                composeTestRule
                    .onNodeWithTag("nav_container")
                    .performClick()
                composeTestRule.waitForIdle()
            }

            assert(transitionTime <= 300) { // La transition ne devrait pas dépasser 300ms
                "Navigation transition took too long: $transitionTime ms"
            }
        }
    }

    @Test
    fun multipleStateAnimationsPerformance() {
        runBenchmark {
            composeTestRule.setContent {
                StateAnimationsTest()
            }

            repeat(10) { // Tester plusieurs transitions d'état
                composeTestRule
                    .onNodeWithTag("state_container")
                    .performClick()
                composeTestRule.waitForIdle()
            }
        }
    }

    @Composable
    private fun AnimatedChart() {
        val progress = remember { Animatable(0f) }

        LaunchedEffect(Unit) {
            progress.animateTo(1f, tween(1000))
        }

        Box(modifier = androidx.compose.ui.Modifier.testTag("animated_chart")) {
            ChartAnimations.AnimatedBarChart(
                data = List(100) { it.toFloat() },
                maxValue = 100f,
                currentValue = progress.value
            )
        }
    }

    @Composable
    private fun NavigationTransitionTest() {
        Box(modifier = androidx.compose.ui.Modifier.testTag("nav_container")) {
            NavigationAnimations.SlideTransition(
                isVisible = true,
                content = { Box {} }
            )
        }
    }

    @Composable
    private fun StateAnimationsTest() {
        val states = remember { List(100) { it % 3 } }
        Box(modifier = androidx.compose.ui.Modifier.testTag("state_container")) {
            states.forEach { state ->
                StateAnimations.CrossFadeState(
                    state = state,
                    content = { Box {} }
                )
            }
        }
    }

    private suspend fun simulateUserInteraction() {
        delay(100) // Simule le délai utilisateur
    }
}
