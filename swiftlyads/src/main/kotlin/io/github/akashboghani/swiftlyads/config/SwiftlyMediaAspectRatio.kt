package io.github.akashboghani.swiftlyads.config

import com.google.android.gms.ads.nativead.NativeAdOptions

/**
 * Preferred aspect ratio for the media (image/video) asset of a native ad.
 *
 * Mirrors `SwiftlyMediaAspectRatio` from the iOS library.
 */
enum class SwiftlyMediaAspectRatio {
    ANY,
    LANDSCAPE,
    PORTRAIT,
    SQUARE,
    ;

    internal fun toNativeAdOptionsValue(): Int = when (this) {
        ANY -> NativeAdOptions.NATIVE_MEDIA_ASPECT_RATIO_ANY
        LANDSCAPE -> NativeAdOptions.NATIVE_MEDIA_ASPECT_RATIO_LANDSCAPE
        PORTRAIT -> NativeAdOptions.NATIVE_MEDIA_ASPECT_RATIO_PORTRAIT
        SQUARE -> NativeAdOptions.NATIVE_MEDIA_ASPECT_RATIO_SQUARE
    }
}
