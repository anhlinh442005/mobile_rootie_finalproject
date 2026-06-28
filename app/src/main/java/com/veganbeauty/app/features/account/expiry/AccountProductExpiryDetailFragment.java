package com.veganbeauty.app.features.account.expiry;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwnerKt;


import com.veganbeauty.app.R;
import com.veganbeauty.app.core.base.RootieFragment;
import com.veganbeauty.app.data.local.LocalJsonReader;
import com.veganbeauty.app.data.local.ProfileSession;
import com.veganbeauty.app.data.local.RootieDatabase;
import com.veganbeauty.app.data.local.entities.ProductEntity;
import com.veganbeauty.app.data.repository.ProductRepository;
import com.veganbeauty.app.databinding.AccountProductExpiryDetailFragmentBinding;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import kotlinx.coroutines.BuildersKt;
import kotlinx.coroutines.Dispatchers;

public class AccountProductExpiryDetailFragment extends RootieFragment {

    private AccountProductExpiryDetailFragmentBinding binding;
    private ProductRepository repository;
    private String productId = null;
    private boolean isWeek1Checked = false;
    private boolean isWeek2Checked = false;
    private boolean isNotificationChecked = false;

    private final ActivityResultLauncher<String> requestNotificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Toast.makeText(getContext(), "Đã cấp quyền thông báo!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Vui lòng cấp quyền thông báo trong cài đặt để nhận nhắc nhở!", Toast.LENGTH_LONG).show();
                }
            });

    private Date baselineDate;

    private static final String ARG_PRODUCT_ID = "PRODUCT_ID";

    public static AccountProductExpiryDetailFragment newInstance(String productId) {
        AccountProductExpiryDetailFragment fragment = new AccountProductExpiryDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PRODUCT_ID, productId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            productId = getArguments().getString(ARG_PRODUCT_ID);
        }
        RootieDatabase db = RootieDatabase.getDatabase(requireContext());
        repository = new ProductRepository(
                db.productDao(),
                new LocalJsonReader(requireContext()),
                new com.veganbeauty.app.data.remote.FirestoreService(),
                db.userProductExpiryDao()
        );

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        try {
            baselineDate = sdf.parse("04/06/2026");
        } catch (Exception e) {
            baselineDate = new Date();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = AccountProductExpiryDetailFragmentBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    protected void setupUI(@NonNull View view) {
        binding.btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        if (productId != null) {
            java.util.concurrent.Executors.newSingleThreadExecutor().execute(() -> {
                String userId = ProfileSession.getUserId(requireContext());
                com.veganbeauty.app.data.local.entities.ProductEntity product = repository.getExpiryProductById(userId, productId);
                if (product != null) {
                    requireActivity().runOnUiThread(() -> bindProductDetails(product));
                }
            });
        }

        String userId = ProfileSession.getUserId(requireContext());
        String pId = (productId != null) ? productId : "";

        isNotificationChecked = ProfileSession.getProductNotiEnabled(requireContext(), userId, pId);
        isWeek1Checked = ProfileSession.getProductWeek1Enabled(requireContext(), userId, pId);
        isWeek2Checked = ProfileSession.getProductWeek2Enabled(requireContext(), userId, pId);

        updateSwitchUI(binding.switchNotification, binding.switchNotificationThumb, isNotificationChecked);
        updateSwitchUI(binding.switchWeek1, binding.switchWeek1Thumb, isWeek1Checked);
        updateSwitchUI(binding.switchWeek2, binding.switchWeek2Thumb, isWeek2Checked);

        binding.switchWeek1.setEnabled(isNotificationChecked);
        binding.switchWeek1.setAlpha(isNotificationChecked ? 1.0f : 0.5f);
        binding.switchWeek2.setEnabled(isNotificationChecked);
        binding.switchWeek2.setAlpha(isNotificationChecked ? 1.0f : 0.5f);

        binding.switchWeek1.setOnClickListener(v -> {
            if (!isNotificationChecked) return;
            isWeek1Checked = !isWeek1Checked;
            updateSwitchUI(binding.switchWeek1, binding.switchWeek1Thumb, isWeek1Checked);
            ProfileSession.setProductWeek1Enabled(requireContext(), userId, pId, isWeek1Checked);
            if (isWeek1Checked) {
                checkAndRequestNotiPermission(() -> {
                    String productName = binding.tvProductName.getText().toString();
                    triggerCustomExpiryNotification(1, productName);
                });
            } else {
                Toast.makeText(getContext(), "Tắt nhắc nhở trước 1 tuần", Toast.LENGTH_SHORT).show();
            }
        });

        binding.switchWeek2.setOnClickListener(v -> {
            if (!isNotificationChecked) return;
            isWeek2Checked = !isWeek2Checked;
            updateSwitchUI(binding.switchWeek2, binding.switchWeek2Thumb, isWeek2Checked);
            ProfileSession.setProductWeek2Enabled(requireContext(), userId, pId, isWeek2Checked);
            if (isWeek2Checked) {
                checkAndRequestNotiPermission(() -> {
                    String productName = binding.tvProductName.getText().toString();
                    triggerCustomExpiryNotification(2, productName);
                });
            } else {
                Toast.makeText(getContext(), "Tắt nhắc nhở trước 2 tuần", Toast.LENGTH_SHORT).show();
            }
        });

        binding.switchNotification.setOnClickListener(v -> {
            isNotificationChecked = !isNotificationChecked;
            updateSwitchUI(binding.switchNotification, binding.switchNotificationThumb, isNotificationChecked);
            ProfileSession.setProductNotiEnabled(requireContext(), userId, pId, isNotificationChecked);

            binding.switchWeek1.setEnabled(isNotificationChecked);
            binding.switchWeek1.setAlpha(isNotificationChecked ? 1.0f : 0.5f);
            binding.switchWeek2.setEnabled(isNotificationChecked);
            binding.switchWeek2.setAlpha(isNotificationChecked ? 1.0f : 0.5f);

            if (!isNotificationChecked) {
                isWeek1Checked = false;
                isWeek2Checked = false;
                updateSwitchUI(binding.switchWeek1, binding.switchWeek1Thumb, false);
                updateSwitchUI(binding.switchWeek2, binding.switchWeek2Thumb, false);
                ProfileSession.setProductWeek1Enabled(requireContext(), userId, pId, false);
                ProfileSession.setProductWeek2Enabled(requireContext(), userId, pId, false);
            }
            String status = isNotificationChecked ? "Bật" : "Tắt";
            Toast.makeText(getContext(), status + " nhận thông báo hạn sử dụng", Toast.LENGTH_SHORT).show();
        });
    }

    private void checkAndRequestNotiPermission(Runnable onGranted) {
        Context context = requireContext();
        boolean systemNotificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled();
        if (!systemNotificationsEnabled) {
            Toast.makeText(context, "Thông báo hiện đang bị tắt. Vui lòng bật trong Cài đặt hệ thống.", Toast.LENGTH_LONG).show();
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                onGranted.run();
            } else {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        } else {
            onGranted.run();
        }
    }

    private void triggerCustomExpiryNotification(int weeks, String productName) {
        Context context = requireContext();
        String titleText = "Chỉ còn " + weeks + " tuần!";
        String messageText = productName + " của bạn sắp hết hạn. Kiểm tra ngay!";

        if (binding != null) {
            binding.tvNotiTitle.setText(titleText);
            binding.tvNotiMessage.setText(messageText);
            binding.cvNotificationBanner.setVisibility(View.VISIBLE);
            binding.cvNotificationBanner.setTranslationY(-300f);

            binding.cvNotificationBanner.animate()
                    .translationY(0f)
                    .setDuration(500)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (binding != null) {
                binding.cvNotificationBanner.animate()
                        .translationY(-300f)
                        .setDuration(500)
                        .withEndAction(() -> {
                            if (binding != null) {
                                binding.cvNotificationBanner.setVisibility(View.GONE);
                            }
                        })
                        .start();
            }
        }, 5000);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = "rootie_expiry_channel";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String name = "Nhắc nhở hết hạn sản phẩm";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(channelId, name, importance);
            channel.setDescription("Thông báo nhắc nhở hạn sử dụng mỹ phẩm");
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(titleText)
                .setContentText(messageText)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        try {
            if (notificationManager != null) {
                notificationManager.notify(weeks * 1000 + 99, builder.build());
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    private void bindProductDetails(ProductEntity product) {
        binding.tvProductName.setText(product.getName());
        com.bumptech.glide.Glide.with(binding.ivProductImage.getContext()).load(product.getMainImage()).placeholder(android.R.color.darker_gray).error(android.R.color.darker_gray).into(binding.ivProductImage);

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        Date expiry = null;
        try {
            if (product.getExpiryDate() != null) {
                expiry = sdf.parse(product.getExpiryDate());
            }
        } catch (Exception e) {
            expiry = null;
        }

        if (expiry == null) {
            binding.tvPurchaseDate.setText("Mua hàng: Không xác định");
            binding.tvTotalShelfLife.setText("Không xác định");
            binding.tvRemainingValue.setText("--");
            binding.tvRemainingUnit.setText("");
            binding.circularProgress.setProgress(0f);
            return;
        }

        Calendar cal = Calendar.getInstance();
        cal.setTime(expiry);
        cal.add(Calendar.MONTH, -18);
        Date purchaseDate = cal.getTime();

        binding.tvPurchaseDate.setText("Mua hàng: " + sdf.format(purchaseDate));
        binding.tvTotalShelfLife.setText("18 tháng");

        long diffMs = expiry.getTime() - baselineDate.getTime();
        int diffDays = (int) (diffMs / (1000 * 60 * 60 * 24));

        String valueText;
        String unitText;
        float ratio;

        if (diffDays <= 0) {
            valueText = "0";
            unitText = "ngày";
            ratio = 0.0f;
        } else if (diffDays < 30) {
            int weeks = diffDays / 7;
            if (weeks > 0) {
                valueText = String.format(Locale.getDefault(), "%02d", weeks);
                unitText = "tuần";
            } else {
                valueText = String.format(Locale.getDefault(), "%02d", diffDays);
                unitText = "ngày";
            }
            ratio = (float) diffDays / (18f * 30f);
        } else {
            int months = diffDays / 30;
            valueText = String.format(Locale.getDefault(), "%02d", months);
            unitText = "tháng";
            ratio = (float) diffDays / (18f * 30f);
        }

        binding.tvRemainingValue.setText(valueText);
        binding.tvRemainingUnit.setText(unitText);
        binding.circularProgress.setProgress(ratio);

        int progressColor;
        if (diffDays <= 0) {
            progressColor = Color.parseColor("#8E8E8E");
        } else if (diffDays <= 14) {
            progressColor = Color.parseColor("#C62828");
        } else {
            progressColor = Color.parseColor("#3E4D44");
        }
        binding.circularProgress.setProgressColor(progressColor);
    }

    private void updateSwitchUI(FrameLayout container, ImageView thumb, boolean enabled) {
        if (enabled) {
            container.setBackgroundResource(R.drawable.ic_switch_track_on);
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) thumb.getLayoutParams();
            lp.gravity = Gravity.CENTER_VERTICAL | Gravity.END;
            lp.setMarginStart(0);
            lp.setMarginEnd((int) (2 * getResources().getDisplayMetrics().density));
            thumb.setLayoutParams(lp);
        } else {
            container.setBackgroundResource(R.drawable.ic_switch_track_off);
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) thumb.getLayoutParams();
            lp.gravity = Gravity.CENTER_VERTICAL | Gravity.START;
            lp.setMarginEnd(0);
            lp.setMarginStart((int) (2 * getResources().getDisplayMetrics().density));
            thumb.setLayoutParams(lp);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
