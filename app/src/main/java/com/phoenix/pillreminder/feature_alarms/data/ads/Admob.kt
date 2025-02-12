package com.phoenix.pillreminder.feature_alarms.data.ads

import android.app.Activity
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.phoenix.pillreminder.BuildConfig
import com.phoenix.pillreminder.feature_alarms.data.ads.Admob.gatherUserConsent
import com.phoenix.pillreminder.feature_alarms.data.ads.Admob.showInterstitial
import com.phoenix.pillreminder.feature_alarms.presentation.activities.MainActivity.Companion.TEST_DEVICE_HASHED_ID
import java.util.concurrent.atomic.AtomicBoolean

/**
 *  Instance a singleton of Admob.
 *  It must always call [gatherUserConsent] before proceeding with any ad-related operations
 *  to verify if user consent has been granted and initialize the sdk.
 *  Use [showInterstitial] to show the ad.
 */
object Admob {
    private const val TAG = "Admob"
    private lateinit var googleMobileAdsConsentManager: GoogleMobileAdsConsentManager
    private val isMobileAdsInitializeCalled = AtomicBoolean(false)
    private var interstitialAd: InterstitialAd? = null
    private var adIsLoading: Boolean = false

    // This is an ad unit ID for a test ad. Replace with your own interstitial ad unit ID.
    private const val AD_UNIT_ID = BuildConfig.AD_UNIT_ID

    fun gatherUserConsent(activity: Activity, initCallback: (() -> Unit)) {
        googleMobileAdsConsentManager = GoogleMobileAdsConsentManager.getInstance(activity)
        googleMobileAdsConsentManager.gatherConsent(activity) { consentError ->
            if (consentError != null) {
                // Consent not obtained in current session.
                Log.w(TAG, "${consentError.errorCode}: ${consentError.message}")
            }

            if (googleMobileAdsConsentManager.canRequestAds) {
                initializeMobileAdsSdk()
            }
            if (googleMobileAdsConsentManager.isPrivacyOptionsRequired) {
                // Regenerate the options menu to include a privacy setting.
                AppCompatActivity().invalidateOptionsMenu()
            }
        }

        // Attempts to load ads using consent obtained in the previous session.
        if (googleMobileAdsConsentManager.canRequestAds) {
            val initializedSdk = initializeMobileAdsSdk()

            if(!initializedSdk) {
                initCallback()
            }
        }
    }

    private fun initializeMobileAdsSdk(): Boolean {
        if (isMobileAdsInitializeCalled.getAndSet(true)) {
            return true
        }

        // Set your test devices.
        MobileAds.setRequestConfiguration(
            RequestConfiguration.Builder().setTestDeviceIds(listOf(TEST_DEVICE_HASHED_ID)).build()
        )
        return false
    }

    fun loadAd(activity: Activity) {
        // Request a new ad if one isn't already loaded.
        if (adIsLoading || interstitialAd != null) {
            return
        }
        adIsLoading = true

        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(
            activity,
            AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    interstitialAd = null
                    adIsLoading = false
                    val error =
                        "domain: ${adError.domain}, code: ${adError.code}, " + "message: ${adError.message}"
                    Log.d(TAG, "onAdFailedToLoad() with error $error")
                }

                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d(TAG, "Ad was loaded.")
                    interstitialAd = ad
                    adIsLoading = false
                }
            },
        )
    }

    // Show the ad if it's ready. Otherwise restart the game.
    fun showInterstitial(activity: Activity) {
        if (interstitialAd != null) {
            interstitialAd?.fullScreenContentCallback =
                object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        Log.d(TAG, "Ad was dismissed.")
                        // Don't forget to set the ad reference to null so you
                        // don't show the ad a second time.
                        interstitialAd = null
                    }

                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                        Log.d(TAG, "Ad failed to show.")
                        // Don't forget to set the ad reference to null so you
                        // don't show the ad a second time.
                        interstitialAd = null
                    }

                    override fun onAdShowedFullScreenContent() {
                        Log.d(TAG, "Ad showed fullscreen content.")
                        // Called when ad is dismissed.
                    }
                }
            interstitialAd?.show(activity)
        } else {
            if (googleMobileAdsConsentManager.canRequestAds) {
                loadAd(activity)
            }
        }
    }
}