package ly.com.tahaben.flavorizr.processor.ios

import ly.com.tahaben.flavorizr.model.FlavorConfig
import ly.com.tahaben.flavorizr.model.IosFlavor
import ly.com.tahaben.flavorizr.processor.AbstractProcessor
import ly.com.tahaben.flavorizr.util.Constants
import java.io.File

/**
 * Generates xcconfig files for each flavor and build mode (Debug/Profile/Release).
 * Each xcconfig contains the flavor-specific build variables.
 */
class IosXcConfigProcessor(
    private val iosProjectPath: String,
    private val iosTargetName: String,
) : AbstractProcessor() {
    override val name = "ios:xcconfig"

    override fun execute(config: FlavorConfig, projectDir: File) {
        val iosDir = File(projectDir, iosProjectPath)
        val targetDir = File(iosDir, iosTargetName)
        val globalBuildSettings = config.globalConfig.ios?.buildSettings ?: emptyMap()

        for ((flavorName, flavor) in config.iosFlavors) {
            val ios = flavor.ios!!

            for (mode in Constants.IOS_BUILD_MODES) {
                val content = generateXcConfig(flavorName, flavor, ios, mode, globalBuildSettings)
                val fileName = "$flavorName${mode}.xcconfig"
                val file = File(targetDir, fileName)
                file.parentFile?.mkdirs()
                file.writeText(content)
                log("Generated: $iosTargetName/$fileName")
            }
        }
    }

    private fun generateXcConfig(
        flavorName: String,
        flavor: ly.com.tahaben.flavorizr.model.Flavor,
        ios: IosFlavor,
        mode: String,
        globalBuildSettings: Map<String, String>,
    ): String {
        val podConfigName = when (mode) {
            "Debug" -> "debug"
            "Profile" -> "release"
            "Release" -> "release"
            else -> mode.lowercase()
        }

        return buildString {
            // Includes
            appendLine("#include? \"Pods/Target Support Files/Pods-$iosTargetName/Pods-$iosTargetName.$podConfigName.xcconfig\"")
            appendLine()

            // Core variables (always present)
            appendLine("ASSET_PREFIX=$flavorName")
            appendLine("BUNDLE_NAME=${flavor.app.name}")
            appendLine("BUNDLE_DISPLAY_NAME=${flavor.app.name}")

            // Global build settings
            for ((key, value) in globalBuildSettings) {
                appendLine("$key=$value")
            }

            // Flavor-specific build settings
            for ((key, value) in ios.buildSettings) {
                appendLine("$key=$value")
            }

            // Variables filtered by target
            for ((varName, variable) in ios.variables) {
                val target = variable.target
                if (target == null || target.equals(mode, ignoreCase = true)) {
                    appendLine("$varName=${variable.value}")
                }
            }
        }
    }
}
