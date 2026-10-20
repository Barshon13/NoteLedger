package com.example.ui.coinstore

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.UserBalanceDao
import com.example.data.local.PurchaseDao
import com.example.billing.BillingManager
import com.example.billing.BillingConnectionState
import com.example.data.model.UserBalanceEntity
import com.example.data.model.PurchaseRecord
import com.android.billingclient.api.ProductDetails
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CoinStoreUiState(
    val userBalance: UserBalanceEntity = UserBalanceEntity(),
    val connectionState: BillingConnectionState = BillingConnectionState.DISCONNECTED,
    val productDetails: Map<String, ProductDetails> = emptyMap(),
    val isProcessing: Boolean = false,
    val errorMessage: String? = null
)

class CoinStoreViewModel(
    private val userBalanceDao: UserBalanceDao,
    private val purchaseDao: PurchaseDao,
    private val billingManager: BillingManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(CoinStoreUiState())
    val uiState: StateFlow<CoinStoreUiState> = _uiState.asStateFlow()

    init {
        // Observe balance
        viewModelScope.launch {
            userBalanceDao.getBalance().collect { balance ->
                _uiState.update { it.copy(userBalance = balance ?: UserBalanceEntity()) }
            }
        }

        // Observe billing connection
        viewModelScope.launch {
            billingManager.connectionState.collect { state ->
                _uiState.update { it.copy(connectionState = state) }
            }
        }

        // Observe product details
        viewModelScope.launch {
            billingManager.productDetails.collect { details ->
                _uiState.update { it.copy(productDetails = details) }
            }
        }

        // Observe purchases
        viewModelScope.launch {
            billingManager.purchaseFlow.collect { (result, purchase) ->
                _uiState.update { it.copy(isProcessing = false) }
                if (result.responseCode == com.android.billingclient.api.BillingClient.BillingResponseCode.OK && purchase != null) {
                    val productId = purchase.products.firstOrNull() ?: ""
                    val coins = getCoinsFromProductId(productId)
                    val price = _uiState.value.productDetails[productId]?.oneTimePurchaseOfferDetails?.formattedPrice ?: "Unknown"
                    
                    addCoins(coins)
                    recordPurchase(purchase.orderId ?: "unknown_${System.currentTimeMillis()}", productId, coins, price)
                } else if (result.responseCode != com.android.billingclient.api.BillingClient.BillingResponseCode.USER_CANCELED) {
                    _uiState.update { it.copy(errorMessage = "Purchase failed: ${result.debugMessage}") }
                }
            }
        }
    }

    fun onPurchaseClicked(activity: Activity, productId: String) {
        if (_uiState.value.connectionState != BillingConnectionState.CONNECTED) {
            _uiState.update { it.copy(errorMessage = "Billing not ready. Please try again or check your connection.") }
            billingManager.startConnection()
            return
        }

        _uiState.update { it.copy(isProcessing = true) }
        billingManager.launchPurchaseFlow(activity, productId) { error ->
            _uiState.update { it.copy(isProcessing = false, errorMessage = error) }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun getCoinsFromProductId(productId: String): Double {
        return when (productId) {
            "coin_pack_0_51" -> 0.51
            "coin_pack_1" -> 1.0
            "coin_pack_1_01" -> 1.01
            "coin_pack_2" -> 2.0
            "coin_pack_3" -> 3.0
            "coin_pack_4_01" -> 4.01
            "coin_pack_5_01" -> 5.01
            "coin_pack_10_01" -> 10.01
            "coin_pack_40_05" -> 40.05
            "coin_pack_50_01" -> 50.01
            else -> 0.0
        }
    }

    private fun addCoins(coins: Double) {
        viewModelScope.launch {
            val current = _uiState.value.userBalance
            userBalanceDao.updateBalance(current.copy(coinBalance = current.coinBalance + coins))
        }
    }

    private fun recordPurchase(orderId: String, productId: String, coins: Double, price: String) {
        viewModelScope.launch {
            purchaseDao.insertPurchase(
                PurchaseRecord(
                    orderId = orderId,
                    productId = productId,
                    coinAmount = coins,
                    price = price
                )
            )
        }
    }
}
