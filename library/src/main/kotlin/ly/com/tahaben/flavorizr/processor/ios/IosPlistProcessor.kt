package ly.com.tahaben.flavorizr.processor.ios

import ly.com.tahaben.flavorizr.model.FlavorConfig
import ly.com.tahaben.flavorizr.processor.AbstractProcessor
import java.io.File

/**
 * Modifies Info.plist to use build variable references instead of hardcoded values.
 * Replaces CFBundleName and CFBundleDisplayName with $(BUNDLE_NAME) and $(BUNDLE_DISPLAY_NAME).
 */
class IosPlistProcessor(
    private val iosProjectPath: String,
    private val iosTargetName: String,
) : AbstractProcessor() {
    override val name = "ios:plist"

    override fun execute(config: FlavorConfig, projectDir: File) {
        val plistFile = File(projectDir, "$iosProjectPath/$iosTargetName/Info.plist")
        if (!plistFile.exists()) {
            log("Info.plist not found at $iosProjectPath/$iosTargetName/Info.plist, skipping")
            return
        }
        processPlist(plistFile, projectDir)
    }

    private fun processPlist(plistFile: File, projectDir: File) {
        var content = plistFile.readText()

        // Replace CFBundleName value
        content = replacePlistValue(content, "CFBundleName", "\$(BUNDLE_NAME)")

        // Replace CFBundleDisplayName value
        content = replacePlistValue(content, "CFBundleDisplayName", "\$(BUNDLE_DISPLAY_NAME)")

        plistFile.writeText(content)
        log("Modified: ${plistFile.relativeTo(projectDir)}")
    }

    /**
     * Replaces the <string> value following a <key> in a plist XML file.
     */
    private fun replacePlistValue(content: String, key: String, newValue: String): String {
        val pattern = Regex(
            """(<key>\s*${Regex.escape(key)}\s*</key>\s*<string>)(.*?)(</string>)""",
            RegexOption.DOT_MATCHES_ALL,
        )
        return pattern.replace(content) { matchResult ->
            "${matchResult.groupValues[1]}$newValue${matchResult.groupValues[3]}"
        }
    }
}
