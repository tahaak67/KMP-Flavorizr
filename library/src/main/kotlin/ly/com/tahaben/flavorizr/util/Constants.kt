package ly.com.tahaben.flavorizr.util

object Constants {
    const val FLAVOR_DIMENSIONS_DEFAULT = "flavor-type"
    const val PLUGIN_NAME = "kmp-flavorizr"

    // Android icon sizes: density -> size in pixels
    val ANDROID_ICON_SIZES = mapOf(
        "mipmap-mdpi" to 48,
        "mipmap-hdpi" to 72,
        "mipmap-xhdpi" to 96,
        "mipmap-xxhdpi" to 144,
        "mipmap-xxxhdpi" to 192,
    )

    // Android adaptive icon layer sizes: density -> size in pixels
    val ANDROID_ADAPTIVE_ICON_SIZES = mapOf(
        "drawable-mdpi" to 108,
        "drawable-hdpi" to 162,
        "drawable-xhdpi" to 216,
        "drawable-xxhdpi" to 324,
        "drawable-xxxhdpi" to 432,
    )

    // iOS icon sizes: (size, scale) -> actual pixels
    data class IosIconSpec(
        val size: Double,
        val scale: Int,
        val idiom: String,
    ) {
        val actualPixels: Int get() = (size * scale).toInt()
        val filename: String
            get() = "icon_${
                size.let {
                    if (it == it.toLong().toDouble()) "${it.toInt()}" else "$it"
                }
            }@${scale}x.png"
    }

    val IOS_ICON_SPECS = listOf(
        IosIconSpec(20.0, 2, "iphone"),
        IosIconSpec(20.0, 3, "iphone"),
        IosIconSpec(29.0, 1, "iphone"),
        IosIconSpec(29.0, 2, "iphone"),
        IosIconSpec(29.0, 3, "iphone"),
        IosIconSpec(40.0, 2, "iphone"),
        IosIconSpec(40.0, 3, "iphone"),
        IosIconSpec(60.0, 2, "iphone"),
        IosIconSpec(60.0, 3, "iphone"),
        IosIconSpec(20.0, 1, "ipad"),
        IosIconSpec(20.0, 2, "ipad"),
        IosIconSpec(29.0, 1, "ipad"),
        IosIconSpec(29.0, 2, "ipad"),
        IosIconSpec(40.0, 1, "ipad"),
        IosIconSpec(40.0, 2, "ipad"),
        IosIconSpec(76.0, 1, "ipad"),
        IosIconSpec(76.0, 2, "ipad"),
        IosIconSpec(83.5, 2, "ipad"),
        IosIconSpec(1024.0, 1, "ios-marketing"),
    )

    val IOS_BUILD_MODES = listOf("Debug", "Profile", "Release")

    // Ruby script resource paths
    val RUBY_SCRIPTS = listOf(
        "add_file.rb",
        "add_build_configuration.rb",
        "create_scheme.rb",
        "add_firebase_build_phase.rb",
    )
}
