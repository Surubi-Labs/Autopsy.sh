package dev.autopsy.extraction

import com.fasterxml.jackson.annotation.JsonProperty

data class FileChange(
    val path: String,
    val additions: Int,
    val deletions: Int,
)

data class CommitExtraction(
    val sha: String,
    @JsonProperty("author_email") val authorEmail: String,
    val timestamp: String,
    val message: String,
    @JsonProperty("files_changed") val filesChanged: List<FileChange>,
)

data class DependencyInfo(
    val name: String,
    val current: String,
    val latest: String? = null,
    @JsonProperty("outdated_days") val outdatedDays: Int? = null,
    val cves: List<String> = emptyList(),
)

data class DependencyExtraction(
    val file: String,
    val manager: String,
    val dependencies: List<DependencyInfo>,
)

data class DocExtraction(
    val type: String,
    val path: String,
    val title: String,
    val content: String,
    @JsonProperty("last_modified") val lastModified: String,
)

data class ExtractionResult(
    val commits: List<CommitExtraction>,
    val dependencies: List<DependencyExtraction>,
    val docs: List<DocExtraction>,
    @JsonProperty("test_files") val testFiles: List<String>,
    @JsonProperty("source_files") val sourceFiles: List<String>,
)
