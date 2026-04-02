package dev.autopsy.bridge

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import dev.autopsy.extraction.ExtractionResult
import java.util.concurrent.TimeUnit

class PythonBridge(
    private val pythonCommand: String = "python3",
    private val engineDir: String = "autopsy-engine",
    private val timeoutSeconds: Long = 120,
) {

    private val mapper = jacksonObjectMapper()

    /**
     * Run the Python extraction CLI and return parsed results.
     */
    fun extract(repoPath: String): ExtractionResult {
        val result = runProcess(
            listOf(pythonCommand, "-m", "autopsy.cli", "extract", repoPath),
            workDir = engineDir,
        )
        return try {
            mapper.readValue(result)
        } catch (e: Exception) {
            throw BridgeException("Failed to parse extraction JSON: ${e.message}", e)
        }
    }

    /**
     * Run the Python report CLI and return markdown.
     */
    fun generateReport(metricsJson: String): String {
        return runProcess(
            listOf(pythonCommand, "-m", "autopsy.cli", "report"),
            workDir = engineDir,
            stdin = metricsJson,
        )
    }

    private fun runProcess(
        command: List<String>,
        workDir: String,
        stdin: String? = null,
    ): String {
        val pb = ProcessBuilder(command)
            .directory(java.io.File(workDir))
            .redirectErrorStream(false)

        val process = try {
            pb.start()
        } catch (e: Exception) {
            throw BridgeException("Failed to start process: ${command.joinToString(" ")}", e)
        }

        if (stdin != null) {
            process.outputStream.bufferedWriter().use { it.write(stdin) }
        }

        val completed = process.waitFor(timeoutSeconds, TimeUnit.SECONDS)
        if (!completed) {
            process.destroyForcibly()
            throw BridgeException("Process timed out after ${timeoutSeconds}s: ${command.joinToString(" ")}")
        }

        val stdout = process.inputStream.bufferedReader().readText()
        val stderr = process.errorStream.bufferedReader().readText()

        if (process.exitValue() != 0) {
            throw BridgeException("Process exited with code ${process.exitValue()}: $stderr")
        }

        return stdout
    }
}
