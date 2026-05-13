package ly.com.tahaben.flavorizr.dsl

import ly.com.tahaben.flavorizr.model.*
import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import javax.inject.Inject

abstract class GlobalAppDsl @Inject constructor(
    private val objects: ObjectFactory,
) {
    val android: GlobalAndroidDsl = objects.newInstance(GlobalAndroidDsl::class.java)
    val ios: GlobalIosDsl = objects.newInstance(GlobalIosDsl::class.java)
    val desktop: GlobalDesktopDsl = objects.newInstance(GlobalDesktopDsl::class.java)
    val web: GlobalWebDsl = objects.newInstance(GlobalWebDsl::class.java)

    private var androidConfigured = false
    private var iosConfigured = false
    private var desktopConfigured = false
    private var webConfigured = false

    fun android(action: Action<GlobalAndroidDsl>) {
        androidConfigured = true
        action.execute(android)
    }

    fun ios(action: Action<GlobalIosDsl>) {
        iosConfigured = true
        action.execute(ios)
    }

    fun desktop(action: Action<GlobalDesktopDsl>) {
        desktopConfigured = true
        action.execute(desktop)
    }

    fun web(action: Action<GlobalWebDsl>) {
        webConfigured = true
        action.execute(web)
    }

    internal fun resolve(): GlobalConfig {
        return GlobalConfig(
            android = if (androidConfigured) android.resolve() else null,
            ios = if (iosConfigured) ios.resolve() else null,
            desktop = if (desktopConfigured) desktop.resolve() else null,
            web = if (webConfigured) web.resolve() else null,
        )
    }
}

abstract class GlobalAndroidDsl {
    var flavorDimensions: String = "flavor-type"

    private val _resValues = mutableMapOf<String, ResValue>()
    private val _buildConfigFields = mutableMapOf<String, BuildConfigField>()

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

    internal fun resolve(): GlobalAndroidConfig {
        return GlobalAndroidConfig(
            flavorDimensions = flavorDimensions,
            resValues = _resValues.toMap(),
            buildConfigFields = _buildConfigFields.toMap(),
        )
    }
}

abstract class GlobalIosDsl {
    private val _buildSettings = mutableMapOf<String, String>()

    fun buildSettings(action: Action<BuildSettingsScope>) {
        action.execute(object : BuildSettingsScope {
            override fun put(key: String, value: String) {
                _buildSettings[key] = value
            }
        })
    }

    internal fun resolve(): GlobalIosConfig {
        return GlobalIosConfig(
            buildSettings = _buildSettings.toMap(),
        )
    }
}

abstract class GlobalDesktopDsl {
    /**
     * When true, generates per-flavor IntelliJ/Android Studio Gradle run configurations
     * under .idea/runConfigurations/, injecting the active flavor via the configured
     * environment variable key. Off by default.
     */
    var generateRunConfigurations: Boolean = false

    /** Gradle task each generated run configuration invokes. */
    var runTask: String = "run"

    internal fun resolve(): GlobalDesktopConfig {
        return GlobalDesktopConfig(
            generateRunConfigurations = generateRunConfigurations,
            runTask = runTask,
        )
    }
}

abstract class GlobalWebDsl {
    /**
     * When true, generates per-flavor IntelliJ/Android Studio Gradle run configurations
     * under .idea/runConfigurations/, injecting the active flavor via the configured
     * environment variable key. Off by default.
     */
    var generateRunConfigurations: Boolean = false

    /** Gradle task each generated run configuration invokes. */
    var runTask: String = "wasmJsBrowserDevelopmentRun"

    internal fun resolve(): GlobalWebConfig {
        return GlobalWebConfig(
            generateRunConfigurations = generateRunConfigurations,
            runTask = runTask,
        )
    }
}
