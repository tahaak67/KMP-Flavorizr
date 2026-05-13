package ly.com.tahaben.flavorizr.processor.ios

import ly.com.tahaben.flavorizr.model.FlavorConfig
import ly.com.tahaben.flavorizr.processor.AbstractProcessor
import ly.com.tahaben.flavorizr.util.Constants
import java.io.File
import java.util.*

/**
 * Creates Xcode build configurations for each flavor by running the
 * add_build_configuration.rb Ruby script.
 */
class IosBuildConfigProcessor(
    private val iosProjectPath: String,
    private val iosTargetName: String,
    private val scriptsDir: File,
) : AbstractProcessor() {
    override val name = "ios:buildConfigs"

    override fun execute(config: FlavorConfig, projectDir: File) {
        val iosDir = File(projectDir, iosProjectPath)
        val xcodeprojPath = findXcodeproj(iosDir)

        if (xcodeprojPath == null) {
            log("No .xcodeproj found in $iosProjectPath, skipping")
            return
        }

        checkRubyAvailable()

        val globalBuildSettings = config.globalConfig.ios?.buildSettings ?: emptyMap()
        val envVariableKey = config.environmentVariableKey

        for ((flavorName, flavor) in config.iosFlavors) {
            val ios = flavor.ios!!

            for (mode in Constants.IOS_BUILD_MODES) {
                val xcConfigPath = "$iosTargetName/$flavorName${mode}.xcconfig"
                val buildSettings = buildBuildSettings(
                    flavorName,
                    ios.bundleId,
                    globalBuildSettings,
                    ios.buildSettings,
                    mode,
                    envVariableKey
                )
                val buildSettingsBase64 = Base64.getEncoder().encodeToString(
                    buildSettings.toJson().toByteArray()
                )

                val command = listOf(
                    "ruby",
                    File(scriptsDir, "add_build_configuration.rb").absolutePath,
                    xcodeprojPath.absolutePath,
                    xcConfigPath,
                    flavorName,
                    mode.lowercase(),
                    buildSettingsBase64,
                    iosTargetName,
                )

                log("Creating build config: $mode-$flavorName")
                val process = ProcessBuilder(command)
                    .directory(iosDir)
                    .redirectErrorStream(true)
                    .start()

                val output = process.inputStream.bufferedReader().readText()
                val exitCode = process.waitFor()
                if (exitCode != 0) {
                    throw RuntimeException("Failed to create build config $mode-$flavorName:\n$output")
                }
            }
        }
    }

    private fun buildBuildSettings(
        flavorName: String,
        bundleId: String,
        globalSettings: Map<String, String>,
        flavorSettings: Map<String, String>,
        mode: String,
        envVariableKey: String,
    ): Map<String, String> {
        val settings = linkedMapOf<String, String>()
        settings["PRODUCT_NAME"] = "\$(BUNDLE_DISPLAY_NAME)"
        settings["ASSETCATALOG_COMPILER_APPICON_NAME"] = "\$(ASSET_PREFIX)AppIcon"
        settings["LD_RUNPATH_SEARCH_PATHS"] = "\$(inherited) @executable_path/Frameworks"
        settings["SWIFT_VERSION"] = "5.0"
        settings["INFOPLIST_FILE"] = "$iosTargetName/Info.plist"
        settings["PRODUCT_BUNDLE_IDENTIFIER"] = bundleId
        settings["KOTLIN_FRAMEWORK_BUILD_TYPE"] = if (mode.equals("debug", ignoreCase = true)) "debug" else "release"
        settings[envVariableKey] = flavorName
        settings.putAll(globalSettings)
        settings.putAll(flavorSettings)
        return settings
    }

    private fun Map<String, String>.toJson(): String {
        return buildString {
            append("{")
            entries.forEachIndexed { index, (key, value) ->
                if (index > 0) append(",")
                append("\"$key\":\"$value\"")
            }
            append("}")
        }
    }

    private fun findXcodeproj(iosDir: File): File? {
        return iosDir.listFiles()?.firstOrNull { it.extension == "xcodeproj" }
    }

    private fun checkRubyAvailable() {
        try {
            val process = ProcessBuilder("ruby", "--version")
                .redirectErrorStream(true)
                .start()
            process.waitFor()
            if (process.exitValue() != 0) {
                throw RuntimeException("Ruby is not available. iOS flavor configuration requires Ruby and the 'xcodeproj' gem.")
            }
        } catch (e: Exception) {
            if (e is RuntimeException) throw e
            throw RuntimeException(
                "Ruby is not available. iOS flavor configuration requires Ruby and the 'xcodeproj' gem.",
                e
            )
        }
    }
}
