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

        adapter = new ComNotificationAdapter(item -> {
            viewModel.markAsRead(requireContext(), item.getId());
            switch (item.getType()) {
                case "POST":
                case "INTERACTION":
                    if (item.getPostId() != null && !item.getPostId().isEmpty()) {
                        boolean isNewsPost = true;
                        for (char c : item.getPostId().toCharArray()) {
                            if (!Character.isDigit(c)) {
                                isNewsPost = false;
                                break;
                            }
                        }
                        if (isNewsPost) {
                            com.veganbeauty.app.features.community.beauty_hub.CommunityNewsFragment newsFragment = com.veganbeauty.app.features.community.beauty_hub.CommunityNewsFragment.newInstance(item.getPostId());
                            getParentFragmentManager().beginTransaction()
                                    .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                                    .replace(R.id.main_container, newsFragment)
                                    .addToBackStack(null)
                                    .commit();
                        } else {
                            String targetUserId = item.getUserId() != null ? item.getUserId() : "test_001";
                            com.veganbeauty.app.features.community.profile.ProfilePostDetailFragment postDetailFragment = com.veganbeauty.app.features.community.profile.ProfilePostDetailFragment.newInstance(targetUserId, 0, 0, item.getPostId());
                            getParentFragmentManager().beginTransaction()
                                    .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                                    .replace(R.id.main_container, postDetailFragment)
                                    .addToBackStack(null)
                                    .commit();
                        }

                        boolean shouldShowComments = "COMMENT".equals(item.getActionType()) || "REPLY".equals(item.getActionType()) || "LIKE".equals(item.getActionType()) || (item.getContent() != null && item.getContent().contains("bình luận"));
                        if (shouldShowComments) {
                            View v = getView();
                            if (v != null) {
                                v.postDelayed(() -> {
                                    if (isAdded()) {
                                        com.veganbeauty.app.features.community.com_feed.CommunityCommentBottomSheet commentBottomSheet = com.veganbeauty.app.features.community.com_feed.CommunityCommentBottomSheet.newInstance(item.getPostId(), 5, item.getCommentId());
                                        commentBottomSheet.show(getParentFragmentManager(), com.veganbeauty.app.features.community.com_feed.CommunityCommentBottomSheet.TAG);
                                    }
                                }, 350);
                            }
                        }
                    }
                    break;
                case "ORDER":
                    if ("WITHDRAW".equals(item.getActionType())) {
                        WithdrawalDetailPlaceholderFragment wdFragment = WithdrawalDetailPlaceholderFragment.newInstance(
                                "#WD20260615",
                                "500.000đ",
                                item.getTime() + " • " + item.getDate(),
                                "Thành công"
                        );
                        getParentFragmentManager().beginTransaction()
                                .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                                .replace(R.id.main_container, wdFragment)
                                .addToBackStack(null)
                                .commit();
                    } else {
                        Toast.makeText(requireContext(), "Chi tiết đơn hàng: " + item.getUserName(), Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
        }, item -> {
            viewModel.deleteNotification(requireContext(), item.getId());
            Toast.makeText(requireContext(), "Đã xóa thông báo", Toast.LENGTH_SHORT).show();
        });

        _binding.rvNotifications.setAdapter(adapter);

        _binding.tabAll.setOnClickListener(v -> viewModel.selectTab("ALL"));
        _binding.tabUnread.setOnClickListener(v -> viewModel.selectTab("UNREAD"));
        _binding.tabPosts.setOnClickListener(v -> viewModel.selectTab("POST"));
        _binding.tabInteractions.setOnClickListener(v -> viewModel.selectTab("INTERACTION"));

        _binding.tvMarkAllRead.setOnClickListener(v -> {
            viewModel.markAllRead(requireContext());
            Toast.makeText(requireContext(), "Đã đánh dấu tất cả là đã đọc", Toast.LENGTH_SHORT).show();
        });

        String userAvatar = com.veganbeauty.app.data.local.ProfileSession.INSTANCE.getAvatar(requireContext());
        if (userAvatar != null && !userAvatar.isEmpty()) {
            com.bumptech.glide.Glide.with(this).load(userAvatar).placeholder(R.drawable.img_avatar).error(R.drawable.img_avatar).circleCrop().into(_binding.ivUserAvatar);
        } else {
            com.bumptech.glide.Glide.with(this).load(R.drawable.img_avatar).circleCrop().into(_binding.ivUserAvatar);
        }

        _binding.ivUserAvatar.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(requireContext(), com.veganbeauty.app.MainActivity.class);
            intent.putExtra("navigateToTab", "profile");
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP | android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });

        // Swipe LEFT to reveal delete (red) + mark-read (green) buttons
        int DELETE_BTN_WIDTH = (int) (80 * getResources().getDisplayMetrics().density);
        int MARK_BTN_WIDTH   = (int) (80 * getResources().getDisplayMetrics().density);
        int TOTAL_WIDTH = DELETE_BTN_WIDTH + MARK_BTN_WIDTH;

        androidx.recyclerview.widget.ItemTouchHelper.SimpleCallback swipeCallback =
            new androidx.recyclerview.widget.ItemTouchHelper.SimpleCallback(0, androidx.recyclerview.widget.ItemTouchHelper.LEFT) {
                @Override
                public boolean onMove(@NonNull androidx.recyclerview.widget.RecyclerView rv,
                                      @NonNull androidx.recyclerview.widget.RecyclerView.ViewHolder vh,
                                      @NonNull androidx.recyclerview.widget.RecyclerView.ViewHolder target) {
                    return false;
                }

                @Override
                public void onSwiped(@NonNull androidx.recyclerview.widget.RecyclerView.ViewHolder vh, int dir) {
                    // snap back immediately; tap on drawn buttons is handled in onChildDraw via touch interceptor
                    adapter.notifyItemChanged(vh.getAdapterPosition());
                }

                @Override
                public float getSwipeThreshold(@NonNull androidx.recyclerview.widget.RecyclerView.ViewHolder vh) {
                    return 2f; // never auto-dismiss, user must tap icon
                }

                @Override
                public void onChildDraw(@NonNull android.graphics.Canvas c,
                                        @NonNull androidx.recyclerview.widget.RecyclerView rv,
                                        @NonNull androidx.recyclerview.widget.RecyclerView.ViewHolder vh,
                                        float dX, float dY, int actionState, boolean isActive) {
                    if (!(vh instanceof ComNotificationAdapter.NotificationViewHolder)) {
                        super.onChildDraw(c, rv, vh, dX, dY, actionState, isActive);
                        return;
                    }
                    View item = vh.itemView;
                    float absDx = Math.abs(dX);
                    if (absDx == 0) { super.onChildDraw(c, rv, vh, dX, dY, actionState, isActive); return; }

                    // Clamp dX so the item doesn't scroll further than needed
                    float clampedDx = Math.max(dX, -TOTAL_WIDTH);

                    // Draw delete (red) background on the far right
                    android.graphics.Paint deletePaint = new android.graphics.Paint();
                    deletePaint.setColor(android.graphics.Color.parseColor("#F44336"));
                    c.drawRect(item.getRight() - DELETE_BTN_WIDTH, item.getTop(), item.getRight(), item.getBottom(), deletePaint);

                    // Draw mark-read (teal/green) background just to the left of delete
                    android.graphics.Paint readPaint = new android.graphics.Paint();
                    readPaint.setColor(android.graphics.Color.parseColor("#4CAF50"));
                    c.drawRect(item.getRight() - TOTAL_WIDTH, item.getTop(), item.getRight() - DELETE_BTN_WIDTH, item.getBottom(), readPaint);

                    // Draw icons
                    int iconSize = (int) (24 * getResources().getDisplayMetrics().density);
                    int iconPad  = (item.getHeight() - iconSize) / 2;

                    // Trash icon (delete)
                    android.graphics.drawable.Drawable trash = ContextCompat.getDrawable(requireContext(), R.drawable.ic_bin);
                    if (trash != null) {
                        trash.setTint(android.graphics.Color.WHITE);
                        int left = item.getRight() - DELETE_BTN_WIDTH + (DELETE_BTN_WIDTH - iconSize) / 2;
                        trash.setBounds(left, item.getTop() + iconPad, left + iconSize, item.getTop() + iconPad + iconSize);
                        trash.draw(c);
                    }

                    // Check icon (mark read)
                    android.graphics.drawable.Drawable check = ContextCompat.getDrawable(requireContext(), R.drawable.ic_circle_checked);
                    if (check != null) {
                        check.setTint(android.graphics.Color.WHITE);
                        int left = item.getRight() - TOTAL_WIDTH + (MARK_BTN_WIDTH - iconSize) / 2;
                        check.setBounds(left, item.getTop() + iconPad, left + iconSize, item.getTop() + iconPad + iconSize);
                        check.draw(c);
                    }

                    super.onChildDraw(c, rv, vh, clampedDx, dY, actionState, isActive);
                }
            };

        androidx.recyclerview.widget.ItemTouchHelper touchHelper = new androidx.recyclerview.widget.ItemTouchHelper(swipeCallback);
        touchHelper.attachToRecyclerView(_binding.rvNotifications);

        // Touch interceptor to detect taps on the swipe-revealed buttons
        _binding.rvNotifications.addOnItemTouchListener(new androidx.recyclerview.widget.RecyclerView.OnItemTouchListener() {
            private float startX, startY;
            @Override
            public boolean onInterceptTouchEvent(@NonNull androidx.recyclerview.widget.RecyclerView rv, @NonNull android.view.MotionEvent e) {
                if (e.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                    startX = e.getX(); startY = e.getY();
                } else if (e.getAction() == android.view.MotionEvent.ACTION_UP) {
                    float dx = Math.abs(e.getX() - startX);
                    float dy = Math.abs(e.getY() - startY);
                    if (dx < 10 && dy < 10) {
                        float touchX = e.getX();
                        float touchY = e.getY();
                        // Find which ViewHolder was touched
                        View child = rv.findChildViewUnder(touchX + TOTAL_WIDTH, touchY);
                        if (child != null) {
                            androidx.recyclerview.widget.RecyclerView.ViewHolder vh = rv.getChildViewHolder(child);
                            if (vh instanceof ComNotificationAdapter.NotificationViewHolder) {
                                ComNotificationItem notifItem = ((ComNotificationAdapter.NotificationViewHolder) vh).getBoundItem();
                                if (notifItem != null) {
                                    float itemRight = child.getRight();
                                    // Delete zone
                                    if (touchX >= itemRight - DELETE_BTN_WIDTH && touchX <= itemRight) {
                                        new android.app.AlertDialog.Builder(requireContext())
                                            .setTitle("Xóa thông báo")
                                            .setMessage("Bạn có chắc chắn muốn xóa thông báo này?")
                                            .setPositiveButton("Xóa", (d, w) -> {
                                                viewModel.deleteNotification(requireContext(), notifItem.getId());
                                                Toast.makeText(requireContext(), "Đã xóa thông báo", Toast.LENGTH_SHORT).show();
                                            })
                                            .setNegativeButton("Hủy", (d, w) -> adapter.notifyItemChanged(vh.getAdapterPosition()))
                                            .setOnCancelListener(d -> adapter.notifyItemChanged(vh.getAdapterPosition()))
                                            .show();
                                        return true;
                                    }
                                    // Mark-read zone
                                    if (touchX >= itemRight - TOTAL_WIDTH && touchX < itemRight - DELETE_BTN_WIDTH) {
                                        viewModel.markAsRead(requireContext(), notifItem.getId());
                                        Toast.makeText(requireContext(), "Đã đánh dấu đã đọc", Toast.LENGTH_SHORT).show();
                                        adapter.notifyItemChanged(vh.getAdapterPosition());
                                        return true;
                                    }
                                }
                            }
                        }
                    }
                }
                return false;
            }
            @Override public void onTouchEvent(@NonNull androidx.recyclerview.widget.RecyclerView rv, @NonNull android.view.MotionEvent e) {}
            @Override public void onRequestDisallowInterceptTouchEvent(boolean b) {}
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
        boolean postsActive = "POST".equals(activeTab);
        boolean interactionsActive = "INTERACTION".equals(activeTab);

        int activeBg = R.drawable.bg_btn_buy;
        int inactiveBg = R.drawable.tab_inactive_bg;
        _binding.tabAll.setBackgroundResource(allActive ? activeBg : inactiveBg);
        _binding.tabUnread.setBackgroundResource(unreadActive ? activeBg : inactiveBg);
        _binding.tabPosts.setBackgroundResource(postsActive ? activeBg : inactiveBg);
        _binding.tabInteractions.setBackgroundResource(interactionsActive ? activeBg : inactiveBg);

        int white = ContextCompat.getColor(requireContext(), R.color.white);
        int primary = ContextCompat.getColor(requireContext(), R.color.primary);
        _binding.tabAll.setTextColor(allActive ? white : primary);
        _binding.tabUnread.setTextColor(unreadActive ? white : primary);
        _binding.tabPosts.setTextColor(postsActive ? white : primary);
        _binding.tabInteractions.setTextColor(interactionsActive ? white : primary);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        _binding = null;
    }
}

