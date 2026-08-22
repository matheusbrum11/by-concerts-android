plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(17)
}

// :domain — entidades, use cases, interfaces de repositório e o contrato de
// pagamento (modelos PaymentResult/PaymentRequest/PaymentCode). Kotlin PURO:
// nenhuma dependência de Android. A interface PaymentGateway vive no :payment;
// o :domain só define os MODELOS de domínio que ela troca.
dependencies {
    implementation(project(":core:common"))
    implementation(libs.kotlinx.coroutines.core)

    // Koin-core (puro JVM) só para declarar o grafo de use cases do domínio.
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
