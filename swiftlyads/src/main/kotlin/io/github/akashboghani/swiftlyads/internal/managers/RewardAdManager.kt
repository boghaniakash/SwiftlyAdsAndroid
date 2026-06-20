package io.github.akashboghani.swiftlyads.internal.managers

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import io.github.akashboghani.swiftlyads.error.SwiftlyAdError
import io.github.akashboghani.swiftlyads.internal.MainDispatch
import io.github.akashboghani.swiftlyads.presentation.SwiftlyRewardAd

/**
 * Manages the rewarded ad lifecycle. Mirrors iOS `RewardAdManager`.
 *
 * The presentation is supplied per `show` call (see [InterAdManager] for the rationale);
 * [activePresentation] tracks the show currently in flight and receives the reward callback.
 */
internal class RewardAdManager(
    private val appContext: Context,
    private val adUnitId: String,
    private val adRequestProvider: () -> AdRequest,
) {
    private var rewardedAd: RewardedAd? = null
    private var isLoading = false
    private var activePresentation: SwiftlyRewardAd? = null

    val isReady: Boolean get() = rewardedAd != null

    fun loadAd() {
        if (rewardedAd != null || isLoading) return
        isLoading = true
        RewardedAd.load(
            appContext,
            adUnitId,
            adRequestProvider(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    isLoading = false
                    ad.fullScreenContentCallback = fullScreenCallback()
                    rewardedAd = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    isLoading = false
                    rewardedAd = null
                }
            },
        )
    }

    fun stopLoading() {
        rewardedAd?.fullScreenContentCallback = null
        rewardedAd = null
    }

    fun show(activity: Activity, presentation: SwiftlyRewardAd) {
        activePresentation = presentation
        val ad = rewardedAd
        if (ad != null) {
            ad.show(activity) { rewardItem ->
                presentation.onRewardCallback?.invoke(rewardItem.amount)
            }
        } else {
            reload()
            MainDispatch.nextTick {
                presentation.onErrorCallback?.invoke(SwiftlyAdError.RewardedAdNotLoaded)
            }
        }
    }

    fun loadAndShow(activity: Activity, presentation: SwiftlyRewardAd) {
        activePresentation = presentation
        if (rewardedAd != null) {
            show(activity, presentation)
            return
        }
        isLoading = true
        RewardedAd.load(
            appContext,
            adUnitId,
            adRequestProvider(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    isLoading = false
                    ad.fullScreenContentCallback = fullScreenCallback()
                    rewardedAd = ad
                    ad.show(activity) { rewardItem ->
                        presentation.onRewardCallback?.invoke(rewardItem.amount)
                    }
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    isLoading = false
                    rewardedAd = null
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
            rewardedAd = null
            activePresentation?.onCloseCallback?.invoke()
            reload()
        }

        override fun onAdFailedToShowFullScreenContent(error: AdError) {
            rewardedAd = null
            activePresentation?.onErrorCallback?.invoke(SwiftlyAdError.SdkError(error.message, error.code))
            reload()
        }
    }

    private fun reload() = loadAd()
}
