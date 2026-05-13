package ly.com.tahaben.flavorizr.processor.ios

import ly.com.tahaben.flavorizr.model.FlavorConfig
import ly.com.tahaben.flavorizr.processor.AbstractProcessor
import java.io.File

/**
 * Registers a file in the Xcode project using the add_file.rb Ruby script.
 * Used internally by other iOS processors.
 */
class IosAddFileProcessor(
    private val iosProjectPath: String,
    private val iosTargetName: String,
    private val scriptsDir: File,
    private val filePath: String,
    private val groupName: String,
) : AbstractProcessor() {
    override val name = "ios:addFile"

    override fun execute(config: FlavorConfig, projectDir: File) {
        val iosDir = File(projectDir, iosProjectPath)
        val xcodeprojPath = findXcodeproj(iosDir) ?: return

        val command = listOf(
            "ruby",
            File(scriptsDir, "add_file.rb").absolutePath,
            xcodeprojPath.absolutePath,
            filePath,
            groupName,
            iosTargetName,
        )

        log("Adding file to Xcode project: $filePath")
        val process = ProcessBuilder(command)
            .directory(iosDir)
            .redirectErrorStream(true)
            .start()

        val output = process.inputStream.bufferedReader().readText()
        val exitCode = process.waitFor()
        if (exitCode != 0) {
            log("WARNING: Failed to add file to Xcode project: $output")
        }
    }

    private fun findXcodeproj(iosDir: File): File? {
        return iosDir.listFiles()?.firstOrNull { it.extension == "xcodeproj" }
    }
}
