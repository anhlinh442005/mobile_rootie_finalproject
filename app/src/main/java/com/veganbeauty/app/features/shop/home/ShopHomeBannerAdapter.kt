package com.veganbeauty.app.features.shop.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.veganbeauty.app.databinding.ShopHomeBannerBinding
import com.veganbeauty.app.features.shop.home.models.BannerUiModel

class ShopHomeBannerAdapter : ListAdapter<BannerUiModel, ShopHomeBannerAdapter.BannerViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BannerViewHolder {
        val binding = ShopHomeBannerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BannerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BannerViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class BannerViewHolder(private val binding: ShopHomeBannerBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(banner: BannerUiModel) {
            // Tạm thời hiển thị màu nền hoặc ảnh mặc định
            // Sau này bạn có thể dùng Coil/Glide: binding.ivBanner.load(banner.imageUrl)
            binding.ivBanner.setImageResource(banner.imageRes)
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<BannerUiModel>() {
        override fun areItemsTheSame(oldItem: BannerUiModel, newItem: BannerUiModel) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: BannerUiModel, newItem: BannerUiModel) = oldItem == newItem
    }
}
