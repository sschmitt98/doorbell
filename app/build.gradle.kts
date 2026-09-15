group = "foo.schmitt.doorbell"
version = "0.0.0-SNAPSHOT"

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.versions)
    alias(libs.plugins.ksp)
    alias(libs.plugins.telegram.bot)
    alias(libs.plugins.jib)

    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.arrow.core)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.core.jvm)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.forwarded.header)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.websockets)
    implementation(libs.ktor.server.metrics.micrometer)
    implementation(libs.ktor.server.metrics.micrometer.jvm)
    implementation(libs.micrometer.registry.prometheus)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.logback)
    implementation(libs.slf4j.api)

    testImplementation(libs.assertk.jvm)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.ktor.client.content.negotiation)
}

testing {
    suites {
        val test = named<JvmTestSuite>("test") {
            useJUnitJupiter("6.0.1")
        }
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

application {
    mainClass = "foo.schmitt.doorbell.AppKt"
}

jib {
    container {
        ports = listOf("8080")
    }
}

// exclude jib from Gradle configuration cache
// see: https://github.com/GoogleContainerTools/jib/issues/3132
tasks.filter { it.name in setOf("jibDockerBuild", "jibBuildTar", "jib") }.onEach {
    it.notCompatibleWithConfigurationCache("Jib is not compatible with configuration cache")
}
