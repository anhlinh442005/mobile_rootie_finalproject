package com.veganbeauty.app.features.community.message;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.veganbeauty.app.R;
import com.veganbeauty.app.core.base.RootieFragment;
import com.veganbeauty.app.data.local.ProfileSession;
import com.veganbeauty.app.data.local.entities.ConversationEntity;
import com.veganbeauty.app.data.repository.CommunityNotificationRepository;
import com.veganbeauty.app.databinding.ComFragmentMessageBinding;
import com.veganbeauty.app.features.community.notification.CommunityNotificationFragment;
import com.veganbeauty.app.utils.ComBottomNavHelper;

import java.util.ArrayList;
import java.util.List;

import kotlinx.coroutines.flow.Flow;

public class CommunityMessageFragment extends RootieFragment {

    private static final int FILTER_ALL = 0;
    private static final int FILTER_UNREAD = 1;

    private ComFragmentMessageBinding binding;
    private MessageAdapter messageAdapter;
    private ActiveUserAdapter activeUserAdapter;
    private String currentUserId = "";
    private final List<ConversationEntity> allConversations = new ArrayList<>();
    private int currentFilter = FILTER_ALL;
    private String searchQuery = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = ComFragmentMessageBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    protected void setupUI(@NonNull View view) {
        try {
            Context ctx = getContext();
            if (ctx == null || binding == null) return;

            MessageHelper.initDataIfNeed(ctx);
            currentUserId = ProfileSession.getCurrentUserId(ctx);
            if (currentUserId == null || currentUserId.isEmpty()) {
                currentUserId = ProfileSession.getUserId(ctx);
            }
            if (currentUserId == null) currentUserId = "";

            binding.btnHome.setOnClickListener(v -> ComBottomNavHelper.navigateToAppHome(this));

            binding.ivNotification.setOnClickListener(v -> {
                getParentFragmentManager().beginTransaction()
                        .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                        .replace(R.id.main_container, new CommunityNotificationFragment())
                        .addToBackStack(null)
                        .commit();
            });

            activeUserAdapter = new ActiveUserAdapter(new ArrayList<>(), currentUserId, conv -> openChat(conv.getId()));
            binding.rvActiveUsers.setLayoutManager(new LinearLayoutManager(ctx, LinearLayoutManager.HORIZONTAL, false));
            binding.rvActiveUsers.setAdapter(activeUserAdapter);

            messageAdapter = new MessageAdapter(new ArrayList<>(), currentUserId, conv -> openChat(conv.getId()));
            binding.rvMessages.setLayoutManager(new LinearLayoutManager(ctx));
            binding.rvMessages.setAdapter(messageAdapter);

            binding.etSearch.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    searchQuery = s != null ? s.toString().trim().toLowerCase() : "";
                    applyFilter();
                }
            });

            binding.chipFilterAll.setOnClickListener(v -> setFilter(FILTER_ALL));
            binding.chipFilterUnread.setOnClickListener(v -> setFilter(FILTER_UNREAD));
            updateFilterChipStyles();

            setupBottomNav();
            loadConversations();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setFilter(int filter) {
        currentFilter = filter;
        updateFilterChipStyles();
        applyFilter();
    }

    private void updateFilterChipStyles() {
        if (binding == null) return;
        boolean allSelected = currentFilter == FILTER_ALL;
        binding.chipFilterAll.setBackgroundResource(allSelected ? R.drawable.bg_chip_selected : R.drawable.bg_chip_normal);
        binding.chipFilterAll.setTextColor(getResources().getColor(allSelected ? R.color.primary : R.color.gray_dark, null));
        binding.chipFilterAll.setTypeface(null, allSelected ? Typeface.BOLD : Typeface.NORMAL);

        binding.chipFilterUnread.setBackgroundResource(allSelected ? R.drawable.bg_chip_normal : R.drawable.bg_chip_selected);
        binding.chipFilterUnread.setTextColor(getResources().getColor(allSelected ? R.color.gray_dark : R.color.primary, null));
        binding.chipFilterUnread.setTypeface(null, allSelected ? Typeface.NORMAL : Typeface.BOLD);
    }

    private void applyFilter() {
        List<ConversationEntity> filtered = new ArrayList<>();
        for (ConversationEntity conversation : allConversations) {
            if (currentFilter == FILTER_UNREAD && !isUnread(conversation)) {
                continue;
            }
            if (!matchesSearch(conversation)) {
                continue;
            }
            filtered.add(conversation);
        }
        messageAdapter.updateList(filtered);
    }

    private boolean isUnread(ConversationEntity conversation) {
        return conversation.getUnreadBy() != null && conversation.getUnreadBy().contains(currentUserId);
    }

    private boolean matchesSearch(ConversationEntity conversation) {
        if (searchQuery.isEmpty()) {
            return true;
        }
        String lastMessage = conversation.getLastMessage() != null ? conversation.getLastMessage().toLowerCase() : "";
        if (MessageHelper.isLikeMessage(conversation.getLastMessage())) {
            lastMessage = "like";
        }
        if (lastMessage.contains(searchQuery)) {
            return true;
        }
        if (conversation.getMemberInfo() != null) {
            for (com.veganbeauty.app.data.local.entities.MemberInfoEntity info : conversation.getMemberInfo().values()) {
                if (info.getName() != null && info.getName().toLowerCase().contains(searchQuery)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void setupBottomNav() {
        if (binding == null || binding.comBottomNav == null) return;
        ComBottomNavHelper.setup(this, binding.comBottomNav.getRoot(), ComBottomNavHelper.TAB_CHAT);
    }

    private void loadConversations() {
        Context appCtx = getContext();
        if (appCtx == null) return;

        final Context context = appCtx.getApplicationContext();
        final String userId = currentUserId;

        MessageHelper.syncConversationsFromAssets(context, userId);
        MessageHelper.fetchAndMergeFirebaseConversations(context, userId, () -> {
            if (!isAdded() || getActivity() == null) return;
            getActivity().runOnUiThread(() -> {
                if (!isAdded() || activeUserAdapter == null || messageAdapter == null) return;
                allConversations.clear();
                allConversations.addAll(MessageHelper.getConversations(context, userId));
                activeUserAdapter.updateList(new ArrayList<>(allConversations));
                applyFilter();
            });
        });
    }

    private void openChat(String conversationId) {
        if (conversationId == null || conversationId.isEmpty()) return;
        Context ctx = getContext();
        if (ctx != null) {
            MessageHelper.markAsRead(ctx.getApplicationContext(), conversationId, currentUserId);
            for (ConversationEntity conversation : allConversations) {
                if (conversationId.equals(conversation.getId())) {
                    if (conversation.getUnreadBy() != null) {
                        conversation.getUnreadBy().remove(currentUserId);
                    }
                    break;
                }
            }
            applyFilter();
        }

        try {
            getParentFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                    .replace(R.id.main_container, ChatDetailFragment.newInstance(conversationId))
                    .addToBackStack(null)
                    .commitAllowingStateLoss();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected Flow<Integer> getUnreadCountFlow(Context context) {
        return CommunityNotificationRepository.getInstance(context).getUnreadCount();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (binding != null) {
            loadConversations();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        messageAdapter = null;
        activeUserAdapter = null;
    }
}
