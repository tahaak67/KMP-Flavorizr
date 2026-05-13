plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-gradle-plugin`
    alias(libs.plugins.vanniktech.mavenPublish)
}

group = "ly.com.tahaben"
version = "1.0.0"

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}

gradlePlugin {
    plugins {
        create("kmpflavorizr") {
            id = "ly.com.tahaben.kmp-flavorizr"
            implementationClass = "ly.com.tahaben.flavorizr.KmpFlavorPlugin"
            displayName = "KMP flavorizr"
            description =
                "A Gradle plugin for configuring build flavors across Android and iOS in Kotlin Multiplatform projects"
        }
    }
}

dependencies {
    implementation(gradleApi())

    testImplementation(libs.junit.jupiter)
    testImplementation(gradleTestKit())
    testImplementation(libs.kotlin.test)
}

tasks.withType<Test> {
    useJUnitPlatform()
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()

    coordinates(group.toString(), "kmp-flavorizr", version.toString())

    pom {
        name = "KMP flavorizr"
        description =
            "A Gradle plugin for configuring build flavors across Android and iOS in Kotlin Multiplatform projects"
        inceptionYear = "2025"
        url = "https://github.com/tahaak67/KMP-Flavorizr"
        licenses {
            license {
                name = "The Apache License, Version 2.0"
                url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
            }
        }
        developers {
            developer {
                id = "tahaak67"
                name = "Taha Banashur"
                url = "https://github.com/tahaak67"
                email = "dev@tahaben.com.ly"
                organization = "Taha Banashur"
                organizationUrl = "https://tahaben.com.ly"
            }
        }
        scm {
            url = "https://github.com/tahaak67/KMP-Flavorizr"
            connection = "scm:git:git://github.com/tahaak67/KMP-Flavorizr.git"
            developerConnection = "scm:git:ssh://github.com/tahaak67/KMP-Flavorizr.git"
        }
    }
}
