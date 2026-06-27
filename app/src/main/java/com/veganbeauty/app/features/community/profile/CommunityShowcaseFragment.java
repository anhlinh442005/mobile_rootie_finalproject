package com.veganbeauty.app.features.community.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwnerKt;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import coil.Coil;
import coil.request.ImageRequest;
import coil.transform.CircleCropTransformation;

import com.veganbeauty.app.R;
import com.veganbeauty.app.data.local.LocalJsonReader;
import com.veganbeauty.app.data.local.entities.CommunityProduct;
import com.veganbeauty.app.data.local.RootieDatabase;
import com.veganbeauty.app.databinding.ComFragmentShowcaseBinding;
import com.veganbeauty.app.databinding.ComItemShowcaseProductBinding;
import com.veganbeauty.app.features.shop.product.CartBottomSheetFragment;
import com.veganbeauty.app.features.shop.product.CartHelper;

import kotlinx.coroutines.BuildersKt;
import kotlinx.coroutines.Dispatchers;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class CommunityShowcaseFragment extends Fragment {

    private ComFragmentShowcaseBinding _binding;

    private String userId = null;
    private String avatarUrl = null;
    private String userName = null;
    private String coverUrl = null;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            userId = getArguments().getString("USER_ID");
            avatarUrl = getArguments().getString("AVATAR_URL");
            userName = getArguments().getString("USER_NAME");
            coverUrl = getArguments().getString("COVER_URL");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        _binding = ComFragmentShowcaseBinding.inflate(inflater, container, false);
        return _binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        _binding.ivBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        String titleText = userName != null ? userName : "Na Na";
        _binding.tvShowcaseTitle.setText("Trang trưng bày của " + titleText);

        ImageRequest avatarRequest = new ImageRequest.Builder(requireContext())
                .data(avatarUrl != null ? avatarUrl : "https://i.pinimg.com/736x/1a/d8/4b/1ad84b9ab4a1e2ab17c7aab37fcff0a5.jpg")
                .crossfade(true)
                .transformations(new CircleCropTransformation())
                .error(R.drawable.img_avatar)
                .target(_binding.ivAvatar)
                .build();
        Coil.imageLoader(requireContext()).enqueue(avatarRequest);

        _binding.ivFilterSort.setOnClickListener(v -> {
            CommunitySortBottomSheet bottomSheet = new CommunitySortBottomSheet(0, selectedSort -> {
            });
            bottomSheet.show(getParentFragmentManager(), "CommunitySortBottomSheet");
        });

        ImageRequest coverRequest = new ImageRequest.Builder(requireContext())
                .data(coverUrl != null ? coverUrl : (avatarUrl != null ? avatarUrl : "https://i.pinimg.com/736x/1a/d8/4b/1ad84b9ab4a1e2ab17c7aab37fcff0a5.jpg"))
                .crossfade(true)
                .error(android.R.color.darker_gray)
                .target(_binding.ivCover)
                .build();
        Coil.imageLoader(requireContext()).enqueue(coverRequest);

        android.content.Context ctx = requireContext();
        LocalJsonReader jsonReader = new LocalJsonReader(ctx);

        Map<String, List<String>> socialData = jsonReader.getSocialDataForUser(userId != null ? userId : "test_001");
        int followersCount = (socialData != null && socialData.containsKey("followers")) ? socialData.get("followers").size() : 867;
        _binding.tvFollowers.setText(String.valueOf(followersCount));

        String currentUserId = userId != null ? userId : "test_001";
        List<String> productIds = jsonReader.getShowcaseProductsForUser(currentUserId);
        List<CommunityProduct> allProducts = jsonReader.getProducts();
        List<CommunityProduct> finalProducts = new ArrayList<>();
        Set<String> addedProductIds = new HashSet<>();

        for (String pId : productIds) {
            if (!addedProductIds.contains(pId)) {
                CommunityProduct pData = null;
                for (CommunityProduct cp : allProducts) {
                    if (cp.getId().equals(pId)) {
                        pData = cp;
                        break;
                    }
                }
                if (pData != null) {
                    finalProducts.add(pData);
                    addedProductIds.add(pId);
                }
            }
        }

        _binding.tvProductCount.setText(String.valueOf(finalProducts.size()));

        _binding.rvProducts.setLayoutManager(new LinearLayoutManager(context));
        _binding.rvProducts.setAdapter(new ShowcaseProductAdapter(finalProducts, (product, v) -> {
            List<com.veganbeauty.app.data.local.entities.ProductEntity> allEntityProducts = jsonReader.getAllProducts();
            com.veganbeauty.app.data.local.entities.ProductEntity entityProduct = null;
            for (com.veganbeauty.app.data.local.entities.ProductEntity pe : allEntityProducts) {
                if (pe.getId().equals(product.getId())) {
                    entityProduct = pe;
                    break;
                }
            }
            if (entityProduct != null) {
                CartHelper.addToCart(ctx, LifecycleOwnerKt.getLifecycleScope(getViewLifecycleOwner()), entityProduct, 1);
                animateAddToCart(v, _binding.ivCart);
            }
        }));

        RootieDatabase db = RootieDatabase.getDatabase(ctx);
        BuildersKt.launch(LifecycleOwnerKt.getLifecycleScope(getViewLifecycleOwner()), Dispatchers.getMain(), kotlinx.coroutines.CoroutineStart.DEFAULT, (coroutineScope, continuation) -> {
            db.cartDao().getAllCartItems().collect(new kotlinx.coroutines.flow.FlowCollector<List<com.veganbeauty.app.data.local.entities.CartItemEntity>>() {
                @Nullable
                @Override
                public Object emit(List<com.veganbeauty.app.data.local.entities.CartItemEntity> items, @NonNull kotlin.coroutines.Continuation<? super kotlin.Unit> continuation) {
                    int count = 0;
                    for (com.veganbeauty.app.data.local.entities.CartItemEntity item : items) {
                        count += item.getQuantity();
                    }
                    if (_binding != null) {
                        if (count > 0) {
                            _binding.tvCartBadge.setVisibility(View.VISIBLE);
                            _binding.tvCartBadge.setText(count > 99 ? "99+" : String.valueOf(count));
                        } else {
                            _binding.tvCartBadge.setVisibility(View.GONE);
                        }
                    }
                    return kotlin.Unit.INSTANCE;
                }
            }, continuation);
            return kotlin.Unit.INSTANCE;
        });

        _binding.ivCart.setOnClickListener(v -> {
            CartBottomSheetFragment bottomSheet = new CartBottomSheetFragment();
            bottomSheet.show(getParentFragmentManager(), "CartBottomSheet");
        });
    }

    private void animateAddToCart(View startView, View targetView) {
        ViewGroup rootLayout = (ViewGroup) _binding.getRoot();
        int[] startLoc = new int[2];
        startView.getLocationInWindow(startLoc);
        int[] targetLoc = new int[2];
        targetView.getLocationInWindow(targetLoc);

        int[] rootLoc = new int[2];
        rootLayout.getLocationInWindow(rootLoc);

        float startX = startLoc[0] - rootLoc[0];
        float startY = startLoc[1] - rootLoc[1];
        float targetX = targetLoc[0] - rootLoc[0];
        float targetY = targetLoc[1] - rootLoc[1];

        ImageView flyingIcon = new ImageView(requireContext());
        flyingIcon.setImageResource(R.drawable.ic_cart);
        flyingIcon.setLayoutParams(new ViewGroup.LayoutParams(startView.getWidth(), startView.getHeight()));
        flyingIcon.setX(startX);
        flyingIcon.setY(startY);
        flyingIcon.setElevation(100f);
        flyingIcon.setTranslationZ(100f);

        rootLayout.addView(flyingIcon);

        flyingIcon.animate()
                .x(targetX)
                .y(targetY)
                .scaleX(0.2f)
                .scaleY(0.2f)
                .setDuration(500)
                .setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator())
                .withEndAction(() -> rootLayout.removeView(flyingIcon))
                .start();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        _binding = null;
    }

    private static class ShowcaseProductAdapter extends RecyclerView.Adapter<ShowcaseProductAdapter.ProductViewHolder> {

        private final List<CommunityProduct> products;
        private final OnAddToCartListener onAddToCart;

        interface OnAddToCartListener {
            void onAddToCart(CommunityProduct product, View view);
        }

        public ShowcaseProductAdapter(List<CommunityProduct> products, OnAddToCartListener onAddToCart) {
            this.products = products;
            this.onAddToCart = onAddToCart;
        }

        static class ProductViewHolder extends RecyclerView.ViewHolder {
            ComItemShowcaseProductBinding binding;

            public ProductViewHolder(ComItemShowcaseProductBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }

        @NonNull
        @Override
        public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ComItemShowcaseProductBinding binding = ComItemShowcaseProductBinding.inflate(
                    LayoutInflater.from(parent.getContext()), parent, false
            );
            return new ProductViewHolder(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
            CommunityProduct product = products.get(position);
            holder.binding.tvProductName.setText(product.getName());

            NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
            holder.binding.tvPrice.setText(format.format(product.getPrice()));

            holder.binding.tvSold.setText("Đã bán " + product.getSold());

            if (product.getRating() > 0) {
                holder.binding.tvRating.setVisibility(View.VISIBLE);
                holder.binding.ivStar.setVisibility(View.VISIBLE);
                holder.binding.tvRating.setText(String.format(Locale.US, "%.1f", product.getRating()));
            } else {
                holder.binding.tvRating.setVisibility(View.GONE);
                holder.binding.ivStar.setVisibility(View.GONE);
            }

            if (product.getMainImage() != null && !product.getMainImage().isEmpty()) {
                ImageRequest request = new ImageRequest.Builder(holder.itemView.getContext())
                        .data(product.getMainImage())
                        .crossfade(true)
                        .placeholder(android.R.color.darker_gray)
                        .target(holder.binding.ivProductImage)
                        .build();
                Coil.imageLoader(holder.itemView.getContext()).enqueue(request);
            }

            if (product.getOriginalPrice() != null && product.getOriginalPrice() > product.getPrice()) {
                int discount = (int) (((double) (product.getOriginalPrice() - product.getPrice()) / product.getOriginalPrice()) * 100);
                holder.binding.tvDiscountBadge.setVisibility(View.VISIBLE);
                holder.binding.tvDiscountBadge.setText("-" + discount + "%");
            } else {
                holder.binding.tvDiscountBadge.setVisibility(View.GONE);
            }

            holder.binding.tvShopName.setText("Cocoon Vietnam >");

            holder.binding.tvBuy.setOnClickListener(v -> onAddToCart.onAddToCart(product, v));
            holder.binding.ivAddToCart.setOnClickListener(v -> onAddToCart.onAddToCart(product, v));
        }

        @Override
        public int getItemCount() {
            return products.size();
        }
    }
}
