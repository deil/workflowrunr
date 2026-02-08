import com.diffplug.gradle.spotless.SpotlessExtension
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.compile.JavaCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "2.7.18" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
    kotlin("jvm") apply false
    kotlin("plugin.lombok") apply false
    id("org.jetbrains.kotlin.plugin.spring") apply false
    id("io.freefair.lombok") version "8.13.1" apply false
    id("com.diffplug.spotless") version "7.0.2" apply false
}

group = "club.kosya"
version = "0.0.1-SNAPSHOT"

subprojects {
    apply(plugin = "com.diffplug.spotless")

    repositories {
        mavenCentral()
    }

    pluginManager.withPlugin("java") {
        extensions.configure<JavaPluginExtension> {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(21))
            }
        }

        configurations.named("compileOnly") {
            extendsFrom(configurations.named("annotationProcessor").get())
        }

        dependencies {
            add("annotationProcessor", "org.projectlombok:lombok")
            add("compileOnly", "org.projectlombok:lombok")
        }

        tasks.withType<JavaCompile>().configureEach {
            options.compilerArgs.add("-parameters")
        }
    }

    pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
        tasks.withType<KotlinCompile>().configureEach {
            compilerOptions {
                javaParameters.set(true)
            }
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    pluginManager.withPlugin("com.diffplug.spotless") {
        extensions.configure<SpotlessExtension> {
            java {
                palantirJavaFormat()
                removeUnusedImports()
            }
            kotlin {
                ktlint().editorConfigOverride(
                    mapOf(
                        "ktlint_standard_no-wildcard-imports" to "disabled",
                    ),
                )
                trimTrailingWhitespace()
            }
        }
    }
}
