package io.github.akashboghani.swiftlyads.compose

import android.graphics.Outline
import android.view.View
import android.view.ViewOutlineProvider
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.nativead.MediaView
import io.github.akashboghani.swiftlyads.SwiftlyNativeAds

/**
 * Displays the media (image/video) asset of a native ad and registers it with the enclosing
 * [SwiftlyNativeAdView]. Mirrors iOS `SwiftlyMediaView`.
 *
 * Must be used inside a [SwiftlyNativeAdView]'s `content`.
 *
 * @param keepAspectRatio when true (default), the view is constrained to the media's aspect ratio
 * @param cornerRadius rounds the corners of the media (both image and video). Defaults to 0.dp
 */
@Composable
fun SwiftlyMediaView(
    nativeAd: SwiftlyNativeAds?,
    modifier: Modifier = Modifier,
    keepAspectRatio: Boolean = true,
    cornerRadius: Dp = 0.dp,
) {
    val registrar = LocalNativeAdElementRegistrar.current
    val mediaContent = nativeAd?.mediaContent
    val aspect = mediaContent?.aspectRatio?.takeIf { it > 0f }

    // Always occupy the full width of the parent; height follows the media's aspect ratio when
    // keepAspectRatio is on.
    val sizedModifier = if (keepAspectRatio && aspect != null) {
        modifier.fillMaxWidth().aspectRatio(aspect)
    } else {
        modifier.fillMaxWidth()
    }

    // Clip on the Android View via an outline so the rounding also applies to the video surface —
    // a Compose Modifier.clip would not clip the MediaView's underlying video content.
    val radiusPx = with(LocalDensity.current) { cornerRadius.toPx() }

    AndroidView(
        modifier = sizedModifier,
        factory = { ctx ->
            MediaView(ctx).also { registrar?.registerMediaView(it) }
        },
        update = { mediaView ->
            registrar?.registerMediaView(mediaView)
            if (mediaContent != null) mediaView.mediaContent = mediaContent
            mediaView.clipToOutline = radiusPx > 0f
            mediaView.outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, radiusPx)
                }
            }
            mediaView.invalidateOutline()
        },
    )
}
