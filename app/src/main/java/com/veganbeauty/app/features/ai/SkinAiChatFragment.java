package com.veganbeauty.app.features.ai;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwnerKt;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.veganbeauty.app.R;
import com.veganbeauty.app.core.base.RootieFragment;
import com.veganbeauty.app.data.local.ProfileSession;
import com.veganbeauty.app.databinding.SkinAiChatBinding;
import com.veganbeauty.app.features.ai.RootieChatAdapter.RootieChatItem;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class SkinAiChatFragment extends RootieFragment {

    private SkinAiChatBinding binding;
    private RootieChatAdapter chatAdapter;
    private boolean isFullScreen = false;

    public static SkinAiChatFragment newInstance(boolean fullScreen) {
        SkinAiChatFragment fragment = new SkinAiChatFragment();
        Bundle args = new Bundle();
        args.putBoolean("FULL_SCREEN", fullScreen);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            isFullScreen = getArguments().getBoolean("FULL_SCREEN", false);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = SkinAiChatBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    protected void setupUI(@NonNull View view) {
        if (isFullScreen) {
            view.setBackgroundColor(Color.WHITE);
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
    }

    private void setupListeners() {
        binding.btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        binding.btnSend.setOnClickListener(v -> {
            String text = binding.etMessageInput.getText().toString().trim();
            if (!text.isEmpty()) {
                handleUserMessage(text);
                binding.etMessageInput.setText("");
            }
        });

        binding.chipWeatherAdvice.setOnClickListener(v -> handleUserMessage("Cho mình xin lời khuyên dưỡng da theo thời tiết hôm nay."));
        binding.chipWeatherRoutine.setOnClickListener(v -> handleUserMessage("Routine chăm sóc da phù hợp với thời tiết này là gì?"));
        binding.chipMatchProducts.setOnClickListener(v -> handleUserMessage("Sản phẩm nào phù hợp với làn da của mình?"));
        binding.chipSkinDiagnosis.setOnClickListener(v -> handleUserMessage("Mình muốn xem phác đồ chẩn đoán da."));
    }

    private void sendInitialGreeting() {
        String skinType = ProfileSession.getSavedUserSkinType(requireContext());
        String timeStr = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());

        RootieChatItem greetingItem = new RootieChatItem(
                UUID.randomUUID().toString(),
                RootieChatItem.Sender.AI,
                "Chào bạn! Mình là Rootie AI, chuyên gia tư vấn da liễu của bạn. Dựa trên thông tin của bạn (" + skinType + "), mình đã có một số đánh giá ban đầu.",
                timeStr,
                RootieChatItem.ItemType.TEXT,
                null
        );
        chatAdapter.addMessage(greetingItem);

        RootieChatItem.DiagnosticData diagnosticData = generateRuleBasedDiagnostic(skinType);
        RootieChatItem diagnosticItem = new RootieChatItem(
                UUID.randomUUID().toString(),
                RootieChatItem.Sender.AI,
                "Bản phân tích da chi tiết của bạn:",
                timeStr,
                RootieChatItem.ItemType.DIAGNOSTIC,
                diagnosticData
        );
        chatAdapter.addMessage(diagnosticItem);
        
        saveChatHistory();
    }

    private void handleUserMessage(String text) {
        String timeStr = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
        RootieChatItem userMsg = new RootieChatItem(
                UUID.randomUUID().toString(),
                RootieChatItem.Sender.USER,
                text,
                timeStr,
                RootieChatItem.ItemType.TEXT,
                null
        );
        chatAdapter.addMessage(userMsg);
        binding.rvChatList.scrollToPosition(chatAdapter.getItemCount() - 1);

        // Simulate AI Response
        binding.rvChatList.postDelayed(() -> {
            RootieChatItem thinkingMsg = new RootieChatItem(
                    UUID.randomUUID().toString(),
                    RootieChatItem.Sender.AI,
                    "Đang phân tích câu hỏi của bạn...",
                    timeStr,
                    RootieChatItem.ItemType.TEXT,
                    null
            );
            chatAdapter.addMessage(thinkingMsg);
            
            binding.rvChatList.postDelayed(() -> {
                if (chatAdapter.getItemCount() > 0) {
                    chatAdapter.removeMessageAt(chatAdapter.getItemCount() - 1);
                }
                RootieChatItem responseMsg = new RootieChatItem(
                        UUID.randomUUID().toString(),
                        RootieChatItem.Sender.AI,
                        "Cảm ơn bạn đã hỏi. Đối với vấn đề '" + text + "', mình khuyên bạn nên tập trung vào việc cấp ẩm và phục hồi hàng rào bảo vệ da.",
                        timeStr,
                        RootieChatItem.ItemType.TEXT,
                        null
                );
                chatAdapter.addMessage(responseMsg);
                binding.rvChatList.scrollToPosition(chatAdapter.getItemCount() - 1);
                saveChatHistory();
            }, 2000);
        }, 500);
    }

    private void saveChatHistory() {
        ChatHistoryHelper.saveChatHistory(requireContext(), chatAdapter.getItems());
    }

    private RootieChatItem.DiagnosticData generateRuleBasedDiagnostic(String skinType) {
        List<String> products = new ArrayList<>();
        products.add("0b8fadbc1bd44562f75704e6");
        products.add("dd23909f6a123054c8cf62f4");

        List<String> phases = new ArrayList<>();
        phases.add("Làm sạch");
        phases.add("Dưỡng ẩm");

        List<String> subcats = new ArrayList<>();
        subcats.add("Tẩy trang");
        subcats.add("Kem dưỡng");

        List<String> reasons = new ArrayList<>();
        reasons.add("Dịu nhẹ cho da");
        reasons.add("Cấp ẩm sâu");

        return new RootieChatItem.DiagnosticData(
                "Tình trạng da: " + skinType,
                "Da bạn đang gặp vấn đề về độ ẩm và cần được chăm sóc kỹ lưỡng hơn.",
                "35%",
                "Nhạy cảm nhẹ",
                "Cần phục hồi",
                "Do tác động từ môi trường và quy trình làm sạch chưa tối ưu.",
                products,
                phases,
                subcats,
                reasons
        );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
