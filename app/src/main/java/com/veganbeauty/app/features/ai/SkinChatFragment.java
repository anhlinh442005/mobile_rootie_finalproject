package com.veganbeauty.app.features.ai;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.veganbeauty.app.R;
import com.veganbeauty.app.data.local.ProfileSession;
import com.veganbeauty.app.databinding.ItemSkinChatLeftBinding;
import com.veganbeauty.app.databinding.ItemSkinChatRightBinding;
import com.veganbeauty.app.databinding.ItemSkinChatTimeBinding;
import com.veganbeauty.app.databinding.SkinChatBinding;
import com.veganbeauty.app.features.community.message.MessageHelper;
import com.veganbeauty.app.data.local.entities.ChatMessageEntity;
import com.veganbeauty.app.utils.AvatarLoader;
import com.veganbeauty.app.utils.RootieBrandHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

public class SkinChatFragment extends DialogFragment {

    private SkinChatBinding _binding;
    private SkinChatAdapter chatAdapter;
    private final List<ChatMessage> messagesList = new ArrayList<>();
    private String conversationId = "";

    private final ActivityResultLauncher<String> pickFileLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    String fileName = getFileName(uri);
                    sendTextMessage("Đã đính kèm tệp: " + fileName);
                }
            }
    );

    private final ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    sendTextMessage("image:" + uri.toString());
                }
            }
    );

    private final ActivityResultLauncher<Void> takePhotoLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicturePreview(), bitmap -> {
                if (bitmap != null) {
                    String uriStr = saveBitmapToCache(bitmap);
                    if (uriStr != null) {
                        sendTextMessage("image:" + uriStr);
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

    private void sendTextMessage(String text) {
        String currentUserId = getCurrentUserId();
        MessageHelper.sendMessage(requireContext(), conversationId, currentUserId, "rootie_vn", text);
        loadConversationData();
    }

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
        if (_binding == null) return;
        boolean hasFocus = _binding.etMessageInput.hasFocus();
        boolean hasText = _binding.etMessageInput.getText().length() > 0;
        int visibility = (hasFocus || hasText) ? View.GONE : View.VISIBLE;
        _binding.btnCamera.setVisibility(visibility);
        _binding.btnGallery.setVisibility(visibility);
    }

    public enum Sender {
        AGENT, USER, TIME
    }

    public static class ChatMessage {
        public final Sender sender;
        public final String text;
        public final String time;
        public final long timestamp;

        public ChatMessage(Sender sender, String text, String time, long timestamp) {
            this.sender = sender;
            this.text = text;
            this.time = time;
            this.timestamp = timestamp;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        _binding = SkinChatBinding.inflate(inflater, container, false);
        return _binding.getRoot();
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

    private void setupUI(View view) {
        boolean isInsideContainer = getParentFragment() instanceof SkinChatDialogContainer;
        if (isInsideContainer) {
            view.setBackgroundResource(android.R.color.transparent);
        } else if (getShowsDialog()) {
            view.setBackgroundResource(R.drawable.bg_chat_dialog);
            view.setClipToOutline(true);
            view.setOutlineProvider(android.view.ViewOutlineProvider.BACKGROUND);
        } else {
            view.setBackgroundResource(R.color.neutral);
        }

        setupRecyclerView();
        setupListeners();

        AvatarLoader.loadAvatar(_binding.ivHeaderAvatar, RootieBrandHelper.AVATAR_URL);

        String currentUserId = getCurrentUserId();
        conversationId = MessageHelper.getOrCreateConversation(
                requireContext(),
                currentUserId,
                RootieBrandHelper.USER_ID_VN,
                "Rootie VietNam",
                RootieBrandHelper.AVATAR_URL
        );

        loadConversationData();

        MessageHelper.listenToConversation(requireContext(), conversationId, () -> {
            if (isAdded()) {
                loadConversationData();
            }
        });
    }

    private void setupRecyclerView() {
        chatAdapter = new SkinChatAdapter(messagesList);
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        layoutManager.setStackFromEnd(true);
        _binding.rvChatList.setLayoutManager(layoutManager);
        _binding.rvChatList.setAdapter(chatAdapter);
    }

    private void setupListeners() {
        _binding.btnBack.setOnClickListener(v -> {
            if (getParentFragment() instanceof SkinChatDialogContainer) {
                ((SkinChatDialogContainer) getParentFragment()).dismiss();
            } else if (getShowsDialog()) {
                dismiss();
            } else {
                getParentFragmentManager().popBackStack();
            }
        });

        _binding.btnSend.setOnClickListener(v -> sendMessage());

        _binding.etMessageInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage();
                return true;
            }
            return false;
        });

        _binding.btnPlus.setOnClickListener(v -> pickFileLauncher.launch("*/*"));
        _binding.btnCamera.setOnClickListener(v -> {
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
        _binding.btnGallery.setOnClickListener(v -> pickImageLauncher.launch("image/*"));

        _binding.etMessageInput.setOnFocusChangeListener((v, hasFocus) -> updateInputIconsVisibility());
        _binding.etMessageInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateInputIconsVisibility();
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });
    }

    private void loadConversationData() {
        String currentUserId = getCurrentUserId();
        MessageHelper.markAsRead(requireContext(), conversationId, currentUserId);

        List<ChatMessageEntity> rawMessages = MessageHelper.getMessages(requireContext(), conversationId);
        messagesList.clear();
        messagesList.add(new ChatMessage(Sender.TIME, "Lịch sử cuộc trò chuyện", "", System.currentTimeMillis()));
        for (ChatMessageEntity msg : rawMessages) {
            boolean isAgent = "rootie_vn".equals(msg.getSenderId());
            long timestamp = parseIsoString(msg.getSentAt());
            String timeStr = formatTime(timestamp);
            messagesList.add(new ChatMessage(
                    isAgent ? Sender.AGENT : Sender.USER,
                    msg.getText(),
                    timeStr,
                    timestamp
            ));
        }
        chatAdapter.notifyDataSetChanged();
        if (!messagesList.isEmpty()) {
            _binding.rvChatList.scrollToPosition(messagesList.size() - 1);
        }
    }

    private long parseIsoString(String isoStr) {
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
            format.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = format.parse(isoStr);
            return date != null ? date.getTime() : System.currentTimeMillis();
        } catch (Exception e) {
            return System.currentTimeMillis();
        }
    }

    private String formatTime(long timestamp) {
        return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(timestamp));
    }

    private String getCurrentUserId() {
        String userId = ProfileSession.getCurrentUserId(requireContext());
        return userId != null ? userId : "guest_user";
    }

    private void sendMessage() {
        String text = _binding.etMessageInput.getText().toString().trim();
        if (text.isEmpty()) return;

        String currentUserId = getCurrentUserId();

        List<ChatMessageEntity> rawMessages = MessageHelper.getMessages(requireContext(), conversationId);
        long now = System.currentTimeMillis();
        boolean isNewSession;
        if (rawMessages.isEmpty()) {
            isNewSession = true;
        } else {
            long lastMsgTime = parseIsoString(rawMessages.get(rawMessages.size() - 1).getSentAt());
            isNewSession = (now - lastMsgTime) > 3600000;
        }

        MessageHelper.sendMessage(requireContext(), conversationId, currentUserId, "rootie_vn", text);
        _binding.etMessageInput.setText("");
        loadConversationData();

        if (isNewSession) {
            _binding.rvChatList.postDelayed(() -> {
                if (isAdded()) {
                    MessageHelper.sendMessage(
                            requireContext(),
                            conversationId,
                            "rootie_vn",
                            currentUserId,
                            "Chào bạn, tôi là chuyên gia tư vấn Rootie. Tôi có thể giúp gì cho bạn hôm nay?"
                    );
                    loadConversationData();
                }
            }, 1000);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        MessageHelper.removeConversationListener(conversationId);
        _binding = null;
    }

    private class SkinChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private final List<ChatMessage> list;
        private static final int TYPE_LEFT = 0;
        private static final int TYPE_RIGHT = 1;
        private static final int TYPE_TIME = 2;

        public SkinChatAdapter(List<ChatMessage> list) {
            this.list = list;
        }

        @Override
        public int getItemViewType(int position) {
            switch (list.get(position).sender) {
                case AGENT:
                    return TYPE_LEFT;
                case USER:
                    return TYPE_RIGHT;
                case TIME:
                default:
                    return TYPE_TIME;
            }
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            if (viewType == TYPE_LEFT) {
                ItemSkinChatLeftBinding binding = ItemSkinChatLeftBinding.inflate(inflater, parent, false);
                return new LeftViewHolder(binding);
            } else if (viewType == TYPE_RIGHT) {
                ItemSkinChatRightBinding binding = ItemSkinChatRightBinding.inflate(inflater, parent, false);
                return new RightViewHolder(binding);
            } else {
                ItemSkinChatTimeBinding binding = ItemSkinChatTimeBinding.inflate(inflater, parent, false);
                return new TimeViewHolder(binding);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            ChatMessage item = list.get(position);
            if (holder instanceof LeftViewHolder) {
                LeftViewHolder h = (LeftViewHolder) holder;
                if (item.text != null && item.text.startsWith("image:")) {
                    h.binding.tvMessage.setVisibility(View.GONE);
                    h.binding.ivMessageImage.setVisibility(View.VISIBLE);
                    String imgUri = item.text.substring(6);
                    com.bumptech.glide.Glide.with(h.binding.ivMessageImage.getContext())
                            .load(imgUri)
                            .placeholder(android.R.color.darker_gray)
                            .error(android.R.color.darker_gray)
                            .into(h.binding.ivMessageImage);
                } else {
                    h.binding.tvMessage.setVisibility(View.VISIBLE);
                    h.binding.ivMessageImage.setVisibility(View.GONE);
                    h.binding.tvMessage.setText(item.text);
                }
                h.binding.tvTime.setText(item.time);
                AvatarLoader.loadAvatar(h.binding.ivAvatar, RootieBrandHelper.AVATAR_URL);
            } else if (holder instanceof RightViewHolder) {
                RightViewHolder h = (RightViewHolder) holder;
                if (item.text != null && item.text.startsWith("image:")) {
                    h.binding.tvMessage.setVisibility(View.GONE);
                    h.binding.ivMessageImage.setVisibility(View.VISIBLE);
                    String imgUri = item.text.substring(6);
                    com.bumptech.glide.Glide.with(h.binding.ivMessageImage.getContext())
                            .load(imgUri)
                            .placeholder(android.R.color.darker_gray)
                            .error(android.R.color.darker_gray)
                            .into(h.binding.ivMessageImage);
                } else {
                    h.binding.tvMessage.setVisibility(View.VISIBLE);
                    h.binding.ivMessageImage.setVisibility(View.GONE);
                    h.binding.tvMessage.setText(item.text);
                }
                h.binding.tvTime.setText(item.time);
            } else if (holder instanceof TimeViewHolder) {
                TimeViewHolder h = (TimeViewHolder) holder;
                h.binding.tvTimeText.setText(item.text);
            }
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class LeftViewHolder extends RecyclerView.ViewHolder {
            ItemSkinChatLeftBinding binding;

            LeftViewHolder(ItemSkinChatLeftBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }

        class RightViewHolder extends RecyclerView.ViewHolder {
            ItemSkinChatRightBinding binding;

            RightViewHolder(ItemSkinChatRightBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }

        class TimeViewHolder extends RecyclerView.ViewHolder {
            ItemSkinChatTimeBinding binding;

            TimeViewHolder(ItemSkinChatTimeBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }
}
