package io.github.akashboghani.swiftlyads.presentation

import io.github.akashboghani.swiftlyads.SwiftlyNativeAds

/** Fluent callbacks for native ads. Mirrors iOS `SwiftlyNativeAd`. */
class SwiftlyNativeAd {
    internal var onReceiveAdCallback: ((SwiftlyNativeAds?) -> Unit)? = null
    internal var onAdLoadedCallback: (() -> Unit)? = null
    internal var onErrorCallback: ((Throwable) -> Unit)? = null

    fun onReceiveAd(handler: (SwiftlyNativeAds?) -> Unit): SwiftlyNativeAd = apply { onReceiveAdCallback = handler }
    fun onAdLoaded(handler: () -> Unit): SwiftlyNativeAd = apply { onAdLoadedCallback = handler }
    fun onError(handler: (Throwable) -> Unit): SwiftlyNativeAd = apply { onErrorCallback = handler }
}
