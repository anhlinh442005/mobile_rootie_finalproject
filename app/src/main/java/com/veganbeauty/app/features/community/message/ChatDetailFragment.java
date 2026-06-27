package com.veganbeauty.app.features.community.message;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import coil.Coil;
import coil.request.ImageRequest;
import coil.transform.CircleCropTransformation;

import com.veganbeauty.app.R;
import com.veganbeauty.app.data.local.LocalJsonReader;
import com.veganbeauty.app.data.local.ProfileSession;
import com.veganbeauty.app.data.local.entities.ChatMessageEntity;
import com.veganbeauty.app.data.local.entities.ConversationEntity;
import com.veganbeauty.app.data.local.entities.UserEntity;
import com.veganbeauty.app.databinding.ComFragmentChatDetailBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ChatDetailFragment extends Fragment {

    private ComFragmentChatDetailBinding _binding;
    private String conversationId = null;
    private ChatDetailAdapter chatAdapter;
    private String partnerId = "";

    private static final String ARG_CONVERSATION_ID = "conversation_id";

    public static ChatDetailFragment newInstance(String id) {
        ChatDetailFragment fragment = new ChatDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CONVERSATION_ID, id);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            conversationId = getArguments().getString(ARG_CONVERSATION_ID);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        _binding = ComFragmentChatDetailBinding.inflate(inflater, container, false);
        return _binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        chatAdapter = new ChatDetailAdapter(new ArrayList<>(), this::showMessageOptionsDialog);
        _binding.rvChat.setAdapter(chatAdapter);

        _binding.btnBack.setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());

        _binding.ivSend.setOnClickListener(v -> {
            String text = _binding.etMessage.getText().toString().trim();
            if (!text.isEmpty()) {
                sendMessage(text);
                _binding.etMessage.setText("");
            }
        });

        _binding.etMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s != null && s.length() > 0) {
                    _binding.ivSend.setVisibility(View.VISIBLE);
                    _binding.llRightIcons.setVisibility(View.GONE);
                } else {
                    _binding.ivSend.setVisibility(View.GONE);
                    _binding.llRightIcons.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        loadData();

        if (conversationId != null) {
            MessageHelper.listenToConversation(requireContext(), conversationId, () -> {
                if (isAdded()) {
                    refreshLocalMessages(conversationId);
                }
                return kotlin.Unit.INSTANCE;
            });
        }
    }

    private void loadData() {
        if (conversationId != null) {
            String currentUserId = ProfileSession.getCurrentUserId(requireContext());
            MessageHelper.markAsRead(requireContext(), conversationId, currentUserId);

            ConversationEntity conv = MessageHelper.getConversation(requireContext(), conversationId);
            if (conv != null) {
                List<String> members = conv.getMembers() != null ? conv.getMembers() : new ArrayList<>();
                for (String m : members) {
                    if (!m.equals(currentUserId)) {
                        partnerId = m;
                        break;
                    }
                }

                Map<String, ConversationEntity.MemberInfo> memberInfo = conv.getMemberInfo();
                ConversationEntity.MemberInfo partnerInfo = memberInfo != null ? memberInfo.get(partnerId) : null;
                String partnerName = partnerInfo != null ? partnerInfo.getName() : "Unknown";
                String partnerAvatar = partnerInfo != null ? partnerInfo.getAvatar() : "";

                if ("You".equals(partnerName) || "Unknown".equals(partnerName) || partnerAvatar.isEmpty()) {
                    List<UserEntity> users = new LocalJsonReader(requireContext()).getUsers();
                    UserEntity user = null;
                    for (UserEntity u : users) {
                        if (u.getUser_id().equals(partnerId)) {
                            user = u;
                            break;
                        }
                    }

                    if (user != null) {
                        if ("You".equals(partnerName) || "Unknown".equals(partnerName)) {
                            partnerName = user.getFull_name() != null ? user.getFull_name() : partnerName;
                        }
                        if (partnerAvatar.isEmpty()) {
                            partnerAvatar = user.getAvatar() != null ? user.getAvatar() : "";
                        }
                    } else if ("test_001".equals(partnerId)) {
                        if ("You".equals(partnerName) || "Unknown".equals(partnerName)) {
                            partnerName = "Test User";
                        }
                    }
                }

                _binding.tvName.setText(partnerName);

                if ("rootie_vn".equals(partnerId)) {
                    _binding.ivVerified.setVisibility(View.VISIBLE);
                } else {
                    _binding.ivVerified.setVisibility(View.GONE);
                }

                if (!partnerAvatar.isEmpty()) {
                    ImageRequest request = new ImageRequest.Builder(requireContext())
                            .data(partnerAvatar)
                            .crossfade(true)
                            .placeholder(R.color.gray_light)
                            .error("rootie_vn".equals(partnerId) ? R.drawable.ic_logo_rootie : R.drawable.mascot_message)
                            .transformations(new CircleCropTransformation())
                            .target(_binding.ivAvatar)
                            .build();
                    Coil.imageLoader(requireContext()).enqueue(request);
                } else {
                    int defaultRes = "rootie_vn".equals(partnerId) ? R.drawable.ic_logo_rootie : R.drawable.mascot_message;
                    ImageRequest request = new ImageRequest.Builder(requireContext())
                            .data(defaultRes)
                            .transformations(new CircleCropTransformation())
                            .target(_binding.ivAvatar)
                            .build();
                    Coil.imageLoader(requireContext()).enqueue(request);
                }

                List<String> activeBy = conv.getActiveBy() != null ? conv.getActiveBy() : new ArrayList<>();
                boolean isActive = activeBy.contains(partnerId);
                _binding.vActiveDot.setVisibility(isActive ? View.VISIBLE : View.GONE);
                _binding.tvStatus.setText(isActive ? "Đang hoạt động" : "Ngoại tuyến");

                chatAdapter.setPartnerAvatar(partnerAvatar);
            }

            List<ChatMessageEntity> messages = MessageHelper.getMessages(requireContext(), conversationId);
            chatAdapter.updateData(messages);
            if (!messages.isEmpty()) {
                _binding.rvChat.scrollToPosition(messages.size() - 1);
            }
        }
    }

    private void refreshLocalMessages(String convId) {
        List<ChatMessageEntity> messages = MessageHelper.getMessages(requireContext(), convId);
        chatAdapter.updateData(messages);
        if (!messages.isEmpty()) {
            _binding.rvChat.scrollToPosition(messages.size() - 1);
        }
    }

    private void sendMessage(String text) {
        if (conversationId != null) {
            String currentUserId = ProfileSession.getCurrentUserId(requireContext());
            MessageHelper.sendMessage(requireContext(), conversationId, currentUserId, partnerId, text);
            refreshLocalMessages(conversationId);
        }
    }

    private void showMessageOptionsDialog(ChatMessageEntity msg) {
        String[] options = {"Chỉnh sửa", "Thu hồi"};
        new AlertDialog.Builder(requireContext())
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            showEditDialog(msg);
                            break;
                        case 1:
                            revokeMessage(msg);
                            break;
                    }
                })
                .show();
    }

    private void showEditDialog(ChatMessageEntity msg) {
        EditText input = new EditText(requireContext());
        input.setText(msg.getText());
        input.setSelection(input.getText().length());
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(50, 20, 50, 0);
        input.setLayoutParams(params);

        FrameLayout container = new FrameLayout(requireContext());
        container.addView(input);

        new AlertDialog.Builder(requireContext())
                .setTitle("Chỉnh sửa tin nhắn")
                .setView(container)
                .setPositiveButton("Lưu", (dialog, which) -> {
                    String newText = input.getText().toString().trim();
                    if (!newText.isEmpty() && !newText.equals(msg.getText())) {
                        updateMessage(msg.getId(), newText);
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void updateMessage(String msgId, String newText) {
        if (conversationId != null) {
            MessageHelper.updateMessage(requireContext(), conversationId, msgId, newText);
            refreshLocalMessages(conversationId);
        }
    }

    private void revokeMessage(ChatMessageEntity msg) {
        if (conversationId != null) {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Thu hồi tin nhắn")
                    .setMessage("Tin nhắn này sẽ bị xóa. Bạn có chắc chắn muốn thu hồi?")
                    .setPositiveButton("Thu hồi", (dialog, which) -> {
                        MessageHelper.deleteMessage(requireContext(), conversationId, msg.getId());
                        refreshLocalMessages(conversationId);
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (conversationId != null) {
            MessageHelper.removeConversationListener(conversationId);
        }
        _binding = null;
    }
}
