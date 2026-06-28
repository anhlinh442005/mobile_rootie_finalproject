package com.veganbeauty.app.features.community.message;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwnerKt;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.veganbeauty.app.R;
import com.veganbeauty.app.core.base.RootieFragment;
import com.veganbeauty.app.data.local.ProfileSession;
import com.veganbeauty.app.data.local.entities.ChatMessageEntity;
import com.veganbeauty.app.data.local.entities.ConversationEntity;
import com.veganbeauty.app.data.local.entities.MemberInfoEntity;
import com.veganbeauty.app.databinding.ComFragmentChatDetailBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import coil.Coil;
import coil.request.ImageRequest;
import coil.transform.CircleCropTransformation;
import kotlinx.coroutines.BuildersKt;
import kotlinx.coroutines.Dispatchers;

public class ChatDetailFragment extends RootieFragment {

    private ComFragmentChatDetailBinding binding;
    private ChatMessageAdapter adapter;
    private String conversationId;
    private String partnerId;
    private String currentUserId;

    public static ChatDetailFragment newInstance(String conversationId) {
        ChatDetailFragment fragment = new ChatDetailFragment();
        Bundle args = new Bundle();
        args.putString("CONVERSATION_ID", conversationId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            conversationId = getArguments().getString("CONVERSATION_ID");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = ComFragmentChatDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    protected void setupUI(@NonNull View view) {
        currentUserId = ProfileSession.getUserId(requireContext());
        
        binding.btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        adapter = new ChatMessageAdapter(currentUserId);
        binding.rvMessages.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvMessages.setAdapter(adapter);

        binding.btnSend.setOnClickListener(v -> {
            String text = binding.etMessage.getText().toString().trim();
            if (!text.isEmpty()) {
                sendMessage(text);
                binding.etMessage.setText("");
            }
        });

        loadConversation();
    }

    private void loadConversation() {
        final Context context = getContext();
        if (context == null || conversationId == null) return;

        new Thread(() -> {
            try {
                ConversationEntity conv = MessageHelper.getConversationById(context, conversationId);
                if (conv != null && isAdded() && getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (!isAdded() || binding == null) return;
                        bindHeader(conv);
                        if (conv.getMessages() != null && !conv.getMessages().isEmpty()) {
                            adapter.submitList(conv.getMessages());
                            binding.rvMessages.scrollToPosition(conv.getMessages().size() - 1);
                        } else if (adapter != null) {
                            adapter.submitList(new ArrayList<>());
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void bindHeader(ConversationEntity conv) {
        if (conv.getMembers() != null) {
            for (String m : conv.getMembers()) {
                if (!m.equals(currentUserId)) {
                    partnerId = m;
                    break;
                }
            }
        }

        Map<String, MemberInfoEntity> memberInfo = conv.getMemberInfo();
        MemberInfoEntity partnerInfo = memberInfo != null ? memberInfo.get(partnerId) : null;

        if (partnerInfo != null) {
            binding.tvPartnerName.setText(partnerInfo.getName());
            ImageRequest request = new ImageRequest.Builder(requireContext())
                    .data(partnerInfo.getAvatar())
                    .crossfade(true)
                    .transformations(new CircleCropTransformation())
                    .placeholder(R.drawable.img_avatar)
                    .error(R.drawable.img_avatar)
                    .target(binding.ivPartnerAvatar)
                    .build();
            Coil.imageLoader(requireContext()).enqueue(request);
        }
    }

    private void sendMessage(String text) {
        final Context context = getContext();
        if (context == null || conversationId == null) return;

        new Thread(() -> {
            try {
                MessageHelper.sendMessage(context, conversationId, currentUserId, text);
                if (isAdded() && getActivity() != null) {
                    getActivity().runOnUiThread(this::loadConversation);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
