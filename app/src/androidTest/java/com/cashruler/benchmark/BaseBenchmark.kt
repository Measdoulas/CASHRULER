package com.cashruler.benchmark

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
abstract class BaseBenchmark {
    @get:Rule
    val benchmarkRule = BenchmarkRule()

    protected fun runBenchmark(
        warmupIterations: Int = 3,
        measurementIterations: Int = 5,
        setupAction: () -> Unit = {},
        testAction: () -> Unit
    ) {
        benchmarkRule.measureRepeated(
            packageName = "com.cashruler",
            metrics = listOf(
                androidx.benchmark.InstrumentationResults.METRIC_WALL_TIME,
                androidx.benchmark.InstrumentationResults.METRIC_MEMORY_USAGE
            ),
            warmupIterations = warmupIterations,
            iterations = measurementIterations,
            setupBlock = { setupAction() }
        ) {
            testAction()
        }
    }

    protected fun warmUp(action: () -> Unit) {
        repeat(3) { action() }
    }
}

/**
 * Configuration des tests de performance
 */
object BenchmarkConfig {
    const val SMALL_DATASET_SIZE = 100
    const val MEDIUM_DATASET_SIZE = 1000
    const val LARGE_DATASET_SIZE = 10000
    
    const val WARMUP_ITERATIONS = 3
    const val MEASUREMENT_ITERATIONS = 5
    
    const val MIN_BATTERY_LEVEL = 20
    const val TARGET_FRAME_TIME_MS = 16L // 60 FPS
    
    const val MEMORY_THRESHOLD_MB = 50 // Seuil d'alerte pour la consommation mémoire
}
