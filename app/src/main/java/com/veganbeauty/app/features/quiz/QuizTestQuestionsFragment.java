package com.veganbeauty.app.features.quiz;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.veganbeauty.app.R;
import com.veganbeauty.app.core.base.RootieFragment;
import com.veganbeauty.app.databinding.QuizTestQuestionsBinding;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class QuizTestQuestionsFragment extends RootieFragment {

    private static final String ARG_LEVEL_TYPE = "level_type";

    private QuizTestQuestionsBinding binding;
    private JSONArray questions = new JSONArray();
    private int currentQuestionIndex = 0;
    private String levelType = "advanced";
    private int totalQuestionsLimit = 20;

    private final Map<Integer, Integer> selectedAnswers = new HashMap<>();

    public static QuizTestQuestionsFragment newInstance(String level) {
        QuizTestQuestionsFragment fragment = new QuizTestQuestionsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_LEVEL_TYPE, level);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = QuizTestQuestionsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void setupUI(View view) {
        loadQuestions();

        binding.btnBack.setOnClickListener(v -> {
            if (currentQuestionIndex > 0) goToPrevQuestion();
            else exitQuiz();
        });

        binding.btnPrevQuestion.setOnClickListener(v -> goToPrevQuestion());

        displayQuestion();
    }

    private void loadQuestions() {
        try {
            if (getArguments() != null) {
                levelType = getArguments().getString(ARG_LEVEL_TYPE, "advanced");
            }
            totalQuestionsLimit = "basic".equals(levelType) ? 15 : 30;

            BufferedReader br = new BufferedReader(new InputStreamReader(requireContext().getAssets().open("quiz_cauhoi.json")));
            StringBuilder sb = new StringBuilder(); String line;
            while ((line = br.readLine()) != null) sb.append(line);
            br.close();

            JSONObject jsonObject = new JSONObject(sb.toString());
            JSONArray allQuestions = jsonObject.getJSONArray("questions");

            Map<String, JSONObject> questionsMap = new HashMap<>();
            for (int i = 0; i < allQuestions.length(); i++) {
                JSONObject q = allQuestions.getJSONObject(i);
                questionsMap.put(q.getString("id"), q);
            }

            List<String> targetIds;
            if ("basic".equals(levelType)) {
                targetIds = Arrays.asList("q1", "q2", "q3", "q4", "q5", "q6", "q7", "q8", "q9", "q10", "q21", "q22", "q23", "q33", "q39");
            } else {
                targetIds = Arrays.asList("q1", "q2", "q3", "q4", "q5", "q6", "q7", "q8", "q9", "q10",
                        "q21", "q22", "q23", "q33", "q39", "q11", "q12", "q13", "q14", "q15", "q16", "q17", "q18", "q19", "q20",
                        "q26", "q27", "q28", "q29", "q30");
            }

            JSONArray limitedList = new JSONArray();
            for (String id : targetIds) {
                JSONObject q = questionsMap.get(id);
                if (q != null) limitedList.put(q);
            }
            questions = limitedList;

            binding.pbQuiz.setMax(questions.length());
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Lỗi tải câu hỏi quiz", Toast.LENGTH_SHORT).show();
        }
    }

    private void displayQuestion() {
        if (questions.length() == 0) return;

        binding.pbQuiz.setProgress(currentQuestionIndex + 1);
        binding.tvProgressText.setText("Câu " + (currentQuestionIndex + 1) + " / " + questions.length());

        try {
            JSONObject questionObj = questions.getJSONObject(currentQuestionIndex);
            binding.tvQuestionText.setText(questionObj.getString("question"));

            JSONArray optionsArray = questionObj.getJSONArray("options");
            binding.llOptionsContainer.removeAllViews();

            Integer savedAnswer = selectedAnswers.get(currentQuestionIndex);

            for (int i = 0; i < optionsArray.length(); i++) {
                JSONObject optionObj = optionsArray.getJSONObject(i);
                String optionText = optionObj.getString("text");

                View optionView = LayoutInflater.from(getContext()).inflate(R.layout.quiz_item_option, binding.llOptionsContainer, false);
                TextView tvOptionText = optionView.findViewById(R.id.tv_option_text);
                FrameLayout flOptionIconBg = optionView.findViewById(R.id.fl_option_icon_bg);
                ImageView ivOptionIcon = optionView.findViewById(R.id.iv_option_icon);

                tvOptionText.setText(optionText);

                String optionTextLower = optionText.toLowerCase();
                int iconResId;
                if (optionTextLower.contains("khô") || optionTextLower.contains("căng") || optionTextLower.contains("yếu") || optionTextLower.contains("mỏng") || optionTextLower.contains("thiếu")) {
                    iconResId = R.drawable.ic_dry;
                } else if (optionTextLower.contains("dầu") || optionTextLower.contains("nhờn") || optionTextLower.contains("bóng") || optionTextLower.contains("mụn") || optionTextLower.contains("nhiều") || optionTextLower.contains("to")) {
                    iconResId = R.drawable.ic_water;
                } else if (optionTextLower.contains("hỗn hợp") || optionTextLower.contains("không đều") || optionTextLower.contains("ửng đỏ") || optionTextLower.contains("rát") || optionTextLower.contains("kích ứng") || optionTextLower.contains("thỉnh thoảng") || optionTextLower.contains("nhạy cảm") || optionTextLower.contains("dễ")) {
                    iconResId = R.drawable.ic_ai_face;
                } else if (optionTextLower.contains("bình thường") || optionTextLower.contains("không") || optionTextLower.contains("khỏe") || optionTextLower.contains("ít")) {
                    iconResId = R.drawable.ic_face_normal;
                } else {
                    switch (i) {
                        case 0: iconResId = R.drawable.ic_dry; break;
                        case 1: iconResId = R.drawable.ic_face_normal; break;
                        case 2: iconResId = R.drawable.ic_water; break;
                        default: iconResId = R.drawable.ic_ai_face; break;
                    }
                }
                ivOptionIcon.setImageResource(iconResId);

                if (savedAnswer != null && savedAnswer == i) {
                    optionView.setBackgroundResource(R.drawable.quiz_bg_option);
                    optionView.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#D8E8C6")));
                    tvOptionText.setTextColor(Color.parseColor("#3E4D44"));
                    tvOptionText.setTypeface(null, Typeface.BOLD);
                    flOptionIconBg.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#3E4D44")));
                    ivOptionIcon.setImageTintList(ColorStateList.valueOf(Color.parseColor("#FFFFFF")));
                } else {
                    optionView.setBackgroundResource(R.drawable.quiz_bg_option);
                    optionView.setBackgroundTintList(null);
                    tvOptionText.setTextColor(Color.parseColor("#3E4D44"));
                    tvOptionText.setTypeface(null, Typeface.NORMAL);
                    flOptionIconBg.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#EAF2E3")));
                    ivOptionIcon.setImageTintList(ColorStateList.valueOf(Color.parseColor("#3E4D44")));
                }

                int finalI = i;
                optionView.setOnClickListener(v -> {
                    selectedAnswers.put(currentQuestionIndex, finalI);

                    optionView.setBackgroundResource(R.drawable.quiz_bg_option);
                    optionView.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#D8E8C6")));
                    tvOptionText.setTextColor(Color.parseColor("#3E4D44"));
                    tvOptionText.setTypeface(null, Typeface.BOLD);
                    flOptionIconBg.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#3E4D44")));
                    ivOptionIcon.setImageTintList(ColorStateList.valueOf(Color.parseColor("#FFFFFF")));

                    optionView.postDelayed(this::goToNextQuestion, 200);
                });

                binding.llOptionsContainer.addView(optionView);
            }
        } catch (Exception e) { e.printStackTrace(); }

        binding.btnPrevQuestion.setVisibility(currentQuestionIndex == 0 ? View.INVISIBLE : View.VISIBLE);
    }

    private void goToNextQuestion() {
        if (currentQuestionIndex < questions.length() - 1) {
            currentQuestionIndex++;
            displayQuestion();
        } else {
            calculateAndFinish();
        }
    }

    private void goToPrevQuestion() {
        if (currentQuestionIndex > 0) {
            currentQuestionIndex--;
            displayQuestion();
        }
    }

    private void exitQuiz() {
        getParentFragmentManager().popBackStack();
    }

    private void calculateAndFinish() {
        try {
            Map<String, Integer> scores = new HashMap<>();
            List<String> skinTypes = Arrays.asList("dry", "oily", "sensitive", "dehydrated", "aging", "reactive", "acne", "normal", "combination");
            for (String type : skinTypes) scores.put(type, 0);

            Set<String> flaggedGroups = new HashSet<>();

            for (int qIdx = 0; qIdx < questions.length(); qIdx++) {
                Integer selectedOptIdx = selectedAnswers.get(qIdx);
                if (selectedOptIdx == null) continue;

                JSONObject questionObj = questions.getJSONObject(qIdx);
                JSONObject optionObj = questionObj.getJSONArray("options").getJSONObject(selectedOptIdx);

                if (optionObj.has("score")) {
                    JSONObject scoreObj = optionObj.getJSONObject("score");
                    Iterator<String> keys = scoreObj.keys();
                    while (keys.hasNext()) {
                        String key = keys.next();
                        int value = scoreObj.getInt(key);
                        scores.put(key, scores.get(key) + value);
                    }
                }

                if (optionObj.has("group")) {
                    String groupVal = optionObj.getString("group");
                    if (!groupVal.isEmpty() && !"null".equals(groupVal)) {
                        flaggedGroups.add(groupVal);
                    }
                }
            }

            List<String> primaryTypes = Arrays.asList("oily", "dry", "normal", "combination");
            String primaryType = "normal";
            int maxPrimaryScore = -1;

            for (String pt : primaryTypes) {
                int s = scores.get(pt);
                if (s > maxPrimaryScore) {
                    maxPrimaryScore = s;
                    primaryType = pt;
                }
            }

            String finalSkinTypeKey = primaryType;
            int sensitiveScore = scores.get("sensitive");
            int dehydratedScore = scores.get("dehydrated");
            int acneScore = scores.get("acne");

            if ("oily".equals(primaryType) && sensitiveScore >= 3) finalSkinTypeKey = "oily_sensitive";
            else if ("dry".equals(primaryType) && dehydratedScore >= 3) finalSkinTypeKey = "dehydrated";
            else if ("oily".equals(primaryType) && acneScore >= 3) finalSkinTypeKey = "acne";
            else if (sensitiveScore >= 4) finalSkinTypeKey = "sensitive";

            String finalSkinTypeName;
            switch (finalSkinTypeKey) {
                case "normal": finalSkinTypeName = "Da thường"; break;
                case "dry": finalSkinTypeName = "Da khô"; break;
                case "oily": finalSkinTypeName = "Da dầu"; break;
                case "combination": finalSkinTypeName = "Da hỗn hợp"; break;
                case "sensitive": finalSkinTypeName = "Da nhạy cảm"; break;
                case "acne": finalSkinTypeName = "Da mụn"; break;
                case "aging": finalSkinTypeName = "Da lão hóa"; break;
                case "dehydrated": finalSkinTypeName = "Da mất nước"; break;
                case "reactive": finalSkinTypeName = "Da dễ kích ứng"; break;
                case "oily_sensitive": finalSkinTypeName = "Da dầu nhạy cảm"; break;
                default: finalSkinTypeName = "Da thường"; break;
            }

            String recommendation;
            switch (finalSkinTypeKey) {
                case "oily_sensitive": recommendation = "Nên dùng sản phẩm dịu nhẹ, không cồn, không hương liệu và cấp nước nhẹ nhàng."; break;
                case "acne": recommendation = "Tập trung kháng viêm, ngừa khuẩn, tránh các sản phẩm dầu khoáng gây bít tắc lỗ chân lông."; break;
                case "dehydrated": recommendation = "Cấp ẩm tầng sâu bằng Hyaluronic Acid và glycerin, tránh sữa rửa mặt tạo bọt mạnh."; break;
                case "sensitive": recommendation = "Sử dụng các thành phần làm dịu chiết xuất rau má, trà xanh, tránh tuyệt đối cồn khô."; break;
                case "dry": recommendation = "Cấp ẩm bằng kem dưỡng ẩm đậm đặc, khóa ẩm tốt để tránh mất nước qua da."; break;
                case "oily": recommendation = "Kiềm dầu nhẹ nhàng, cấp nước dạng gel, rửa mặt sạch sâu bằng chất làm sạch dịu nhẹ."; break;
                default: recommendation = "Duy trì chế độ dưỡng ẩm cân bằng và làm sạch da dịu nhẹ hàng ngày."; break;
            }

            int dryScore = scores.get("dry");
            int agingScore = scores.get("aging");
            int oilyScore = scores.get("oily");

            int sensitivityVal = Math.max(10, Math.min(100, sensitiveScore * 20));
            int hydrationVal = Math.max(15, Math.min(95, 95 - (dryScore * 15) - (dehydratedScore * 10)));
            int elasticityVal = Math.max(30, Math.min(98, 98 - (agingScore * 15)));
            int sebumVal = Math.max(10, Math.min(100, oilyScore * 20));

            String skinAreasVal;
            switch (finalSkinTypeKey) {
                case "oily_sensitive": skinAreasVal = "Vùng chữ T (trán, mũi, cằm) tiết nhiều dầu thừa, bóng nhờn; hai bên má nhạy cảm, dễ nổi mẩn đỏ, châm chích."; break;
                case "acne": skinAreasVal = "Vùng trán và cằm dễ bị bít tắc gây mụn; lượng dầu phân bổ không đều làm bít tắc cổ nang lông."; break;
                case "dehydrated": skinAreasVal = "Vùng chữ U (mũi và má) căng khô, thiếu nước trầm trọng; vùng chữ T có thể đổ dầu nhẹ do phản ứng bù ẩm."; break;
                case "sensitive": skinAreasVal = "Toàn bộ bề mặt da có lớp màng bảo vệ yếu, mỏng và dễ phản ứng châm chích với mọi mỹ phẩm mới."; break;
                case "dry": skinAreasVal = "Khô ráp toàn mặt, vùng má căng chặt và có xu hướng bong tróc vảy da chết li ti."; break;
                case "oily": skinAreasVal = "Lượng dầu hoạt động mạnh mẽ trên toàn bộ khuôn mặt, bóng loáng đặc biệt ở vùng chữ T và hai bên cánh mũi."; break;
                case "combination": skinAreasVal = "Vùng chữ T đổ dầu nhiều và lỗ chân lông to; vùng chữ U (má) bình thường hoặc khô nhẹ."; break;
                case "normal": skinAreasVal = "Độ ẩm phân bổ đều đặn, vùng chữ T dầu nhẹ không đáng kể, vùng má mịn màng đàn hồi tốt."; break;
                default: skinAreasVal = "Độ ẩm và bã nhờn phân bổ tương đối đồng đều trên các vùng da."; break;
            }

            SharedPreferences prefs = requireContext().getSharedPreferences("RootieQuizPrefs", Context.MODE_PRIVATE);
            prefs.edit()
                    .putString("SKIN_TYPE_RESULT", finalSkinTypeName)
                    .putString("RECOMMENDATION", recommendation)
                    .putStringSet("FLAGGED_GROUPS", flaggedGroups)
                    .putInt("SENSITIVITY_PERCENT", sensitivityVal)
                    .putInt("HYDRATION_PERCENT", hydrationVal)
                    .putInt("ELASTICITY_PERCENT", elasticityVal)
                    .putInt("SEBUM_PERCENT", sebumVal)
                    .putString("SKIN_AREAS_DESC", skinAreasVal)
                    .apply();

            getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, new QuizTestLoadingFragment())
                    .commit();

        } catch (Exception e) { e.printStackTrace(); }
    }

    @Override
    public void observeViewModel() {}

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
