plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.byconcerts.feature.events"
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

/**
 * Unit tests da variante `release` desligados de propósito: rodariam o mesmo
 * código-fonte do `debug`, porém sem as dependências `debugImplementation` que
 * a suíte de Compose exige (`ui-test-manifest`) — falhariam por configuração,
 * não por regressão. Mesma decisão adotada no design system.
 */
androidComponents {
    beforeVariants(selector().withBuildType("release")) { variant ->
        variant.enableUnitTest = false
    }
}

// :feature:events — listagem e detalhe/seleção de quantidade. MVI + Compose.
// Todo visual passa pelo Design System (mns-design-system), nunca Material cru.
dependencies {
    implementation(project(":domain"))
    implementation(project(":core:common"))
    implementation(project(":core:ui"))

    // Design System — resolvido pelo composite build (includeBuild no settings).
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
    // Necessário para createAndroidComposeRule<ComponentActivity> sob Robolectric.
    testImplementation(libs.androidx.activity.compose)
    debugImplementation(libs.compose.ui.test.manifest)
}
