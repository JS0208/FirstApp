package app.staronground.dailyquote

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import app.staronground.dailyquote.ad.BannerAd
import app.staronground.dailyquote.ad.InterstitialHolder
import app.staronground.dailyquote.ad.AdIds
import app.staronground.dailyquote.billing.BillingManager
import app.staronground.dailyquote.data.QuoteRepository
import app.staronground.dailyquote.data.ProStore
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.AdRequest

class MainActivity : ComponentActivity() {
    private lateinit var interstitial: InterstitialHolder
    private lateinit var billing: BillingManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Mobile Ads + mark test devices (replace with your real device hash)
        MobileAds.initialize(this) {}
        MobileAds.setRequestConfiguration(
            RequestConfiguration.Builder()
                .setTestDeviceIds(listOf(AdRequest.DEVICE_ID_EMULATOR, "YOUR_TEST_DEVICE_ID"))
                .build()
        )

        interstitial = InterstitialHolder(this, adUnitId = AdIds.interstitial())
        interstitial.load()

        billing = BillingManager(this)
        val proState = mutableStateOf(false)

        // 구매 복구 & Pro 반영
        billing.startConnection {
            billing.refreshPurchases { isPro ->
                runOnUiThread {
                    proState.value = isPro
                    lifecycleScope.launch { ProStore.set(this@MainActivity, isPro) }
                }
            }
        }
        // 저장된 Pro 상태 반영(앱 시작 직후 즉시 표시)
        lifecycleScope.launch {
            ProStore.flow(this@MainActivity).collect { pro ->
                proState.value = pro
            }
        }

        val repo = QuoteRepository(this)

        setContent {
            MaterialTheme {
                val current by repo.current.collectAsState()
                var nextCount by remember { mutableStateOf(0) }
                val isPro by remember { proState }

                Scaffold(
                    bottomBar = {
                        if (!isPro) {
                            BannerAd(
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                adUnitId = AdIds.banner()
                            )
                        }
                    }
                ) { pad ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(pad)
                            .padding(24.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "\u201C${current.text}\u201D",
                                style = MaterialTheme.typography.headlineSmall,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(text = "— ${current.author}")
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(onClick = { repo.toggleFavorite(current.id) }) { Text("즐겨찾기") }
                            Button(onClick = {
                                repo.next()
                                nextCount += 1
                                if (!isPro && nextCount % 3 == 0) interstitial.showIfReady()
                            }) { Text("다음 명언") }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (!isPro) {
                                Button(onClick = {
                                    billing.launchPurchase(this@MainActivity, productId = "remove_ads_once", isSubs = false)
                                }) { Text("광고 제거 (일회성)") }
                            }
                            Button(onClick = {
                                billing.launchPurchase(this@MainActivity, productId = "premium_monthly", isSubs = true)
                            }) { Text("프리미엄 (구독)") }
                        }
                    }
                }
            }
        }

        billing.onPurchaseCompleted = {
            billing.refreshPurchases { isPro ->
                runOnUiThread {
                    proState.value = isPro
                    lifecycleScope.launch { ProStore.set(this@MainActivity, isPro) }
                }
            }
        }
    }
}
