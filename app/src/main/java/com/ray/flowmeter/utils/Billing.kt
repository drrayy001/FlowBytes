// Helper managing Google Play Billing Client API connections, products query, and purchase lifecycle.
package com.ray.flowmeter.utils

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

sealed class BillingEvent {
    object Success : BillingEvent()
    object Cancelled : BillingEvent()
    data class Error(val message: String) : BillingEvent()
}

class BillingManager(context: Context, private val scope: CoroutineScope) {

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { purchase ->
                    scope.launch { handlePurchase(purchase) }
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                scope.launch { _events.emit(BillingEvent.Cancelled) }
            }
            else -> {
                scope.launch { _events.emit(BillingEvent.Error(billingResult.debugMessage)) }
            }
        }
    }

    private var billingClient = BillingClient.newBuilder(context)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    private val _events = MutableSharedFlow<BillingEvent>()
    val events: SharedFlow<BillingEvent> = _events.asSharedFlow()

    init {
        startConnection()
    }

    private fun startConnection() {
        billingClient.startConnection(
            object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        queryPurchases()
                    }
                }

                override fun onBillingServiceDisconnected() {

                }
            },
        )
    }

    private fun queryPurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                for (purchase in purchases) {
                    scope.launch { handlePurchase(purchase) }
                }
            }
        }
    }

    fun makePurchase(activity: Activity, productId: String) {
        if (!billingClient.isReady) {
            scope.launch {
                _events.emit(BillingEvent.Error("Billing service is not ready. Try again in a moment."))
            }
            startConnection()
            return
        }

        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(BillingClient.ProductType.INAPP)
                .build(),
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, result ->
            scope.launch {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    val productDetailsList = result.productDetailsList
                    if (productDetailsList.isNotEmpty()) {
                        val flowParams = BillingFlowParams.newBuilder()
                            .setProductDetailsParamsList(
                                listOf(
                                    BillingFlowParams.ProductDetailsParams.newBuilder()
                                        .setProductDetails(productDetailsList[0])
                                        .build(),
                                ),
                            )
                            .build()
                        billingClient.launchBillingFlow(activity, flowParams)
                    } else {
                        _events.emit(BillingEvent.Error("Product details not found. Make sure the app is installed from Google Play."))
                    }
                } else {
                    val debugMsg = billingResult.debugMessage
                    val errMsg = debugMsg.ifEmpty { "Google Play Billing error (Code: ${billingResult.responseCode})" }
                    _events.emit(BillingEvent.Error(errMsg))
                }
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            val consumeParams = ConsumeParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            
            billingClient.consumeAsync(consumeParams) { billingResult, _ ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    scope.launch { _events.emit(BillingEvent.Success) }
                }
            }
        }
    }
}
