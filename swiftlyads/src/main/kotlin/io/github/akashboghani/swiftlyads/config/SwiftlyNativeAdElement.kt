package io.github.akashboghani.swiftlyads.config

/**
 * Tags a Composable inside a [io.github.akashboghani.swiftlyads.compose.SwiftlyNativeAdView]
 * so the underlying `NativeAdView` can register it as the matching ad asset for click
 * attribution. Mirrors iOS `SwiftlyNativeAdElement`.
 */
enum class SwiftlyNativeAdElement {
    HEADLINE,
    CALL_TO_ACTION,
    ICON,
    BODY,
    STORE,
    PRICE,
    IMAGE,
    STAR_RATING,
    ADVERTISER,
    MEDIA,
    AD_CHOICES,
}
