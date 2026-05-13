package ly.com.tahaben.flavorizr.processor

import ly.com.tahaben.flavorizr.processor.android.AndroidBuildGradleProcessor
import ly.com.tahaben.flavorizr.processor.android.AndroidFlavorizrGradleProcessor
import ly.com.tahaben.flavorizr.processor.android.AndroidIconProcessor
import ly.com.tahaben.flavorizr.processor.android.AndroidManifestProcessor
import ly.com.tahaben.flavorizr.processor.firebase.AndroidFirebaseProcessor
import ly.com.tahaben.flavorizr.processor.firebase.IosFirebaseProcessor
import ly.com.tahaben.flavorizr.processor.ios.*
import ly.com.tahaben.flavorizr.processor.shared.FlavorEnumProcessor
import ly.com.tahaben.flavorizr.processor.shared.RunConfigurationsProcessor
import java.io.File

class ProcessorRegistry(
    private val projectDir: File,
    private val rootProjectDir: File,
    private val scriptsDir: File,
    private val iosProjectPath: String,
    private val iosTargetName: String,
) {
    private val factories = mutableMapOf<String, () -> Processor>()

    private val moduleRelativePath: String =
        projectDir.toPath().let { module ->
            val root = rootProjectDir.toPath()
            if (module == root) "" else root.relativize(module).toString().replace(File.separatorChar, '/')
        }

    init {
        // Android processors
        register("android:manifest") { AndroidManifestProcessor() }
        register("android:flavorizrGradle") { AndroidFlavorizrGradleProcessor() }
        register("android:buildGradle") { AndroidBuildGradleProcessor() }
        register("android:icons") { AndroidIconProcessor() }

        // Shared processors
        register("shared:flavorEnum") { FlavorEnumProcessor() }

        // Run configurations for non-mobile targets
        register("runConfig:desktop") {
            RunConfigurationsProcessor(
                RunConfigurationsProcessor.Target.DESKTOP,
                rootProjectDir = rootProjectDir,
                moduleRelativePath = moduleRelativePath,
            )
        }
        register("runConfig:web") {
            RunConfigurationsProcessor(
                RunConfigurationsProcessor.Target.WEB,
                rootProjectDir = rootProjectDir,
                moduleRelativePath = moduleRelativePath,
            )
        }

        // iOS processors
        register("ios:xcconfig") { IosXcConfigProcessor(iosProjectPath, iosTargetName) }
        register("ios:buildConfigs") { IosBuildConfigProcessor(iosProjectPath, iosTargetName, scriptsDir) }
        register("ios:schemes") { IosSchemeProcessor(iosProjectPath, iosTargetName, scriptsDir) }
        register("ios:plist") { IosPlistProcessor(iosProjectPath, iosTargetName) }
        register("ios:podfile") { IosPodfileProcessor(iosProjectPath, iosTargetName) }
        register("ios:icons") { IosIconProcessor(iosProjectPath, iosTargetName) }

        // Firebase processors
        register("firebase:android") { AndroidFirebaseProcessor() }
        register("firebase:ios") { IosFirebaseProcessor(iosProjectPath, iosTargetName, scriptsDir) }
    }

    private fun register(name: String, factory: () -> Processor) {
        factories[name] = factory
    }

    fun get(name: String): Processor? = factories[name]?.invoke()
}
