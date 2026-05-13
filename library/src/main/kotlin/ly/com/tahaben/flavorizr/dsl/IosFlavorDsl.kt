package ly.com.tahaben.flavorizr.dsl

import ly.com.tahaben.flavorizr.model.IosFlavor
import ly.com.tahaben.flavorizr.model.IosVariable
import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import javax.inject.Inject

abstract class IosFlavorDsl @Inject constructor(
    private val objects: ObjectFactory,
) {
    var bundleId: String = ""
    var icon: String? = null
    var generateDummyAssets: Boolean = true

    private val _buildSettings = mutableMapOf<String, String>()
    private val _variables = mutableMapOf<String, IosVariable>()

    val firebase: FirebaseDsl = objects.newInstance(FirebaseDsl::class.java)
    private var firebaseConfigured = false

    fun buildSettings(action: Action<BuildSettingsScope>) {
        action.execute(object : BuildSettingsScope {
            override fun put(key: String, value: String) {
                _buildSettings[key] = value
            }
        })
    }

    fun variables(action: Action<IosVariablesScope>) {
        action.execute(object : IosVariablesScope {
            override fun variable(name: String, value: String, target: String?) {
                _variables[name] = IosVariable(value, target)
            }
        })
    }

    fun firebase(action: Action<FirebaseDsl>) {
        firebaseConfigured = true
        action.execute(firebase)
    }

    internal fun resolve(): IosFlavor {
        return IosFlavor(
            bundleId = bundleId,
            icon = icon,
            generateDummyAssets = generateDummyAssets,
            buildSettings = _buildSettings.toMap(),
            variables = _variables.toMap(),
            firebase = if (firebaseConfigured) firebase.resolve() else null,
        )
    }
}

interface BuildSettingsScope {
    fun put(key: String, value: String)
}

interface IosVariablesScope {
    fun variable(name: String, value: String, target: String? = null)
}
