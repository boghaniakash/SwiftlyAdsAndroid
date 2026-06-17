package io.github.akashboghani.swiftlyads.config

import android.content.Context
import com.google.android.gms.ads.AdSize

/**
 * Banner ad sizes. Mirrors iOS `SwiftlyBannerAdFormat`.
 *
 * [ADAPTIVE_BANNER] is sized to the current screen width, matching the iOS behaviour.
 */
enum class SwiftlyBannerAdFormat {
    STANDARD_BANNER,
    LARGE_BANNER,
    MEDIUM_RECTANGLE,
    FULL_BANNER,
    LEADERBOARD,
    SKYSCRAPER,
    FLUID,
    ADAPTIVE_BANNER,
    ;

    internal fun adSize(context: Context): AdSize = when (this) {
        STANDARD_BANNER -> AdSize.BANNER
        LARGE_BANNER -> AdSize.LARGE_BANNER
        MEDIUM_RECTANGLE -> AdSize.MEDIUM_RECTANGLE
        FULL_BANNER -> AdSize.FULL_BANNER
        LEADERBOARD -> AdSize.LEADERBOARD
        SKYSCRAPER -> AdSize.WIDE_SKYSCRAPER
        FLUID -> AdSize.FLUID
        ADAPTIVE_BANNER -> {
            val metrics = context.resources.displayMetrics
            val widthDp = (metrics.widthPixels / metrics.density).toInt()
            AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp)
        }
    }
}
