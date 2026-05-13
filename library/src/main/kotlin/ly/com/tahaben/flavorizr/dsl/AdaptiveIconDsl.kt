package ly.com.tahaben.flavorizr.dsl

import ly.com.tahaben.flavorizr.model.AdaptiveIcon

abstract class AdaptiveIconDsl {
    var foreground: String = ""
    var background: String = ""
    var monochrome: String? = null

    internal fun resolve(): AdaptiveIcon {
        return AdaptiveIcon(
            foreground = foreground,
            background = background,
            monochrome = monochrome,
        )
    }
}
