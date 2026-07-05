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
import androidx.fragment.app.DialogFragment;
import com.veganbeauty.app.data.local.ProfileSession;
import com.veganbeauty.app.databinding.SkinAiChatBinding;
import com.veganbeauty.app.features.ai.RootieChatAdapter.RootieChatItem;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

public class SkinAiChatFragment extends DialogFragment {

    private SkinAiChatBinding binding;
    private RootieChatAdapter chatAdapter;
    private boolean isFullScreen = false;

    private final ActivityResultLauncher<String> pickFileLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    String fileName = getFileName(uri);
                    handleUserMessage("Đã đính kèm tệp: " + fileName);
                }
            }
    );

    private final ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    handleUserMessage("image:" + uri.toString());
                }
            }
    );

    private final ActivityResultLauncher<Void> takePhotoLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicturePreview(), bitmap -> {
                if (bitmap != null) {
                    String uriStr = saveBitmapToCache(bitmap);
                    if (uriStr != null) {
                        handleUserMessage("image:" + uriStr);
                    }
                }
            }
    );

    private String saveBitmapToCache(android.graphics.Bitmap bitmap) {
        try {
            java.io.File cachePath = new java.io.File(requireContext().getCacheDir(), "images");
            cachePath.mkdirs();
            java.io.File file = new java.io.File(cachePath, "image_" + System.currentTimeMillis() + ".jpg");
            java.io.FileOutputStream stream = new java.io.FileOutputStream(file);
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 100, stream);
            stream.close();
            return android.net.Uri.fromFile(file).toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private final ActivityResultLauncher<String> requestCameraPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    try {
                        takePhotoLauncher.launch(null);
                    } catch (Exception e) {
                        Toast.makeText(requireContext(), "Không thể mở camera", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(requireContext(), "Cần cấp quyền camera để chụp ảnh", Toast.LENGTH_SHORT).show();
                }
            }
    );

    private String getFileName(android.net.Uri uri) {
        String result = null;
        if ("content".equals(uri.getScheme())) {
            android.database.Cursor cursor = requireContext().getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (index != -1) {
                        result = cursor.getString(index);
                    }
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    private void updateInputIconsVisibility() {
        if (binding == null) return;
        boolean hasFocus = binding.etMessageInput.hasFocus();
        boolean hasText = binding.etMessageInput.getText().length() > 0;
        int visibility = (hasFocus || hasText) ? View.GONE : View.VISIBLE;
        binding.btnCamera.setVisibility(visibility);
        binding.btnGallery.setVisibility(visibility);
    }

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
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupUI(view);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getShowsDialog() && getDialog() != null && getDialog().getWindow() != null) {
            android.view.Window window = getDialog().getWindow();
            int width = android.view.ViewGroup.LayoutParams.MATCH_PARENT;
            int height = (int) (getResources().getDisplayMetrics().heightPixels * 0.80);
            window.setLayout(width, height);
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setGravity(android.view.Gravity.BOTTOM);
            window.setWindowAnimations(0);
        }
    }

    private void setupUI(@NonNull View view) {
        boolean isInsideContainer = getParentFragment() instanceof SkinChatDialogContainer;
        if (isInsideContainer) {
            view.setBackgroundResource(android.R.color.transparent);
        } else if (getShowsDialog()) {
            view.setBackgroundResource(R.drawable.bg_chat_dialog);
            view.setClipToOutline(true);
            view.setOutlineProvider(ViewOutlineProvider.BACKGROUND);
        } else {
            if (isFullScreen) {
                view.setBackgroundColor(Color.WHITE);
            } else {
                view.setBackgroundResource(R.color.neutral);
            }
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
        binding.btnBack.setOnClickListener(v -> {
            if (getParentFragment() instanceof SkinChatDialogContainer) {
                ((SkinChatDialogContainer) getParentFragment()).dismiss();
            } else if (getShowsDialog()) {
                dismiss();
            } else {
                getParentFragmentManager().popBackStack();
            }
        });

        binding.btnSend.setOnClickListener(v -> {
            String text = binding.etMessageInput.getText().toString().trim();
            if (!text.isEmpty()) {
                handleUserMessage(text);
                binding.etMessageInput.setText("");
            }
        });

        binding.btnPlus.setOnClickListener(v -> pickFileLauncher.launch("*/*"));
        binding.btnCamera.setOnClickListener(v -> {
            if (androidx.core.content.ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.CAMERA)
                    == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                try {
                    takePhotoLauncher.launch(null);
                } catch (Exception e) {
                    Toast.makeText(requireContext(), "Không thể mở camera", Toast.LENGTH_SHORT).show();
                }
            } else {
                requestCameraPermissionLauncher.launch(android.Manifest.permission.CAMERA);
            }
        });
        binding.btnGallery.setOnClickListener(v -> pickImageLauncher.launch("image/*"));

        binding.etMessageInput.setOnFocusChangeListener((v, hasFocus) -> updateInputIconsVisibility());
        binding.etMessageInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateInputIconsVisibility();
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
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
