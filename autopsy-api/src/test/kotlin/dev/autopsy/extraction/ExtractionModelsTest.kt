package dev.autopsy.extraction

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ExtractionModelsTest {

    private val mapper = jacksonObjectMapper()

    @Test
    fun `deserialize FileChange from JSON`() {
        val json = """{"path": "src/main.py", "additions": 10, "deletions": 5}"""
        val fc: FileChange = mapper.readValue(json)
        assertEquals("src/main.py", fc.path)
        assertEquals(10, fc.additions)
        assertEquals(5, fc.deletions)
    }

    @Test
    fun `deserialize CommitExtraction from JSON`() {
        val json = """
        {
            "sha": "abc123",
            "author_email": "dev@company.com",
            "timestamp": "2026-03-15T14:30:00Z",
            "message": "refactor payment handler",
            "files_changed": [
                {"path": "src/payments/handler.kt", "additions": 45, "deletions": 12}
            ]
        }
        """.trimIndent()

        val commit: CommitExtraction = mapper.readValue(json)
        assertEquals("abc123", commit.sha)
        assertEquals("dev@company.com", commit.authorEmail)
        assertEquals("2026-03-15T14:30:00Z", commit.timestamp)
        assertEquals("refactor payment handler", commit.message)
        assertEquals(1, commit.filesChanged.size)
        assertEquals("src/payments/handler.kt", commit.filesChanged[0].path)
    }

    @Test
    fun `deserialize CommitExtraction with empty files_changed`() {
        val json = """
        {
            "sha": "abc123",
            "author_email": "dev@company.com",
            "timestamp": "2026-03-15T14:30:00Z",
            "message": "empty merge",
            "files_changed": []
        }
        """.trimIndent()

        val commit: CommitExtraction = mapper.readValue(json)
        assertTrue(commit.filesChanged.isEmpty())
    }

    @Test
    fun `deserialize DependencyInfo with all fields`() {
        val json = """
        {
            "name": "express",
            "current": "4.18.2",
            "latest": "4.19.1",
            "outdated_days": 142,
            "cves": ["CVE-2021-23337"]
        }
        """.trimIndent()

        val dep: DependencyInfo = mapper.readValue(json)
        assertEquals("express", dep.name)
        assertEquals("4.18.2", dep.current)
        assertEquals("4.19.1", dep.latest)
        assertEquals(142, dep.outdatedDays)
        assertEquals(listOf("CVE-2021-23337"), dep.cves)
    }

    @Test
    fun `deserialize DependencyInfo with optional fields missing`() {
        val json = """{"name": "lodash", "current": "4.17.21"}"""
        val dep: DependencyInfo = mapper.readValue(json)
        assertEquals("lodash", dep.name)
        assertNull(dep.latest)
        assertNull(dep.outdatedDays)
        assertTrue(dep.cves.isEmpty())
    }

    @Test
    fun `deserialize DependencyExtraction from JSON`() {
        val json = """
        {
            "file": "package.json",
            "manager": "npm",
            "dependencies": [
                {"name": "express", "current": "4.18.2", "latest": "4.19.1", "outdated_days": 142, "cves": []}
            ]
        }
        """.trimIndent()

        val ext: DependencyExtraction = mapper.readValue(json)
        assertEquals("package.json", ext.file)
        assertEquals("npm", ext.manager)
        assertEquals(1, ext.dependencies.size)
    }

    @Test
    fun `deserialize DocExtraction from JSON`() {
        val json = """
        {
            "type": "adr",
            "path": "docs/adr/001.md",
            "title": "Use PostgreSQL",
            "content": "## Context\nWe need a database...",
            "last_modified": "2025-08-12T10:00:00Z"
        }
        """.trimIndent()

        val doc: DocExtraction = mapper.readValue(json)
        assertEquals("adr", doc.type)
        assertEquals("docs/adr/001.md", doc.path)
        assertEquals("Use PostgreSQL", doc.title)
    }

    @Test
    fun `deserialize full ExtractionResult from JSON`() {
        val json = """
        {
            "commits": [
                {
                    "sha": "abc123",
                    "author_email": "dev@co.com",
                    "timestamp": "2026-03-15T14:30:00Z",
                    "message": "feat: add handler",
                    "files_changed": [
                        {"path": "src/handler.kt", "additions": 45, "deletions": 12}
                    ]
                }
            ],
            "dependencies": [
                {
                    "file": "package.json",
                    "manager": "npm",
                    "dependencies": [
                        {"name": "express", "current": "4.18.2"}
                    ]
                }
            ],
            "docs": [
                {
                    "type": "readme",
                    "path": "README.md",
                    "title": "Project",
                    "content": "# Project",
                    "last_modified": "2026-01-01T00:00:00Z"
                }
            ],
            "test_files": ["tests/test_handler.py"],
            "source_files": ["src/handler.kt"]
        }
        """.trimIndent()

        val result: ExtractionResult = mapper.readValue(json)
        assertEquals(1, result.commits.size)
        assertEquals("abc123", result.commits[0].sha)
        assertEquals(1, result.dependencies.size)
        assertEquals(1, result.docs.size)
        assertEquals(listOf("tests/test_handler.py"), result.testFiles)
        assertEquals(listOf("src/handler.kt"), result.sourceFiles)
    }

    @Test
    fun `deserialize empty ExtractionResult`() {
        val json = """
        {
            "commits": [],
            "dependencies": [],
            "docs": [],
            "test_files": [],
            "source_files": []
        }
        """.trimIndent()

        val result: ExtractionResult = mapper.readValue(json)
        assertTrue(result.commits.isEmpty())
        assertTrue(result.dependencies.isEmpty())
        assertTrue(result.docs.isEmpty())
    }
}
