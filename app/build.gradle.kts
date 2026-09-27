plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.felix021.puff"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.felix021.puff"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }

    // puff 专属签名（独立 key，不与其他项目共享）。凭据在机器本地私有仓库
    // ~/.config/puff-browser（gitea puff/config，见 AGENTS.md），可用 PUFF_CONFIG_DIR 重定向。
    val puffConfigDir = System.getenv("PUFF_CONFIG_DIR")
        ?: "${System.getProperty("user.home")}/.config/puff-browser"
    val puffStoreFile = File(puffConfigDir, "keys/puff-debug.keystore")
    val puffStorePassword = File(puffConfigDir, "keys/debug.password").readText().trim()

    signingConfigs {
        create("puff") {
            storeFile = puffStoreFile
            storePassword = puffStorePassword
            keyAlias = "puff"
            keyPassword = puffStorePassword
        }
    }

    buildTypes {
        debug {
            // 调试包独立包名 + 桌面名 dev 后缀，与正式版并存互不影响
            applicationIdSuffix = ".dev"
            signingConfig = signingConfigs.getByName("puff")
        }
        release {
            isMinifyEnabled = false
            // 个人应用：release 同签名，便于直接分发安装
            signingConfig = signingConfigs.getByName("puff")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation(platform("androidx.compose:compose-bom:2024.09.03"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.webkit:webkit:1.12.1")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
