package io.github.akashboghani.swiftlyads.internal

import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executors

/**
 * Helper that posts work onto the main thread.
 *
 * The fluent API (`showInterstitialAd(...).onClose { }`) attaches its callbacks
 * *after* the call returns. To let those handlers register before a synthetic callback
 * fires, we post the callback with [nextTick] — it runs on the *next* main-thread message,
 * i.e. once the current statement (including the chained `.onClose { }` setters) has fully
 * executed. This replaces the old fixed 100 ms delay: there is no arbitrary latency and the
 * ordering guarantee is exact rather than a race against a timer.
 */
internal object MainDispatch {

    private val handler = Handler(Looper.getMainLooper())
    private val backgroundExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "SwiftlyAds-init").apply { isDaemon = true }
    }

    /**
     * Runs [block] on the next main-thread message. Always defers (never runs inline), even when
     * already on the main thread, so callers chaining `.onX { }` after a `show*`/`request*` call
     * are guaranteed to have registered their handlers before [block] runs.
     */
    fun nextTick(block: () -> Unit) {
        handler.post(block)
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
