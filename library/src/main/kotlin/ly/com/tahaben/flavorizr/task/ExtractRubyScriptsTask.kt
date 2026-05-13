package ly.com.tahaben.flavorizr.task

import ly.com.tahaben.flavorizr.util.Constants
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

abstract class ExtractRubyScriptsTask : DefaultTask() {

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    init {
        group = "kmp-flavorizr"
        description = "Extracts bundled Ruby scripts for Xcode project manipulation"
    }

    @TaskAction
    fun extract() {
        val outputDirectory = outputDir.get().asFile
        outputDirectory.mkdirs()

        for (scriptName in Constants.RUBY_SCRIPTS) {
            val resourcePath = "scripts/darwin/$scriptName"
            val inputStream = javaClass.classLoader.getResourceAsStream(resourcePath)
                ?: throw IllegalStateException("Ruby script resource not found: $resourcePath")

            val outputFile = outputDirectory.resolve(scriptName)
            inputStream.use { input ->
                outputFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            outputFile.setExecutable(true)

            logger.lifecycle("[kmp-flavorizr] Extracted: $scriptName")
        }
    }
}
