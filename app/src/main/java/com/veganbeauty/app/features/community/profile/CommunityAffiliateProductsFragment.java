package com.veganbeauty.app.features.community.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import coil.Coil;
import coil.request.ImageRequest;

import com.veganbeauty.app.R;
import com.veganbeauty.app.data.local.LocalJsonReader;
import com.veganbeauty.app.data.local.entities.OrderEntity;
import com.veganbeauty.app.data.local.entities.ProductEntity;
import com.veganbeauty.app.features.community.affiliate.AffiliateProductsHelper;
import com.veganbeauty.app.features.shop.product.detail.ShopDetailFragment;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CommunityAffiliateProductsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.com_fragment_affiliate_products, container, false);

        LinearLayout navOverview = view.findViewById(R.id.navOverview);
        if (navOverview != null) {
            navOverview.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        }

        LinearLayout navOrders = view.findViewById(R.id.navOrders);
        if (navOrders != null) {
            navOrders.setOnClickListener(v -> getParentFragmentManager().beginTransaction().replace(R.id.main_container, new CommunityAffiliateOrdersFragment()).commit());
        }

        LinearLayout navProducts = view.findViewById(R.id.navProducts);
        if (navProducts != null) {
            navProducts.setOnClickListener(v -> getParentFragmentManager().beginTransaction().replace(R.id.main_container, new CommunityAffiliateProductsFragment()).commit());
        }

        LinearLayout navWithdraw = view.findViewById(R.id.navWithdraw);
        if (navWithdraw != null) {
            navWithdraw.setOnClickListener(v -> getParentFragmentManager().beginTransaction().replace(R.id.main_container, new CommunityAffiliateWithdrawFragment()).commit());
        }

        loadProductsData(view);

        return view;
    }

    private void loadProductsData(View view) {
        try {
            DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("vi", "VN"));
            symbols.setGroupingSeparator('.');
            DecimalFormat format = new DecimalFormat("#,###đ", symbols);

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
            Map<String, String> productImagesMap = new HashMap<>();
            for (int i = 0; i < productsArr.length(); i++) {
                JSONObject p = productsArr.optJSONObject(i);
                if (p == null) continue;
                JSONArray categoryIds = p.optJSONArray("categoryId");
                String catId = (categoryIds != null && categoryIds.length() > 0) ? categoryIds.optString(0) : "";
                String img = p.optString("mainImage", p.optString("image", ""));
                if (!catId.isEmpty() && !img.isEmpty()) {
                    productImagesMap.put(catId, img);
                }

                String id = p.optString("id", p.optString("_id"));
                if (!id.isEmpty() && !img.isEmpty()) {
                    productImagesMap.put(id, img);
                }
            }

            class ProductStats {
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

            Map<String, ProductStats> productMap = new HashMap<>();

            String currentUserId = "test_001";
            LocalJsonReader jsonReader = new LocalJsonReader(requireContext());

            List<String> eligibleProductIds = jsonReader.getShowcaseProductsForUser(currentUserId);
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
            List<OrderEntity> completedOrders = new ArrayList<>();
            for (OrderEntity order : allOrders) {
                if ("Hoàn tất".equals(order.getStatus()) && order.getAffiliate() != null && currentUserId.equals(order.getAffiliate().getReferrerUserId())) {
                    completedOrders.add(order);
                }
            }

            for (OrderEntity order : completedOrders) {
                long commission = order.getAffiliate() != null ? order.getAffiliate().getCommissionAmount() : 0L;
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

            LinearLayout llProductsContainer = view.findViewById(R.id.llProductsContainer);
            if (llProductsContainer != null) {
                llProductsContainer.removeAllViews();
            }

            for (Map.Entry<String, ProductStats> entry : productMap.entrySet()) {
                String prodId = entry.getKey();
                ProductStats stats = entry.getValue();
                
                boolean isDisplayed = AffiliateProductsHelper.isProductDisplayed(requireContext(), currentUserId, prodId);
                if (!isDisplayed) continue;

                View prodView = LayoutInflater.from(getContext()).inflate(R.layout.com_item_affiliate_product_card, llProductsContainer, false);

                ((TextView) prodView.findViewById(R.id.tvProductName)).setText(stats.name);
                ((TextView) prodView.findViewById(R.id.tvProductSold)).setText("Đã bán\n" + stats.count);
                TextView tvPrice = prodView.findViewById(R.id.tvProductPrice);
                if (tvPrice != null) tvPrice.setText(format.format(stats.price));
                TextView tvComm = prodView.findViewById(R.id.tvProductCommission);
                if (tvComm != null) tvComm.setText("Hoa hồng: " + format.format(stats.commission));

                ImageView ivProduct = prodView.findViewById(R.id.ivProductImage);
                if (ivProduct != null && !stats.image.isEmpty()) {
                    ImageRequest request = new ImageRequest.Builder(requireContext())
                            .data(stats.image)
                            .target(ivProduct)
                            .build();
                    Coil.imageLoader(requireContext()).enqueue(request);
                }

                prodView.setOnClickListener(v -> {
                    ShopDetailFragment detailFragment = new ShopDetailFragment();
                    getParentFragmentManager().beginTransaction()
                            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                            .replace(R.id.main_container, detailFragment)
                            .addToBackStack(null)
                            .commit();
                });

                Switch swDisplay = prodView.findViewById(R.id.swDisplay);
                if (swDisplay != null) {
                    swDisplay.setOnCheckedChangeListener((buttonView, isChecked) -> {
                        if (!isChecked) {
                            View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_confirm_delete, null);
                            AlertDialog dialog = new AlertDialog.Builder(requireContext(), R.style.CustomDialogTheme)
                                    .setView(dialogView)
                                    .create();
                            
                            if (dialog.getWindow() != null) {
                                dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                            }
                            dialog.setCancelable(false);

                            dialogView.findViewById(R.id.btnCancel).setOnClickListener(v -> {
                                swDisplay.setChecked(true);
                                dialog.dismiss();
                            });

                            dialogView.findViewById(R.id.btnConfirm).setOnClickListener(v -> {
                                if (llProductsContainer != null) {
                                    llProductsContainer.removeView(prodView);
                                }
                                AffiliateProductsHelper.setProductDisplayed(requireContext(), currentUserId, prodId, false);
                                dialog.dismiss();
                            });

                            dialog.show();
                        }
                    });
                }

                if (llProductsContainer != null) {
                    llProductsContainer.addView(prodView);
                }
            }

            View btnAddProduct = view.findViewById(R.id.btnAddProduct);
            if (btnAddProduct != null) {
                btnAddProduct.setOnClickListener(v -> getParentFragmentManager().beginTransaction()
                        .replace(R.id.main_container, new CommunityAddAffiliateProductsFragment())
                        .addToBackStack(null)
                        .commit());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
