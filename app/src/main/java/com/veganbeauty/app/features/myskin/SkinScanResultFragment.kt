package com.veganbeauty.app.features.myskin

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.data.RadarData
import com.github.mikephil.charting.data.RadarDataSet
import com.github.mikephil.charting.data.RadarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.veganbeauty.app.R
import com.veganbeauty.app.databinding.SkinFragmentScanResultBinding
import org.json.JSONArray

class SkinScanResultFragment : Fragment() {

    companion object {
        private const val ARG_IMAGE_URI = "arg_image_uri"

        fun newInstance(imageUri: String): SkinScanResultFragment {
            val fragment = SkinScanResultFragment()
            val args = Bundle()
            args.putString(ARG_IMAGE_URI, imageUri)
            fragment.arguments = args
            return fragment
        }
    }

    private var _binding: SkinFragmentScanResultBinding? = null
    private val binding get() = _binding!!
    private var currentData: org.json.JSONObject? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = SkinFragmentScanResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupListeners()
        loadData()
    }

    private fun setupListeners() {
        binding.skinScanResultBtnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.skinResultBtnSave.setOnClickListener {
            currentData?.let {
                com.veganbeauty.app.data.local.LocalJsonReader(requireContext()).addSkinHistory(it)
                Toast.makeText(requireContext(), "Đã lưu kết quả phân tích vào Lịch sử!", Toast.LENGTH_SHORT).show()
            }
        }

        binding.skinScanBtnTopHistory.setOnClickListener {
            openHistory()
        }

        binding.skinResultBtnHistoryBottom.setOnClickListener {
            openHistory()
        }
    }

    private fun openHistory() {
        parentFragmentManager.beginTransaction()
            .setCustomAnimations(
                android.R.anim.slide_in_left,
                android.R.anim.fade_out,
                android.R.anim.fade_in,
                android.R.anim.slide_out_right
            )
            .replace(R.id.main_container, SkinHistoryFragment())
            .addToBackStack(null)
            .commit()
    }

    private fun loadData() {
        try {
            val historyArray = com.veganbeauty.app.data.local.LocalJsonReader(requireContext()).getSkinHistory()
            if (historyArray.length() > 0) {
                // Lấy data mới nhất (mock) để dùng làm kết quả phân tích
                val data = org.json.JSONObject(historyArray.getJSONObject(0).toString())
                
                // Cập nhật thời gian hiện tại cho kết quả quét mới
                val cal = java.util.Calendar.getInstance()
                val dateFormat = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale("vi"))
                val timeFormat = java.text.SimpleDateFormat("HH:mm", java.util.Locale("vi"))
                data.put("date", dateFormat.format(cal.time))
                data.put("time", timeFormat.format(cal.time))
                
                // Cập nhật id mới
                data.put("id", "sh_" + System.currentTimeMillis())
                
                val imageUri = arguments?.getString(ARG_IMAGE_URI)
                if (imageUri != null) {
                    data.put("imageUrl", imageUri)
                }

                currentData = data
                bindData(data)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Lỗi tải dữ liệu", Toast.LENGTH_SHORT).show()
        }
    }

    private fun bindData(data: org.json.JSONObject) {
        val score = data.getInt("score")
        binding.skinResultScoreVal.text = score.toString()
        binding.skinResultScoreLabel.text = data.getString("overallCondition")
        binding.skinResultSummaryText.text = data.getString("summaryText")
        
        val datetime = "${data.getString("date")} - ${data.getString("time")}"
        binding.skinResultDateTime.text = datetime

        val imageUrl = data.optString("imageUrl", "")
        if (imageUrl.isNotEmpty()) {
            try {
                if (imageUrl.startsWith("/")) {
                    binding.skinResultImage.setImageURI(android.net.Uri.fromFile(java.io.File(imageUrl)))
                } else {
                    binding.skinResultImage.setImageURI(android.net.Uri.parse(imageUrl))
                }
            } catch (e: Exception) {
                binding.skinResultImage.setImageResource(R.drawable.about_us_pd)
            }
        } else {
            binding.skinResultImage.setImageResource(R.drawable.about_us_pd)
        }

        // Progress bar
        val fillParams = binding.skinResultProgressFill.layoutParams as LinearLayout.LayoutParams
        fillParams.weight = score / 100f
        binding.skinResultProgressFill.layoutParams = fillParams
        
        val emptyParams = binding.skinResultProgressEmpty.layoutParams as LinearLayout.LayoutParams
        emptyParams.weight = 1f - (score / 100f)
        binding.skinResultProgressEmpty.layoutParams = emptyParams

        val detailedEval = data.getJSONObject("detailedEvaluation")
        setupRadarChart(detailedEval)
        populateMetrics(detailedEval)
        
        val skinCondition = data.getJSONObject("skinCondition")
        populateSkinCondition(skinCondition)

        val suggestions = data.getJSONArray("suggestions")
        populateSuggestions(suggestions)

        val routine = data.getJSONArray("routine")
        populateRoutine(routine)
    }

    private fun setupRadarChart(eval: org.json.JSONObject) {
        val labels = listOf("Độ ẩm", "Dầu", "Lỗ chân lông", "Sắc tố", "Nhạy cảm")
        val moisture = eval.getJSONObject("moisture").getInt("score").toFloat()
        val oil = eval.getJSONObject("oil").getInt("score").toFloat()
        val pores = eval.getJSONObject("pores").getInt("score").toFloat()
        val pigmentation = eval.getJSONObject("pigmentation").getInt("score").toFloat()
        val sensitivity = eval.getJSONObject("sensitivity").getInt("score").toFloat()
        
        val scores = listOf(moisture, oil, pores, pigmentation, sensitivity)

        val entries = ArrayList<RadarEntry>()
        for (score in scores) {
            entries.add(RadarEntry(score))
        }

        val dataSet = RadarDataSet(entries, "Skin Metrics")
        dataSet.color = Color.parseColor("#677559")
        dataSet.fillColor = Color.parseColor("#E6EBE6")
        dataSet.setDrawFilled(true)
        dataSet.fillAlpha = 180
        dataSet.lineWidth = 1.5f
        dataSet.isDrawHighlightCircleEnabled = true
        dataSet.setDrawHighlightIndicators(false)

        val radarData = RadarData(dataSet)
        radarData.setValueTextSize(9f)
        radarData.setValueTextColor(Color.parseColor("#333333"))

        binding.skinResultRadarChart.apply {
            data = radarData
            description.isEnabled = false
            webLineWidth = 0.5f
            webColor = Color.LTGRAY
            webLineWidthInner = 0.5f
            webColorInner = Color.LTGRAY
            webAlpha = 100

            val xAxis = xAxis
            xAxis.textSize = 10f
            xAxis.yOffset = 0f
            xAxis.xOffset = 0f
            xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            xAxis.textColor = Color.parseColor("#555555")

            val yAxis = yAxis
            yAxis.setLabelCount(5, false)
            yAxis.textSize = 9f
            yAxis.axisMinimum = 0f
            yAxis.axisMaximum = 100f
            yAxis.setDrawLabels(false)

            legend.isEnabled = false
            animateXY(1000, 1000)
            invalidate()
        }
    }

    private fun populateMetrics(eval: org.json.JSONObject) {
        val container = binding.skinResultMetricsContainer
        container.removeAllViews()

        val keys = listOf("moisture", "oil", "pores", "pigmentation", "sensitivity")
        val titles = listOf("Độ ẩm", "Lượng dầu", "Lỗ chân lông", "Sắc tố", "Độ nhạy cảm")
        val icons = listOf(R.drawable.ic_skin_moisture, R.drawable.ic_skin_moisture, R.drawable.ic_skin_pores, R.drawable.ic_skin_pigmentation, R.drawable.ic_skin_sensitivity)
        val colors = listOf("#1D82CD", "#3CA754", "#D88B2A", "#8D62A6", "#E35B5B")

        for (i in keys.indices) {
            val obj = eval.getJSONObject(keys[i])
            val view = layoutInflater.inflate(R.layout.item_skin_metric, container, false)
            
            val iconContainer = view.findViewById<android.widget.FrameLayout>(R.id.metric_icon_container)
            val icon = view.findViewById<ImageView>(R.id.metric_icon)
            val title = view.findViewById<TextView>(R.id.metric_title)
            val badge = view.findViewById<TextView>(R.id.metric_badge)
            val desc = view.findViewById<TextView>(R.id.metric_desc)

            val gd = android.graphics.drawable.GradientDrawable()
            gd.shape = android.graphics.drawable.GradientDrawable.OVAL
            gd.setColor(Color.parseColor(colors[i]))
            iconContainer.background = gd

            icon.setImageResource(icons[i])
            title.text = titles[i]
            badge.text = obj.getString("level")
            desc.text = obj.getString("description")

            container.addView(view)
        }
    }

    private fun populateSkinCondition(cond: org.json.JSONObject) {
        val container = binding.skinResultAttributesContainer
        container.removeAllViews()

        val keys = listOf("skinType", "acne", "pigmentationStatus", "wrinkles", "evenness")
        val titles = listOf("Loại da", "Mụn", "Thâm nám", "Nếp nhăn", "Độ đều màu")
        val icons = listOf(R.drawable.ic_skin_type_outline, R.drawable.ic_skin_acne_outline, R.drawable.ic_skin_spot_outline, R.drawable.ic_skin_wrinkle_outline, R.drawable.ic_skin_evenness_outline)

        for (i in keys.indices) {
            val view = layoutInflater.inflate(R.layout.item_skin_attribute, container, false)
            val icon = view.findViewById<ImageView>(R.id.attr_icon)
            val title = view.findViewById<TextView>(R.id.attr_title)
            val value = view.findViewById<TextView>(R.id.attr_value)

            icon.setImageResource(icons[i])
            title.text = titles[i]
            value.text = cond.getString(keys[i])

            container.addView(view)
            
            if (i < keys.size - 1) {
                val divider = View(requireContext())
                val params = LinearLayout.LayoutParams(1, ViewGroup.LayoutParams.MATCH_PARENT)
                params.setMargins(4, 12, 4, 12)
                divider.layoutParams = params
                divider.setBackgroundColor(Color.parseColor("#E6EBE6"))
                container.addView(divider)
            }
        }
    }

    private fun populateSuggestions(sugg: JSONArray) {
        val container = binding.skinResultSuggestionsContainer
        container.removeAllViews()

        for (i in 0 until sugg.length()) {
            val text = sugg.getString(i)
            val tv = TextView(requireContext())
            tv.text = "• $text"
            tv.textSize = 11f
            tv.setTextColor(Color.parseColor("#555555"))
            val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            params.setMargins(0, 0, 0, 8)
            tv.layoutParams = params
            try {
                tv.typeface = androidx.core.content.res.ResourcesCompat.getFont(requireContext(), R.font.be_vietnam_pro)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            container.addView(tv)
        }
    }

    private fun populateRoutine(routine: JSONArray) {
        val container = binding.skinResultRoutineContainer
        container.removeAllViews()

        val iconMap = mapOf(
            "ic_cleanser" to R.drawable.ic_routine_cleanser,
            "ic_toner" to R.drawable.ic_routine_toner,
            "ic_serum" to R.drawable.ic_routine_serum,
            "ic_moisturizer" to R.drawable.ic_routine_cream,
            "ic_sunscreen" to R.drawable.ic_routine_sunscreen,
            "ic_makeup_remover" to R.drawable.ic_routine_cleanser,
            "ic_mist" to R.drawable.ic_routine_toner,
            "ic_spot_treatment" to R.drawable.ic_routine_serum
        )

        for (i in 0 until routine.length()) {
            val item = routine.getJSONObject(i)
            val view = layoutInflater.inflate(R.layout.item_skin_routine, container, false)
            
            val icon = view.findViewById<ImageView>(R.id.routine_icon)
            val name = view.findViewById<TextView>(R.id.routine_name)
            val arrow = view.findViewById<ImageView>(R.id.routine_arrow)

            name.text = "${item.getInt("step")}. ${item.getString("name")}"
            
            val iconName = item.optString("icon", "ic_cleanser")
            val resId = iconMap[iconName] ?: R.drawable.ic_routine_cleanser
            icon.setImageResource(resId)

            if (i == routine.length() - 1) {
                arrow.visibility = View.GONE
            }

            container.addView(view)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
