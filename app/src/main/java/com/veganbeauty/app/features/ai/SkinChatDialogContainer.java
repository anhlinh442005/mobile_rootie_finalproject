package com.veganbeauty.app.features.ai;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import com.veganbeauty.app.R;
import com.veganbeauty.app.data.local.ProfileSession;

public class SkinChatDialogContainer extends DialogFragment {

    private boolean isAiActive = true;
    private View tabBubbleAi;
    private View tabBubbleHuman;
    private View cardTabAi;
    private View cardTabHuman;
    private View tabBadgeAi;
    private View tabBadgeHuman;

    public static SkinChatDialogContainer newInstance(boolean startWithAi) {
        SkinChatDialogContainer fragment = new SkinChatDialogContainer();
        Bundle args = new Bundle();
        args.putBoolean("START_WITH_AI", startWithAi);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_skin_chat_container, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tabBubbleAi = view.findViewById(R.id.tab_bubble_ai);
        tabBubbleHuman = view.findViewById(R.id.tab_bubble_human);
        cardTabAi = view.findViewById(R.id.card_tab_ai);
        cardTabHuman = view.findViewById(R.id.card_tab_human);
        tabBadgeAi = view.findViewById(R.id.tab_badge_ai);
        tabBadgeHuman = view.findViewById(R.id.tab_badge_human);

        if (getArguments() != null) {
            isAiActive = getArguments().getBoolean("START_WITH_AI", true);
        }

        // Clicking outside the card/tabs closes the dialog
        view.setOnClickListener(v -> dismiss());

        // Consume click on dialogContentContainer to prevent close
        View contentCard = view.findViewById(R.id.dialogContentContainer);
        if (contentCard != null) {
            contentCard.setOnClickListener(v -> {
                // Consume touch
            });
        }



        setupTabs();
        showActiveChat(false);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            android.view.Window window = getDialog().getWindow();
            int width = android.view.ViewGroup.LayoutParams.MATCH_PARENT;
            int height = android.view.ViewGroup.LayoutParams.MATCH_PARENT;
            window.setLayout(width, height);
            window.setBackgroundDrawableResource(android.R.color.transparent);
        }
    }

    private void setupTabs() {
        tabBubbleAi.setOnClickListener(v -> {
            if (!isAiActive) {
                isAiActive = true;
                showActiveChat(true);
            }
        });

        tabBubbleHuman.setOnClickListener(v -> {
            if (isAiActive) {
                isAiActive = false;
                showActiveChat(true);
            }
        });

        updateTabBadges();
    }

    private void showActiveChat(boolean animate) {
        if (isAiActive) {
            cardTabAi.setScaleX(1.1f);
            cardTabAi.setScaleY(1.1f);
            cardTabAi.setAlpha(1.0f);
            cardTabHuman.setScaleX(1.0f);
            cardTabHuman.setScaleY(1.0f);
            cardTabHuman.setAlpha(0.6f);
        } else {
            cardTabAi.setScaleX(1.0f);
            cardTabAi.setScaleY(1.0f);
            cardTabAi.setAlpha(0.6f);
            cardTabHuman.setScaleX(1.1f);
            cardTabHuman.setScaleY(1.1f);
            cardTabHuman.setAlpha(1.0f);
        }

        Fragment fragment = isAiActive ? new SkinAiChatFragment() : new SkinChatFragment();

        androidx.fragment.app.FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        if (animate) {
            transaction.setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out
            );
        }
        transaction.replace(R.id.dialogContentContainer, fragment).commit();

        updateTabBadges();
    }

    public void updateTabBadges() {
        if (!isAdded()) return;

        if (isAiActive) {
            requireContext().getSharedPreferences("RootieQuizPrefs", android.content.Context.MODE_PRIVATE)
                .edit()
                .putBoolean("SKIN_AI_CHAT_UNREAD", false)
                .apply();
            tabBadgeAi.setVisibility(View.GONE);
        } else {
            boolean isAiUnread = requireContext().getSharedPreferences("RootieQuizPrefs", android.content.Context.MODE_PRIVATE)
                .getBoolean("SKIN_AI_CHAT_UNREAD", true);
            tabBadgeAi.setVisibility(isAiUnread ? View.VISIBLE : View.GONE);
        }

        String currentUserId = ProfileSession.getCurrentUserId(requireContext());
        if (currentUserId == null) currentUserId = "guest_user";
        String skinChatConvId = "chat_rootie_vn_" + currentUserId;
        com.veganbeauty.app.data.local.entities.ConversationEntity conv =
            com.veganbeauty.app.features.community.message.MessageHelper.getConversationById(requireContext(), skinChatConvId);
        boolean isHumanUnread = false;
        if (conv != null && conv.getUnreadBy() != null) {
            isHumanUnread = conv.getUnreadBy().contains(currentUserId);
        }

        if (!isAiActive) {
            tabBadgeHuman.setVisibility(View.GONE);
        } else {
            tabBadgeHuman.setVisibility(isHumanUnread ? View.VISIBLE : View.GONE);
        }
    }
}
