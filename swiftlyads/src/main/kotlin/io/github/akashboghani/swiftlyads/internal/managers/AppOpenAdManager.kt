package io.github.akashboghani.swiftlyads.internal.managers

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import io.github.akashboghani.swiftlyads.error.SwiftlyAdError
import io.github.akashboghani.swiftlyads.internal.MainDispatch
import io.github.akashboghani.swiftlyads.presentation.SwiftlyAppOpenAd

/**
 * Manages the app open ad lifecycle. Mirrors iOS `AppOpenAdManager`.
 *
 * The presentation is supplied per `show` call (see [InterAdManager] for the rationale);
 * [activePresentation] tracks the show currently in flight.
 */
internal class AppOpenAdManager(
    private val appContext: Context,
    private val adUnitId: String,
    private val adRequestProvider: () -> AdRequest,
) {
    private var appOpenAd: AppOpenAd? = null
    private var isLoading = false
    private var activePresentation: SwiftlyAppOpenAd? = null

    val isReady: Boolean get() = appOpenAd != null

    fun loadAd() {
        if (appOpenAd != null || isLoading) return
        isLoading = true
        AppOpenAd.load(
            appContext,
            adUnitId,
            adRequestProvider(),
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    isLoading = false
                    ad.fullScreenContentCallback = fullScreenCallback()
                    appOpenAd = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    isLoading = false
                    appOpenAd = null
                }
            },
        )
    }

    fun stopLoading() {
        appOpenAd?.fullScreenContentCallback = null
        appOpenAd = null
    }

    fun show(activity: Activity, presentation: SwiftlyAppOpenAd) {
        activePresentation = presentation
        val ad = appOpenAd
        if (ad != null) {
            ad.show(activity)
        } else {
            reload()
            MainDispatch.nextTick {
                presentation.onErrorCallback?.invoke(SwiftlyAdError.AppOpenAdNotLoaded)
            }
        }
    }

    fun loadAndShow(activity: Activity, presentation: SwiftlyAppOpenAd) {
        activePresentation = presentation
        if (appOpenAd != null) {
            show(activity, presentation)
            return
        }
        isLoading = true
        AppOpenAd.load(
            appContext,
            adUnitId,
            adRequestProvider(),
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    isLoading = false
                    ad.fullScreenContentCallback = fullScreenCallback()
                    appOpenAd = ad
                    ad.show(activity)
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    isLoading = false
                    appOpenAd = null
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
            appOpenAd = null
            activePresentation?.onCloseCallback?.invoke()
            reload()
        }

        override fun onAdFailedToShowFullScreenContent(error: AdError) {
            appOpenAd = null
            activePresentation?.onErrorCallback?.invoke(SwiftlyAdError.SdkError(error.message, error.code))
            reload()
        }
    }

    private fun reload() = loadAd()
}
