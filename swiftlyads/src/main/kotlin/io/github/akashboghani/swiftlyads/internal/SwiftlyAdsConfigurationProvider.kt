package io.github.akashboghani.swiftlyads.internal

import io.github.akashboghani.swiftlyads.SwiftlyAdRequest
import io.github.akashboghani.swiftlyads.config.SwiftlyAdEnvironment
import io.github.akashboghani.swiftlyads.config.SwiftlyAdsMediationConfigurator
import io.github.akashboghani.swiftlyads.config.SwiftlyDebugGeography
import io.github.akashboghani.swiftlyads.config.SwiftlyMediaAspectRatio

/** Immutable snapshot of a built [io.github.akashboghani.swiftlyads.config.SwiftlyAdsConfiguration]. */
internal data class SwiftlyAdsConfigurationProvider(
    val bannerAdUnitId: String?,
    val interstitialAdUnitId: String?,
    val rewardedAdUnitId: String?,
    val rewardedInterstitialAdUnitId: String?,
    val appOpenAdUnitId: String?,
    val nativeAdUnitId: String?,
    val isTaggedForChildDirectedTreatment: Boolean?,
    val isTaggedForUnderAgeOfConsent: Boolean?,
    val preloadsAds: Boolean,
    val mediationConfigurator: SwiftlyAdsMediationConfigurator?,
    val testDeviceIdentifiers: List<String>,
    val geography: SwiftlyDebugGeography,
    val resetsConsentOnLaunch: Boolean,
    val interAdShowCount: Int,
    val showsFirstInterAd: Boolean,
    val appOpenAdShowCount: Int,
    val nativeAdShowCount: Int,
    val environment: SwiftlyAdEnvironment,
    val adRequest: SwiftlyAdRequest?,
    val nativeAdMediaAspectRatio: SwiftlyMediaAspectRatio?,
)
