package io.github.akashboghani.swiftlyads.compose

import android.os.Bundle
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import io.github.akashboghani.swiftlyads.SwiftlyAds
import io.github.akashboghani.swiftlyads.config.SwiftlyBannerAdFormat
import io.github.akashboghani.swiftlyads.error.SwiftlyAdError

/**
 * A banner ad Composable. Mirrors iOS `makeBannerView(format:isCollapsible:onShow:)`.
 *
 * Renders nothing when ads are disabled, no banner ad unit ID is configured, consent has not been
 * obtained, or the ad fails to load.
 *
 * @param format banner size
 * @param isCollapsible request a collapsible banner anchored to the bottom
 * @param onShow invoked when the banner is shown
 * @param onFailed invoked if the banner fails to load
 */
@Composable
fun SwiftlyBannerAd(
    format: SwiftlyBannerAdFormat,
    modifier: Modifier = Modifier,
    isCollapsible: Boolean = false,
    onShow: (() -> Unit)? = null,
    onFailed: ((Throwable) -> Unit)? = null,
) {
    val unitId = SwiftlyAds.bannerAdUnitIdIfAvailable() ?: return
    val context = LocalContext.current
    val adSize = remember(format) { format.adSize(context) }

    var failed by remember(unitId, format, isCollapsible) { mutableStateOf(false) }
    if (failed) return

    AndroidView(
        modifier = modifier.fillMaxWidth().wrapContentHeight(),
        factory = { ctx ->
            AdView(ctx).apply {
                setAdSize(adSize)
                adUnitId = unitId
                adListener = object : AdListener() {
                    override fun onAdLoaded() {
                        onShow?.invoke()
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        failed = true
                        onFailed?.invoke(SwiftlyAdError.SdkError(error.message, error.code))
                    }
                }
                loadAd(bannerAdRequest(isCollapsible))
            }
        },
        onRelease = { it.destroy() },
    )
}

private fun bannerAdRequest(isCollapsible: Boolean): AdRequest {
    val builder = AdRequest.Builder()
    if (isCollapsible) {
        val extras = Bundle().apply { putString("collapsible", "bottom") }
        builder.addNetworkExtrasBundle(AdMobAdapter::class.java, extras)
    }
    return builder.build()
}
