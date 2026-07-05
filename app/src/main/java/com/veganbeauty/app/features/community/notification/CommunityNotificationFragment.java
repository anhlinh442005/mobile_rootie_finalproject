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
        _binding.tabPosts.setOnClickListener(v -> viewModel.selectTab("POST"));
        _binding.tabInteractions.setOnClickListener(v -> viewModel.selectTab("INTERACTION"));

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

        androidx.recyclerview.widget.ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new androidx.recyclerview.widget.ItemTouchHelper.SimpleCallback(0, androidx.recyclerview.widget.ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull androidx.recyclerview.widget.RecyclerView recyclerView, @NonNull androidx.recyclerview.widget.RecyclerView.ViewHolder viewHolder, @NonNull androidx.recyclerview.widget.RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull androidx.recyclerview.widget.RecyclerView.ViewHolder viewHolder, int swipeDir) {
                if (viewHolder instanceof ComNotificationAdapter.NotificationViewHolder) {
                    ComNotificationItem item = ((ComNotificationAdapter.NotificationViewHolder) viewHolder).getBoundItem();
                    if (item != null) {
                        new android.app.AlertDialog.Builder(requireContext())
                            .setTitle("Xóa thông báo")
                            .setMessage("Bạn có chắc chắn muốn xóa thông báo này?")
                            .setPositiveButton("Xóa", (dialog, which) -> {
                                viewModel.deleteNotification(requireContext(), item.getId());
                                Toast.makeText(requireContext(), "Đã xóa thông báo", Toast.LENGTH_SHORT).show();
                            })
                            .setNegativeButton("Hủy", (dialog, which) -> {
                                adapter.notifyItemChanged(viewHolder.getAdapterPosition());
                            })
                            .setOnCancelListener(dialog -> {
                                adapter.notifyItemChanged(viewHolder.getAdapterPosition());
                            })
                            .show();
                    }
                } else {
                    adapter.notifyItemChanged(viewHolder.getAdapterPosition());
                }
            }

            @Override
            public void onChildDraw(@NonNull android.graphics.Canvas c, @NonNull androidx.recyclerview.widget.RecyclerView recyclerView, @NonNull androidx.recyclerview.widget.RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                if (viewHolder instanceof ComNotificationAdapter.NotificationViewHolder) {
                    View itemView = viewHolder.itemView;
                    android.graphics.drawable.ColorDrawable background = new android.graphics.drawable.ColorDrawable(android.graphics.Color.parseColor("#FFCDD2"));
                    background.setBounds(itemView.getLeft(), itemView.getTop(), itemView.getLeft() + (int) dX, itemView.getBottom());
                    background.draw(c);

                    android.graphics.drawable.Drawable deleteIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_bin);
                    if (deleteIcon != null) {
                        deleteIcon.setTint(android.graphics.Color.parseColor("#B71C1C"));
                        int iconMargin = (itemView.getHeight() - deleteIcon.getIntrinsicHeight()) / 2;
                        int iconTop = itemView.getTop() + iconMargin;
                        int iconBottom = iconTop + deleteIcon.getIntrinsicHeight();
                        int iconLeft = itemView.getLeft() + iconMargin;
                        int iconRight = iconLeft + deleteIcon.getIntrinsicWidth();
                        if (dX > iconRight) {
                            deleteIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                            deleteIcon.draw(c);
                        }
                    }
                }
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        };

        androidx.recyclerview.widget.ItemTouchHelper itemTouchHelper = new androidx.recyclerview.widget.ItemTouchHelper(simpleItemTouchCallback);
        itemTouchHelper.attachToRecyclerView(_binding.rvNotifications);

        _binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
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
        boolean postsActive = "POST".equals(activeTab);
        boolean interactionsActive = "INTERACTION".equals(activeTab);

        _binding.tabAll.setBackgroundResource(allActive ? R.drawable.bg_btn_buy : R.drawable.tab_inactive_bg);
        _binding.tabPosts.setBackgroundResource(postsActive ? R.drawable.bg_btn_buy : R.drawable.tab_inactive_bg);
        _binding.tabInteractions.setBackgroundResource(interactionsActive ? R.drawable.bg_btn_buy : R.drawable.tab_inactive_bg);

        int whiteColor = ContextCompat.getColor(requireContext(), R.color.white);
        int primaryColor = ContextCompat.getColor(requireContext(), R.color.primary);

        _binding.tabAll.setTextColor(allActive ? whiteColor : primaryColor);
        _binding.tabPosts.setTextColor(postsActive ? whiteColor : primaryColor);
        _binding.tabInteractions.setTextColor(interactionsActive ? whiteColor : primaryColor);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        _binding = null;
    }
}
