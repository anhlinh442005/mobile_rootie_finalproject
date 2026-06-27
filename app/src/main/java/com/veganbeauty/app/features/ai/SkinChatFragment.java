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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class SkinChatFragment extends DialogFragment {

    private SkinChatBinding _binding;
    private SkinChatAdapter chatAdapter;
    private final List<ChatMessage> messagesList = new ArrayList<>();
    private String conversationId = "";

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
            int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.90);
            int height = (int) (getResources().getDisplayMetrics().heightPixels * 0.85);
            window.setLayout(width, height);
            window.setBackgroundDrawableResource(android.R.color.transparent);
        }
    }

    private void setupUI(View view) {
        if (getShowsDialog()) {
            view.setBackgroundResource(R.drawable.bg_chat_dialog);
            view.setClipToOutline(true);
            view.setOutlineProvider(android.view.ViewOutlineProvider.BACKGROUND);
        }

        setupRecyclerView();
        setupListeners();

        String currentUserId = getCurrentUserId();
        conversationId = MessageHelper.getOrCreateConversation(
                requireContext(),
                currentUserId,
                "rootie_vn",
                "Rootie VietNam",
                "https://res.cloudinary.com/dpjkzxjl2/image/upload/v1780560866/Rootie_logo.png"
        );

        loadConversationData();

        MessageHelper.listenToConversation(requireContext(), conversationId, () -> {
            if (isAdded()) {
                loadConversationData();
            }
            return kotlin.Unit.INSTANCE;
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
        _binding.btnBack.setOnClickListener(v -> dismiss());

        _binding.btnSend.setOnClickListener(v -> sendMessage());

        _binding.etMessageInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage();
                return true;
            }
            return false;
        });

        _binding.btnPlus.setOnClickListener(v -> Toast.makeText(requireContext(), "Tính năng đính kèm tệp sẽ sớm ra mắt!", Toast.LENGTH_SHORT).show());
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
                h.binding.tvMessage.setText(item.text);
                h.binding.tvTime.setText(item.time);
            } else if (holder instanceof RightViewHolder) {
                RightViewHolder h = (RightViewHolder) holder;
                h.binding.tvMessage.setText(item.text);
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
