package com.veganbeauty.app.features.community.blog;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.veganbeauty.app.R;
import com.veganbeauty.app.data.local.BlogRepository;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CommunityBlogFragment extends Fragment {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.com_fragment_blog, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        try {
            ImageView ivBack = view.findViewById(R.id.ivBack);
            if (ivBack != null) {
                ivBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());
            }

            RecyclerView rvBlogList = view.findViewById(R.id.rvBlogList);
            BlogAdapter adapter = new BlogAdapter(new java.util.ArrayList<>(), post -> {
                getParentFragmentManager().beginTransaction()
                        .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                        .replace(R.id.main_container, BlogDetailFragment.newInstance(post))
                        .addToBackStack(null)
                        .commit();
            });
            rvBlogList.setLayoutManager(new LinearLayoutManager(requireContext()));
            rvBlogList.setAdapter(adapter);

            if (getContext() == null) return;
            BlogRepository repo = new BlogRepository(getContext());

            TextView tvCountSkincare = view.findViewById(R.id.tvCountSkincare);
            if (tvCountSkincare != null) tvCountSkincare.setText("677 bài viết");
            TextView tvCountHaircare = view.findViewById(R.id.tvCountHaircare);
            if (tvCountHaircare != null) tvCountHaircare.setText("215 bài viết");
            TextView tvCountBodycare = view.findViewById(R.id.tvCountBodycare);
            if (tvCountBodycare != null) tvCountBodycare.setText("823 bài viết");
            TextView tvCountCosmetics = view.findViewById(R.id.tvCountCosmetics);
            if (tvCountCosmetics != null) tvCountCosmetics.setText("156 bài viết");

            View llCategorySkincare = view.findViewById(R.id.llCategorySkincare);
            if (llCategorySkincare != null) llCategorySkincare.setOnClickListener(v -> navigateToCategory("Dưỡng da"));
            View llCategoryHaircare = view.findViewById(R.id.llCategoryHaircare);
            if (llCategoryHaircare != null) llCategoryHaircare.setOnClickListener(v -> navigateToCategory("Chăm sóc tóc"));
            View llCategoryBodycare = view.findViewById(R.id.llCategoryBodycare);
            if (llCategoryBodycare != null) llCategoryBodycare.setOnClickListener(v -> navigateToCategory("Chăm sóc cơ thể"));
            View llCategoryCosmetics = view.findViewById(R.id.llCategoryCosmetics);
            if (llCategoryCosmetics != null) llCategoryCosmetics.setOnClickListener(v -> navigateToCategory("Mỹ phẩm"));

            executor.execute(() -> {
                try {
                    List<BlogPost> posts = repo.getBlogPostsSync(20, null, 0);
                    requireActivity().runOnUiThread(() -> {
                        if (posts != null && !posts.isEmpty() && isAdded()) {
                            BlogPost featured = posts.get(0);
                            TextView tvFeaturedTitle = view.findViewById(R.id.tvFeaturedTitle);
                            if (tvFeaturedTitle != null) tvFeaturedTitle.setText(featured.getTitle());
                            TextView tvFeaturedTime = view.findViewById(R.id.tvFeaturedTime);
                            if (tvFeaturedTime != null) tvFeaturedTime.setText(featured.getDate());
                            TextView tvFeaturedDesc = view.findViewById(R.id.tvFeaturedDesc);
                            if (tvFeaturedDesc != null) tvFeaturedDesc.setText(featured.getDescription());
                            
                            ImageView ivFeaturedImage = view.findViewById(R.id.ivFeaturedImage);
                            if (ivFeaturedImage != null && featured.getImageUrl() != null && !featured.getImageUrl().isEmpty()) {
                                com.bumptech.glide.Glide.with(ivFeaturedImage.getContext()).load(featured.getImageUrl()).into(ivFeaturedImage);
                            }
                            
                            if (posts.size() > 1) {
                                adapter.updateData(posts.subList(1, posts.size()));
                            }
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    requireActivity().runOnUiThread(() -> {
                        if (isAdded()) {
                            TextView tvFeaturedDesc = view.findViewById(R.id.tvFeaturedDesc);
                            if (tvFeaturedDesc != null) tvFeaturedDesc.setText("Error inner: " + e.getMessage());
                        }
                    });
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            TextView tvFeaturedDesc = view.findViewById(R.id.tvFeaturedDesc);
            if (tvFeaturedDesc != null) tvFeaturedDesc.setText("Error outer: " + e.getMessage());
        }
    }
    
    private void navigateToCategory(String categoryName) {
        getParentFragmentManager().beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.main_container, BlogCategoryFragment.newInstance(categoryName))
                .addToBackStack(null)
                .commit();
    }
}
