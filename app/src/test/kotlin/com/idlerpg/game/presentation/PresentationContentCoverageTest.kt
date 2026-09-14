package com.idlerpg.game.presentation

import com.idlerpg.game.data.content.DefaultGameContent
import com.idlerpg.game.presentation.content.PresentationContentRegistry

/** Dependency-free FUI-02 metadata coverage regression. */
object PresentationContentCoverageTest {
    @JvmStatic
    fun main(args: Array<String>) {
        val content = DefaultGameContent.registry()
        val presentation = PresentationContentRegistry.default()
        val coverage = presentation.coverage(content)

        check(coverage.missingVisibleIds.isEmpty()) {
            "Missing visible presentation metadata: ${coverage.missingVisibleIds}"
        }
        check(coverage.unclassifiedRegistryIds.isEmpty()) {
            "Unclassified authored content: ${coverage.unclassifiedRegistryIds}"
        }
        check(presentation.allEntries().all { it.titleStringKey != it.shortDescriptionStringKey }) {
            "Every presentation entry must distinguish title and description keys"
        }

        println(
            "FUI02_PRESENTATION_COVERAGE_PASS " +
                "visible=${coverage.requiredVisibleIds.size} " +
                "systemOnly=${coverage.systemOnlyIds.size} " +
                "entries=${presentation.allEntries().size}"
        )
    }
}
