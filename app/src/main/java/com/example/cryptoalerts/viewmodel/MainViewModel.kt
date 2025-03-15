package com.example.cryptoalerts.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cryptoalerts.model.CryptoCurrency
import com.example.cryptoalerts.model.PriceAlert
import com.example.cryptoalerts.repository.CryptoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel : ViewModel() {
    private val repository = CryptoRepository()
    
    // LiveData for search results
    private val _searchResults = MutableLiveData<List<CryptoCurrency>>()
    val searchResults: LiveData<List<CryptoCurrency>> = _searchResults
    
    // LiveData for selected cryptocurrency
    private val _selectedCrypto = MutableLiveData<CryptoCurrency>()
    val selectedCrypto: LiveData<CryptoCurrency> = _selectedCrypto
    
    // LiveData for saved alerts
    private val _alerts = MutableLiveData<List<PriceAlert>>()
    val alerts: LiveData<List<PriceAlert>> = _alerts
    
    // LiveData for loading state
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    // LiveData for error messages
    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    init {
        // Initialize alerts list
        _alerts.value = emptyList()
    }
    
    // Function to search for cryptocurrencies
    fun searchCryptoCurrency(query: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val results = repository.searchCryptoCurrency(query)
                _searchResults.value = results
            } catch (e: Exception) {
                _errorMessage.value = "Error searching for cryptocurrency: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    // Function to select a cryptocurrency
    fun selectCryptoCurrency(crypto: CryptoCurrency) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val updatedCrypto = repository.getCryptoCurrencyDetails(crypto.symbol) ?: crypto
                _selectedCrypto.value = updatedCrypto
            } catch (e: Exception) {
                _errorMessage.value = "Error fetching cryptocurrency details: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    // Function to save an alert
    fun saveAlert(alert: PriceAlert) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.saveAlert(alert)
                val updatedAlerts = repository.getAlerts()
                withContext(Dispatchers.Main) {
                    _alerts.value = updatedAlerts
                }
            }
        }
    }
    
    // Function to refresh alert prices
    fun refreshAlertPrices() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val updatedAlerts = repository.updateAlertPrices()
                _alerts.value = updatedAlerts
            } catch (e: Exception) {
                _errorMessage.value = "Error updating alert prices: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    // Function to check which alerts have been triggered
    fun checkTriggeredAlerts(): List<PriceAlert> {
        var triggeredAlerts = emptyList<PriceAlert>()
        
        viewModelScope.launch {
            triggeredAlerts = repository.checkAlertsTriggered()
            _alerts.value = repository.getAlerts()
        }
        
        return triggeredAlerts
    }
}
