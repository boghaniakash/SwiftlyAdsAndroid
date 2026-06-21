package io.github.akashboghani.swiftlyads.internal.managers

import android.content.Context
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import io.github.akashboghani.swiftlyads.config.SwiftlyMediaAspectRatio
import io.github.akashboghani.swiftlyads.error.SwiftlyAdError
import io.github.akashboghani.swiftlyads.presentation.SwiftlyNativeAd

/**
 * Loads and caches native ads. Mirrors iOS `NativeAdManager`.
 *
 * Note: cached ads are not explicitly `destroy()`-ed on eviction because they may still be
 * displayed (mirroring the iOS ARC behaviour). Call `NativeAd.destroy()` yourself once you are
 * permanently done with an ad if you want to free its resources eagerly.
 */
internal class NativeAdManager(
    private val appContext: Context,
    private val adUnitId: String,
    private val adRequestProvider: () -> AdRequest,
    private val activePresentation: SwiftlyNativeAd,
    private val mediaAspectRatio: SwiftlyMediaAspectRatio?,
) {
    private var adLoader: AdLoader? = null
    private val preLoadedAds = ArrayDeque<NativeAd>()
    private var cachedAspectRatio: SwiftlyMediaAspectRatio? = null
    private val adLimit = 1
    private var isPreloadAds = false

    val isReady: Boolean get() = preLoadedAds.isNotEmpty()

    fun loadAd(isPreloadAds: Boolean, mediaAspectRatioOverride: SwiftlyMediaAspectRatio? = null) {
        this.isPreloadAds = isPreloadAds
        val ratio = mediaAspectRatioOverride ?: mediaAspectRatio
        cachedAspectRatio = ratio

        val builder = AdLoader.Builder(appContext, adUnitId)
            .forNativeAd { nativeAd ->
                if (this.isPreloadAds) {
                    val wasEmpty = preLoadedAds.isEmpty()
                    cache(nativeAd)
                    if (wasEmpty) activePresentation.onReceiveAdCallback?.invoke(nativeAd)
                } else {
                    activePresentation.onReceiveAdCallback?.invoke(nativeAd)
                }
            }
            .withAdListener(object : AdListener() {
                override fun onAdLoaded() {
                    activePresentation.onAdLoadedCallback?.invoke()
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    activePresentation.onErrorCallback?.invoke(
                        SwiftlyAdError.SdkError(error.message, error.code),
                    )
                }
            })

        if (ratio != null) {
            val options = NativeAdOptions.Builder()
                .setMediaAspectRatio(ratio.toNativeAdOptionsValue())
                .build()
            builder.withNativeAdOptions(options)
        }

        adLoader = builder.build()
        adLoader?.loadAd(adRequestProvider())
    }

    fun stopLoading() {
        adLoader = null
        preLoadedAds.clear()
    }

    /**
     * Returns a cached ad when preloading and the aspect ratio matches; otherwise triggers a
     * fresh load and returns null. Mirrors iOS `getNextAd`.
     */
    fun getNextAd(mediaAspectRatioOverride: SwiftlyMediaAspectRatio? = null): NativeAd? {
        val requested = mediaAspectRatioOverride ?: mediaAspectRatio
        if (isPreloadAds && preLoadedAds.isNotEmpty() && requested == cachedAspectRatio) {
            return preLoadedAds.last()
        }
        if (requested != cachedAspectRatio) preLoadedAds.clear()
        loadAd(isPreloadAds, mediaAspectRatioOverride)
        return null
    }

    private fun cache(ad: NativeAd) {
        if (preLoadedAds.size >= adLimit) preLoadedAds.removeFirst()
        preLoadedAds.addLast(ad)
    }
}
