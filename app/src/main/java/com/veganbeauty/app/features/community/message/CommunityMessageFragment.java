package com.veganbeauty.app.features.community.message;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.veganbeauty.app.R;
import com.veganbeauty.app.data.local.LocalJsonReader;
import com.veganbeauty.app.data.local.ProfileSession;
import com.veganbeauty.app.data.local.entities.ConversationEntity;
import com.veganbeauty.app.databinding.ComFragmentMessageBinding;
import com.veganbeauty.app.features.community.beauty_hub.CommunityBeautyHubFragment;
import com.veganbeauty.app.features.community.com_feed.CommunityExploreFragment;
import com.veganbeauty.app.features.community.com_feed.CommunityFeedFragment;
import com.veganbeauty.app.features.community.notification.CommunityNotificationFragment;
import com.veganbeauty.app.features.community.profile.CommunityProfileFragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CommunityMessageFragment extends Fragment {

    private ComFragmentMessageBinding binding;
    private MessageAdapter messageAdapter;
    private ActiveUserAdapter activeUserAdapter;
    private List<ConversationEntity> allConversations = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = ComFragmentMessageBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupAdapters();

        SharedPreferences prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        if (!prefs.getBoolean("has_reset_firebase_chat_v3", false)) {
            MessageHelper.forceResetFirebaseFromAssets(requireContext());
            prefs.edit().putBoolean("has_reset_firebase_chat_v3", true).apply();
        }

        loadData();
        setupBottomNavigation();
        setupInteractions();

        String currentRealId = ProfileSession.getCurrentUserId(requireContext());
        MessageHelper.listenToAllConversations(requireContext(), currentRealId, () -> {
            if (isAdded()) {
                loadData();
            }
        });
    }

    private void setupAdapters() {
        messageAdapter = new MessageAdapter(new ArrayList<>(), conv -> {
            getParentFragmentManager().beginTransaction()
                    .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                    .replace(R.id.main_container, ChatDetailFragment.newInstance(conv.getId()))
                    .addToBackStack(null)
                    .commit();
        });
        activeUserAdapter = new ActiveUserAdapter(new ArrayList<>());

        binding.rvMessages.setAdapter(messageAdapter);
        binding.rvActiveUsers.setAdapter(activeUserAdapter);
    }

    private void loadData() {
        try {
            String currentRealId = ProfileSession.getCurrentUserId(requireContext());
            allConversations = MessageHelper.getConversations(requireContext(), currentRealId);
            filterConversations(binding.etSearch.getText() != null ? binding.etSearch.getText().toString() : "");

            String currentUsername = ProfileSession.getUsername(requireContext());

            List<ConversationEntity> activeUsers = new ArrayList<>();
            for (ConversationEntity conv : allConversations) {
                String partnerId = "";
                if (conv.getMembers() != null) {
                    for (String m : conv.getMembers()) {
                        if (!currentRealId.equals(m)) {
                            partnerId = m;
                            break;
                        }
                    }
                }
                if (conv.getActiveBy() != null && conv.getActiveBy().contains(partnerId)) {
                    activeUsers.add(conv);
                }
            }

            List<String> myFriendsIds = new LocalJsonReader(requireContext()).getFriendsForUser(currentUsername);

            Collections.sort(activeUsers, (c1, c2) -> {
                String partnerId1 = "";
                if (c1.getMembers() != null) {
                    for (String m : c1.getMembers()) {
                        if (!currentRealId.equals(m)) {
                            partnerId1 = m;
                            break;
                        }
                    }
                }
                String partnerId2 = "";
                if (c2.getMembers() != null) {
                    for (String m : c2.getMembers()) {
                        if (!currentRealId.equals(m)) {
                            partnerId2 = m;
                            break;
                        }
                    }
                }
                boolean f1 = myFriendsIds.contains(partnerId1);
                boolean f2 = myFriendsIds.contains(partnerId2);
                return Boolean.compare(f2, f1);
            });

            activeUserAdapter.updateData(activeUsers);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData();
    }

    private void setupInteractions() {
        binding.ivNotification.setOnClickListener(v -> getParentFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                .replace(R.id.main_container, new CommunityNotificationFragment())
                .addToBackStack(null)
                .commit());

        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                filterConversations(s != null ? s.toString() : "");
            }
        });
    }

    private void filterConversations(String query) {
        if (query.isEmpty()) {
            messageAdapter.updateData(allConversations);
        } else {
            String currentRealId = ProfileSession.getCurrentUserId(requireContext());
            List<ConversationEntity> filtered = new ArrayList<>();
            for (ConversationEntity conv : allConversations) {
                String partnerId = "";
                if (conv.getMembers() != null) {
                    for (String m : conv.getMembers()) {
                        if (!currentRealId.equals(m)) {
                            partnerId = m;
                            break;
                        }
                    }
                }
                ConversationEntity.MemberInfo partnerInfo = conv.getMemberInfo() != null ? conv.getMemberInfo().get(partnerId) : null;
                String partnerName = partnerInfo != null && partnerInfo.getName() != null ? partnerInfo.getName() : "Unknown";
                if (partnerName.toLowerCase().contains(query.toLowerCase())) {
                    filtered.add(conv);
                }
            }
            messageAdapter.updateData(filtered);
        }
    }

    private void setupBottomNavigation() {
        binding.comBottomNav.navComFeed.setOnClickListener(v -> getParentFragmentManager().beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.main_container, new CommunityFeedFragment())
                .commit());

        binding.comBottomNav.navComHub.setOnClickListener(v -> getParentFragmentManager().beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.main_container, new CommunityBeautyHubFragment())
                .commit());

        binding.comBottomNav.navComExplore.setOnClickListener(v -> getParentFragmentManager().beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.main_container, new CommunityExploreFragment())
                .commit());

        binding.comBottomNav.navComProfile.setOnClickListener(v -> getParentFragmentManager().beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.main_container, new CommunityProfileFragment())
                .commit());

        ImageView chatIcon = (ImageView) binding.comBottomNav.navComChat.getChildAt(0);
        if (chatIcon != null) {
            chatIcon.setColorFilter(getResources().getColor(R.color.primary, null));
        }
        TextView chatText = (TextView) binding.comBottomNav.navComChat.getChildAt(1);
        if (chatText != null) {
            chatText.setTextColor(getResources().getColor(R.color.primary, null));
            chatText.setTypeface(null, Typeface.BOLD);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        String currentRealId = ProfileSession.getCurrentUserId(requireContext());
        MessageHelper.removeAllConversationsListener(currentRealId);
        binding = null;
    }
}
