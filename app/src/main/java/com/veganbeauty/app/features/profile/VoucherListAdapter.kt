package com.veganbeauty.app.features.profile

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.veganbeauty.app.R
import com.veganbeauty.app.databinding.ItemVoucherBinding

data class VoucherItem(
    val id: String,
    val title: String,
    val description: String,
    val code: String,
    val status: String, // valid, expiring, expired
    val hsd: String, // format: "yyyy-MM-dd HH:mm:ss"
    val type: String, // discount, free ship, voucher_discount, voucher_freeship
    val fromGift: Boolean,
    val quantity: Int? = null,
    val minOrderValue: Int = 0,
    val applicableProducts: String = "Tất cả sản phẩm",
    val offerType: String = "fixed_amount",
    val discountValue: Int = 0
)

class VoucherListAdapter(
    private var vouchers: List<VoucherItem>
) : RecyclerView.Adapter<VoucherListAdapter.VoucherViewHolder>() {

    var onDeleteClickListener: ((VoucherItem) -> Unit)? = null
    var onItemClickListener: ((VoucherItem) -> Unit)? = null
    var onUseClickListener: ((VoucherItem) -> Unit)? = null

    class VoucherViewHolder(val binding: ItemVoucherBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VoucherViewHolder {
        val binding = ItemVoucherBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VoucherViewHolder(binding)
    }

    override fun onBindViewHolder(holder: VoucherViewHolder, position: Int) {
        val item = vouchers[position]
        val context = holder.itemView.context
        val binding = holder.binding

        holder.itemView.setOnClickListener {
            onItemClickListener?.invoke(item)
        }

        // Bind data
        binding.tvVoucherTitle.text = item.title
        binding.tvVoucherDesc.text = item.description
        
        // Expiration display format
        val displayHsd = if (item.hsd.contains(" ")) item.hsd.split(" ")[0] else item.hsd
        // If it expires today, we can format it differently if needed, e.g. "Hôm nay"
        binding.tvVoucherHsd.text = "HSD: $displayHsd"
        binding.tvVoucherCodeLabel.text = "MÃ: ${item.code}"

        // Set Icons and Tints depending on type
        when (item.type.lowercase().replace(" ", "").replace("_", "")) {
            "freeship", "voucherfreeship" -> {
                binding.ivVoucherIcon.setImageResource(R.drawable.ic_truck)
                binding.ivVoucherIcon.setColorFilter(Color.parseColor("#556348"))
                binding.ivVoucherIconBg.setBackgroundResource(R.drawable.bg_icon_rounded_green)
            }
            "gift", "productgift", "product", "product_gift" -> {
                binding.ivVoucherIcon.setImageResource(R.drawable.ic_gift)
                binding.ivVoucherIcon.setColorFilter(Color.parseColor("#02542D"))
                binding.ivVoucherIconBg.setBackgroundResource(R.drawable.bg_icon_rounded_green)
            }
            "discount", "voucherdiscount" -> {
                binding.ivVoucherIcon.setImageResource(R.drawable.ic_voucher)
                binding.ivVoucherIcon.setColorFilter(Color.parseColor("#02542D"))
                binding.ivVoucherIconBg.setBackgroundResource(R.drawable.bg_icon_rounded_green)
            }
            else -> { // expired_block or other
                binding.ivVoucherIcon.setImageResource(R.drawable.ic_cancel)
                binding.ivVoucherIcon.setColorFilter(Color.parseColor("#888888"))
                binding.ivVoucherIconBg.setBackgroundResource(R.drawable.bg_icon_rounded_grey)
            }
        }

        // Apply Layout Styling based on status: valid, expiring, expired
        when (item.status) {
            "valid", "expiring" -> {
                val isValid = item.status == "valid"
                binding.layoutCardContainer.setBackgroundResource(
                    if (isValid) R.drawable.bg_card_white else R.drawable.bg_card_expiring
                )
                
                // Show/hide low-quantity pill or expiring badge
                val isExpiringToday = item.status == "expiring"
                val isLowQuantity = !item.fromGift && item.quantity != null && item.quantity < 10
                
                if (isExpiringToday) {
                    binding.tvVoucherExpiringBadge.visibility = View.VISIBLE
                    binding.tvVoucherExpiringBadge.text = "Sắp hết hạn"
                } else if (isLowQuantity) {
                    binding.tvVoucherExpiringBadge.visibility = View.VISIBLE
                    binding.tvVoucherExpiringBadge.text = "Sắp hết"
                } else {
                    binding.tvVoucherExpiringBadge.visibility = View.GONE
                }
                
                binding.btnVoucherDelete.visibility = View.GONE
                binding.btnVoucherDelete.setOnClickListener(null)
                
                binding.tvVoucherStatusTag.text = if (isExpiringToday) "Sắp hết" else "Còn hạn"
                binding.tvVoucherStatusTag.setBackgroundResource(
                    if (isExpiringToday) R.drawable.bg_tag_expiring else R.drawable.bg_tag_valid
                )
                binding.tvVoucherStatusTag.setTextColor(
                    if (isExpiringToday) Color.parseColor("#C62828") else Color.parseColor("#2E7D32")
                )
                
                binding.tvVoucherTitle.paintFlags = binding.tvVoucherTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                binding.tvVoucherTitle.setTextColor(Color.parseColor("#3E4D44"))
                binding.tvVoucherDesc.setTextColor(Color.parseColor("#555555"))
                binding.tvVoucherHsd.setTextColor(
                    if (isExpiringToday) Color.parseColor("#C62828") else Color.parseColor("#888888")
                )
                binding.tvVoucherCodeLabel.setTextColor(Color.parseColor("#666666"))

                binding.btnVoucherAction.text = "Sử dụng ngay"
                binding.btnVoucherAction.setBackgroundResource(R.drawable.bg_button_copy)
                binding.btnVoucherAction.setTextColor(Color.WHITE)
                binding.btnVoucherAction.isEnabled = true
                binding.btnVoucherAction.setOnClickListener {
                    onUseClickListener?.invoke(item)
                }
            }
            "expired" -> {
                binding.layoutCardContainer.setBackgroundResource(R.drawable.bg_card_expired)
                binding.tvVoucherExpiringBadge.visibility = View.GONE
                binding.btnVoucherDelete.visibility = View.VISIBLE
                binding.btnVoucherDelete.setOnClickListener {
                    onDeleteClickListener?.invoke(item)
                }
                
                binding.tvVoucherStatusTag.text = "Hết hạn"
                binding.tvVoucherStatusTag.setBackgroundResource(R.drawable.bg_tag_expired)
                binding.tvVoucherStatusTag.setTextColor(Color.parseColor("#888888"))
                
                // Crossed-out / strike-through title
                binding.tvVoucherTitle.paintFlags = binding.tvVoucherTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                binding.tvVoucherTitle.setTextColor(Color.parseColor("#888888"))
                binding.tvVoucherDesc.setTextColor(Color.parseColor("#999999"))
                binding.tvVoucherHsd.setTextColor(Color.parseColor("#999999"))
                binding.tvVoucherCodeLabel.setTextColor(Color.parseColor("#999999"))

                binding.btnVoucherAction.text = "Đã hết hạn"
                binding.btnVoucherAction.setBackgroundResource(R.drawable.bg_button_disabled)
                binding.btnVoucherAction.setTextColor(Color.parseColor("#888888"))
                binding.btnVoucherAction.isEnabled = false
                binding.btnVoucherAction.setOnClickListener(null)
            }
        }
    }

    override fun getItemCount(): Int = vouchers.size

    fun updateList(newList: List<VoucherItem>) {
        vouchers = newList
        notifyDataSetChanged()
    }

    private fun copyToClipboard(context: Context, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Voucher Code", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Đã sao chép mã: $text", Toast.LENGTH_SHORT).show()
    }
}
