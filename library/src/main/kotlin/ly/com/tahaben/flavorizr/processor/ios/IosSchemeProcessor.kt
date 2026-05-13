package ly.com.tahaben.flavorizr.processor.ios

import ly.com.tahaben.flavorizr.model.FlavorConfig
import ly.com.tahaben.flavorizr.processor.AbstractProcessor
import java.io.File

/**
 * Creates Xcode schemes for each flavor by running create_scheme.rb.
 * Each scheme maps build actions to the flavor's build configurations.
 */
class IosSchemeProcessor(
    private val iosProjectPath: String,
    private val iosTargetName: String,
    private val scriptsDir: File,
) : AbstractProcessor() {
    override val name = "ios:schemes"

    override fun execute(config: FlavorConfig, projectDir: File) {
        val iosDir = File(projectDir, iosProjectPath)
        val xcodeprojPath = findXcodeproj(iosDir)

        if (xcodeprojPath == null) {
            log("No .xcodeproj found in $iosProjectPath, skipping")
            return
        }

        for ((flavorName, _) in config.iosFlavors) {
            val command = listOf(
                "ruby",
                File(scriptsDir, "create_scheme.rb").absolutePath,
                xcodeprojPath.absolutePath,
                flavorName,
                iosTargetName,
            )

            log("Creating scheme: $flavorName")
            val process = ProcessBuilder(command)
                .directory(iosDir)
                .redirectErrorStream(true)
                .start()

            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()
            if (exitCode != 0) {
                throw RuntimeException("Failed to create scheme for '$flavorName':\n$output")
            }
        }
    }

    private fun findXcodeproj(iosDir: File): File? {
        return iosDir.listFiles()?.firstOrNull { it.extension == "xcodeproj" }
    }
}
