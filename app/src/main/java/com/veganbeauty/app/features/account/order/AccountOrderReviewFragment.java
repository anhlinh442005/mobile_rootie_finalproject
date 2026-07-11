package com.veganbeauty.app.features.account.order;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.FlowLiveDataConversions;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.veganbeauty.app.R;
import com.veganbeauty.app.core.base.RootieFragment;
import com.veganbeauty.app.data.local.LocalJsonReader;
import com.veganbeauty.app.data.local.OrderReviewLocalStore;
import com.veganbeauty.app.data.local.RootieDatabase;
import com.veganbeauty.app.data.local.entities.OrderEntity;
import com.veganbeauty.app.data.local.entities.OrderEntity.OrderItem;
import com.veganbeauty.app.data.repository.OrderRepository;
import com.veganbeauty.app.databinding.AccountOrderReviewFragmentBinding;
import com.veganbeauty.app.utils.CoinRewardDialogHelper;

import org.json.JSONArray;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class AccountOrderReviewFragment extends RootieFragment {

    private static final int MAX_REVIEW_IMAGES = 6;

    private AccountOrderReviewFragmentBinding _binding;

    private OrderRepository repository;
    private String orderId = "";
    private OrderEntity order = null;

    private int selectedStars = 0;
    private final List<String> selectedImages = new ArrayList<>();
    private boolean isEditMode = false;
    private boolean isSummaryMode = false;

    private final ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    addImageFromUri(uri);
                }
            }
    );

    private final ActivityResultLauncher<String> pickMultipleImagesLauncher = registerForActivityResult(
            new ActivityResultContracts.GetMultipleContents(),
            uris -> {
                if (uris == null || uris.isEmpty()) {
                    return;
                }
                int added = 0;
                for (Uri uri : uris) {
                    if (selectedImages.size() >= MAX_REVIEW_IMAGES) {
                        break;
                    }
                    if (addImageFromUri(uri, false)) {
                        added++;
                    }
                }
                refreshImagesUi();
                if (added > 0) {
                    Toast.makeText(getContext(), "Đã thêm " + added + " ảnh", Toast.LENGTH_SHORT).show();
                } else if (selectedImages.size() >= MAX_REVIEW_IMAGES) {
                    Toast.makeText(getContext(), "Tối đa " + MAX_REVIEW_IMAGES + " ảnh", Toast.LENGTH_SHORT).show();
                }
            }
    );

    private final ActivityResultLauncher<Void> takePhotoLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicturePreview(),
            bitmap -> {
                if (bitmap != null) {
                    String path = saveBitmapToCache(bitmap);
                    if (path != null) {
                        addImagePath(path);
                    }
                }
            }
    );

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

        refreshImagesUi();
        loadOrderData();
    }

    private void loadOrderData() {
        FlowLiveDataConversions.asLiveData(repository.getOrderById(orderId))
            .observe(getViewLifecycleOwner(), list -> {
                if (list != null && !list.isEmpty()) {
                    order = list.get(0);
                    bindOrderDetails(order);
                }
            });
    }

    private void bindOrderDetails(OrderEntity ord) {
        if (ord.getItems() == null || ord.getItems().isEmpty()) return;

        // Khôi phục feedback đã lưu theo đơn (tránh mất khi sync Firebase)
        OrderReviewLocalStore.applyTo(requireContext(), ord);

        OrderItem firstItem = ord.getItems().get(0);
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

        com.bumptech.glide.Glide.with(_binding.ivProductImage.getContext())
                .load(firstItem.getProductImage())
                .placeholder(android.R.color.darker_gray)
                .error(android.R.color.darker_gray)
                .into(_binding.ivProductImage);

        if (ord.isHasReview() && !isEditMode) {
            enterSummaryMode(ord);
        } else if (!isEditMode) {
            // Không reset form nếu đang xem summary mà Flow emit lại data thiếu review
            if (!isSummaryMode) {
                enterCreateMode();
            }
        }
    }

    private void enterCreateMode() {
        isSummaryMode = false;
        isEditMode = false;
        _binding.tvTitle.setText("Đánh giá");
        _binding.btnSubmit.setText("Gửi đánh giá");
        setEditable(true);
        refreshImagesUi();
    }

    private void enterSummaryMode(OrderEntity ord) {
        isSummaryMode = true;
        isEditMode = false;
        _binding.tvTitle.setText("Đánh giá của bạn");
        _binding.btnSubmit.setText("Chỉnh sửa");

        int stars = ord.getReviewStars() > 0 ? ord.getReviewStars() : 5;
        setStars(stars);
        String reviewText = ord.getReviewText();
        _binding.etReviewText.setText(
                reviewText != null && !reviewText.trim().isEmpty()
                        ? reviewText
                        : "Cảm ơn bạn đã đánh giá sản phẩm!"
        );
        _binding.cbAnonymous.setChecked(ord.isAnonymous());
        _binding.cbRecommend.setChecked(ord.isRecommendToFriends());

        selectedImages.clear();
        selectedImages.addAll(parseReviewImages(ord.getReviewImage()));
        setEditable(false);
        refreshImagesUi();
    }

    private void enterEditMode() {
        isEditMode = true;
        _binding.tvTitle.setText("Chỉnh sửa đánh giá");
        _binding.btnSubmit.setText("Cập nhật đánh giá");
        setEditable(true);
        refreshImagesUi();
    }

    private void setEditable(boolean editable) {
        _binding.etReviewText.setEnabled(editable);
        _binding.cbAnonymous.setEnabled(editable);
        _binding.cbRecommend.setEnabled(editable);
        _binding.btnAddImage.setVisibility(editable && selectedImages.size() < MAX_REVIEW_IMAGES
                ? View.VISIBLE : View.GONE);
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
        if (selectedImages.size() >= MAX_REVIEW_IMAGES) {
            Toast.makeText(requireContext(), "Tối đa " + MAX_REVIEW_IMAGES + " ảnh", Toast.LENGTH_SHORT).show();
            return;
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Thêm hình ảnh đánh giá")
                .setItems(new CharSequence[]{
                        "Chụp ảnh camera",
                        "Chọn 1 ảnh từ thư viện",
                        "Chọn nhiều ảnh từ thư viện"
                }, (dialog, which) -> {
                    if (which == 0) {
                        takePhotoLauncher.launch(null);
                    } else if (which == 1) {
                        pickImageLauncher.launch("image/*");
                    } else {
                        pickMultipleImagesLauncher.launch("image/*");
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void addImageFromUri(Uri uri) {
        addImageFromUri(uri, true);
    }

    private boolean addImageFromUri(Uri uri, boolean refreshAndToast) {
        if (uri == null) return false;
        if (selectedImages.size() >= MAX_REVIEW_IMAGES) {
            if (refreshAndToast) {
                Toast.makeText(requireContext(), "Tối đa " + MAX_REVIEW_IMAGES + " ảnh", Toast.LENGTH_SHORT).show();
            }
            return false;
        }
        String path = copyUriToCache(uri);
        if (path == null) {
            if (refreshAndToast) {
                Toast.makeText(requireContext(), "Không thể đọc ảnh", Toast.LENGTH_SHORT).show();
            }
            return false;
        }
        selectedImages.add(path);
        if (refreshAndToast) {
            refreshImagesUi();
            Toast.makeText(requireContext(), "Đã thêm ảnh từ thư viện!", Toast.LENGTH_SHORT).show();
        }
        return true;
    }

    private void addImagePath(String path) {
        if (path == null || path.trim().isEmpty()) return;
        if (selectedImages.size() >= MAX_REVIEW_IMAGES) {
            Toast.makeText(requireContext(), "Tối đa " + MAX_REVIEW_IMAGES + " ảnh", Toast.LENGTH_SHORT).show();
            return;
        }
        selectedImages.add(path.trim());
        refreshImagesUi();
        Toast.makeText(requireContext(), "Đã chụp ảnh thành công!", Toast.LENGTH_SHORT).show();
    }

    private void removeImageAt(int index) {
        if (index < 0 || index >= selectedImages.size()) return;
        selectedImages.remove(index);
        refreshImagesUi();
    }

    private void refreshImagesUi() {
        if (_binding == null) return;
        LinearLayout container = _binding.layoutImagesContainer;
        // Giữ nút Thêm ảnh (child 0), xóa các thumbnail phía sau
        while (container.getChildCount() > 1) {
            container.removeViewAt(1);
        }

        boolean canEdit = !isSummaryMode || isEditMode;
        _binding.btnAddImage.setVisibility(
                canEdit && selectedImages.size() < MAX_REVIEW_IMAGES ? View.VISIBLE : View.GONE
        );
        if (_binding.tvAddImageLabel != null) {
            _binding.tvAddImageLabel.setText(selectedImages.isEmpty()
                    ? "Thêm ảnh"
                    : selectedImages.size() + "/" + MAX_REVIEW_IMAGES);
        }

        float density = getResources().getDisplayMetrics().density;
        int size = (int) (84 * density);
        int margin = (int) (12 * density);

        for (int i = 0; i < selectedImages.size(); i++) {
            container.addView(createThumbnailView(selectedImages.get(i), i, size, margin, canEdit));
        }
    }

    private View createThumbnailView(String path, int index, int size, int marginEnd, boolean canRemove) {
        MaterialCardView card = new MaterialCardView(requireContext());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
        lp.setMarginEnd(marginEnd);
        card.setLayoutParams(lp);
        card.setRadius(8 * getResources().getDisplayMetrics().density);
        card.setCardElevation(0f);
        card.setStrokeWidth(1);
        card.setStrokeColor(ContextCompat.getColor(requireContext(), R.color.gray_light));

        FrameLayout frame = new FrameLayout(requireContext());
        frame.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        ImageView imageView = new ImageView(requireContext());
        imageView.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        Object loadTarget = path.startsWith("content://") || path.startsWith("http")
                ? path
                : new File(path);
        com.bumptech.glide.Glide.with(requireContext()).load(loadTarget).centerCrop().into(imageView);
        frame.addView(imageView);

        if (canRemove) {
            ImageView removeBtn = new ImageView(requireContext());
            int removeSize = (int) (22 * getResources().getDisplayMetrics().density);
            FrameLayout.LayoutParams removeLp = new FrameLayout.LayoutParams(
                    removeSize, removeSize, Gravity.TOP | Gravity.END);
            removeLp.topMargin = (int) (4 * getResources().getDisplayMetrics().density);
            removeLp.setMarginEnd((int) (4 * getResources().getDisplayMetrics().density));
            removeBtn.setLayoutParams(removeLp);
            removeBtn.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
            removeBtn.setColorFilter(Color.WHITE);
            removeBtn.setBackgroundResource(R.drawable.home_bg_notification_badge);
            removeBtn.setPadding(4, 4, 4, 4);
            removeBtn.setOnClickListener(v -> removeImageAt(index));
            frame.addView(removeBtn);
        }

        card.addView(frame);
        return card;
    }

    @NonNull
    private List<String> parseReviewImages(@Nullable String raw) {
        List<String> result = new ArrayList<>();
        if (raw == null || raw.trim().isEmpty()) {
            return result;
        }
        String trimmed = raw.trim();
        try {
            if (trimmed.startsWith("[")) {
                JSONArray arr = new JSONArray(trimmed);
                for (int i = 0; i < arr.length(); i++) {
                    String item = arr.optString(i, "").trim();
                    if (!item.isEmpty()) result.add(item);
                }
            } else {
                result.add(trimmed);
            }
        } catch (Exception e) {
            result.add(trimmed);
        }
        return result;
    }

    private String copyUriToCache(Uri uri) {
        try {
            Context context = requireContext();
            File outFile = new File(context.getCacheDir(), "review_photo_" + System.currentTimeMillis() + ".jpg");
            try (InputStream in = context.getContentResolver().openInputStream(uri);
                 FileOutputStream out = new FileOutputStream(outFile)) {
                if (in == null) return null;
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
                out.flush();
            }
            return outFile.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String saveBitmapToCache(Bitmap bitmap) {
        try {
            File cacheDir = requireContext().getCacheDir();
            File file = new File(cacheDir, "review_photo_" + System.currentTimeMillis() + ".jpg");
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();
            return file.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
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

        final List<String> imagesToSave = new ArrayList<>(selectedImages);
        final boolean anonymous = _binding.cbAnonymous.isChecked();
        final boolean recommend = _binding.cbRecommend.isChecked();
        final int stars = selectedStars;
        final String reviewText = text;

        new Thread(() -> {
            try {
                boolean coinsAwarded = repository.updateOrderReview(
                        orderId,
                        stars,
                        reviewText,
                        imagesToSave,
                        anonymous,
                        recommend
                );

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (!isAdded()) {
                            return;
                        }
                        if (coinsAwarded) {
                            int total = com.veganbeauty.app.utils.RewardPointsHelper.getTotalPoints(requireContext());
                            CoinRewardDialogHelper.showWithDismissCallback(
                                    this,
                                    200,
                                    total,
                                    "từ đánh giá đơn hàng",
                                    () -> getParentFragmentManager().popBackStack()
                            );
                        } else {
                            Toast.makeText(requireContext(), "Đã lưu đánh giá thành công!", Toast.LENGTH_SHORT).show();
                            getParentFragmentManager().popBackStack();
                        }
                    });
                }

            } catch (Exception e) {
                e.printStackTrace();
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (!isAdded()) {
                            return;
                        }
                        Toast.makeText(requireContext(), "Không thể lưu đánh giá. Vui lòng thử lại!", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        }).start();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        _binding = null;
    }
}
