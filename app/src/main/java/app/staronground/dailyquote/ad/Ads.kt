package app.staronground.dailyquote.ad

import android.app.Activity
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

@Composable
fun BannerAd(modifier: Modifier = Modifier, adUnitId: String) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                this.adUnitId = adUnitId
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}

class InterstitialHolder(private val activity: Activity, private val adUnitId: String) {
    private var interstitialAd: InterstitialAd? = null
    private var loading = false
    private var lastShown: Long = 0L
    private val minIntervalMs = 60_000L // 60초 쿨다운

    fun load() {
        if (loading) return
        loading = true
        InterstitialAd.load(activity, adUnitId, AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    loading = false
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.d("Ads", "Interstitial load failed: $error")
                    interstitialAd = null
                    loading = false
                }
            })
    }

    fun showIfReady(onShown: () -> Unit = {}) {
        val now = System.currentTimeMillis()
        if (now - lastShown < minIntervalMs) return // 쿨다운 미충족
        interstitialAd?.let { ad ->
            ad.show(activity)
            interstitialAd = null
            lastShown = now
            onShown()
            load()
        } ?: run { load() }
    }
}
