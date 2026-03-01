package io.github.doughawley.monoreporelease.domain

import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

class SemanticVersionTest : FunSpec({

    context("parse returns SemanticVersion for valid input") {
        withData(
            "v1.2.3" to SemanticVersion(1, 2, 3),
            "1.2.3" to SemanticVersion(1, 2, 3),
            "0.0.0" to SemanticVersion(0, 0, 0),
        ) { (input, expected) ->
            SemanticVersion.parse(input) shouldBe expected
        }
    }

    context("parse returns null for invalid input") {
        withData(
            nameFn = { it.ifEmpty { "(empty string)" } },
            "not-a-version", "1.2", "1.2.3.4", "", "a.b.c",
        ) { input ->
            SemanticVersion.parse(input).shouldBeNull()
        }
    }

    context("bump MAJOR resets minor and patch to zero") {
        withData(
            SemanticVersion(1, 2, 3) to SemanticVersion(2, 0, 0),
            SemanticVersion(0, 0, 0) to SemanticVersion(1, 0, 0),
        ) { (version, expected) ->
            version.bump(Scope.MAJOR) shouldBe expected
        }
    }

    context("bump MINOR resets patch to zero") {
        withData(
            SemanticVersion(1, 2, 3) to SemanticVersion(1, 3, 0),
            SemanticVersion(0, 0, 0) to SemanticVersion(0, 1, 0),
        ) { (version, expected) ->
            version.bump(Scope.MINOR) shouldBe expected
        }
    }

    context("bump PATCH increments patch only") {
        withData(
            SemanticVersion(1, 2, 3) to SemanticVersion(1, 2, 4),
            SemanticVersion(0, 0, 0) to SemanticVersion(0, 0, 1),
        ) { (version, expected) ->
            version.bump(Scope.PATCH) shouldBe expected
        }
    }

    context("higher version is greater than lower") {
        withData(
            SemanticVersion(1, 9, 9) to SemanticVersion(2, 0, 0),  // major wins
            SemanticVersion(1, 2, 9) to SemanticVersion(1, 3, 0),  // minor wins
            SemanticVersion(1, 2, 3) to SemanticVersion(1, 2, 4),  // patch wins
        ) { (lower, higher) ->
            higher shouldBeGreaterThan lower
            lower shouldBeLessThan higher
        }
    }

    test("equal versions compare as equal") {
        SemanticVersion(1, 2, 3).compareTo(SemanticVersion(1, 2, 3)) shouldBe 0
    }

    test("max selection picks correct version from list") {
        val versions = listOf(
            SemanticVersion(0, 1, 0),
            SemanticVersion(0, 2, 0),
            SemanticVersion(0, 1, 5),
            SemanticVersion(1, 0, 0)
        )
        versions.max() shouldBe SemanticVersion(1, 0, 0)
    }

    context("toString produces major.minor.patch without v prefix") {
        withData(
            SemanticVersion(1, 2, 3) to "1.2.3",
            SemanticVersion(0, 0, 0) to "0.0.0",
        ) { (version, expected) ->
            version.toString() shouldBe expected
        }
    }
})
