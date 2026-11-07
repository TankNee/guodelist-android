import java.util.Properties
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification
import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("jacoco")
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}

// Local-only build configuration. `local.properties` is git-ignored, so a
// developer can fill in their own server/repository without committing it.
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
val dotenvProperties = Properties().apply {
    val file = rootProject.file(".env")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun buildConfigValue(envName: String, propertyName: String, fallback: String): String {
    System.getenv(envName)?.takeIf { it.isNotBlank() }?.let { return it.trim() }
    providers.gradleProperty(propertyName).orNull?.takeIf { it.isNotBlank() }?.let { return it.trim() }
    localProperties.getProperty(propertyName)?.takeIf { it.isNotBlank() }?.let { return it.trim() }
    dotenvProperties.getProperty(propertyName)?.takeIf { it.isNotBlank() }?.let { return it.trim() }
    return fallback
}

// Environment variables / local.properties:
//   GUODELIST_SERVER=https://api.your-domain.example
//   GUODELIST_GITHUB_REPO=TankNee/guodelist-android
//   GUODELIST_UPDATE_SOURCE=github|api
val defaultServer = buildConfigValue(
    "GUODELIST_SERVER",
    "GUODELIST_SERVER",
    "https://api.example.com",
)
val githubRepo = buildConfigValue(
    "GUODELIST_GITHUB_REPO",
    "GUODELIST_GITHUB_REPO",
    "TankNee/guodelist-android",
)
val updateSource = buildConfigValue(
    "GUODELIST_UPDATE_SOURCE",
    "GUODELIST_UPDATE_SOURCE",
    "github",
)

// Release builds are public artifacts: never bake a private server address
// into them unless the builder explicitly opts in for a private distribution.
val isReleaseBuild = gradle.startParameter.taskNames.any {
    it.contains("release", ignoreCase = true)
}
val allowReleaseConfig = providers.gradleProperty("GUODELIST_ALLOW_RELEASE_CONFIG")
    .orNull
    ?.equals("true", ignoreCase = true) == true
val compiledServer = if (isReleaseBuild && !allowReleaseConfig) {
    "https://api.example.com"
} else {
    defaultServer
}

android {
    namespace = "cn.neeto.guodelist"
    compileSdk = 35

    defaultConfig {
        applicationId = "cn.neeto.guodelist"
        minSdk = 26
        targetSdk = 35
        versionCode = 26
        versionName = "0.4.21"
        vectorDrawables.useSupportLibrary = true
        resourceConfigurations += listOf("en", "zh")
        buildConfigField("String", "DEFAULT_SERVER", "\"$compiledServer\"")
        buildConfigField("String", "GITHUB_REPO", "\"$githubRepo\"")
        buildConfigField("String", "UPDATE_SOURCE", "\"$updateSource\"")
    }

    signingConfigs {
        if (keystorePropertiesFile.exists()) {
            create("release") {
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isDebuggable = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf("-opt-in=kotlin.RequiresOptIn")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources.excludes += setOf(
            "/META-INF/{AL2.0,LGPL2.1}",
            "/META-INF/DEPENDENCIES",
            "/META-INF/LICENSE*",
            "/META-INF/NOTICE*",
        )
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
}

jacoco {
    toolVersion = "0.8.12"
}

tasks.withType<Test>().configureEach {
    extensions.configure<JacocoTaskExtension> {
        isIncludeNoLocationClasses = true
        excludes = listOf("jdk.internal.*")
    }
}

val taskOpsClasses = layout.buildDirectory.dir("tmp/kotlin-classes/debug").map { dir ->
    fileTree(dir) { include("cn/neeto/guodelist/core/data/TaskOps*.class") }
}
val unitTestExecutionData = layout.buildDirectory.file("jacoco/testDebugUnitTest.exec")

tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn("testDebugUnitTest")
    classDirectories.setFrom(taskOpsClasses)
    sourceDirectories.setFrom(files("src/main/java"))
    executionData.setFrom(unitTestExecutionData)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

tasks.register<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    dependsOn("testDebugUnitTest")
    classDirectories.setFrom(taskOpsClasses)
    sourceDirectories.setFrom(files("src/main/java"))
    executionData.setFrom(unitTestExecutionData)
    violationRules {
        rule {
            limit {
                counter = "LINE"
                minimum = "0.80".toBigDecimal()
            }
        }
    }
}

tasks.named("check") {
    dependsOn("jacocoTestCoverageVerification")
}
