package io.github.akashboghani.swiftlyads.compose

import android.view.View
import android.widget.FrameLayout
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import io.github.akashboghani.swiftlyads.config.SwiftlyNativeAdElement

/**
 * Registers, sizes and positions transparent "hit-target" Views inside a [NativeAdView] so that
 * Google Mobile Ads can attribute clicks/impressions to Compose-rendered ad assets.
 *
 * This mirrors the iOS implementation, which overlays an invisible `UINativeAdView` over the
 * SwiftUI content. Text/button/icon assets get an invisible [View] overlay; the media asset is
 * registered using the real [MediaView] created by [SwiftlyMediaView].
 */
internal class NativeAdElementRegistrar(private val nativeAdView: NativeAdView) {

    private val overlays = mutableMapOf<SwiftlyNativeAdElement, View>()

    /**
     * The ad currently bound to the view. Retained so we can re-bind once the [MediaView] is
     * registered — `setNativeAd` only attaches the video stream to a media view that is already
     * set on the [NativeAdView].
     */
    private var boundAd: NativeAd? = null

    /**
     * Binds [ad] to the underlying [NativeAdView]. Safe to call repeatedly; the Google Mobile Ads
     * SDK re-populates the view's assets each time.
     */
    fun bindAd(ad: NativeAd) {
        boundAd = ad
        nativeAdView.setNativeAd(ad)
    }

    /** Called by [SwiftlyMediaView] to register its real [MediaView] as the media asset. */
    fun registerMediaView(mediaView: MediaView) {
        if (nativeAdView.mediaView !== mediaView) {
            nativeAdView.mediaView = mediaView
            // The Compose content (and thus this MediaView) is laid out on a later frame than the
            // initial setNativeAd call, so re-bind now that the media view exists — otherwise the
            // ad's video has no surface to play on and only a static image (or nothing) shows.
            boundAd?.let { nativeAdView.setNativeAd(it) }
        }
    }

    /** Reports the latest bounds (in root pixels) of a tagged Composable element. */
    fun updateBounds(element: SwiftlyNativeAdElement, bounds: Rect) {
        if (element == SwiftlyNativeAdElement.MEDIA) return // handled by registerMediaView
        nativeAdView.post {
            val view = overlays.getOrPut(element) {
                View(nativeAdView.context).also { v ->
                    v.isClickable = false
                    nativeAdView.addView(v)
                    assignToSlot(element, v)
                }
            }
            val lp = (view.layoutParams as? FrameLayout.LayoutParams)
                ?: FrameLayout.LayoutParams(0, 0)
            lp.width = bounds.width.toInt().coerceAtLeast(0)
            lp.height = bounds.height.toInt().coerceAtLeast(0)
            view.layoutParams = lp
            view.x = bounds.left
            view.y = bounds.top
        }
    }

    private fun assignToSlot(element: SwiftlyNativeAdElement, view: View) {
        when (element) {
            SwiftlyNativeAdElement.HEADLINE -> nativeAdView.headlineView = view
            SwiftlyNativeAdElement.CALL_TO_ACTION -> nativeAdView.callToActionView = view
            SwiftlyNativeAdElement.ICON -> nativeAdView.iconView = view
            SwiftlyNativeAdElement.BODY -> nativeAdView.bodyView = view
            SwiftlyNativeAdElement.STORE -> nativeAdView.storeView = view
            SwiftlyNativeAdElement.PRICE -> nativeAdView.priceView = view
            SwiftlyNativeAdElement.IMAGE -> nativeAdView.imageView = view
            SwiftlyNativeAdElement.STAR_RATING -> nativeAdView.starRatingView = view
            SwiftlyNativeAdElement.ADVERTISER -> nativeAdView.advertiserView = view
            SwiftlyNativeAdElement.MEDIA -> Unit
            SwiftlyNativeAdElement.AD_CHOICES -> Unit // AdChoices is rendered automatically
        }
    }
}

internal val LocalNativeAdElementRegistrar = staticCompositionLocalOf<NativeAdElementRegistrar?> { null }

/**
 * Tags this Composable as the given native ad [element]. Use inside the `content` of a
 * [SwiftlyNativeAdView] so the ad SDK can attribute clicks to it. Mirrors iOS `.nativeAdElement(_:)`.
 */
fun Modifier.nativeAdElement(element: SwiftlyNativeAdElement): Modifier = composed {
    val registrar = LocalNativeAdElementRegistrar.current
    onGloballyPositioned { coordinates ->
        registrar?.updateBounds(element, coordinates.boundsInRoot())
    }
}
