package ly.com.tahaben.flavorizr.processor.ios

import ly.com.tahaben.flavorizr.model.FlavorConfig
import ly.com.tahaben.flavorizr.processor.AbstractProcessor
import ly.com.tahaben.flavorizr.util.Constants
import ly.com.tahaben.flavorizr.util.ImageResizer
import java.io.File

/**
 * Resizes and places app icons for each iOS flavor into the appropriate
 * asset catalog directories, and generates Contents.json manifests.
 */
class IosIconProcessor(
    private val iosProjectPath: String,
    private val iosTargetName: String,
) : AbstractProcessor() {
    override val name = "ios:icons"

    override fun execute(config: FlavorConfig, projectDir: File) {
        for ((flavorName, flavor) in config.iosFlavors) {
            val ios = flavor.ios!!

            val iconPath = ios.icon ?: flavor.app.icon
            if (iconPath == null) {
                log("No icon configured for flavor '$flavorName', skipping")
                continue
            }

            val iconFile = File(projectDir, iconPath)
            if (!iconFile.exists()) {
                log("WARNING: Icon file not found: $iconPath for flavor '$flavorName'")
                continue
            }

            val appIconDir = File(
                projectDir,
                "$iosProjectPath/$iosTargetName/Assets.xcassets/${flavorName}AppIcon.appiconset"
            )
            appIconDir.mkdirs()

            // Generate icon files
            val generatedFilenames = mutableSetOf<String>()
            for (spec in Constants.IOS_ICON_SPECS) {
                val outputFile = File(appIconDir, spec.filename)
                if (generatedFilenames.add(spec.filename)) {
                    ImageResizer.resize(iconFile, outputFile, spec.actualPixels)
                }
            }

            // Generate Contents.json
            val contentsJson = generateContentsJson()
            File(appIconDir, "Contents.json").writeText(contentsJson)

            log("Generated iOS icons for flavor '$flavorName'")
        }
    }

    private fun generateContentsJson(): String {
        return buildString {
            appendLine("{")
            appendLine("  \"images\": [")

            val specs = Constants.IOS_ICON_SPECS
            for ((index, spec) in specs.withIndex()) {
                val sizeStr = spec.size.let {
                    if (it == it.toLong().toDouble()) "${it.toInt()}x${it.toInt()}"
                    else "${it}x${it}"
                }
                appendLine("    {")
                appendLine("      \"filename\": \"${spec.filename}\",")
                appendLine("      \"idiom\": \"${spec.idiom}\",")
                appendLine("      \"scale\": \"${spec.scale}x\",")
                appendLine("      \"size\": \"$sizeStr\"")
                if (index < specs.size - 1) {
                    appendLine("    },")
                } else {
                    appendLine("    }")
                }
            }

            appendLine("  ],")
            appendLine("  \"info\": {")
            appendLine("    \"author\": \"xcode\",")
            appendLine("    \"version\": 1")
            appendLine("  }")
            appendLine("}")
        }
    }
}
