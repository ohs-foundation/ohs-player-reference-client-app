import com.diffplug.gradle.spotless.SpotlessExtension

plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.androidKotlinMultiplatformLibrary) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.composeHotReload) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.spotless)
}

allprojects {
    apply(plugin = rootProject.libs.plugins.spotless.get().pluginId)

    configure<SpotlessExtension> {
        val ktfmtVersion = rootProject.libs.versions.ktfmt.get()
        val licenseHeaderFile = rootProject.file("license-header.txt")

        kotlin {
            target("src/**/*.kt")
            ktfmt(ktfmtVersion).googleStyle()
            licenseHeaderFile(licenseHeaderFile)
        }
        kotlinGradle {
            target("*.gradle.kts")
            ktfmt(ktfmtVersion).googleStyle()
            licenseHeaderFile(licenseHeaderFile, "(^(?![\\/ ]\\*).*$)")
        }
    }
}
