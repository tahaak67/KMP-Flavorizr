package ly.com.tahaben.flavorizr

import ly.com.tahaben.flavorizr.dsl.KmpFlavorizrExtension
import org.gradle.api.Project
import java.util.regex.Pattern

/**
 * Returns the Android build flavor extracted from the Gradle task request (e.g. `assembleProdRelease` → `"prod"`),
 * or null if the running task does not match a configured flavor.
 */
internal fun Project.getAndroidBuildFlavorOrNull(): String? {
    val flavors = kmpFlavorNames()
    if (flavors.isEmpty()) return null
    val taskRequestsStr = gradle.startParameter.taskRequests.toString()
    val pattern: Pattern = if (taskRequestsStr.contains("assemble")) {
        Pattern.compile("assemble(\\w+)(Release|Debug)")
    } else {
        Pattern.compile("bundle(\\w+)(Release|Debug)")
    }
    val matcher = pattern.matcher(taskRequestsStr)
    val flavor = if (matcher.find()) matcher.group(1).lowercase() else null
    return if (flavor in flavors) flavor else null
}

/**
 * Returns the current build flavor by checking (in order):
 * 1. The Gradle task name (e.g. `assembleProdRelease`)
 * 2. The `FLAVOR` environment variable
 *
 * Returns null if neither source resolves to a configured flavor.
 */
fun Project.currentBuildFlavorOrNull(): String? {
    val extension = extensions.findByType(KmpFlavorizrExtension::class.java)
        ?.resolve(this)
    val envVariableKey = extension?.environmentVariableKey ?: "FLAVOR"
    val flavors = kmpFlavorNames()
    if (flavors.isEmpty()) return null
    return getAndroidBuildFlavorOrNull()
        ?: System.getenv()[envVariableKey]?.takeIf { it in flavors }
}

internal fun Project.kmpFlavorNames(): Set<String> =
    extensions.findByType(KmpFlavorizrExtension::class.java)
        ?.resolve(this)
        ?.flavors
        ?.keys
        ?.toSet()
        ?: emptySet()
