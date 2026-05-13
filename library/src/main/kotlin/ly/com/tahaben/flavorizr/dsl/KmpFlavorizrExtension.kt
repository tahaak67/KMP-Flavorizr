package ly.com.tahaben.flavorizr.dsl

import ly.com.tahaben.flavorizr.model.FlavorConfig
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import javax.inject.Inject

abstract class KmpFlavorizrExtension @Inject constructor(
    private val objects: ObjectFactory,
) {
    private val flavors = mutableMapOf<String, FlavorDsl>()

    val globalApp: GlobalAppDsl = objects.newInstance(GlobalAppDsl::class.java)

    var iosProjectPath: String = "../iosApp"
    var iosTargetName: String = "iosApp"
    var flavorEnumPackage: String = ""

    /*
    * The environment variable key used to determine the active flavor at build time.
    * Used on all platforms except Android.
    * */
    var envVariableKey: String = "FLAVOR"

    private val _ideTargets = mutableListOf<String>()

    fun flavor(name: String, action: Action<FlavorDsl>) {
        val flavor = flavors.getOrPut(name) { objects.newInstance(FlavorDsl::class.java, name) }
        action.execute(flavor)
    }

    fun app(action: Action<GlobalAppDsl>) {
        action.execute(globalApp)
    }

    fun ide(vararg targets: String) {
        _ideTargets.addAll(targets)
    }

    internal fun resolve(project: Project): FlavorConfig {
        val resolvedPackage = flavorEnumPackage.ifEmpty {
            project.group.toString().ifEmpty { "com.example.flavors" }
        }

        return FlavorConfig(
            flavors = flavors.map { (name, dsl) -> name to dsl.resolve(name) }.toMap(),
            globalConfig = globalApp.resolve(),
            ideTargets = _ideTargets.toList(),
            iosProjectPath = iosProjectPath,
            iosTargetName = iosTargetName,
            flavorEnumPackage = resolvedPackage,
            environmentVariableKey = envVariableKey,
        )
    }
}
