package dev.autopsy.cli

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class CliRunnerTest {

    @Test
    fun `no arguments returns exit code 1`() {
        val runner = CliRunner()
        val code = runner.run(arrayOf())
        assertEquals(1, code)
    }

    @Test
    fun `unknown command returns exit code 1`() {
        val runner = CliRunner()
        val code = runner.run(arrayOf("unknown"))
        assertEquals(1, code)
    }

    @Test
    fun `analyze without repo path returns exit code 1`() {
        val runner = CliRunner()
        val code = runner.run(arrayOf("analyze"))
        assertEquals(1, code)
    }

    @Test
    fun `analyze with nonexistent repo returns exit code 1`() {
        val runner = CliRunner()
        val code = runner.run(arrayOf("analyze", "/nonexistent/path"))
        assertEquals(1, code)
    }
}
