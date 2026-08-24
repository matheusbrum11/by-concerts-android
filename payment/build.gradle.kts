import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
}

// Leitura MANUAL do local.properties (não usamos gradleLocalProperties, cuja
// assinatura mudou entre versões do AGP e quebra o sync). As credenciais Cielo
// ficam fora do versionamento e chegam ao código via BuildConfig.
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) FileInputStream(f).use { load(it) }
}
val cieloClientId: String = localProps.getProperty("CIELO_CLIENT_ID") ?: ""
val cieloAccessToken: String = localProps.getProperty("CIELO_ACCESS_TOKEN") ?: ""
// Opcional: só é usado em cenários multi-estabelecimento.
val cieloMerchantCode: String = localProps.getProperty("CIELO_MERCHANT_CODE") ?: ""

android {
    namespace = "com.byconcerts.payment"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "CIELO_CLIENT_ID", "\"$cieloClientId\"")
        buildConfigField("String", "CIELO_ACCESS_TOKEN", "\"$cieloAccessToken\"")
        buildConfigField("String", "CIELO_MERCHANT_CODE", "\"$cieloMerchantCode\"")
    }

    buildFeatures { buildConfig = true }

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

// :payment — abstração PaymentGateway + integração Cielo via Deeplink, isolada
// do resto do app. Depende do :domain só para trocar os MODELOS de domínio
// (PaymentRequest/PaymentResult). Nenhuma feature conhece detalhes da Cielo.
dependencies {
    implementation(project(":domain"))
    implementation(project(":core:common"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.koin.android)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.ext.junit)
}
