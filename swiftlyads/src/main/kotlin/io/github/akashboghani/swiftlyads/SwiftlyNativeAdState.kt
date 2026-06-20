package io.github.akashboghani.swiftlyads

/**
 * State of a native ad request, emitted by `SwiftlyAds.nativeAdFlow(...)`. Collect it in Compose
 * (e.g. with `collectAsStateWithLifecycle`) to drive your native ad UI.
 */
sealed interface SwiftlyNativeAdState {
    /** The request is in flight. */
    data object Loading : SwiftlyNativeAdState

    /** An ad was received; render it with `SwiftlyNativeAdView`. */
    data class Loaded(val ad: SwiftlyNativeAds) : SwiftlyNativeAdState

    /** No ad is available right now (frequency-capped or ads disabled). */
    data object Unavailable : SwiftlyNativeAdState

    /** The request failed. */
    data class Failed(val error: Throwable) : SwiftlyNativeAdState
}
