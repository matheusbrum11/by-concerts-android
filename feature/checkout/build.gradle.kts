plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.byconcerts.feature.checkout"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures { compose = true }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
}

/** Ver :feature:events — unit tests de release desligados (deps debug-only). */
androidComponents {
    beforeVariants(selector().withBuildType("release")) { variant ->
        variant.enableUnitTest = false
    }
}

// :feature:checkout — fluxo de pagamento + comprovante (com QR via MnsQrCode do
// Design System). MVI + Compose. Depende do :payment para orquestrar o gateway
// (gateway.pay() → PaymentResult → use case de conciliação/idempotência).
dependencies {
    implementation(project(":domain"))
    implementation(project(":core:common"))
    implementation(project(":core:ui"))
    implementation(project(":payment"))

    implementation(libs.mns.design.system)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui)
    implementation(libs.compose.material.icons.core)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.kotlinx.coroutines.android)

    implementation(platform(libs.koin.bom))
    implementation(libs.koin.androidx.compose)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.compose.ui.test.junit4)
    testImplementation(libs.androidx.test.ext.junit)
    debugImplementation(libs.compose.ui.test.manifest)
}
