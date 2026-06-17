package com.veganbeauty.app.features.community.notification

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.veganbeauty.app.R
import com.veganbeauty.app.core.base.RootieFragment
import com.veganbeauty.app.databinding.ComFragmentNotificationBinding

class CommunityNotificationFragment : RootieFragment() {

    private var _binding: ComFragmentNotificationBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ComNotificationViewModel

    private val adapter by lazy {
        ComNotificationAdapter(
            onItemClick = { item ->
                viewModel.markAsRead(requireContext(), item.id)
                when (item.type) {
                    "POST", "INTERACTION" -> {
                        if (!item.postId.isNullOrEmpty()) {
                            val isNewsPost = item.postId.all { it.isDigit() }
                            if (isNewsPost) {
                                val newsFragment = com.veganbeauty.app.features.community.beauty_hub.CommunityNewsFragment.newInstance(item.postId)
                                parentFragmentManager.beginTransaction()
                                    .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                                    .replace(R.id.main_container, newsFragment)
                                    .addToBackStack(null)
                                    .commit()
                            } else {
                                val targetUserId = item.userId ?: "test_001"
                                val postDetailFragment = com.veganbeauty.app.features.community.profile.ProfilePostDetailFragment.newInstance(targetUserId, 0, item.postId)
                                parentFragmentManager.beginTransaction()
                                    .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                                    .replace(R.id.main_container, postDetailFragment)
                                    .addToBackStack(null)
                                    .commit()
                            }

                            val shouldShowComments = item.actionType == "COMMENT" || item.actionType == "REPLY" || item.actionType == "LIKE" || item.content.contains("bình luận")
                            if (shouldShowComments) {
                                view?.postDelayed({
                                    if (isAdded) {
                                        val commentBottomSheet = com.veganbeauty.app.features.community.com_feed.CommunityCommentBottomSheet.newInstance(item.postId, 5, item.commentId)
                                        commentBottomSheet.show(parentFragmentManager, com.veganbeauty.app.features.community.com_feed.CommunityCommentBottomSheet.TAG)
                                    }
                                }, 350)
                            }
                        }
                    }
                    "ORDER" -> {
                        if (item.actionType == "WITHDRAW") {
                            val wdFragment = WithdrawalDetailPlaceholderFragment.newInstance(
                                withdrawId = "#WD20260615",
                                amount = "500.000đ",
                                date = "${item.time} • ${item.date}",
                                status = "Thành công"
                            )
                            parentFragmentManager.beginTransaction()
                                .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                                .replace(R.id.main_container, wdFragment)
                                .addToBackStack(null)
                                .commit()
                        } else {
                            context?.let { ctx ->
                                Toast.makeText(ctx, "Chi tiết đơn hàng: ${item.userName}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            },
            onDeleteClick = { item ->
                viewModel.deleteNotification(requireContext(), item.id)
                context?.let { ctx ->
                    Toast.makeText(ctx, "Đã xóa thông báo", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ComFragmentNotificationBinding.inflate(inflater, container, false)
        setupViewModel()
        return binding.root
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(this)[ComNotificationViewModel::class.java]
        viewModel.initData(requireContext())
    }

    override fun setupUI(view: View) {
        // Back Navigation with slide pop transition
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Set adapter
        binding.rvNotifications.adapter = adapter

        // Tab selection click listeners
        binding.tabPosts.setOnClickListener { viewModel.selectTab("POST") }
        binding.tabInteractions.setOnClickListener { viewModel.selectTab("INTERACTION") }
        binding.tabOrders.setOnClickListener { viewModel.selectTab("ORDER") }

        // Mark all read
        binding.btnMarkRead.setOnClickListener {
            viewModel.markAllRead(requireContext())
            context?.let { ctx ->
                Toast.makeText(ctx, "Đã đánh dấu đọc tất cả thông báo!", Toast.LENGTH_SHORT).show()
            }
        }

        // Delete all
        binding.btnDeleteAll.setOnClickListener {
            viewModel.deleteAllNotifications(requireContext())
            context?.let { ctx ->
                Toast.makeText(ctx, "Đã xóa tất cả thông báo trong mục này!", Toast.LENGTH_SHORT).show()
            }
        }

        // Search text watcher
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.setSearchQuery(s?.toString().orEmpty())
            }
        })
    }

    override fun observeViewModel() {
        // Observe filtered notifications
        viewModel.filteredNotifications.observe(viewLifecycleOwner) { items ->
            if (items.isNullOrEmpty()) {
                binding.rvNotifications.visibility = View.GONE
                binding.layoutEmptyState.visibility = View.VISIBLE
            } else {
                binding.rvNotifications.visibility = View.VISIBLE
                binding.layoutEmptyState.visibility = View.GONE
                adapter.submitList(items)
            }
        }

        // Observe active tab to update visual indicator
        viewModel.activeTab.observe(viewLifecycleOwner) { activeTab ->
            updateTabStyles(activeTab)
        }
    }

    private fun updateTabStyles(activeTab: String) {
        val postsActive = activeTab == "POST"
        val interactionsActive = activeTab == "INTERACTION"
        val ordersActive = activeTab == "ORDER"

        // Set backgrounds
        binding.tabPosts.setBackgroundResource(if (postsActive) R.drawable.tab_active_bg else R.drawable.tab_inactive_bg)
        binding.tabInteractions.setBackgroundResource(if (interactionsActive) R.drawable.tab_active_bg else R.drawable.tab_inactive_bg)
        binding.tabOrders.setBackgroundResource(if (ordersActive) R.drawable.tab_active_bg else R.drawable.tab_inactive_bg)

        // Set text colors
        val whiteColor = ContextCompat.getColor(requireContext(), R.color.white)
        val primaryColor = ContextCompat.getColor(requireContext(), R.color.primary)

        binding.tabPosts.setTextColor(if (postsActive) whiteColor else primaryColor)
        binding.tabInteractions.setTextColor(if (interactionsActive) whiteColor else primaryColor)
        binding.tabOrders.setTextColor(if (ordersActive) whiteColor else primaryColor)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
