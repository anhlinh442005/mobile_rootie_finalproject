package com.veganbeauty.app.features.community.notification

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.veganbeauty.app.R

/**
 * Placeholder fragment displaying details of an affiliate withdrawal request.
 * Note: This screen is a mock/placeholder detail view as requested by the user,
 * designed to show withdrawal status and transaction info.
 */
class WithdrawalDetailPlaceholderFragment : Fragment() {

    private var withdrawId: String = "#WD20260615"
    private var amount: String = "500.000đ"
    private var date: String = "15/06/2026"
    private var status: String = "Thành công"

    companion object {
        fun newInstance(
            withdrawId: String,
            amount: String,
            date: String,
            status: String
        ): WithdrawalDetailPlaceholderFragment {
            val fragment = WithdrawalDetailPlaceholderFragment()
            val args = Bundle()
            args.putString("WITHDRAW_ID", withdrawId)
            args.putString("AMOUNT", amount)
            args.putString("DATE", date)
            args.putString("STATUS", status)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            withdrawId = it.getString("WITHDRAW_ID") ?: "#WD20260615"
            amount = it.getString("AMOUNT") ?: "500.000đ"
            date = it.getString("DATE") ?: "15/06/2026"
            status = it.getString("STATUS") ?: "Thành công"
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.com_fragment_withdrawal_detail_placeholder, container, false)

        view.findViewById<View>(R.id.btnBack)?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        view.findViewById<TextView>(R.id.tvAmount)?.text = amount
        view.findViewById<TextView>(R.id.tvTransactionCode)?.text = withdrawId
        view.findViewById<TextView>(R.id.tvRequestTime)?.text = date
        view.findViewById<TextView>(R.id.tvStatusBadge)?.text = status

        return view
    }
}
