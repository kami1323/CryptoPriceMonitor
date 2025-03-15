package com.example.cryptoalerts.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.cryptoalerts.R
import com.example.cryptoalerts.model.PriceAlert

class AlertsAdapter : ListAdapter<PriceAlert, AlertsAdapter.AlertViewHolder>(AlertDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlertViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_alert, parent, false)
        return AlertViewHolder(view)
    }

    override fun onBindViewHolder(holder: AlertViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class AlertViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textViewSymbol: TextView = itemView.findViewById(R.id.textViewSymbol)
        private val textViewCurrentPrice: TextView = itemView.findViewById(R.id.textViewCurrentPrice)
        private val textViewAlertPrice: TextView = itemView.findViewById(R.id.textViewAlertPrice)
        private val textViewDailyChange: TextView = itemView.findViewById(R.id.textViewDailyChange)
        private val textViewStatus: TextView = itemView.findViewById(R.id.textViewStatus)

        fun bind(alert: PriceAlert) {
            textViewSymbol.text = alert.cryptoSymbol
            textViewCurrentPrice.text = "Price: $${String.format("%.2f", alert.currentPrice)}"
            textViewAlertPrice.text = "Alert: $${String.format("%.2f", alert.alertPrice)}"
            textViewDailyChange.text = "24h: ${String.format("%.2f", alert.priceChangePercent)}%"

            // Set color for price change percentage
            val context = itemView.context
            if (alert.priceChangePercent >= 0) {
                textViewDailyChange.setTextColor(context.getColor(android.R.color.holo_green_light))
            } else {
                textViewDailyChange.setTextColor(context.getColor(android.R.color.holo_red_light))
            }

            // Set alert status
            if (alert.isTriggered) {
                textViewStatus.text = "TRIGGERED"
                textViewStatus.setTextColor(context.getColor(android.R.color.holo_orange_light))
            } else {
                textViewStatus.text = "ACTIVE"
                textViewStatus.setTextColor(context.getColor(android.R.color.holo_green_light))
            }

            // Highlight background if threshold has been crossed but status hasn't been updated yet
            if (!alert.isTriggered && alert.isPriceThresholdCrossed()) {
                itemView.setBackgroundColor(context.getColor(R.color.alertHighlight))
            } else {
                itemView.setBackgroundColor(context.getColor(android.R.color.transparent))
            }
        }
    }

    class AlertDiffCallback : DiffUtil.ItemCallback<PriceAlert>() {
        override fun areItemsTheSame(oldItem: PriceAlert, newItem: PriceAlert): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: PriceAlert, newItem: PriceAlert): Boolean {
            return oldItem.currentPrice == newItem.currentPrice &&
                   oldItem.isTriggered == newItem.isTriggered &&
                   oldItem.priceChangePercent == newItem.priceChangePercent
        }
    }
}
