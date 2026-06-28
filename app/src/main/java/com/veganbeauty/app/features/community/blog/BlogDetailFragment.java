package com.veganbeauty.app.features.community.blog;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.text.HtmlCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import com.veganbeauty.app.R;
import com.veganbeauty.app.data.local.BlogRepository;
import com.veganbeauty.app.data.local.LocalJsonReader;
import com.veganbeauty.app.data.local.entities.ProductEntity;
import com.veganbeauty.app.features.shop.product.list.ShopListAdapter;

import java.util.ArrayList;
import java.util.List;

public class BlogDetailFragment extends Fragment {

    private static final String ARG_BLOG_POST = "blog_post";
    private int currentProductCount = 6;

    public static BlogDetailFragment newInstance(BlogPost post) {
        BlogDetailFragment fragment = new BlogDetailFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_BLOG_POST, post);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.com_fragment_blog_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() == null) return;
        BlogPost post = (BlogPost) getArguments().getSerializable(ARG_BLOG_POST);
        if (post == null) return;

        ImageView ivBack = view.findViewById(R.id.ivBack);
        if (ivBack != null) {
            ivBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        }

        if (post.getImageUrl() != null && !post.getImageUrl().isEmpty()) {
            ImageView ivPrimaryImage = view.findViewById(R.id.ivPrimaryImage);
            com.bumptech.glide.Glide.with(ivPrimaryImage.getContext()).load(post.getImageUrl()).into(ivPrimaryImage);
        }

        ((TextView) view.findViewById(R.id.tvCategory)).setText(post.getCategory());
        ((TextView) view.findViewById(R.id.tvTitle)).setText(post.getTitle());
        ((TextView) view.findViewById(R.id.tvDate)).setText(post.getDate());
        ((TextView) view.findViewById(R.id.tvShortDescription)).setText(post.getDescription());

        if (post.getContent() != null && !post.getContent().isEmpty()) {
            TextView tvContent = view.findViewById(R.id.tvContent);
            String noImagesHtml = post.getContent().replaceAll("<img[^>]*>", "");
            tvContent.setText(HtmlCompat.fromHtml(noImagesHtml, HtmlCompat.FROM_HTML_MODE_COMPACT));
        } else {
            View tvContent = view.findViewById(R.id.tvContent);
            if (tvContent != null && tvContent.getParent() != null) {
                ((View) tvContent.getParent()).setVisibility(View.GONE);
            }
        }

        if (post.getDoctorName() != null && !post.getDoctorName().isEmpty()) {
            ((TextView) view.findViewById(R.id.tvDoctorName)).setText(post.getDoctorName());
            ((TextView) view.findViewById(R.id.tvDoctorBio)).setText(post.getDoctorBio());
            if (post.getDoctorAvatar() != null && !post.getDoctorAvatar().isEmpty()) {
                ImageView ivDoctorAvatar = view.findViewById(R.id.ivDoctorAvatar);
                com.bumptech.glide.Glide.with(ivDoctorAvatar.getContext()).load(post.getDoctorAvatar()).error(R.drawable.img_avatar).into(ivDoctorAvatar);
            }
        } else {
            View tvDoctorName = view.findViewById(R.id.tvDoctorName);
            if (tvDoctorName != null && tvDoctorName.getParent() != null && tvDoctorName.getParent().getParent() != null && ((View) tvDoctorName.getParent().getParent()).getParent() != null) {
                ((View) ((View) tvDoctorName.getParent().getParent()).getParent()).setVisibility(View.GONE);
            }
        }

        RecyclerView rvRelatedArticles = view.findViewById(R.id.rvRelatedArticles);
        rvRelatedArticles.setLayoutManager(new LinearLayoutManager(requireContext()));
        BlogAdapter blogAdapter = new BlogAdapter(new ArrayList<>(), relatedPost -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, BlogDetailFragment.newInstance(relatedPost))
                    .addToBackStack(null)
                    .commit();
        });
        rvRelatedArticles.setAdapter(blogAdapter);

        BlogRepository blogRepo = new BlogRepository(requireContext());
        List<BlogPost> allPosts = blogRepo.getBlogPostsSync(10, post.getCategory());
        List<BlogPost> relatedPosts = new ArrayList<>();
        for (BlogPost bp : allPosts) {
            if (!bp.getTitle().equals(post.getTitle())) {
                relatedPosts.add(bp);
                if (relatedPosts.size() >= 6) break;
            }
        }

        if (!relatedPosts.isEmpty()) {
            BlogPost featuredPost = relatedPosts.get(0);
            FrameLayout flFeaturedPost = view.findViewById(R.id.flFeaturedPost);
            flFeaturedPost.setVisibility(View.VISIBLE);

            ((TextView) view.findViewById(R.id.tvFeaturedTitle)).setText(featuredPost.getTitle());
            ((TextView) view.findViewById(R.id.tvFeaturedTime)).setText(featuredPost.getDate());
            ((TextView) view.findViewById(R.id.tvFeaturedDesc)).setText(featuredPost.getDescription());

            ImageView ivFeaturedImage = view.findViewById(R.id.ivFeaturedImage);
            if (featuredPost.getImageUrl() != null && !featuredPost.getImageUrl().isEmpty()) {
                com.bumptech.glide.Glide.with(ivFeaturedImage.getContext()).load(featuredPost.getImageUrl()).placeholder(R.color.gray_light).error(R.color.gray_light).into(ivFeaturedImage);
            } else {
                ivFeaturedImage.setImageResource(R.color.gray_light);
            }

            flFeaturedPost.setOnClickListener(v -> getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, BlogDetailFragment.newInstance(featuredPost))
                    .addToBackStack(null)
                    .commit());

            List<BlogPost> remainingPosts = new ArrayList<>();
            for (int i = 1; i < relatedPosts.size(); i++) {
                remainingPosts.add(relatedPosts.get(i));
            }
            blogAdapter.updateData(remainingPosts);
        } else {
            View llRelatedArticles = view.findViewById(R.id.llRelatedArticles);
            if (llRelatedArticles != null) {
                llRelatedArticles.setVisibility(View.GONE);
            }
        }

        RecyclerView rvRelatedProducts = view.findViewById(R.id.rvRelatedProducts);
        rvRelatedProducts.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        ShopListAdapter productAdapter = new ShopListAdapter(
                p -> { /* no-op */ },
                p -> { /* no-op */ }
        );
        rvRelatedProducts.setAdapter(productAdapter);

        List<ProductEntity> allProducts = new LocalJsonReader(requireContext()).getAllProducts();
        List<ProductEntity> relatedProductsList = new ArrayList<>();
        
        for (ProductEntity p : allProducts) {
            if (p.getCategory() != null && p.getCategory().toLowerCase().contains(post.getCategory().toLowerCase())) {
                relatedProductsList.add(p);
            }
        }

        if (relatedProductsList.isEmpty() && "Chăm sóc tóc".equalsIgnoreCase(post.getCategory())) {
            for (ProductEntity p : allProducts) {
                if (p.getCategory() != null && p.getCategory().toLowerCase().contains("tóc")) {
                    relatedProductsList.add(p);
                }
            }
        }
        if (relatedProductsList.isEmpty() && post.getCategory() != null && post.getCategory().toLowerCase().contains("da")) {
            for (ProductEntity p : allProducts) {
                if (p.getCategory() != null && p.getCategory().toLowerCase().contains("da")) {
                    relatedProductsList.add(p);
                }
            }
        }
        if (relatedProductsList.isEmpty() && "Khỏe đẹp".equalsIgnoreCase(post.getCategory())) {
            for (ProductEntity p : allProducts) {
                if (p.getCategory() != null && p.getCategory().toLowerCase().contains("cơ thể")) {
                    relatedProductsList.add(p);
                }
            }
        }
        if (relatedProductsList.isEmpty()) {
            relatedProductsList.addAll(allProducts);
        }

        List<ProductEntity> displayedProducts = new ArrayList<>();
        for (int i = 0; i < Math.min(currentProductCount, relatedProductsList.size()); i++) {
            displayedProducts.add(relatedProductsList.get(i));
        }
        productAdapter.submitList(displayedProducts);

        View btnLoadMoreProducts = view.findViewById(R.id.btnLoadMoreProducts);
        if (relatedProductsList.size() > currentProductCount) {
            btnLoadMoreProducts.setVisibility(View.VISIBLE);
        } else {
            btnLoadMoreProducts.setVisibility(View.GONE);
        }

        btnLoadMoreProducts.setOnClickListener(v -> {
            currentProductCount += 2;
            List<ProductEntity> newDisplayed = new ArrayList<>();
            for (int i = 0; i < Math.min(currentProductCount, relatedProductsList.size()); i++) {
                newDisplayed.add(relatedProductsList.get(i));
            }
            productAdapter.submitList(newDisplayed);
            if (currentProductCount >= relatedProductsList.size()) {
                btnLoadMoreProducts.setVisibility(View.GONE);
            }
        });

        TextView tvRelatedProductsLabel = view.findViewById(R.id.tvRelatedProductsLabel);
        if ("Chăm sóc tóc".equalsIgnoreCase(post.getCategory())) {
            tvRelatedProductsLabel.setText("Sản phẩm dưỡng tóc");
        } else if (post.getCategory() != null && post.getCategory().toLowerCase().contains("da")) {
            tvRelatedProductsLabel.setText("Sản phẩm dưỡng da");
        } else if (post.getCategory() != null && post.getCategory().toLowerCase().contains("cơ thể")) {
            tvRelatedProductsLabel.setText("Sản phẩm chăm sóc cơ thể");
        } else {
            tvRelatedProductsLabel.setText("Sản phẩm gợi ý");
        }
    }
}
