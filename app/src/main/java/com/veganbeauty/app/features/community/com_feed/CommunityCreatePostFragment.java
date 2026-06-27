package com.veganbeauty.app.features.community.com_feed;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.LifecycleOwnerKt;

import coil.Coil;
import coil.ImageLoader;
import coil.request.ImageRequest;
import coil.transform.CircleCropTransformation;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.shape.ShapeAppearanceModel;
import com.veganbeauty.app.R;
import com.veganbeauty.app.core.base.RootieFragment;
import com.veganbeauty.app.data.local.LocalJsonReader;
import com.veganbeauty.app.data.local.ProfileSession;
import com.veganbeauty.app.data.local.RootieDatabase;
import com.veganbeauty.app.data.local.dao.CommunityDao;
import com.veganbeauty.app.data.local.entities.CommunityPostEntity;
import com.veganbeauty.app.data.remote.FirestoreService;
import com.veganbeauty.app.databinding.ComFragmentCreatePostBinding;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

import kotlinx.coroutines.BuildersKt;
import kotlinx.coroutines.Dispatchers;

public class CommunityCreatePostFragment extends RootieFragment {

    private ComFragmentCreatePostBinding binding;
    private final List<String> selectedImageUris = new ArrayList<>();
    private final List<String> selectedProductIds = new ArrayList<>();
    private BottomSheetBehavior<View> bottomSheetBehavior;
    private CommunityDao communityDao;
    private final FirestoreService firestoreService = new FirestoreService();

    private String loggedUserId = "test_001";
    private String loggedUsername = "Test User";
    private String loggedDisplayName = "Test User";
    private String loggedAvatarUrl = "";

    private final ActivityResultLauncher<String> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetMultipleContents(),
            uris -> LifecycleOwnerKt.getLifecycleScope(getViewLifecycleOwner()).launchWhenStarted((scope, continuation) -> {
                return BuildersKt.withContext(Dispatchers.getIO(), (coroutineScopeIO, continuationIO) -> {
                    List<String[]> savedUris = new ArrayList<>();
                    for (Uri uri : uris) {
                        savedUris.add(new String[]{uri.toString(), copyUriToInternalStorage(uri)});
                    }
                    BuildersKt.withContext(Dispatchers.getMain(), (coroutineScopeMain, continuationMain) -> {
                        for (String[] uriPair : savedUris) {
                            String originalUriStr = uriPair[0];
                            String savedUriStr = uriPair[1];
                            selectedImageUris.add(savedUriStr);

                            FrameLayout container = new FrameLayout(requireContext());
                            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                                    (int) (100 * getResources().getDisplayMetrics().density),
                                    (int) (100 * getResources().getDisplayMetrics().density)
                            );
                            lp.setMarginEnd((int) (12 * getResources().getDisplayMetrics().density));
                            container.setLayoutParams(lp);

                            ShapeableImageView iv = new ShapeableImageView(requireContext());
                            iv.setLayoutParams(new FrameLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                            ));
                            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                            iv.setBackgroundColor(Color.LTGRAY);
                            float radius = 12 * getResources().getDisplayMetrics().density;
                            ShapeAppearanceModel shapeModel = iv.getShapeAppearanceModel().toBuilder().setAllCornerSizes(radius).build();
                            iv.setShapeAppearanceModel(shapeModel);
                            iv.setImageURI(Uri.parse(originalUriStr));

                            ImageView ivClose = new ImageView(requireContext());
                            FrameLayout.LayoutParams closeLp = new FrameLayout.LayoutParams(
                                    (int) (24 * getResources().getDisplayMetrics().density),
                                    (int) (24 * getResources().getDisplayMetrics().density)
                            );
                            closeLp.gravity = Gravity.TOP | Gravity.END;
                            closeLp.topMargin = (int) (4 * getResources().getDisplayMetrics().density);
                            closeLp.setMarginEnd((int) (4 * getResources().getDisplayMetrics().density));
                            ivClose.setLayoutParams(closeLp);
                            ivClose.setImageResource(R.drawable.ic_close);
                            ivClose.setBackgroundResource(R.drawable.bg_circle_white);
                            ivClose.setPadding(12, 12, 12, 12);
                            ivClose.setOnClickListener(v -> {
                                if (container.getParent() instanceof ViewGroup) {
                                    ((ViewGroup) container.getParent()).removeView(container);
                                }
                                selectedImageUris.remove(savedUriStr);
                            });

                            container.addView(iv);
                            container.addView(ivClose);
                            if (binding.llImagePreviewContainer != null) {
                                binding.llImagePreviewContainer.addView(container);
                            }
                        }
                        return kotlin.Unit.INSTANCE;
                    }, continuationIO);
                    return kotlin.Unit.INSTANCE;
                }, continuation);
            })
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = ComFragmentCreatePostBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    private void setupChips(LinearLayout container) {
        if (container == null) return;
        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            if (child instanceof TextView) {
                TextView tv = (TextView) child;
                if ("+".equals(tv.getText().toString())) continue;
                tv.setBackgroundResource(R.drawable.com_bg_chip_solid_normal);
                tv.setTextColor(Color.parseColor("#4B6554"));
                tv.setOnClickListener(v -> {
                    boolean isSelected = tv.getCurrentTextColor() == Color.WHITE;
                    if (isSelected) {
                        tv.setBackgroundResource(R.drawable.com_bg_chip_solid_normal);
                        tv.setTextColor(Color.parseColor("#4B6554"));
                    } else {
                        tv.setBackgroundResource(R.drawable.com_bg_filter_selected);
                        tv.setTextColor(Color.WHITE);
                    }
                });
            }
        }
    }

    private String copyUriToInternalStorage(Uri uri) {
        try {
            InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
            if (inputStream == null) return uri.toString();
            String fileName = "post_img_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 5) + ".jpg";
            File file = new File(requireContext().getFilesDir(), fileName);
            FileOutputStream outputStream = new FileOutputStream(file);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            inputStream.close();
            outputStream.close();
            return "file://" + file.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return uri.toString();
        }
    }

    private void loadCurrentUserInfo() {
        Context ctx = requireContext();
        String loggedEmail = ProfileSession.getEmail(ctx);
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(ctx.getAssets().open("users.json")));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            String usersJson = sb.toString().replace("\uFEFF", "");
            JSONArray usersArr = new JSONArray(usersJson);
            for (int i = 0; i < usersArr.length(); i++) {
                JSONObject u = usersArr.getJSONObject(i);
                if (loggedEmail != null && loggedEmail.equals(u.optString("email"))) {
                    loggedUserId = u.optString("user_id", "test_001");
                    loggedUsername = u.optString("username", "");
                    loggedDisplayName = u.optString("full_name", loggedUsername);
                    loggedAvatarUrl = u.optString("avatar", "");
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupHideKeyboard(View view) {
        if (!(view instanceof EditText)) {
            view.setOnTouchListener((v, event) -> {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                    }
                    if (binding != null && binding.etContent != null) {
                        binding.etContent.clearFocus();
                    }
                }
                return false;
            });
        }
        if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                setupHideKeyboard(((ViewGroup) view).getChildAt(i));
            }
        }
    }

    @Override
    public void setupUI(@NonNull View view) {
        setupHideKeyboard(binding.getRoot());
        RootieDatabase db = RootieDatabase.Companion.getDatabase(requireContext());
        communityDao = db.communityDao();

        loadCurrentUserInfo();
        if (binding.tvUserName != null) binding.tvUserName.setText(loggedDisplayName);

        if (binding.ivUserAvatar != null) {
            if (!loggedAvatarUrl.isEmpty()) {
                ImageLoader imageLoader = Coil.imageLoader(requireContext());
                ImageRequest request = new ImageRequest.Builder(requireContext())
                        .data(loggedAvatarUrl)
                        .crossfade(true)
                        .transformations(new CircleCropTransformation())
                        .placeholder(R.drawable.img_avatar)
                        .target(binding.ivUserAvatar)
                        .build();
                imageLoader.enqueue(request);
            } else {
                binding.ivUserAvatar.setImageResource(R.drawable.img_avatar);
            }
        }

        setupChips(binding.llTopicsContainer);
        setupChips(binding.llSkinIssuesContainer);

        if (binding.ivClose != null) {
            binding.ivClose.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        }

        if (binding.tvAddSkinIssue != null) {
            binding.tvAddSkinIssue.setOnClickListener(v -> {
                EditText editText = new EditText(getContext());
                editText.setHint("Nhập vấn đề về da");
                new AlertDialog.Builder(requireContext(), R.style.CustomDialogTheme)
                        .setTitle("Thêm vấn đề về da")
                        .setView(editText)
                        .setPositiveButton("Thêm", (dialog, which) -> {
                            String issue = editText.getText().toString();
                            if (!issue.trim().isEmpty()) {
                                TextView newChip = new TextView(getContext());
                                newChip.setText(issue);
                                newChip.setTextColor(Color.WHITE);
                                newChip.setBackgroundResource(R.drawable.com_bg_filter_selected);
                                int padH = (int) (12 * getResources().getDisplayMetrics().density);
                                int padV = (int) (4 * getResources().getDisplayMetrics().density);
                                newChip.setPadding(padH, padV, padH, padV);
                                newChip.setTextSize(12f);
                                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                        ViewGroup.LayoutParams.WRAP_CONTENT,
                                        ViewGroup.LayoutParams.WRAP_CONTENT
                                );
                                params.setMarginEnd((int) (8 * getResources().getDisplayMetrics().density));
                                newChip.setLayoutParams(params);

                                newChip.setOnClickListener(v1 -> {
                                    boolean isSelected = newChip.getCurrentTextColor() == Color.WHITE;
                                    if (isSelected) {
                                        newChip.setBackgroundResource(R.drawable.com_bg_chip_solid_normal);
                                        newChip.setTextColor(Color.parseColor("#4B6554"));
                                    } else {
                                        newChip.setBackgroundResource(R.drawable.com_bg_filter_selected);
                                        newChip.setTextColor(Color.WHITE);
                                    }
                                });
                                if (binding.llSkinIssuesContainer != null) {
                                    binding.llSkinIssuesContainer.addView(newChip, binding.llSkinIssuesContainer.getChildCount() - 1);
                                }
                            }
                        })
                        .setNegativeButton("Hủy", null)
                        .show();
            });
        }

        View tvAddTopic = binding.getRoot().findViewById(R.id.tvAddTopic);
        if (tvAddTopic != null) {
            tvAddTopic.setOnClickListener(v -> {
                EditText editText = new EditText(getContext());
                editText.setHint("Nhập chủ đề bài đăng");
                new AlertDialog.Builder(requireContext(), R.style.CustomDialogTheme)
                        .setTitle("Thêm chủ đề")
                        .setView(editText)
                        .setPositiveButton("Thêm", (dialog, which) -> {
                            String topic = editText.getText().toString();
                            if (!topic.trim().isEmpty()) {
                                TextView newChip = new TextView(getContext());
                                newChip.setText(topic);
                                newChip.setTextColor(Color.WHITE);
                                newChip.setBackgroundResource(R.drawable.com_bg_filter_selected);
                                int padH = (int) (12 * getResources().getDisplayMetrics().density);
                                int padV = (int) (4 * getResources().getDisplayMetrics().density);
                                newChip.setPadding(padH, padV, padH, padV);
                                newChip.setTextSize(12f);
                                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                        ViewGroup.LayoutParams.WRAP_CONTENT,
                                        ViewGroup.LayoutParams.WRAP_CONTENT
                                );
                                params.setMarginEnd((int) (8 * getResources().getDisplayMetrics().density));
                                newChip.setLayoutParams(params);

                                newChip.setOnClickListener(v12 -> {
                                    boolean isSelected = newChip.getCurrentTextColor() == Color.WHITE;
                                    if (isSelected) {
                                        newChip.setBackgroundResource(R.drawable.com_bg_chip_solid_normal);
                                        newChip.setTextColor(Color.parseColor("#4B6554"));
                                    } else {
                                        newChip.setBackgroundResource(R.drawable.com_bg_filter_selected);
                                        newChip.setTextColor(Color.WHITE);
                                    }
                                });
                                if (binding.llTopicsContainer != null) {
                                    binding.llTopicsContainer.addView(newChip, binding.llTopicsContainer.getChildCount() - 1);
                                }
                            }
                        })
                        .setNegativeButton("Hủy", null)
                        .show();
            });
        }

        View llPrivacy = binding.getRoot().findViewById(R.id.llPrivacy);
        if (llPrivacy != null) {
            llPrivacy.setOnClickListener(v -> {
                PopupMenu popup = new PopupMenu(getContext(), v);
                popup.getMenu().add("Công khai");
                popup.getMenu().add("Bạn bè");
                popup.getMenu().add("Chỉ mình tôi");
                popup.getMenu().add("Tuỳ chọn");
                popup.setOnMenuItemClickListener(item -> {
                    TextView tvPrivacy = binding.getRoot().findViewById(R.id.tvPrivacy);
                    if (tvPrivacy != null) {
                        tvPrivacy.setText(item.getTitle());
                    }
                    return true;
                });
                popup.show();
            });
        }

        if (binding.llAddMedia != null) binding.llAddMedia.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
        if (binding.ivAddMediaHorizontal != null) binding.ivAddMediaHorizontal.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
        if (binding.llAddProduct != null) binding.llAddProduct.setOnClickListener(v -> showProductSelectionDialog());
        if (binding.ivAddProductHorizontal != null) binding.ivAddProductHorizontal.setOnClickListener(v -> showProductSelectionDialog());

        if (binding.bottomSheet != null) {
            bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheet);
            bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
                @Override
                public void onStateChanged(@NonNull View bottomSheet, int newState) {
                    if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                        if (binding.llHorizontalIcons != null) binding.llHorizontalIcons.setVisibility(View.GONE);
                        if (binding.llVerticalList != null) binding.llVerticalList.setVisibility(View.VISIBLE);
                    } else if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                        if (binding.llHorizontalIcons != null) binding.llHorizontalIcons.setVisibility(View.VISIBLE);
                        if (binding.llVerticalList != null) binding.llVerticalList.setVisibility(View.GONE);
                    }
                }

                @Override
                public void onSlide(@NonNull View bottomSheet, float slideOffset) {}
            });
        }

        if (binding.vHandle != null) {
            binding.vHandle.setOnClickListener(v -> {
                if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED) {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                } else {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                }
            });
        }

        if (binding.etContent != null) {
            binding.etContent.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (s != null && !s.toString().trim().isEmpty()) {
                        if (binding.btnPost != null) binding.btnPost.setTextColor(Color.parseColor("#4B6554"));
                    } else {
                        if (binding.btnPost != null) binding.btnPost.setTextColor(Color.parseColor("#9CA3AF"));
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        if (binding.btnPost != null) {
            binding.btnPost.setOnClickListener(v -> {
                String content = binding.etContent != null ? binding.etContent.getText().toString() : "";
                if (!content.trim().isEmpty()) {
                    savePost(content);
                } else {
                    Toast.makeText(getContext(), "Vui lòng nhập nội dung bài viết", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void showProductSelectionDialog() {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(requireContext().getAssets().open("products.json")));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            String jsonStr = sb.toString().replace("\uFEFF", "");
            JSONObject jsonObject = new JSONObject(jsonStr);

            Map<String, JSONObject> productsMap = new HashMap<>();
            try {
                String currentUserId = "test_001";
                LocalJsonReader jsonReader = new LocalJsonReader(requireContext());
                List<String> eligibleProductIds = jsonReader.getShowcaseProductsForUser(currentUserId);

                JSONArray allProductsArray = jsonObject.optJSONArray("products");
                if (allProductsArray == null) allProductsArray = new JSONArray(jsonStr);
                Map<String, JSONObject> allProductsMap = new HashMap<>();
                for (int i = 0; i < allProductsArray.length(); i++) {
                    JSONObject p = allProductsArray.optJSONObject(i);
                    if (p == null) continue;
                    String pId = p.optString("id", p.optString("_id"));
                    allProductsMap.put(pId, p);
                }

                for (String pId : eligibleProductIds) {
                    if (!productsMap.containsKey(pId)) {
                        JSONObject pData = allProductsMap.get(pId);
                        JSONObject obj = new JSONObject();
                        obj.put("id", pId);
                        obj.put("name", pData != null ? pData.optString("name") : "Sản phẩm " + pId);
                        obj.put("thumbnail_url", pData != null ? pData.optString("mainImage", pData.optString("image", "")) : "");
                        obj.put("brand", pData != null ? pData.optString("brand", "") : "");
                        productsMap.put(pId, obj);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            JSONArray productsArray = new JSONArray();
            for (JSONObject obj : productsMap.values()) {
                productsArray.put(obj);
            }

            BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext(), 0);
            LinearLayout container = new LinearLayout(requireContext());
            container.setOrientation(LinearLayout.VERTICAL);
            container.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            container.setPadding(0, 48, 0, 48);
            container.setBackgroundResource(R.drawable.com_bg_top_rounded_white);

            TextView title = new TextView(requireContext());
            title.setText("Chọn sản phẩm để gắn");
            title.setTextSize(18f);
            title.setTypeface(null, Typeface.BOLD);
            title.setTextColor(Color.BLACK);
            title.setPadding(48, 0, 48, 48);
            container.addView(title);

            ScrollView scrollView = new ScrollView(requireContext());
            scrollView.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    (int) (400 * getResources().getDisplayMetrics().density)
            ));

            LinearLayout listContainer = new LinearLayout(requireContext());
            listContainer.setOrientation(LinearLayout.VERTICAL);

            if (productsArray.length() == 0) {
                TextView tvEmpty = new TextView(requireContext());
                tvEmpty.setText("Bạn chưa trưng bày sản phẩm nào trong cửa hàng.");
                tvEmpty.setTextSize(14f);
                tvEmpty.setTextColor(Color.GRAY);
                tvEmpty.setPadding(48, 48, 48, 48);
                tvEmpty.setGravity(Gravity.CENTER);
                listContainer.addView(tvEmpty);
            } else {
                for (int i = 0; i < productsArray.length(); i++) {
                    JSONObject prod = productsArray.getJSONObject(i);
                    String id = prod.optString("id", "");
                    String name = prod.optString("name", "Sản phẩm không tên");
                    String img = prod.optString("thumbnail_url", "");
                    String brand = prod.optString("brand", "");

                    LinearLayout itemLayout = new LinearLayout(requireContext());
                    itemLayout.setOrientation(LinearLayout.HORIZONTAL);
                    itemLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                    itemLayout.setPadding(48, 24, 48, 24);
                    itemLayout.setGravity(Gravity.CENTER_VERTICAL);

                    ImageView iv = new ImageView(requireContext());
                    LinearLayout.LayoutParams ivLp = new LinearLayout.LayoutParams(
                            (int) (50 * getResources().getDisplayMetrics().density),
                            (int) (50 * getResources().getDisplayMetrics().density)
                    );
                    ivLp.setMarginEnd(32);
                    iv.setLayoutParams(ivLp);
                    iv.setScaleType(ImageView.ScaleType.CENTER_CROP);

                    if (!img.isEmpty()) {
                        ImageLoader imageLoader = Coil.imageLoader(requireContext());
                        ImageRequest request = new ImageRequest.Builder(requireContext())
                                .data(img)
                                .crossfade(true)
                                .target(iv)
                                .build();
                        imageLoader.enqueue(request);
                    } else {
                        iv.setImageResource(R.color.gray_light);
                    }

                    LinearLayout textLayout = new LinearLayout(requireContext());
                    textLayout.setOrientation(LinearLayout.VERTICAL);
                    textLayout.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

                    TextView tvName = new TextView(requireContext());
                    tvName.setText(name);
                    tvName.setTextSize(14f);
                    tvName.setTextColor(Color.BLACK);
                    tvName.setTypeface(null, Typeface.BOLD);
                    tvName.setMaxLines(2);
                    tvName.setEllipsize(TextUtils.TruncateAt.END);

                    TextView tvBrand = new TextView(requireContext());
                    tvBrand.setText(brand);
                    tvBrand.setTextSize(12f);
                    tvBrand.setTextColor(Color.GRAY);
                    tvBrand.setPadding(0, 8, 0, 0);

                    textLayout.addView(tvName);
                    textLayout.addView(tvBrand);

                    itemLayout.addView(iv);
                    itemLayout.addView(textLayout);

                    itemLayout.setOnClickListener(v -> {
                        if (!selectedProductIds.contains(id)) {
                            selectedProductIds.add(id);

                            LinearLayout llList = binding.getRoot().findViewById(R.id.llAttachedProductsList);
                            LinearLayout llContainer = binding.getRoot().findViewById(R.id.llAttachedProductsContainer);

                            if (llList != null) {
                                View productView = LayoutInflater.from(getContext()).inflate(R.layout.com_item_post_product, llList, false);
                                TextView tvProductName = productView.findViewById(R.id.tvProductName);
                                ImageView ivProductImg = productView.findViewById(R.id.ivProductImage);

                                tvProductName.setText(name);
                                if (!img.isEmpty()) {
                                    ImageLoader loader = Coil.imageLoader(requireContext());
                                    ImageRequest req = new ImageRequest.Builder(requireContext())
                                            .data(img)
                                            .crossfade(true)
                                            .target(ivProductImg)
                                            .build();
                                    loader.enqueue(req);
                                }

                                productView.setOnClickListener(pv -> {
                                    selectedProductIds.remove(id);
                                    if (productView.getParent() instanceof ViewGroup) {
                                        ((ViewGroup) productView.getParent()).removeView(productView);
                                    }
                                    if (llList.getChildCount() == 0 && llContainer != null) {
                                        llContainer.setVisibility(View.GONE);
                                    }
                                });

                                llList.addView(productView);
                                if (llContainer != null) llContainer.setVisibility(View.VISIBLE);
                            }
                        }
                        bottomSheetDialog.dismiss();
                    });

                    listContainer.addView(itemLayout);

                    if (i < productsArray.length() - 1) {
                        View divider = new View(requireContext());
                        LinearLayout.LayoutParams divLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1);
                        divLp.setMarginStart(48);
                        divLp.setMarginEnd(48);
                        divider.setLayoutParams(divLp);
                        divider.setBackgroundColor(Color.parseColor("#EEEEEE"));
                        listContainer.addView(divider);
                    }
                }
            }

            scrollView.addView(listContainer);
            container.addView(scrollView);
            bottomSheetDialog.setContentView(container);

            View bottomSheet = bottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }

            bottomSheetDialog.show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Lỗi tải danh sách sản phẩm", Toast.LENGTH_SHORT).show();
        }
    }

    private void savePost(String content) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

        String selectedTopic = "";
        String selectedSkinIssue = "";

        if (binding.llTopicsContainer != null) {
            for (int i = 0; i < binding.llTopicsContainer.getChildCount(); i++) {
                View child = binding.llTopicsContainer.getChildAt(i);
                if (child instanceof TextView) {
                    TextView tv = (TextView) child;
                    if (tv.getCurrentTextColor() == Color.WHITE && !"+".equals(tv.getText().toString())) {
                        selectedTopic = tv.getText().toString();
                        break;
                    }
                }
            }
        }

        if (binding.llSkinIssuesContainer != null) {
            for (int i = 0; i < binding.llSkinIssuesContainer.getChildCount(); i++) {
                View child = binding.llSkinIssuesContainer.getChildAt(i);
                if (child instanceof TextView) {
                    TextView tv = (TextView) child;
                    if (tv.getCurrentTextColor() == Color.WHITE && !"+".equals(tv.getText().toString())) {
                        selectedSkinIssue = tv.getText().toString();
                        break;
                    }
                }
            }
        }

        StringBuilder urisBuilder = new StringBuilder();
        for (int i = 0; i < selectedImageUris.size(); i++) {
            urisBuilder.append(selectedImageUris.get(i));
            if (i < selectedImageUris.size() - 1) urisBuilder.append(",");
        }

        StringBuilder productsBuilder = new StringBuilder();
        for (int i = 0; i < selectedProductIds.size(); i++) {
            productsBuilder.append(selectedProductIds.get(i));
            if (i < selectedProductIds.size() - 1) productsBuilder.append(",");
        }

        CommunityPostEntity newPost = new CommunityPostEntity(
                UUID.randomUUID().toString(),
                loggedUserId,
                loggedUsername,
                loggedDisplayName,
                loggedAvatarUrl,
                content,
                urisBuilder.toString(),
                sdf.format(new Date()),
                0,
                0,
                0,
                selectedSkinIssue,
                selectedTopic,
                selectedTopic,
                productsBuilder.toString()
        );

        View flLoading = binding.getRoot().findViewById(R.id.flLoading);
        if (flLoading != null) flLoading.setVisibility(View.VISIBLE);
        if (binding.btnPost != null) binding.btnPost.setEnabled(false);

        LifecycleOwnerKt.getLifecycleScope(getViewLifecycleOwner()).launchWhenStarted((scope, continuation) -> {
            return BuildersKt.withContext(Dispatchers.getIO(), (coroutineScopeIO, continuationIO) -> {
                new LocalJsonReader(requireContext()).saveLocalPost(newPost);
                List<CommunityPostEntity> list = new ArrayList<>();
                list.add(newPost);
                communityDao.insertPosts(list);
                firestoreService.uploadCommunityPost(newPost);

                BuildersKt.withContext(Dispatchers.getMain(), (coroutineScopeMain, continuationMain) -> {
                    if (flLoading != null) flLoading.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Đã đăng bài viết!", Toast.LENGTH_SHORT).show();
                    getParentFragmentManager().popBackStack();
                    return kotlin.Unit.INSTANCE;
                }, continuationIO);

                return kotlin.Unit.INSTANCE;
            }, continuation);
        });
    }

    @Override
    public void observeViewModel() {}

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
