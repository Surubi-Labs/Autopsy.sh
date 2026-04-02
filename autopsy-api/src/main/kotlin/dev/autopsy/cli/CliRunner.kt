package dev.autopsy.cli

import dev.autopsy.bridge.BridgeException
import dev.autopsy.bridge.PythonBridge
import dev.autopsy.extraction.ExtractionResult
import dev.autopsy.metrics.*
import dev.autopsy.report.ReportFormatter

class CliRunner(
    private val bridge: PythonBridge = PythonBridge(),
) {

    fun run(args: Array<String>): Int {
        if (args.isEmpty() || args[0] != "analyze") {
            printUsage()
            return 1
        }

        val config = parseArgs(args) ?: return 1

        return try {
            val report = analyze(config)
            if (config.outputFile != null) {
                java.io.File(config.outputFile).writeText(report)
                System.err.println("Report written to ${config.outputFile}")
            } else {
                print(report)
            }
            0
        } catch (e: BridgeException) {
            System.err.println("Error: ${e.message}")
            1
        } catch (e: Exception) {
            System.err.println("Unexpected error: ${e.message}")
            1
        }
    }

    internal fun analyze(config: AnalyzeConfig): String {
        // Step 1: Extract data from repo via Python
        val extraction = bridge.extract(config.repoPath)

        // Step 2: Detect modules
        val modules = ModuleDetector.detectModules(extraction.commits)

        // Step 3: Compute metrics
        val churn = ChurnCalculator.calculateChurn(extraction.commits, modules)
        val busFactor = BusFactorCalculator.calculateBusFactor(extraction.commits, modules)
        val hotspots = HotspotDetector.detectHotspots(extraction.commits)
        val staleness = DependencyStalenessScorer.scoreStaleness(extraction.dependencies)
        val testCoverage = TestCoverageTrendTracker.trackTestCoverage(
            extraction.commits, extraction.testFiles, extraction.sourceFiles, modules,
        )

        // Step 4: Compute health score
        val healthScore = HealthScoreComputer.computeHealthScore(
            churn, busFactor, staleness, testCoverage, hotspots,
        )

        // Step 5: Format report
        return ReportFormatter.format(
            repoPath = config.repoPath,
            healthScore = healthScore,
            modules = modules,
            churn = churn,
            busFactor = busFactor,
            hotspots = hotspots,
            staleness = staleness,
            testCoverage = testCoverage,
        )
    }

    private fun parseArgs(args: Array<String>): AnalyzeConfig? {
        if (args.size < 2) {
            System.err.println("Error: missing repo path")
            printUsage()
            return null
        }

        var repoPath = ""
        var noAi = false
        var outputFile: String? = null

        var i = 1
        while (i < args.size) {
            when (args[i]) {
                "--no-ai" -> noAi = true
                "--output" -> {
                    if (i + 1 >= args.size) {
                        System.err.println("Error: --output requires a file path")
                        return null
                    }
                    outputFile = args[++i]
                }
                else -> {
                    if (repoPath.isEmpty()) {
                        repoPath = args[i]
                    } else {
                        System.err.println("Error: unexpected argument: ${args[i]}")
                        return null
                    }
                }
            }
            i++
        }

        if (repoPath.isEmpty()) {
            System.err.println("Error: missing repo path")
            return null
        }

        return AnalyzeConfig(repoPath, noAi, outputFile)
    }

    private fun printUsage() {
        System.err.println("Usage: autopsy analyze <repo-path> [--no-ai] [--output <file>]")
    }
}

data class AnalyzeConfig(
    val repoPath: String,
    val noAi: Boolean = false,
    val outputFile: String? = null,
)
