package com.veganbeauty.app.features.myskin

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.github.mikephil.charting.data.RadarData
import com.github.mikephil.charting.data.RadarDataSet
import com.github.mikephil.charting.data.RadarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.veganbeauty.app.R
import com.veganbeauty.app.databinding.SkinDialogScanResultBinding
import org.json.JSONArray
import org.json.JSONObject

class SkinScanResultDialogFragment : DialogFragment() {

    private var _binding: SkinDialogScanResultBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val ARG_DATA = "arg_data"

        fun newInstance(dataString: String): SkinScanResultDialogFragment {
            val fragment = SkinScanResultDialogFragment()
            val args = Bundle()
            args.putString(ARG_DATA, dataString)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, android.R.style.Theme_DeviceDefault_Light_Dialog_MinWidth)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = SkinDialogScanResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val dataString = arguments?.getString(ARG_DATA)
        if (dataString != null) {
            val data = JSONObject(dataString)
            bindData(data)
        }

        binding.dialogSkinBtnClose.setOnClickListener {
            dismiss()
        }
    }

    private fun bindData(data: JSONObject) {
        val score = data.getInt("score")
        binding.dialogSkinScoreVal.text = score.toString()
        binding.dialogSkinScoreLabel.text = data.getString("overallCondition")
        binding.dialogSkinSummaryText.text = data.getString("summaryText")
        binding.dialogSkinDate.text = data.getString("date")
        binding.dialogSkinTime.text = data.getString("time")
        binding.dialogSkinType.text = data.optString("scanType", "Quét AI")

        val imageUrl = data.optString("imageUrl", "")
        if (imageUrl.isNotEmpty()) {
            try {
                if (imageUrl.startsWith("/")) {
                    binding.dialogSkinImage.setImageURI(android.net.Uri.fromFile(java.io.File(imageUrl)))
                } else {
                    binding.dialogSkinImage.setImageURI(android.net.Uri.parse(imageUrl))
                }
            } catch (e: Exception) {
                binding.dialogSkinImage.setImageResource(R.drawable.about_us_pd)
            }
        } else {
            binding.dialogSkinImage.setImageResource(R.drawable.about_us_pd)
        }

        // Progress bar
        val fillParams = binding.dialogSkinProgressFill.layoutParams as LinearLayout.LayoutParams
        fillParams.weight = score / 100f
        binding.dialogSkinProgressFill.layoutParams = fillParams
        
        val emptyParams = binding.dialogSkinProgressEmpty.layoutParams as LinearLayout.LayoutParams
        emptyParams.weight = 1f - (score / 100f)
        binding.dialogSkinProgressEmpty.layoutParams = emptyParams

        val detailedEval = data.getJSONObject("detailedEvaluation")
        setupRadarChart(detailedEval)
        populateMetrics(detailedEval)

        val suggestions = data.getJSONArray("suggestions")
        populateSuggestions(suggestions)
    }

    private fun setupRadarChart(eval: JSONObject) {
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
        radarData.setValueTextSize(0f) // Hide values
        radarData.setDrawValues(false)

        binding.dialogSkinRadarChart.apply {
            data = radarData
            description.isEnabled = false
            webLineWidth = 0.5f
            webColor = Color.LTGRAY
            webLineWidthInner = 0.5f
            webColorInner = Color.LTGRAY
            webAlpha = 100

            val xAxis = xAxis
            xAxis.textSize = 8f
            xAxis.yOffset = 0f
            xAxis.xOffset = 0f
            xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            xAxis.textColor = Color.parseColor("#555555")

            val yAxis = yAxis
            yAxis.setLabelCount(5, false)
            yAxis.axisMinimum = 0f
            yAxis.axisMaximum = 100f
            yAxis.setDrawLabels(false)

            legend.isEnabled = false
            invalidate()
        }
    }

    private fun populateMetrics(eval: JSONObject) {
        val container = binding.dialogSkinMetricsContainer
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
            
            title.textSize = 10f
            desc.textSize = 9f
            badge.textSize = 8f
            badge.setPadding(8, 2, 8, 2)
            
            badge.text = obj.getString("level")
            desc.text = obj.getString("description")

            container.addView(view)
        }
    }

    private fun populateSuggestions(sugg: JSONArray) {
        val container = binding.dialogSkinSuggestionsContainer
        container.removeAllViews()

        for (i in 0 until sugg.length()) {
            val text = sugg.getString(i)
            val tv = TextView(requireContext())
            tv.text = "• $text"
            tv.textSize = 9f
            tv.setTextColor(Color.parseColor("#555555"))
            val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            params.setMargins(0, 0, 0, 6)
            tv.layoutParams = params
            try {
                tv.typeface = androidx.core.content.res.ResourcesCompat.getFont(requireContext(), R.font.be_vietnam_pro)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            container.addView(tv)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
