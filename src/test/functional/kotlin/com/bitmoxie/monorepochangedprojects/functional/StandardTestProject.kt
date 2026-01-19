package com.bitmoxie.monorepochangedprojects.functional

import java.io.File

/**
 * Creates a standard test project structure that can be reused across functional tests.
 *
 * Project structure:
 * - common-lib (no dependencies)
 * - modules/module1 (depends on common-lib)
 * - modules/module2 (depends on common-lib)
 * - apps/app1 (depends on module1, module2)
 * - apps/app2 (depends on module2)
 */
object StandardTestProject {

    fun create(projectDir: File, withRemote: Boolean = true): TestProject {
        return TestProjectBuilder(projectDir)
            .withSubproject("common-lib")
            .withSubproject("modules/module1", dependsOn = listOf("common-lib"))
            .withSubproject("modules/module2", dependsOn = listOf("common-lib"))
            .withSubproject("apps/app1", dependsOn = listOf("modules/module1", "modules/module2"))
            .withSubproject("apps/app2", dependsOn = listOf("modules/module2"))
            .applyPlugin()
            .apply { if (withRemote) withRemote() }
            .build()
    }

    fun createAndInitialize(projectDir: File, withRemote: Boolean = true): TestProject {
        val project = create(projectDir, withRemote)
        project.initGit()
        project.commitAll("Initial commit")
        if (withRemote) {
            project.pushToRemote()
        }
        return project
    }

    /**
     * Project paths for easy reference in tests.
     */
    object Projects {
        const val COMMON_LIB = ":common-lib"
        const val MODULE1 = ":modules:module1"
        const val MODULE2 = ":modules:module2"
        const val APP1 = ":apps:app1"
        const val APP2 = ":apps:app2"
    }

    /**
     * File paths for easy reference in tests.
     */
    object Files {
        const val COMMON_LIB_SOURCE = "common-lib/src/main/kotlin/com/example/Common-lib.kt"
        const val MODULE1_SOURCE = "modules/module1/src/main/kotlin/com/example/Module1.kt"
        const val MODULE2_SOURCE = "modules/module2/src/main/kotlin/com/example/Module2.kt"
        const val APP1_SOURCE = "apps/app1/src/main/kotlin/com/example/App1.kt"
        const val APP2_SOURCE = "apps/app2/src/main/kotlin/com/example/App2.kt"

        const val COMMON_LIB_BUILD = "common-lib/build.gradle.kts"
        const val MODULE1_BUILD = "modules/module1/build.gradle.kts"
        const val MODULE2_BUILD = "modules/module2/build.gradle.kts"
        const val APP1_BUILD = "apps/app1/build.gradle.kts"
        const val APP2_BUILD = "apps/app2/build.gradle.kts"
    }
}
