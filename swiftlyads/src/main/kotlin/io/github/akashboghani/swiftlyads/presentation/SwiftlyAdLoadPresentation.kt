package io.github.akashboghani.swiftlyads.presentation

/** Fluent callbacks for SDK initialization. Mirrors iOS `SwiftlyAdLoadPresentation`. */
class SwiftlyAdLoadPresentation {
    internal var onSuccessCallback: (() -> Unit)? = null
    internal var onErrorCallback: ((Throwable) -> Unit)? = null

    fun onSuccess(handler: () -> Unit): SwiftlyAdLoadPresentation = apply { onSuccessCallback = handler }
    fun onError(handler: (Throwable) -> Unit): SwiftlyAdLoadPresentation = apply { onErrorCallback = handler }
}
