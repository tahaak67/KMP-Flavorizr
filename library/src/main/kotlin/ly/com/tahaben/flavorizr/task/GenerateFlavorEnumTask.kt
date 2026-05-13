package ly.com.tahaben.flavorizr.task

import ly.com.tahaben.flavorizr.model.FlavorConfig
import ly.com.tahaben.flavorizr.processor.shared.FlavorEnumProcessor
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*

@CacheableTask
abstract class GenerateFlavorEnumTask : DefaultTask() {

    @get:Input
    abstract val flavorNames: ListProperty<String>

    @get:Input
    abstract val flavorAppNames: MapProperty<String, String>

    @get:Input
    abstract val packageName: Property<String>

    @get:Input
    @get:Optional
    abstract val currentFlavorName: Property<String>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    init {
        group = "kmp-flavorizr"
        description = "Generates the Flavor enum into commonMain source set"
    }

    @TaskAction
    fun generate() {
        val names = flavorNames.get()
        val appNames = flavorAppNames.get()
        val pkg = packageName.get()

        if (names.isEmpty()) {
            logger.lifecycle("[kmp-flavorizr] No flavors configured, skipping enum generation")
            return
        }

        // Build a minimal FlavorConfig for enum generation
        val config = FlavorConfig(
            flavors = names.associateWith { name ->
                ly.com.tahaben.flavorizr.model.Flavor(
                    name = name,
                    app = ly.com.tahaben.flavorizr.model.FlavorApp(
                        name = appNames[name] ?: name,
                        icon = null,
                    ),
                    android = null,
                    ios = null,
                )
            },
            globalConfig = ly.com.tahaben.flavorizr.model.GlobalConfig(null, null, null, null),
            ideTargets = emptyList(),
            iosProjectPath = "",
            iosTargetName = "",
            flavorEnumPackage = pkg,
            environmentVariableKey = "",
        )

        val currentFlavor = currentFlavorName.orNull?.takeIf { it.isNotEmpty() }
        val content = FlavorEnumProcessor.generateFlavorEnum(config, currentFlavor)
        val packageDir = pkg.replace('.', '/')
        val outputDirectory = outputDir.get().asFile.resolve(packageDir)
        outputDirectory.mkdirs()
        val outputFile = outputDirectory.resolve("Flavor.kt")
        outputFile.writeText(content)

        logger.lifecycle("[kmp-flavorizr] Generated Flavor.kt with ${names.size} flavors (package: $pkg, current=${currentFlavor ?: "<none>"})")
    }
}
