package io.github.akashboghani.swiftlyads.internal.managers

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback
import io.github.akashboghani.swiftlyads.error.SwiftlyAdError
import io.github.akashboghani.swiftlyads.internal.MainDispatch
import io.github.akashboghani.swiftlyads.presentation.SwiftlyRewardInterAd

/**
 * Manages the rewarded interstitial ad lifecycle. Mirrors iOS `RewardInterAdManager`.
 *
 * The presentation is supplied per `show` call (see [InterAdManager] for the rationale);
 * [activePresentation] tracks the show currently in flight and receives the reward callback.
 */
internal class RewardInterAdManager(
    private val appContext: Context,
    private val adUnitId: String,
    private val adRequestProvider: () -> AdRequest,
) {
    private var rewardedInterstitialAd: RewardedInterstitialAd? = null
    private var isLoading = false
    private var activePresentation: SwiftlyRewardInterAd? = null

    val isReady: Boolean get() = rewardedInterstitialAd != null

    fun loadAd() {
        if (rewardedInterstitialAd != null || isLoading) return
        isLoading = true
        RewardedInterstitialAd.load(
            appContext,
            adUnitId,
            adRequestProvider(),
            object : RewardedInterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedInterstitialAd) {
                    isLoading = false
                    ad.fullScreenContentCallback = fullScreenCallback()
                    rewardedInterstitialAd = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    isLoading = false
                    rewardedInterstitialAd = null
                }
            },
        )
    }

    fun stopLoading() {
        rewardedInterstitialAd?.fullScreenContentCallback = null
        rewardedInterstitialAd = null
    }

    fun show(activity: Activity, presentation: SwiftlyRewardInterAd) {
        activePresentation = presentation
        val ad = rewardedInterstitialAd
        if (ad != null) {
            ad.show(activity) { rewardItem ->
                presentation.onRewardCallback?.invoke(rewardItem.amount)
            }
        } else {
            reload()
            MainDispatch.nextTick {
                presentation.onErrorCallback?.invoke(SwiftlyAdError.RewardedInterAdNotLoaded)
            }
        }
    }

    fun loadAndShow(activity: Activity, presentation: SwiftlyRewardInterAd) {
        activePresentation = presentation
        if (rewardedInterstitialAd != null) {
            show(activity, presentation)
            return
        }
        isLoading = true
        RewardedInterstitialAd.load(
            appContext,
            adUnitId,
            adRequestProvider(),
            object : RewardedInterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedInterstitialAd) {
                    isLoading = false
                    ad.fullScreenContentCallback = fullScreenCallback()
                    rewardedInterstitialAd = ad
                    ad.show(activity) { rewardItem ->
                        presentation.onRewardCallback?.invoke(rewardItem.amount)
                    }
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    isLoading = false
                    rewardedInterstitialAd = null
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
            rewardedInterstitialAd = null
            activePresentation?.onCloseCallback?.invoke()
            reload()
        }

        override fun onAdFailedToShowFullScreenContent(error: AdError) {
            rewardedInterstitialAd = null
            activePresentation?.onErrorCallback?.invoke(SwiftlyAdError.SdkError(error.message, error.code))
            reload()
        }
    }

    private fun reload() = loadAd()
}
