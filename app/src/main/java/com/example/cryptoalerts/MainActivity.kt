package com.example.cryptoalerts

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.cryptoalerts.adapter.AlertsAdapter
import com.example.cryptoalerts.databinding.ActivityMainBinding
import com.example.cryptoalerts.model.PriceAlert
import com.example.cryptoalerts.viewmodel.MainViewModel
import com.example.cryptoalerts.worker.PriceCheckWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel
    private lateinit var alertsAdapter: AlertsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)
        
        setupUI()
        setupObservers()
        setupWorker()
    }

    private fun setupUI() {
        // Setup RecyclerView for alerts
        alertsAdapter = AlertsAdapter()
        binding.recyclerViewAlerts.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = alertsAdapter
        }

        // Setup cryptocurrency search with auto-completion
        binding.editTextCryptoName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                s?.toString()?.let { query ->
                    if (query.isNotEmpty()) {
                        viewModel.searchCryptoCurrency(query)
                    }
                }
            }
            
            override fun afterTextChanged(s: Editable?) {}
        })

        // Setup save alert button
        binding.buttonSaveAlert.setOnClickListener {
            val alertPrice = binding.editTextAlertPrice.text.toString().toDoubleOrNull()
            
            if (alertPrice != null && viewModel.selectedCrypto.value != null) {
                val crypto = viewModel.selectedCrypto.value!!
                val alert = PriceAlert(
                    cryptoSymbol = crypto.symbol,
                    cryptoName = crypto.name,
                    currentPrice = crypto.price,
                    alertPrice = alertPrice,
                    priceChangePercent = crypto.priceChangePercent
                )
                
                viewModel.saveAlert(alert)
                binding.editTextAlertPrice.text.clear()
                Toast.makeText(this, "Alert saved", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Please select a cryptocurrency and enter a valid alert price", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupObservers() {
        // Observe cryptocurrency search results
        viewModel.searchResults.observe(this) { cryptos ->
            if (cryptos.isNotEmpty()) {
                // Show suggestion dropdown or select first match
                viewModel.selectCryptoCurrency(cryptos[0])
            }
        }

        // Observe selected cryptocurrency
        viewModel.selectedCrypto.observe(this) { crypto ->
            if (crypto != null) {
                binding.editTextCryptoName.setText(crypto.symbol)
                binding.textViewCurrentPrice.text = "Price: $${String.format("%.2f", crypto.price)}"
                binding.textViewDailyChange.text = "24h Change: ${String.format("%.2f", crypto.priceChangePercent)}%"
                
                if (crypto.priceChangePercent >= 0) {
                    binding.textViewDailyChange.setTextColor(getColor(android.R.color.holo_green_light))
                } else {
                    binding.textViewDailyChange.setTextColor(getColor(android.R.color.holo_red_light))
                }
            }
        }

        // Observe saved alerts
        viewModel.alerts.observe(this) { alerts ->
            alertsAdapter.submitList(alerts)
            
            if (alerts.isEmpty()) {
                binding.textViewNoAlerts.visibility = View.VISIBLE
                binding.recyclerViewAlerts.visibility = View.GONE
            } else {
                binding.textViewNoAlerts.visibility = View.GONE
                binding.recyclerViewAlerts.visibility = View.VISIBLE
            }
        }

        // Observe loading state
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        // Observe error messages
        viewModel.errorMessage.observe(this) { errorMsg ->
            if (errorMsg.isNotEmpty()) {
                Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupWorker() {
        // Create constraints for the worker
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // Create a periodic work request to check prices every minute
        val priceCheckRequest = PeriodicWorkRequestBuilder<PriceCheckWorker>(
            15, TimeUnit.MINUTES, // Minimum interval allowed is 15 minutes for testing
            5, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()

        // Enqueue the work request
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "PRICE_CHECK_WORK",
            ExistingPeriodicWorkPolicy.REPLACE,
            priceCheckRequest
        )
    }

    override fun onResume() {
        super.onResume()
        // Refresh prices when app comes to foreground
        CoroutineScope(Dispatchers.Main).launch {
            viewModel.refreshAlertPrices()
        }
    }
}
