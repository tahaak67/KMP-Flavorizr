package ly.com.tahaben.flavorizr.processor.firebase

import ly.com.tahaben.flavorizr.model.FlavorConfig
import ly.com.tahaben.flavorizr.processor.AbstractProcessor
import java.io.File

/**
 * Copies google-services.json files to flavor-specific source set directories.
 * The Android Gradle plugin automatically picks up the config from src/{flavor}/.
 */
class AndroidFirebaseProcessor : AbstractProcessor() {
    override val name = "firebase:android"

    override fun execute(config: FlavorConfig, projectDir: File) {
        for ((flavorName, flavor) in config.androidFirebaseFlavors) {
            val firebase = flavor.android!!.firebase!!
            val sourceFile = File(projectDir, firebase.configPath)

            if (!sourceFile.exists()) {
                log("WARNING: Firebase config not found: ${firebase.configPath} for flavor '$flavorName'")
                continue
            }

            val destFile = File(projectDir, "src/$flavorName/google-services.json")
            destFile.parentFile?.mkdirs()
            sourceFile.copyTo(destFile, overwrite = true)
            log("Copied google-services.json for flavor '$flavorName'")
        }
    }
}
