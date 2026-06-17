package com.veganbeauty.app.features.account.order

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.veganbeauty.app.R
import com.veganbeauty.app.core.base.RootieFragment
import com.veganbeauty.app.data.local.LocalJsonReader
import com.veganbeauty.app.data.local.RootieDatabase
import com.veganbeauty.app.data.local.entities.OrderEntity
import com.veganbeauty.app.data.repository.OrderRepository
import com.veganbeauty.app.databinding.AccountOrderListFragmentBinding
import androidx.lifecycle.lifecycleScope
import com.veganbeauty.app.data.repository.NotificationRepository
import com.veganbeauty.app.features.account.notification.AccountNotificationFragment
import kotlinx.coroutines.launch

class AccountOrderListFragment : RootieFragment() {

    private var _binding: AccountOrderListFragmentBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: OrderListViewModel

    private val orderAdapter = OrderListAdapter(
        onItemClick = { order ->
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_container, AccountOrderDetailFragment.newInstance(order.id))
                .addToBackStack(null)
                .commit()
        },
        onCancelClick = { order -> showCancelConfirmationDialog(order) },
        onDetailClick = { order -> 
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_container, AccountOrderDetailFragment.newInstance(order.id))
                .addToBackStack(null)
                .commit()
        },
        onReorderClick = { order ->
            Toast.makeText(requireContext(), "Mua lại sản phẩm từ đơn ${order.id}", Toast.LENGTH_SHORT).show()
        },
        onTrackClick = { order ->
            Toast.makeText(requireContext(), "Theo dõi đơn hàng: ${order.id}", Toast.LENGTH_SHORT).show()
        },
        onContactClick = { order ->
            Toast.makeText(requireContext(), "Kết nối với tư vấn viên Rootie hỗ trợ đơn ${order.id}", Toast.LENGTH_SHORT).show()
        },
        onReviewClick = { order ->
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_container, AccountOrderReviewFragment.newInstance(order.id))
                .addToBackStack(null)
                .commit()
        }
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = AccountOrderListFragmentBinding.inflate(inflater, container, false)
        setupViewModel()
        return binding.root
    }

    private fun setupViewModel() {
        val db = RootieDatabase.getDatabase(requireContext())
        val repository = OrderRepository(db.orderDao(), db.rewardPointDao(), db.userGiftDao(), LocalJsonReader(requireContext()))

        viewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return OrderListViewModel(repository) as T
            }
        })[OrderListViewModel::class.java]
    }

    override fun setupUI(view: View) {
        // Apply initial filter if passed in arguments
        val initialStatus = arguments?.getString(ARG_INITIAL_STATUS) ?: "Tất cả"
        viewModel.setFilter(initialStatus)

        // Setup Toolbar back button
        binding.btnBack.setOnClickListener {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }

        // Setup notification button (bind to both container and image button to prevent click consumption)
        val navigateToNotification = View.OnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_container, AccountNotificationFragment())
                .addToBackStack(null)
                .commit()
        }
        binding.layoutNotification.setOnClickListener(navigateToNotification)
        binding.btnNotification.setOnClickListener(navigateToNotification)

        // Setup RecyclerView
        binding.rvOrders.adapter = orderAdapter

        // Setup click listeners for filter tabs (including newly added Success and Cancelled tabs)
        binding.tabAll.setOnClickListener { viewModel.setFilter("Tất cả") }
        binding.tabPending.setOnClickListener { viewModel.setFilter("Chờ xác nhận") }
        binding.tabProcessing.setOnClickListener { viewModel.setFilter("Đang xử lý") }
        binding.tabDelivering.setOnClickListener { viewModel.setFilter("Đang giao") }
        binding.tabSuccess.setOnClickListener { viewModel.setFilter("Hoàn tất") }
        binding.tabCancelled.setOnClickListener { viewModel.setFilter("Đã hủy") }
    }

    override fun observeViewModel() {
        // Observe and update orders list
        viewModel.filteredOrders.observe(viewLifecycleOwner) { orders ->
            orderAdapter.submitList(orders)
        }

        // Observe and update order stats
        viewModel.orderStats.observe(viewLifecycleOwner) { stats ->
            binding.tvOrderStats.text = stats
        }

        // Observe and update tab selection styling
        viewModel.selectedStatus.observe(viewLifecycleOwner) { status ->
            updateTabStyles(status)
        }

        // Observe and update notification unread count badge reactively using viewLifecycleOwner and safe binding checks
        viewLifecycleOwner.lifecycleScope.launch {
            NotificationRepository.getInstance(requireContext()).unreadCount.collect { count ->
                _binding?.viewNotificationBadge?.visibility = if (count > 0) View.VISIBLE else View.GONE
            }
        }
    }

    private fun updateTabStyles(activeStatus: String) {
        val tabs = mapOf(
            "Tất cả" to binding.tabAll,
            "Chờ xác nhận" to binding.tabPending,
            "Đang xử lý" to binding.tabProcessing,
            "Đang giao" to binding.tabDelivering,
            "Hoàn tất" to binding.tabSuccess,
            "Đã hủy" to binding.tabCancelled
        )

        for ((status, textView) in tabs) {
            if (status.equals(activeStatus, ignoreCase = true)) {
                textView.setBackgroundResource(R.drawable.tab_active_bg)
                textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            } else {
                textView.setBackgroundResource(R.drawable.tab_inactive_bg)
                textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary))
            }
        }
    }

    private fun showCancelConfirmationDialog(order: OrderEntity) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Hủy đơn hàng")
            .setMessage("Bạn có chắc chắn muốn hủy đơn hàng ${order.id} không?")
            .setPositiveButton("Xác nhận") { _, _ ->
                viewModel.cancelOrder(order.id)
                Toast.makeText(
                    requireContext(),
                    "Đã hủy đơn hàng ${order.id} thành công!",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .setNegativeButton("Quay lại", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_INITIAL_STATUS = "arg_initial_status"

        fun newInstance(initialStatus: String): AccountOrderListFragment {
            val fragment = AccountOrderListFragment()
            val args = Bundle()
            args.putString(ARG_INITIAL_STATUS, initialStatus)
            fragment.arguments = args
            return fragment
        }
    }
}

