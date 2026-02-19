plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.comprafacil"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.comprafacil"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

tasks.register<Exec>("cloneWeb") {
    commandLine("git", "clone", "https://github.com/deivid22srk/Compra-Facil-web.git", "../web")
    // Only clone if it doesn't exist
    onlyIf { !file("../web").exists() }
}

tasks.register("fixWebIndex") {
    dependsOn("cloneWeb")
    doLast {
        val indexFile = file("../web/index.html")
        if (indexFile.exists()) {
            val content = indexFile.readText()
            if (!content.contains("index.tsx")) {
                val newContent = content.replace(
                    "</div>",
                    "</div>\n    <script type=\"module\" src=\"/index.tsx\"></script>"
                )
                indexFile.writeText(newContent)
            }
        }
    }
}

tasks.register<Exec>("installWebDeps") {
    dependsOn("fixWebIndex")
    workingDir = file("../web")
    commandLine("npm", "install")
}

tasks.register<Exec>("buildWeb") {
    dependsOn("installWebDeps")
    workingDir = file("../web")
    commandLine("npm", "run", "build")
}

tasks.register<Copy>("copyWebArtifacts") {
    dependsOn("buildWeb")
    from("../web/dist")
    into("src/main/assets/www")
}

tasks.named("preBuild") {
    dependsOn("copyWebArtifacts")
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
}
