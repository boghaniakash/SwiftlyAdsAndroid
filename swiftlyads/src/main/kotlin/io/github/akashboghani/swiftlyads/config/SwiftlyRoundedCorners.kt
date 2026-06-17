package io.github.akashboghani.swiftlyads.config

/**
 * A bit-set describing which corners of a native ad element should be rounded.
 * Mirrors iOS `SwiftlyRoundedCorners` (an `OptionSet`).
 *
 * Combine with [or], e.g. `SwiftlyRoundedCorners.TopLeft or SwiftlyRoundedCorners.TopRight`.
 */
@JvmInline
value class SwiftlyRoundedCorners(val rawValue: Int) {

    infix fun or(other: SwiftlyRoundedCorners): SwiftlyRoundedCorners =
        SwiftlyRoundedCorners(rawValue or other.rawValue)

    fun contains(other: SwiftlyRoundedCorners): Boolean =
        (rawValue and other.rawValue) == other.rawValue

    companion object {
        val TopLeft = SwiftlyRoundedCorners(1 shl 0)
        val TopRight = SwiftlyRoundedCorners(1 shl 1)
        val BottomLeft = SwiftlyRoundedCorners(1 shl 2)
        val BottomRight = SwiftlyRoundedCorners(1 shl 3)

        val Top = TopLeft or TopRight
        val Bottom = BottomLeft or BottomRight
        val Left = TopLeft or BottomLeft
        val Right = TopRight or BottomRight
        val AllCorners = TopLeft or TopRight or BottomLeft or BottomRight
    }
}
