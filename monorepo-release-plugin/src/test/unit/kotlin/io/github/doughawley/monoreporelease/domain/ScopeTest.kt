package io.github.doughawley.monoreporelease.domain

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

class ScopeTest : FunSpec({

    test("fromString returns MAJOR for 'major' case-insensitively") {
        Scope.fromString("major") shouldBe Scope.MAJOR
        Scope.fromString("MAJOR") shouldBe Scope.MAJOR
        Scope.fromString("Major") shouldBe Scope.MAJOR
    }

    test("fromString returns MINOR for 'minor' case-insensitively") {
        Scope.fromString("minor") shouldBe Scope.MINOR
        Scope.fromString("MINOR") shouldBe Scope.MINOR
    }

    test("fromString returns PATCH for 'patch' case-insensitively") {
        Scope.fromString("patch") shouldBe Scope.PATCH
        Scope.fromString("PATCH") shouldBe Scope.PATCH
    }

    test("fromString returns null for unrecognised input") {
        Scope.fromString("release").shouldBeNull()
        Scope.fromString("hotfix").shouldBeNull()
        Scope.fromString("").shouldBeNull()
    }
})
