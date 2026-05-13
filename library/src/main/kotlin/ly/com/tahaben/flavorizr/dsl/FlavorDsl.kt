package ly.com.tahaben.flavorizr.dsl

import ly.com.tahaben.flavorizr.model.Flavor
import ly.com.tahaben.flavorizr.model.FlavorApp
import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import javax.inject.Inject

abstract class FlavorDsl @Inject constructor(
    private val name: String,
    private val objects: ObjectFactory,
) {
    val app: AppDsl = objects.newInstance(AppDsl::class.java)
    val android: AndroidFlavorDsl = objects.newInstance(AndroidFlavorDsl::class.java)
    val ios: IosFlavorDsl = objects.newInstance(IosFlavorDsl::class.java)

    private var androidConfigured = false
    private var iosConfigured = false

    fun app(action: Action<AppDsl>) {
        action.execute(app)
    }

    fun android(action: Action<AndroidFlavorDsl>) {
        androidConfigured = true
        action.execute(android)
    }

    fun ios(action: Action<IosFlavorDsl>) {
        iosConfigured = true
        action.execute(ios)
    }

    internal fun resolve(flavorName: String): Flavor {
        return Flavor(
            name = flavorName,
            app = FlavorApp(
                name = app.name ?: flavorName,
                icon = app.icon,
            ),
            android = if (androidConfigured) android.resolve() else null,
            ios = if (iosConfigured) ios.resolve() else null,
        )
    }
}
