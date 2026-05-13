package ly.com.tahaben.flavorizr

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KmpFlavorPluginTest {

    @TempDir
    lateinit var testProjectDir: File

    private lateinit var buildFile: File
    private lateinit var settingsFile: File

    @BeforeEach
    fun setup() {
        settingsFile = File(testProjectDir, "settings.gradle.kts").apply {
            writeText(
                """
                rootProject.name = "test-project"
            """.trimIndent()
            )
        }
    }

    @Test
    fun `plugin applies successfully`() {
        buildFile = File(testProjectDir, "build.gradle.kts").apply {
            writeText(
                """
                plugins {
                    id("ly.com.tahaben.kmp-flavorizr")
                }

                kmpFlavorizr {
                    flavor("development") {
                        app {
                            name = "Test App Dev"
                        }
                        android {
                            applicationId = "com.example.test.dev"
                        }
                    }
                    flavor("production") {
                        app {
                            name = "Test App"
                        }
                        android {
                            applicationId = "com.example.test"
                        }
                    }
                }
            """.trimIndent()
            )
        }

        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withPluginClasspath()
            .withArguments("tasks", "--group=kmp-flavorizr")
            .build()

        assertTrue(result.output.contains("flavorizr"))
        assertTrue(result.output.contains("generateFlavorEnum"))
        assertTrue(result.output.contains("extractFlavorizrScripts"))
    }

    @Test
    fun `generateFlavorEnum produces Flavor kt file`() {
        buildFile = File(testProjectDir, "build.gradle.kts").apply {
            writeText(
                """
                plugins {
                    id("ly.com.tahaben.kmp-flavorizr")
                }

                group = "com.example.test"

                kmpFlavorizr {
                    flavor("development") {
                        app {
                            name = "My App Dev"
                        }
                        android {
                            applicationId = "com.example.app.dev"
                        }
                    }
                    flavor("production") {
                        app {
                            name = "My App"
                        }
                        android {
                            applicationId = "com.example.app"
                        }
                    }
                }
            """.trimIndent()
            )
        }

        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withPluginClasspath()
            .withArguments("generateFlavorEnum")
            .build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":generateFlavorEnum")?.outcome)

        // Verify the generated file
        val generatedDir = File(testProjectDir, "build/generated/kmpFlavorizr/commonMain/kotlin")
        assertTrue(generatedDir.exists(), "Generated directory should exist")

        val flavorFile = generatedDir.walkTopDown().find { it.name == "Flavor.kt" }
        assertTrue(flavorFile != null, "Flavor.kt should be generated")

        val content = flavorFile.readText()
        assertTrue(content.contains("enum class Flavor"), "Should contain enum class")
        assertTrue(content.contains("DEVELOPMENT"), "Should contain DEVELOPMENT")
        assertTrue(content.contains("PRODUCTION"), "Should contain PRODUCTION")
        assertTrue(content.contains("My App Dev"), "Should contain app name 'My App Dev'")
        assertTrue(content.contains("My App"), "Should contain app name 'My App'")
        assertTrue(content.contains("package com.example.test"), "Should have correct package")
    }

    @Test
    fun `extractFlavorizrScripts extracts Ruby scripts`() {
        buildFile = File(testProjectDir, "build.gradle.kts").apply {
            writeText(
                """
                plugins {
                    id("ly.com.tahaben.kmp-flavorizr")
                }

                kmpFlavorizr {
                    flavor("dev") {
                        app { name = "Dev" }
                    }
                }
            """.trimIndent()
            )
        }

        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withPluginClasspath()
            .withArguments("extractFlavorizrScripts")
            .build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":extractFlavorizrScripts")?.outcome)

        val scriptsDir = File(testProjectDir, "build/kmpFlavorizr/scripts/darwin")
        assertTrue(scriptsDir.exists(), "Scripts directory should exist")
        assertTrue(File(scriptsDir, "add_file.rb").exists(), "add_file.rb should exist")
        assertTrue(File(scriptsDir, "create_scheme.rb").exists(), "create_scheme.rb should exist")
        assertTrue(File(scriptsDir, "add_build_configuration.rb").exists())
        assertTrue(File(scriptsDir, "add_firebase_build_phase.rb").exists())
    }
}
