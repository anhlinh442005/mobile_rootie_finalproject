package com.veganbeauty.app.features.community.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.veganbeauty.app.R

class CommunitySortBottomSheet(
    private val currentSort: Int = 0,
    private val onSortSelected: (Int) -> Unit
) : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.com_bottom_sheet_sort, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<ImageView>(R.id.ivClose).setOnClickListener { dismiss() }

        val ivRadioCreator = view.findViewById<ImageView>(R.id.ivRadioCreator)
        val ivRadioSuggest = view.findViewById<ImageView>(R.id.ivRadioSuggest)
        val ivRadioBestSeller = view.findViewById<ImageView>(R.id.ivRadioBestSeller)
        val ivRadioPriceLow = view.findViewById<ImageView>(R.id.ivRadioPriceLow)
        val ivRadioPriceHigh = view.findViewById<ImageView>(R.id.ivRadioPriceHigh)

        val radios = listOf(ivRadioCreator, ivRadioSuggest, ivRadioBestSeller, ivRadioPriceLow, ivRadioPriceHigh)

        fun updateUI(selected: Int) {
            radios.forEachIndexed { index, imageView ->
                if (index == selected) {
                    imageView.setImageResource(R.drawable.ic_radio_primary_checked)
                } else {
                    imageView.setImageResource(R.drawable.ic_radio_primary_unchecked)
                }
            }
        }

        updateUI(currentSort)

        val layouts = listOf(
            R.id.layoutSortCreator,
            R.id.layoutSortSuggest,
            R.id.layoutSortBestSeller,
            R.id.layoutSortPriceLow,
            R.id.layoutSortPriceHigh
        )

        layouts.forEachIndexed { index, id ->
            view.findViewById<LinearLayout>(id).setOnClickListener {
                updateUI(index)
                onSortSelected(index)
                dismiss()
            }
        }
    }
}
