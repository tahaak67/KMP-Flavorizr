package ly.com.tahaben.flavorizr.dsl

import ly.com.tahaben.flavorizr.model.AndroidFlavor
import ly.com.tahaben.flavorizr.model.BuildConfigField
import ly.com.tahaben.flavorizr.model.ResValue
import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import javax.inject.Inject

abstract class AndroidFlavorDsl @Inject constructor(
    private val objects: ObjectFactory,
) {
    var applicationId: String = ""
    var icon: String? = null
    var roundIcon: String? = null
    var generateDummyAssets: Boolean = true

    private val _customConfig = mutableMapOf<String, Any>()
    private val _resValues = mutableMapOf<String, ResValue>()
    private val _buildConfigFields = mutableMapOf<String, BuildConfigField>()

    val adaptiveIcon: AdaptiveIconDsl = objects.newInstance(AdaptiveIconDsl::class.java)
    val firebase: FirebaseDsl = objects.newInstance(FirebaseDsl::class.java)
    private var firebaseConfigured = false
    private var adaptiveIconConfigured = false

    fun customConfig(action: Action<CustomConfigScope>) {
        action.execute(object : CustomConfigScope {
            override fun put(key: String, value: Any) {
                _customConfig[key] = value
            }
        })
    }

    fun resValues(action: Action<ResValueScope>) {
        action.execute(object : ResValueScope {
            override fun resValue(name: String, type: String, value: String) {
                _resValues[name] = ResValue(type, value)
            }
        })
    }

    fun buildConfigFields(action: Action<BuildConfigFieldScope>) {
        action.execute(object : BuildConfigFieldScope {
            override fun buildConfigField(name: String, type: String, value: String) {
                _buildConfigFields[name] = BuildConfigField(type, value)
            }
        })
    }

    fun adaptiveIcon(action: Action<AdaptiveIconDsl>) {
        adaptiveIconConfigured = true
        action.execute(adaptiveIcon)
    }

    fun firebase(action: Action<FirebaseDsl>) {
        firebaseConfigured = true
        action.execute(firebase)
    }

    internal fun resolve(): AndroidFlavor {
        return AndroidFlavor(
            applicationId = applicationId,
            icon = icon,
            roundIcon = roundIcon,
            generateDummyAssets = generateDummyAssets,
            customConfig = _customConfig.toMap(),
            resValues = _resValues.toMap(),
            buildConfigFields = _buildConfigFields.toMap(),
            adaptiveIcon = if (adaptiveIconConfigured) adaptiveIcon.resolve() else null,
            firebase = if (firebaseConfigured) firebase.resolve() else null,
        )
    }
}

interface CustomConfigScope {
    fun put(key: String, value: Any)
}

interface ResValueScope {
    fun resValue(name: String, type: String, value: String)
}

interface BuildConfigFieldScope {
    fun buildConfigField(name: String, type: String, value: String)
}
