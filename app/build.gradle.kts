plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose.compiler)
    id("kotlin-parcelize")
    alias(libs.plugins.hilt.android)
    id("com.google.gms.google-services")
    id("androidx.navigation.safeargs.kotlin")
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
    id("org.openapi.generator") // Version is managed by pluginManagement or project level
    id("com.google.firebase.crashlytics")
    id("com.google.firebase.firebase-perf")
}

// Common versions
val composeVersion = "1.5.4"
val firebaseBomVersion = "32.7.0"
val lifecycleVersion = "2.6.2"

android {
    namespace = "dev.aurakai.auraframefx"
    compileSdk = 36

    defaultConfig {
        applicationId = "dev.aurakai.auraframefx"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        multiDexEnabled = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlinOptions {
        jvmTarget = "21"
        freeCompilerArgs += listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=androidx.compose.animation.ExperimentalAnimationApi",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            "-opt-in=androidx.compose.ui.ExperimentalComposeUiApi",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=kotlinx.coroutines.FlowPreview",
            "-opt-in=com.google.accompanist.pager.ExperimentalPagerApi",
            "-Xjvm-default=all"
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
        viewBinding = true
        aidl = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = composeVersion
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildToolsVersion = "36.0.3"
}

// OpenAPI Generator configuration
val openApiGeneratedDir = layout.buildDirectory.dir("generated/openapi")

tasks.register<org.openapitools.generator.gradle.plugin.tasks.GenerateTask>("generateKotlinClient") {
    group = "openapi tools"
    generatorName.set("kotlin")
    inputSpec.set("${project.rootDir}/api-spec/aura-framefx-api.yaml")
    outputDir.set("${openApiGeneratedDir.get().asFile}/kotlin")
    apiPackage.set("dev.aurakai.auraframefx.api.kotlin.api")
    modelPackage.set("dev.aurakai.auraframefx.api.kotlin.model")
    configOptions.set(
        mapOf(
            "dateLibrary" to "java8",
            "serializationLibrary" to "kotlinx_serialization",
            "useCoroutines" to "true",
            "enumPropertyNaming" to "UPPERCASE",
            "serializableModel" to "true",
            "library" to "jvm-retrofit2"
        )
    )
    ignoreFileOverride.set("${project.rootDir}/.openapi-generator-ignore")
}

tasks.register<org.openapitools.generator.gradle.plugin.tasks.GenerateTask>("generateTypescriptClient") {
    group = "openapi tools"
    generatorName.set("typescript-axios")
    inputSpec.set("${project.rootDir}/api-spec/aura-framefx-api.yaml")
    outputDir.set("${openApiGeneratedDir.get().asFile}/typescript")
    configOptions.set(
        mapOf(
            "supportsES6" to "true",
            "npmName" to "auraframefx-api-client-ts",
            "npmVersion" to "1.0.0",
            "stringEnums" to "true"
        )
    )
    ignoreFileOverride.set("${project.rootDir}/.openapi-generator-ignore")
}

tasks.register<org.openapitools.generator.gradle.plugin.tasks.GenerateTask>("generateJavaClient") {
    group = "openapi tools"
    generatorName.set("java")
    inputSpec.set("${project.rootDir}/api-spec/aura-framefx-api.yaml")
    outputDir.set("${openApiGeneratedDir.get().asFile}/java")
    apiPackage.set("dev.aurakai.auraframefx.api.java.api")
    modelPackage.set("dev.aurakai.auraframefx.api.java.model")
    configOptions.set(
        mapOf(
            "dateLibrary" to "java8",
            "library" to "retrofit2",
            "useJakartaEe" to "false", // For Android compatibility
            "useBeanValidation" to "false"
        )
    )
    ignoreFileOverride.set("${project.rootDir}/.openapi-generator-ignore")
}

tasks.register<org.openapitools.generator.gradle.plugin.tasks.GenerateTask>("generatePythonClient") {
    group = "openapi tools"
    generatorName.set("python")
    inputSpec.set("${project.rootDir}/api-spec/aura-framefx-api.yaml")
    outputDir.set("${openApiGeneratedDir.get().asFile}/python")
    configOptions.set(
        mapOf(
            "packageName" to "auraframefx_api_client_py",
            "projectName" to "auraframefx-api-client-py",
            "packageVersion" to "1.0.0"
        )
    )
    ignoreFileOverride.set("${project.rootDir}/.openapi-generator-ignore")
}

// Add generated Kotlin sources to the main source set
afterEvaluate {
    android.sourceSets.getByName("main") {
        java.srcDir("${openApiGeneratedDir.get()}/kotlin/src/main/kotlin")
    }
}

val generateOpenApiClients = tasks.register("generateOpenApiClients") {
    group = "openapi tools"
    description = "Generates all OpenAPI clients (Kotlin, TypeScript, Java, Python)"
    dependsOn("generateKotlinClient")
    dependsOn("generateTypescriptClient")
    dependsOn("generateJavaClient")
    dependsOn("generatePythonClient")
}

// Ensure the OpenAPI generation happens before compilation
tasks.named("preBuild") {
    dependsOn(generateOpenApiClients)
}

// Xposed framework configurations
val xposedApiJar = files("${project.rootDir}/Libs/api-82.jar") // Corrected path
val xposedBridgeJar = files("${project.rootDir}/Libs/xposed-api-82.jar") // Corrected path and file
val xposedCompileOnly = configurations.create("xposedCompileOnly")

dependencies {
    // Core Android dependencies
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:$lifecycleVersion")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    
    // Protocol Buffers and Netty
    implementation("com.google.protobuf:protobuf-java:3.25.5")
    implementation("commons-io:commons-io:2.14.0")
    implementation("io.netty:netty-codec-http2:4.1.124.Final")
    implementation("io.netty:netty-handler:4.1.118.Final")
    implementation("org.bouncycastle:bcprov-jdk18on:1.78")
    implementation("io.netty:netty-common:4.1.118.Final")
    implementation("org.apache.commons:commons-compress:1.26.0")
    implementation("com.google.guava:guava:32.1.3-android")
    implementation("io.netty:netty-codec-http:4.1.118.Final")

    // Kotlinx Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-xml:1.6.0")

    // Dagger Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.work)

    // Kotlin Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

    // Compose
    val composeBom = platform("androidx.compose:compose-bom:$composeVersion")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material3:material3-window-size-class")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.runtime:runtime")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Google Cloud
    implementation(platform("com.google.cloud:libraries-bom:26.25.0"))
    implementation("com.google.cloud:google-cloud-generativeai")

    // Room
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // Work Manager
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // UI Components
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("io.coil-kt:coil-compose:2.5.0")
    implementation("com.google.accompanist:accompanist-systemuicontroller:0.32.0")
    implementation("com.google.accompanist:accompanist-permissions:0.32.0")

    // Compose Glance
    implementation("androidx.glance:glance-appwidget:1.0.0")
    implementation("androidx.glance:glance-compose:1.0.0")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:$firebaseBomVersion"))
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")
    implementation("com.google.firebase:firebase-crashlytics-ktx")
    implementation("com.google.firebase:firebase-ml-modeldownloader-ktx")
    implementation("com.google.mlkit:language-id:17.0.5")
    implementation("com.google.mlkit:translate:17.0.2")
    implementation("org.tensorflow:tensorflow-lite:2.14.0")
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4")
    implementation("org.tensorflow:tensorflow-lite-metadata:0.4.4")
    implementation("org.tensorflow:tensorflow-lite-task-vision:0.4.4")
    implementation("org.tensorflow:tensorflow-lite-task-text:0.4.4")

    // Accompanist
    implementation("com.google.accompanist:accompanist-pager:0.32.0")
    implementation("com.google.accompanist:accompanist-flowlayout:0.32.0")
    implementation("com.google.accompanist:accompanist-navigation-animation:0.32.0")
    implementation("com.google.accompanist:accompanist-swiperefresh:0.32.0")
    implementation("com.google.accompanist:accompanist-webview:0.32.0")
    implementation("com.google.accompanist:accompanist-pager-indicators:0.32.0")
    implementation("com.google.accompanist:accompanist-placeholder-material:0.32.0")
    implementation("com.google.accompanist:accompanist-navigation-material:0.32.0")
    implementation("com.google.accompanist:accompanist-systemuicontroller:0.32.0")

    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")

    // Xposed dependencies - using local JARs
    compileOnly(xposedApiJar)
    compileOnly(xposedBridgeJar)
    xposedCompileOnly("org.lsposed.hiddenapibypass:hiddenapibypass:6.1") {
        exclude(group = "de.robv.android.xposed", module = "api")
    }
    xposedCompileOnly("org.lsposed:libxposed:82")
    xposedCompileOnly("org.lsposed:libxposed:82:sources") // For development only

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("org.mockito:mockito-core:5.7.0")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("app.cash.turbine:turbine:1.0.0")
    testImplementation("com.google.truth:truth:1.1.5")

    // AndroidX Test
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test:rules:1.5.0")
    androidTestImplementation("io.mockk:mockk-android:1.13.8")
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.android.compiler)

    // Debug implementations
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Desugar JDK libs for Java 8+ APIs on older Android versions
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
}
