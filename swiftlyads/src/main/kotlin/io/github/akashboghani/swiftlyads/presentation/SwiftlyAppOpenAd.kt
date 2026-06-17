package io.github.akashboghani.swiftlyads.presentation

/** Fluent callbacks for app open ads. Mirrors iOS `SwiftlyAppOpenAd`. */
class SwiftlyAppOpenAd {
    internal var onOpenCallback: (() -> Unit)? = null
    internal var onCloseCallback: (() -> Unit)? = null
    internal var onErrorCallback: ((Throwable) -> Unit)? = null

    fun onOpen(handler: () -> Unit): SwiftlyAppOpenAd = apply { onOpenCallback = handler }
    fun onClose(handler: () -> Unit): SwiftlyAppOpenAd = apply { onCloseCallback = handler }
    fun onError(handler: (Throwable) -> Unit): SwiftlyAppOpenAd = apply { onErrorCallback = handler }
}
