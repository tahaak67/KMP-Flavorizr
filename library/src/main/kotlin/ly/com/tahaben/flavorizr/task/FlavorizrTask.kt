package ly.com.tahaben.flavorizr.task

import ly.com.tahaben.flavorizr.model.FlavorConfig
import ly.com.tahaben.flavorizr.processor.ProcessorPipeline
import ly.com.tahaben.flavorizr.processor.ProcessorRegistry
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

abstract class FlavorizrTask : DefaultTask() {

    @get:Internal
    abstract val scriptsDir: DirectoryProperty

    @get:Internal
    abstract val projectDirectory: DirectoryProperty

    @get:Internal
    abstract val rootProjectDirectory: DirectoryProperty

    /**
     * Serialized config set at configuration time via a provider.
     * This avoids calling project at execution time.
     */
    @get:Internal
    lateinit var resolvedConfig: FlavorConfig

    init {
        group = "kmp-flavorizr"
        description = "Runs the full kmp-flavorizr processor pipeline to configure flavors"
    }

    @TaskAction
    fun execute() {
        val config = resolvedConfig
        val projectDir = projectDirectory.get().asFile
        val rootProjectDir = rootProjectDirectory.get().asFile

        logger.lifecycle("[kmp-flavorizr] Starting flavorizr with ${config.flavors.size} flavors")
        logger.lifecycle("[kmp-flavorizr] Android flavors: ${config.androidFlavors.keys}")
        logger.lifecycle("[kmp-flavorizr] iOS flavors: ${config.iosFlavors.keys}")

        val scripts = scriptsDir.get().asFile
        val registry = ProcessorRegistry(
            projectDir = projectDir,
            rootProjectDir = rootProjectDir,
            scriptsDir = scripts,
            iosProjectPath = config.iosProjectPath,
            iosTargetName = config.iosTargetName,
        )

        val pipeline = ProcessorPipeline(registry)
        pipeline.execute(config, projectDir)

        logger.lifecycle("[kmp-flavorizr] Done!")
    }
}
