plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

repositories {
    mavenCentral()
    google()
}

dependencies {
    implementation("com.squareup:kotlinpoet:2.0.0")
    implementation("dev.ohs.fhir:fhir-model:1.0.0-beta03")
    implementation("dev.ohs.fhir:fhir-path:1.0.0-beta02")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
}
