package com.veganbeauty.app.features.home.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.veganbeauty.app.databinding.HomeItemBannerBinding

data class HomeBannerItem(
    val imageRes: Int? = null,
    val imageUrl: String? = null,
    val title: String = "",
    val actionText: String = ""
)

class HomeBannerAdapter : RecyclerView.Adapter<HomeBannerAdapter.BannerViewHolder>() {

    private val items = mutableListOf<HomeBannerItem>()

    fun submitBanners(banners: List<HomeBannerItem>) {
        items.clear()
        items.addAll(banners)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BannerViewHolder {
        val binding = HomeItemBannerBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return BannerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BannerViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class BannerViewHolder(
        private val binding: HomeItemBannerBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: HomeBannerItem) {
            val hasOverlay = item.title.isNotBlank() || item.actionText.isNotBlank()
            binding.llBannerOverlay.visibility =
                if (hasOverlay) android.view.View.VISIBLE else android.view.View.GONE
            binding.tvBannerTitle.text = item.title
            binding.btnBannerAction.text = item.actionText
            when {
                item.imageRes != null -> binding.ivBanner.setImageResource(item.imageRes)
                !item.imageUrl.isNullOrBlank() -> binding.ivBanner.load(item.imageUrl) {
                    crossfade(true)
                }
            }
        }
    }
}
