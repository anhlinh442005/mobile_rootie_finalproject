package com.veganbeauty.app.features.community.message;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.veganbeauty.app.R;
import com.veganbeauty.app.core.base.RootieFragment;
import com.veganbeauty.app.data.local.ProfileSession;
import com.veganbeauty.app.data.local.entities.ChatMessageEntity;
import com.veganbeauty.app.data.local.entities.ConversationEntity;
import com.veganbeauty.app.data.local.entities.MemberInfoEntity;
import com.veganbeauty.app.databinding.ComFragmentChatDetailBinding;
import com.veganbeauty.app.utils.RootieBrandHelper;

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
        currentUserId = ProfileSession.getCurrentUserId(requireContext());
        if (currentUserId == null || currentUserId.isEmpty()) {
            currentUserId = ProfileSession.getUserId(requireContext());
        }
        
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

        binding.ivLike.setOnClickListener(v -> sendMessage(MessageHelper.MESSAGE_LIKE));

        setupInputBarInsets();
        setupInputFocusBehavior();

        loadConversation();
    }

    private void setupInputBarInsets() {
        final int baseBottomPad = (int) (12 * getResources().getDisplayMetrics().density);
        ViewCompat.setOnApplyWindowInsetsListener(binding.llInput, (v, windowInsets) -> {
            Insets navBars = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars());
            v.setPadding(
                    v.getPaddingLeft(),
                    v.getPaddingTop(),
                    v.getPaddingRight(),
                    baseBottomPad + navBars.bottom
            );
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(binding.llInput);
    }

    private void setupInputFocusBehavior() {
        binding.etMessage.setOnFocusChangeListener((v, hasFocus) -> updateInputExpandedState());
        binding.etMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateInputExpandedState();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void updateInputExpandedState() {
        if (binding == null) {
            return;
        }
        boolean expanded = binding.etMessage.hasFocus()
                || !binding.etMessage.getText().toString().trim().isEmpty();
        binding.llInputActions.setVisibility(expanded ? View.GONE : View.VISIBLE);

        ViewGroup.LayoutParams inputLp = binding.llMessageInput.getLayoutParams();
        if (inputLp instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams marginLp = (ViewGroup.MarginLayoutParams) inputLp;
            marginLp.setMarginStart(expanded ? 0 : (int) (14 * getResources().getDisplayMetrics().density));
            binding.llMessageInput.setLayoutParams(marginLp);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        markConversationRead();
    }

    private void markConversationRead() {
        final Context context = getContext();
        if (context == null || conversationId == null || currentUserId == null) return;
        new Thread(() -> MessageHelper.markAsRead(context.getApplicationContext(), conversationId, currentUserId)).start();
    }

    private void loadConversation() {
        final Context context = getContext();
        if (context == null || conversationId == null) return;

        new Thread(() -> {
            try {
                MessageHelper.syncConversationsFromAssets(context, currentUserId);
                MessageHelper.markAsRead(context, conversationId, currentUserId);
                ConversationEntity conv = MessageHelper.getConversationById(context, conversationId);
                if (conv != null && isAdded() && getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (!isAdded() || binding == null) return;
                        String partnerAvatarUrl = bindHeader(conv);
                        adapter.setPartnerAvatarUrl(partnerAvatarUrl);
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

    private String bindHeader(ConversationEntity conv) {
        partnerId = "";
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
            String displayName = partnerInfo.getName();
            binding.tvPartnerName.setText(displayName);
            if (RootieBrandHelper.isRootieUser(partnerId, displayName)) {
                binding.ivVerified.setVisibility(View.VISIBLE);
            } else {
                binding.ivVerified.setVisibility(View.GONE);
            }
            String avatarUrl = RootieBrandHelper.resolveAvatar(partnerId, partnerInfo.getAvatar());
            if (avatarUrl == null || avatarUrl.trim().isEmpty()) {
                binding.ivPartnerAvatar.setImageResource(R.drawable.img_avatar);
                return "";
            }
            ImageRequest request = new ImageRequest.Builder(requireContext())
                    .data(avatarUrl)
                    .crossfade(true)
                    .transformations(new CircleCropTransformation())
                    .placeholder(R.drawable.img_avatar)
                    .error(R.drawable.img_avatar)
                    .target(binding.ivPartnerAvatar)
                    .build();
            Coil.imageLoader(requireContext()).enqueue(request);
            return avatarUrl;
        }

        binding.ivPartnerAvatar.setImageResource(R.drawable.img_avatar);
        return "";
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
