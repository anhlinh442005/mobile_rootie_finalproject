package com.veganbeauty.app.features.myskin;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.DialogFragment;

import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.RadarData;
import com.github.mikephil.charting.data.RadarDataSet;
import com.github.mikephil.charting.data.RadarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.veganbeauty.app.R;
import com.veganbeauty.app.databinding.SkinDialogScanResultBinding;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class SkinScanResultDialogFragment extends DialogFragment {

    private SkinDialogScanResultBinding _binding;

    private static final String ARG_DATA = "arg_data";

    public static SkinScanResultDialogFragment newInstance(String dataString) {
        SkinScanResultDialogFragment fragment = new SkinScanResultDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_DATA, dataString);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NORMAL, android.R.style.Theme_DeviceDefault_Light_Dialog_MinWidth);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        _binding = SkinDialogScanResultBinding.inflate(inflater, container, false);
        return _binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String dataString = getArguments() != null ? getArguments().getString(ARG_DATA) : null;
        if (dataString != null) {
            try {
                JSONObject data = new JSONObject(dataString);
                bindData(data);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        _binding.dialogSkinBtnClose.setOnClickListener(v -> dismiss());
    }

    private void bindData(JSONObject data) throws Exception {
        int score = data.getInt("score");
        _binding.dialogSkinScoreVal.setText(String.valueOf(score));
        _binding.dialogSkinScoreLabel.setText(data.getString("overallCondition"));
        _binding.dialogSkinSummaryText.setText(data.getString("summaryText"));
        _binding.dialogSkinDate.setText(data.getString("date"));
        _binding.dialogSkinTime.setText(data.getString("time"));
        _binding.dialogSkinType.setText(data.optString("scanType", "Quét AI"));

        String imageUrl = data.optString("imageUrl", "");
        if (!imageUrl.isEmpty()) {
            try {
                if (imageUrl.startsWith("/")) {
                    _binding.dialogSkinImage.setImageURI(android.net.Uri.fromFile(new java.io.File(imageUrl)));
                } else {
                    _binding.dialogSkinImage.setImageURI(android.net.Uri.parse(imageUrl));
                }
            } catch (Exception e) {
                _binding.dialogSkinImage.setImageResource(R.drawable.about_us_pd);
            }
        } else {
            _binding.dialogSkinImage.setImageResource(R.drawable.about_us_pd);
        }

        // Progress bar
        LinearLayout.LayoutParams fillParams = (LinearLayout.LayoutParams) _binding.dialogSkinProgressFill.getLayoutParams();
        fillParams.weight = score / 100f;
        _binding.dialogSkinProgressFill.setLayoutParams(fillParams);

        LinearLayout.LayoutParams emptyParams = (LinearLayout.LayoutParams) _binding.dialogSkinProgressEmpty.getLayoutParams();
        emptyParams.weight = 1f - (score / 100f);
        _binding.dialogSkinProgressEmpty.setLayoutParams(emptyParams);

        JSONObject detailedEval = data.getJSONObject("detailedEvaluation");
        setupRadarChart(detailedEval);
        populateMetrics(detailedEval);

        JSONArray suggestions = data.getJSONArray("suggestions");
        populateSuggestions(suggestions);
    }

    private void setupRadarChart(JSONObject eval) throws Exception {
        List<String> labels = new ArrayList<>();
        labels.add("Độ ẩm");
        labels.add("Dầu");
        labels.add("Lỗ chân lông");
        labels.add("Sắc tố");
        labels.add("Nhạy cảm");

        float moisture = eval.getJSONObject("moisture").getInt("score");
        float oil = eval.getJSONObject("oil").getInt("score");
        float pores = eval.getJSONObject("pores").getInt("score");
        float pigmentation = eval.getJSONObject("pigmentation").getInt("score");
        float sensitivity = eval.getJSONObject("sensitivity").getInt("score");

        List<Float> scores = new ArrayList<>();
        scores.add(moisture);
        scores.add(oil);
        scores.add(pores);
        scores.add(pigmentation);
        scores.add(sensitivity);

        ArrayList<RadarEntry> entries = new ArrayList<>();
        for (Float score : scores) {
            entries.add(new RadarEntry(score));
        }

        RadarDataSet dataSet = new RadarDataSet(entries, "Skin Metrics");
        dataSet.setColor(Color.parseColor("#677559"));
        dataSet.setFillColor(Color.parseColor("#E6EBE6"));
        dataSet.setDrawFilled(true);
        dataSet.setFillAlpha(180);
        dataSet.setLineWidth(1.5f);
        dataSet.setDrawHighlightCircleEnabled(true);
        dataSet.setDrawHighlightIndicators(false);

        RadarData radarData = new RadarData(dataSet);
        radarData.setValueTextSize(0f); // Hide values
        radarData.setDrawValues(false);

        _binding.dialogSkinRadarChart.setData(radarData);
        _binding.dialogSkinRadarChart.getDescription().setEnabled(false);
        _binding.dialogSkinRadarChart.setWebLineWidth(0.5f);
        _binding.dialogSkinRadarChart.setWebColor(Color.LTGRAY);
        _binding.dialogSkinRadarChart.setWebLineWidthInner(0.5f);
        _binding.dialogSkinRadarChart.setWebColorInner(Color.LTGRAY);
        _binding.dialogSkinRadarChart.setWebAlpha(100);

        XAxis xAxis = _binding.dialogSkinRadarChart.getXAxis();
        xAxis.setTextSize(8f);
        xAxis.setYOffset(0f);
        xAxis.setXOffset(0f);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setTextColor(Color.parseColor("#555555"));

        YAxis yAxis = _binding.dialogSkinRadarChart.getYAxis();
        yAxis.setLabelCount(5, false);
        yAxis.setAxisMinimum(0f);
        yAxis.setAxisMaximum(100f);
        yAxis.setDrawLabels(false);

        _binding.dialogSkinRadarChart.getLegend().setEnabled(false);
        _binding.dialogSkinRadarChart.invalidate();
    }

    private void populateMetrics(JSONObject eval) throws Exception {
        LinearLayout container = _binding.dialogSkinMetricsContainer;
        container.removeAllViews();

        String[] keys = {"moisture", "oil", "pores", "pigmentation", "sensitivity"};
        String[] titles = {"Độ ẩm", "Lượng dầu", "Lỗ chân lông", "Sắc tố", "Độ nhạy cảm"};
        String[] colors = {"#1D82CD", "#3CA754", "#D88B2A", "#8D62A6", "#E35B5B"};

        LayoutInflater inflater = getLayoutInflater();

        for (int i = 0; i < keys.length; i++) {
            JSONObject obj = eval.getJSONObject(keys[i]);
            View view = inflater.inflate(R.layout.item_skin_metric, container, false);

            View dot = view.findViewById(R.id.metric_dot);
            TextView title = view.findViewById(R.id.metric_title);
            TextView badge = view.findViewById(R.id.metric_badge);
            TextView desc = view.findViewById(R.id.metric_desc);

            android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
            gd.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            gd.setColor(Color.parseColor(colors[i]));
            dot.setBackground(gd);

            title.setText(titles[i]);

            title.setTextSize(10f);
            desc.setTextSize(9f);
            badge.setTextSize(8f);
            badge.setPadding(8, 2, 8, 2);

            badge.setText(obj.getString("level"));
            desc.setText(obj.getString("description"));

            container.addView(view);
        }
    }

    private void populateSuggestions(JSONArray sugg) throws Exception {
        LinearLayout container = _binding.dialogSkinSuggestionsContainer;
        container.removeAllViews();

        for (int i = 0; i < sugg.length(); i++) {
            String text = sugg.getString(i);
            TextView tv = new TextView(requireContext());
            tv.setText("• " + text);
            tv.setTextSize(9f);
            tv.setTextColor(Color.parseColor("#555555"));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 0, 6);
            tv.setLayoutParams(params);
            try {
                tv.setTypeface(ResourcesCompat.getFont(requireContext(), R.font.be_vietnam_pro));
            } catch (Exception e) {
                e.printStackTrace();
            }
            container.addView(tv);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        _binding = null;
    }
}
