package ly.com.tahaben.flavorizr.processor

import ly.com.tahaben.flavorizr.model.FlavorConfig
import java.io.File

class ProcessorPipeline(
    private val registry: ProcessorRegistry,
) {
    companion object {
        val DEFAULT_INSTRUCTIONS = listOf(
            // Android
            "android:manifest",
            "android:flavorizrGradle",
            "android:buildGradle",
            "android:icons",
            // Shared
            "shared:flavorEnum",
            // iOS
            "ios:xcconfig",
            "ios:buildConfigs",
            "ios:schemes",
            "ios:plist",
            "ios:podfile",
            "ios:icons",
            // Firebase
            "firebase:android",
            "firebase:ios",
            // Non-mobile run configurations (off unless explicitly enabled)
            "runConfig:desktop",
            "runConfig:web",
        )
    }

    fun execute(config: FlavorConfig, projectDir: File, customInstructions: List<String>? = null) {
        val instructions = customInstructions ?: DEFAULT_INSTRUCTIONS

        val filtered = instructions.filter { instruction ->
            when {
                instruction.startsWith("android:") -> config.hasAndroid
                instruction.startsWith("ios:") -> config.hasIos
                instruction.startsWith("firebase:android") -> config.androidFirebaseFlavors.isNotEmpty()
                instruction.startsWith("firebase:ios") -> config.iosFirebaseFlavors.isNotEmpty()
                instruction == "runConfig:desktop" -> config.hasDesktopRunConfigs
                instruction == "runConfig:web" -> config.hasWebRunConfigs
                else -> true
            }
        }

        println("[kmp-flavorizr] Executing ${filtered.size} processors (${instructions.size - filtered.size} skipped)")

        for (instruction in filtered) {
            val processor = registry.get(instruction)
            if (processor != null) {
                processor.execute(config, projectDir)
            } else {
                println("[kmp-flavorizr] WARNING: Unknown instruction '$instruction', skipping")
            }
        }

        println("[kmp-flavorizr] All processors complete")
    }
}
