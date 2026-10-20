package com.example.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class BillingConnectionState {
    DISCONNECTED, CONNECTING, CONNECTED, CLOSED
}

class BillingManager(context: Context, private val externalScope: CoroutineScope) : PurchasesUpdatedListener {

    private val TAG = "BillingManager"
    private val billingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases()
        .build()

    private val _connectionState = MutableStateFlow(BillingConnectionState.DISCONNECTED)
    val connectionState: StateFlow<BillingConnectionState> = _connectionState.asStateFlow()

    private val _productDetails = MutableStateFlow<Map<String, ProductDetails>>(emptyMap())
    val productDetails: StateFlow<Map<String, ProductDetails>> = _productDetails.asStateFlow()

    private val _purchaseFlow = MutableSharedFlow<Pair<BillingResult, Purchase?>>()
    val purchaseFlow = _purchaseFlow.asSharedFlow()

    private var reconnectAttempt = 0L

    init {
        startConnection()
    }

    fun startConnection() {
        if (_connectionState.value == BillingConnectionState.CONNECTED || _connectionState.value == BillingConnectionState.CONNECTING) return

        _connectionState.value = BillingConnectionState.CONNECTING
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Billing Setup Finished: OK")
                    _connectionState.value = BillingConnectionState.CONNECTED
                    reconnectAttempt = 0
                    queryProductDetails()
                } else {
                    Log.e(TAG, "Billing Setup Finished: Error ${billingResult.responseCode}")
                    _connectionState.value = BillingConnectionState.DISCONNECTED
                    retryConnection()
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "Billing Service Disconnected")
                _connectionState.value = BillingConnectionState.DISCONNECTED
                retryConnection()
            }
        })
    }

    private fun retryConnection() {
        externalScope.launch {
            val delayMillis = (1L shl reconnectAttempt.toInt().coerceAtMost(6)) * 1000L
            delay(delayMillis)
            reconnectAttempt++
            startConnection()
        }
    }

    fun queryProductDetails() {
        val productList = productIds.map {
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(it)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        }

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                _productDetails.value = productDetailsList.associateBy { it.productId }
            } else {
                Log.e(TAG, "Query Product Details Error: ${billingResult.responseCode}")
            }
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                    acknowledgeAndConsumePurchase(purchase)
                }
            }
        }
        externalScope.launch { _purchaseFlow.emit(Pair(billingResult, purchases?.firstOrNull())) }
    }

    private fun acknowledgeAndConsumePurchase(purchase: Purchase) {
        val consumeParams = ConsumeParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        billingClient.consumeAsync(consumeParams) { result, _ ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                Log.e(TAG, "Consumption Failed: ${result.responseCode}")
            }
        }
    }

    fun launchPurchaseFlow(activity: Activity, productId: String, onError: (String) -> Unit) {
        val details = _productDetails.value[productId]
        if (details == null) {
            onError("Product details not found for $productId")
            return
        }

        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(details)
                .build()
        )
        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()
        
        billingClient.launchBillingFlow(activity, billingFlowParams)
    }

    companion object {
        val productIds = listOf(
            "coin_pack_0_51", "coin_pack_1", "coin_pack_1_01", "coin_pack_2",
            "coin_pack_3", "coin_pack_4_01", "coin_pack_5_01", "coin_pack_10_01",
            "coin_pack_40_05", "coin_pack_50_01"
        )
    }
}
