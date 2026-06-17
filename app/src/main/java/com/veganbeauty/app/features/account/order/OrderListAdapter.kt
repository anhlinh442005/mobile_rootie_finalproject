package com.veganbeauty.app.features.account.order

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.veganbeauty.app.R
import com.veganbeauty.app.data.local.entities.OrderEntity
import com.veganbeauty.app.databinding.AccountOrderItemBinding

class OrderListAdapter(
    private val onItemClick: (OrderEntity) -> Unit = {},
    private val onCancelClick: (OrderEntity) -> Unit = {},
    private val onDetailClick: (OrderEntity) -> Unit = {},
    private val onReorderClick: (OrderEntity) -> Unit = {},
    private val onTrackClick: (OrderEntity) -> Unit = {},
    private val onContactClick: (OrderEntity) -> Unit = {},
    private val onReviewClick: (OrderEntity) -> Unit = {}
) : ListAdapter<OrderEntity, OrderListAdapter.OrderViewHolder>(OrderDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val binding = AccountOrderItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return OrderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        holder.bind(
            getItem(position),
            onItemClick,
            onCancelClick,
            onDetailClick,
            onReorderClick,
            onTrackClick,
            onContactClick,
            onReviewClick
        )
    }

    class OrderViewHolder(private val binding: AccountOrderItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(
            order: OrderEntity,
            onItemClick: (OrderEntity) -> Unit,
            onCancelClick: (OrderEntity) -> Unit,
            onDetailClick: (OrderEntity) -> Unit,
            onReorderClick: (OrderEntity) -> Unit,
            onTrackClick: (OrderEntity) -> Unit,
            onContactClick: (OrderEntity) -> Unit,
            onReviewClick: (OrderEntity) -> Unit
        ) {
            val context = binding.root.context

            // Bind root click listener for order detail navigation (phần này làm sau)
            binding.root.setOnClickListener { onItemClick(order) }

            // Bind Date and Time
            binding.tvOrderDateTime.text = "${order.orderDate} • ${order.orderTime}"
            binding.tvOrderId.text = order.id

            // Bind Status Badge
            binding.tvOrderStatus.text = order.status
            val (bgResId, textResId) = when (order.status) {
                "Chờ xác nhận" -> Pair(R.color.status_pending_bg, R.color.status_pending_text)
                "Đang xử lý" -> Pair(R.color.status_processing_bg, R.color.status_processing_text)
                "Đang giao" -> Pair(R.color.status_delivering_bg, R.color.status_delivering_text)
                "Hoàn tất" -> Pair(R.color.status_success_bg, R.color.status_success_text)
                "Đã hủy" -> Pair(R.color.status_cancelled_bg, R.color.status_cancelled_text)
                else -> Pair(R.color.status_pending_bg, R.color.status_pending_text)
            }
            binding.tvOrderStatus.background.mutate().setTint(ContextCompat.getColor(context, bgResId))
            binding.tvOrderStatus.setTextColor(ContextCompat.getColor(context, textResId))

            // Product Container Header
            binding.tvProductCountLabel.text = "Sản phẩm (${order.items.size})"
            
            // State for expand/collapse (defaults to false meaning collapsed)
            var isExpanded = false
            
            fun updateProductsVisibility() {
                binding.llProductsContainer.removeAllViews()
                val itemsToShow = if (isExpanded) order.items else order.items.take(2)
                
                for (item in itemsToShow) {
                    val rowView = LayoutInflater.from(context).inflate(R.layout.item_account_order_product_row, binding.llProductsContainer, false)
                    
                    val tvName = rowView.findViewById<android.widget.TextView>(R.id.tvProductName)
                    val tvQuantity = rowView.findViewById<android.widget.TextView>(R.id.tvProductQuantity)
                    val tvPrice = rowView.findViewById<android.widget.TextView>(R.id.tvProductPrice)
                    val ivImage = rowView.findViewById<android.widget.ImageView>(R.id.ivProductImage)
                    
                    tvName.text = item.productName
                    tvQuantity.text = "x${item.quantity}"
                    
                    val itemPriceFormatted = String.format("%,dđ", item.price).replace(',', '.')
                    tvPrice.text = itemPriceFormatted
                    
                    ivImage.load(item.productImage) {
                        crossfade(true)
                        placeholder(android.R.color.darker_gray)
                        error(android.R.color.darker_gray)
                    }
                    
                    binding.llProductsContainer.addView(rowView)
                }
                
                if (order.items.size > 2) {
                    binding.llToggleProducts.visibility = View.VISIBLE
                    if (isExpanded) {
                        binding.tvToggleText.text = "Thu gọn"
                        binding.ivToggleIcon.setImageResource(R.drawable.ic_chevron_up_thin)
                    } else {
                        binding.tvToggleText.text = "Xem thêm"
                        binding.ivToggleIcon.setImageResource(R.drawable.ic_chevron_down_thin)
                    }
                } else {
                    binding.llToggleProducts.visibility = View.GONE
                }
            }
            
            binding.llToggleProducts.setOnClickListener {
                isExpanded = !isExpanded
                updateProductsVisibility()
            }
            
            // Initial render
            updateProductsVisibility()

            // Formatting Price as 395.000đ
            val formattedAmount = String.format("%,dđ", order.totalAmount).replace(',', '.')
            binding.tvTotalAmount.text = formattedAmount

            // Dynamic Action Buttons
            setupActionButtons(
                order,
                onCancelClick,
                onDetailClick,
                onReorderClick,
                onTrackClick,
                onContactClick,
                onReviewClick
            )
        }

        private fun setupActionButtons(
            order: OrderEntity,
            onCancelClick: (OrderEntity) -> Unit,
            onDetailClick: (OrderEntity) -> Unit,
            onReorderClick: (OrderEntity) -> Unit,
            onTrackClick: (OrderEntity) -> Unit,
            onContactClick: (OrderEntity) -> Unit,
            onReviewClick: (OrderEntity) -> Unit
        ) {
            when (order.status) {
                "Chờ xác nhận" -> {
                    // Outlined: Hủy đơn, Filled: (Gone)
                    binding.btnActionOutlined.visibility = View.VISIBLE
                    binding.btnActionOutlined.text = "Hủy đơn"
                    binding.btnActionOutlined.setOnClickListener { onCancelClick(order) }

                    binding.btnActionFilled.visibility = View.GONE
                }
                "Đang xử lý" -> {
                    // Outlined: Liên hệ, Filled: (Gone)
                    binding.btnActionOutlined.visibility = View.VISIBLE
                    binding.btnActionOutlined.text = "Liên hệ"
                    binding.btnActionOutlined.setOnClickListener { onContactClick(order) }

                    binding.btnActionFilled.visibility = View.GONE
                }
                "Đang giao" -> {
                    // Outlined: Liên hệ, Filled: Theo dõi
                    binding.btnActionOutlined.visibility = View.VISIBLE
                    binding.btnActionOutlined.text = "Liên hệ"
                    binding.btnActionOutlined.setOnClickListener { onContactClick(order) }

                    binding.btnActionFilled.visibility = View.VISIBLE
                    binding.btnActionFilled.text = "Theo dõi"
                    binding.btnActionFilled.setOnClickListener { onTrackClick(order) }
                }
                "Hoàn tất" -> {
                    // Outlined: Đánh giá/Xem đánh giá, Filled: Mua lại
                    binding.btnActionOutlined.visibility = View.VISIBLE
                    binding.btnActionOutlined.text = if (order.hasReview) "Xem đánh giá" else "Đánh giá"
                    binding.btnActionOutlined.setOnClickListener { onReviewClick(order) }

                    binding.btnActionFilled.visibility = View.VISIBLE
                    binding.btnActionFilled.text = "Mua lại"
                    binding.btnActionFilled.setOnClickListener { onReorderClick(order) }
                }

                "Đã hủy" -> {
                    // Outlined: (Gone) - Chi tiết is removed, Filled: Mua lại
                    binding.btnActionOutlined.visibility = View.GONE

                    binding.btnActionFilled.visibility = View.VISIBLE
                    binding.btnActionFilled.text = "Mua lại"
                    binding.btnActionFilled.setOnClickListener { onReorderClick(order) }
                }
                else -> {
                    binding.btnActionOutlined.visibility = View.GONE
                    binding.btnActionFilled.visibility = View.GONE
                }
            }
        }
    }

    class OrderDiffCallback : DiffUtil.ItemCallback<OrderEntity>() {
        override fun areItemsTheSame(oldItem: OrderEntity, newItem: OrderEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: OrderEntity, newItem: OrderEntity): Boolean {
            return oldItem == newItem
        }
    }
}
