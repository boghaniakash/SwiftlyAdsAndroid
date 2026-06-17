package io.github.akashboghani.swiftlyads.internal

import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executors

/**
 * Helper that posts work onto the main thread, optionally after a short delay.
 *
 * The fluent API (`showInterstitialAd(...).onClose { }`) attaches its callbacks
 * *after* the call returns, so — exactly like the iOS library's `doWork(after: 0.1)` —
 * we delay synthetic callbacks by [DEFAULT_DELAY_MS] to give the caller a chance to
 * register handlers before they fire.
 */
internal object MainDispatch {
    const val DEFAULT_DELAY_MS = 100L

    private val handler = Handler(Looper.getMainLooper())
    private val backgroundExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "SwiftlyAds-init").apply { isDaemon = true }
    }

    fun afterDefaultDelay(block: () -> Unit) {
        handler.postDelayed(block, DEFAULT_DELAY_MS)
    }

    fun post(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) block() else handler.post(block)
    }

    /**
     * Runs [block] on a background thread. The Google Mobile Ads SDK's first-time
     * `MobileAds.initialize()` does heavy synchronous work, so calling it on the main
     * thread blocks the UI and triggers an ANR — Google's docs require a worker thread.
     */
    fun background(block: () -> Unit) {
        backgroundExecutor.execute(block)
    }
}
