package com.veganbeauty.app.features.account.notification

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.veganbeauty.app.R
import com.veganbeauty.app.core.base.RootieFragment
import com.veganbeauty.app.data.repository.NotificationRepository
import com.veganbeauty.app.databinding.AccountNotificationFragmentBinding

class AccountNotificationFragment : RootieFragment() {

    private var _binding: AccountNotificationFragmentBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: NotificationViewModel

    private val listAdapter by lazy {
        NotificationListAdapter(
            onItemClick = { item ->
                viewModel.markAsRead(item.id)
                context?.let { ctx ->
                    Toast.makeText(
                        ctx,
                        "Đã đọc: ${item.title}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },
            onActionClick = { item ->
                viewModel.markAsRead(item.id)
                context?.let { ctx ->
                    if (item.actionText == "COPY MÃ" && !item.voucherCode.isNullOrEmpty()) {
                        copyToClipboard(ctx, item.voucherCode)
                        Toast.makeText(
                            ctx,
                            "Đã sao chép mã voucher ${item.voucherCode} thành công!",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            ctx,
                            "Thực hiện hành động: ${item.actionText} cho ${item.title}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            },
            onBannerClick = {
                context?.let { ctx ->
                    Toast.makeText(
                        ctx,
                        "Cảm ơn bạn đã đồng hành cùng Rootie trong chiến dịch sống xanh bảo vệ môi trường!",
                        Toast.LENGTH_LONG
                    ).show()
                }
            },
            onLeftGridClick = {
                context?.let { ctx ->
                    Toast.makeText(
                        ctx,
                        "Tính năng tích điểm đổi quà sẽ sớm được ra mắt trong phiên bản tiếp theo!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },
            onRightGridClick = {
                context?.let { ctx ->
                    Toast.makeText(
                        ctx,
                        "Hạng Thành viên Bạc: Tận hưởng đặc quyền giảm 6% và quà sinh nhật đặc biệt từ Rootie!",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = AccountNotificationFragmentBinding.inflate(inflater, container, false)
        setupViewModel()
        return binding.root
    }

    private fun setupViewModel() {
        val repository = NotificationRepository.getInstance(requireContext())
        viewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return NotificationViewModel(repository) as T
            }
        })[NotificationViewModel::class.java]
    }

    override fun setupUI(view: View) {
        // Back Navigation
        binding.btnBack.setOnClickListener {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }

        // Bind RecyclerView
        binding.rvNotifications.adapter = listAdapter

        // Bind Chips/Tabs Clicks
        binding.tabAll.setOnClickListener { viewModel.selectTab("Tất cả") }
        binding.tabPromo.setOnClickListener { viewModel.selectTab("Khuyến mãi") }
        binding.tabOrder.setOnClickListener { viewModel.selectTab("Đơn hàng") }
        binding.tabMessage.setOnClickListener { viewModel.selectTab("Tin nhắn") }

        // Bind Search Input Bar
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.setSearchQuery(s?.toString().orEmpty())
            }
        })
    }

    override fun observeViewModel() {
        // Observe search and tab filters to render lists
        viewModel.notificationItems.observe(viewLifecycleOwner) { items ->
            listAdapter.submitList(items)
        }

        // Observe active tab to update visual styles dynamically
        viewModel.selectedTab.observe(viewLifecycleOwner) { activeTab ->
            updateTabStyles(activeTab)
        }
    }

    private fun updateTabStyles(activeTab: String) {
        val tabs = mapOf(
            "Tất cả" to binding.tabAll,
            "Khuyến mãi" to binding.tabPromo,
            "Đơn hàng" to binding.tabOrder,
            "Tin nhắn" to binding.tabMessage
        )

        for ((tab, textView) in tabs) {
            if (tab.equals(activeTab, ignoreCase = true)) {
                textView.setBackgroundResource(R.drawable.tab_active_bg)
                textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            } else {
                textView.setBackgroundResource(R.drawable.tab_inactive_bg)
                textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary))
            }
        }
    }

    private fun copyToClipboard(context: Context, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Rootie Voucher Code", text)
        clipboard.setPrimaryClip(clip)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
