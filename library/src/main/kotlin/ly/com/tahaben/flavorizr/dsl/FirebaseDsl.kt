package ly.com.tahaben.flavorizr.dsl

import ly.com.tahaben.flavorizr.model.FirebaseConfig

abstract class FirebaseDsl {
    var config: String = ""

    internal fun resolve(): FirebaseConfig? {
        return if (config.isNotEmpty()) FirebaseConfig(config) else null
    }
}
