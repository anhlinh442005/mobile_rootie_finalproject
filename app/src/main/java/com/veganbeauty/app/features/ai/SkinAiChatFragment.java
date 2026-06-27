package com.veganbeauty.app.features.ai;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.LifecycleOwnerKt;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.veganbeauty.app.BuildConfig;
import com.veganbeauty.app.R;
import com.veganbeauty.app.data.local.LocalJsonReader;
import com.veganbeauty.app.data.local.ProfileSession;
import com.veganbeauty.app.data.local.entities.ProductEntity;
import com.veganbeauty.app.data.local.entities.StoreEntity;
import com.veganbeauty.app.data.local.entities.IngredientEntity;
import com.veganbeauty.app.databinding.SkinAiChatBinding;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import kotlinx.coroutines.BuildersKt;
import kotlinx.coroutines.Dispatchers;

public class SkinAiChatFragment extends DialogFragment {

    private SkinAiChatBinding binding;
    private RootieChatAdapter chatAdapter;
    private List<ProductEntity> allProducts;

    private final String GEMINI_API_KEY = BuildConfig.GEMINI_API_KEY;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = SkinAiChatBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupUI(view);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getShowsDialog() && getDialog() != null && getDialog().getWindow() != null) {
            int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.90);
            int height = (int) (getResources().getDisplayMetrics().heightPixels * 0.85);
            getDialog().getWindow().setLayout(width, height);
            getDialog().getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
    }

    private void setupUI(View view) {
        allProducts = new LocalJsonReader(requireContext()).getAllProducts();

        if (!getShowsDialog()) {
            if (getActivity() != null) {
                View floatingHead = getActivity().findViewById(R.id.skin_ai_floating_chat_head);
                if (floatingHead != null) {
                    floatingHead.setVisibility(View.GONE);
                }
            }
        } else {
            view.setBackgroundResource(R.drawable.bg_chat_dialog);
            view.setClipToOutline(true);
            view.setOutlineProvider(ViewOutlineProvider.BACKGROUND);
        }

        setupRecyclerView();
        setupListeners();

        List<RootieChatItem> savedHistory = ChatHistoryHelper.loadChatHistory(requireContext());
        if (savedHistory.isEmpty()) {
            sendInitialGreeting();
        } else {
            chatAdapter.submitList(savedHistory);
            binding.rvChatList.scrollToPosition(chatAdapter.getItemCount() - 1);
        }
    }

    private void setupRecyclerView() {
        chatAdapter = new RootieChatAdapter(requireContext(), LifecycleOwnerKt.getLifecycleScope(getViewLifecycleOwner()));
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        layoutManager.setStackFromEnd(true);
        binding.rvChatList.setLayoutManager(layoutManager);
        binding.rvChatList.setAdapter(chatAdapter);

        binding.rvChatList.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            if (bottom < oldBottom) {
                binding.rvChatList.postDelayed(() -> {
                    int count = chatAdapter.getItemCount();
                    if (count > 0) {
                        binding.rvChatList.smoothScrollToPosition(count - 1);
                    }
                }, 100);
            }
        });
    }

    private void setupListeners() {
        binding.btnBack.setOnClickListener(v -> {
            if (getShowsDialog()) {
                dismiss();
            } else {
                getParentFragmentManager().popBackStack();
            }
        });

        binding.btnSend.setOnClickListener(v -> sendMessage());

        binding.etMessageInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage();
                return true;
            }
            return false;
        });

        binding.chipWeatherAdvice.setOnClickListener(v -> sendQuickPrompt("⛅ Thời tiết & Da hôm nay"));
        binding.chipWeatherRoutine.setOnClickListener(v -> sendQuickPrompt("🧴 Routine theo thời tiết"));
        binding.chipMatchProducts.setOnClickListener(v -> sendQuickPrompt("🎯 Sản phẩm Rootie của tôi"));
        binding.chipSkinDiagnosis.setOnClickListener(v -> sendQuickPrompt("📋 Phác đồ chẩn đoán da"));

        binding.btnPlus.setOnClickListener(v -> Toast.makeText(requireContext(), "Tính năng đính kèm tệp sẽ sớm ra mắt!", Toast.LENGTH_SHORT).show());

        binding.btnMore.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(requireContext(), v);
            popup.getMenu().add("Xóa lịch sử trò chuyện");
            popup.setOnMenuItemClickListener(menuItem -> {
                if ("Xóa lịch sử trò chuyện".equals(menuItem.getTitle())) {
                    ChatHistoryHelper.clearChatHistory(requireContext());
                    chatAdapter.submitList(new ArrayList<>());
                    sendInitialGreeting();
                    Toast.makeText(requireContext(), "Đã xóa lịch sử trò chuyện.", Toast.LENGTH_SHORT).show();
                    return true;
                }
                return false;
            });
            popup.show();
        });
    }

    private String getCurrentTime() {
        return new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());
    }

    private void sendInitialGreeting() {
        SharedPreferences prefs = requireContext().getSharedPreferences("RootieQuizPrefs", Context.MODE_PRIVATE);
        String skinType = prefs.getString("SAVED_USER_SKIN_TYPE", "Da hỗn hợp thiên dầu");
        String fullName = ProfileSession.getFullName(requireContext());
        if (fullName == null) fullName = "bạn";

        String greetingText = "Chào " + fullName + "! Mình là Rootie AI. Dựa trên kết quả quiz da mới nhất của bạn (" + skinType + "), mình đã thực hiện chẩn đoán chuyên sâu và thiết kế lộ trình chăm sóc thuần chay riêng cho bạn dưới đây:";
        RootieChatItem greetingItem = new RootieChatItem(
                RootieChatItem.Sender.AI,
                greetingText,
                getCurrentTime(),
                RootieChatItem.ItemType.TEXT,
                null
        );
        chatAdapter.addMessage(greetingItem);

        RootieChatItem.DiagnosticData diagnosticData = generateRuleBasedDiagnostic(skinType);
        RootieChatItem diagnosticItem = new RootieChatItem(
                RootieChatItem.Sender.AI,
                null,
                getCurrentTime(),
                RootieChatItem.ItemType.DIAGNOSTIC,
                diagnosticData
        );
        chatAdapter.addMessage(diagnosticItem);
        binding.rvChatList.scrollToPosition(chatAdapter.getItemCount() - 1);

        ChatHistoryHelper.saveChatHistory(requireContext(), chatAdapter.getItems());
    }

    private void sendMessage() {
        String text = binding.etMessageInput.getText().toString().trim();
        if (text.isEmpty()) return;

        RootieChatItem userMsg = new RootieChatItem(
                RootieChatItem.Sender.USER,
                text,
                getCurrentTime(),
                RootieChatItem.ItemType.TEXT,
                null
        );
        chatAdapter.addMessage(userMsg);
        binding.etMessageInput.setText("");
        binding.rvChatList.scrollToPosition(chatAdapter.getItemCount() - 1);
        ChatHistoryHelper.saveChatHistory(requireContext(), chatAdapter.getItems());

        RootieChatItem thinkingMsg = new RootieChatItem(
                RootieChatItem.Sender.AI,
                "Rootie AI đang suy nghĩ...",
                getCurrentTime(),
                RootieChatItem.ItemType.TEXT,
                null
        );
        chatAdapter.addMessage(thinkingMsg);
        int thinkingPos = chatAdapter.getItemCount() - 1;
        binding.rvChatList.scrollToPosition(thinkingPos);

        SharedPreferences prefs = requireContext().getSharedPreferences("RootieQuizPrefs", Context.MODE_PRIVATE);
        String skinType = prefs.getString("SAVED_USER_SKIN_TYPE", "Da hỗn hợp thiên dầu");
        String fullName = ProfileSession.getFullName(requireContext());
        if (fullName == null) fullName = "bạn";

        if (GEMINI_API_KEY == null || GEMINI_API_KEY.trim().isEmpty() || GEMINI_API_KEY.equals("YOUR_GEMINI_API_KEY_HERE")) {
            LifecycleOwnerKt.getLifecycleScope(getViewLifecycleOwner()).launchWhenStarted((coroutineScope, continuation) -> {
                return BuildersKt.withContext(Dispatchers.getMain(), (cScope, cCont) -> {
                    binding.rvChatList.postDelayed(() -> {
                        removeChatItemAt(thinkingPos);
                        handleFallbackReply(text, skinType);
                    }, 1200);
                    return kotlin.Unit.INSTANCE;
                }, continuation);
            });
        } else {
            callGeminiApi(text, skinType, fullName, thinkingPos);
        }
    }

    private void sendQuickPrompt(String promptText) {
        RootieChatItem userMsg = new RootieChatItem(
                RootieChatItem.Sender.USER,
                promptText,
                getCurrentTime(),
                RootieChatItem.ItemType.TEXT,
                null
        );
        chatAdapter.addMessage(userMsg);
        binding.rvChatList.scrollToPosition(chatAdapter.getItemCount() - 1);
        ChatHistoryHelper.saveChatHistory(requireContext(), chatAdapter.getItems());

        RootieChatItem thinkingMsg = new RootieChatItem(
                RootieChatItem.Sender.AI,
                "Rootie AI đang suy nghĩ...",
                getCurrentTime(),
                RootieChatItem.ItemType.TEXT,
                null
        );
        chatAdapter.addMessage(thinkingMsg);
        int thinkingPos = chatAdapter.getItemCount() - 1;
        binding.rvChatList.scrollToPosition(thinkingPos);

        SharedPreferences prefs = requireContext().getSharedPreferences("RootieQuizPrefs", Context.MODE_PRIVATE);
        String skinType = prefs.getString("SAVED_USER_SKIN_TYPE", "Da hỗn hợp thiên dầu");
        String fullName = ProfileSession.getFullName(requireContext());
        if (fullName == null) fullName = "bạn";

        if (GEMINI_API_KEY == null || GEMINI_API_KEY.trim().isEmpty() || GEMINI_API_KEY.equals("YOUR_GEMINI_API_KEY_HERE")) {
            LifecycleOwnerKt.getLifecycleScope(getViewLifecycleOwner()).launchWhenStarted((coroutineScope, continuation) -> {
                return BuildersKt.withContext(Dispatchers.getMain(), (cScope, cCont) -> {
                    binding.rvChatList.postDelayed(() -> {
                        removeChatItemAt(thinkingPos);
                        handleFallbackReply(promptText, skinType);
                    }, 1200);
                    return kotlin.Unit.INSTANCE;
                }, continuation);
            });
        } else {
            callGeminiApi(promptText, skinType, fullName, thinkingPos);
        }
    }

    private void removeChatItemAt(int position) {
        if (position >= 0 && position < chatAdapter.getItemCount()) {
            chatAdapter.removeMessageAt(position);
        }
    }

    private void callGeminiApi(String userMessage, String skinType, String fullName, int thinkingPos) {
        SharedPreferences prefs = requireContext().getSharedPreferences("RootieQuizPrefs", Context.MODE_PRIVATE);
        int sensitivity = prefs.getInt("SAVED_SENSITIVITY", 50);
        int hydration = prefs.getInt("SAVED_HYDRATION", 50);
        int elasticity = prefs.getInt("SAVED_ELASTICITY", 75);
        int sebum = prefs.getInt("SAVED_SEBUM", 50);
        String skinAreas = prefs.getString("SAVED_SKIN_AREAS", "Chưa xác định chi tiết vùng da.");
        Set<String> flaggedGroups = prefs.getStringSet("SAVED_FLAGGED_GROUPS", new HashSet<>());

        float temp = prefs.getFloat("SAVED_WEATHER_TEMP", 32.0f);
        int humidityVal = prefs.getInt("SAVED_WEATHER_HUMIDITY", 68);
        float uv = prefs.getFloat("SAVED_WEATHER_UV", 9.2f);
        int pm25 = prefs.getInt("SAVED_WEATHER_PM25", 55);
        String city = prefs.getString("SAVED_WEATHER_CITY", "Thành phố Hồ Chí Minh");
        String weatherCondition = prefs.getString("SAVED_WEATHER_CONDITION", "NẮNG NHIỀU, OI NHẸ");

        StringBuilder prodSummary = new StringBuilder();
        for (ProductEntity p : allProducts) {
            prodSummary.append("- ID: ").append(p.getId()).append(" | Tên: ").append(p.getName()).append(" | Loại: ").append(p.getCategory()).append(" | Thành phần chính: ").append(p.getMainIngredientsSummary()).append(" | Giá: ").append(p.getPrice()).append("đ\n");
        }

        String flaggedGroupsStr = flaggedGroups != null && !flaggedGroups.isEmpty() ? String.join(", ", flaggedGroups) : "Không có";
        String sensitivityValStr = sensitivity >= 70 ? "Rất Cao" : (sensitivity >= 40 ? "Trung Bình" : "Thấp");
        String barrierValStr = elasticity >= 75 ? "Khỏe" : (elasticity >= 50 ? "Ổn định" : "Yếu");

        String systemPrompt = "Bạn là Rootie AI, chuyên gia tư vấn da liễu hữu cơ và thuần chay (Vegan Skincare) kiêm trợ lý thông minh đa nhiệm của ứng dụng ROOTIE.\n" +
                "Bạn có khả năng trả lời chính xác, thông minh mọi câu hỏi của người dùng, bao gồm cả các chủ đề ngoài da liễu như lập trình (viết/sửa code), toán học, dịch thuật, tóm tắt văn bản, viết nội dung sáng tạo, và kiến thức tổng hợp, đồng thời luôn giữ thái độ thân thiện, ấm áp và cá nhân hóa phản hồi theo tên người dùng (" + fullName + ").\n\n" +
                "Thông tin khách hàng:\n" +
                "- Tên: " + fullName + "\n" +
                "- Loại da hiện tại: " + skinType + "\n" +
                "- Độ ẩm da: " + hydration + "%\n" +
                "- Độ nhạy cảm: " + sensitivity + "%\n" +
                "- Độ đàn hồi: " + elasticity + "%\n" +
                "- Lượng bã nhờn: " + sebum + "%\n" +
                "- Tình trạng phân bố vùng da: " + skinAreas + "\n" +
                "- Các hoạt chất/thành phần dị ứng cần tránh hoàn toàn: " + flaggedGroupsStr + "\n\n" +
                "Thông tin thời tiết hiện tại ngày/tuần tại địa điểm của khách hàng:\n" +
                "- Địa điểm: " + city + "\n" +
                "- Thời tiết: " + temp + "°C (" + weatherCondition + ")\n" +
                "- Độ ẩm không khí: " + humidityVal + "%\n" +
                "- Chỉ số UV: " + String.format(Locale.US, "%.1f", uv) + "\n" +
                "- Chỉ số bụi mịn PM2.5: " + pm25 + " μg/m³\n\n" +
                "Các chức năng chính của ứng dụng Rootie mà bạn có thể tư vấn/hỗ trợ người dùng sử dụng:\n" +
                "1. Quiz Da / Chẩn đoán da: Đánh giá 4 chỉ số chính của da để gợi ý routine phù hợp.\n" +
                "2. Lộ trình Routine (Sáng/Tối): Thiết lập, tùy chỉnh các bước chăm sóc da và theo dõi việc thực hiện hàng ngày.\n" +
                "3. Thống kê Streak: Theo dõi số ngày liên tục hoàn thành routine để tích điểm đổi quà.\n" +
                "4. Cửa hàng mỹ phẩm: Cung cấp sản phẩm thuần chay 100% Việt Nam được chứng nhận của Cruelty-Free & Vegan (Gel rửa mặt Bí đao, Tinh chất Bí đao N15, Thạch hoa hồng hữu cơ Cao Bằng, Tinh chất nghệ Hưng Yên C10...).\n" +
                "5. Đặt lịch Spa: Đặt hẹn các buổi spa chăm sóc chuyên sâu trực tiếp tại các cửa hàng của Rootie.\n" +
                "6. Dự báo thời tiết & Da: Xem thời tiết, chỉ số UV, bụi mịn PM2.5 thời gian thực của khu vực để điều chỉnh cách chăm sóc da phù hợp.\n\n" +
                "Danh sách sản phẩm chính thức của Rootie (Chỉ giới thiệu sản phẩm trong danh sách này để người dùng mua được trên app):\n" +
                prodSummary.toString() + "\n" +
                "Nhiệm vụ và hướng dẫn trả lời:\n" +
                "1. Trả lời bằng tiếng Việt, giọng điệu ấm áp, chuyên nghiệp, thông minh.\n" +
                "2. Nếu người dùng hỏi các câu hỏi chung ngoài lề (lập trình, dịch thuật, toán học, tóm tắt...), hãy thực hiện nhiệm vụ một cách xuất sắc, chính xác và đầy đủ nhất, sau đó có thể đính kèm một câu chào thân thiện liên quan tới Rootie ở cuối.\n" +
                "3. Nếu người dùng yêu cầu lộ trình routine, chẩn đoán da hoặc gợi ý sản phẩm phù hợp với da và thời tiết hôm nay, hãy trả về khối JSON hợp lệ nằm giữa dấu ```json và ``` để hiển thị Thẻ Chẩn đoán. JSON phải có cấu trúc chính xác:\n" +
                "{\n" +
                "  \"is_diagnostic\": true,\n" +
                "  \"assessment\": \"Nhận định ngắn gọn về màng Lipid/tình trạng da của họ dựa trên thời tiết hôm nay.\",\n" +
                "  \"detailExplanation\": \"Giải thích chi tiết nguyên nhân khoa học liên quan đến các chỉ số ẩm " + hydration + "%, nhạy cảm " + sensitivity + "%, đàn hồi " + elasticity + "%, bã nhờn " + sebum + "% kết hợp với thời tiết nóng ẩm " + temp + "°C, chỉ số UV " + uv + " hoặc bụi mịn " + pm25 + ".\",\n" +
                "  \"moistureVal\": \"" + hydration + "%\",\n" +
                "  \"sensitivityVal\": \"" + sensitivityValStr + "\",\n" +
                "  \"barrierVal\": \"" + barrierValStr + "\",\n" +
                "  \"whyExplanation\": \"Tại sao lộ trình chăm sóc này hiệu quả đối với làn da của " + fullName + " dưới thời tiết hiện tại.\",\n" +
                "  \"recommendedProductIds\": [\"id_sản_phẩm_1\", \"id_sản_phẩm_2\"],\n" +
                "  \"productPhases\": [\"GIAI ĐOẠN 1\", \"GIAI ĐOẠN 2\"],\n" +
                "  \"productSubcategories\": [\"LÀM SẠCH DỊU NHẸ\", \"PHỤC HỒI ĐA TẦNG\"],\n" +
                "  \"productExpertReasons\": [\"Lý do chuyên gia khuyên dùng sản phẩm 1 dưới thời tiết này...\", \"Lý do chuyên gia khuyên dùng sản phẩm 2 dưới thời tiết này...\"]\n" +
                "}\n" +
                "4. Đối với các câu hỏi tư vấn thông thường, giải đáp thành phần hoặc trò chuyện tự do, hãy trả lời bằng văn bản thuần túy, KHÔNG đính kèm JSON.";

        LifecycleOwnerKt.getLifecycleScope(getViewLifecycleOwner()).launchWhenStarted((coroutineScope, continuation) -> {
            return BuildersKt.withContext(Dispatchers.getIO(), (cScope, cCont) -> {
                try {
                    String urlString = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + GEMINI_API_KEY;
                    URL url = new URL(urlString);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Content-Type", "application/json");
                    connection.setConnectTimeout(10000);
                    connection.setReadTimeout(10000);
                    connection.setDoOutput(true);

                    JSONObject requestJson = new JSONObject();
                    JSONArray contentsArray = new JSONArray();
                    
                    List<RootieChatItem> historyItems = chatAdapter.getItems();
                    List<RootieChatItem> activeHistory = new ArrayList<>();
                    for (RootieChatItem item : historyItems) {
                        if (item.getMessageText() == null || !item.getMessageText().equals("Rootie AI đang suy nghĩ...")) {
                            activeHistory.add(item);
                        }
                    }
                    
                    List<RootieChatItem> lastMessages = activeHistory;
                    if (activeHistory.size() > 10) {
                        lastMessages = activeHistory.subList(activeHistory.size() - 10, activeHistory.size());
                    }

                    for (RootieChatItem chatItem : lastMessages) {
                        String role = chatItem.getSender() == RootieChatItem.Sender.USER ? "user" : "model";
                        String text = "";
                        if (chatItem.getType() == RootieChatItem.ItemType.DIAGNOSTIC) {
                            RootieChatItem.DiagnosticData diag = chatItem.getDiagnosticData();
                            if (diag != null) {
                                text = "Đã chẩn đoán da: " + diag.getAssessment() + ". Chi tiết: " + diag.getDetailExplanation();
                            } else {
                                text = chatItem.getMessageText() != null ? chatItem.getMessageText() : "";
                            }
                        } else {
                            text = chatItem.getMessageText() != null ? chatItem.getMessageText() : "";
                        }

                        if (!text.trim().isEmpty()) {
                            JSONArray partsArr = new JSONArray();
                            partsArr.put(new JSONObject().put("text", text));
                            JSONObject contentObj = new JSONObject();
                            contentObj.put("role", role);
                            contentObj.put("parts", partsArr);
                            contentsArray.put(contentObj);
                        }
                    }
                    requestJson.put("contents", contentsArray);

                    JSONObject systemInstruction = new JSONObject();
                    JSONArray systemParts = new JSONArray();
                    systemParts.put(new JSONObject().put("text", systemPrompt));
                    systemInstruction.put("parts", systemParts);
                    requestJson.put("systemInstruction", systemInstruction);

                    JSONObject generationConfig = new JSONObject();
                    generationConfig.put("temperature", 0.7);
                    generationConfig.put("maxOutputTokens", 1500);
                    requestJson.put("generationConfig", generationConfig);

                    byte[] input = requestJson.toString().getBytes("UTF-8");
                    connection.getOutputStream().write(input, 0, input.length);
                    connection.getOutputStream().flush();
                    connection.getOutputStream().close();

                    if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                        BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"));
                        StringBuilder response = new StringBuilder();
                        String responseLine = null;
                        while ((responseLine = br.readLine()) != null) {
                            response.append(responseLine.trim());
                        }
                        
                        JSONObject json = new JSONObject(response.toString());
                        JSONArray candidates = json.getJSONArray("candidates");
                        if (candidates.length() > 0) {
                            JSONObject content = candidates.getJSONObject(0).getJSONObject("content");
                            JSONArray parts = content.getJSONArray("parts");
                            if (parts.length() > 0) {
                                String textResult = parts.getJSONObject(0).getString("text").trim();
                                BuildersKt.withContext(Dispatchers.getMain(), (mScope, mCont) -> {
                                    removeThinkingAndShowResponse(thinkingPos, textResult, skinType);
                                    return kotlin.Unit.INSTANCE;
                                }, cCont);
                                return kotlin.Unit.INSTANCE;
                            }
                        }
                    }

                    BuildersKt.withContext(Dispatchers.getMain(), (mScope, mCont) -> {
                        removeThinkingAndShowFallback(thinkingPos, userMessage, skinType);
                        return kotlin.Unit.INSTANCE;
                    }, cCont);
                } catch (Exception e) {
                    e.printStackTrace();
                    BuildersKt.withContext(Dispatchers.getMain(), (mScope, mCont) -> {
                        removeThinkingAndShowFallback(thinkingPos, userMessage, skinType);
                        return kotlin.Unit.INSTANCE;
                    }, cCont);
                }
                return kotlin.Unit.INSTANCE;
            }, continuation);
        });
    }

    private void removeThinkingAndShowResponse(int thinkingPos, String responseText, String skinType) {
        if (thinkingPos < chatAdapter.getItemCount()) {
            chatAdapter.removeMessageAt(thinkingPos);
        }

        String jsonStr = null;
        String matchedText = null;

        String cleanResponse = responseText.trim();
        if (cleanResponse.startsWith("{") && cleanResponse.endsWith("}")) {
            jsonStr = cleanResponse;
            matchedText = responseText;
        } else {
            Pattern jsonPattern = Pattern.compile("```(?:json)?([\\s\\S]*?)```");
            Matcher matchResult = jsonPattern.matcher(responseText);
            if (matchResult.find()) {
                jsonStr = matchResult.group(1).trim();
                matchedText = matchResult.group(0);
            }
        }

        if (jsonStr != null && matchedText != null) {
            try {
                JSONObject jsonObject = new JSONObject(jsonStr);
                boolean isDiagnostic = jsonObject.optBoolean("is_diagnostic", false);
                if (isDiagnostic) {
                    String assessment = jsonObject.getString("assessment");
                    String detail = jsonObject.getString("detailExplanation");
                    String moisture = jsonObject.getString("moistureVal");
                    String sensitivity = jsonObject.getString("sensitivityVal");
                    String barrier = jsonObject.getString("barrierVal");
                    String why = jsonObject.getString("whyExplanation");

                    JSONArray prodIdsArray = jsonObject.getJSONArray("recommendedProductIds");
                    List<String> prodIds = new ArrayList<>();
                    for (int i = 0; i < prodIdsArray.length(); i++) {
                        prodIds.add(prodIdsArray.getString(i));
                    }

                    JSONArray phasesArray = jsonObject.getJSONArray("productPhases");
                    List<String> phases = new ArrayList<>();
                    for (int i = 0; i < phasesArray.length(); i++) {
                        phases.add(phasesArray.getString(i));
                    }

                    JSONArray subcatsArray = jsonObject.getJSONArray("productSubcategories");
                    List<String> subcats = new ArrayList<>();
                    for (int i = 0; i < subcatsArray.length(); i++) {
                        subcats.add(subcatsArray.getString(i));
                    }

                    JSONArray reasonsArray = jsonObject.getJSONArray("productExpertReasons");
                    List<String> reasons = new ArrayList<>();
                    for (int i = 0; i < reasonsArray.length(); i++) {
                        reasons.add(reasonsArray.getString(i));
                    }

                    RootieChatItem.DiagnosticData diagnosticData = new RootieChatItem.DiagnosticData(
                            assessment, detail, moisture, sensitivity, barrier, why, prodIds, phases, subcats, reasons
                    );

                    String prefixText = responseText.replace(matchedText, "").trim();
                    if (!prefixText.isEmpty()) {
                        chatAdapter.addMessage(new RootieChatItem(
                                RootieChatItem.Sender.AI,
                                prefixText,
                                getCurrentTime(),
                                RootieChatItem.ItemType.TEXT,
                                null
                        ));
                    }

                    RootieChatItem diagnosticItem = new RootieChatItem(
                            RootieChatItem.Sender.AI,
                            null,
                            getCurrentTime(),
                            RootieChatItem.ItemType.DIAGNOSTIC,
                            diagnosticData
                    );
                    chatAdapter.addMessage(diagnosticItem);
                } else {
                    chatAdapter.addMessage(new RootieChatItem(
                            RootieChatItem.Sender.AI,
                            responseText,
                            getCurrentTime(),
                            RootieChatItem.ItemType.TEXT,
                            null
                    ));
                }
            } catch (Exception e) {
                chatAdapter.addMessage(new RootieChatItem(
                        RootieChatItem.Sender.AI,
                        responseText,
                        getCurrentTime(),
                        RootieChatItem.ItemType.TEXT,
                        null
                ));
            }
        } else {
            chatAdapter.addMessage(new RootieChatItem(
                    RootieChatItem.Sender.AI,
                    responseText,
                    getCurrentTime(),
                    RootieChatItem.ItemType.TEXT,
                    null
            ));
        }
        binding.rvChatList.scrollToPosition(chatAdapter.getItemCount() - 1);
        ChatHistoryHelper.saveChatHistory(requireContext(), chatAdapter.getItems());
    }

    private void removeThinkingAndShowFallback(int thinkingPos, String userMessage, String skinType) {
        if (thinkingPos < chatAdapter.getItemCount()) {
            chatAdapter.removeMessageAt(thinkingPos);
        }
        handleFallbackReply(userMessage, skinType);
    }

    private void handleFallbackReply(String userMessage, String skinType) {
        Context context = requireContext();
        SharedPreferences prefs = context.getSharedPreferences("RootieQuizPrefs", Context.MODE_PRIVATE);
        String fullName = ProfileSession.getFullName(context);
        if (fullName == null) fullName = "bạn";
        
        Set<String> flaggedGroups = prefs.getStringSet("SAVED_FLAGGED_GROUPS", new HashSet<>());
        int sensitivity = prefs.getInt("SAVED_SENSITIVITY", 50);
        int hydration = prefs.getInt("SAVED_HYDRATION", 50);
        int elasticity = prefs.getInt("SAVED_ELASTICITY", 75);
        int sebum = prefs.getInt("SAVED_SEBUM", 50);
        String query = userMessage.toLowerCase().trim();

        float temp = prefs.getFloat("SAVED_WEATHER_TEMP", 32.0f);
        int humidityVal = prefs.getInt("SAVED_WEATHER_HUMIDITY", 68);
        float uv = prefs.getFloat("SAVED_WEATHER_UV", 9.2f);
        int pm25 = prefs.getInt("SAVED_WEATHER_PM25", 55);
        String city = prefs.getString("SAVED_WEATHER_CITY", "Thành phố Hồ Chí Minh");
        String weatherCondition = prefs.getString("SAVED_WEATHER_CONDITION", "NẮNG NHIỀU, OI NHẸ");

        String replyText = "";
        boolean isDiagnostic = false;

        LocalJsonReader jsonReader = new LocalJsonReader(context);
        List<ProductEntity> localProducts = jsonReader.getAllProducts();
        List<IngredientEntity> allIngredients = jsonReader.getIngredients();
        List<StoreEntity> allStores = jsonReader.getStores();

        if (query.contains("phác đồ") || query.contains("chẩn đoán") || query.contains("routine") || query.contains("lộ trình")) {
            isDiagnostic = true;
            replyText = "";
        } else if (query.contains("spa") || query.contains("đặt lịch") || query.contains("địa chỉ") || query.contains("cửa hàng") || query.contains("chi nhánh") || query.contains("vị trí")) {
            StringBuilder sb = new StringBuilder("📍 **Hệ thống Cửa hàng & Spa chăm sóc da thuần chay của Rootie:**\n\n");
            int count = 0;
            for (StoreEntity s : allStores) {
                if (count >= 3) break;
                sb.append((count + 1)).append(". **").append(s.getTenCuaHang()).append("** (").append(s.getLoaiHinh()).append(")\n");
                sb.append("   • Địa chỉ: ").append(s.getDiaChiDayDu()).append("\n");
                sb.append("   • Số điện thoại: ").append(s.getSoDienThoai()).append("\n");
                sb.append("   • Giờ mở cửa: ").append(s.getMoCua()).append(" - ").append(s.getDongCua()).append("\n");
                sb.append("   • Tiện nghi: ").append(s.getTienNghi().replace(",", ", ")).append("\n");
                sb.append("   • Trạng thái: ").append(s.getTrangThai()).append("\n\n");
                count++;
            }
            if (count > 0) {
                sb.append("💡 Bạn có thể bấm vào chức năng 'Đặt lịch Spa' ngoài màn hình để tiến hành đặt lịch hẹn nhanh nhất.");
            } else {
                sb.append("Rootie Spa có các chi nhánh phục vụ tại TP. Hồ Chí Minh chuyên sâu về chăm sóc và soi da thuần chay. Hãy mở bản đồ trong ứng dụng để tìm cơ sở gần nhất!");
            }
            replyText = sb.toString();
        } else {
            IngredientEntity matchedIng = null;
            for (IngredientEntity ing : allIngredients) {
                if (query.contains(ing.getName().toLowerCase()) || (!ing.getScientificName().isEmpty() && query.contains(ing.getScientificName().toLowerCase()))) {
                    matchedIng = ing;
                    break;
                }
            }

            if (matchedIng != null) {
                List<ProductEntity> matchedProds = new ArrayList<>();
                for (ProductEntity p : localProducts) {
                    if (p.getName().toLowerCase().contains(matchedIng.getName().toLowerCase()) ||
                            p.getMainIngredientsSummary().toLowerCase().contains(matchedIng.getName().toLowerCase()) ||
                            p.getDescription().toLowerCase().contains(matchedIng.getName().toLowerCase())) {
                        matchedProds.add(p);
                        if (matchedProds.size() >= 2) break;
                    }
                }

                String prodRecommendation = "";
                if (!matchedProds.isEmpty()) {
                    StringBuilder sb = new StringBuilder("\n\n🛍️ **Sản phẩm Rootie chứa thành phần này:**\n");
                    for (ProductEntity p : matchedProds) {
                        String desc = p.getDescription().length() > 100 ? p.getDescription().substring(0, 100) + "..." : p.getDescription();
                        sb.append("- **").append(p.getName()).append("** (").append(NumberFormat.getNumberInstance(Locale.US).format(p.getPrice())).append("đ): ").append(desc).append("\n");
                    }
                    prodRecommendation = sb.toString();
                }

                replyText = "🌿 **Kiến thức nguyên liệu Rootie AI:**\n" +
                        "• **Thành phần**: " + matchedIng.getName() + " (" + matchedIng.getScientificName() + ")\n" +
                        "• **Công dụng chính**: " + matchedIng.getUses() + "\n" +
                        "• **Chi tiết**: " + matchedIng.getDescription() + prodRecommendation;
            } else if (query.contains("sản phẩm") || query.contains("mỹ phẩm") || query.contains("mã") || query.contains("bán") || query.contains("mua") ||
                    query.contains("rửa mặt") || query.contains("chống nắng") || query.contains("toner") || query.contains("nước hoa hồng") || query.contains("tẩy trang") || query.contains("serum") || query.contains("thạch nghệ")) {

                String keyword = null;
                if (query.contains("bí đao")) keyword = "bí đao";
                else if (query.contains("nghệ")) keyword = "nghệ";
                else if (query.contains("hoa hồng")) keyword = "hoa hồng";
                else if (query.contains("cà phê")) keyword = "cà phê";
                else if (query.contains("rửa mặt")) keyword = "rửa mặt";
                else if (query.contains("chống nắng")) keyword = "chống nắng";
                else if (query.contains("toner") || query.contains("hoa hồng")) keyword = "hoa hồng";
                else if (query.contains("tẩy trang")) keyword = "tẩy trang";
                else if (query.contains("serum") || query.contains("tinh chất")) keyword = "tinh chất";
                else if (query.contains("dưỡng ẩm") || query.contains("thạch")) keyword = "thạch";

                List<ProductEntity> filteredProds = new ArrayList<>();
                if (keyword != null) {
                    for (ProductEntity p : localProducts) {
                        if (p.getName().toLowerCase().contains(keyword) || p.getCategory().toLowerCase().contains(keyword)) {
                            filteredProds.add(p);
                        }
                    }
                } else {
                    String typeKey = "nghệ";
                    if (skinType.toLowerCase().contains("dầu") || skinType.toLowerCase().contains("hỗn hợp")) typeKey = "bí đao";
                    else if (skinType.toLowerCase().contains("khô")) typeKey = "hoa hồng";

                    for (ProductEntity p : localProducts) {
                        if (p.getName().toLowerCase().contains(typeKey)) {
                            filteredProds.add(p);
                        }
                    }
                }

                StringBuilder sb = new StringBuilder("🛍️ **Danh sách sản phẩm Rootie đề xuất cho bạn:**\n\n");
                int count = 0;
                for (ProductEntity p : filteredProds) {
                    if (count >= 3) break;
                    String formattedPrice = NumberFormat.getNumberInstance(Locale.US).format(p.getPrice());
                    sb.append((count + 1)).append(". **").append(p.getName()).append("**\n");
                    sb.append("   • Giá: ").append(formattedPrice).append("đ | Loại: ").append(p.getCategory()).append("\n");
                    sb.append("   • Thành phần chính: ").append(p.getMainIngredientsSummary()).append("\n");

                    boolean containsAllergen = false;
                    for (String allergen : flaggedGroups) {
                        boolean foundInDetailed = false;
                        for (String ing : p.getDetailedIngredients()) {
                            if (ing.toLowerCase().contains(allergen.toLowerCase())) {
                                foundInDetailed = true;
                                break;
                            }
                        }
                        if (foundInDetailed || p.getAllergyInformation().toLowerCase().contains(allergen.toLowerCase()) || p.getMainIngredientsSummary().toLowerCase().contains(allergen.toLowerCase())) {
                            containsAllergen = true;
                            break;
                        }
                    }

                    if (containsAllergen) {
                        sb.append("   • ⚠️ *Cảnh báo*: Sản phẩm chứa thành phần nhạy cảm với da bạn!\n");
                    } else {
                        sb.append("   • ✅ Phù hợp: ").append(p.getSuitableFor()).append("\n");
                    }
                    sb.append("\n");
                    count++;
                }

                if (count > 0) {
                    sb.append("💡 Bạn có thể tìm thấy các sản phẩm trên trực tiếp tại Cửa hàng của Rootie để được tư vấn soi da miễn phí.");
                } else {
                    sb.append("Hiện tại Rootie đang cung cấp các dòng sản phẩm thuần chay 100% từ Bí Đao (ngừa mụn), Nghệ Hưng Yên (mờ thâm), và Hoa Hồng Cao Bằng (cấp ẩm). Vui lòng nói rõ hơn dòng sản phẩm bạn cần tìm nhé!");
                }
                replyText = sb.toString();

            } else if (query.contains("thời tiết") || query.contains("hôm nay") || query.contains("nhiệt độ") || query.contains("bụi") || query.contains("nắng") || query.contains("uv") || query.contains("weather")) {
                String matchedWeatherName = "Nắng ấm dễ chịu";
                String matchedWeatherDesc = "Thời tiết ôn hòa, phù hợp mọi loại da";
                String matchedWeatherIcon = "🌤️";
                try {
                    BufferedReader br = new BufferedReader(new InputStreamReader(context.getAssets().open("weathers.json")));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line);
                    br.close();
                    JSONObject root = new JSONObject(sb.toString());
                    JSONArray weathersArr = root.getJSONArray("weathers");
                    for (int i = 0; i < weathersArr.length(); i++) {
                        JSONObject w = weathersArr.getJSONObject(i);
                        JSONObject tempRange = w.getJSONObject("temperature_range");
                        JSONObject humRange = w.getJSONObject("humidity_range");
                        double minT = tempRange.getDouble("min");
                        double maxT = tempRange.getDouble("max");
                        double minH = humRange.getDouble("min");
                        double maxH = humRange.getDouble("max");
                        if (temp >= minT && temp <= maxT && humidityVal >= minH && humidityVal <= maxH) {
                            matchedWeatherName = w.getString("name");
                            matchedWeatherDesc = w.getString("description");
                            matchedWeatherIcon = w.getString("icon");
                            break;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                String uvLevelStr = uv < 3 ? "Thấp (An toàn)" : (uv < 6 ? "Trung bình (Cần che chắn)" : (uv < 8 ? "Cao (Nguy cơ gây hại)" : "Rất cao \uD83D\uDEA8 (Nguy hiểm cho da)"));
                String dustLevelStr = pm25 < 25 ? "Tốt (Không khí sạch)" : (pm25 < 50 ? "Trung bình" : "Kém ⚠️ (Dễ gây bít tắc lỗ chân lông)");

                String weatherImpact = "";
                if (skinType.toLowerCase().contains("dầu") || skinType.toLowerCase().contains("hỗn hợp")) {
                    weatherImpact = "Thời tiết '" + matchedWeatherName + "' (" + temp + "°C) làm tăng lượng dầu tiết ra (" + sebum + "%), dễ gây bít tắc ở vùng chữ T. Rootie AI khuyên bạn dùng Gel rửa mặt Bí Đao và thoa serum Niacinamide mỏng nhẹ.";
                } else if (skinType.toLowerCase().contains("khô")) {
                    weatherImpact = "Không khí có độ ẩm " + humidityVal + "%. Làn da khô của bạn đang có độ ẩm khá thấp (" + hydration + "%). Hãy cấp ẩm tầng sâu bằng Thạch hoa hồng hữu cơ Cao Bằng để tránh bong tróc và thô ráp.";
                } else {
                    weatherImpact = "Nền da nhạy cảm (" + sensitivity + "%) của bạn rất dễ bị tổn thương bởi tia UV hôm nay đang ở mức " + uv + " (" + uvLevelStr + "). Bụi mịn PM2.5 là " + pm25 + " μg/m³ (" + dustLevelStr + "). Hãy thoa Sữa chống nắng Bí Đao và che chắn thật kỹ.";
                }

                replyText = "⛅ **Dự báo thời tiết & Phân tích da cùng Rootie:**\n" +
                        "• **Vị trí**: " + city + " | **Thời tiết**: " + temp + "°C (" + weatherCondition + ") " + matchedWeatherIcon + "\n" +
                        "• **Trạng thái**: " + matchedWeatherName + " - " + matchedWeatherDesc + "\n" +
                        "• **Chỉ số UV**: " + String.format(Locale.US, "%.1f", uv) + " (" + uvLevelStr + ")\n" +
                        "• **Bụi mịn PM2.5**: " + pm25 + " μg/m³ (" + dustLevelStr + ")\n\n" +
                        "🎯 **Lời khuyên cá nhân hóa cho da " + skinType + " của " + fullName + ":**\n" +
                        weatherImpact + "\n\n" +
                        "💡 *Mẹo:* Gửi tin nhắn 'routine' để nhận ngay phác đồ các sản phẩm thuần chay tối ưu nhất cho ngày hôm nay!";
            } else if (query.contains("mụn") || query.contains("thâm") || query.contains("viêm")) {
                replyText = "Chào " + fullName + ", đối với da bị mụn và thâm, Rootie khuyên bạn nên:\n" +
                        "1. Làm sạch sâu nhẹ nhàng với **Gel rửa mặt Bí đao** giúp ngừa khuẩn mụn sưng đỏ.\n" +
                        "2. Thoa **Tinh chất Bí đao N15** chứa Niacinamide và Tràm trà giúp gom cồi mụn ẩn nhanh chóng.\n" +
                        "3. Dùng **Tinh chất Nghệ Hưng Yên C10** làm mờ vết thâm sau khi mụn đã gom khô cồi.\n\n" +
                        "Lưu ý: Không tự ý nặn các nốt mụn viêm đang sưng to để tránh để lại sẹo lõm bạn nhé!";
            } else if (query.contains("dầu") || query.contains("nhờn") || query.contains("lỗ chân lông") || query.contains("bít tắc")) {
                replyText = "Lượng bã nhờn hiện tại của bạn là " + sebum + "% (khá cao). Để điều tiết lượng dầu thừa:\n" +
                        "1. Cấp đủ nước: Thiếu nước sẽ kích thích da đổ dầu bù đắp. Hãy thoa **Toner Bí Đao** cấp nước mát da.\n" +
                        "2. Rửa mặt đúng cách: Không dùng sữa rửa mặt chứa chất tẩy tạo bọt mạnh quá 2 lần/ngày.\n" +
                        "3. Sử dụng tinh chất chứa chiết xuất Bí Đao giúp kháng viêm và se khít lỗ chân lông hiệu quả.";
            } else if (query.contains("khô") || query.contains("bong tróc") || query.contains("căng")) {
                replyText = "Độ ẩm da hiện tại của bạn là " + hydration + "%. Với mức ẩm thấp này, da rất dễ xuất hiện nếp nhăn và khô sạm. Hãy:\n" +
                        "1. Bổ sung ngay nước hoa hồng cấp ẩm dạng xịt/vỗ sau khi rửa mặt.\n" +
                        "2. Dưỡng khóa ẩm sâu bằng **Thạch hoa hồng hữu cơ Cao Bằng** giúp duy trì độ ẩm kéo dài.\n" +
                        "3. Tránh rửa mặt bằng nước quá nóng vì sẽ làm mất đi lớp dầu tự nhiên bảo vệ da.";
            } else if (query.contains("nhạy cảm") || query.contains("đỏ") || query.contains("kích ứng") || query.contains("rộp")) {
                String allergyMsg = flaggedGroups.isEmpty() ? "Nên tránh cồn khô và hương liệu nhân tạo." : "⚠️ Hãy tránh tuyệt đối các thành phần: " + String.join(", ", flaggedGroups) + ".";
                replyText = "Da bạn có độ nhạy cảm cao (" + sensitivity + "%). " + allergyMsg + "\n" +
                        "Rootie khuyên bạn nên sử dụng **Gel rửa mặt Hoa Hồng** làm sạch dịu nhẹ kết hợp với **Thạch Hoa Hồng hữu cơ** để củng cố hàng rào bảo vệ da (" + elasticity + "% đàn hồi) đang mỏng yếu.";
            } else if (query.contains("code") || query.contains("lập trình") || query.contains("python") || query.contains("java") || query.contains("kotlin") || query.contains("html") || query.contains("javascript")) {
                replyText = "💻 **Trợ lý lập trình Rootie (Offline):**\n\n" +
                        "Hệ thống AI hiện đang chạy ở chế độ offline (Chưa cấu hình API Key). Dưới đây là mẫu hàm đệ quy Fibonacci trong Python:\n" +
                        "```python\n" +
                        "def fibonacci(n):\n" +
                        "    if n <= 1:\n" +
                        "        return n\n" +
                        "    return fibonacci(n-1) + fibonacci(n-2)\n" +
                        "```\n\n" +
                        "💡 *Mẹo:* Bạn chỉ cần thêm API Key thực tế từ Google AI Studio vào `local.properties` (dòng `gemini.api.key=...`) để kích hoạt Gemini 1.5 cực kỳ thông minh hỗ trợ code nâng cao!";
            } else if (query.contains("dịch") || query.contains("translate") || query.contains("tiếng anh") || query.contains("tiếng việt")) {
                replyText = "🌐 **Dịch thuật Rootie (Offline):**\n\n" +
                        "Tôi dịch nhanh một số thuật ngữ da liễu cho bạn:\n" +
                        "- *Acne-prone skin* ➔ Da dễ nổi mụn\n" +
                        "- *Dehydrated skin* ➔ Da thiếu nước\n" +
                        "- *Moisturizer* ➔ Kem dưỡng ẩm\n" +
                        "- *Double cleansing* ➔ Làm sạch kép (Tẩy trang + Rửa mặt)\n\n" +
                        "💡 Hãy thêm API Key để dịch tự do các đoạn văn bản dài và đa ngôn ngữ!";
            } else if (query.contains("chào") || query.contains("hi") || query.contains("hello") || query.contains("xin chào")) {
                replyText = "Chào " + fullName + "! Mình là Rootie AI, chuyên gia tư vấn da liễu thuần chay của bạn. Hôm nay, làn da của bạn đang có độ ẩm " + hydration + "%, bã nhờn " + sebum + "% và độ nhạy cảm " + sensitivity + "%.\n\nMình có thể giúp bạn kiểm tra sản phẩm, thiết kế lộ trình (hãy nhắn 'routine'), tư vấn về các nguyên liệu thiên nhiên hoặc các spa của Rootie. Bạn hãy hỏi nhé!";
            } else if (query.contains("bạn là ai") || query.contains("tên gì") || query.contains("rootie")) {
                replyText = "Mình là Rootie AI - trợ lý ảo tư vấn chăm sóc da thuần chay. Nhiệm vụ của mình là hỗ trợ bạn phân tích các chỉ số da, đề xuất lộ trình chăm sóc phù hợp với thời tiết ngày hôm nay, và hướng dẫn bạn chọn sản phẩm thuần chay tối ưu nhất.";
            } else {
                replyText = "Cảm ơn câu hỏi của " + fullName + ". Mình ghi nhận làn da " + skinType + " của bạn đang có Độ ẩm " + hydration + "%, Nhạy cảm " + sensitivity + "% và Bã nhờn " + sebum + "%. Để trả lời chi tiết và đa nhiệm nhất về câu hỏi này, bạn hãy cấu hình API Key Gemini của Google nhé.\n\n" +
                        "💡 *Mẹo:* Nhập API Key của bạn vào dòng `gemini.api.key=...` trong tệp `local.properties` để kích hoạt bộ não thông minh vượt trội của Rootie AI!";
            }
        }

        if (isDiagnostic) {
            RootieChatItem.DiagnosticData diagData = generateRuleBasedDiagnostic(skinType);
            RootieChatItem diagnosticItem = new RootieChatItem(
                    RootieChatItem.Sender.AI,
                    null,
                    getCurrentTime(),
                    RootieChatItem.ItemType.DIAGNOSTIC,
                    diagData
            );
            chatAdapter.addMessage(diagnosticItem);
        } else {
            RootieChatItem textItem = new RootieChatItem(
                    RootieChatItem.Sender.AI,
                    replyText,
                    getCurrentTime(),
                    RootieChatItem.ItemType.TEXT,
                    null
            );
            chatAdapter.addMessage(textItem);
        }

        binding.rvChatList.scrollToPosition(chatAdapter.getItemCount() - 1);
        ChatHistoryHelper.saveChatHistory(context, chatAdapter.getItems());
    }

    private RootieChatItem.DiagnosticData generateRuleBasedDiagnostic(String skinType) {
        Context context = requireContext();
        SharedPreferences prefs = context.getSharedPreferences("RootieQuizPrefs", Context.MODE_PRIVATE);
        int sensitivity = prefs.getInt("SAVED_SENSITIVITY", 50);
        int hydration = prefs.getInt("SAVED_HYDRATION", 50);
        int elasticity = prefs.getInt("SAVED_ELASTICITY", 75);
        int sebum = prefs.getInt("SAVED_SEBUM", 50);
        String fullName = ProfileSession.getFullName(context);
        if (fullName == null) fullName = "bạn";

        float temp = prefs.getFloat("SAVED_WEATHER_TEMP", 32.0f);
        int humidityVal = prefs.getInt("SAVED_WEATHER_HUMIDITY", 68);
        float uv = prefs.getFloat("SAVED_WEATHER_UV", 9.2f);
        int pm25 = prefs.getInt("SAVED_WEATHER_PM25", 55);
        String city = prefs.getString("SAVED_WEATHER_CITY", "Thành phố Hồ Chí Minh");
        String weatherCondition = prefs.getString("SAVED_WEATHER_CONDITION", "NẮNG NHIỀU, OI NHẸ");

        String moistureStr = hydration + "%";
        String sensitivityStr = sensitivity >= 70 ? "Rất Cao" : (sensitivity >= 40 ? "Trung Bình" : "Thấp");
        String barrierStr = elasticity >= 75 ? "Khỏe" : (elasticity >= 50 ? "Ổn định" : "Yếu");

        String matchedWeatherName = "Nắng ấm dễ chịu";
        String matchedWeatherIcon = "🌤️";
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(context.getAssets().open("weathers.json")));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            br.close();
            JSONObject root = new JSONObject(sb.toString());
            JSONArray weathersArr = root.getJSONArray("weathers");
            for (int i = 0; i < weathersArr.length(); i++) {
                JSONObject w = weathersArr.getJSONObject(i);
                JSONObject tempRange = w.getJSONObject("temperature_range");
                JSONObject humRange = w.getJSONObject("humidity_range");
                double minT = tempRange.getDouble("min");
                double maxT = tempRange.getDouble("max");
                double minH = humRange.getDouble("min");
                double maxH = humRange.getDouble("max");
                if (temp >= minT && temp <= maxT && humidityVal >= minH && humidityVal <= maxH) {
                    matchedWeatherName = w.getString("name");
                    matchedWeatherIcon = w.getString("icon");
                    break;
                }
            }
        } catch (Exception e) { e.printStackTrace(); }

        String weatherInfo = "Thời tiết hôm nay: " + matchedWeatherName + " " + matchedWeatherIcon + " (" + temp + "°C, độ ẩm " + humidityVal + "%, UV " + String.format(Locale.US, "%.1f", uv) + ", bụi mịn PM2.5 là " + pm25 + ").";

        if (skinType.toLowerCase().contains("dầu") || skinType.toLowerCase().contains("hỗn hợp")) {
            List<String> recommendedIds = uv >= 6.0 ? Arrays.asList("5f29c9fa19873eb44aedced4", "641cdf538114d9cb102d9ab2", "1e499ed75a31e4a02af2d962") : Arrays.asList("5f29c9fa19873eb44aedced4", "641cdf538114d9cb102d9ab2", "113023f9cf480dbe4182e96c");
            List<String> phases = uv >= 6.0 ? Arrays.asList("GIAI ĐOẠN 1: LÀM SẠCH", "GIAI ĐOẠN 2: KIỀM DẦU", "GIAI ĐOẠN 3: BẢO VỆ NẮNG") : Arrays.asList("GIAI ĐOẠN 1: LÀM SẠCH", "GIAI ĐOẠN 2: KIỀM DẦU", "GIAI ĐOẠN 3: KHÓA ẨM DỊU NHẸ");
            List<String> subcats = uv >= 6.0 ? Arrays.asList("SẠCH SÂU BÃ NHỜN", "ĐIỀU TIẾT DẦU MỤN", "CHỐNG UV GAY GẮT") : Arrays.asList("SẠCH SÂU BÃ NHỜN", "ĐIỀU TIẾT DẦU MỤN", "KHÓA NƯỚC DỊU MÁT");
            List<String> reasons = uv >= 6.0 ? Arrays.asList(
                    "Rửa mặt bằng Gel Bí Đao làm sạch dầu nhờn ở mức " + sebum + "% mà không làm khô căng da.",
                    "Tinh chất Bí Đao N15 với Niacinamide điều tiết bã nhờn trong thời tiết nóng ẩm " + temp + "°C.",
                    "Thời tiết UV đạt " + String.format(Locale.US, "%.1f", uv) + " (mức nguy cơ), bắt buộc dùng Sữa chống nắng Bí Đao để bảo vệ màng da."
            ) : Arrays.asList(
                    "Gel rửa mặt Bí Đao giữ ẩm ở mức " + moistureStr + " và loại bỏ bã nhờn vùng chữ T.",
                    "Tinh chất Bí Đao N15 giúp giảm mụn sưng viêm cực tốt dưới thời tiết " + weatherCondition + ".",
                    "Thạch bí đao mỏng nhẹ cấp ẩm cho da khô mất nước do ngồi máy lạnh dưới trời oi nóng " + temp + "°C."
            );

            return new RootieChatItem.DiagnosticData(
                    "Rootie nhận định: Da " + skinType + " của bạn đang chịu áp lực lớn dưới thời tiết " + temp + "°C của " + city + ".",
                    weatherInfo + " Với tình trạng da của " + fullName + " (độ ẩm " + moistureStr + ", độ dầu " + sebum + "%), thời tiết này làm tăng tốc độ bài tiết bã nhờn, dễ gây bít tắc lỗ chân lông gây mụn.",
                    moistureStr, sensitivityStr, barrierStr,
                    "Routine này tối ưu hóa khả năng kiểm soát bã nhờn (" + sebum + "%) bằng Bí đao tự nhiên, đồng thời bảo vệ da khỏi tia UV " + String.format(Locale.US, "%.1f", uv) + " nguy hại hôm nay.",
                    recommendedIds, phases, subcats, reasons
            );
        } else if (skinType.toLowerCase().contains("khô")) {
            List<String> recommendedIds = uv >= 6.0 ? Arrays.asList("7df6fb8720d3cc5566f0c4ca", "fdc1ca708d8cf2225c9d9697", "1e499ed75a31e4a02af2d962") : Arrays.asList("7df6fb8720d3cc5566f0c4ca", "fdc1ca708d8cf2225c9d9697", "ca192eb70b03e780dc19d872");
            List<String> phases = uv >= 6.0 ? Arrays.asList("GIAI ĐOẠN 1: LÀM SẠCH", "GIAI ĐOẠN 2: DƯỠNG ẨM SÂU", "GIAI ĐOẠN 3: BẢO VỆ UV") : Arrays.asList("GIAI ĐOẠN 1: LÀM SẠCH", "GIAI ĐOẠN 2: DƯỠNG ẨM SÂU", "GIAI ĐOẠN 3: KHÓA ẨM DÀY");
            List<String> subcats = uv >= 6.0 ? Arrays.asList("SẠCH DỊU NHẸ", "CẤP NƯỚC HOA HỒNG", "CHỐNG NẮNG BẢO VỆ") : Arrays.asList("SẠCH DỊU NHẸ", "CẤP NƯỚC HOA HỒNG", "KHÓA ẨM BIỂU BÌ");
            List<String> reasons = uv >= 6.0 ? Arrays.asList(
                    "Gel rửa mặt Hoa Hồng giúp bảo vệ lớp màng Lipid đang thiếu nước ẩm (" + moistureStr + ").",
                    "Thạch Hoa Hồng cấp ẩm và bù đắp nước nhanh chóng khi độ ẩm không khí thấp (" + humidityVal + "%).",
                    "Tia UV hôm nay đạt mức nguy hại " + String.format(Locale.US, "%.1f", uv) + ", bắt buộc thoa Sữa chống nắng Bí Đao để ngăn ngừa sạm nám."
            ) : Arrays.asList(
                    "Gel rửa mặt Hoa Hồng làm sạch nhẹ nhàng và giữ ẩm lý tưởng cho làn da khô ráp.",
                    "Thạch Hoa Hồng cấp ẩm sâu đưa độ ẩm từ " + moistureStr + " lên mức tối ưu.",
                    "Thạch hoa hồng 30ml nhỏ gọn dùng khóa ẩm tăng cường độ đàn hồi biểu bì (" + barrierStr + ")."
            );

            return new RootieChatItem.DiagnosticData(
                    "Rootie nhận định: Da khô của bạn đang mất nước nghiêm trọng (" + moistureStr + ") dưới nhiệt độ " + temp + "°C.",
                    weatherInfo + " Độ ẩm không khí chỉ đạt " + humidityVal + "%, làm đẩy nhanh hiện tượng mất nước qua biểu bì của da khô ráp. Chỉ số đàn hồi da hiện tại là " + elasticity + "%.",
                    moistureStr, sensitivityStr, barrierStr,
                    "Sử dụng tinh chất và thạch hoa hồng hữu cơ giúp bổ sung độ ẩm tầng sâu, tạo màng khóa nước vững chắc ngăn bong tróc da.",
                    recommendedIds, phases, subcats, reasons
            );
        } else {
            List<String> recommendedIds = uv >= 6.0 ? Arrays.asList("13afaa472ab5642f72112123", "c4b7cebcaf1a27611a9395af", "1e499ed75a31e4a02af2d962") : Arrays.asList("13afaa472ab5642f72112123", "c4b7cebcaf1a27611a9395af", "fdc1ca708d8cf2225c9d9697");
            List<String> phases = uv >= 6.0 ? Arrays.asList("GIAI ĐOẠN 1: LÀM SẠCH", "GIAI ĐOẠN 2: DƯỠNG SÁNG", "GIAI ĐOẠN 3: BẢO VỆ UV") : Arrays.asList("GIAI ĐOẠN 1: LÀM SẠCH", "GIAI ĐOẠN 2: DƯỠNG SÁNG", "GIAI ĐOẠN 3: PHỤC HỒI DỊU NHẸ");
            List<String> subcats = uv >= 6.0 ? Arrays.asList("SẠCH MỊN NGHỆ", "TINH CHẤT SÁNG DA", "CHỐNG UV GAY GẮT") : Arrays.asList("SẠCH MỊN NGHỆ", "TINH CHẤT SÁNG DA", "KHÓA ẨM CHỮA LÀNH");
            List<String> reasons = uv >= 6.0 ? Arrays.asList(
                    "Sữa rửa mặt Nghệ Hưng Yên làm sạch sâu tế bào xỉn màu dưới trời nắng " + weatherCondition + ".",
                    "Tinh chất Nghệ Hưng Yên C22 làm sáng da và chống oxy hóa mạnh mẽ.",
                    "Tia UV " + String.format(Locale.US, "%.1f", uv) + " cực kỳ độc hại cho da nhạy cảm mỏng yếu, bắt buộc che chắn và bôi KCN."
            ) : Arrays.asList(
                    "Sữa rửa mặt Nghệ làm sạch tế bào chết dịu nhẹ mà không gây kích ứng da.",
                    "Tinh chất Nghệ Hưng Yên giúp mờ thâm và tăng cường sức đề kháng da.",
                    "Thạch Hoa Hồng phục hồi hàng rào ẩm (" + barrierStr + ") và làm dịu những vùng da mẩn đỏ dưới trời " + temp + "°C."
            );

            return new RootieChatItem.DiagnosticData(
                    "Rootie nhận định: Làn da nhạy cảm của bạn cần bảo vệ và phục hồi tích cực dưới tia UV " + uv + " hôm nay.",
                    weatherInfo + " Chỉ số nhạy cảm của bạn đang ở mức " + sensitivity + "% (" + sensitivityStr + "). Tia UV cao và khói bụi mịn " + pm25 + " dễ làm da bị kích ứng đỏ rát.",
                    moistureStr, sensitivityStr, barrierStr,
                    "Sử dụng curcumin từ nghệ và thạch phục hồi giúp làm dịu kích ứng tức thì, đẩy lùi tổn thương gốc tự do gây ra bởi tia UV.",
                    recommendedIds, phases, subcats, reasons
            );
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (!getShowsDialog()) {
            if (getActivity() != null) {
                SharedPreferences prefs = getActivity().getSharedPreferences("RootieQuizPrefs", Context.MODE_PRIVATE);
                boolean enabled = prefs.getBoolean("SKIN_AI_FLOATING_CHAT_ENABLED", true);
                if (enabled) {
                    View floatingHead = getActivity().findViewById(R.id.skin_ai_floating_chat_head);
                    if (floatingHead != null) {
                        floatingHead.setVisibility(View.VISIBLE);
                    }
                }
            }
        }
        binding = null;
    }
}
