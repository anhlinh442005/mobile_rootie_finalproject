package com.veganbeauty.app.features.community.profile;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.veganbeauty.app.R;
import com.veganbeauty.app.data.local.LocalJsonReader;
import com.veganbeauty.app.data.local.entities.OrderEntity;
import com.veganbeauty.app.data.local.entities.ProductEntity;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CommunityAffiliateProductsFragment extends Fragment {

    private static final String FILTER_ALL = "ALL";
    private static final String FILTER_SOLD = "SOLD";

    private LinearLayout llProductsContainer;
    private EditText etSearch;
    private LinearLayout btnFilter;
    private TextView tvFilterLabel;
    private DecimalFormat format;
    private String currentUserId;
    private String searchQuery = "";
    private String currentFilter = FILTER_ALL;
    private final List<ProductEntry> productEntries = new ArrayList<>();

    private static class ProductStats {
        final String name;
        int count;
        long commission;
        final String image;
        final long price;

        ProductStats(String name, int count, long commission, String image, long price) {
            this.name = name;
            this.count = count;
            this.commission = commission;
            this.image = image;
            this.price = price;
        }
    }

    private static class ProductEntry {
        final String productId;
        final ProductStats stats;

        ProductEntry(String productId, ProductStats stats) {
            this.productId = productId;
            this.stats = stats;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.com_fragment_affiliate_products, container, false);

        llProductsContainer = view.findViewById(R.id.llProductsContainer);
        etSearch = view.findViewById(R.id.etSearch);
        btnFilter = view.findViewById(R.id.btnFilter);
        if (btnFilter != null) {
            tvFilterLabel = btnFilter.findViewById(R.id.tvFilterLabel);
        }

        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("vi", "VN"));
        symbols.setGroupingSeparator('.');
        format = new DecimalFormat("#,###đ", symbols);

        LinearLayout navOverview = view.findViewById(R.id.navOverview);
        if (navOverview != null) {
            navOverview.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        }

        LinearLayout navOrders = view.findViewById(R.id.navOrders);
        if (navOrders != null) {
            navOrders.setOnClickListener(v -> getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, new CommunityAffiliateOrdersFragment())
                    .commit());
        }

        LinearLayout navProducts = view.findViewById(R.id.navProducts);
        if (navProducts != null) {
            navProducts.setOnClickListener(v -> getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, new CommunityAffiliateProductsFragment())
                    .commit());
        }

        LinearLayout navWithdraw = view.findViewById(R.id.navWithdraw);
        if (navWithdraw != null) {
            navWithdraw.setOnClickListener(v -> getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, new CommunityAffiliateWithdrawFragment())
                    .commit());
        }

        View btnAddProduct = view.findViewById(R.id.btnAddProduct);
        if (btnAddProduct != null) {
            btnAddProduct.setOnClickListener(v -> getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, new CommunityAddAffiliateProductsFragment())
                    .addToBackStack(null)
                    .commit());
        }

        setupSearch();
        setupFilter();
        loadProductsData();
        renderProducts();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (llProductsContainer == null) {
            return;
        }
        loadProductsData();
        renderProducts();
    }

    private void setupSearch() {
        if (etSearch == null) {
            return;
        }
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s != null ? s.toString().trim().toLowerCase(Locale.getDefault()) : "";
                renderProducts();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void setupFilter() {
        if (btnFilter == null) {
            return;
        }
        btnFilter.setOnClickListener(v -> {
            currentFilter = FILTER_ALL.equals(currentFilter) ? FILTER_SOLD : FILTER_ALL;
            if (tvFilterLabel != null) {
                tvFilterLabel.setText(FILTER_SOLD.equals(currentFilter) ? "Đã bán" : "Lọc");
            }
            renderProducts();
        });
    }

    private void loadProductsData() {
        productEntries.clear();
        try {
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(requireContext().getAssets().open("products.json")))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }
            String jsonProducts = sb.toString();
            JSONObject productsData = new JSONObject(jsonProducts);
            JSONArray productsArr = productsData.optJSONArray("products");
            if (productsArr == null) {
                productsArr = new JSONArray(jsonProducts);
            }

            Map<String, ProductStats> productMap = new HashMap<>();

            currentUserId = ProfileSessionHelper.getEffectiveUserId(requireContext());
            if (currentUserId == null) {
                currentUserId = "";
            }
            final String finalUserId = currentUserId;
            LocalJsonReader jsonReader = new LocalJsonReader(requireContext());

            List<String> eligibleProductIds = AffiliateProductsHelper.getShowcaseProductIds(requireContext(), finalUserId);
            List<ProductEntity> allProducts = jsonReader.getAllProducts();

            for (String pId : eligibleProductIds) {
                ProductEntity pData = null;
                for (ProductEntity pe : allProducts) {
                    if (pe.getId().equals(pId)) {
                        pData = pe;
                        break;
                    }
                }
                String name = pData != null ? pData.getName() : "Sản phẩm " + pId;
                String img = pData != null ? pData.getMainImage() : "";
                long price = pData != null ? pData.getPrice() : 0L;
                productMap.put(pId, new ProductStats(name, 0, 0L, img, price));
            }

            List<OrderEntity> allOrders = jsonReader.getAllOrders();
            for (OrderEntity order : allOrders) {
                if (!"Hoàn tất".equals(order.getStatus())
                        || order.getAffiliate() == null
                        || !finalUserId.equals(order.getAffiliate().getReferrerUserId())) {
                    continue;
                }
                long commission = order.getAffiliate().getCommissionAmount();
                for (OrderEntity.OrderItem item : order.getItems()) {
                    String pId = item.getProductId();
                    long itemComm = !order.getItems().isEmpty() ? commission / order.getItems().size() : 0L;
                    ProductStats stats = productMap.get(pId);
                    if (stats != null) {
                        stats.count += item.getQuantity();
                        stats.commission += (itemComm * item.getQuantity());
                    }
                }
            }

            for (Map.Entry<String, ProductStats> entry : productMap.entrySet()) {
                String prodId = entry.getKey();
                if (!AffiliateProductsHelper.isProductDisplayed(requireContext(), finalUserId, prodId)) {
                    continue;
                }
                productEntries.add(new ProductEntry(prodId, entry.getValue()));
            }

            Collections.sort(productEntries, Comparator.comparing(
                    e -> e.stats.name,
                    String.CASE_INSENSITIVE_ORDER
            ));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void renderProducts() {
        if (llProductsContainer == null) {
            return;
        }
        llProductsContainer.removeAllViews();

        List<ProductEntry> visibleEntries = new ArrayList<>();
        for (ProductEntry entry : productEntries) {
            if (!matchesSearch(entry)) {
                continue;
            }
            if (FILTER_SOLD.equals(currentFilter) && entry.stats.count <= 0) {
                continue;
            }
            visibleEntries.add(entry);
        }

        if (visibleEntries.isEmpty()) {
            TextView emptyView = new TextView(requireContext());
            emptyView.setText(searchQuery.isEmpty()
                    ? "Chưa có sản phẩm affiliate nào được hiển thị."
                    : "Không tìm thấy sản phẩm phù hợp.");
            emptyView.setTextColor(0xFF9E9E9E);
            emptyView.setTextSize(14f);
            emptyView.setPadding(0, 32, 0, 32);
            emptyView.setGravity(android.view.Gravity.CENTER);
            llProductsContainer.addView(emptyView);
            return;
        }

        for (ProductEntry entry : visibleEntries) {
            addProductCard(entry);
        }
    }

    private boolean matchesSearch(ProductEntry entry) {
        if (searchQuery.isEmpty()) {
            return true;
        }
        return entry.stats.name.toLowerCase(Locale.getDefault()).contains(searchQuery);
    }

    private void addProductCard(ProductEntry entry) {
        String prodId = entry.productId;
        ProductStats stats = entry.stats;

        View prodView = LayoutInflater.from(getContext()).inflate(R.layout.com_item_affiliate_product_card, llProductsContainer, false);

        ((TextView) prodView.findViewById(R.id.tvProductName)).setText(stats.name);
        ((TextView) prodView.findViewById(R.id.tvProductSold)).setText("Đã bán\n" + stats.count);
        TextView tvPrice = prodView.findViewById(R.id.tvProductPrice);
        if (tvPrice != null) {
            tvPrice.setText(format.format(stats.price));
        }
        TextView tvComm = prodView.findViewById(R.id.tvProductCommission);
        if (tvComm != null) {
            tvComm.setText("Hoa hồng: " + format.format(stats.commission));
        }

        ImageView ivProduct = prodView.findViewById(R.id.ivProductImage);
        if (ivProduct != null && stats.image != null && !stats.image.isEmpty()) {
            com.bumptech.glide.Glide.with(ivProduct.getContext()).load(stats.image).into(ivProduct);
        }

        prodView.setOnClickListener(v -> ProductDetailLauncher.open(this, prodId));

        FrameLayout swDisplay = prodView.findViewById(R.id.swDisplay);
        ImageView swDisplayThumb = prodView.findViewById(R.id.swDisplayThumb);
        if (swDisplay != null && swDisplayThumb != null) {
            updateSwitchUI(swDisplay, swDisplayThumb, true);
            swDisplay.setOnClickListener(v -> {
                View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_confirm_delete, null);
                AlertDialog dialog = new AlertDialog.Builder(requireContext(), R.style.CustomDialogTheme)
                        .setView(dialogView)
                        .create();

                if (dialog.getWindow() != null) {
                    dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                }
                dialog.setCancelable(false);

                dialogView.findViewById(R.id.btnCancel).setOnClickListener(btn -> {
                    updateSwitchUI(swDisplay, swDisplayThumb, true);
                    dialog.dismiss();
                });

                dialogView.findViewById(R.id.btnConfirm).setOnClickListener(btn -> {
                    AffiliateProductsHelper.setProductDisplayed(requireContext(), currentUserId, prodId, false);
                    productEntries.remove(entry);
                    renderProducts();
                    dialog.dismiss();
                });

                updateSwitchUI(swDisplay, swDisplayThumb, false);
                dialog.show();
            });
        }

        llProductsContainer.addView(prodView);
    }

    private void updateSwitchUI(FrameLayout container, ImageView thumb, boolean enabled) {
        int margin = (int) (2 * getResources().getDisplayMetrics().density);
        if (enabled) {
            container.setBackgroundResource(R.drawable.ic_switch_track_on);
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) thumb.getLayoutParams();
            lp.gravity = Gravity.CENTER_VERTICAL | Gravity.END;
            lp.setMarginStart(0);
            lp.setMarginEnd(margin);
            thumb.setLayoutParams(lp);
        } else {
            container.setBackgroundResource(R.drawable.ic_switch_track_off);
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) thumb.getLayoutParams();
            lp.gravity = Gravity.CENTER_VERTICAL | Gravity.START;
            lp.setMarginEnd(0);
            lp.setMarginStart(margin);
            thumb.setLayoutParams(lp);
        }
    }
}
