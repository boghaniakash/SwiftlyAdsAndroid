package io.github.akashboghani.swiftlyads.internal.managers

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import io.github.akashboghani.swiftlyads.error.SwiftlyAdError
import io.github.akashboghani.swiftlyads.internal.MainDispatch
import io.github.akashboghani.swiftlyads.presentation.SwiftlyInterAd

/**
 * Manages the interstitial ad lifecycle. Mirrors iOS `InterAdManager`.
 *
 * The presentation (callback sink) is supplied per `show` call rather than once at construction,
 * so two overlapping show requests never clobber each other's callbacks. [activePresentation]
 * tracks the show currently in flight; the full-screen callback — which may have been attached at
 * preload time, before any show — reads it at fire time.
 */
internal class InterAdManager(
    private val appContext: Context,
    private val adUnitId: String,
    private val adRequestProvider: () -> AdRequest,
) {
    private var interstitialAd: InterstitialAd? = null
    private var isLoading = false
    private var activePresentation: SwiftlyInterAd? = null

    val isReady: Boolean get() = interstitialAd != null

    /** Loads (and caches) an interstitial. No-op if one is already loaded or loading. */
    fun loadAd() {
        if (interstitialAd != null || isLoading) return
        isLoading = true
        InterstitialAd.load(
            appContext,
            adUnitId,
            adRequestProvider(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    isLoading = false
                    ad.fullScreenContentCallback = fullScreenCallback()
                    interstitialAd = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    isLoading = false
                    interstitialAd = null
                }
            },
        )
    }

    fun stopLoading() {
        interstitialAd?.fullScreenContentCallback = null
        interstitialAd = null
    }

    /** Shows a preloaded ad for [presentation], or surfaces [SwiftlyAdError.InterstitialAdNotLoaded] and reloads. */
    fun show(activity: Activity, presentation: SwiftlyInterAd) {
        activePresentation = presentation
        val ad = interstitialAd
        if (ad != null) {
            ad.show(activity)
        } else {
            reload()
            MainDispatch.nextTick {
                presentation.onErrorCallback?.invoke(SwiftlyAdError.InterstitialAdNotLoaded)
            }
        }
    }

    /** On-demand path used when preloading is disabled: load, then show as soon as it is ready. */
    fun loadAndShow(activity: Activity, presentation: SwiftlyInterAd) {
        activePresentation = presentation
        if (interstitialAd != null) {
            show(activity, presentation)
            return
        }
        isLoading = true
        InterstitialAd.load(
            appContext,
            adUnitId,
            adRequestProvider(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    isLoading = false
                    ad.fullScreenContentCallback = fullScreenCallback()
                    interstitialAd = ad
                    ad.show(activity)
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    isLoading = false
                    interstitialAd = null
                    presentation.onErrorCallback?.invoke(
                        SwiftlyAdError.SdkError(error.message, error.code),
                    )
                }
            },
        )
    }

    private fun fullScreenCallback() = object : FullScreenContentCallback() {
        override fun onAdShowedFullScreenContent() {
            activePresentation?.onOpenCallback?.invoke()
        }

        override fun onAdDismissedFullScreenContent() {
            interstitialAd = null
            activePresentation?.onCloseCallback?.invoke()
            reload()
        }

        override fun onAdFailedToShowFullScreenContent(error: AdError) {
            interstitialAd = null
            activePresentation?.onErrorCallback?.invoke(SwiftlyAdError.SdkError(error.message, error.code))
            reload()
        }
    }

    private fun reload() = loadAd()
}
