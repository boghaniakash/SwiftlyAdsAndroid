package io.github.akashboghani.swiftlyads.config

import com.google.android.ump.ConsentDebugSettings

/**
 * Debug geography used to simulate a user's region while testing the UMP consent flow.
 * Only honoured in [SwiftlyAdEnvironment.DEVELOPMENT]. Mirrors iOS `SwiftlyDebugGeography`.
 */
enum class SwiftlyDebugGeography {
    DISABLED,
    EEA,
    NOT_EEA,
    REGULATED_US_STATE,
    OTHER,
    ;

    @Suppress("DEPRECATION") // DEBUG_GEOGRAPHY_NOT_EEA is deprecated in UMP but still valid.
    internal fun toUmpValue(): Int = when (this) {
        DISABLED -> ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_DISABLED
        EEA -> ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA
        NOT_EEA -> ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_NOT_EEA
        REGULATED_US_STATE -> ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_REGULATED_US_STATE
        OTHER -> ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_OTHER
    }
}
