# SwiftlyAds (Android / Jetpack Compose)

[![Build & Test](https://github.com/boghaniakash/SwiftlyAdsAndroid/actions/workflows/build.yml/badge.svg)](https://github.com/boghaniakash/SwiftlyAdsAndroid/actions/workflows/build.yml)

A lightweight, Compose-first wrapper around the **Google Mobile Ads SDK (AdMob)** for Android —
the Kotlin port of the [SwiftlyAds](../SwiftlyAds) Swift package. Configure once, show ads anywhere,
with built-in UMP consent management, ad preloading, frequency capping, and a fluent callback API.

## Requirements

| Requirement | Minimum |
|---|---|
| Android (minSdk) | 24 (Android 7.0) |
| compileSdk | 36 |
| Google Mobile Ads | 24.x |
| User Messaging Platform | 3.x |
| Jetpack Compose | Yes |

## Supported ad formats

| Format | API | Description |
|---|---|---|
| **Banner** | `SwiftlyBannerAd` (Composable) | Inline banner with size options + collapsible support |
| **Interstitial** | `SwiftlyAds.showInterstitialAd` | Full-screen, with frequency capping |
| **App Open** | `SwiftlyAds.showAppOpenAd` | Full-screen, with frequency capping |
| **Rewarded** | `SwiftlyAds.showRewardAd` | Video ads granting a reward |
| **Rewarded Interstitial** | `SwiftlyAds.showRewardInterAd` | Hybrid rewarded + interstitial |
| **Native** | `SwiftlyNativeAdView` (Composable) | Fully custom Compose layouts |

## Installation

SwiftlyAds is published to **Maven Central**. `mavenCentral()` is already in the default Android
project, so just add the dependency to your app module's **`build.gradle.kts`**:

```kotlin
dependencies {
    implementation("io.github.boghaniakash:swiftlyads:0.2.0")
}
```

<details>
<summary>Using a version catalog (<code>libs.versions.toml</code>)?</summary>

```toml
[versions]
swiftlyads = "0.2.0"

[libraries]
swiftlyads = { module = "io.github.boghaniakash:swiftlyads", version.ref = "swiftlyads" }
```

then `implementation(libs.swiftlyads)`.
</details>

The library declares the AdMob and UMP dependencies as `api`, so they are available to your app
transitively.

### Alternative — build from source

This repository is also a standalone Gradle library project (module `:swiftlyads`) you can build
locally.

#### Option A — use it as a local module

Copy the `swiftlyads/` directory into your app project and add to your **`settings.gradle.kts`**:

```kotlin
include(":swiftlyads")
```

Then in your app module's **`build.gradle.kts`**:

```kotlin
dependencies {
    implementation(project(":swiftlyads"))
}
```

#### Option B — publish an AAR

Run `./gradlew :swiftlyads:assembleRelease` and consume `swiftlyads/build/outputs/aar/swiftlyads-release.aar`.

### Add your AdMob App ID to the app manifest

The Google Mobile Ads SDK requires your **AdMob App ID** in the *app's* `AndroidManifest.xml`
(inside `<application>`):

```xml
<meta-data
    android:name="com.google.android.gms.ads.APPLICATION_ID"
    android:value="ca-app-pub-3940256099942544~3347511713" /> <!-- test App ID -->
```

The library already merges the required `INTERNET`, `ACCESS_NETWORK_STATE`, and `AD_ID` permissions.

## Quick start

### 1. Configure (once, e.g. in `Application.onCreate`)

```kotlin
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        SwiftlyAds.configure(
            this,
            SwiftlyAdsConfiguration()
                .bannerAdUnitId("ca-app-pub-3940256099942544/9214589741")
                .interstitialAdUnitId("ca-app-pub-3940256099942544/1033173712")
                .rewardedAdUnitId("ca-app-pub-3940256099942544/5224354917")
                .rewardedInterstitialAdUnitId("ca-app-pub-3940256099942544/5354046379")
                .appOpenAdUnitId("ca-app-pub-3940256099942544/9257395921")
                .nativeAdUnitId("ca-app-pub-3940256099942544/2247696110")
                .preloadsAds(true)
                .interAdShowCount(3)
                .environment(SwiftlyAdEnvironment.DEVELOPMENT)
                .testDeviceIdentifiers(listOf("YOUR-TEST-DEVICE-ID"))
        )
    }
}
```

> Difference from iOS: `configure` takes a `Context` because Android needs one to load ads.

### 2. Initialize (with consent), from an `Activity`

```kotlin
SwiftlyAds.initializeIfNeeded(activity)
    .onSuccess { Log.d("Ads", "SDK ready") }
    .onError { e -> Log.e("Ads", "init failed", e) }

// Skip the UMP consent flow:
SwiftlyAds.initializeIfNeeded(activity, showConsent = false)
```

### 3. Show full-screen ads

```kotlin
SwiftlyAds.showInterstitialAd(activity)
    .onOpen { }
    .onClose { }
    .onError { e -> }

SwiftlyAds.showRewardAd(activity)
    .onReward { amount -> /* grant `amount` */ }
    .onClose { }

SwiftlyAds.showAppOpenAd(activity)
    .onOpen { }
    .onClose { }

SwiftlyAds.showRewardInterAd(activity)
    .onReward { amount -> }
    .onClose { }
```

Bypass frequency capping for a specific call:

```kotlin
SwiftlyAds.showInterstitialAd(activity, bypassingFrequencyLimit = true)
SwiftlyAds.showAppOpenAd(activity, bypassingFrequencyLimit = true)
```

## Coroutines & Flow (optional)

Prefer `suspend` over callbacks? Every full-screen format has a coroutine-friendly extension that
suspends until the ad is dismissed (or fails). These live alongside the callback API — mix and match
freely — and are safe under cancellation.

```kotlin
// Initialize (suspends until the SDK is ready; throws on failure)
SwiftlyAds.initialize(activity)                       // showConsent = true by default

// Full-screen ads return a SwiftlyAdResult
when (val result = SwiftlyAds.showInterstitial(activity)) {
    is SwiftlyAdResult.Dismissed -> { }
    is SwiftlyAdResult.Failed    -> result.error
    is SwiftlyAdResult.Rewarded  -> { }               // only for rewarded formats
}

// Rewarded formats resolve to Rewarded(amount) when the reward is earned
val reward = SwiftlyAds.showReward(activity)
if (reward is SwiftlyAdResult.Rewarded) grant(reward.amount)

SwiftlyAds.showAppOpen(activity)
SwiftlyAds.showRewardInter(activity)

// Run the UMP consent flow and await the resulting status
val status: SwiftlyConsentStatus = SwiftlyAds.awaitConsent(activity)
```

### Native ads as suspend / Flow

```kotlin
// One-shot: returns the ad, or null when frequency-capped / disabled (throws on SDK error)
val ad: SwiftlyNativeAds? = SwiftlyAds.loadNativeAd()

// Or observe the request lifecycle as a Flow — ideal for Compose:
val state by SwiftlyAds.nativeAdFlow().collectAsStateWithLifecycle(SwiftlyNativeAdState.Loading)

when (val s = state) {
    SwiftlyNativeAdState.Loading     -> CircularProgressIndicator()
    is SwiftlyNativeAdState.Loaded   -> SwiftlyNativeAdView(s.ad) { ad -> /* your layout */ }
    SwiftlyNativeAdState.Unavailable -> { }
    is SwiftlyNativeAdState.Failed   -> { /* s.error */ }
}
```

> Requires `androidx.lifecycle:lifecycle-runtime-compose` for `collectAsStateWithLifecycle`.
> The coroutines dependency itself is exposed transitively (`api`), so no extra setup is needed.

## Banner ads

Banner is a Composable. It renders nothing when ads are disabled, no banner unit ID is set,
consent is not obtained, or the ad fails to load.

```kotlin
SwiftlyBannerAd(
    format = SwiftlyBannerAdFormat.ADAPTIVE_BANNER,
    isCollapsible = true,
    onShow = { /* banner visible */ },
)
```

Available formats (`SwiftlyBannerAdFormat`): `STANDARD_BANNER`, `LARGE_BANNER`, `MEDIUM_RECTANGLE`,
`FULL_BANNER`, `LEADERBOARD`, `SKYSCRAPER`, `FLUID`, `ADAPTIVE_BANNER`.

## Native ads

Native ads are built from your own Compose layout. Tag each asset with
`Modifier.nativeAdElement(...)` so the underlying `NativeAdView` can attribute clicks; use
`SwiftlyMediaView` for the media asset.

### Request an ad

```kotlin
var nativeAd by remember { mutableStateOf<SwiftlyNativeAds?>(null) }

LaunchedEffect(Unit) {
    SwiftlyAds.requestNativeAd()
        .onReceiveAd { ad -> nativeAd = ad }
        .onAdLoaded { }
        .onError { e -> }
}
```

### Build the layout

```kotlin
SwiftlyNativeAdView(nativeAd, modifier = Modifier.fillMaxWidth()) { ad ->
    Column(Modifier.padding(12.dp)) {
        Text(
            ad?.headline.orEmpty(),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.nativeAdElement(SwiftlyNativeAdElement.HEADLINE),
        )

        SwiftlyMediaView(ad, Modifier.fillMaxWidth())

        Text(
            ad?.body.orEmpty(),
            modifier = Modifier.nativeAdElement(SwiftlyNativeAdElement.BODY),
        )

        ad?.icon?.drawable?.let { drawable ->
            Image(
                painter = rememberDrawablePainter(drawable),
                contentDescription = null,
                modifier = Modifier.size(40.dp).nativeAdElement(SwiftlyNativeAdElement.ICON),
            )
        }

        Button(
            onClick = {},
            modifier = Modifier.nativeAdElement(SwiftlyNativeAdElement.CALL_TO_ACTION),
        ) {
            Text(ad?.callToAction ?: "Learn more")
        }
    }
}
```

Elements (`SwiftlyNativeAdElement`): `HEADLINE`, `CALL_TO_ACTION`, `ICON`, `BODY`, `STORE`,
`PRICE`, `IMAGE`, `STAR_RATING`, `ADVERTISER`, `MEDIA`, `AD_CHOICES`.

> **How clicks work:** `SwiftlyNativeAdView` hosts your Compose content inside a real `NativeAdView`
> and overlays invisible, registered hit-target views over each tagged element — the same technique
> the iOS library uses. Treat the content as display-only; taps are routed to the ad.

## Consent (UMP)

Consent is requested automatically during `initializeIfNeeded(activity)` (unless
`showConsent = false`). Update it manually:

```kotlin
SwiftlyAds.updateConsent(activity) { result ->
    result
        .onSuccess { status -> /* SwiftlyConsentStatus */ }
        .onFailure { e -> }
}

val status = SwiftlyAds.consentStatus
```

Development consent settings:

```kotlin
SwiftlyAdsConfiguration()
    .environment(SwiftlyAdEnvironment.DEVELOPMENT)
    .testDeviceIdentifiers(listOf("YOUR-TEST-DEVICE-ID"))
    .geography(SwiftlyDebugGeography.EEA)
    .resetsConsentOnLaunch(true)
```

## Frequency capping, preloading, enable/disable

```kotlin
SwiftlyAdsConfiguration()
    .interAdShowCount(3)     // show interstitial every 3rd request
    .appOpenAdShowCount(2)
    .nativeAdShowCount(2)
    .preloadsAds(true)       // load all configured formats after init; reload after dismissal

// Premium upgrade:
SwiftlyAds.disable()         // stops all managers, ads stop showing
SwiftlyAds.enable()          // reloads configured ad types
SwiftlyAds.isDisabled

// Readiness:
SwiftlyAds.isInterstitialAdReady
SwiftlyAds.isAppOpenAdReady
SwiftlyAds.isRewardAdReady
SwiftlyAds.isRewardInterAdReady
```

## Mediation

```kotlin
class MyMediationConfigurator : SwiftlyAdsMediationConfigurator {
    override fun updateCOPPA(isTaggedForChildDirectedTreatment: Boolean) { }
    override fun updateGDPR(consentStatus: SwiftlyConsentStatus, isTaggedForUnderAgeOfConsent: Boolean) { }
}

SwiftlyAdsConfiguration().mediationConfigurator(MyMediationConfigurator())
```

## iOS → Android API mapping

| iOS (Swift) | Android (Kotlin) |
|---|---|
| `SwiftlyAds.shared.configure(_:)` | `SwiftlyAds.configure(context, config)` |
| `initializeIfNeeded(from: vc)` | `initializeIfNeeded(activity)` |
| `showInterstitialAd(from: vc)` | `showInterstitialAd(activity)` |
| `showRewardAd(from: vc)` | `showRewardAd(activity)` |
| `requestNativeAd()` | `requestNativeAd()` |
| `makeBannerView(format:)` (returns a `View`) | `SwiftlyBannerAd(format = ...)` (Composable) |
| `SwiftlyNativeAdView(nativeAd: $ad) { }` | `SwiftlyNativeAdView(nativeAd) { }` |
| `.nativeAdElement(.headline)` | `Modifier.nativeAdElement(SwiftlyNativeAdElement.HEADLINE)` |
| `SwiftlyMediaView(mediaContent:)` | `SwiftlyMediaView(nativeAd)` |
| `onReward { amount: NSDecimalNumber }` | `onReward { amount: Int }` |

## Notes

- Full-screen ads require an `Activity` (not the application context).
- For the `rememberDrawablePainter` helper used in the native example, add
  `com.google.accompanist:accompanist-drawablepainter`, or load the icon however you prefer.
- Native ad instances are cached internally; the library does not eagerly call `NativeAd.destroy()`
  on cache eviction (mirroring iOS ARC behaviour). Call `destroy()` yourself when permanently done.

## License

MIT (same as the original SwiftlyAds Swift package).
