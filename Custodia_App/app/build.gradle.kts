plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.custodia_app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.example.custodia_app"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        // Se puede cambiar con -PcustodiaApiBaseUrl para usar otro servidor.
        val apiBaseUrl = providers.gradleProperty("custodiaApiBaseUrl")
            .orElse("https://kevinsoft-custodia.onrender.com/api/").get()
        require((apiBaseUrl.startsWith("http://") || apiBaseUrl.startsWith("https://")) && apiBaseUrl.endsWith("/")) {
            "custodiaApiBaseUrl debe ser una URL HTTP(S) terminada en /."
        }
        buildConfigField("String", "API_BASE_URL", "\"${apiBaseUrl.replace("\\", "\\\\").replace("\"", "\\\"")}\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
    // Navegación entre pantallas para Compose
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Retrofit (Para comunicarse con tu API PHP)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // Corrutinas (Para que la app no se congele mientras espera el internet)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // ViewModel para mantener los datos vivos al rotar la pantalla
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

    implementation("io.coil-kt:coil-compose:2.5.0")
}
