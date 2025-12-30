import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.kotlinSerialization)
}
configurations.all {
    resolutionStrategy {
        force(
            "org.jetbrains.kotlinx:kotlinx-datetime:0.6.1",
            "org.jetbrains.kotlinx:kotlinx-datetime-jvm:0.6.1"
        )
    }
}

kotlin {
    androidTarget {
        compilerOptions { jvmTarget.set(JvmTarget.JVM_11) }
    }

    // ✅ Target Desktop explícito (Gradle creará desktopMain/desktopTest)
    jvm("desktop") {
        compilations.all {
            compilerOptions.configure {
                jvmTarget.set(JvmTarget.JVM_11)
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation(compose.components.resources)
                implementation(compose.components.uiToolingPreview)
                implementation(compose.materialIconsExtended)

                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.datetime) // ok en common
            }
        }

        val commonTest by getting {
            dependencies { implementation(libs.kotlin.test) }
        }

        val androidMain by getting {
            dependencies {
                implementation(compose.preview)
                implementation(libs.androidx.activity.compose)
                implementation(libs.androidx.activity.ktx)

                implementation(libs.androidx.lifecycle.runtimeKtx)
                implementation(libs.androidx.lifecycle.runtimeCompose)
                implementation(libs.androidx.lifecycle.viewmodelCompose)
            }
        }

        /**
         * ✅ Mantengo tu código en src/jvmMain SIN mover nada.
         * Creamos un sourceSet intermedio "jvmMain" que apunta a src/jvmMain.
         */
        val jvmMain by creating {
            dependsOn(commonMain)
            kotlin.srcDir("src/jvmMain/kotlin")
            resources.srcDir("src/jvmMain/resources")

            dependencies {
                // Si tenías cosas comunes a JVM, ponlas aquí (opcional)
                implementation(libs.kotlinx.coroutinesSwing)
                implementation(libs.kotlinx.datetime)
            }
        }

        /**
         * ✅ El Desktop real compila desde desktopMain.
         * Hacemos que desktopMain dependa de jvmMain (tu código),
         * y aquí forzamos el artefacto JVM de datetime.
         */
        val desktopMain by getting {
            dependsOn(jvmMain)
            dependencies {
                implementation(compose.desktop.currentOs)

                // ✅ FORZAR JAR JVM (esto elimina el NoClassDefFoundError de Clock$System)
                implementation("org.jetbrains.kotlinx:kotlinx-datetime-jvm:0.6.1")
            }
        }

        val desktopTest by getting {
            dependsOn(commonTest)
        }
    }
}

android {
    namespace = "org.example.project"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "org.example.project"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }

    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    }

    buildTypes {
        getByName("release") { isMinifyEnabled = false }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
}

compose.desktop {
    application {
        mainClass = "org.example.project.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Msi)
            packageName = "BolsaCotarelo"
            packageVersion = "1.0.0"
        }
    }
}
