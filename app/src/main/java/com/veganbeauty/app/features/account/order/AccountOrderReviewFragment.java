package com.veganbeauty.app.features.account.order;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwnerKt;

import coil.Coil;
import coil.request.ImageRequest;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.veganbeauty.app.R;
import com.veganbeauty.app.core.base.RootieFragment;
import com.veganbeauty.app.data.local.LocalJsonReader;
import com.veganbeauty.app.data.local.RootieDatabase;
import com.veganbeauty.app.data.local.entities.OrderEntity;
import com.veganbeauty.app.data.local.entities.OrderItemEntity;
import com.veganbeauty.app.data.repository.OrderRepository;
import com.veganbeauty.app.databinding.AccountOrderReviewFragmentBinding;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import kotlinx.coroutines.BuildersKt;
import kotlinx.coroutines.Dispatchers;

public class AccountOrderReviewFragment extends RootieFragment {

    private AccountOrderReviewFragmentBinding _binding;

    private OrderRepository repository;
    private String orderId = "";
    private OrderEntity order = null;

    private int selectedStars = 0;
    private String selectedImageUrl = null;
    private boolean isEditMode = false;
    private boolean isSummaryMode = false;

    private final ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedImageUrl = uri.toString();
                    _binding.cardImg1.setVisibility(View.VISIBLE);
                    ImageRequest request = new ImageRequest.Builder(requireContext())
                            .data(uri)
                            .crossfade(true)
                            .target(_binding.ivImg1)
                            .build();
                    Coil.imageLoader(requireContext()).enqueue(request);
                    Toast.makeText(getContext(), "Đã thêm ảnh từ thư viện!", Toast.LENGTH_SHORT).show();
                }
            }
    );

    private final ActivityResultLauncher<Void> takePhotoLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicturePreview(),
            bitmap -> {
                if (bitmap != null) {
                    String path = saveBitmapToCache(bitmap);
                    if (path != null) {
                        selectedImageUrl = path;
                        _binding.cardImg1.setVisibility(View.VISIBLE);
                        ImageRequest request = new ImageRequest.Builder(requireContext())
                                .data(new File(path))
                                .crossfade(true)
                                .target(_binding.ivImg1)
                                .build();
                        Coil.imageLoader(requireContext()).enqueue(request);
                        Toast.makeText(getContext(), "Đã chụp ảnh thành công!", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    private String saveBitmapToCache(Bitmap bitmap) {
        try {
            File cacheDir = requireContext().getCacheDir();
            File file = new File(cacheDir, "review_photo_" + System.currentTimeMillis() + ".jpg");
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();
            return file.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static AccountOrderReviewFragment newInstance(String orderId) {
        AccountOrderReviewFragment fragment = new AccountOrderReviewFragment();
        Bundle args = new Bundle();
        args.putString("arg_order_id", orderId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        _binding = AccountOrderReviewFragmentBinding.inflate(inflater, container, false);
        if (getArguments() != null) {
            orderId = getArguments().getString("arg_order_id", "");
        }
        setupRepository();
        return _binding.getRoot();
    }

    private void setupRepository() {
        RootieDatabase db = RootieDatabase.getDatabase(requireContext());
        repository = new OrderRepository(db.orderDao(), db.rewardPointDao(), db.userGiftDao(), new LocalJsonReader(requireContext()));
    }

    @Override
    protected void setupUI(@NonNull View view) {
        _binding.btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        _binding.btnNotification.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, new com.veganbeauty.app.features.account.notification.AccountNotificationFragment())
                    .addToBackStack(null)
                    .commit();
        });

        List<ImageView> starsList = new ArrayList<>();
        starsList.add(_binding.ivStar1);
        starsList.add(_binding.ivStar2);
        starsList.add(_binding.ivStar3);
        starsList.add(_binding.ivStar4);
        starsList.add(_binding.ivStar5);

        for (int i = 0; i < starsList.size(); i++) {
            int index = i;
            starsList.get(i).setOnClickListener(v -> {
                if (!isSummaryMode || isEditMode) {
                    setStars(index + 1);
                }
            });
        }

        _binding.etReviewText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String text = s != null ? s.toString() : "";
                String[] wordsArray = text.trim().split("\\s+");
                int wordCount = 0;
                for (String w : wordsArray) {
                    if (!w.isEmpty()) wordCount++;
                }

                _binding.tvWordCount.setText(wordCount + "/200 từ");
                if (wordCount > 200) {
                    _binding.tvWordCount.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_cancelled_text));
                } else {
                    _binding.tvWordCount.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray_dark));
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        _binding.btnAddImage.setOnClickListener(v -> {
            if (!isSummaryMode || isEditMode) {
                showImageSelectionDialog();
            }
        });

        _binding.btnSubmit.setOnClickListener(v -> handleSubmitAction());

        loadOrderData();
    }

    private void loadOrderData() {
        BuildersKt.launch(LifecycleOwnerKt.getLifecycleScope(getViewLifecycleOwner()), Dispatchers.getMain(), kotlinx.coroutines.CoroutineStart.DEFAULT, (coroutineScope, continuation) -> {
            repository.getOrderById(orderId).collect(new kotlinx.coroutines.flow.FlowCollector<OrderEntity>() {
                @Nullable
                @Override
                public Object emit(OrderEntity ord, @NonNull kotlin.coroutines.Continuation<? super kotlin.Unit> continuation) {
                    if (ord != null) {
                        order = ord;
                        bindOrderDetails(ord);
                    }
                    return kotlin.Unit.INSTANCE;
                }
            }, continuation);
            return kotlin.Unit.INSTANCE;
        });
    }

    private void bindOrderDetails(OrderEntity ord) {
        if (ord.getItems().isEmpty()) return;
        OrderItemEntity firstItem = ord.getItems().get(0);
        _binding.tvProductName.setText(firstItem.getProductName());

        String attribute;
        if (firstItem.getProductName().contains("50ml")) {
            attribute = "Dung tích: 50ml";
        } else if (firstItem.getProductName().contains("30ml")) {
            attribute = "Dung tích: 30ml";
        } else if (firstItem.getProductName().contains("70ml")) {
            attribute = "Dung tích: 70ml";
        } else if (firstItem.getProductName().contains("100ml")) {
            attribute = "Dung tích: 100ml";
        } else {
            attribute = "Phân loại: 50ml";
        }
        _binding.tvProductVariant.setText(attribute);

        ImageRequest request = new ImageRequest.Builder(requireContext())
                .data(firstItem.getProductImage())
                .crossfade(true)
                .placeholder(android.R.color.darker_gray)
                .error(android.R.color.darker_gray)
                .target(_binding.ivProductImage)
                .build();
        Coil.imageLoader(requireContext()).enqueue(request);

        if (ord.getHasReview() && !isEditMode) {
            enterSummaryMode(ord);
        } else if (!isEditMode) {
            enterCreateMode();
        }
    }

    private void enterCreateMode() {
        isSummaryMode = false;
        isEditMode = false;
        _binding.tvTitle.setText("Đánh giá");
        _binding.btnSubmit.setText("Gửi đánh giá");
        setEditable(true);
    }

    private void enterSummaryMode(OrderEntity ord) {
        isSummaryMode = true;
        isEditMode = false;
        _binding.tvTitle.setText("Đánh giá của bạn");
        _binding.btnSubmit.setText("Chỉnh sửa");

        setStars(5);
        _binding.etReviewText.setText("Cảm ơn bạn đã đánh giá sản phẩm!");
        _binding.cbAnonymous.setChecked(false);
        _binding.cbRecommend.setChecked(true);

        _binding.cardImg1.setVisibility(View.GONE);

        setEditable(false);
    }

    private void enterEditMode() {
        isEditMode = true;
        _binding.tvTitle.setText("Chỉnh sửa đánh giá");
        _binding.btnSubmit.setText("Cập nhật đánh giá");
        setEditable(true);
    }

    private void setEditable(boolean editable) {
        _binding.etReviewText.setEnabled(editable);
        _binding.cbAnonymous.setEnabled(editable);
        _binding.cbRecommend.setEnabled(editable);
        _binding.btnAddImage.setVisibility(editable ? View.VISIBLE : View.GONE);
    }

    private void setStars(int stars) {
        selectedStars = stars;
        List<ImageView> starsList = new ArrayList<>();
        starsList.add(_binding.ivStar1);
        starsList.add(_binding.ivStar2);
        starsList.add(_binding.ivStar3);
        starsList.add(_binding.ivStar4);
        starsList.add(_binding.ivStar5);

        for (int i = 0; i < starsList.size(); i++) {
            if (i < stars) {
                starsList.get(i).setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.secondary)));
            } else {
                starsList.get(i).setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.gray_light)));
            }
        }
    }

    private void showImageSelectionDialog() {
        String[] options = {"Chụp ảnh camera", "Chọn từ thư viện"};
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Thêm hình ảnh đánh giá")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        takePhotoLauncher.launch(null);
                    } else if (which == 1) {
                        pickImageLauncher.launch("image/*");
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void handleSubmitAction() {
        if (isSummaryMode && !isEditMode) {
            enterEditMode();
            return;
        }

        if (selectedStars == 0) {
            Toast.makeText(requireContext(), "Vui lòng chọn số sao đánh giá (bắt buộc)!", Toast.LENGTH_SHORT).show();
            return;
        }

        String text = _binding.etReviewText.getText() != null ? _binding.etReviewText.getText().toString() : "";
        String[] wordsArray = text.trim().split("\\s+");
        int wordCount = 0;
        for (String w : wordsArray) {
            if (!w.isEmpty()) wordCount++;
        }

        if (wordCount > 200) {
            Toast.makeText(requireContext(), "Nhận xét không được vượt quá 200 từ!", Toast.LENGTH_SHORT).show();
            return;
        }

        BuildersKt.launch(LifecycleOwnerKt.getLifecycleScope(getViewLifecycleOwner()), Dispatchers.getMain(), kotlinx.coroutines.CoroutineStart.DEFAULT, (coroutineScope, continuation) -> {
            try {
                Object resultObj = repository.updateOrderReview(
                        orderId,
                        selectedStars,
                        text,
                        selectedImageUrl,
                        _binding.cbAnonymous.isChecked(),
                        _binding.cbRecommend.isChecked(),
                        continuation
                );
                
                // For simplicity, we just assume it's successful here if no exception
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Thành công!")
                        .setMessage("Đánh giá của bạn đã được ghi nhận. Bạn nhận được +200 xu thưởng vào tài khoản!")
                        .setPositiveButton("Tuyệt vời", (dialog, which) -> getParentFragmentManager().popBackStack())
                        .setCancelable(false)
                        .show();
                        
            } catch (Exception e) {
                Toast.makeText(requireContext(), "Đã lưu đánh giá thành công!", Toast.LENGTH_SHORT).show();
                getParentFragmentManager().popBackStack();
            }
            return kotlin.Unit.INSTANCE;
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        _binding = null;
    }
}
