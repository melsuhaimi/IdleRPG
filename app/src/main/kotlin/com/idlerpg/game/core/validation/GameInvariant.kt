package com.idlerpg.game.core.validation

/**
 * Shared helpers for runtime invariant checks.
 *
 * Foundation 1 provides generic invariant machinery only. Feature-specific invariant
 * checks are added beside the domain state/system that owns them in later foundations.
 */
object GameInvariant {

    fun issueIfFalse(
        condition: Boolean,
        code: String,
        path: String,
        message: String,
        severity: ValidationSeverity = ValidationSeverity.ERROR
    ): ValidationIssue? =
        if (condition) {
            null
        } else {
            ValidationIssue(
                code = code,
                severity = severity,
                path = path,
                message = message
            )
        }

    fun require(
        condition: Boolean,
        code: String,
        path: String,
        message: String
    ) {
        if (!condition) {
            throw IllegalStateException("[$code] $path: $message")
        }
    }

    fun resultOf(vararg issues: ValidationIssue?): ValidationResult =
        ValidationResult(issues.filterNotNull())
}
