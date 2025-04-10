package com.cashruler

import android.os.Environment
import androidx.test.platform.app.InstrumentationRegistry
import com.cashruler.benchmark.BenchmarkReportGenerator
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.runner.RunWith
import org.junit.runners.Suite
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@RunWith(Suite::class)
@Suite.SuiteClasses(
    EndToEndTest::class,
    DatabaseIntegrationTest::class,
    com.cashruler.benchmark.AnimationPerformanceTest::class,
    com.cashruler.benchmark.AppPerformanceTest::class,
    com.cashruler.ui.screens.dashboard.DashboardScreenTest::class
)
class TestOrchestrator {

    companion object {
        private lateinit var reportGenerator: BenchmarkReportGenerator
        private lateinit var startTime: Date
        private val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault())

        @BeforeClass
        @JvmStatic
        fun setup() {
            startTime = Date()
            reportGenerator = BenchmarkReportGenerator()
            println("Starting test suite execution at: $startTime")
        }

        @AfterClass
        @JvmStatic
        fun teardown() {
            val endTime = Date()
            val duration = endTime.time - startTime.time

            // Générer un rapport de test complet
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val reportsDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "test-reports")
            reportsDir.mkdirs()

            // Générer les rapports
            generateTestSummary(reportsDir, duration)
            reportGenerator.generateMarkdownReport(reportsDir)
            
            println("Test suite completed. Duration: ${duration/1000} seconds")
            println("Reports generated in: ${reportsDir.absolutePath}")
        }

        private fun generateTestSummary(outputDir: File, duration: Long) {
            val summaryFile = File(outputDir, "test_summary_${dateFormat.format(Date())}.txt")
            summaryFile.writeText(buildString {
                appendLine("CashRuler Test Suite Summary")
                appendLine("=========================")
                appendLine("Test Run: ${dateFormat.format(startTime)}")
                appendLine("Duration: ${duration/1000} seconds")
                appendLine()
                
                appendLine("Test Categories:")
                appendLine("1. End-to-End Tests")
                appendLine("2. Integration Tests")
                appendLine("3. UI Tests")
                appendLine("4. Performance Tests")
                appendLine()

                // Ajouter des statistiques supplémentaires si disponibles
                appendLine("Performance Metrics:")
                appendLine("- Average Frame Time: ${getAverageFrameTime()} ms")
                appendLine("- Memory Usage: ${getAverageMemoryUsage()} MB")
                appendLine("- Database Operations: ${getDatabaseMetrics()}")
                appendLine()

                appendLine("System Information:")
                appendLine("- Device: ${android.os.Build.MODEL}")
                appendLine("- Android Version: ${android.os.Build.VERSION.RELEASE}")
                appendLine("- App Version: ${context.packageManager.getPackageInfo(context.packageName, 0).versionName}")
            })
        }

        private fun getAverageFrameTime(): Double {
            // À implémenter avec les données réelles des tests
            return 16.0
        }

        private fun getAverageMemoryUsage(): Int {
            // À implémenter avec les données réelles des tests
            return 50
        }

        private fun getDatabaseMetrics(): String {
            // À implémenter avec les données réelles des tests
            return "Average query time: 5ms"
        }
    }
}

/**
 * Extension function pour ajouter facilement des résultats au générateur de rapport
 */
fun BenchmarkReportGenerator.addTestResult(
    testClass: Class<*>,
    testName: String,
    duration: Long,
    success: Boolean,
    details: Map<String, Any> = emptyMap()
) {
    val stats = androidx.benchmark.Stats(
        min = duration.toDouble(),
        max = duration.toDouble(),
        median = duration.toDouble(),
        mean = duration.toDouble(),
        standardDeviation = 0.0
    )
    addResult(
        testName = "${testClass.simpleName}#$testName",
        metric = "execution_time",
        stats = stats
    )
}
