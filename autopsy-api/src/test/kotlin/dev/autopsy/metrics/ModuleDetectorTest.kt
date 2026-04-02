package dev.autopsy.metrics

import dev.autopsy.extraction.CommitExtraction
import dev.autopsy.extraction.FileChange
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ModuleDetectorTest {

    @Test
    fun `detects modules from nested src structure`() {
        val commits = listOf(
            commit(files = listOf("src/payments/handler.kt", "src/auth/login.kt", "lib/utils.kt"))
        )
        val modules = ModuleDetector.detectModules(commits)
        assertEquals(listOf("lib", "src/auth", "src/payments"), modules)
    }

    @Test
    fun `flat repo files go to root module`() {
        val commits = listOf(
            commit(files = listOf("main.kt", "utils.kt"))
        )
        val modules = ModuleDetector.detectModules(commits)
        assertEquals(listOf("."), modules)
    }

    @Test
    fun `empty commits returns empty modules`() {
        val modules = ModuleDetector.detectModules(emptyList())
        assertTrue(modules.isEmpty())
    }

    @Test
    fun `deduplicates modules from multiple commits`() {
        val commits = listOf(
            commit(files = listOf("src/payments/a.kt")),
            commit(files = listOf("src/payments/b.kt", "src/auth/c.kt")),
        )
        val modules = ModuleDetector.detectModules(commits)
        assertEquals(listOf("src/auth", "src/payments"), modules)
    }

    @Test
    fun `src as only top level uses second level`() {
        val commits = listOf(
            commit(files = listOf("src/module1/a.kt", "src/module2/b.kt"))
        )
        val modules = ModuleDetector.detectModules(commits)
        assertEquals(listOf("src/module1", "src/module2"), modules)
    }

    private fun commit(files: List<String>): CommitExtraction = CommitExtraction(
        sha = "abc123",
        authorEmail = "dev@test.com",
        timestamp = "2026-03-15T14:30:00Z",
        message = "test commit",
        filesChanged = files.map { FileChange(it, 10, 5) },
    )
}
