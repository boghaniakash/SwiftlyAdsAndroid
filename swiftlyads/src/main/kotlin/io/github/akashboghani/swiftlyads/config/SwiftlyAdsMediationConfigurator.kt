package io.github.akashboghani.swiftlyads.config

/**
 * Forward COPPA and GDPR signals to third-party mediation adapters.
 * Mirrors the iOS `SwiftlyAdsMediationConfigurator` protocol.
 */
interface SwiftlyAdsMediationConfigurator {
    fun updateCOPPA(isTaggedForChildDirectedTreatment: Boolean)
    fun updateGDPR(consentStatus: SwiftlyConsentStatus, isTaggedForUnderAgeOfConsent: Boolean)
}
