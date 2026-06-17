package io.github.akashboghani.swiftlyads.presentation

/** Fluent callbacks for interstitial ads. Mirrors iOS `SwiftlyInterAd`. */
class SwiftlyInterAd {
    internal var onOpenCallback: (() -> Unit)? = null
    internal var onCloseCallback: (() -> Unit)? = null
    internal var onErrorCallback: ((Throwable) -> Unit)? = null

    fun onOpen(handler: () -> Unit): SwiftlyInterAd = apply { onOpenCallback = handler }
    fun onClose(handler: () -> Unit): SwiftlyInterAd = apply { onCloseCallback = handler }
    fun onError(handler: (Throwable) -> Unit): SwiftlyInterAd = apply { onErrorCallback = handler }
}
