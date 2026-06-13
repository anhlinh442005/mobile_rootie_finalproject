package com.veganbeauty.app.features.community.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.veganbeauty.app.R
import org.json.JSONArray
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class CommunityAffiliateWithdrawFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.com_fragment_affiliate_withdraw, container, false)
        
        view.findViewById<LinearLayout>(R.id.navOverview)?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
        view.findViewById<LinearLayout>(R.id.navOrders)?.setOnClickListener {
            parentFragmentManager.beginTransaction().replace(R.id.main_container, CommunityAffiliateOrdersFragment()).commit()
        }
        view.findViewById<LinearLayout>(R.id.navProducts)?.setOnClickListener {
            parentFragmentManager.beginTransaction().replace(R.id.main_container, CommunityAffiliateProductsFragment()).commit()
        }
        view.findViewById<LinearLayout>(R.id.navWithdraw)?.setOnClickListener {
            parentFragmentManager.beginTransaction().replace(R.id.main_container, CommunityAffiliateWithdrawFragment()).commit()
        }
        
        view.findViewById<LinearLayout>(R.id.llTotalWithdrawn)?.setOnClickListener {
            showHistoryDialog()
        }
        
        view.findViewById<View>(R.id.tvGuideWithdraw)?.setOnClickListener {
            showGuideDialog()
        }
        
        loadWithdrawData(view)
        
        return view
    }
    
    private fun loadWithdrawData(view: View) {
        try {
            val symbols = DecimalFormatSymbols(Locale("vi", "VN"))
            symbols.groupingSeparator = '.'
            val format = DecimalFormat("#,###đ", symbols)
            
            val jsonStr = requireContext().assets.open("affiliate.json").bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(jsonStr)
            if (jsonArray.length() == 0) return
            
            val data = jsonArray.getJSONObject(0)

            // ── Compute from orders ───────────────────────────────────────────
            val orders = data.optJSONArray("orders") ?: org.json.JSONArray()
            var successCommission = 0L
            var pendingCommission = 0L
            for (i in 0 until orders.length()) {
                val order = orders.getJSONObject(i)
                val status = order.optString("status")
                val comm = order.optLong("commission", 0)
                if (status == "Thành công" || status == "Đã duyệt") {
                    successCommission += comm
                } else if (status == "Đang xử lý" || status == "Đang chờ") {
                    pendingCommission += comm
                }
            }

            var totalWithdrawn = 0L
            val withdrawals = data.optJSONArray("withdrawals")
            if (withdrawals != null) {
                for (i in 0 until withdrawals.length()) {
                    val wd = withdrawals.getJSONObject(i)
                    val amt = wd.optLong("amount", 0)
                    when (wd.optString("status")) {
                        "Đã chuyển" -> totalWithdrawn += amt
                    }
                }
            }
            availableBalance = (successCommission - totalWithdrawn).coerceAtLeast(0L)
            // ────────────────────────────────────────────────────────────────
            
            view.findViewById<TextView>(R.id.tvAvailableBalance)?.text = format.format(availableBalance)
            view.findViewById<TextView>(R.id.tvTotalWithdrawn)?.text = format.format(totalWithdrawn)
            view.findViewById<TextView>(R.id.tvPendingAmount)?.text = format.format(pendingCommission)
            
            setupBankAccounts(view)
            setupWithdrawInput(view)
            
            view.findViewById<View>(R.id.btnAddBank)?.setOnClickListener {
                showAddBankDialog()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private var availableBalance = 0L
    private var bankAccounts = mutableListOf<AffiliateBankAccount>()
    private var adapter: AffiliateBankAccountAdapter? = null

    private fun setupBankAccounts(view: View) {
        val rvBankAccounts = view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvBankAccounts) ?: return
        
        try {
            val jsonStr = requireContext().assets.open("affiliate_bankaccount.json").bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(jsonStr)
            bankAccounts.clear()
            if (jsonArray.length() > 0) {
                val userObj = jsonArray.getJSONObject(0)
                val bankArr = userObj.optJSONArray("bank_accounts") ?: JSONArray()
                for (i in 0 until bankArr.length()) {
                    val obj = bankArr.getJSONObject(i)
                    bankAccounts.add(
                        AffiliateBankAccount(
                            id = obj.getInt("id"),
                            bankName = obj.getString("bank_name"),
                            accountNumber = obj.getString("account_number"),
                            accountHolder = obj.getString("account_holder"),
                            logo = obj.getString("logo"),
                            isDefault = obj.getBoolean("is_default")
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        adapter = AffiliateBankAccountAdapter(requireContext(), bankAccounts) { selectedAcc ->
            bankAccounts.forEach { it.isDefault = false }
            selectedAcc.isDefault = true
            adapter?.notifyDataSetChanged()
        }
        
        rvBankAccounts.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
        rvBankAccounts.adapter = adapter
    }
    
    private fun setupWithdrawInput(view: View) {
        val etAmount = view.findViewById<android.widget.EditText>(R.id.etWithdrawAmount) ?: return
        val tvWarning = view.findViewById<TextView>(R.id.tvWarningMessage)
        val tvReceive = view.findViewById<TextView>(R.id.tvReceiveAmountValue)
        val btnSubmit = view.findViewById<TextView>(R.id.btnSubmitWithdraw)
        
        val tv100k = view.findViewById<TextView>(R.id.tvAmount100k)
        val tv200k = view.findViewById<TextView>(R.id.tvAmount200k)
        val tv500k = view.findViewById<TextView>(R.id.tvAmount500k)
        val tvAll = view.findViewById<TextView>(R.id.tvAmountAll)
        
        val symbols = DecimalFormatSymbols(Locale("vi", "VN"))
        symbols.groupingSeparator = '.'
        val format = DecimalFormat("#,###", symbols)
        
        tv100k?.setOnClickListener { etAmount.setText("100.000") }
        tv200k?.setOnClickListener { etAmount.setText("200.000") }
        tv500k?.setOnClickListener { etAmount.setText("500.000") }
        tvAll?.setOnClickListener { etAmount.setText(format.format(availableBalance)) }
        
        etAmount.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            
            override fun afterTextChanged(s: android.text.Editable?) {
                etAmount.removeTextChangedListener(this)
                try {
                    val cleanString = s.toString().replace(".", "")
                    if (cleanString.isNotEmpty()) {
                        val parsed = cleanString.toLong()
                        val formatted = format.format(parsed)
                        etAmount.setText(formatted)
                        etAmount.setSelection(formatted.length)
                        
                        tvReceive?.text = "${formatted}đ"
                        
                        if (parsed > availableBalance) {
                            tvWarning?.visibility = View.VISIBLE
                            btnSubmit?.isEnabled = false
                            btnSubmit?.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#9E9E9E"))
                        } else {
                            tvWarning?.visibility = View.GONE
                            btnSubmit?.isEnabled = true
                            btnSubmit?.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#56694E"))
                        }
                    } else {
                        tvReceive?.text = "0đ"
                        tvWarning?.visibility = View.GONE
                        btnSubmit?.isEnabled = false
                        btnSubmit?.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#9E9E9E"))
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                etAmount.addTextChangedListener(this)
            }
        })
    }
    
    private fun showAddBankDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_bank_account, null)
        val builder = androidx.appcompat.app.AlertDialog.Builder(requireContext(), R.style.CustomDialogTheme)
            .setView(dialogView)
            
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        val inputBank = dialogView.findViewById<android.widget.EditText>(R.id.etBankName)
        val inputName = dialogView.findViewById<android.widget.EditText>(R.id.etAccountHolder)
        val inputNumber = dialogView.findViewById<android.widget.EditText>(R.id.etAccountNumber)
        
        dialogView.findViewById<View>(R.id.btnCancel).setOnClickListener {
            dialog.dismiss()
        }
        
        dialogView.findViewById<View>(R.id.btnAdd).setOnClickListener {
            val newId = if (bankAccounts.isEmpty()) 1 else bankAccounts.maxOf { it.id } + 1
            val newAccount = AffiliateBankAccount(
                id = newId,
                bankName = inputBank.text.toString(),
                accountNumber = inputNumber.text.toString(),
                accountHolder = inputName.text.toString(),
                logo = "ic_wallet",
                isDefault = bankAccounts.isEmpty()
            )
            bankAccounts.add(newAccount)
            adapter?.notifyDataSetChanged()
            dialog.dismiss()
        }
        
        dialog.show()
    }

    private fun showGuideDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_withdrawal_guide, null)
        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext(), R.style.CustomDialogTheme)
            .setView(dialogView)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialogView.findViewById<View>(R.id.btnClose).setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun showHistoryDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_withdrawal_history, null)
        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext(), R.style.CustomDialogTheme)
            .setView(dialogView)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialogView.findViewById<View>(R.id.btnClose).setOnClickListener { dialog.dismiss() }
        
        val llContainer = dialogView.findViewById<LinearLayout>(R.id.llWithdrawalsContainer)
        try {
            val jsonStr = requireContext().assets.open("affiliate.json").bufferedReader().use { it.readText() }
            val data = JSONArray(jsonStr).getJSONObject(0)
            val withdrawals = data.optJSONArray("withdrawals")
            if (withdrawals != null) {
                val symbols = DecimalFormatSymbols(Locale("vi", "VN"))
                symbols.groupingSeparator = '.'
                val format = DecimalFormat("#,###đ", symbols)
                for (i in 0 until withdrawals.length()) {
                    val wd = withdrawals.optJSONObject(i) ?: continue
                    val wdView = LayoutInflater.from(context).inflate(R.layout.com_item_revenue_withdrawal, llContainer, false)
                    wdView.findViewById<TextView>(R.id.tvWithdrawDate).text = wd.optString("date")
                    wdView.findViewById<TextView>(R.id.tvWithdrawAmount).text = format.format(wd.optLong("amount"))
                    val tvStatus = wdView.findViewById<TextView>(R.id.tvWithdrawStatus)
                    val status = wd.optString("status")
                    if (status == "Đã chuyển") {
                        tvStatus.text = "Đã chuyển"
                        tvStatus.setTextColor(android.graphics.Color.parseColor("#6E846A"))
                        tvStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#EAF1E7"))
                    } else {
                        tvStatus.text = status
                        tvStatus.setTextColor(android.graphics.Color.parseColor("#FF9800"))
                        tvStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#FFF3E0"))
                    }
                    llContainer.addView(wdView)
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
        
        dialog.show()
    }
}
