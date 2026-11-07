package com.example.ui.coinstore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.PurchaseDao
import com.example.data.model.PurchaseRecord
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class PurchaseHistoryViewModel(
    private val purchaseDao: PurchaseDao
) : ViewModel() {

    val purchaseHistory: StateFlow<List<PurchaseRecord>> = purchaseDao.getAllPurchases()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
}
