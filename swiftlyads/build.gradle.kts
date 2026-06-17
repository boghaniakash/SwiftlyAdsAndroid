import com.vanniktech.maven.publish.SonatypeHost

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.maven.publish)
}

android {
    namespace = "io.github.akashboghani.swiftlyads"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
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

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.activity.compose)

    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.foundation)
    implementation(libs.androidx.ui.tooling.preview)
    debugImplementation(libs.androidx.ui.tooling)

    // Google Mobile Ads (AdMob) + User Messaging Platform (UMP consent)
    api(libs.play.services.ads)
    api(libs.user.messaging.platform)
}

mavenPublishing {
    // Uploads to the Sonatype Central Portal (the current Maven Central pipeline).
    // automaticRelease = false → the deployment is staged so you can review and release it
    // manually from https://central.sonatype.com. Set to true once you trust the pipeline.
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL, automaticRelease = false)
    signAllPublications()

    coordinates(
        groupId = "io.github.akashboghani",
        artifactId = "swiftlyads",
        version = "0.1.0",
    )

    pom {
        name.set("SwiftlyAds")
        description.set(
            "A lightweight, Compose-first wrapper around the Google Mobile Ads SDK (AdMob) " +
                "for Android — banner, interstitial, app open, rewarded, rewarded interstitial " +
                "and native ads with built-in UMP consent, preloading and frequency capping.",
        )
        inceptionYear.set("2026")
        url.set("https://github.com/akashboghani/SwiftlyAdsAndroid")

        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        developers {
            developer {
                id.set("akashboghani")
                name.set("Akash Boghani")
                url.set("https://github.com/akashboghani")
            }
        }
        scm {
            url.set("https://github.com/akashboghani/SwiftlyAdsAndroid")
            connection.set("scm:git:git://github.com/akashboghani/SwiftlyAdsAndroid.git")
            developerConnection.set("scm:git:ssh://git@github.com/akashboghani/SwiftlyAdsAndroid.git")
        }
    }
}
