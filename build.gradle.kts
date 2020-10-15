plugins {
    kotlin("multiplatform") version "1.4.10"
    kotlin("plugin.serialization") version "1.4.10"
}
group = "me.fjerabek"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    jcenter()
    maven {
        url = uri("https://dl.bintray.com/badoo/maven")
    }
}
kotlin {
    val targetOs = System.getProperty("target")
    val hostOs = System.getProperty("os.name")
    val isMingwX64 = hostOs.startsWith("Windows")
    val nativeTarget = when {
        hostOs == "Mac OS X" -> macosX64("native")
//        hostOs == "Linux" -> linuxArm32Hfp("native") {
//            val main by compilations.getting
//            val bluez by main.cinterops.creating
//        }
        hostOs == "Linux" -> linuxX64("native") {
            val main by compilations.getting
            val bluez by main.cinterops.creating
        }
        isMingwX64 -> mingwX64("native")
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
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
//                implementation( "co.touchlab:stately-common:1.1.0")
//                implementation("co.touchlab:stately-concurrency:1.1.0")
//                implementation("co.touchlab:stately-isolate:1.1.1-a1")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.0.0-RC")
//                implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.0.0-RC")
                implementation("org.kodein.di:kodein-di:7.1.0")
                implementation("com.badoo.reaktive:reaktive:1.1.17")
//                implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:+")

            }
        }
        val nativeTest by getting
    }
}