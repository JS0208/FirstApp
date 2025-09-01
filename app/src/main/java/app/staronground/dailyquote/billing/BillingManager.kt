package app.staronground.dailyquote.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*

class BillingManager(private val context: Context) : PurchasesUpdatedListener {

    private var billingClient: BillingClient = BillingClient.newBuilder(context)
        .enablePendingPurchases()
        .setListener(this)
        .build()

    var onPurchaseCompleted: (() -> Unit)? = null

    fun startConnection(onReady: () -> Unit) {
        if (billingClient.isReady) {
            onReady()
            return
        }
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    onReady()
                }
            }
            override fun onBillingServiceDisconnected() { /* auto-reconnect on next call */ }
        })
    }

    fun launchPurchase(activity: Activity, productId: String, isSubs: Boolean) {
        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(productId)
            .setProductType(if (isSubs) BillingClient.ProductType.SUBS else BillingClient.ProductType.INAPP)
            .build()

        val queryParams = QueryProductDetailsParams.newBuilder()
            .setProductList(listOf(product))
            .build()

        billingClient.queryProductDetailsAsync(queryParams) { result, detailsList ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK || detailsList.isEmpty()) return@queryProductDetailsAsync

            val pd = detailsList.first()
            val paramsBuilder = BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(pd)

            // For SUBS use the first available offer token
            pd.subscriptionOfferDetails?.firstOrNull()?.offerToken?.let { token ->
                paramsBuilder.setOfferToken(token)
            }

            val flowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(listOf(paramsBuilder.build()))
                .build()

            billingClient.launchBillingFlow(activity, flowParams)
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            handlePurchases(purchases)
        }
    }

    private fun handlePurchases(purchases: List<Purchase>) {
        purchases.forEach { p ->
            if (p.purchaseState == Purchase.PurchaseState.PURCHASED && !p.isAcknowledged) {
                val ackParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(p.purchaseToken)
                    .build()
                billingClient.acknowledgePurchase(ackParams) { /* ignore result */ }
            }
        }
        onPurchaseCompleted?.invoke()
    }

    /** Queries INAPP + SUBS; returns true if any PURCHASED item exists. */
    fun refreshPurchases(callback: (Boolean) -> Unit) {
        var isPro = false
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
        ) { r1, inappList ->
            if (r1.responseCode == BillingClient.BillingResponseCode.OK) {
                inappList.forEach { if (it.purchaseState == Purchase.PurchaseState.PURCHASED) isPro = true }
            }
            billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()
            ) { r2, subsList ->
                if (r2.responseCode == BillingClient.BillingResponseCode.OK) {
                    subsList.forEach { if (it.purchaseState == Purchase.PurchaseState.PURCHASED) isPro = true }
                }
                callback(isPro)
            }
        }
    }
}
