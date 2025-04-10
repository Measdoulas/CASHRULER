package com.cashruler.benchmark

import androidx.benchmark.BenchmarkState
import androidx.benchmark.Stats
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Générateur de rapports pour les tests de performance
 */
class BenchmarkReportGenerator {

    private val reports = mutableListOf<BenchmarkReport>()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault())

    fun addResult(
        testName: String,
        metric: String,
        stats: Stats,
        threshold: Double? = null
    ) {
        reports.add(
            BenchmarkReport(
                testName = testName,
                metric = metric,
                min = stats.min,
                max = stats.max,
                median = stats.median,
                mean = stats.mean,
                standardDeviation = stats.standardDeviation,
                timestamp = System.currentTimeMillis(),
                threshold = threshold
            )
        )
    }

    fun generateReport(outputDir: File) {
        val reportFile = File(
            outputDir,
            "benchmark_report_${dateFormat.format(Date())}.txt"
        )

        reportFile.writeText(buildReportContent())
    }

    fun generateMarkdownReport(outputDir: File) {
        val reportFile = File(
            outputDir,
            "benchmark_report_${dateFormat.format(Date())}.md"
        )

        reportFile.writeText(buildMarkdownContent())
    }

    private fun buildReportContent(): String = buildString {
        appendLine("Performance Test Report")
        appendLine("=====================")
        appendLine("Generated: ${Date()}")
        appendLine()

        reports.groupBy { it.testName }.forEach { (testName, results) ->
            appendLine("Test: $testName")
            appendLine("-".repeat(50))
            
            results.forEach { report ->
                appendLine("Metric: ${report.metric}")
                appendLine("- Minimum: ${report.min}")
                appendLine("- Maximum: ${report.max}")
                appendLine("- Median: ${report.median}")
                appendLine("- Mean: ${report.mean}")
                appendLine("- Standard Deviation: ${report.standardDeviation}")
                
                report.threshold?.let { threshold ->
                    val status = if (report.mean <= threshold) "PASS" else "FAIL"
                    appendLine("- Status: $status (Threshold: $threshold)")
                }
                
                appendLine()
            }
            appendLine()
        }

        appendLine("Performance Recommendations:")
        appendLine("---------------------------")
        generateRecommendations().forEach { appendLine("- $it") }
    }

    private fun buildMarkdownContent(): String = buildString {
        appendLine("# Performance Test Report")
        appendLine()
        appendLine("Generated: ${Date()}")
        appendLine()

        reports.groupBy { it.testName }.forEach { (testName, results) ->
            appendLine("## $testName")
            appendLine()
            
            appendLine("| Metric | Min | Max | Median | Mean | Std Dev | Status |")
            appendLine("|--------|-----|-----|---------|------|----------|--------|")
            
            results.forEach { report ->
                val status = report.threshold?.let { threshold ->
                    if (report.mean <= threshold) "✅" else "❌"
                } ?: "N/A"
                
                appendLine("| ${report.metric} | ${report.min} | ${report.max} | ${report.median} | " +
                    "${report.mean} | ${report.standardDeviation} | $status |")
            }
            appendLine()
        }

        appendLine("## Recommendations")
        appendLine()
        generateRecommendations().forEach { appendLine("* $it") }
    }

    private fun generateRecommendations(): List<String> {
        val recommendations = mutableListOf<String>()

        reports.forEach { report ->
            when {
                report.metric.contains("frame") && report.mean > BenchmarkConfig.TARGET_FRAME_TIME_MS -> {
                    recommendations.add("Optimize animations in ${report.testName} to maintain 60 FPS")
                }
                report.metric.contains("memory") && report.max > BenchmarkConfig.MEMORY_THRESHOLD_MB -> {
                    recommendations.add("Reduce memory usage in ${report.testName}")
                }
                report.standardDeviation > report.mean * 0.2 -> {
                    recommendations.add("Investigate performance inconsistency in ${report.testName}")
                }
            }
        }

        return recommendations
    }

    data class BenchmarkReport(
        val testName: String,
        val metric: String,
        val min: Double,
        val max: Double,
        val median: Double,
        val mean: Double,
        val standardDeviation: Double,
        val timestamp: Long,
        val threshold: Double? = null
    )

    companion object {
        fun fromBenchmarkState(
            testName: String,
            metric: String,
            state: BenchmarkState,
            threshold: Double? = null
        ): BenchmarkReportGenerator {
            return BenchmarkReportGenerator().apply {
                addResult(testName, metric, state.stats, threshold)
            }
        }
    }
}
