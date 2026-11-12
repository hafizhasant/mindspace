plugins {
    id("com.android.application")
}
android {
    namespace = "com.local.chat"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.local.chat"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }
    buildTypes {
        release { isMinifyEnabled = false }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
