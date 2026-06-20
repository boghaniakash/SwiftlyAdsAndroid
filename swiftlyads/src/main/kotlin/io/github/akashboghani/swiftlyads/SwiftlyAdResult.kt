package io.github.akashboghani.swiftlyads

/**
 * Terminal outcome of a `suspend` full-screen ad call (interstitial, app open, rewarded,
 * rewarded interstitial). Returned by the coroutine API in `SwiftlyAdsCoroutines.kt`.
 */
sealed interface SwiftlyAdResult {
    /** The ad was shown and dismissed without granting a reward. */
    data object Dismissed : SwiftlyAdResult

    /** The user earned a reward of [amount] (rewarded / rewarded-interstitial ads only). */
    data class Rewarded(val amount: Int) : SwiftlyAdResult

    /** The ad could not be shown (not loaded, no consent, not configured, or an SDK error). */
    data class Failed(val error: Throwable) : SwiftlyAdResult
}
