package com.idlerpg.game.core.validation

/** Aggregate structured validation result. */
data class ValidationResult(
    val issues: List<ValidationIssue> = emptyList()
) {
    val isValid: Boolean
        get() = issues.none { it.severity == ValidationSeverity.ERROR }

    val errors: List<ValidationIssue>
        get() = issues.filter { it.severity == ValidationSeverity.ERROR }

    val warnings: List<ValidationIssue>
        get() = issues.filter { it.severity == ValidationSeverity.WARNING }

    operator fun plus(other: ValidationResult): ValidationResult =
        ValidationResult(issues + other.issues)

    companion object {
        val VALID: ValidationResult = ValidationResult()

        fun of(vararg issues: ValidationIssue): ValidationResult =
            ValidationResult(issues.toList())
    }
}
