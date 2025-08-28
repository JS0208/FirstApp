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
import app.staronground.dailyquote.ad.BannerAd
import app.staronground.dailyquote.ad.InterstitialHolder
import app.staronground.dailyquote.billing.BillingManager
import app.staronground.dailyquote.data.QuoteRepository

class MainActivity : ComponentActivity() {
    private lateinit var interstitial: InterstitialHolder
    private lateinit var billing: BillingManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        interstitial = InterstitialHolder(this, adUnitId = "ca-app-pub-3940256099942544/1033173712")
        interstitial.load()

        billing = BillingManager(this)
        billing.startConnection {}

        val repo = QuoteRepository()

        setContent {
            MaterialTheme {
                val current by repo.current.collectAsState()
                var nextCount by remember { mutableStateOf(0) }

                Scaffold(
                    bottomBar = {
                        BannerAd(
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            adUnitId = "ca-app-pub-3940256099942544/6300978111"
                        )
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
                                if (nextCount % 3 == 0) interstitial.showIfReady()
                            }) { Text("다음 명언") }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(onClick = {
                                billing.launchPurchase(this@MainActivity, productId = "remove_ads_once", isSubs = false)
                            }) { Text("광고 제거 (일회성)") }

                            Button(onClick = {
                                billing.launchPurchase(this@MainActivity, productId = "premium_monthly", isSubs = true)
                            }) { Text("프리미엄 (구독)") }
                        }
                    }
                }
            }
        }
    }
}
