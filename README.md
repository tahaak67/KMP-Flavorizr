# KMP flavorizr

A Gradle plugin for configuring build flavors (variants) across Android and iOS in Kotlin Multiplatform projects. Define
your flavors once in Gradle DSL and let the plugin generate the platform-specific configuration files.

I found myself repeating the same setup for each project when I wanted to add flavors or configure my icons or any other
things for my flavors, so I decided to create a plugin to reuse it across all the projects and automate this process.
> I only tested this plugin locally, and it worked well for me.
>
> Please **back up** your project before using it. and report any issues you find.

## Features

- Gradle Kotlin DSL for flavor configuration — no YAML files
- Android: generates `productFlavors`, modifies `AndroidManifest.xml`, resizes launcher icons
- iOS: generates xcconfig files, creates Xcode build configurations and schemes, modifies `Info.plist` and `Podfile`
- Shared: generates a Kotlin `Flavor` enum in `commonMain` for use across all platforms
- Firebase support: copies per-flavor `google-services.json` and `GoogleService-Info.plist`
- Idempotent — safe to re-run when adding or changing flavors

## Requirements

- Gradle 8.0+
- Kotlin 2.0+
- For iOS: Ruby and the `xcodeproj` gem (`gem install xcodeproj`)

## Installation

Add the plugin to your module's `build.gradle.kts`:

```kotlin
plugins {
    id("ly.com.tahaben.kmp-flavorizr") version "<version>"
}
```

Or using the `libs.versions.toml` version catalog:

```toml
[plugins]
kmp-flavorizr = { id = "ly.com.tahaben.kmp-flavorizr", version = "<version>" }
```

```kotlin
plugins {
    alias(libs.plugins.kmp.flavorizr)
}
```

## Usage

### Basic configuration

```kotlin
kmpFlavorizr {
    flavor("development") {
        app {
            name = "My App Dev"
        }
        android {
            applicationId = "com.example.myapp.dev"
        }
        ios {
            bundleId = "com.example.myapp.dev"
        }
    }

    flavor("production") {
        app {
            name = "My App"
        }
        android {
            applicationId = "com.example.myapp"
        }
        ios {
            bundleId = "com.example.myapp"
        }
    }
}
```

### Running

```bash
# Run the full pipeline (generates Gradle files, xcconfigs, modifies manifests, etc.)
./gradlew flavorizr

# The Flavor enum is generated automatically during compilation — no manual step needed
./gradlew build
```

The `flavorizr` task is meant to be run once during setup and again whenever you add or change flavors. The generated
files should be committed to version control.

### Generated Flavor enum

The plugin generates a `Flavor.kt` file into your `commonMain` source set:

```kotlin
enum class Flavor(val appName: String) {
    DEVELOPMENT(appName = "My App Dev"),
    PRODUCTION(appName = "My App");

    companion object {
        fun fromName(name: String): Flavor =
            entries.first { it.name.equals(name, ignoreCase = true) }
    }
}
```

You can use this in shared code to branch on the current flavor.

### Reading the current flavor in Gradle

The plugin provides an extension function on `Project` that detect the active flavor at configuration time. They read
the flavor list from your `kmpFlavorizr { }` DSL, so there is nothing to hardcode.

```kotlin
// Checks (in order):
//   1. Gradle task name  (assembleProdRelease → "prod")
//   2. FLAVOR environment variable
// Returns null if neither resolves to a configured flavor.
val flavor: String? = currentBuildFlavorOrNull()
```

The function is available directly in any `build.gradle.kts` that applies the plugin — no import or extra setup needed.

## Configuration Reference

### Global settings

```kotlin
kmpFlavorizr {
    // Path to the iOS project directory, relative to this module (default: "../iosApp")
    iosProjectPath = "../iosApp"

    // Name of the Xcode target for the iOS app (default: "iosApp")
    iosTargetName = "iosApp"

    // Package name for the generated Flavor enum (default: project group)
    flavorEnumPackage = "com.example.myapp"

    // Global config applied to all flavors
    app {
        android {
            flavorDimensions = "environment" // default: "flavor-type"
            resValues {
                resValue("shared_key", "string", "shared_value")
            }
            buildConfigFields {
                buildConfigField("SHARED_FLAG", "boolean", "true")
            }
        }
        ios {
            buildSettings {
                put("SWIFT_VERSION", "5.0")
            }
        }
        desktop {
            // Off by default. When true, the plugin writes per-flavor IntelliJ/Android
            // Studio Gradle run configurations under .idea/runConfigurations/
            generateRunConfigurations = true
            runTask = "run" // default: "run"
        }
        web {
            generateRunConfigurations = true
            runTask = "wasmJsBrowserDevelopmentRun" // default: "wasmJsBrowserDevelopmentRun"
        }
    }
}
```

### Non-mobile run configurations (desktop & web)

KMP desktop and web targets don't have a native product-flavor concept. Setting
`generateRunConfigurations = true` under `app { desktop { } }` or `app { web { } }`
makes the plugin emit one IntelliJ Gradle run configuration per flavor into
`.idea/runConfigurations/`. Each generated configuration invokes the target's
`runTask` and exports the active flavor as an environment variable using the key
configured via `envVariableKey` (default `FLAVOR`).

For example, with the snippet above and two flavors `dev` / `prod`, you get:

- `.idea/runConfigurations/Desktop_dev.xml` → runs `./gradlew run` with `FLAVOR=dev`
- `.idea/runConfigurations/Desktop_prod.xml` → runs `./gradlew run` with `FLAVOR=prod`
- `.idea/runConfigurations/Web_dev.xml` → runs `./gradlew wasmJsBrowserDevelopmentRun` with `FLAVOR=dev`
- `.idea/runConfigurations/Web_prod.xml` → runs `./gradlew wasmJsBrowserDevelopmentRun` with `FLAVOR=prod`

Override `runTask` when your target lives in a sub-module, e.g.
`runTask = ":composeApp:run"`. Read the active flavor from your shared code via
`System.getenv("FLAVOR")` (JVM/desktop) or the equivalent for your web target.

### Per-flavor Android settings

```kotlin
flavor("staging") {
    app {
        name = "My App Staging"
        icon = "icons/staging_icon.png" // shared icon for all platforms
    }
    android {
        applicationId = "com.example.myapp.staging"
        icon = "icons/android_staging.png"             // overrides app.icon for Android
        roundIcon = "icons/android_staging_round.png"  // Android-only — written to mipmap-*/ic_launcher_round.png

        customConfig {
            put("versionCode", 100)
            put("minSdkVersion", 23)
        }
        resValues {
            resValue("api_host", "string", "staging.api.example.com")
        }
        buildConfigFields {
            buildConfigField("API_URL", "String", "https://staging.api.example.com")
        }
        adaptiveIcon {
            foreground = "icons/fg.png"
            background = "icons/bg.png"
            monochrome = "icons/mono.png" // optional, Android 13+
        }
        firebase {
            config = "firebase/staging/google-services.json"
        }
    }
}
```

### Per-flavor iOS settings

```kotlin
flavor("staging") {
    ios {
        bundleId = "com.example.myapp.staging"
        icon = "icons/ios_staging.png"

        buildSettings {
            put("CODE_SIGN_IDENTITY", "iPhone Distribution")
            put("DEVELOPMENT_TEAM", "ABCDE12345")
        }
        variables {
            variable("API_BASE_URL", "https://staging.api.example.com")
            variable("LOG_LEVEL", "verbose", target = "debug") // only in Debug xcconfig
        }
        firebase {
            config = "firebase/staging/GoogleService-Info.plist"
        }
    }
}
```

### Per-flavor constants

KMP flavorizer does not directly support a per-flavor constant. However, you can combine it with something like
the [BuildKonfig]("https://github.com/yshrsmz/BuildKonfig") plugin to introduce the per-flavor constants for each
flavor, you can use the `currentBuildFlavorOrNull()` function to get the current flavor

Example:

```kotlin

project.extra.set("buildkonfig.flavor", currentBuildFlavorOrNull() ?: "development")

buildkonfig {
    packageName = "ly.com.tahaben.demoflavorizr"
    objectName = "BuildKonfig"
    defaultConfigs {
        buildConfigField(FieldSpec.Type.STRING, "BASE_URL", "https://api-dev.test.com/")
    }
    defaultConfigs("production") {
        buildConfigField(FieldSpec.Type.STRING, "BASE_URL", "https://api.test.com/")
    }
    defaultConfigs("development") {
        buildConfigField(FieldSpec.Type.STRING, "BASE_URL", "https://api-dev.test.com/")
    }
}

```

### Value merging

For Android `resValues` and `buildConfigFields`, values are merged in this order (later entries win on key conflicts):

1. Auto-generated `app_name` from `flavor.app.name`
2. Global values from the top-level `app { android { } }` block
3. Per-flavor values

For iOS `buildSettings`, the same merging applies: global settings first, then per-flavor overrides.

## What gets generated

After running `./gradlew flavorizr`, the plugin creates or modifies these files:

**Android:**

- `flavorizr.gradle.kts` — product flavor definitions (applied into your build.gradle.kts)
- `src/{flavor}/res/mipmap-*/ic_launcher.png` — resized launcher icons
- `src/{flavor}/res/mipmap-*/ic_launcher_round.png` — resized round launcher icons (if `roundIcon` is configured)
- `src/{flavor}/res/mipmap-anydpi-v26/ic_launcher.xml` — adaptive icon XML (if configured)
- `AndroidManifest.xml` — `android:label` set to `@string/app_name`; `android:roundIcon="@mipmap/ic_launcher_round"`
  injected when any flavor configures a round icon and the attribute is missing

**iOS:**

- `Runner/{flavor}Debug.xcconfig`, `Runner/{flavor}Profile.xcconfig`, `Runner/{flavor}Release.xcconfig`
- Xcode build configurations and schemes (via `xcodeproj` gem)
- `Info.plist` — bundle name/display name set to build variable references
- `Podfile` — flavor configuration mappings
- `Assets.xcassets/{flavor}AppIcon.appiconset/` — resized icons with `Contents.json`

**Shared:**

- `build/generated/kmpFlavorizr/commonMain/kotlin/.../Flavor.kt`

**Non-mobile run configurations** (only when `app { desktop }` / `app { web }` enables `generateRunConfigurations`):

- `.idea/runConfigurations/Desktop_{flavor}.xml`
- `.idea/runConfigurations/Web_{flavor}.xml`

## Customization

### Skipping platforms

If a flavor only has an `android { }` block and no `ios { }` block, all iOS processors are skipped for that flavor (and
vice versa). You can define Android-only or iOS-only flavors freely.

### Custom iOS project path and target name

KMP projects have varying directory structures. If your Xcode project isn't at `../iosApp` relative to the module
applying the plugin, or your Xcode target has a different name, configure both:

```kotlin
kmpFlavorizr {
    iosProjectPath = "../ios"       // directory containing the .xcodeproj
    iosTargetName = "MyApp"         // name of the Xcode target (default: "iosApp")
}
```

The `iosTargetName` must match the target name in your `.xcodeproj`. You can find it in Xcode under the project
navigator, or by running `xcodebuild -list` in the iOS project directory.

### Flavor enum package

By default, the generated `Flavor` enum uses your project's `group` as its package. Override it with:

```kotlin
kmpFlavorizr {
    flavorEnumPackage = "com.example.myapp.config"
}
```

## TODO

- [ ] Support for AGP 9.0+

## License

Apache License 2.0

## Contributing

Contributions are welcome, please open a branch and submit a PR.

KMP Flavorizr is inspired by [Flutter Flavorizr](https://pub.dev/packages/flutter_flavorizr)
and [psuzn's article](https://sujanpoudel.me/blogs/managing-configurations-for-different-environments-in-kmp/)