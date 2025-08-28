package app.staronground.dailyquote.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*

class BillingManager(context: Context) : PurchasesUpdatedListener {
    private val billing = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases()
        .build()

    var onPurchaseCompleted: ((Purchase) -> Unit)? = null

    fun startConnection(onReady: () -> Unit = {}) {
        billing.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) onReady()
            }
            override fun onBillingServiceDisconnected() { /* retry if needed */ }
        })
    }

    fun launchPurchase(activity: Activity, productId: String, isSubs: Boolean) {
        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(productId)
            .setProductType(if (isSubs) BillingClient.ProductType.SUBS else BillingClient.ProductType.INAPP)
            .build()

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(listOf(product))
            .build()

        billing.queryProductDetailsAsync(params) { result, detailsList ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK || detailsList.isEmpty()) return@queryProductDetailsAsync
            val details = detailsList.first()
            val offerToken = details.subscriptionOfferDetails?.firstOrNull()?.offerToken
            val billingParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(
                    listOf(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                            .setProductDetails(details)
                            .apply { if (offerToken != null) setOfferToken(offerToken) }
                            .build()
                    )
                ).build()
            billing.launchBillingFlow(activity, billingParams)
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            purchases.forEach { onPurchaseCompleted?.invoke(it) }
        }
    }
}
