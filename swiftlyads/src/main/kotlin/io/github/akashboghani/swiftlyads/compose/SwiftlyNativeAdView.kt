package io.github.akashboghani.swiftlyads.compose

import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.nativead.NativeAdView
import io.github.akashboghani.swiftlyads.SwiftlyNativeAds

/**
 * Renders a fully custom native ad layout. Mirrors iOS `SwiftlyNativeAdView`.
 *
 * Build any Compose layout in [content] and tag each asset with [Modifier.nativeAdElement] so the
 * underlying `NativeAdView` can attribute clicks. Use [SwiftlyMediaView] for the media asset.
 *
 * ```
 * SwiftlyNativeAdView(nativeAd) { ad ->
 *     Column {
 *         Text(ad?.headline ?: "", Modifier.nativeAdElement(SwiftlyNativeAdElement.HEADLINE))
 *         SwiftlyMediaView(ad, Modifier.fillMaxWidth())
 *         Button(onClick = {}, Modifier.nativeAdElement(SwiftlyNativeAdElement.CALL_TO_ACTION)) {
 *             Text(ad?.callToAction ?: "Learn more")
 *         }
 *     }
 * }
 * ```
 */
@Composable
fun SwiftlyNativeAdView(
    nativeAd: SwiftlyNativeAds?,
    modifier: Modifier = Modifier,
    content: @Composable (SwiftlyNativeAds?) -> Unit,
) {
    val density = LocalDensity.current
    // The content lives in a ComposeView nested inside the NativeAdView, which composes
    // asynchronously. A bare AndroidView won't re-measure to fit it on its own — it grows but
    // never shrinks below its previous height when a smaller creative loads. So we drive the
    // host height explicitly from the content's measured height.
    var contentHeightPx by remember { mutableStateOf(0) }

    AndroidView(
        modifier = if (contentHeightPx > 0) {
            modifier.height(with(density) { contentHeightPx.toDp() })
        } else {
            modifier
        },
        factory = { ctx ->
            val adView = NativeAdView(ctx)
            val composeView = ComposeView(ctx)
            adView.addView(
                composeView,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ),
            )
            // Whenever the content is laid out, measure it with an UNSPECIFIED height to learn its
            // natural height — independent of whatever height is currently imposed on the host — and
            // feed that back so the card both grows AND shrinks with the ad (media shown/hidden, a
            // taller or shorter creative loading, etc.).
            composeView.addOnLayoutChangeListener { v, _, _, _, _, _, _, _, _ ->
                v.measure(
                    View.MeasureSpec.makeMeasureSpec(v.width, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                )
                val natural = v.measuredHeight
                if (natural > 0 && natural != contentHeightPx) contentHeightPx = natural
            }
            adView.tag = NativeAdViewHolder(composeView, NativeAdElementRegistrar(adView))
            adView
        },
        update = { adView ->
            val holder = adView.tag as NativeAdViewHolder
            holder.composeView.setContent {
                CompositionLocalProvider(LocalNativeAdElementRegistrar provides holder.registrar) {
                    content(nativeAd)
                }
            }
            if (nativeAd != null) holder.registrar.bindAd(nativeAd)
        },
    )
}

private class NativeAdViewHolder(
    val composeView: ComposeView,
    val registrar: NativeAdElementRegistrar,
)
