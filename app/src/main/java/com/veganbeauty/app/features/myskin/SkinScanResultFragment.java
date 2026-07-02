package com.veganbeauty.app.features.myskin;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
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

import com.github.mikephil.charting.charts.RadarChart;
import com.github.mikephil.charting.data.RadarData;
import com.github.mikephil.charting.data.RadarDataSet;
import com.github.mikephil.charting.data.RadarEntry;
import com.google.firebase.firestore.FirebaseFirestore;
import com.veganbeauty.app.BuildConfig;
import com.veganbeauty.app.R;
import com.veganbeauty.app.core.base.RootieFragment;
import com.veganbeauty.app.data.local.LocalJsonReader;
import com.veganbeauty.app.data.local.ProfileSession;
import com.veganbeauty.app.databinding.SkinFragmentScanResultBinding;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SkinScanResultFragment extends RootieFragment {

    private static final String ARG_IMAGE_URI = "arg_image_uri";
    private SkinFragmentScanResultBinding binding;
    private JSONObject currentData = null;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    
    // API key for Google Gemini (Free tier). Replace with your own key in local.properties.
    private final String GEMINI_API_KEY = BuildConfig.GEMINI_API_KEY;

    public static SkinScanResultFragment newInstance(String imageUri) {
        SkinScanResultFragment fragment = new SkinScanResultFragment();
        Bundle args = new Bundle();
        args.putString(ARG_IMAGE_URI, imageUri);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = SkinFragmentScanResultBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void setupUI(@NonNull View view) {
        setupListeners();
        loadData();
    }

    private void setupListeners() {
        binding.skinScanResultBtnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        binding.skinResultBtnSave.setOnClickListener(v -> {
            if (currentData != null) {
                String userId = ProfileSession.getUserId(requireContext());
                saveToFirestore(userId, currentData);
            }
        });

        binding.skinScanBtnTopHistory.setOnClickListener(v -> openHistory());
        binding.skinResultBtnHistoryBottom.setOnClickListener(v -> openHistory());

        binding.skinResultBtnGoRoutine.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .setCustomAnimations(
                            android.R.anim.slide_in_left,
                            android.R.anim.fade_out,
                            android.R.anim.fade_in,
                            android.R.anim.slide_out_right
                    )
                    .replace(R.id.main_container, new MySkinFragment())
                    .addToBackStack(null)
                    .commit();
        });
    }

    private void openHistory() {
        getParentFragmentManager().beginTransaction()
                .setCustomAnimations(
                        android.R.anim.slide_in_left,
                        android.R.anim.fade_out,
                        android.R.anim.fade_in,
                        android.R.anim.slide_out_right
                )
                .replace(R.id.main_container, new SkinHistoryFragment())
                .addToBackStack(null)
                .commit();
    }

    private void saveToFirestore(String userId, JSONObject historyObj) {
        try {
            String id = historyObj.optString("id", UUID.randomUUID().toString());
            historyObj.put("userId", userId);
            
            // Chuyển đổi JSONObject thành Map
            Map<String, Object> map = new HashMap<>();
            java.util.Iterator<String> keys = historyObj.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                map.put(key, historyObj.get(key)); // Note: This might need deep conversion if Firebase doesn't accept nested JSONObjects
            }

            FirebaseFirestore.getInstance().collection("skin_history").document(id)
                    .set(toMapDeep(historyObj))
                    .addOnSuccessListener(aVoid -> 
                        Toast.makeText(requireContext(), "Đã lưu kết quả phân tích vào Lịch sử!", Toast.LENGTH_SHORT).show()
                    )
                    .addOnFailureListener(e -> 
                        Toast.makeText(requireContext(), "Lỗi khi lưu: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Map<String, Object> toMapDeep(JSONObject json) throws org.json.JSONException {
        Map<String, Object> map = new HashMap<>();
        java.util.Iterator<String> keys = json.keys();
        while(keys.hasNext()) {
            String key = keys.next();
            Object value = json.get(key);
            if(value instanceof JSONObject) {
                value = toMapDeep((JSONObject) value);
            } else if(value instanceof JSONArray) {
                value = toListDeep((JSONArray) value);
            }
            map.put(key, value);
        }
        return map;
    }

    private List<Object> toListDeep(JSONArray array) throws org.json.JSONException {
        List<Object> list = new ArrayList<>();
        for(int i = 0; i < array.length(); i++) {
            Object value = array.get(i);
            if(value instanceof JSONObject) {
                value = toMapDeep((JSONObject) value);
            } else if(value instanceof JSONArray) {
                value = toListDeep((JSONArray) value);
            }
            list.add(value);
        }
        return list;
    }

    private void loadData() {
        String imageUri = getArguments() != null ? getArguments().getString(ARG_IMAGE_URI) : null;
        if (imageUri != null && !imageUri.isEmpty()) {
            // New scan: Analyze image with Gemini AI
            analyzeImageWithGemini(imageUri);
        } else {
            // No image provided, maybe history view -> Fallback to mock/history
            useFallbackMockData(null);
        }
    }

    private void analyzeImageWithGemini(String imageUri) {
        showLoadingState(true);

        executorService.execute(() -> {
            boolean success = false;
            try {
                // 1. Process and compress image to Base64
                String base64Image = getBase64FromUri(imageUri);
                if (base64Image == null || base64Image.isEmpty() || GEMINI_API_KEY.isEmpty() || GEMINI_API_KEY.equals("YOUR_GEMINI_API_KEY_HERE")) {
                    throw new Exception("Invalid image or missing API Key");
                }

                // 2. Prepare JSON payload for Gemini 1.5 Flash Vision
                JSONObject requestBody = buildGeminiRequestPayload(base64Image);

                // 3. Make HTTP POST Request
                URL url = new URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + GEMINI_API_KEY);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                
                try(OutputStream os = conn.getOutputStream()) {
                    byte[] input = requestBody.toString().getBytes("utf-8");
                    os.write(input, 0, input.length);           
                }

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    InputStream is = conn.getInputStream();
                    java.util.Scanner scanner = new java.util.Scanner(is, "UTF-8").useDelimiter("\\A");
                    String responseStr = scanner.hasNext() ? scanner.next() : "";
                    
                    // Parse response
                    JSONObject responseJson = new JSONObject(responseStr);
                    JSONArray candidates = responseJson.optJSONArray("candidates");
                    if (candidates != null && candidates.length() > 0) {
                        JSONObject content = candidates.getJSONObject(0).getJSONObject("content");
                        JSONArray parts = content.getJSONArray("parts");
                        String textResponse = parts.getJSONObject(0).getString("text");
                        
                        // Clean markdown if Gemini returned code blocks
                        String cleanJsonStr = textResponse.replaceAll("(?s)```json(.*?)```", "$1")
                                                          .replaceAll("(?s)```(.*?)```", "$1")
                                                          .trim();
                        
                        JSONObject resultJson = new JSONObject(cleanJsonStr);
                        
                        // Add meta data
                        Calendar cal = Calendar.getInstance();
                        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", new Locale("vi"));
                        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", new Locale("vi"));
                        resultJson.put("date", dateFormat.format(cal.getTime()));
                        resultJson.put("time", timeFormat.format(cal.getTime()));
                        resultJson.put("id", "sh_" + System.currentTimeMillis());
                        resultJson.put("imageUrl", imageUri);

                        success = true;
                        mainHandler.post(() -> {
                            showLoadingState(false);
                            currentData = resultJson;
                            bindData(resultJson);
                            Toast.makeText(requireContext(), "Phân tích AI thành công!", Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (!success) {
                mainHandler.post(() -> {
                    showLoadingState(false);
                    useFallbackMockData(imageUri);
                });
            }
        });
    }

    private JSONObject buildGeminiRequestPayload(String base64Image) throws Exception {
        JSONObject payload = new JSONObject();
        JSONArray contents = new JSONArray();
        JSONObject content = new JSONObject();
        JSONArray parts = new JSONArray();

        // System Instruction/Prompt Text
        String prompt = "Bạn là bác sĩ da liễu. Hãy phân tích hình ảnh khuôn mặt này và trả về kết quả dưới định dạng JSON nguyên chất (không có markdown code block như ```json). Cấu trúc bắt buộc:\n" +
                "{\n" +
                "  \"score\": 78,\n" +
                "  \"overallCondition\": \"Da tốt\",\n" +
                "  \"summaryText\": \"Làn da của bạn đang ở trạng thái tốt. Hãy duy trì chăm sóc da đều đặn để đạt kết quả tốt hơn!\",\n" +
                "  \"detailedEvaluation\": {\n" +
                "    \"moisture\": {\"score\": 72, \"level\": \"Trung bình\", \"description\": \"Da bạn cần bổ sung độ ẩm...\"},\n" +
                "    \"oil\": {\"score\": 81, \"level\": \"Tốt\", \"description\": \"Tuyến dầu hoạt động ổn định...\"},\n" +
                "    \"pores\": {\"score\": 85, \"level\": \"Khá\", \"description\": \"Lỗ chân lông hơi to ở vùng mũi...\"},\n" +
                "    \"pigmentation\": {\"score\": 70, \"level\": \"Trung bình\", \"description\": \"Có dấu hiệu thâm nhẹ...\"},\n" +
                "    \"sensitivity\": {\"score\": 75, \"level\": \"Khá\", \"description\": \"Da đôi lúc nhạy cảm...\"}\n" +
                "  },\n" +
                "  \"skinCondition\": {\n" +
                "    \"skinType\": \"Da hỗn hợp thiên dầu\",\n" +
                "    \"acne\": \"Ít mụn ẩn\",\n" +
                "    \"pigmentationStatus\": \"Nhẹ\",\n" +
                "    \"wrinkles\": \"Ít dấu hiệu\",\n" +
                "    \"evenness\": \"Trung bình\"\n" +
                "  },\n" +
                "  \"suggestions\": [\n" +
                "    \"Duy trì cấp ẩm và khóa ẩm mỗi ngày.\",\n" +
                "    \"Tẩy tế bào chết 1-2 lần/tuần để thông thoáng lỗ chân lông.\",\n" +
                "    \"Sử dụng kem chống nắng SPF 30+ mỗi ngày.\",\n" +
                "    \"Ưu tiên sản phẩm dịu nhẹ, không chứa cồn và hương liệu.\"\n" +
                "  ],\n" +
                "  \"routine\": [\n" +
                "    {\"step\": 1, \"name\": \"Sữa rửa mặt\", \"icon\": \"ic_cleanser\"},\n" +
                "    {\"step\": 2, \"name\": \"Toner\", \"icon\": \"ic_toner\"},\n" +
                "    {\"step\": 3, \"name\": \"Serum\", \"icon\": \"ic_serum\"},\n" +
                "    {\"step\": 4, \"name\": \"Kem dưỡng\", \"icon\": \"ic_moisturizer\"},\n" +
                "    {\"step\": 5, \"name\": \"Kem chống nắng\", \"icon\": \"ic_sunscreen\"}\n" +
                "  ]\n" +
                "}";

        JSONObject textPart = new JSONObject();
        textPart.put("text", prompt);

        // Image data
        JSONObject inlineDataPart = new JSONObject();
        JSONObject inlineData = new JSONObject();
        inlineData.put("mime_type", "image/jpeg");
        inlineData.put("data", base64Image);
        inlineDataPart.put("inline_data", inlineData);

        parts.put(textPart);
        parts.put(inlineDataPart);
        content.put("parts", parts);
        contents.put(content);
        payload.put("contents", contents);

        return payload;
    }

    private String getBase64FromUri(String uriString) {
        try {
            Uri uri = Uri.parse(uriString);
            InputStream is;
            if (uriString.startsWith("/")) {
                is = new java.io.FileInputStream(new File(uriString));
            } else {
                is = requireContext().getContentResolver().openInputStream(uri);
            }
            if (is == null) return null;

            Bitmap bitmap = BitmapFactory.decodeStream(is);
            is.close();

            // Resize to max 800x800 for API optimization
            int maxDim = 800;
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            if (width > maxDim || height > maxDim) {
                float ratio = Math.min((float) maxDim / width, (float) maxDim / height);
                width = Math.round((float) ratio * width);
                height = Math.round((float) ratio * height);
                Bitmap resized = Bitmap.createScaledBitmap(bitmap, width, height, true);
                if (resized != bitmap) {
                    bitmap.recycle();
                    bitmap = resized;
                }
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
            byte[] b = baos.toByteArray();
            bitmap.recycle();
            return Base64.encodeToString(b, Base64.NO_WRAP);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void showLoadingState(boolean isLoading) {
        if (binding == null) return;
        // Hiện tại không có id loading indicator cụ thể trong layout,
        // Ta có thể làm mờ container và đổi text nút save tạm thời
        binding.skinResultMetricsContainer.setAlpha(isLoading ? 0.3f : 1.0f);
        binding.skinResultRadarChart.setAlpha(isLoading ? 0.3f : 1.0f);
        if (isLoading) {
            binding.skinResultSummaryText.setText("AI đang xử lý hình ảnh khuôn mặt của bạn...");
        }
    }

    private void useFallbackMockData(String imageUri) {
        try {
            String userId = ProfileSession.getUserId(requireContext());
            JSONArray allHistory = new LocalJsonReader(requireContext()).getSkinHistory();
            JSONArray arrayToUse = new JSONArray();
            for (int i = 0; i < allHistory.length(); i++) {
                JSONObject item = allHistory.getJSONObject(i);
                String ownerId = item.optString("userId", item.optString("user_id", ""));
                if (ownerId.isEmpty() || userId.equals(ownerId)) {
                    arrayToUse.put(item);
                }
            }
            if (arrayToUse.length() == 0) {
                arrayToUse = allHistory;
            }
            if (arrayToUse.length() > 0) {
                JSONObject data = new JSONObject(arrayToUse.getJSONObject(0).toString());
                
                Calendar cal = Calendar.getInstance();
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", new Locale("vi"));
                SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", new Locale("vi"));
                data.put("date", dateFormat.format(cal.getTime()));
                data.put("time", timeFormat.format(cal.getTime()));
                data.put("id", "sh_" + System.currentTimeMillis());
                
                if (imageUri != null) {
                    data.put("imageUrl", imageUri);
                }

                currentData = data;
                bindData(data);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), "Lỗi tải dữ liệu", Toast.LENGTH_SHORT).show();
        }
    }

    private void bindData(JSONObject data) {
        if (binding == null) return;
        try {
            int score = data.getInt("score");
            binding.skinResultScoreVal.setText(String.valueOf(score));
            binding.skinResultScoreLabel.setText(data.getString("overallCondition"));
            binding.skinResultSummaryText.setText(data.getString("summaryText"));
            
            String datetime = data.getString("date") + " - " + data.getString("time");
            binding.skinResultDateTime.setText(datetime);

            String imageUrl = data.optString("imageUrl", "");
            if (!imageUrl.isEmpty()) {
                try {
                    if (imageUrl.startsWith("/")) {
                        binding.skinResultImage.setImageURI(Uri.fromFile(new File(imageUrl)));
                    } else {
                        binding.skinResultImage.setImageURI(Uri.parse(imageUrl));
                    }
                } catch (Exception e) {
                    binding.skinResultImage.setImageResource(R.drawable.about_us_pd);
                }
            } else {
                binding.skinResultImage.setImageResource(R.drawable.about_us_pd);
            }

            // Progress bar
            LinearLayout.LayoutParams fillParams = (LinearLayout.LayoutParams) binding.skinResultProgressFill.getLayoutParams();
            fillParams.weight = score / 100f;
            binding.skinResultProgressFill.setLayoutParams(fillParams);
            
            LinearLayout.LayoutParams emptyParams = (LinearLayout.LayoutParams) binding.skinResultProgressEmpty.getLayoutParams();
            emptyParams.weight = 1f - (score / 100f);
            binding.skinResultProgressEmpty.setLayoutParams(emptyParams);

            JSONObject detailedEval = data.getJSONObject("detailedEvaluation");
            setupRadarChart(detailedEval);
            populateMetrics(detailedEval);
            
            JSONObject skinCondition = data.getJSONObject("skinCondition");
            populateSkinCondition(skinCondition);

            JSONArray suggestions = data.getJSONArray("suggestions");
            populateSuggestions(suggestions);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupRadarChart(JSONObject eval) throws Exception {
        float moisture = (float) eval.getJSONObject("moisture").getInt("score");
        float oil = (float) eval.getJSONObject("oil").getInt("score");
        float pores = (float) eval.getJSONObject("pores").getInt("score");
        float pigmentation = (float) eval.getJSONObject("pigmentation").getInt("score");
        float sensitivity = (float) eval.getJSONObject("sensitivity").getInt("score");
        
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
        radarData.setDrawValues(false); // Hide overlapping data point values
        radarData.setValueTextColor(Color.parseColor("#333333"));

        RadarChart chart = binding.skinResultRadarChart;
        chart.setData(radarData);
        chart.getDescription().setEnabled(false);
        chart.setWebLineWidth(1f);
        chart.setWebColor(Color.parseColor("#E6EBE6"));
        chart.setWebLineWidthInner(1f);
        chart.setWebColorInner(Color.parseColor("#E6EBE6"));
        chart.setWebAlpha(255);

        chart.getXAxis().setDrawLabels(false);
        chart.getYAxis().setLabelCount(5, false);
        chart.getYAxis().setTextSize(9f);
        chart.getYAxis().setAxisMinimum(0f);
        chart.getYAxis().setAxisMaximum(100f);
        chart.getYAxis().setDrawLabels(false);

        chart.getLegend().setEnabled(false);
        chart.animateXY(1000, 1000);
        chart.invalidate();
    }

    private void populateMetrics(JSONObject eval) throws Exception {
        LinearLayout container = binding.skinResultMetricsContainer;
        container.removeAllViews();

        String[] keys = {"moisture", "oil", "pores", "pigmentation", "sensitivity"};
        String[] titles = {"Độ ẩm", "Lượng dầu", "Lỗ chân lông", "Sắc tố", "Độ nhạy cảm"};
        int[] icons = {R.drawable.ic_skin_moisture, R.drawable.ic_skin_moisture, R.drawable.ic_skin_pores, R.drawable.ic_skin_pigmentation, R.drawable.ic_skin_sensitivity};
        String[] colors = {"#1D82CD", "#3CA754", "#D88B2A", "#8D62A6", "#E35B5B"};

        for (int i = 0; i < keys.length; i++) {
            JSONObject obj = eval.getJSONObject(keys[i]);
            View view = getLayoutInflater().inflate(R.layout.item_skin_metric, container, false);
            
            FrameLayout iconContainer = view.findViewById(R.id.metric_icon_container);
            ImageView icon = view.findViewById(R.id.metric_icon);
            TextView title = view.findViewById(R.id.metric_title);
            TextView badge = view.findViewById(R.id.metric_badge);
            TextView desc = view.findViewById(R.id.metric_desc);

            android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
            gd.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            gd.setColor(Color.parseColor(colors[i]));
            iconContainer.setBackground(gd);

            icon.setImageResource(icons[i]);
            title.setText(titles[i]);
            badge.setText(obj.getString("level"));
            desc.setText(obj.getString("description"));

            container.addView(view);
        }
    }

    private void populateSkinCondition(JSONObject cond) throws Exception {
        LinearLayout container = binding.skinResultAttributesContainer;
        container.removeAllViews();

        String[] keys = {"skinType", "acne", "pigmentationStatus", "wrinkles", "evenness"};
        String[] titles = {"Loại da", "Mụn", "Thâm nám", "Nếp nhăn", "Độ đều màu"};
        int[] icons = {R.drawable.ic_skin_type_outline, R.drawable.ic_skin_acne_outline, R.drawable.ic_skin_spot_outline, R.drawable.ic_skin_wrinkle_outline, R.drawable.ic_skin_evenness_outline};

        for (int i = 0; i < keys.length; i++) {
            View view = getLayoutInflater().inflate(R.layout.item_skin_attribute, container, false);
            ImageView icon = view.findViewById(R.id.attr_icon);
            TextView title = view.findViewById(R.id.attr_title);
            TextView value = view.findViewById(R.id.attr_value);

            icon.setImageResource(icons[i]);
            title.setText(titles[i]);
            value.setText(cond.getString(keys[i]));

            container.addView(view);
            
            if (i < keys.length - 1) {
                View divider = new View(requireContext());
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(1, ViewGroup.LayoutParams.MATCH_PARENT);
                params.setMargins(4, 12, 4, 12);
                divider.setLayoutParams(params);
                divider.setBackgroundColor(Color.parseColor("#E6EBE6"));
                container.addView(divider);
            }
        }
    }

    private void populateSuggestions(JSONArray sugg) throws Exception {
        LinearLayout container = binding.skinResultSuggestionsContainer;
        container.removeAllViews();

        for (int i = 0; i < sugg.length(); i++) {
            String text = sugg.getString(i);
            TextView tv = new TextView(requireContext());
            tv.setText("• " + text);
            tv.setTextSize(11f);
            tv.setTextColor(Color.parseColor("#555555"));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 0, 8);
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
        binding = null;
        executorService.shutdown();
    }
}
