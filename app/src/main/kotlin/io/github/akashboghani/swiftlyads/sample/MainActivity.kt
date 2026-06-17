package io.github.akashboghani.swiftlyads.sample

import android.app.Activity
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import io.github.akashboghani.swiftlyads.SwiftlyAds
import io.github.akashboghani.swiftlyads.SwiftlyNativeAds
import io.github.akashboghani.swiftlyads.compose.SwiftlyBannerAd
import io.github.akashboghani.swiftlyads.compose.SwiftlyMediaView
import io.github.akashboghani.swiftlyads.compose.SwiftlyNativeAdView
import io.github.akashboghani.swiftlyads.compose.nativeAdElement
import io.github.akashboghani.swiftlyads.config.SwiftlyBannerAdFormat
import io.github.akashboghani.swiftlyads.config.SwiftlyNativeAdElement

private const val TAG = "SwiftlyAdsSample"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) {
                    SampleScreen(this)
                }
            }
        }
    }
}

@Composable
private fun SampleScreen(activity: Activity) {
    var status by remember { mutableStateOf("Initializing…") }
    var nativeAd by remember { mutableStateOf<SwiftlyNativeAds?>(null) }

    LaunchedEffect(Unit) {
        // showConsent = false keeps the sample frictionless; flip to true to test the UMP form.
        SwiftlyAds.initializeIfNeeded(activity, showConsent = false)
            .onSuccess {
                status = "SDK ready"
                SwiftlyAds.requestNativeAd()
                    .onReceiveAd { nativeAd = it }
                    .onError {
                        status = "Native error: ${it.message}"
                        Log.e(TAG, "Native ad failed to load: ${it.message}", it)
                    }
            }
            .onError {
                status = "Init failed: ${it.message}"
                Log.e(TAG, "SDK init failed: ${it.message}", it)
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("SwiftlyAds Sample", style = MaterialTheme.typography.headlineSmall)
        Text(status, style = MaterialTheme.typography.bodyMedium)

        Button(
            onClick = {
                SwiftlyAds.showInterstitialAd(activity)
                    .onClose { status = "Interstitial closed" }
                    .onError {
                        status = "Interstitial error: ${it.message}"
                        Log.e(TAG, "Interstitial failed: ${it.message}", it)
                    }
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Show Interstitial") }

        Button(
            onClick = {
                SwiftlyAds.showRewardAd(activity)
                    .onReward { amount -> status = "Reward earned: $amount" }
                    .onError {
                        status = "Rewarded error: ${it.message}"
                        Log.e(TAG, "Rewarded failed: ${it.message}", it)
                    }
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Show Rewarded") }

        Button(
            onClick = {
                SwiftlyAds.showRewardInterAd(activity)
                    .onReward { amount -> status = "Reward (inter) earned: $amount" }
                    .onError {
                        status = "RewardInter error: ${it.message}"
                        Log.e(TAG, "RewardInter failed: ${it.message}", it)
                    }
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Show Rewarded Interstitial") }

        Button(
            onClick = {
                SwiftlyAds.showAppOpenAd(activity)
                    .onClose { status = "App open closed" }
                    .onError {
                        status = "AppOpen error: ${it.message}"
                        Log.e(TAG, "AppOpen failed: ${it.message}", it)
                    }
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Show App Open") }

        Button(
            onClick = {
                SwiftlyAds.requestNativeAd(bypassingFrequencyLimit = true)
                    .onReceiveAd { nativeAd = it }
                    .onError {
                        status = "Native error: ${it.message}"
                        Log.e(TAG, "Native ad reload failed: ${it.message}", it)
                    }
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Reload Native Ad") }

        HorizontalDivider()
        Text("Native ad", style = MaterialTheme.typography.titleMedium)
        NativeAdCard(nativeAd)

        Spacer(Modifier.height(24.dp))
        Text("Banner", style = MaterialTheme.typography.titleMedium)
        SwiftlyBannerAd(
            format = SwiftlyBannerAdFormat.STANDARD_BANNER,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun NativeAdCard(nativeAd: SwiftlyNativeAds?) {
    if (nativeAd == null) {
        Text("No native ad loaded yet.")
        return
    }

    val cardBg = Color(0xFF1B1C22)
    val barBg = Color(0xFF202129)
    val secondary = Color(0xFF9AA0AC)
    val amber = Color(0xFFF5C518)

    SwiftlyNativeAdView(
        nativeAd,
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(cardBg),
    ) { ad ->
        Column(Modifier.fillMaxWidth()) {
            // Header: icon · headline / "Sponsored · advertiser" + rating · "Ad" badge
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(barBg)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                val iconBitmap = (ad?.icon?.drawable as? BitmapDrawable)?.bitmap
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF2C2D36)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (iconBitmap != null) {
                        Image(
                            bitmap = iconBitmap.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .nativeAdElement(SwiftlyNativeAdElement.ICON),
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        ad?.headline.orEmpty(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.nativeAdElement(SwiftlyNativeAdElement.HEADLINE),
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            "Sponsored · ${ad?.advertiser ?: ad?.store ?: "AdMob"}",
                            color = secondary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.nativeAdElement(SwiftlyNativeAdElement.ADVERTISER),
                        )
                        ad?.starRating?.let { rating ->
                            StarRating(
                                rating = rating,
                                color = amber,
                                modifier = Modifier.nativeAdElement(SwiftlyNativeAdElement.STAR_RATING),
                            )
                            Text(
                                String.format("%.1f", rating),
                                color = secondary,
                                fontSize = 12.sp,
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(amber)
                        .padding(horizontal = 7.dp, vertical = 3.dp),
                ) {
                    Text(
                        "Ad",
                        color = Color(0xFF1B1C22),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                    )
                }
            }

            // Media
            SwiftlyMediaView(ad, Modifier.fillMaxWidth())

            // Footer: body description + call-to-action button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(barBg)
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    ad?.body.orEmpty(),
                    color = secondary,
                    fontSize = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .nativeAdElement(SwiftlyNativeAdElement.BODY),
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF2A2B34))
                        .border(1.dp, Color(0xFF3B3C46), RoundedCornerShape(24.dp))
                        .nativeAdElement(SwiftlyNativeAdElement.CALL_TO_ACTION)
                        .padding(horizontal = 22.dp, vertical = 11.dp),
                ) {
                    Text(
                        ad?.callToAction ?: "Install",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun StarRating(rating: Double, color: Color, modifier: Modifier = Modifier) {
    val filled = rating.roundToInt()
    Row(modifier) {
        repeat(5) { index ->
            Text(
                if (index < filled) "★" else "☆",
                color = color,
                fontSize = 12.sp,
            )
        }
    }
}
