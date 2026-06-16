package com.veganbeauty.app.features.myskin

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.veganbeauty.app.core.base.RootieFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.veganbeauty.app.R
import com.veganbeauty.app.data.local.LocalJsonReader
import com.veganbeauty.app.databinding.SkinFragmentHistoryBinding
import org.json.JSONArray
import org.json.JSONObject

class SkinHistoryFragment : RootieFragment() {

    private var _binding: SkinFragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private lateinit var allHistory: JSONArray
    private lateinit var currentHistory: JSONArray
    private lateinit var adapter: SkinHistoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = SkinFragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun setupUI(view: View) {
        allHistory = LocalJsonReader(requireContext()).getSkinHistory()
        currentHistory = allHistory

        setupRecyclerView()
        setupListeners()
        setupFilters()
        updateChartAndList(currentHistory)
    }

    private fun setupRecyclerView() {
        adapter = SkinHistoryAdapter(currentHistory) { item ->
            // Mở dialog chi tiết
            val dialog = SkinScanResultDialogFragment.newInstance(item.toString())
            dialog.show(parentFragmentManager, "SkinScanResultDialog")
        }
        binding.skinHistoryRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.skinHistoryRecyclerView.adapter = adapter
    }

    private fun setupListeners() {
        binding.skinHistoryBtnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
        binding.skinHistoryBtnRoutine.setOnClickListener {
            Toast.makeText(requireContext(), "Gợi ý Routine clicked", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupFilters() {
        val filters = listOf(
            binding.skinHistoryFilterAll,
            binding.skinHistoryFilterAi,
            binding.skinHistoryFilterOffline
        )

        fun selectFilter(selected: TextView, type: String?) {
            // Update UI
            for (f in filters) {
                if (f == selected) {
                    f.setBackgroundResource(R.drawable.skin_bg_btn_solid_dark)
                    f.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                } else {
                    f.setBackgroundResource(R.drawable.skin_bg_card_outline)
                    f.setTextColor(ContextCompat.getColor(requireContext(), R.color.content))
                }
            }
            
            // Filter Data
            if (type == null) {
                currentHistory = allHistory
            } else {
                val filtered = JSONArray()
                for (i in 0 until allHistory.length()) {
                    val item = allHistory.getJSONObject(i)
                    if (item.optString("scanType") == type) {
                        filtered.put(item)
                    }
                }
                currentHistory = filtered
            }
            updateChartAndList(currentHistory)
        }

        binding.skinHistoryFilterAll.setOnClickListener { selectFilter(binding.skinHistoryFilterAll, null) }
        binding.skinHistoryFilterAi.setOnClickListener { selectFilter(binding.skinHistoryFilterAi, "Quét AI") }
        binding.skinHistoryFilterOffline.setOnClickListener { selectFilter(binding.skinHistoryFilterOffline, "Soi da offline") }
    }

    private fun updateChartAndList(data: JSONArray) {
        adapter.updateData(data)
        
        // Cập nhật biểu đồ. Dữ liệu trong json xếp từ mới nhất -> cũ nhất.
        // Biểu đồ vẽ từ cũ -> mới (trái -> phải).
        val entries = ArrayList<Entry>()
        val labels = ArrayList<String>()

        val len = data.length()
        for (i in 0 until len) {
            val item = data.getJSONObject(len - 1 - i) // Đảo ngược thứ tự
            val score = item.optInt("score", 0).toFloat()
            val dateStr = item.optString("date", "")
            
            val shortDate = if (dateStr.length >= 5) dateStr.substring(0, 5) else dateStr
            
            entries.add(Entry(i.toFloat(), score))
            labels.add(shortDate)
        }

        val dataSet = LineDataSet(entries, "Điểm số da")
        dataSet.color = Color.parseColor("#4B5541")
        dataSet.setCircleColor(Color.parseColor("#4B5541"))
        dataSet.lineWidth = 2f
        dataSet.circleRadius = 5f
        dataSet.setDrawCircleHole(false)
        dataSet.valueTextSize = 11f
        dataSet.valueTextColor = Color.parseColor("#333333")
        dataSet.valueTypeface = androidx.core.content.res.ResourcesCompat.getFont(requireContext(), R.font.be_vietnam_pro_bold)
        dataSet.setDrawFilled(true)
        dataSet.fillColor = Color.parseColor("#E6EBE6")
        dataSet.fillAlpha = 50
        dataSet.mode = LineDataSet.Mode.LINEAR

        // Để giá trị hiện lên cao hơn nút một chút
        dataSet.valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
            override fun getPointLabel(entry: Entry?): String {
                return entry?.y?.toInt()?.toString() ?: ""
            }
        }

        val lineData = LineData(dataSet)

        binding.skinHistoryLineChart.apply {
            this.data = lineData
            description.isEnabled = false
            legend.isEnabled = false
            setTouchEnabled(false)

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                valueFormatter = IndexAxisValueFormatter(labels)
                setDrawGridLines(true)
                gridColor = Color.parseColor("#EAEAEA")
                textColor = Color.parseColor("#555555")
                textSize = 10f
                typeface = androidx.core.content.res.ResourcesCompat.getFont(requireContext(), R.font.be_vietnam_pro_bold)
                granularity = 1f
                axisLineColor = Color.TRANSPARENT
                yOffset = 8f
            }

            axisLeft.apply {
                axisMinimum = 0f
                axisMaximum = 100f
                setLabelCount(5, true)
                textColor = Color.parseColor("#555555")
                textSize = 10f
                typeface = androidx.core.content.res.ResourcesCompat.getFont(requireContext(), R.font.be_vietnam_pro_bold)
                setDrawGridLines(true)
                gridColor = Color.parseColor("#EAEAEA")
                axisLineColor = Color.TRANSPARENT
                xOffset = 12f
            }
            
            // Xóa đường viền phải và trên
            axisRight.isEnabled = false
            setDrawBorders(false)

            // Thêm padding
            setExtraOffsets(0f, 20f, 16f, 0f)

            animateX(500)
            invalidate()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
