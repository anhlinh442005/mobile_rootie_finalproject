package com.veganbeauty.app.features.community.profile;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;


import com.veganbeauty.app.R;
import com.veganbeauty.app.data.local.LocalJsonReader;
import com.veganbeauty.app.data.local.ProfileSession;
import com.veganbeauty.app.data.local.entities.OrderEntity;
import com.veganbeauty.app.data.local.entities.OrderEntity.OrderItem;
import com.veganbeauty.app.features.community.affiliate.AffiliateProductsHelper;
import com.veganbeauty.app.features.shop.product.detail.ProductDetailLauncher;
import com.veganbeauty.app.utils.ProfileSessionHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class CommunityAddAffiliateProductsFragment extends Fragment {

    private final List<JSONObject> allProducts = new ArrayList<>();
    private final Set<String> purchasedProductIds = new HashSet<>();
    private final Set<String> showcasedProductIds = new HashSet<>();

    private String currentFilter = "AVAILABLE"; // AVAILABLE, ALL, FACE, BODY, HAIR

    private LinearLayout llAddProductsContainer;
    private DecimalFormat format;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.com_fragment_affiliate_add_products, container, false);

        ImageView ivBack = view.findViewById(R.id.ivBack);
        if (ivBack != null) {
            ivBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        }

        llAddProductsContainer = view.findViewById(R.id.llAddProductsContainer);

        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("vi", "VN"));
        symbols.setGroupingSeparator('.');
        format = new DecimalFormat("#,###đ", symbols);

        setupChips(view);
        loadData();
        renderProducts();

        return view;
    }

    private void setupChips(View view) {
        TextView chipAvailable = view.findViewById(R.id.chipAvailable);
        TextView chipAll = view.findViewById(R.id.chipAll);
        TextView chipFace = view.findViewById(R.id.chipFace);
        TextView chipBody = view.findViewById(R.id.chipBody);
        TextView chipHair = view.findViewById(R.id.chipHair);

        TextView[] chips = {chipAvailable, chipAll, chipFace, chipBody, chipHair};
        String[] filters = {"AVAILABLE", "ALL", "FACE", "BODY", "HAIR"};

        for (int i = 0; i < chips.length; i++) {
            final TextView chip = chips[i];
            if (chip == null) continue;
            final String filter = filters[i];

            chip.setOnClickListener(v -> {
                currentFilter = filter;

                for (TextView c : chips) {
                    if (c == null) continue;
                    if (c == chip) {
                        c.setBackgroundResource(R.drawable.bg_btn_buy);
                        c.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#6E846A")));
                        c.setTextColor(Color.WHITE);
                    } else {
                        c.setBackgroundResource(R.drawable.com_bg_btn_outline);
                        c.setBackgroundTintList(null);
                        c.setTextColor(Color.BLACK);
                    }
                }

                renderProducts();
            });
        }
    }

    private void loadData() {
        try {
            String currentUserId = ProfileSessionHelper.getEffectiveUserId(requireContext());
            if (currentUserId == null || currentUserId.isEmpty()) {
                currentUserId = "test_001";
            }
            try {
                StringBuilder usersStr = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(requireContext().getAssets().open("users.json")))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        usersStr.append(line);
                    }
                }
                JSONArray usersArr = new JSONArray(usersStr.toString());
                for (int i = 0; i < usersArr.length(); i++) {
                    JSONObject obj = usersArr.getJSONObject(i);
                    if (ProfileSession.getEmail(requireContext()) != null
                            && ProfileSession.getEmail(requireContext()).equals(obj.optString("email"))) {
                        currentUserId = obj.optString("user_id", currentUserId);
                        break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            purchasedProductIds.clear();
            List<OrderEntity> orders = new LocalJsonReader(requireContext()).getAllOrders();
            for (OrderEntity order : orders) {
                if (currentUserId.equals(order.getUserId()) && "Hoàn tất".equals(order.getStatus())) {
                    for (OrderItem item : order.getItems()) {
                        if (item.getProductId() != null && !item.getProductId().isEmpty()) {
                            purchasedProductIds.add(item.getProductId());
                        }
                    }
                }
            }

            StringBuilder jsonProducts = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(requireContext().getAssets().open("products.json")))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    jsonProducts.append(line);
                }
            }
            JSONObject productsData = new JSONObject(jsonProducts.toString());
            JSONArray productsArr = productsData.optJSONArray("products");
            if (productsArr == null) productsArr = new JSONArray();

            allProducts.clear();
            for (int i = 0; i < productsArr.length(); i++) {
                JSONObject p = productsArr.optJSONObject(i);
                if (p == null) continue;
                String id = p.optString("id", p.optString("_id", ""));
                if (purchasedProductIds.contains(id)) {
                    allProducts.add(p);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void renderProducts() {
        llAddProductsContainer.removeAllViews();

        String showcaseUserId = ProfileSessionHelper.getEffectiveUserId(requireContext());
        if (showcaseUserId == null || showcaseUserId.isEmpty()) {
            showcaseUserId = "test_001";
        }
        final String finalShowcaseUserId = showcaseUserId;

        List<JSONObject> filtered = new ArrayList<>();
        for (JSONObject p : allProducts) {
            String id = p.optString("id", p.optString("_id", ""));
            JSONArray subcatsArr = p.optJSONArray("subcategory");
            String subcats = subcatsArr != null ? subcatsArr.toString().toLowerCase() : "";
            String cats = p.optString("category").toLowerCase();

            boolean matches = false;
            switch (currentFilter) {
                case "AVAILABLE":
                    matches = purchasedProductIds.contains(id);
                    break;
                case "ALL":
                    matches = true;
                    break;
                case "FACE":
                    matches = subcats.contains("mặt") || cats.contains("mặt");
                    break;
                case "BODY":
                    matches = subcats.contains("cơ thể") || cats.contains("cơ thể") || subcats.contains("body");
                    break;
                case "HAIR":
                    matches = subcats.contains("tóc") || cats.contains("tóc") || subcats.contains("hair");
                    break;
                default:
                    matches = true;
                    break;
            }
            if (matches) {
                filtered.add(p);
            }
        }

        for (JSONObject p : filtered) {
            String id = p.optString("id", p.optString("_id", ""));
            String name = p.optString("name", "Sản phẩm");
            String img = p.optString("mainImage", p.optString("image", ""));
            long price = p.optLong("price", 0L);
            long commission = (long) (price * 0.08);

            boolean isPurchased = purchasedProductIds.contains(id);
            boolean isDisplayed = AffiliateProductsHelper.isProductDisplayed(requireContext(), finalShowcaseUserId, id);
            boolean isShowcased = isPurchased && isDisplayed;

            View item = LayoutInflater.from(getContext()).inflate(R.layout.com_item_affiliate_add_product_card, llAddProductsContainer, false);

            TextView tvProductName = item.findViewById(R.id.tvProductName);
            if (tvProductName != null) tvProductName.setText(name);

            TextView tvProductPrice = item.findViewById(R.id.tvProductPrice);
            if (tvProductPrice != null) tvProductPrice.setText(format.format(price));

            TextView tvProductCommission = item.findViewById(R.id.tvProductCommission);
            if (tvProductCommission != null) tvProductCommission.setText("Hoa hồng: " + format.format(commission));

            ImageView ivProduct = item.findViewById(R.id.ivProductImage);
            if (ivProduct != null && !img.isEmpty()) {
                com.bumptech.glide.Glide.with(ivProduct.getContext()).load(img).into(ivProduct);
            }

            View vOverlay = item.findViewById(R.id.vOverlay);
            TextView btnAdd = item.findViewById(R.id.btnAdd);

            if (isShowcased) {
                if (vOverlay != null) vOverlay.setVisibility(View.GONE);
                if (btnAdd != null) {
                    btnAdd.setText("Đã trưng bày");
                    btnAdd.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#9E9E9E")));
                    btnAdd.setEnabled(false);
                }
            } else if (isPurchased) {
                if (vOverlay != null) vOverlay.setVisibility(View.GONE);
                if (btnAdd != null) {
                    btnAdd.setText("Trưng bày");
                    btnAdd.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#6E846A")));
                    btnAdd.setEnabled(true);
                    btnAdd.setOnClickListener(v -> {
                        btnAdd.setText("Đã trưng bày");
                        btnAdd.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#9E9E9E")));
                        btnAdd.setEnabled(false);
                        AffiliateProductsHelper.setProductDisplayed(requireContext(), finalShowcaseUserId, id, true);
                        Toast.makeText(getContext(), "Đã thêm sản phẩm vào cửa hàng", Toast.LENGTH_SHORT).show();
                    });
                }
            } else {
                if (vOverlay != null) vOverlay.setVisibility(View.VISIBLE);
                if (btnAdd != null) {
                    btnAdd.setText("Trưng bày");
                    btnAdd.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#6E846A")));
                    btnAdd.setOnClickListener(v -> {
                        Toast.makeText(getContext(), "Bạn cần mua sản phẩm này để có thể gắn link affiliate", Toast.LENGTH_SHORT).show();
                    });
                }
            }

            item.setOnClickListener(v -> ProductDetailLauncher.open(CommunityAddAffiliateProductsFragment.this, id));

            llAddProductsContainer.addView(item);
        }
    }
}
