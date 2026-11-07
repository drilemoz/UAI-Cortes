plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "br.com.uaicortes"
    compileSdk = 36
    defaultConfig {
        applicationId = "br.com.uaicortes"
        minSdk = 26
        targetSdk = 36
        versionCode = 210
        versionName = "0.1.209"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // Definido pelo runner do GitHub Actions, sem inventar nome de repositório.
        val ciRepository = System.getenv("GITHUB_REPOSITORY").orEmpty()
        val safeRepository = ciRepository.takeIf {
            Regex("[A-Za-z0-9_.-]{1,100}/[A-Za-z0-9_.-]{1,100}").matches(it)
        }.orEmpty()
        buildConfigField("String", "GITHUB_RELEASE_REPOSITORY", "\"$safeRepository\"")
        // IDs públicos, fornecidos no CI. Nunca colocar App Secret no APK.
        val metaAppId = System.getenv("META_APP_ID").orEmpty()
            .takeIf { it.matches(Regex("[0-9]{5,30}")) }.orEmpty()
        val metaClientToken = System.getenv("META_CLIENT_TOKEN").orEmpty()
            .takeIf { it.matches(Regex("[A-Za-z0-9_-]{1,150}")) }.orEmpty()
        buildConfigField("String", "META_APP_ID", "\"$metaAppId\"")
        buildConfigField("String", "META_CLIENT_TOKEN", "\"$metaClientToken\"")
        resValue("string", "facebook_app_id", if (metaAppId.isBlank()) "0" else metaAppId)
        resValue("string", "fb_login_protocol_scheme", "fb${if (metaAppId.isBlank()) "0" else metaAppId}")
        resValue("string", "facebook_client_token", metaClientToken)
        // Preparação TikTok: o SDK exige redirect HTTPS e troca de token num backend.
        // Não ativar login sem domínio verificado e backend seguro.
        val tikTokKey = System.getenv("TIKTOK_CLIENT_KEY").orEmpty()
            .takeIf { it.matches(Regex("[A-Za-z0-9_-]{8,100}")) }.orEmpty()
        buildConfigField("String", "TIKTOK_CLIENT_KEY", "\"$tikTokKey\"")

    }
    signingConfigs {
        create("ciRelease") {
            val store = System.getenv("ANDROID_KEYSTORE_FILE")
            if (!store.isNullOrBlank()) {
                storeFile = file(store)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = if (System.getenv("ANDROID_KEYSTORE_FILE").isNullOrBlank())
                signingConfigs.getByName("debug") else signingConfigs.getByName("ciRelease")
        }
    }
    // Um build Release, cinco APKs diretos para GitHub Releases.
    // Todos usam o mesmo versionCode para permitir trocar de arquitetura/universal
    // sem criar versões artificiais. Não publicamos multi-APK na Play Store.
    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
            isUniversalApk = true
        }
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true; buildConfig = true }
    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
}

dependencies {
    implementation(project(":subtitle-core"))
    val composeBom = platform("androidx.compose:compose-bom:2025.05.01")
    implementation(composeBom)
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.core:core:1.16.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.0")
    implementation("androidx.media3:media3-exoplayer:1.11.1")
    implementation("androidx.media3:media3-ui:1.11.1")
    implementation("androidx.media3:media3-transformer:1.11.1")
    implementation("androidx.media3:media3-effect:1.11.1")
    implementation("androidx.media3:media3-common:1.11.1")
    implementation("androidx.documentfile:documentfile:1.1.0")
    implementation("com.google.android.gms:play-services-auth:22.0.0")
    implementation("com.facebook.android:facebook-login:19.0.0")
    implementation("com.alphacephei:vosk-android:0.3.75")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
    androidTestImplementation("androidx.test:core:1.6.1")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation(composeBom)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
