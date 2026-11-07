package com.example.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.UserBalanceDao
import com.example.data.model.UserBalanceEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class NoteSaveResult {
    object Success : NoteSaveResult()
    object OutOfQuota : NoteSaveResult()
}

class NoteDetailViewModel(
    private val userBalanceDao: UserBalanceDao
) : ViewModel() {

    private val _userBalance = MutableStateFlow<UserBalanceEntity?>(null)
    val userBalance = _userBalance.asStateFlow()

    private val _saveResult = MutableSharedFlow<NoteSaveResult>()
    val saveResult = _saveResult.asSharedFlow()

    init {
        viewModelScope.launch {
            userBalanceDao.getBalance().collect {
                _userBalance.value = it
            }
        }
    }

    fun attemptSaveNote(onCanSave: () -> Unit) {
        viewModelScope.launch {
            val balance = _userBalance.value ?: UserBalanceEntity()
            if (balance.freeNotesRemaining > 0 || balance.coinBalance >= 1.0) {
                onCanSave()
                deductQuota()
                _saveResult.emit(NoteSaveResult.Success)
            } else {
                _saveResult.emit(NoteSaveResult.OutOfQuota)
            }
        }
    }

    fun addRewardedQuota(amount: Int = 1) {
        viewModelScope.launch {
            val balance = userBalanceDao.getBalanceDirect() ?: UserBalanceEntity()
            userBalanceDao.updateBalance(balance.copy(freeNotesRemaining = balance.freeNotesRemaining + amount))
        }
    }

    fun addRewardedCoins(amount: Double = 1.0) {
        viewModelScope.launch {
            val balance = userBalanceDao.getBalanceDirect() ?: UserBalanceEntity()
            userBalanceDao.updateBalance(balance.copy(coinBalance = balance.coinBalance + amount))
        }
    }

    private suspend fun deductQuota() {
        val balance = _userBalance.value ?: UserBalanceEntity()
        if (balance.freeNotesRemaining > 0) {
            userBalanceDao.updateBalance(balance.copy(freeNotesRemaining = balance.freeNotesRemaining - 1))
        } else if (balance.coinBalance >= 1.0) {
            userBalanceDao.updateBalance(balance.copy(coinBalance = balance.coinBalance - 1.0))
        }
    }
}
