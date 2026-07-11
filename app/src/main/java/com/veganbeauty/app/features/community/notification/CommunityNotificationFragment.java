package com.veganbeauty.app.features.community.notification;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.veganbeauty.app.R;
import com.veganbeauty.app.core.base.RootieFragment;
import com.veganbeauty.app.databinding.ComFragmentNotificationBinding;
import com.veganbeauty.app.utils.AvatarLoader;
import com.veganbeauty.app.utils.ProfileSessionHelper;

public class CommunityNotificationFragment extends RootieFragment {

    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Toast.makeText(requireContext(), "Đã cấp quyền nhận thông báo", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(requireContext(), "Bạn chưa cấp quyền nhận thông báo", Toast.LENGTH_SHORT).show();
                }
            }
    );

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    private ComFragmentNotificationBinding _binding;

    private ComNotificationViewModel viewModel;

    private ComNotificationAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        _binding = ComFragmentNotificationBinding.inflate(inflater, container, false);
        setupViewModel();
        return _binding.getRoot();
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(ComNotificationViewModel.class);
        viewModel.initData(requireContext());
    }

    @Override
    protected void setupUI(@NonNull View view) {
        checkNotificationPermission();

        _binding.btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        adapter = new ComNotificationAdapter(
            // onItemClick
            item -> {
                viewModel.markAsRead(requireContext(), item.getId());
                switch (item.getType()) {
                    case "POST":
                    case "INTERACTION":
                        if (item.getPostId() != null && !item.getPostId().isEmpty()) {
                            boolean isNewsPost = true;
                            for (char c : item.getPostId().toCharArray()) {
                                if (!Character.isDigit(c)) { isNewsPost = false; break; }
                            }
                            if (isNewsPost) {
                                com.veganbeauty.app.features.community.beauty_hub.CommunityNewsFragment newsFragment =
                                    com.veganbeauty.app.features.community.beauty_hub.CommunityNewsFragment.newInstance(item.getPostId());
                                getParentFragmentManager().beginTransaction()
                                    .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                                    .replace(R.id.main_container, newsFragment).addToBackStack(null).commit();
                            } else {
                                String uid = item.getUserId() != null ? item.getUserId() : ProfileSessionHelper.getEffectiveUserId(requireContext());
                                com.veganbeauty.app.features.community.profile.ProfilePostDetailFragment postFrag =
                                    com.veganbeauty.app.features.community.profile.ProfilePostDetailFragment.newInstance(uid, 0, 0, item.getPostId());
                                getParentFragmentManager().beginTransaction()
                                    .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                                    .replace(R.id.main_container, postFrag).addToBackStack(null).commit();
                            }
                        }
                        break;
                }
            },
            // onDeleteClick
            item -> new android.app.AlertDialog.Builder(requireContext())
                .setTitle("Xóa thông báo")
                .setMessage("Bạn có chắc chắn muốn xóa thông báo này?")
                .setPositiveButton("Xóa", (d, w) -> {
                    viewModel.deleteNotification(requireContext(), item.getId());
                    Toast.makeText(requireContext(), "Đã xóa thông báo", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Hủy", null)
                .show(),
            // onMarkReadClick
            item -> {
                viewModel.markAsRead(requireContext(), item.getId());
                Toast.makeText(requireContext(), "Đã đánh dấu đã đọc", Toast.LENGTH_SHORT).show();
            }
        );

        _binding.rvNotifications.setAdapter(adapter);

        // ── Swipe gesture: swipe left to open, release to stay open ──
        final float actionWidthPx = 128 * getResources().getDisplayMetrics().density;
        final float SWIPE_THRESHOLD = actionWidthPx * 0.3f;

        _binding.rvNotifications.addOnItemTouchListener(new androidx.recyclerview.widget.RecyclerView.OnItemTouchListener() {
            private float startX, startY, lastX, initialTx;
            private boolean isSwiping = false;
            private ComNotificationAdapter.NotificationViewHolder swipeHolder = null;

            @Override
            public boolean onInterceptTouchEvent(@NonNull androidx.recyclerview.widget.RecyclerView rv, @NonNull android.view.MotionEvent e) {
                switch (e.getAction()) {
                    case android.view.MotionEvent.ACTION_DOWN:
                        startX = e.getX(); startY = e.getY(); lastX = startX; isSwiping = false; swipeHolder = null; initialTx = 0f;
                        View child = rv.findChildViewUnder(startX, startY);
                        if (child != null) {
                            androidx.recyclerview.widget.RecyclerView.ViewHolder vh = rv.getChildViewHolder(child);
                            if (vh instanceof ComNotificationAdapter.NotificationViewHolder) {
                                swipeHolder = (ComNotificationAdapter.NotificationViewHolder) vh;
                                initialTx = swipeHolder.getForeground().getTranslationX();
                            }
                        }
                        break;
                    case android.view.MotionEvent.ACTION_MOVE:
                        if (swipeHolder == null) break;
                        float dx = e.getX() - startX;
                        float dy = e.getY() - startY;
                        if (!isSwiping && Math.abs(dx) > Math.abs(dy) && Math.abs(dx) > 12) {
                            if (dx < 0 || (initialTx < 0 && dx > 0)) {
                                isSwiping = true;
                                rv.getParent().requestDisallowInterceptTouchEvent(true);
                            }
                        }
                        break;
                }
                return isSwiping;
            }

            @Override
            public void onTouchEvent(@NonNull androidx.recyclerview.widget.RecyclerView rv, @NonNull android.view.MotionEvent e) {
                if (swipeHolder == null) return;
                float curX = e.getX();
                float totalDx = curX - startX;

                switch (e.getAction()) {
                    case android.view.MotionEvent.ACTION_MOVE:
                        if (isSwiping) {
                            float tx = Math.max(-actionWidthPx, Math.min(0f, initialTx + totalDx));
                            swipeHolder.getForeground().setTranslationX(tx);
                        }
                        lastX = curX;
                        break;

                    case android.view.MotionEvent.ACTION_UP:
                    case android.view.MotionEvent.ACTION_CANCEL:
                        if (isSwiping) {
                            float finalTx = swipeHolder.getForeground().getTranslationX();
                            if (finalTx < -SWIPE_THRESHOLD) {
                                // Snap fully open
                                adapter.openItem(swipeHolder, actionWidthPx);
                            } else {
                                // Snap closed
                                adapter.closeOpenedItem();
                                swipeHolder.animateForeground(0f);
                            }
                            rv.getParent().requestDisallowInterceptTouchEvent(false);
                        }
                        isSwiping = false; swipeHolder = null;
                        break;
                }
            }

            @Override
            public void onRequestDisallowInterceptTouchEvent(boolean b) {}
        });


        _binding.tabAll.setOnClickListener(v -> viewModel.selectTab("ALL"));
        _binding.tabUnread.setOnClickListener(v -> viewModel.selectTab("UNREAD"));
        _binding.tabInteractions.setOnClickListener(v -> viewModel.selectTab("INTERACTION"));
        _binding.tabAffiliate.setOnClickListener(v -> viewModel.selectTab("AFFILIATE"));
        _binding.tabNews.setOnClickListener(v -> viewModel.selectTab("NEWS"));

        _binding.tvMarkAllRead.setOnClickListener(v -> {
            viewModel.markAllRead(requireContext());
            Toast.makeText(requireContext(), "Đã đánh dấu tất cả là đã đọc", Toast.LENGTH_SHORT).show();
        });

        bindUserAvatar();

        _binding.ivUserAvatar.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(requireContext(), com.veganbeauty.app.MainActivity.class);
            intent.putExtra("navigateToTab", "profile");
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP | android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });

        _binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                viewModel.setSearchQuery(s != null ? s.toString() : "");
            }
        });
    }

    @Override
    protected void observeViewModel() {
        viewModel.getFilteredNotifications().observe(getViewLifecycleOwner(), items -> {
            if (items == null || items.isEmpty()) {
                _binding.rvNotifications.setVisibility(View.GONE);
                _binding.layoutEmptyState.setVisibility(View.VISIBLE);
            } else {
                _binding.rvNotifications.setVisibility(View.VISIBLE);
                _binding.layoutEmptyState.setVisibility(View.GONE);
                adapter.submitList(items);
            }
        });

        viewModel.getActiveTab().observe(getViewLifecycleOwner(), this::updateTabStyles);
    }

    private void updateTabStyles(String activeTab) {
        boolean allActive = "ALL".equals(activeTab);
        boolean unreadActive = "UNREAD".equals(activeTab);
        boolean interactionsActive = "INTERACTION".equals(activeTab);
        boolean affiliateActive = "AFFILIATE".equals(activeTab);
        boolean newsActive = "NEWS".equals(activeTab);

        int activeBg = R.drawable.tab_active_bg;
        int inactiveBg = R.drawable.tab_inactive_bg;
        _binding.tabAll.setBackgroundResource(allActive ? activeBg : inactiveBg);
        _binding.tabUnread.setBackgroundResource(unreadActive ? activeBg : inactiveBg);
        _binding.tabInteractions.setBackgroundResource(interactionsActive ? activeBg : inactiveBg);
        _binding.tabAffiliate.setBackgroundResource(affiliateActive ? activeBg : inactiveBg);
        _binding.tabNews.setBackgroundResource(newsActive ? activeBg : inactiveBg);

        int white = ContextCompat.getColor(requireContext(), R.color.white);
        int primary = ContextCompat.getColor(requireContext(), R.color.primary);
        _binding.tabAll.setTextColor(allActive ? white : primary);
        _binding.tabUnread.setTextColor(unreadActive ? white : primary);
        _binding.tabInteractions.setTextColor(interactionsActive ? white : primary);
        _binding.tabAffiliate.setTextColor(affiliateActive ? white : primary);
        _binding.tabNews.setTextColor(newsActive ? white : primary);
    }

    private void bindUserAvatar() {
        if (_binding == null || !isAdded()) return;
        String avatarUrl = ProfileSessionHelper.getAccountProfileAvatarUrl(requireContext());
        AvatarLoader.loadAvatar(_binding.ivUserAvatar, avatarUrl);
    }

    @Override
    public void onResume() {
        super.onResume();
        bindUserAvatar();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        _binding = null;
    }
}

