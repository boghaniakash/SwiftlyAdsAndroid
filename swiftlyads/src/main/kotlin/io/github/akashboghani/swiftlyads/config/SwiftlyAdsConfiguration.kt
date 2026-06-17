package io.github.akashboghani.swiftlyads.config

import io.github.akashboghani.swiftlyads.SwiftlyAdRequest
import io.github.akashboghani.swiftlyads.internal.SwiftlyAdsConfigurationProvider

/**
 * Fluent builder for configuring SwiftlyAds. Mirrors the iOS `SwiftlyAdsConfiguration`.
 *
 * Every method returns `this` for chaining:
 * ```
 * SwiftlyAdsConfiguration()
 *     .bannerAdUnitId("ca-app-pub-.../banner")
 *     .interstitialAdUnitId("ca-app-pub-.../inter")
 *     .preloadsAds(true)
 *     .environment(SwiftlyAdEnvironment.DEVELOPMENT)
 * ```
 */
class SwiftlyAdsConfiguration {
    internal var bannerAdUnitId: String? = null
    internal var interstitialAdUnitId: String? = null
    internal var rewardedAdUnitId: String? = null
    internal var rewardedInterstitialAdUnitId: String? = null
    internal var appOpenAdUnitId: String? = null
    internal var nativeAdUnitId: String? = null
    internal var isTaggedForChildDirectedTreatment: Boolean? = null
    internal var isTaggedForUnderAgeOfConsent: Boolean? = null
    internal var preloadsAds: Boolean? = null
    internal var mediationConfigurator: SwiftlyAdsMediationConfigurator? = null
    internal var testDeviceIdentifiers: List<String>? = null
    internal var geography: SwiftlyDebugGeography? = null
    internal var resetsConsentOnLaunch: Boolean? = null
    internal var interAdShowCount: Int? = null
    internal var appOpenAdShowCount: Int? = null
    internal var nativeAdShowCount: Int? = null
    internal var environment: SwiftlyAdEnvironment? = null
    internal var adRequest: SwiftlyAdRequest? = null
    internal var nativeAdMediaAspectRatio: SwiftlyMediaAspectRatio? = null

    fun bannerAdUnitId(value: String?) = apply { bannerAdUnitId = value }
    fun interstitialAdUnitId(value: String?) = apply { interstitialAdUnitId = value }
    fun rewardedAdUnitId(value: String?) = apply { rewardedAdUnitId = value }
    fun rewardedInterstitialAdUnitId(value: String?) = apply { rewardedInterstitialAdUnitId = value }
    fun appOpenAdUnitId(value: String?) = apply { appOpenAdUnitId = value }
    fun nativeAdUnitId(value: String?) = apply { nativeAdUnitId = value }
    fun isTaggedForChildDirectedTreatment(value: Boolean?) = apply { isTaggedForChildDirectedTreatment = value }
    fun isTaggedForUnderAgeOfConsent(value: Boolean?) = apply { isTaggedForUnderAgeOfConsent = value }
    fun preloadsAds(value: Boolean) = apply { preloadsAds = value }
    fun mediationConfigurator(value: SwiftlyAdsMediationConfigurator?) = apply { mediationConfigurator = value }
    fun testDeviceIdentifiers(value: List<String>) = apply { testDeviceIdentifiers = value }
    fun geography(value: SwiftlyDebugGeography) = apply { geography = value }
    fun resetsConsentOnLaunch(value: Boolean) = apply { resetsConsentOnLaunch = value }
    fun interAdShowCount(value: Int) = apply { interAdShowCount = value }
    fun appOpenAdShowCount(value: Int) = apply { appOpenAdShowCount = value }
    fun nativeAdShowCount(value: Int) = apply { nativeAdShowCount = value }
    fun environment(value: SwiftlyAdEnvironment) = apply { environment = value }
    fun adRequest(value: SwiftlyAdRequest) = apply { adRequest = value }
    fun nativeAdMediaAspectRatio(value: SwiftlyMediaAspectRatio?) = apply { nativeAdMediaAspectRatio = value }

    @Deprecated("Renamed", ReplaceWith("preloadsAds(value)"))
    fun isPreLoadAds(value: Boolean) = preloadsAds(value)

    @Deprecated("Renamed", ReplaceWith("environment(value)"))
    fun setEnvironment(value: SwiftlyAdEnvironment) = environment(value)

    internal fun build(): SwiftlyAdsConfigurationProvider = SwiftlyAdsConfigurationProvider(
        bannerAdUnitId = bannerAdUnitId,
        interstitialAdUnitId = interstitialAdUnitId,
        rewardedAdUnitId = rewardedAdUnitId,
        rewardedInterstitialAdUnitId = rewardedInterstitialAdUnitId,
        appOpenAdUnitId = appOpenAdUnitId,
        nativeAdUnitId = nativeAdUnitId,
        isTaggedForChildDirectedTreatment = isTaggedForChildDirectedTreatment,
        isTaggedForUnderAgeOfConsent = isTaggedForUnderAgeOfConsent,
        preloadsAds = preloadsAds ?: false,
        mediationConfigurator = mediationConfigurator,
        testDeviceIdentifiers = testDeviceIdentifiers ?: emptyList(),
        geography = geography ?: SwiftlyDebugGeography.EEA,
        resetsConsentOnLaunch = resetsConsentOnLaunch ?: false,
        interAdShowCount = interAdShowCount ?: 1,
        appOpenAdShowCount = appOpenAdShowCount ?: 1,
        nativeAdShowCount = nativeAdShowCount ?: 1,
        environment = environment ?: SwiftlyAdEnvironment.DEVELOPMENT,
        adRequest = adRequest,
        nativeAdMediaAspectRatio = nativeAdMediaAspectRatio,
    )
}
