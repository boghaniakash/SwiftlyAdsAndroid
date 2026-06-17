package io.github.akashboghani.swiftlyads.config

import com.google.android.ump.ConsentInformation

/** UMP consent status, mirroring iOS `ConsentStatus`. */
enum class SwiftlyConsentStatus {
    UNKNOWN,
    NOT_REQUIRED,
    REQUIRED,
    OBTAINED,
    ;

    internal companion object {
        fun from(value: Int): SwiftlyConsentStatus = when (value) {
            ConsentInformation.ConsentStatus.NOT_REQUIRED -> NOT_REQUIRED
            ConsentInformation.ConsentStatus.REQUIRED -> REQUIRED
            ConsentInformation.ConsentStatus.OBTAINED -> OBTAINED
            else -> UNKNOWN
        }
    }
}
