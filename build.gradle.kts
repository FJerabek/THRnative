import org.gradle.kotlin.dsl.support.compileKotlinScriptModuleTo

plugins {
    kotlin("multiplatform") version "1.4.21"
    kotlin("plugin.serialization") version "1.4.21"
}
group = "cz.fjerabek"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    jcenter()
    maven {
        url = uri("https://dl.bintray.com/badoo/maven")
    }
}

kotlin {
    val hostOs = System.getProperty("os.name")
    val nativeTarget = when {
        hostOs == "Linux" && project.hasProperty("rpi") -> linuxArm32Hfp("native") {
            val main by compilations.getting
            val bluez by main.cinterops.creating
        }
        hostOs == "Linux" -> linuxX64("native") {
            val main by compilations.getting
            val bluez by main.cinterops.creating
        }
        else -> throw GradleException("Host OS is not supported")
    }

    nativeTarget.apply {
        binaries {
            executable {
                entryPoint = "cz.fjerabek.thr.main"
            }
        }
    }
    sourceSets {
        val nativeMain by getting {
            dependencies {
                implementation(kotlin("stdlib", org.jetbrains.kotlin.config.KotlinCompilerVersion.VERSION))
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.0.0-RC")
//                implementation("org.kodein.di:kodein-di:7.1.0")
                implementation("com.badoo.reaktive:reaktive:1.1.19")

            }
        }
        val nativeTest by getting
    }
}
