package io.github.akashboghani.swiftlyads.sample

import android.app.Application
import io.github.akashboghani.swiftlyads.SwiftlyAds
import io.github.akashboghani.swiftlyads.config.SwiftlyAdEnvironment
import io.github.akashboghani.swiftlyads.config.SwiftlyAdsConfiguration
import io.github.akashboghani.swiftlyads.config.SwiftlyMediaAspectRatio

/**
 * Configures SwiftlyAds once at process start using Google's official AdMob *test* ad unit IDs,
 * so the sample shows test ads safely. Replace these IDs (and the App ID in the manifest) with
 * your own for production.
 */
class SampleApp : Application() {
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
                .nativeAdUnitId("/21775744923/example/native")
                .preloadsAds(true)
                .environment(SwiftlyAdEnvironment.PRODUCTION)
                .nativeAdMediaAspectRatio(SwiftlyMediaAspectRatio.LANDSCAPE),
        )
    }
}
