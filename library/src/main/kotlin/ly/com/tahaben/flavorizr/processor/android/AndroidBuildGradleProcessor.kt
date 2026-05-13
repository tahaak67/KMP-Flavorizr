package ly.com.tahaben.flavorizr.processor.android

import ly.com.tahaben.flavorizr.model.FlavorConfig
import ly.com.tahaben.flavorizr.processor.AbstractProcessor
import ly.com.tahaben.flavorizr.util.FileMarker
import java.io.File

/**
 * Injects the flavorizr.gradle apply statement into the consumer's build file.
 * Supports both build.gradle (Groovy) and build.gradle.kts (Kotlin DSL).
 */
class AndroidBuildGradleProcessor : AbstractProcessor() {
    override val name = "android:buildGradle"

    companion object {
        private const val SECTION = "flavorDimensions"
        private const val APPLY_GROOVY = "apply from: 'flavorizr.gradle'"
        private const val APPLY_KTS = """apply(from = "flavorizr.gradle")"""
    }

    override fun execute(config: FlavorConfig, projectDir: File) {
        val groovyFile = File(projectDir, "build.gradle")
        val ktsFile = File(projectDir, "build.gradle.kts")
        when {
            groovyFile.exists() -> injectApply(groovyFile, APPLY_GROOVY)
            ktsFile.exists() -> injectApply(ktsFile, APPLY_KTS)
            else -> log("No build.gradle or build.gradle.kts found, skipping")
        }
    }

    private fun injectApply(buildFile: File, applyStatement: String) {
        val content = buildFile.readText()
        val begin = FileMarker.beginMarker(SECTION)

        if (content.contains(begin)) {
            val modified = FileMarker.replaceOrAppend(content, SECTION, "    $applyStatement")
            buildFile.writeText(modified)
            log("Updated existing injection in ${buildFile.name}")
            return
        }

        if (content.contains("flavorDimensions") && !content.contains(begin)) {
            log("WARNING: flavorDimensions already exists in ${buildFile.name} without kmp-flavorizr markers. Skipping injection to avoid conflicts.")
            return
        }

        val androidBlockPattern = Regex("""android\s*\{""")
        if (androidBlockPattern.containsMatchIn(content)) {
            val modified = FileMarker.replaceOrInsert(
                content,
                SECTION,
                "    $applyStatement",
                androidBlockPattern,
            )
            buildFile.writeText(modified)
            log("Injected apply statement into ${buildFile.name}")
        } else {
            val modified = FileMarker.replaceOrAppend(content, SECTION, applyStatement)
            buildFile.writeText(modified)
            log("Appended apply statement to ${buildFile.name}")
        }
    }
}
