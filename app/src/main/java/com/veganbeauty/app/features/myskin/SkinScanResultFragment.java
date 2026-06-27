package com.veganbeauty.app.features.myskin;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

import com.github.mikephil.charting.data.RadarData;
import com.github.mikephil.charting.data.RadarDataSet;
import com.github.mikephil.charting.data.RadarEntry;
import com.veganbeauty.app.R;
import com.veganbeauty.app.core.base.RootieFragment;
import com.veganbeauty.app.data.local.LocalJsonReader;
import com.veganbeauty.app.data.local.ProfileSession;
import com.veganbeauty.app.data.remote.FirestoreService;
import com.veganbeauty.app.databinding.SkinFragmentScanResultBinding;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SkinScanResultFragment extends RootieFragment {

    private static final String ARG_IMAGE_URI = "arg_image_uri";
    private SkinFragmentScanResultBinding binding;
    private JSONObject currentData;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public static SkinScanResultFragment newInstance(String imageUri) {
        SkinScanResultFragment f = new SkinScanResultFragment();
        Bundle args = new Bundle();
        args.putString(ARG_IMAGE_URI, imageUri);
        f.setArguments(args);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = SkinFragmentScanResultBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void setupUI(View view) {
        binding.skinScanResultBtnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        binding.skinResultBtnSave.setOnClickListener(v -> {
            if (currentData != null) {
                String userEmail = ProfileSession.getEmail(requireContext());
                if (userEmail.isBlank()) userEmail = "test@example.com";
                final String email = userEmail;
                final JSONObject data = currentData;
                executor.execute(() -> {
                    try {
                        new FirestoreService().addSkinHistory(email, data);
                        if (isAdded()) requireActivity().runOnUiThread(() ->
                                Toast.makeText(requireContext(), "Đã lưu kết quả phân tích vào Lịch sử!", Toast.LENGTH_SHORT).show());
                    } catch (Exception e) { e.printStackTrace(); }
                });
            }
        });
        binding.skinScanBtnTopHistory.setOnClickListener(v -> openHistory());
        binding.skinResultBtnHistoryBottom.setOnClickListener(v -> openHistory());
        loadData();
    }

    private void openHistory() {
        getParentFragmentManager().beginTransaction()
                .setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.slide_out_right)
                .replace(R.id.main_container, new SkinHistoryFragment())
                .addToBackStack(null).commit();
    }

    private void loadData() {
        executor.execute(() -> {
            try {
                String userEmail = ProfileSession.getEmail(requireContext());
                if (userEmail.isBlank()) userEmail = "test@example.com";
                JSONArray historyArray = new FirestoreService().getSkinHistory(userEmail);
                JSONArray arrayToUse = historyArray.length() > 0 ? historyArray
                        : new LocalJsonReader(requireContext()).getSkinHistory();

                if (arrayToUse.length() > 0) {
                    JSONObject data = new JSONObject(arrayToUse.getJSONObject(0).toString());
                    Calendar cal = Calendar.getInstance();
                    data.put("date", new SimpleDateFormat("dd/MM/yyyy", new Locale("vi")).format(cal.getTime()));
                    data.put("time", new SimpleDateFormat("HH:mm", new Locale("vi")).format(cal.getTime()));
                    data.put("id", "sh_" + System.currentTimeMillis());
                    Bundle args = getArguments();
                    if (args != null) {
                        String imageUri = args.getString(ARG_IMAGE_URI);
                        if (imageUri != null) data.put("imageUrl", imageUri);
                    }
                    currentData = data;
                    JSONObject finalData = data;
                    if (isAdded()) requireActivity().runOnUiThread(() -> bindData(finalData));
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (isAdded()) requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "Lỗi tải dữ liệu", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void bindData(JSONObject data) {
        try {
            int score = data.getInt("score");
            binding.skinResultScoreVal.setText(String.valueOf(score));
            binding.skinResultScoreLabel.setText(data.getString("overallCondition"));
            binding.skinResultSummaryText.setText(data.getString("summaryText"));
            binding.skinResultDateTime.setText(data.getString("date") + " - " + data.getString("time"));

            String imageUrl = data.optString("imageUrl", "");
            if (!imageUrl.isEmpty()) {
                try {
                    if (imageUrl.startsWith("/")) binding.skinResultImage.setImageURI(Uri.fromFile(new File(imageUrl)));
                    else binding.skinResultImage.setImageURI(Uri.parse(imageUrl));
                } catch (Exception e) { binding.skinResultImage.setImageResource(R.drawable.about_us_pd); }
            } else {
                binding.skinResultImage.setImageResource(R.drawable.about_us_pd);
            }

            LinearLayout.LayoutParams fill = (LinearLayout.LayoutParams) binding.skinResultProgressFill.getLayoutParams();
            fill.weight = score / 100f;
            binding.skinResultProgressFill.setLayoutParams(fill);
            LinearLayout.LayoutParams empty = (LinearLayout.LayoutParams) binding.skinResultProgressEmpty.getLayoutParams();
            empty.weight = 1f - (score / 100f);
            binding.skinResultProgressEmpty.setLayoutParams(empty);

            JSONObject detailedEval = data.getJSONObject("detailedEvaluation");
            setupRadarChart(detailedEval);
            populateMetrics(detailedEval);
            populateSkinCondition(data.getJSONObject("skinCondition"));
            populateSuggestions(data.getJSONArray("suggestions"));
            populateRoutine(data.getJSONArray("routine"));
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void setupRadarChart(JSONObject eval) {
        try {
            float moisture = (float) eval.getJSONObject("moisture").getInt("score");
            float oil = (float) eval.getJSONObject("oil").getInt("score");
            float pores = (float) eval.getJSONObject("pores").getInt("score");
            float pigmentation = (float) eval.getJSONObject("pigmentation").getInt("score");
            float sensitivity = (float) eval.getJSONObject("sensitivity").getInt("score");

            ArrayList<RadarEntry> entries = new ArrayList<>();
            for (float s : new float[]{moisture, oil, pores, pigmentation, sensitivity})
                entries.add(new RadarEntry(s));

            RadarDataSet dataSet = new RadarDataSet(entries, "Skin Metrics");
            dataSet.setColor(Color.parseColor("#677559"));
            dataSet.setFillColor(Color.parseColor("#E6EBE6"));
            dataSet.setDrawFilled(true);
            dataSet.setFillAlpha(180);
            dataSet.setLineWidth(1.5f);
            dataSet.setDrawHighlightCircleEnabled(true);
            dataSet.setDrawHighlightIndicators(false);

            RadarData radarData = new RadarData(dataSet);
            radarData.setDrawValues(false);

            binding.skinResultRadarChart.setData(radarData);
            binding.skinResultRadarChart.getDescription().setEnabled(false);
            binding.skinResultRadarChart.setWebLineWidth(1f);
            binding.skinResultRadarChart.setWebColor(Color.parseColor("#E6EBE6"));
            binding.skinResultRadarChart.setWebLineWidthInner(1f);
            binding.skinResultRadarChart.setWebColorInner(Color.parseColor("#E6EBE6"));
            binding.skinResultRadarChart.setWebAlpha(255);
            binding.skinResultRadarChart.getXAxis().setDrawLabels(false);
            binding.skinResultRadarChart.getYAxis().setLabelCount(5, false);
            binding.skinResultRadarChart.getYAxis().setAxisMinimum(0f);
            binding.skinResultRadarChart.getYAxis().setAxisMaximum(100f);
            binding.skinResultRadarChart.getYAxis().setDrawLabels(false);
            binding.skinResultRadarChart.getLegend().setEnabled(false);
            binding.skinResultRadarChart.animateXY(1000, 1000);
            binding.skinResultRadarChart.invalidate();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void populateMetrics(JSONObject eval) {
        try {
            binding.skinResultMetricsContainer.removeAllViews();
            String[] keys = {"moisture","oil","pores","pigmentation","sensitivity"};
            String[] titles = {"Độ ẩm","Lượng dầu","Lỗ chân lông","Sắc tố","Độ nhạy cảm"};
            int[] icons = {R.drawable.ic_skin_moisture,R.drawable.ic_skin_moisture,R.drawable.ic_skin_pores,R.drawable.ic_skin_pigmentation,R.drawable.ic_skin_sensitivity};
            String[] colors = {"#1D82CD","#3CA754","#D88B2A","#8D62A6","#E35B5B"};
            for (int i = 0; i < keys.length; i++) {
                JSONObject obj = eval.getJSONObject(keys[i]);
                View v = getLayoutInflater().inflate(R.layout.item_skin_metric, binding.skinResultMetricsContainer, false);
                FrameLayout iconContainer = v.findViewById(R.id.metric_icon_container);
                GradientDrawable gd = new GradientDrawable();
                gd.setShape(GradientDrawable.OVAL);
                gd.setColor(Color.parseColor(colors[i]));
                iconContainer.setBackground(gd);
                ((ImageView) v.findViewById(R.id.metric_icon)).setImageResource(icons[i]);
                ((TextView) v.findViewById(R.id.metric_title)).setText(titles[i]);
                ((TextView) v.findViewById(R.id.metric_badge)).setText(obj.getString("level"));
                ((TextView) v.findViewById(R.id.metric_desc)).setText(obj.getString("description"));
                binding.skinResultMetricsContainer.addView(v);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void populateSkinCondition(JSONObject cond) {
        try {
            binding.skinResultAttributesContainer.removeAllViews();
            String[] keys = {"skinType","acne","pigmentationStatus","wrinkles","evenness"};
            String[] titles = {"Loại da","Mụn","Thâm nám","Nếp nhăn","Độ đều màu"};
            int[] icons = {R.drawable.ic_skin_type_outline,R.drawable.ic_skin_acne_outline,R.drawable.ic_skin_spot_outline,R.drawable.ic_skin_wrinkle_outline,R.drawable.ic_skin_evenness_outline};
            for (int i = 0; i < keys.length; i++) {
                View v = getLayoutInflater().inflate(R.layout.item_skin_attribute, binding.skinResultAttributesContainer, false);
                ((ImageView) v.findViewById(R.id.attr_icon)).setImageResource(icons[i]);
                ((TextView) v.findViewById(R.id.attr_title)).setText(titles[i]);
                ((TextView) v.findViewById(R.id.attr_value)).setText(cond.getString(keys[i]));
                binding.skinResultAttributesContainer.addView(v);
                if (i < keys.length - 1) {
                    View div = new View(requireContext());
                    LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(1, ViewGroup.LayoutParams.MATCH_PARENT);
                    p.setMargins(4, 12, 4, 12);
                    div.setLayoutParams(p);
                    div.setBackgroundColor(Color.parseColor("#E6EBE6"));
                    binding.skinResultAttributesContainer.addView(div);
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void populateSuggestions(JSONArray sugg) {
        try {
            binding.skinResultSuggestionsContainer.removeAllViews();
            for (int i = 0; i < sugg.length(); i++) {
                TextView tv = new TextView(requireContext());
                tv.setText("• " + sugg.getString(i));
                tv.setTextSize(11f);
                tv.setTextColor(Color.parseColor("#555555"));
                LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                p.setMargins(0, 0, 0, 8);
                tv.setLayoutParams(p);
                try { tv.setTypeface(ResourcesCompat.getFont(requireContext(), R.font.be_vietnam_pro)); } catch (Exception ignored) {}
                binding.skinResultSuggestionsContainer.addView(tv);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void populateRoutine(JSONArray routine) {
        try {
            binding.skinResultRoutineContainer.removeAllViews();
            Map<String, Integer> iconMap = new HashMap<>();
            iconMap.put("ic_cleanser", R.drawable.ic_routine_cleanser);
            iconMap.put("ic_toner", R.drawable.ic_routine_toner);
            iconMap.put("ic_serum", R.drawable.ic_routine_serum);
            iconMap.put("ic_moisturizer", R.drawable.ic_routine_cream);
            iconMap.put("ic_sunscreen", R.drawable.ic_routine_sunscreen);
            iconMap.put("ic_makeup_remover", R.drawable.ic_routine_cleanser);
            iconMap.put("ic_mist", R.drawable.ic_routine_toner);
            iconMap.put("ic_spot_treatment", R.drawable.ic_routine_serum);
            for (int i = 0; i < routine.length(); i++) {
                JSONObject item = routine.getJSONObject(i);
                View v = getLayoutInflater().inflate(R.layout.item_skin_routine, binding.skinResultRoutineContainer, false);
                ((TextView) v.findViewById(R.id.routine_name)).setText(item.getInt("step") + ". " + item.getString("name"));
                String iconName = item.optString("icon", "ic_cleanser");
                int resId = iconMap.containsKey(iconName) ? iconMap.get(iconName) : R.drawable.ic_routine_cleanser;
                ((ImageView) v.findViewById(R.id.routine_icon)).setImageResource(resId);
                if (i == routine.length() - 1) v.findViewById(R.id.routine_arrow).setVisibility(View.GONE);
                binding.skinResultRoutineContainer.addView(v);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    @Override
    public void observeViewModel() {}

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        executor.shutdown();
    }
}
