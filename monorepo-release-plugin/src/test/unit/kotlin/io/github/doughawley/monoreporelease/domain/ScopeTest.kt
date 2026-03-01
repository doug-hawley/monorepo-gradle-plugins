package io.github.doughawley.monoreporelease.domain

import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

class ScopeTest : FunSpec({

    context("fromString maps recognised strings to the correct scope") {
        withData(
            "major" to Scope.MAJOR,
            "MAJOR" to Scope.MAJOR,
            "Major" to Scope.MAJOR,
            "minor" to Scope.MINOR,
            "MINOR" to Scope.MINOR,
            "patch" to Scope.PATCH,
            "PATCH" to Scope.PATCH,
        ) { (input, expected) ->
            Scope.fromString(input) shouldBe expected
        }
    }

    context("fromString returns null for unrecognised input") {
        withData(
            nameFn = { it.ifEmpty { "(empty string)" } },
            "release", "hotfix", "",
        ) { input ->
            Scope.fromString(input).shouldBeNull()
        }
    }
})
