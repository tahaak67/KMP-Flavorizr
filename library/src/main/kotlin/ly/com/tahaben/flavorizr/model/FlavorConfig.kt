package ly.com.tahaben.flavorizr.model

data class FlavorConfig(
    val flavors: Map<String, Flavor>,
    val globalConfig: GlobalConfig,
    val ideTargets: List<String>,
    val iosProjectPath: String,
    val iosTargetName: String,
    val flavorEnumPackage: String,
    val environmentVariableKey: String,
) {
    val androidFlavors: Map<String, Flavor>
        get() = flavors.filter { it.value.android != null }

    val iosFlavors: Map<String, Flavor>
        get() = flavors.filter { it.value.ios != null }

    val hasAndroid: Boolean get() = androidFlavors.isNotEmpty()
    val hasIos: Boolean get() = iosFlavors.isNotEmpty()

    val androidFirebaseFlavors: Map<String, Flavor>
        get() = flavors.filter { it.value.android?.firebase != null }

    val iosFirebaseFlavors: Map<String, Flavor>
        get() = flavors.filter { it.value.ios?.firebase != null }

    val hasDesktopRunConfigs: Boolean
        get() = flavors.isNotEmpty() && globalConfig.desktop?.generateRunConfigurations == true

    val hasWebRunConfigs: Boolean
        get() = flavors.isNotEmpty() && globalConfig.web?.generateRunConfigurations == true
}

data class Flavor(
    val name: String,
    val app: FlavorApp,
    val android: AndroidFlavor?,
    val ios: IosFlavor?,
)

data class FlavorApp(
    val name: String,
    val icon: String?,
)

data class AndroidFlavor(
    val applicationId: String,
    val icon: String?,
    val roundIcon: String?,
    val generateDummyAssets: Boolean,
    val customConfig: Map<String, Any>,
    val resValues: Map<String, ResValue>,
    val buildConfigFields: Map<String, BuildConfigField>,
    val adaptiveIcon: AdaptiveIcon?,
    val firebase: FirebaseConfig?,
)

data class IosFlavor(
    val bundleId: String,
    val icon: String?,
    val generateDummyAssets: Boolean,
    val buildSettings: Map<String, String>,
    val variables: Map<String, IosVariable>,
    val firebase: FirebaseConfig?,
)

data class ResValue(
    val type: String,
    val value: String,
)

data class BuildConfigField(
    val type: String,
    val value: String,
)

data class AdaptiveIcon(
    val foreground: String,
    val background: String,
    val monochrome: String?,
)

data class FirebaseConfig(
    val configPath: String,
)

data class IosVariable(
    val value: String,
    val target: String?,
)

data class GlobalConfig(
    val android: GlobalAndroidConfig?,
    val ios: GlobalIosConfig?,
    val desktop: GlobalDesktopConfig?,
    val web: GlobalWebConfig?,
)

data class GlobalAndroidConfig(
    val flavorDimensions: String,
    val resValues: Map<String, ResValue>,
    val buildConfigFields: Map<String, BuildConfigField>,
)

data class GlobalIosConfig(
    val buildSettings: Map<String, String>,
)

data class GlobalDesktopConfig(
    val generateRunConfigurations: Boolean,
    val runTask: String,
)

data class GlobalWebConfig(
    val generateRunConfigurations: Boolean,
    val runTask: String,
)
