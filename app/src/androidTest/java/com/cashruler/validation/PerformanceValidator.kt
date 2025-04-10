package com.cashruler.validation

import android.app.ActivityManager
import android.content.Context
import android.os.Debug
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.system.measureTimeMillis

/**
 * Validateur de performance pour détecter les problèmes potentiels
 */
class PerformanceValidator {

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

    /**
     * Vérifie la performance d'une opération de base de données
     */
    suspend fun validateDatabaseOperation(
        operation: suspend () -> Unit,
        maxExecutionTime: Long = ValidationConfig.MAX_DB_OPERATION_TIME_MS
    ): OperationResult {
        val executionTime = measureTimeMillis {
            operation()
        }

        return OperationResult(
            isValid = executionTime <= maxExecutionTime,
            executionTime = executionTime,
            memoryUsage = getCurrentMemoryUsage(),
            errors = if (executionTime > maxExecutionTime) {
                listOf("L'opération a pris trop de temps: ${executionTime}ms > ${maxExecutionTime}ms")
            } else emptyList()
        )
    }

    /**
     * Vérifie la performance des animations
     */
    fun validateAnimation(
        frameTimesMs: List<Long>,
        targetFps: Int = 60
    ): OperationResult {
        val targetFrameTime = 1000L / targetFps
        val droppedFrames = frameTimesMs.count { it > targetFrameTime }
        val dropRate = droppedFrames.toFloat() / frameTimesMs.size

        return OperationResult(
            isValid = dropRate <= ValidationConfig.TestThresholds.ANIMATION_FRAME_DROP_THRESHOLD,
            executionTime = frameTimesMs.sum(),
            memoryUsage = getCurrentMemoryUsage(),
            errors = if (dropRate > ValidationConfig.TestThresholds.ANIMATION_FRAME_DROP_THRESHOLD) {
                listOf("Trop d'images perdues: ${(dropRate * 100).toInt()}% > ${ValidationConfig.TestThresholds.ANIMATION_FRAME_DROP_THRESHOLD * 100}%")
            } else emptyList()
        )
    }

    /**
     * Vérifie la performance des opérations de flux de données
     */
    suspend fun <T> validateFlow(
        flow: Flow<T>,
        maxEmissionTime: Long = ValidationConfig.MAX_DB_OPERATION_TIME_MS
    ): OperationResult {
        var emissionCount = 0
        var totalTime = 0L

        val monitoredFlow = flow.map { value ->
            val emissionTime = measureTimeMillis {
                emissionCount++
                value
            }
            totalTime += emissionTime
            value
        }

        return OperationResult(
            isValid = totalTime / emissionCount <= maxEmissionTime,
            executionTime = totalTime,
            memoryUsage = getCurrentMemoryUsage(),
            errors = if (totalTime / emissionCount > maxEmissionTime) {
                listOf("Temps d'émission moyen trop élevé: ${totalTime / emissionCount}ms")
            } else emptyList()
        )
    }

    /**
     * Vérifie l'utilisation de la mémoire
     */
    fun validateMemoryUsage(): OperationResult {
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        
        val currentUsage = getCurrentMemoryUsage()
        val threshold = ValidationConfig.MAX_MEMORY_USAGE_MB

        return OperationResult(
            isValid = currentUsage <= threshold,
            executionTime = 0,
            memoryUsage = currentUsage,
            errors = if (currentUsage > threshold) {
                listOf("Utilisation mémoire excessive: ${currentUsage}MB > ${threshold}MB")
            } else emptyList()
        )
    }

    /**
     * Vérifie les fuites de mémoire potentielles
     */
    fun checkMemoryLeaks(
        operation: () -> Unit,
        iterations: Int = 100
    ): OperationResult {
        val initialMemory = getCurrentMemoryUsage()
        
        repeat(iterations) {
            operation()
            System.gc() // Force garbage collection
        }

        val finalMemory = getCurrentMemoryUsage()
        val memoryDelta = finalMemory - initialMemory
        val threshold = ValidationConfig.TestThresholds.MEMORY_LEAK_THRESHOLD_MB

        return OperationResult(
            isValid = memoryDelta <= threshold,
            executionTime = 0,
            memoryUsage = memoryDelta,
            errors = if (memoryDelta > threshold) {
                listOf("Fuite de mémoire potentielle détectée: ${memoryDelta}MB")
            } else emptyList()
        )
    }

    private fun getCurrentMemoryUsage(): Int {
        val info = Debug.MemoryInfo()
        Debug.getMemoryInfo(info)
        return info.totalPss / 1024 // Convertir en MB
    }

    data class OperationResult(
        val isValid: Boolean,
        val executionTime: Long,
        val memoryUsage: Int,
        val errors: List<String> = emptyList()
    )
}
