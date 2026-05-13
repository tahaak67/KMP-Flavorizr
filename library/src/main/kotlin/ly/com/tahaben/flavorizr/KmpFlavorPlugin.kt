package ly.com.tahaben.flavorizr

import ly.com.tahaben.flavorizr.dsl.KmpFlavorizrExtension
import ly.com.tahaben.flavorizr.task.ExtractRubyScriptsTask
import ly.com.tahaben.flavorizr.task.FlavorizrTask
import ly.com.tahaben.flavorizr.task.GenerateFlavorEnumTask
import org.gradle.api.Plugin
import org.gradle.api.Project

class KmpFlavorPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        // 1. Create the DSL extension
        val extension = project.extensions.create(
            "kmpFlavorizr",
            KmpFlavorizrExtension::class.java,
        )

        // 2. Register the Ruby script extraction task
        val extractScripts = project.tasks.register(
            "extractFlavorizrScripts",
            ExtractRubyScriptsTask::class.java,
        ) { task ->
            task.outputDir.set(project.layout.buildDirectory.dir("kmpFlavorizr/scripts/darwin"))
        }

        // 3. Register the main flavorizr task (manual)
        project.tasks.register(
            "flavorizr",
            FlavorizrTask::class.java,
        ) { task ->
            task.dependsOn(extractScripts)
            task.scriptsDir.set(project.layout.buildDirectory.dir("kmpFlavorizr/scripts/darwin"))
            task.projectDirectory.set(project.layout.projectDirectory)
            task.rootProjectDirectory.set(project.rootProject.layout.projectDirectory)
        }

        // 4. Register the enum generation task (automatic, wired into compilation)
        val generateFlavorEnum = project.tasks.register(
            "generateFlavorEnum",
            GenerateFlavorEnumTask::class.java,
        ) { task ->
            task.outputDir.set(project.layout.buildDirectory.dir("generated/kmpFlavorizr/commonMain/kotlin"))

            // Wire extension values as task inputs (deferred via providers)
            task.flavorNames.set(project.provider {
                val config = extension.resolve(project)
                config.flavors.keys.toList()
            })
            task.flavorAppNames.set(project.provider {
                val config = extension.resolve(project)
                config.flavors.mapValues { it.value.app.name }
            })
            task.packageName.set(project.provider {
                val config = extension.resolve(project)
                config.flavorEnumPackage
            })
            // currentFlavorName is wired in afterEvaluate (below), once the consumer's
            // kmpFlavorizr { } block has populated the extension.
        }

        /*// 5. Register the flavor helpers generation task (manual, or via apply(from = ...))
        project.tasks.register(
            "generateFlavorHelpers",
            GenerateFlavorHelpersTask::class.java,
        ) { task ->
            task.outputFile.set(
                project.layout.buildDirectory.file("kmpFlavorizr/flavorizrHelpers.gradle.kts")
            )
            task.flavorNames.set(project.provider {
                extension.resolve(project).flavors.keys.toList()
            })
        }*/

        // 6. Wire config resolution and source set integration after evaluation
        project.afterEvaluate {
            // Resolve config now (configuration time) and set it on the task
            val config = extension.resolve(project)
            project.tasks.named("flavorizr", FlavorizrTask::class.java) { task ->
                task.resolvedConfig = config
            }

            // Wire currentFlavorName as a Provider so the configuration cache invalidates
            // when either the env var or the Gradle task request changes. This ensures
            // Flavor.current in the generated enum is regenerated on every relevant change
            // instead of being stuck on a previously cached value.
            val flavorNames = config.flavors.keys
            val envKey = config.environmentVariableKey
            val envFlavor = project.providers.environmentVariable(envKey)
                .map { it.takeIf { name -> name in flavorNames } ?: "" }
                .orElse("")
            val androidFlavor = project.provider {
                project.getAndroidBuildFlavorOrNull() ?: ""
            }
            project.tasks.named("generateFlavorEnum", GenerateFlavorEnumTask::class.java) { task ->
                // Android task-request match takes precedence, then env var.
                task.currentFlavorName.set(
                    androidFlavor.flatMap { android ->
                        if (android.isNotEmpty()) project.provider { android } else envFlavor
                    }
                )
            }

            wireIntoKmpCompilation(project, generateFlavorEnum)

            // Generate flavorizrHelpers.gradle.kts eagerly so it is ready for apply(from = ...) on the next build
            /*val helpersFile = project.layout.buildDirectory.get().asFile
                .resolve("kmpFlavorizr/flavorizrHelpers.gradle.kts")
            helpersFile.parentFile.mkdirs()
            helpersFile.writeText(GenerateFlavorHelpersTask.buildContent(config.flavors.keys.toList()))*/
        }
    }

    private fun wireIntoKmpCompilation(
        project: Project,
        enumTask: org.gradle.api.tasks.TaskProvider<GenerateFlavorEnumTask>,
    ) {
        // Try to find the KMP extension and add our generated source dir
        try {
            val kmpExtension = project.extensions.findByName("kotlin") ?: return
            val kmpClass = kmpExtension.javaClass

            // Use reflection to avoid hard dependency on KMP plugin
            val sourceSetsMethod = kmpClass.getMethod("getSourceSets")
            val sourceSets = sourceSetsMethod.invoke(kmpExtension)

            val getByNameMethod = sourceSets.javaClass.getMethod("getByName", String::class.java)
            val commonMain = getByNameMethod.invoke(sourceSets, "commonMain")

            val kotlinGetter = commonMain.javaClass.getMethod("getKotlin")
            val kotlinSourceSet = kotlinGetter.invoke(commonMain)

            val srcDirMethod = kotlinSourceSet.javaClass.getMethod("srcDir", Any::class.java)
            srcDirMethod.invoke(kotlinSourceSet, enumTask.flatMap { it.outputDir })

            project.logger.lifecycle("[kmp-flavorizr] Wired Flavor enum generation into commonMain source set")
        } catch (e: Exception) {
            project.logger.warn("[kmp-flavorizr] Could not wire into KMP commonMain source set: ${e.message}")
            project.logger.warn("[kmp-flavorizr] The Flavor enum will still be generated at build/generated/kmpFlavorizr/")
            project.logger.warn("[kmp-flavorizr] You may need to manually add it to your source sets")
        }
    }
}
