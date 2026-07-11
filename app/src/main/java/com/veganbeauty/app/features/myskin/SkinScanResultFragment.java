package com.veganbeauty.app.features.myskin;

import android.animation.ValueAnimator;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.text.TextUtils;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

import com.github.mikephil.charting.charts.RadarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.RadarData;
import com.github.mikephil.charting.data.RadarDataSet;
import com.github.mikephil.charting.data.RadarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import android.view.ScaleGestureDetector;
import android.view.MotionEvent;
import com.veganbeauty.app.BuildConfig;
import com.veganbeauty.app.R;
import com.veganbeauty.app.core.base.RootieFragment;
import com.veganbeauty.app.data.local.LocalJsonReader;
import com.veganbeauty.app.data.local.ProfileSession;
import com.veganbeauty.app.data.local.SkinHistoryLocalStore;
import com.veganbeauty.app.databinding.SkinFragmentScanResultBinding;
import com.veganbeauty.app.utils.SkinHistoryIdHelper;

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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SkinScanResultFragment extends RootieFragment {

    private static final String ARG_IMAGE_URI = "arg_image_uri";
    private static final long MIN_LOADING_MS = 3000L;

    private SkinFragmentScanResultBinding binding;
    private JSONObject currentData = null;
    private ExecutorService executorService;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private long loadingStartMs = 0L;
    private boolean analysisDone = false;
    private JSONObject pendingData = null;
    private TextToSpeech tts;
    private boolean ttsReady = false;
    private ValueAnimator loadingAnimator;
    
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
        if (executorService == null || executorService.isShutdown()) {
            executorService = Executors.newSingleThreadExecutor();
        }
        return binding.getRoot();
    }

    @Override
    public void setupUI(@NonNull View view) {
        loadingStartMs = System.currentTimeMillis();
        showLoadingOverlay(true);
        initTextToSpeech();
        binding.skinResultBtnSave.setEnabled(false);
        setupListeners();
        loadData();
    }

    private void initTextToSpeech() {
        tts = new TextToSpeech(requireContext(), status -> {
            if (status == TextToSpeech.SUCCESS) {
                int langResult = tts.setLanguage(new Locale("vi", "VN"));
                if (langResult == TextToSpeech.LANG_MISSING_DATA
                        || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts.setLanguage(Locale.getDefault());
                }
                ttsReady = true;
            }
        });
    }

    private void adjustPhotoSpacer() {
        if (binding == null) return;
        binding.skinResultBottomSheet.post(() -> {
            if (binding == null) return;
            int screenH = getResources().getDisplayMetrics().heightPixels;
            int bottomBarH = binding.skinScanResultBottomBar.getHeight();
            if (bottomBarH <= 0) {
                bottomBarH = (int) (80 * getResources().getDisplayMetrics().density);
            }
            int peekH = binding.skinResultBottomSheet.getHeight();
            int spacerH = screenH - bottomBarH - peekH;
            int minPhotoRatio = (int) (screenH * 0.62f);
            if (spacerH < minPhotoRatio) {
                spacerH = minPhotoRatio;
            }
            ViewGroup.LayoutParams lp = binding.skinResultPhotoSpacer.getLayoutParams();
            lp.height = spacerH;
            binding.skinResultPhotoSpacer.setLayoutParams(lp);
        });
    }

    private void setupListeners() {
        binding.skinResultBtnClose.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        binding.skinResultBtnSave.setOnClickListener(v -> handleSaveClick());

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
                    .replace(R.id.main_container, new com.veganbeauty.app.features.routine.SkinTimeRoutineFragment())
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

    private void handleSaveClick() {
        JSONObject data = currentData != null ? currentData : pendingData;
        if (data == null) {
            Toast.makeText(requireContext(), "Chưa có kết quả để lưu", Toast.LENGTH_SHORT).show();
            return;
        }
        binding.skinResultBtnSave.setEnabled(false);
        String userId = ProfileSession.getUserId(requireContext());
        JSONObject snapshot;
        try {
            snapshot = new JSONObject(data.toString());
        } catch (Exception e) {
            binding.skinResultBtnSave.setEnabled(true);
            Toast.makeText(requireContext(), "Lỗi khi lưu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return;
        }
        executorService.execute(() -> saveToLocal(userId, snapshot));
    }

    private void saveToLocal(String userId, JSONObject historyObj) {
        try {
            String id = historyObj.optString("id", SkinHistoryIdHelper.generateId());
            historyObj.put("id", id);
            historyObj.put("userId", userId);
            if (!historyObj.has("scanType")) {
                historyObj.put("scanType", "Quét AI");
            }

            String email = ProfileSession.getEmail(requireContext());
            SkinHistoryLocalStore.save(requireContext(), historyObj, userId, email);
            ProfileSession.applySkinProfileFromPayload(requireContext(), historyObj);
            mainHandler.post(() -> {
                if (binding == null) return;
                binding.skinResultBtnSave.setEnabled(true);
                Toast.makeText(requireContext(), "Đã lưu kết quả phân tích vào Lịch sử!", Toast.LENGTH_SHORT).show();
            });
        } catch (Exception e) {
            e.printStackTrace();
            mainHandler.post(() -> {
                if (binding == null) return;
                binding.skinResultBtnSave.setEnabled(true);
                Toast.makeText(requireContext(), "Lỗi khi lưu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void loadData() {
        String imageUri = getArguments() != null ? getArguments().getString(ARG_IMAGE_URI) : null;
        if (imageUri != null && !imageUri.isEmpty()) {
            setHeroImage(imageUri);
            analyzeImageWithGemini(imageUri);
        } else {
            useFallbackMockData(null);
        }
    }

    private void setHeroImage(String imageUrl) {
        if (binding == null) return;
        try {
            if (imageUrl.startsWith("/")) {
                binding.skinResultHeroImage.setImageURI(Uri.fromFile(new File(imageUrl)));
            } else {
                binding.skinResultHeroImage.setImageURI(Uri.parse(imageUrl));
            }
        } catch (Exception e) {
            binding.skinResultHeroImage.setImageResource(R.drawable.about_us_pd);
        }
    }

    private void showLoadingOverlay(boolean show) {
        if (binding == null) return;
        binding.skinResultLoadingOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.skinResultScrollContent.setVisibility(show ? View.GONE : View.VISIBLE);
        binding.skinScanResultBottomBar.setVisibility(show ? View.GONE : View.VISIBLE);
        if (!show) {
            binding.skinScanResultBottomBar.bringToFront();
            binding.skinResultBtnClose.bringToFront();
            binding.skinScanBtnTopHistory.bringToFront();
        }
        if (show) {
            startLoadingAnimation();
        } else {
            stopLoadingAnimation();
        }
    }

    private void startLoadingAnimation() {
        if (binding == null) return;
        stopLoadingAnimation();
        binding.skinResultLoadingOverlay.post(() -> {
            if (binding == null || binding.skinResultLoadingOverlay.getVisibility() != View.VISIBLE) return;
            loadingAnimator = ValueAnimator.ofInt(0, 100);
            loadingAnimator.setDuration(MIN_LOADING_MS);
            loadingAnimator.setRepeatCount(ValueAnimator.INFINITE);
            loadingAnimator.setRepeatMode(ValueAnimator.RESTART);
            loadingAnimator.addUpdateListener(animation -> {
                if (binding == null) return;
                int progress = (int) animation.getAnimatedValue();
                binding.skinLoadingProgressBar.setProgress(progress);
                int trackWidth = binding.skinLoadingProgressBar.getWidth()
                        - binding.ivSkinMascotLoading.getWidth();
                if (trackWidth > 0) {
                    binding.ivSkinMascotLoading.setTranslationX(trackWidth * progress / 100f);
                }
            });
            loadingAnimator.start();
        });
    }

    private void stopLoadingAnimation() {
        if (loadingAnimator != null) {
            loadingAnimator.cancel();
            loadingAnimator = null;
        }
        if (binding != null) {
            binding.skinLoadingProgressBar.setProgress(0);
            binding.ivSkinMascotLoading.setTranslationX(0f);
        }
    }

    private void finishLoadingWithData(JSONObject data) {
        pendingData = data;
        analysisDone = true;
        tryFinishLoading();
    }

    private void tryFinishLoading() {
        if (binding == null || !analysisDone) return;
        long elapsed = System.currentTimeMillis() - loadingStartMs;
        long remaining = MIN_LOADING_MS - elapsed;
        if (remaining <= 0) {
            showLoadingOverlay(false);
            if (pendingData != null) {
                currentData = pendingData;
                bindData(pendingData);
            }
        } else {
            mainHandler.postDelayed(this::tryFinishLoading, remaining);
        }
    }

    private void analyzeImageWithGemini(String imageUri) {
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
                        resultJson.put("id", SkinHistoryIdHelper.generateId());
                        resultJson.put("imageUrl", imageUri);

                        success = true;
                        mainHandler.post(() -> {
                            finishLoadingWithData(resultJson);
                            Toast.makeText(requireContext(), "Phân tích AI thành công!", Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (!success) {
                mainHandler.post(() -> useFallbackMockData(imageUri));
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
        // no-op
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
                data.put("id", SkinHistoryIdHelper.generateId());

                if (imageUri != null) {
                    data.put("imageUrl", imageUri);
                }

                currentData = data;
                finishLoadingWithData(data);
            } else {
                mainHandler.post(() -> showLoadingOverlay(false));
            }
        } catch (Exception e) {
            e.printStackTrace();
            mainHandler.post(() -> {
                Toast.makeText(requireContext(), "Lỗi tải dữ liệu", Toast.LENGTH_SHORT).show();
                showLoadingOverlay(false);
            });
        }
    }

    private void bindData(JSONObject data) {
        if (binding == null) return;
        currentData = data;
        ProfileSession.applySkinProfileFromPayload(requireContext(), data);
        try {
            int score = data.getInt("score");
            binding.skinResultScoreVal.setText(String.valueOf(score));
            binding.skinResultScoreLabel.setText(data.getString("overallCondition"));
            binding.skinResultSummaryText.setText(data.getString("summaryText"));

            String imageUrl = data.optString("imageUrl", "");
            if (!imageUrl.isEmpty()) {
                try {
                    if (imageUrl.startsWith("/")) {
                        binding.skinResultHeroImage.setImageURI(Uri.fromFile(new File(imageUrl)));
                    } else {
                        binding.skinResultHeroImage.setImageURI(Uri.parse(imageUrl));
                    }
                } catch (Exception e) {
                    binding.skinResultHeroImage.setImageResource(R.drawable.about_us_pd);
                }
            } else {
                binding.skinResultHeroImage.setImageResource(R.drawable.about_us_pd);
            }

            JSONObject detailedEval = data.getJSONObject("detailedEvaluation");
            populateHeroGrid(detailedEval);
            setupRadarChart(detailedEval);
            populateMetrics(detailedEval);

            JSONObject skinCondition = data.getJSONObject("skinCondition");
            populateSkinCondition(skinCondition);

            JSONArray suggestions = data.getJSONArray("suggestions");
            populateSuggestions(suggestions);

            adjustPhotoSpacer();
            speakResults(data);
            binding.skinResultBtnSave.setEnabled(true);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void populateHeroGrid(JSONObject eval) throws Exception {
        LinearLayout container = binding.skinResultGridContainer;
        container.removeAllViews();

        String[][] gridDef = {
                {"sensitivity", "Mụn viêm", "#677559"},
                {"oil", "Mụn không viêm", "#F1C40F"},
                {"oil", "Sợi bã nhờn", "#97A49C"},
                {"pigmentation", "Sẹo", "#E74C3C"},
                {"pigmentation", "Sắc tố da", "#E67E22"},
                {"pores", "Lỗ chân lông", "#27AE60"}
        };

        int lowestIdx = 0;
        int lowestScore = 101;
        List<Integer> scores = new ArrayList<>();

        for (int i = 0; i < gridDef.length; i++) {
            int raw = eval.getJSONObject(gridDef[i][0]).getInt("score");
            int outOf10 = Math.max(1, Math.round(raw / 10f));
            scores.add(outOf10);
            if (outOf10 < lowestScore) {
                lowestScore = outOf10;
                lowestIdx = i;
            }
        }

        LinearLayout issuesContainer = binding.skinResultIssuesContainer;
        issuesContainer.removeAllViews();
        List<Integer> sorted = new ArrayList<>();
        for (int i = 0; i < gridDef.length; i++) sorted.add(i);
        sorted.sort((a, b) -> Integer.compare(scores.get(a), scores.get(b)));
        for (int i = 0; i < Math.min(2, sorted.size()); i++) {
            TextView issueTv = new TextView(requireContext());
            issueTv.setText("- " + gridDef[sorted.get(i)][1]);
            issueTv.setTextSize(14f);
            issueTv.setMaxLines(1);
            issueTv.setEllipsize(TextUtils.TruncateAt.END);
            issueTv.setTextColor(requireContext().getColor(R.color.primary));
            try {
                issueTv.setTypeface(ResourcesCompat.getFont(requireContext(), R.font.be_vietnam_pro));
            } catch (Exception ignored) {
            }
            issuesContainer.addView(issueTv);
        }

        for (int row = 0; row < 3; row++) {
            LinearLayout rowLayout = new LinearLayout(requireContext());
            rowLayout.setOrientation(LinearLayout.HORIZONTAL);
            rowLayout.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            for (int col = 0; col < 2; col++) {
                int idx = row * 2 + col;
                View card = getLayoutInflater().inflate(R.layout.item_skin_score_grid, rowLayout, false);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                lp.setMargins(col == 0 ? 0 : 4, 0, col == 1 ? 0 : 4, 8);
                card.setLayoutParams(lp);

                TextView label = card.findViewById(R.id.grid_label);
                TextView scoreTv = card.findViewById(R.id.grid_score);
                View dot = card.findViewById(R.id.grid_dot);

                label.setText(gridDef[idx][1]);
                scoreTv.setText(scores.get(idx) + "/10");

                android.graphics.drawable.GradientDrawable dotBg = new android.graphics.drawable.GradientDrawable();
                dotBg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
                dotBg.setColor(Color.parseColor(gridDef[idx][2]));
                dot.setBackground(dotBg);

                if (idx == lowestIdx) {
                    card.setBackgroundColor(requireContext().getColor(R.color.primary_light));
                }

                rowLayout.addView(card);
            }
            container.addView(rowLayout);
        }
    }

    private void setupRadarChart(JSONObject eval) throws Exception {
        List<String> labels = new ArrayList<>();
        labels.add("Độ ẩm");
        labels.add("Dầu");
        labels.add("Lỗ chân lông");
        labels.add("Sắc tố");
        labels.add("Nhạy cảm");

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
        radarData.setDrawValues(false);
        radarData.setValueTextColor(Color.parseColor("#333333"));

        RadarChart chart = binding.skinResultRadarChart;
        chart.setData(radarData);
        chart.getDescription().setEnabled(false);
        chart.setWebLineWidth(1f);
        chart.setWebColor(Color.parseColor("#E6EBE6"));
        chart.setWebLineWidthInner(1f);
        chart.setWebColorInner(Color.parseColor("#E6EBE6"));
        chart.setWebAlpha(255);

        XAxis xAxis = chart.getXAxis();
        xAxis.setTextSize(10f);
        xAxis.setYOffset(0f);
        xAxis.setXOffset(0f);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setTextColor(Color.parseColor("#4A5D3E"));

        YAxis yAxis = chart.getYAxis();
        yAxis.setLabelCount(5, false);
        yAxis.setTextSize(11f);
        yAxis.setAxisMinimum(0f);
        yAxis.setAxisMaximum(100f);
        yAxis.setDrawLabels(false);

        chart.setRotationEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.animateXY(1000, 1000);
        chart.invalidate();

        setupChartPinchZoom(chart);
    }

    private void setupChartPinchZoom(View chartView) {
        final float[] scaleFactor = {1.0f};
        final float minScale = 0.8f;
        final float maxScale = 2.5f;

        ScaleGestureDetector scaleDetector = new ScaleGestureDetector(requireContext(),
                new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    @Override
                    public boolean onScale(ScaleGestureDetector detector) {
                        scaleFactor[0] *= detector.getScaleFactor();
                        scaleFactor[0] = Math.max(minScale, Math.min(scaleFactor[0], maxScale));
                        chartView.setScaleX(scaleFactor[0]);
                        chartView.setScaleY(scaleFactor[0]);
                        return true;
                    }
                });

        chartView.setOnTouchListener((v, event) -> {
            scaleDetector.onTouchEvent(event);
            if (event.getAction() == MotionEvent.ACTION_UP) {
                v.performClick();
            }
            return true;
        });
    }

    private void populateMetrics(JSONObject eval) throws Exception {
        LinearLayout container = binding.skinResultMetricsContainer;
        container.removeAllViews();

        String[] keys = {"moisture", "oil", "pores", "pigmentation", "sensitivity"};
        String[] titles = {"Độ ẩm", "Lượng dầu", "Lỗ chân lông", "Sắc tố", "Độ nhạy cảm"};
        String[] colors = {"#1D82CD", "#3CA754", "#D88B2A", "#8D62A6", "#E35B5B"};

        for (int i = 0; i < keys.length; i++) {
            JSONObject obj = eval.getJSONObject(keys[i]);
            View view = getLayoutInflater().inflate(R.layout.item_skin_metric, container, false);

            View dot = view.findViewById(R.id.metric_dot);
            TextView title = view.findViewById(R.id.metric_title);
            TextView badge = view.findViewById(R.id.metric_badge);
            TextView desc = view.findViewById(R.id.metric_desc);

            android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
            gd.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            gd.setColor(Color.parseColor(colors[i]));
            dot.setBackground(gd);

            title.setText(titles[i]);
            badge.setText(obj.getString("level"));
            desc.setText(obj.getString("description"));

            container.addView(view);
        }
    }

    private void populateSkinCondition(JSONObject cond) throws Exception {
        LinearLayout container = binding.skinResultAttributesContainer;
        container.removeAllViews();
        container.setOrientation(LinearLayout.VERTICAL);

        String[] keys = {"skinType", "acne", "pigmentationStatus", "wrinkles", "evenness"};
        String[] titles = {"Loại da", "Mụn", "Thâm nám", "Nếp nhăn", "Độ đều màu"};

        for (int row = 0; row < 3; row++) {
            LinearLayout rowLayout = new LinearLayout(requireContext());
            rowLayout.setOrientation(LinearLayout.HORIZONTAL);
            rowLayout.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            boolean isLastRow = row == 2;
            int colCount = isLastRow ? 1 : 2;

            for (int col = 0; col < colCount; col++) {
                int idx = row * 2 + col;
                View card = getLayoutInflater().inflate(R.layout.item_skin_condition_grid, rowLayout, false);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        0, ViewGroup.LayoutParams.WRAP_CONTENT, isLastRow ? 1f : 1f);
                if (!isLastRow) {
                    lp.setMargins(col == 0 ? 0 : 4, 0, col == 1 ? 0 : 4, 8);
                } else {
                    lp.setMargins(0, 0, 0, 8);
                }
                card.setLayoutParams(lp);

                TextView title = card.findViewById(R.id.attr_title);
                TextView value = card.findViewById(R.id.attr_value);
                title.setText(titles[idx]);
                value.setText(cond.getString(keys[idx]));
                rowLayout.addView(card);
            }
            container.addView(rowLayout);
        }
    }

    private void populateSuggestions(JSONArray sugg) throws Exception {
        LinearLayout container = binding.skinResultSuggestionsContainer;
        container.removeAllViews();

        float density = getResources().getDisplayMetrics().density;
        int itemGap = (int) (10 * density);

        for (int i = 0; i < sugg.length(); i++) {
            String text = sugg.getString(i);
            TextView tv = new TextView(requireContext());
            tv.setText("• " + text);
            tv.setTextSize(16f);
            tv.setLineSpacing(4f * density, 1f);
            tv.setTextColor(requireContext().getColor(R.color.content));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 0, itemGap);
            tv.setLayoutParams(params);
            try {
                tv.setTypeface(ResourcesCompat.getFont(requireContext(), R.font.be_vietnam_pro));
            } catch (Exception e) {
                e.printStackTrace();
            }
            container.addView(tv);
        }
    }

    private void speakResults(JSONObject data) {
        if (!ttsReady || tts == null) return;
        try {
            String speech = buildSpeechText(data);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                tts.speak(speech, TextToSpeech.QUEUE_FLUSH, null, "skin_result_tts");
            } else {
                tts.speak(speech, TextToSpeech.QUEUE_FLUSH, null);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String buildSpeechText(JSONObject data) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("Kết quả phân tích da. ");
        sb.append("Điểm da của bạn là ").append(data.getInt("score")).append(" trên 100. ");
        sb.append(data.getString("overallCondition")).append(". ");
        sb.append(data.getString("summaryText")).append(" ");

        JSONObject eval = data.getJSONObject("detailedEvaluation");
        String[] keys = {"moisture", "oil", "pores", "pigmentation", "sensitivity"};
        String[] titles = {"Độ ẩm", "Lượng dầu", "Lỗ chân lông", "Sắc tố", "Độ nhạy cảm"};
        sb.append("Giải thích chi tiết. ");
        for (int i = 0; i < keys.length; i++) {
            JSONObject obj = eval.getJSONObject(keys[i]);
            sb.append(titles[i]).append(": ").append(obj.getString("level")).append(". ");
            sb.append(obj.getString("description")).append(" ");
        }

        JSONObject cond = data.getJSONObject("skinCondition");
        sb.append("Tình trạng da. ");
        sb.append("Loại da: ").append(cond.getString("skinType")).append(". ");
        sb.append("Mụn: ").append(cond.getString("acne")).append(". ");
        sb.append("Thâm nám: ").append(cond.getString("pigmentationStatus")).append(". ");
        sb.append("Nếp nhăn: ").append(cond.getString("wrinkles")).append(". ");
        sb.append("Độ đều màu: ").append(cond.getString("evenness")).append(". ");

        JSONArray suggestions = data.getJSONArray("suggestions");
        sb.append("Gợi ý cho bạn. ");
        for (int i = 0; i < suggestions.length(); i++) {
            sb.append(suggestions.getString(i)).append(" ");
        }
        return sb.toString();
    }

    @Override
    public void onDestroyView() {
        stopLoadingAnimation();
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
            ttsReady = false;
        }
        super.onDestroyView();
        binding = null;
        executorService.shutdown();
    }
}
