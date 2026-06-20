package io.github.akashboghani.swiftlyads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import io.github.akashboghani.swiftlyads.config.SwiftlyAdsConfiguration
import io.github.akashboghani.swiftlyads.config.SwiftlyConsentStatus
import io.github.akashboghani.swiftlyads.config.SwiftlyMediaAspectRatio
import io.github.akashboghani.swiftlyads.error.SwiftlyAdError
import io.github.akashboghani.swiftlyads.internal.MainDispatch
import io.github.akashboghani.swiftlyads.internal.SwiftlyAdsConfigurationProvider
import io.github.akashboghani.swiftlyads.internal.consent.AdsConsentManager
import io.github.akashboghani.swiftlyads.internal.managers.AppOpenAdManager
import io.github.akashboghani.swiftlyads.internal.managers.InterAdManager
import io.github.akashboghani.swiftlyads.internal.managers.NativeAdManager
import io.github.akashboghani.swiftlyads.internal.managers.RewardAdManager
import io.github.akashboghani.swiftlyads.internal.managers.RewardInterAdManager
import io.github.akashboghani.swiftlyads.presentation.SwiftlyAdLoadPresentation
import io.github.akashboghani.swiftlyads.presentation.SwiftlyAppOpenAd
import io.github.akashboghani.swiftlyads.presentation.SwiftlyInterAd
import io.github.akashboghani.swiftlyads.presentation.SwiftlyNativeAd
import io.github.akashboghani.swiftlyads.presentation.SwiftlyRewardAd
import io.github.akashboghani.swiftlyads.presentation.SwiftlyRewardInterAd

/**
 * Singleton entry point for SwiftlyAds — the Jetpack Compose / Android port of the
 * SwiftlyAds Swift package. Configure once, then show ads anywhere.
 *
 * Unlike the iOS API, [configure] takes a [Context] (Android needs one to load ads) and the
 * full-screen `show*` methods take an [Activity] (instead of a `UIViewController`). Banner and
 * native ads are exposed as Composables in the `io.github.akashboghani.swiftlyads.compose` package.
 *
 * Each `show*` / `request*` call returns a **fresh** presentation object, so callbacks attached to
 * one call never clobber another's. A coroutine-friendly surface (`suspend` + `Flow`) is available
 * as extension functions in `SwiftlyAdsCoroutines.kt`.
 */
object SwiftlyAds {

    private var appContext: Context? = null
    private var configuration: SwiftlyAdsConfigurationProvider? = null

    private var interstitialAd: InterAdManager? = null
    private var appOpenAd: AppOpenAdManager? = null
    private var rewardAd: RewardAdManager? = null
    private var rewardInterAd: RewardInterAdManager? = null
    private var nativeAd: NativeAdManager? = null
    private var consentManager: AdsConsentManager? = null

    private var disabled = false
    private var hasInitializedMobileAds = false
    private var interAdCounter = 0
    private var appOpenAdCounter = 0
    private var nativeAdCounter = 0
    private var showConsent = false

    private val hasConsent: Boolean
        get() = if (showConsent) {
            when (consentManager?.consentStatus) {
                SwiftlyConsentStatus.NOT_REQUIRED, SwiftlyConsentStatus.OBTAINED -> true
                else -> false
            }
        } else {
            true
        }

    // region Public API

    /**
     * Configures SwiftlyAds. Call once at app launch (e.g. in `Application.onCreate`).
     * Creates an ad manager for every ad unit ID present in [configuration].
     */
    fun configure(context: Context, configuration: SwiftlyAdsConfiguration) {
        val ctx = context.applicationContext
        appContext = ctx
        val provider = configuration.build()
        this.configuration = provider

        val requestProvider = { buildAdRequest() }

        interstitialAd = provider.interstitialAdUnitId
            ?.let { InterAdManager(ctx, it, requestProvider) }
        appOpenAd = provider.appOpenAdUnitId
            ?.let { AppOpenAdManager(ctx, it, requestProvider) }
        rewardAd = provider.rewardedAdUnitId
            ?.let { RewardAdManager(ctx, it, requestProvider) }
        rewardInterAd = provider.rewardedInterstitialAdUnitId
            ?.let { RewardInterAdManager(ctx, it, requestProvider) }
        nativeAd = provider.nativeAdUnitId
            ?.let { NativeAdManager(ctx, it, requestProvider, provider.nativeAdMediaAspectRatio) }

        provider.isTaggedForChildDirectedTreatment?.let { coppa ->
            provider.mediationConfigurator?.updateCOPPA(coppa)
            val tag = if (coppa) {
                RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE
            } else {
                RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE
            }
            MobileAds.setRequestConfiguration(
                MobileAds.getRequestConfiguration().toBuilder()
                    .setTagForChildDirectedTreatment(tag)
                    .build(),
            )
        }

        if (provider.testDeviceIdentifiers.isNotEmpty()) {
            MobileAds.setRequestConfiguration(
                MobileAds.getRequestConfiguration().toBuilder()
                    .setTestDeviceIds(provider.testDeviceIdentifiers)
                    .build(),
            )
        }

        consentManager = AdsConsentManager(ctx, provider)
    }

    /**
     * Initializes the Mobile Ads SDK (and runs the UMP consent flow when [showConsent] is true).
     * Returns immediately with a fluent presentation; attach `.onSuccess { }` / `.onError { }`.
     */
    fun initializeIfNeeded(activity: Activity, showConsent: Boolean = true): SwiftlyAdLoadPresentation {
        val presentation = SwiftlyAdLoadPresentation()
        if (hasInitializedMobileAds) {
            MainDispatch.nextTick { presentation.onSuccessCallback?.invoke() }
            return presentation
        }
        this.showConsent = showConsent

        val proceed = {
            // MobileAds.initialize() does heavy synchronous work on first call and must not run
            // on the main thread, or it blocks the UI and triggers an ANR (Google's docs require
            // a background thread). The completion listener is delivered back on the main thread.
            MainDispatch.background {
                MobileAds.initialize(activity.applicationContext) {
                    hasInitializedMobileAds = true
                    if (configuration?.preloadsAds == true) loadAds()
                    MainDispatch.nextTick { presentation.onSuccessCallback?.invoke() }
                }
            }
        }

        val cm = consentManager
        if (showConsent && cm != null) {
            cm.request(activity) { error ->
                if (error != null) {
                    MainDispatch.nextTick { presentation.onErrorCallback?.invoke(error) }
                } else {
                    proceed()
                }
            }
        } else {
            proceed()
        }
        return presentation
    }

    /** Stops all managers, re-applies an updated configuration, and re-preloads if enabled. */
    fun updateConfiguration(updates: (SwiftlyAdsConfiguration) -> SwiftlyAdsConfiguration) {
        interstitialAd?.stopLoading()
        appOpenAd?.stopLoading()
        rewardAd?.stopLoading()
        rewardInterAd?.stopLoading()
        nativeAd?.stopLoading()

        val ctx = appContext ?: return
        val newConfig = updates(currentConfiguration())
        configure(ctx, newConfig)
        if (configuration?.preloadsAds == true) loadAds()
    }

    /** Shows an interstitial ad. Honours frequency capping unless [bypassingFrequencyLimit]. */
    fun showInterstitialAd(activity: Activity, bypassingFrequencyLimit: Boolean = false): SwiftlyInterAd {
        val presentation = SwiftlyInterAd()
        if (disabled) {
            MainDispatch.nextTick { presentation.onCloseCallback?.invoke() }
            return presentation
        }
        val manager = interstitialAd ?: run {
            MainDispatch.nextTick { presentation.onErrorCallback?.invoke(SwiftlyAdError.InterstitialAdUnitIdNotSet) }
            return presentation
        }
        if (!bypassingFrequencyLimit && interAdCounter % (configuration?.interAdShowCount ?: 1) != 0) {
            interAdCounter++
            MainDispatch.nextTick { presentation.onCloseCallback?.invoke() }
            return presentation
        }
        if (!hasConsent) {
            MainDispatch.nextTick { presentation.onErrorCallback?.invoke(SwiftlyAdError.ConsentNotObtained) }
            return presentation
        }
        if (configuration?.preloadsAds == false) manager.loadAndShow(activity, presentation) else manager.show(activity, presentation)
        if (!bypassingFrequencyLimit) interAdCounter++
        return presentation
    }

    /** Shows an app open ad. Honours frequency capping unless [bypassingFrequencyLimit]. */
    fun showAppOpenAd(activity: Activity, bypassingFrequencyLimit: Boolean = false): SwiftlyAppOpenAd {
        val presentation = SwiftlyAppOpenAd()
        if (disabled) {
            MainDispatch.nextTick { presentation.onCloseCallback?.invoke() }
            return presentation
        }
        val manager = appOpenAd ?: run {
            MainDispatch.nextTick { presentation.onErrorCallback?.invoke(SwiftlyAdError.AppOpenAdUnitIdNotSet) }
            return presentation
        }
        if (!bypassingFrequencyLimit && appOpenAdCounter % (configuration?.appOpenAdShowCount ?: 1) != 0) {
            appOpenAdCounter++
            MainDispatch.nextTick { presentation.onCloseCallback?.invoke() }
            return presentation
        }
        if (!hasConsent) {
            MainDispatch.nextTick { presentation.onErrorCallback?.invoke(SwiftlyAdError.ConsentNotObtained) }
            return presentation
        }
        if (configuration?.preloadsAds == false) manager.loadAndShow(activity, presentation) else manager.show(activity, presentation)
        if (!bypassingFrequencyLimit) appOpenAdCounter++
        return presentation
    }

    /** Shows a rewarded ad. */
    fun showRewardAd(activity: Activity): SwiftlyRewardAd {
        val presentation = SwiftlyRewardAd()
        if (disabled) {
            MainDispatch.nextTick { presentation.onCloseCallback?.invoke() }
            return presentation
        }
        val manager = rewardAd ?: run {
            MainDispatch.nextTick { presentation.onErrorCallback?.invoke(SwiftlyAdError.RewardedAdUnitIdNotSet) }
            return presentation
        }
        if (!hasConsent) {
            MainDispatch.nextTick { presentation.onErrorCallback?.invoke(SwiftlyAdError.ConsentNotObtained) }
            return presentation
        }
        if (configuration?.preloadsAds == false) manager.loadAndShow(activity, presentation) else manager.show(activity, presentation)
        return presentation
    }

    /** Shows a rewarded interstitial ad. */
    fun showRewardInterAd(activity: Activity): SwiftlyRewardInterAd {
        val presentation = SwiftlyRewardInterAd()
        if (disabled) {
            MainDispatch.nextTick { presentation.onCloseCallback?.invoke() }
            return presentation
        }
        val manager = rewardInterAd ?: run {
            MainDispatch.nextTick { presentation.onErrorCallback?.invoke(SwiftlyAdError.RewardedInterAdUnitIdNotSet) }
            return presentation
        }
        if (!hasConsent) {
            MainDispatch.nextTick { presentation.onErrorCallback?.invoke(SwiftlyAdError.ConsentNotObtained) }
            return presentation
        }
        if (configuration?.preloadsAds == false) manager.loadAndShow(activity, presentation) else manager.show(activity, presentation)
        return presentation
    }

    /**
     * Requests a native ad. Delivers the ad through `onReceiveAd` (or `null` when capped/disabled).
     * Use the result with `SwiftlyNativeAdView` from the compose package.
     */
    fun requestNativeAd(
        bypassingFrequencyLimit: Boolean = false,
        mediaAspectRatio: SwiftlyMediaAspectRatio? = null,
    ): SwiftlyNativeAd {
        val presentation = SwiftlyNativeAd()
        if (disabled) {
            MainDispatch.nextTick { presentation.onReceiveAdCallback?.invoke(null) }
            return presentation
        }
        val manager = nativeAd ?: run {
            MainDispatch.nextTick { presentation.onErrorCallback?.invoke(SwiftlyAdError.NativeAdUnitIdNotSet) }
            return presentation
        }
        manager.activePresentation = presentation
        if (!bypassingFrequencyLimit && nativeAdCounter % (configuration?.nativeAdShowCount ?: 1) != 0) {
            nativeAdCounter++
            MainDispatch.nextTick { presentation.onReceiveAdCallback?.invoke(null) }
            return presentation
        }
        if (!hasConsent) {
            MainDispatch.nextTick { presentation.onErrorCallback?.invoke(SwiftlyAdError.ConsentNotObtained) }
            return presentation
        }
        MainDispatch.nextTick {
            presentation.onReceiveAdCallback?.invoke(manager.getNextAd(mediaAspectRatio))
            if (!bypassingFrequencyLimit) nativeAdCounter++
            if (configuration?.preloadsAds == true) manager.loadAd(true, mediaAspectRatio)
        }
        return presentation
    }

    /**
     * Runs the UMP consent flow on demand. [onComplete] receives the resulting consent status,
     * or a failure (e.g. [SwiftlyAdError.ConsentManagerNotAvailable]).
     */
    fun updateConsent(activity: Activity, onComplete: (Result<SwiftlyConsentStatus>) -> Unit) {
        val cm = consentManager ?: run {
            onComplete(Result.failure(SwiftlyAdError.ConsentManagerNotAvailable))
            return
        }
        cm.request(activity) { error ->
            if (error != null) onComplete(Result.failure(error)) else onComplete(Result.success(cm.consentStatus))
        }
    }

    /** Disables all ads (e.g. after a premium purchase) and stops every manager. */
    fun disable() {
        disabled = true
        interstitialAd?.stopLoading()
        rewardAd?.stopLoading()
        rewardInterAd?.stopLoading()
        appOpenAd?.stopLoading()
        nativeAd?.stopLoading()
    }

    /** Re-enables ads and reloads all configured ad types. */
    fun enable() {
        disabled = false
        loadAds()
    }

    // endregion

    // region Read-only state

    val consentStatus: SwiftlyConsentStatus
        get() = consentManager?.consentStatus ?: SwiftlyConsentStatus.NOT_REQUIRED
    val isInterstitialAdReady: Boolean get() = interstitialAd?.isReady ?: false
    val isAppOpenAdReady: Boolean get() = appOpenAd?.isReady ?: false
    val isRewardAdReady: Boolean get() = rewardAd?.isReady ?: false
    val isRewardInterAdReady: Boolean get() = rewardInterAd?.isReady ?: false
    val isDisabled: Boolean get() = disabled

    // endregion

    // region Deprecated aliases (parity with iOS naming history)

    @Deprecated("Renamed", ReplaceWith("requestNativeAd(skipCount)"))
    fun getNativeAd(skipCount: Boolean = false): SwiftlyNativeAd = requestNativeAd(skipCount)

    @Deprecated("Use disable() or enable() instead")
    fun setDisabled(isDisabled: Boolean) {
        if (isDisabled) disable() else enable()
    }

    // endregion

    // region Internal helpers used by Compose components

    /** Returns the banner ad unit ID if a banner may currently be shown, otherwise null. */
    internal fun bannerAdUnitIdIfAvailable(): String? {
        if (disabled) return null
        val unitId = configuration?.bannerAdUnitId ?: return null
        if (!hasConsent) return null
        return unitId
    }

    // endregion

    private fun buildAdRequest(): AdRequest = configuration?.adRequest ?: AdRequest.Builder().build()

    private fun loadAds() {
        if (disabled) return
        nativeAd?.let { if (!it.isReady) it.loadAd(configuration?.preloadsAds ?: false) }
        rewardAd?.let { if (!it.isReady) it.loadAd() }
        interstitialAd?.let { if (!it.isReady) it.loadAd() }
        rewardInterAd?.let { if (!it.isReady) it.loadAd() }
        appOpenAd?.let { if (!it.isReady) it.loadAd() }
    }

    private fun currentConfiguration(): SwiftlyAdsConfiguration {
        val c = configuration ?: return SwiftlyAdsConfiguration()
        return SwiftlyAdsConfiguration()
            .bannerAdUnitId(c.bannerAdUnitId)
            .interstitialAdUnitId(c.interstitialAdUnitId)
            .rewardedAdUnitId(c.rewardedAdUnitId)
            .rewardedInterstitialAdUnitId(c.rewardedInterstitialAdUnitId)
            .appOpenAdUnitId(c.appOpenAdUnitId)
            .nativeAdUnitId(c.nativeAdUnitId)
            .isTaggedForChildDirectedTreatment(c.isTaggedForChildDirectedTreatment)
            .isTaggedForUnderAgeOfConsent(c.isTaggedForUnderAgeOfConsent)
            .preloadsAds(c.preloadsAds)
            .mediationConfigurator(c.mediationConfigurator)
            .testDeviceIdentifiers(c.testDeviceIdentifiers)
            .geography(c.geography)
            .resetsConsentOnLaunch(c.resetsConsentOnLaunch)
            .interAdShowCount(c.interAdShowCount)
            .appOpenAdShowCount(c.appOpenAdShowCount)
            .nativeAdShowCount(c.nativeAdShowCount)
            .environment(c.environment)
            .nativeAdMediaAspectRatio(c.nativeAdMediaAspectRatio)
            .apply { c.adRequest?.let { adRequest(it) } }
    }
}
