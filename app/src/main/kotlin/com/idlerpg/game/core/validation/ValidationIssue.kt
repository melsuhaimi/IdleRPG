package com.idlerpg.game.core.validation

/** Severity for structured validation diagnostics. */
enum class ValidationSeverity {
    WARNING,
    ERROR
}

/** Structured validation failure or warning. */
data class ValidationIssue(
    val code: String,
    val severity: ValidationSeverity,
    val path: String,
    val message: String
) {
    init {
        require(code.isNotBlank()) { "ValidationIssue.code cannot be blank" }
        require(path.isNotBlank()) { "ValidationIssue.path cannot be blank" }
        require(message.isNotBlank()) { "ValidationIssue.message cannot be blank" }
    }
}
