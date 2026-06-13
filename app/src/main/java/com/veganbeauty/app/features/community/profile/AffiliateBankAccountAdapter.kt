package com.veganbeauty.app.features.community.profile

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.veganbeauty.app.R

class AffiliateBankAccountAdapter(
    private val context: Context,
    private val accounts: List<AffiliateBankAccount>,
    private val onAccountClick: (AffiliateBankAccount) -> Unit
) : RecyclerView.Adapter<AffiliateBankAccountAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivRadio: ImageView = view.findViewById(R.id.ivRadio)
        val ivBankLogo: ImageView = view.findViewById(R.id.ivBankLogo)
        val tvBankName: TextView = view.findViewById(R.id.tvBankName)
        val tvAccountNumber: TextView = view.findViewById(R.id.tvAccountNumber)
        val tvAccountHolder: TextView = view.findViewById(R.id.tvAccountHolder)
        val tvDefaultBadge: TextView = view.findViewById(R.id.tvDefaultBadge)
        val rootView = view

        fun bind(account: AffiliateBankAccount) {
            tvBankName.text = account.bankName
            
            val maskedNumber = if (account.accountNumber.length > 4) "**** " + account.accountNumber.takeLast(4) else account.accountNumber
            tvAccountNumber.text = maskedNumber
            
            tvAccountHolder.text = account.accountHolder

            if (account.isDefault) {
                ivRadio.setImageResource(R.drawable.ic_radio_selected)
                tvDefaultBadge.visibility = View.VISIBLE
            } else {
                ivRadio.setImageResource(R.drawable.ic_radio_unselected)
                tvDefaultBadge.visibility = View.GONE
            }

            val resId = context.resources.getIdentifier(account.logo, "drawable", context.packageName)
            if (resId != 0) {
                ivBankLogo.setImageResource(resId)
            } else {
                ivBankLogo.setImageResource(R.drawable.ic_wallet)
            }

            rootView.setOnClickListener {
                onAccountClick(account)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.com_item_bank_account, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(accounts[position])
    }

    override fun getItemCount() = accounts.size
}
