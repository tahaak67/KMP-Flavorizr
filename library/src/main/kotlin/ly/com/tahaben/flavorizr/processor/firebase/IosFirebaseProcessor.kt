package ly.com.tahaben.flavorizr.processor.firebase

import ly.com.tahaben.flavorizr.model.FlavorConfig
import ly.com.tahaben.flavorizr.processor.AbstractProcessor
import ly.com.tahaben.flavorizr.util.Constants
import java.io.File

/**
 * Handles iOS Firebase configuration:
 * 1. Copies GoogleService-Info.plist to flavor-specific directories
 * 2. Generates firebaseScript.sh for build-time config switching
 * 3. Adds a Firebase build phase to the Xcode project (via Ruby script)
 */
class IosFirebaseProcessor(
    private val iosProjectPath: String,
    private val iosTargetName: String,
    private val scriptsDir: File,
) : AbstractProcessor() {
    override val name = "firebase:ios"

    override fun execute(config: FlavorConfig, projectDir: File) {
        if (config.iosFirebaseFlavors.isEmpty()) return

        val iosDir = File(projectDir, iosProjectPath)

        // Step 1: Copy config files per flavor into firebase/ (outside synchronized group)
        for ((flavorName, flavor) in config.iosFirebaseFlavors) {
            val firebase = flavor.ios!!.firebase!!
            val sourceFile = File(projectDir, firebase.configPath)

            if (!sourceFile.exists()) {
                log("WARNING: Firebase config not found: ${firebase.configPath} for flavor '$flavorName'")
                continue
            }

            val destDir = File(iosDir, "firebase/$flavorName")
            destDir.mkdirs()
            sourceFile.copyTo(File(destDir, "GoogleService-Info.plist"), overwrite = true)
            log("Copied GoogleService-Info.plist for flavor '$flavorName'")
        }

        // Create placeholder inside target dir (this one gets bundled into the app)
        val placeholderFile = File(iosDir, "$iosTargetName/GoogleService-Info.plist")
        if (!placeholderFile.exists()) {
            val firstConfig = config.iosFirebaseFlavors.values.first().ios!!.firebase!!
            val sourceFile = File(projectDir, firstConfig.configPath)
            if (sourceFile.exists()) {
                sourceFile.copyTo(placeholderFile, overwrite = true)
            }
        }

        // Step 2: Generate firebaseScript.sh
        val scriptContent = generateFirebaseScript(config)
        val scriptFile = File(iosDir, "firebaseScript.sh")
        scriptFile.writeText(scriptContent)
        scriptFile.setExecutable(true)
        log("Generated: firebaseScript.sh")

        // Step 3: Add build phase (via Ruby script)
        val xcodeprojPath = iosDir.listFiles()?.firstOrNull { it.extension == "xcodeproj" }
        if (xcodeprojPath != null) {
            addFirebaseBuildPhase(xcodeprojPath, iosDir)
        }
    }

    private fun generateFirebaseScript(config: FlavorConfig): String {
        return buildString {
            appendLine("#!/bin/bash")

            val flavors = config.iosFirebaseFlavors.entries.toList()
            for ((index, entry) in flavors.withIndex()) {
                val (flavorName, _) = entry
                val keyword = if (index == 0) "if" else "elif"

                val conditions = Constants.IOS_BUILD_MODES.joinToString(" || \\\n   ") { mode ->
                    """[ "${"$"}CONFIGURATION" == "$mode-$flavorName" ]"""
                }
                appendLine("$keyword $conditions; then")
                appendLine("""  cp -f "${"$"}SRCROOT/firebase/$flavorName/GoogleService-Info.plist" "${"$"}SRCROOT/$iosTargetName/GoogleService-Info.plist"""")
            }
            appendLine("fi")
        }
    }

    private fun addFirebaseBuildPhase(xcodeprojPath: File, iosDir: File) {
        val script = File(scriptsDir, "add_firebase_build_phase.rb")
        if (!script.exists()) {
            log("WARNING: add_firebase_build_phase.rb not found, skipping build phase registration")
            return
        }

        val command = listOf(
            "ruby",
            script.absolutePath,
            xcodeprojPath.absolutePath,
            iosTargetName,
        )

        val process = ProcessBuilder(command)
            .directory(iosDir)
            .redirectErrorStream(true)
            .start()

        val output = process.inputStream.bufferedReader().readText()
        val exitCode = process.waitFor()
        if (exitCode != 0) {
            log("WARNING: Failed to add Firebase build phase: $output")
        } else {
            log("Added Firebase build phase to Xcode project")
        }
    }
}
