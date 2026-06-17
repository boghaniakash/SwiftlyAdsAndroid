package io.github.akashboghani.swiftlyads.presentation

/**
 * Fluent callbacks for rewarded ads. Mirrors iOS `SwiftlyRewardAd`.
 * [onReward] delivers the reward amount granted by the ad.
 */
class SwiftlyRewardAd {
    internal var onOpenCallback: (() -> Unit)? = null
    internal var onCloseCallback: (() -> Unit)? = null
    internal var onErrorCallback: ((Throwable) -> Unit)? = null
    internal var onRewardCallback: ((Int) -> Unit)? = null

    fun onOpen(handler: () -> Unit): SwiftlyRewardAd = apply { onOpenCallback = handler }
    fun onClose(handler: () -> Unit): SwiftlyRewardAd = apply { onCloseCallback = handler }
    fun onError(handler: (Throwable) -> Unit): SwiftlyRewardAd = apply { onErrorCallback = handler }
    fun onReward(handler: (amount: Int) -> Unit): SwiftlyRewardAd = apply { onRewardCallback = handler }
}
