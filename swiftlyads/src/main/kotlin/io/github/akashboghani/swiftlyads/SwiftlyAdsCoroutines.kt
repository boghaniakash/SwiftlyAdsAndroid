package io.github.akashboghani.swiftlyads

import android.app.Activity
import io.github.akashboghani.swiftlyads.config.SwiftlyConsentStatus
import io.github.akashboghani.swiftlyads.config.SwiftlyMediaAspectRatio
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Coroutine-friendly surface over the callback API. Every function here is a thin wrapper around
 * the existing fluent `show*` / `request*` / `initializeIfNeeded` calls — the callback API stays
 * fully intact, so you can mix and match.
 *
 * The wrappers are safe under cancellation: each only resumes its continuation once (guarded by
 * `isActive`), and `nativeAdFlow` closes its callback channel when the collector goes away.
 */

/** Suspends until the Mobile Ads SDK is initialized (and consent resolved). Throws on failure. */
suspend fun SwiftlyAds.initialize(activity: Activity, showConsent: Boolean = true): Unit =
    suspendCancellableCoroutine { cont ->
        initializeIfNeeded(activity, showConsent)
            .onSuccess { if (cont.isActive) cont.resume(Unit) }
            .onError { e -> if (cont.isActive) cont.resumeWithException(e) }
    }

/** Shows an interstitial and suspends until it is dismissed (or fails). */
suspend fun SwiftlyAds.showInterstitial(
    activity: Activity,
    bypassingFrequencyLimit: Boolean = false,
): SwiftlyAdResult = suspendCancellableCoroutine { cont ->
    showInterstitialAd(activity, bypassingFrequencyLimit)
        .onClose { if (cont.isActive) cont.resume(SwiftlyAdResult.Dismissed) }
        .onError { e -> if (cont.isActive) cont.resume(SwiftlyAdResult.Failed(e)) }
}

/** Shows an app open ad and suspends until it is dismissed (or fails). */
suspend fun SwiftlyAds.showAppOpen(
    activity: Activity,
    bypassingFrequencyLimit: Boolean = false,
): SwiftlyAdResult = suspendCancellableCoroutine { cont ->
    showAppOpenAd(activity, bypassingFrequencyLimit)
        .onClose { if (cont.isActive) cont.resume(SwiftlyAdResult.Dismissed) }
        .onError { e -> if (cont.isActive) cont.resume(SwiftlyAdResult.Failed(e)) }
}

/**
 * Shows a rewarded ad and suspends until it is dismissed. Resolves to [SwiftlyAdResult.Rewarded]
 * when the reward was earned, [SwiftlyAdResult.Dismissed] when closed without earning, or
 * [SwiftlyAdResult.Failed] on error.
 */
suspend fun SwiftlyAds.showReward(activity: Activity): SwiftlyAdResult =
    suspendCancellableCoroutine { cont ->
        var rewardAmount: Int? = null
        showRewardAd(activity)
            .onReward { rewardAmount = it }
            .onClose {
                if (cont.isActive) {
                    cont.resume(rewardAmount?.let { SwiftlyAdResult.Rewarded(it) } ?: SwiftlyAdResult.Dismissed)
                }
            }
            .onError { e -> if (cont.isActive) cont.resume(SwiftlyAdResult.Failed(e)) }
    }

/** Like [showReward], but for the rewarded interstitial format. */
suspend fun SwiftlyAds.showRewardInter(activity: Activity): SwiftlyAdResult =
    suspendCancellableCoroutine { cont ->
        var rewardAmount: Int? = null
        showRewardInterAd(activity)
            .onReward { rewardAmount = it }
            .onClose {
                if (cont.isActive) {
                    cont.resume(rewardAmount?.let { SwiftlyAdResult.Rewarded(it) } ?: SwiftlyAdResult.Dismissed)
                }
            }
            .onError { e -> if (cont.isActive) cont.resume(SwiftlyAdResult.Failed(e)) }
    }

/**
 * Requests a single native ad and suspends until it is delivered. Returns the ad, or `null` when
 * frequency-capped / disabled. Throws on an SDK error.
 */
suspend fun SwiftlyAds.loadNativeAd(
    bypassingFrequencyLimit: Boolean = false,
    mediaAspectRatio: SwiftlyMediaAspectRatio? = null,
): SwiftlyNativeAds? = suspendCancellableCoroutine { cont ->
    requestNativeAd(bypassingFrequencyLimit, mediaAspectRatio)
        .onReceiveAd { if (cont.isActive) cont.resume(it) }
        .onError { e -> if (cont.isActive) cont.resumeWithException(e) }
}

/**
 * Emits the lifecycle of a native ad request as a [Flow]: [SwiftlyNativeAdState.Loading] first,
 * then exactly one of [SwiftlyNativeAdState.Loaded] / [SwiftlyNativeAdState.Unavailable] /
 * [SwiftlyNativeAdState.Failed]. Ideal for Compose via `collectAsStateWithLifecycle`.
 */
fun SwiftlyAds.nativeAdFlow(
    bypassingFrequencyLimit: Boolean = false,
    mediaAspectRatio: SwiftlyMediaAspectRatio? = null,
): Flow<SwiftlyNativeAdState> = callbackFlow {
    trySend(SwiftlyNativeAdState.Loading)
    requestNativeAd(bypassingFrequencyLimit, mediaAspectRatio)
        .onReceiveAd { ad ->
            trySend(if (ad != null) SwiftlyNativeAdState.Loaded(ad) else SwiftlyNativeAdState.Unavailable)
        }
        .onError { e -> trySend(SwiftlyNativeAdState.Failed(e)) }
    awaitClose { }
}

/** Runs the UMP consent flow and suspends until it completes, returning the resulting status. */
suspend fun SwiftlyAds.awaitConsent(activity: Activity): SwiftlyConsentStatus =
    suspendCancellableCoroutine { cont ->
        updateConsent(activity) { result ->
            result
                .onSuccess { if (cont.isActive) cont.resume(it) }
                .onFailure { e -> if (cont.isActive) cont.resumeWithException(e) }
        }
    }
