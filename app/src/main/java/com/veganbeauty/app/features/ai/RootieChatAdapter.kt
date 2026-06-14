package com.veganbeauty.app.features.ai

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.veganbeauty.app.R
import com.veganbeauty.app.data.local.LocalJsonReader
import com.veganbeauty.app.data.local.entities.ProductEntity
import com.veganbeauty.app.features.shop.product.CartHelper
import kotlinx.coroutines.CoroutineScope
import java.text.NumberFormat
import java.util.Locale

data class RootieChatItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: Sender,
    val messageText: String = "",
    val timeStr: String = "",
    val type: ItemType = ItemType.TEXT,
    val diagnosticData: DiagnosticData? = null
) {
    enum class Sender { USER, AI }
    enum class ItemType { TEXT, DIAGNOSTIC }

    data class DiagnosticData(
        val assessment: String,
        val detailExplanation: String,
        val moistureVal: String,
        val sensitivityVal: String,
        val barrierVal: String,
        val whyExplanation: String,
        val recommendedProductIds: List<String>,
        val productPhases: List<String>,
        val productSubcategories: List<String>,
        val productExpertReasons: List<String>
    )
}

class RootieChatAdapter(
    private val context: Context,
    private val coroutineScope: CoroutineScope
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val items = mutableListOf<RootieChatItem>()
    private val allProducts: List<ProductEntity> by lazy {
        LocalJsonReader(context).getAllProducts()
    }

    companion object {
        private const val TYPE_USER = 0
        private const val TYPE_AI_TEXT = 1
        private const val TYPE_AI_DIAGNOSTIC = 2
    }

    fun getItems(): List<RootieChatItem> = items

    fun submitList(newItems: List<RootieChatItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun addMessage(item: RootieChatItem) {
        items.add(item)
        notifyItemInserted(items.size - 1)
    }

    fun removeMessageAt(position: Int) {
        if (position in 0 until items.size) {
            items.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, items.size)
        }
    }

    override fun getItemViewType(position: Int): Int {
        val item = items[position]
        return if (item.sender == RootieChatItem.Sender.USER) {
            TYPE_USER
        } else {
            if (item.type == RootieChatItem.ItemType.DIAGNOSTIC) TYPE_AI_DIAGNOSTIC else TYPE_AI_TEXT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_USER -> {
                val view = inflater.inflate(R.layout.item_chat_user, parent, false)
                UserViewHolder(view)
            }
            TYPE_AI_TEXT -> {
                val view = inflater.inflate(R.layout.item_chat_ai_text, parent, false)
                AiTextViewHolder(view)
            }
            else -> {
                val view = inflater.inflate(R.layout.item_chat_ai_diagnostic, parent, false)
                AiDiagnosticViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        when (holder) {
            is UserViewHolder -> holder.bind(item)
            is AiTextViewHolder -> holder.bind(item)
            is AiDiagnosticViewHolder -> holder.bind(item)
        }
    }

    override fun getItemCount(): Int = items.size

    // ViewHolders
    inner class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvMessage: TextView = view.findViewById(R.id.tvUserMessage)
        private val tvTime: TextView = view.findViewById(R.id.tvUserTime)

        fun bind(item: RootieChatItem) {
            tvMessage.text = item.messageText
            tvTime.text = item.timeStr
        }
    }

    inner class AiTextViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvMessage: TextView = view.findViewById(R.id.tvAiMessage)
        private val tvTime: TextView = view.findViewById(R.id.tvAiTime)
        private val ivAvatar: ImageView = view.findViewById(R.id.ivAiAvatar)

        fun bind(item: RootieChatItem) {
            tvMessage.text = item.messageText
            tvTime.text = item.timeStr
            ivAvatar.setImageResource(R.drawable.mascot_message)
        }
    }

    inner class AiDiagnosticViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvAssessment: TextView = view.findViewById(R.id.tvDiagnosticAssessment)
        private val tvDetail: TextView = view.findViewById(R.id.tvDiagnosticDetail)
        private val tvMoistureVal: TextView = view.findViewById(R.id.tvMetricMoistureVal)
        private val tvSensitivityVal: TextView = view.findViewById(R.id.tvMetricSensitivityVal)
        private val tvBarrierVal: TextView = view.findViewById(R.id.tvMetricBarrierVal)
        private val tvWhyContent: TextView = view.findViewById(R.id.tvWhyContent)
        private val layoutProducts: LinearLayout = view.findViewById(R.id.layoutDiagnosticProducts)
        private val btnAddToCartAll: View = view.findViewById(R.id.btnAddToCartAll)
        private val ivAvatar: ImageView = view.findViewById(R.id.ivAiAvatar)

        fun bind(item: RootieChatItem) {
            ivAvatar.setImageResource(R.drawable.mascot_message)
            val data = item.diagnosticData ?: return

            tvAssessment.text = data.assessment
            tvDetail.text = data.detailExplanation
            tvMoistureVal.text = data.moistureVal
            tvSensitivityVal.text = data.sensitivityVal
            tvBarrierVal.text = data.barrierVal
            tvWhyContent.text = data.whyExplanation

            // Clear and populate products
            layoutProducts.removeAllViews()

            val productsToRecommend = mutableListOf<ProductEntity>()

            data.recommendedProductIds.forEachIndexed { idx, prodId ->
                val product = allProducts.find { it.id == prodId }
                if (product != null) {
                    productsToRecommend.add(product)

                    val pView = LayoutInflater.from(context).inflate(R.layout.item_chat_diagnostic_product, layoutProducts, false)
                    val ivProdImage: ImageView = pView.findViewById(R.id.ivProductImage)
                    val tvPhase: TextView = pView.findViewById(R.id.tvProductPhase)
                    val tvPrice: TextView = pView.findViewById(R.id.tvProductPrice)
                    val tvSubcat: TextView = pView.findViewById(R.id.tvProductSubcategory)
                    val tvName: TextView = pView.findViewById(R.id.tvProductName)
                    val tvReason: TextView = pView.findViewById(R.id.tvExpertReason)

                    // Bind data
                    ivProdImage.load(product.mainImage) {
                        crossfade(true)
                        placeholder(android.R.color.darker_gray)
                    }

                    tvPhase.text = data.productPhases.getOrNull(idx) ?: "GIAI ĐOẠN ${idx + 1}"
                    
                    val formattedPrice = if (product.price >= 1000) {
                        "${product.price / 1000}K"
                    } else {
                        "${product.price}đ"
                    }
                    tvPrice.text = formattedPrice

                    tvSubcat.text = data.productSubcategories.getOrNull(idx) ?: product.category.uppercase()
                    tvName.text = product.name
                    tvReason.text = data.productExpertReasons.getOrNull(idx) ?: "Lý do khuyên dùng bởi chuyên gia Rootie."

                    layoutProducts.addView(pView)
                }
            }

            // Click listener to add all recommended items to cart
            btnAddToCartAll.setOnClickListener {
                if (productsToRecommend.isNotEmpty()) {
                    var addedCount = 0
                    productsToRecommend.forEach { prod ->
                        CartHelper.addToCart(context, coroutineScope, prod, 1)
                        addedCount++
                    }
                }
            }
        }
    }
}
