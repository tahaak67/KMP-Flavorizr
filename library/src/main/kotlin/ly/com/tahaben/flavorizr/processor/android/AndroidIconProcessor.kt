package ly.com.tahaben.flavorizr.processor.android

import ly.com.tahaben.flavorizr.model.FlavorConfig
import ly.com.tahaben.flavorizr.processor.AbstractProcessor
import ly.com.tahaben.flavorizr.util.Constants
import ly.com.tahaben.flavorizr.util.ImageResizer
import java.io.File

/**
 * Resizes and places launcher icons for each Android flavor into the appropriate
 * mipmap density folders. Also handles adaptive icons if configured.
 */
class AndroidIconProcessor : AbstractProcessor() {
    override val name = "android:icons"

    override fun execute(config: FlavorConfig, projectDir: File) {
        for ((flavorName, flavor) in config.androidFlavors) {
            val android = flavor.android!!

            // Determine icon source: platform-specific > shared app icon
            val iconPath = android.icon ?: flavor.app.icon
            if (iconPath == null) {
                log("No icon configured for flavor '$flavorName', skipping")
                continue
            }

            val iconFile = File(projectDir, iconPath)
            if (!iconFile.exists()) {
                log("WARNING: Icon file not found: $iconPath for flavor '$flavorName'")
                continue
            }

            // Standard launcher icons
            for ((density, size) in Constants.ANDROID_ICON_SIZES) {
                val outputFile = File(projectDir, "src/$flavorName/res/$density/ic_launcher.png")
                ImageResizer.resize(iconFile, outputFile, size)
            }
            log("Generated launcher icons for flavor '$flavorName'")

            val roundIconPath = android.roundIcon
            if (roundIconPath == null) {
                log("No round icon configured for flavor '$flavorName', skipping")
                continue
            }

            val roundIconFile = File(projectDir, roundIconPath)
            if (!roundIconFile.exists()) {
                log("WARNING: Round icon file not found: $roundIconPath for flavor '$flavorName'")
                continue
            }

            // Standard launcher icons
            for ((density, size) in Constants.ANDROID_ICON_SIZES) {
                val outputFile = File(projectDir, "src/$flavorName/res/$density/ic_launcher_round.png")
                ImageResizer.resize(roundIconFile, outputFile, size)
            }
            log("Generated round launcher icons for flavor '$flavorName'")

            // Adaptive icon layers
            val adaptiveIcon = android.adaptiveIcon
            if (adaptiveIcon != null) {
                generateAdaptiveIcons(config, projectDir, flavorName, adaptiveIcon)
            }
        }
    }

    private fun generateAdaptiveIcons(
        config: FlavorConfig,
        projectDir: File,
        flavorName: String,
        adaptiveIcon: ly.com.tahaben.flavorizr.model.AdaptiveIcon,
    ) {
        val foregroundFile = File(projectDir, adaptiveIcon.foreground)
        val backgroundFile = File(projectDir, adaptiveIcon.background)
        val monochromeFile = adaptiveIcon.monochrome?.let { File(projectDir, it) }

        // Generate layers for each density
        for ((density, size) in Constants.ANDROID_ADAPTIVE_ICON_SIZES) {
            if (foregroundFile.exists()) {
                val output = File(projectDir, "src/$flavorName/res/$density/ic_launcher_foreground.png")
                ImageResizer.resize(foregroundFile, output, size)
            }
            if (backgroundFile.exists()) {
                val output = File(projectDir, "src/$flavorName/res/$density/ic_launcher_background.png")
                ImageResizer.resize(backgroundFile, output, size)
            }
            if (monochromeFile?.exists() == true) {
                val output = File(projectDir, "src/$flavorName/res/$density/ic_launcher_monochrome.png")
                ImageResizer.resize(monochromeFile, output, size)
            }
        }

        // Generate adaptive icon XML
        val xmlDir = File(projectDir, "src/$flavorName/res/mipmap-anydpi-v26")
        xmlDir.mkdirs()
        val xmlFile = File(xmlDir, "ic_launcher.xml")
        xmlFile.writeText(generateAdaptiveIconXml(adaptiveIcon.monochrome != null))
        log("Generated adaptive icons for flavor '$flavorName'")
    }

    private fun generateAdaptiveIconXml(hasMonochrome: Boolean): String {
        return buildString {
            appendLine("""<?xml version="1.0" encoding="utf-8"?>""")
            appendLine("""<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">""")
            appendLine("""    <background android:drawable="@drawable/ic_launcher_background" />""")
            appendLine("""    <foreground android:drawable="@drawable/ic_launcher_foreground" />""")
            if (hasMonochrome) {
                appendLine("""    <monochrome android:drawable="@drawable/ic_launcher_monochrome" />""")
            }
            appendLine("""</adaptive-icon>""")
        }
    }
}
