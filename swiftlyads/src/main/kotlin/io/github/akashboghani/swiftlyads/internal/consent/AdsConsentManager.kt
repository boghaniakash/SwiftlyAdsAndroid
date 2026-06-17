package io.github.akashboghani.swiftlyads.internal.consent

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import io.github.akashboghani.swiftlyads.config.SwiftlyAdEnvironment
import io.github.akashboghani.swiftlyads.config.SwiftlyConsentStatus
import io.github.akashboghani.swiftlyads.error.SwiftlyAdError
import io.github.akashboghani.swiftlyads.internal.SwiftlyAdsConfigurationProvider

/**
 * Drives the Google User Messaging Platform (UMP) consent flow. Mirrors iOS `AdsConsentManager`.
 */
internal class AdsConsentManager(
    private val appContext: Context,
    private val configuration: SwiftlyAdsConfigurationProvider,
) {
    private val consentInformation: ConsentInformation =
        UserMessagingPlatform.getConsentInformation(appContext)

    val consentStatus: SwiftlyConsentStatus
        get() = SwiftlyConsentStatus.from(consentInformation.consentStatus)

    /**
     * Requests a consent info update and shows the consent form if required.
     * [onComplete] receives `null` on success or a [SwiftlyAdError.ConsentError] on failure.
     */
    fun request(activity: Activity, onComplete: (Throwable?) -> Unit) {
        val paramsBuilder = ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(configuration.isTaggedForUnderAgeOfConsent ?: false)

        if (configuration.environment == SwiftlyAdEnvironment.DEVELOPMENT) {
            val debugBuilder = ConsentDebugSettings.Builder(appContext)
                .setDebugGeography(configuration.geography.toUmpValue())
            configuration.testDeviceIdentifiers.forEach { debugBuilder.addTestDeviceHashedId(it) }
            paramsBuilder.setConsentDebugSettings(debugBuilder.build())
            if (configuration.resetsConsentOnLaunch) consentInformation.reset()
        }

        consentInformation.requestConsentInfoUpdate(
            activity,
            paramsBuilder.build(),
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    configureForStatus()
                    if (formError != null) {
                        onComplete(SwiftlyAdError.ConsentError(formError.message, formError.errorCode))
                    } else {
                        onComplete(null)
                    }
                }
            },
            { requestError ->
                onComplete(SwiftlyAdError.ConsentError(requestError.message, requestError.errorCode))
            },
        )
    }

    private fun configureForStatus() {
        val status = consentStatus
        if (status == SwiftlyConsentStatus.NOT_REQUIRED) return

        val underAge = configuration.isTaggedForUnderAgeOfConsent ?: false
        configuration.mediationConfigurator?.updateGDPR(status, underAge)

        if (configuration.isTaggedForChildDirectedTreatment == true) return

        val tag = if (underAge) {
            RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE
        } else {
            RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_FALSE
        }
        MobileAds.setRequestConfiguration(
            MobileAds.getRequestConfiguration().toBuilder()
                .setTagForUnderAgeOfConsent(tag)
                .build(),
        )
    }
}
