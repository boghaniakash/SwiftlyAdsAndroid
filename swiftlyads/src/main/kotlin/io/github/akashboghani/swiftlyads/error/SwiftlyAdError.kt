package io.github.akashboghani.swiftlyads.error

/**
 * Errors surfaced through the `onError` callbacks. Mirrors iOS `SwiftlyAdError`
 * (which conforms to `LocalizedError`); every case carries a human-readable message.
 */
sealed class SwiftlyAdError(message: String) : Exception(message) {
    object InterstitialAdUnitIdNotSet : SwiftlyAdError("Interstitial ad unit ID is not set.")
    object AppOpenAdUnitIdNotSet : SwiftlyAdError("AppOpen ad unit ID is not set.")
    object RewardedAdUnitIdNotSet : SwiftlyAdError("Rewarded ad unit ID is not set.")
    object RewardedInterAdUnitIdNotSet : SwiftlyAdError("RewardedInterstitial ad unit ID is not set.")
    object NativeAdUnitIdNotSet : SwiftlyAdError("Native ad unit ID is not set.")
    object InterstitialAdNotLoaded : SwiftlyAdError("InterstitialAd not loaded.")
    object AppOpenAdNotLoaded : SwiftlyAdError("AppOpenAd not loaded.")
    object RewardedAdNotLoaded : SwiftlyAdError("RewardedAd not loaded.")
    object RewardedInterAdNotLoaded : SwiftlyAdError("RewardedInterstitialAd not loaded.")
    object ConsentManagerNotAvailable : SwiftlyAdError("ConsentManager not available.")
    object ConsentNotObtained : SwiftlyAdError("Consent not obtained.")

    /** Wraps a load/show failure reported by the underlying Google Mobile Ads SDK. */
    class SdkError(message: String, val code: Int? = null) : SwiftlyAdError(message)

    /** Wraps a UMP consent form/info error. */
    class ConsentError(message: String, val code: Int? = null) : SwiftlyAdError(message)
}
