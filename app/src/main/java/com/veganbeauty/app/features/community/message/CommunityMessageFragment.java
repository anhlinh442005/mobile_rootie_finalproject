package com.veganbeauty.app.features.community.message;



import android.content.Context;

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

import com.veganbeauty.app.databinding.ComFragmentMessageBinding;

import com.veganbeauty.app.features.community.beauty_hub.CommunityBeautyHubFragment;

import com.veganbeauty.app.features.community.com_feed.CommunityExploreFragment;

import com.veganbeauty.app.features.community.com_feed.CommunityFeedFragment;

import com.veganbeauty.app.features.community.profile.CommunityProfileFragment;



import java.util.ArrayList;

import java.util.List;



public class CommunityMessageFragment extends RootieFragment {



    private ComFragmentMessageBinding binding;

    private MessageAdapter messageAdapter;

    private ActiveUserAdapter activeUserAdapter;

    private String currentUserId = "";



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



            binding.btnBack.setOnClickListener(v -> {

                if (getParentFragmentManager().getBackStackEntryCount() > 0) {

                    getParentFragmentManager().popBackStack();

                }

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

                    // Filter logic

                }

            });



            setupBottomNav();

            loadConversations();

        } catch (Exception e) {

            e.printStackTrace();

        }

    }



    private void setupBottomNav() {

        if (binding == null || binding.comBottomNav == null) return;



        binding.comBottomNav.navComFeed.setOnClickListener(v ->

                getParentFragmentManager().beginTransaction()

                        .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)

                        .replace(R.id.main_container, new CommunityFeedFragment())

                        .commitAllowingStateLoss());



        binding.comBottomNav.navComHub.setOnClickListener(v ->

                getParentFragmentManager().beginTransaction()

                        .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)

                        .replace(R.id.main_container, new CommunityBeautyHubFragment())

                        .commitAllowingStateLoss());



        binding.comBottomNav.navComExplore.setOnClickListener(v ->

                getParentFragmentManager().beginTransaction()

                        .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)

                        .replace(R.id.main_container, new CommunityExploreFragment())

                        .commitAllowingStateLoss());



        binding.comBottomNav.navComProfile.setOnClickListener(v ->

                getParentFragmentManager().beginTransaction()

                        .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)

                        .replace(R.id.main_container, new CommunityProfileFragment())

                        .commitAllowingStateLoss());

    }



    private void loadConversations() {

        Context appCtx = getContext();

        if (appCtx == null) return;

        final Context context = appCtx.getApplicationContext();
        final String userId = currentUserId;

        MessageHelper.syncConversationsFromAssets(context);

        MessageHelper.fetchAndMergeFirebaseConversations(context, userId, () -> {

            if (!isAdded()) return;

            if (getActivity() == null) return;

            getActivity().runOnUiThread(() -> {

                if (!isAdded() || activeUserAdapter == null || messageAdapter == null) return;

                List<ConversationEntity> list = MessageHelper.getConversations(context, userId);

                activeUserAdapter.updateList(list);

                messageAdapter.updateList(list);

            });

        });

    }



    private void openChat(String conversationId) {

        if (conversationId == null || conversationId.isEmpty()) return;

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


