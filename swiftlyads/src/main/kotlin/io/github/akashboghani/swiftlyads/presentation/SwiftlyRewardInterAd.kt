package io.github.akashboghani.swiftlyads.presentation

/**
 * Fluent callbacks for rewarded interstitial ads. Mirrors iOS `SwiftlyRewardInterAd`.
 * [onReward] delivers the reward amount granted by the ad.
 */
class SwiftlyRewardInterAd {
    internal var onOpenCallback: (() -> Unit)? = null
    internal var onCloseCallback: (() -> Unit)? = null
    internal var onErrorCallback: ((Throwable) -> Unit)? = null
    internal var onRewardCallback: ((Int) -> Unit)? = null

    fun onOpen(handler: () -> Unit): SwiftlyRewardInterAd = apply { onOpenCallback = handler }
    fun onClose(handler: () -> Unit): SwiftlyRewardInterAd = apply { onCloseCallback = handler }
    fun onError(handler: (Throwable) -> Unit): SwiftlyRewardInterAd = apply { onErrorCallback = handler }
    fun onReward(handler: (amount: Int) -> Unit): SwiftlyRewardInterAd = apply { onRewardCallback = handler }
}
