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

import coil.Coil;
import coil.request.ImageRequest;
import com.veganbeauty.app.R;
import com.veganbeauty.app.data.local.BlogRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BlogCategoryFragment extends Fragment {

    private static final String ARG_CATEGORY_NAME = "category_name";

    private String categoryName;
    private int currentOffset = 0;
    private final List<BlogPost> allPosts = new ArrayList<>();
    private BlogAdapter adapter;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public static BlogCategoryFragment newInstance(String categoryName) {
        BlogCategoryFragment fragment = new BlogCategoryFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CATEGORY_NAME, categoryName);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.com_fragment_blog_category, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            categoryName = getArguments().getString(ARG_CATEGORY_NAME, "Danh mục");
        } else {
            categoryName = "Danh mục";
        }

        ImageView ivBack = view.findViewById(R.id.ivBack);
        if (ivBack != null) {
            ivBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        }

        TextView tvCategoryTitle = view.findViewById(R.id.tvCategoryTitle);
        if (tvCategoryTitle != null) {
            tvCategoryTitle.setText(categoryName);
        }

        RecyclerView rvBlogList = view.findViewById(R.id.rvBlogList);
        adapter = new BlogAdapter(new ArrayList<>(), post -> {
            getParentFragmentManager().beginTransaction()
                    .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                    .replace(R.id.main_container, BlogDetailFragment.newInstance(post))
                    .addToBackStack(null)
                    .commit();
            return null;
        });
        rvBlogList.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvBlogList.setAdapter(adapter);

        View btnLoadMore = view.findViewById(R.id.btnLoadMore);
        
        loadData(view, btnLoadMore, 6);

        if (btnLoadMore != null) {
            btnLoadMore.setOnClickListener(v -> loadData(view, btnLoadMore, 5));
        }
    }

    private void loadData(View view, View btnLoadMore, int limit) {
        if (btnLoadMore != null) btnLoadMore.setEnabled(false);
        
        executor.execute(() -> {
            try {
                if (getContext() == null) return;
                BlogRepository repo = new BlogRepository(getContext());
                List<BlogPost> newPosts = repo.getBlogPostsSync(limit, categoryName, currentOffset);
                
                requireActivity().runOnUiThread(() -> {
                    if (!isAdded()) return;
                    
                    if (newPosts != null && !newPosts.isEmpty()) {
                        if (currentOffset == 0) {
                            BlogPost featured = newPosts.get(0);
                            TextView tvFeaturedTitle = view.findViewById(R.id.tvFeaturedTitle);
                            if (tvFeaturedTitle != null) tvFeaturedTitle.setText(featured.getTitle());
                            TextView tvFeaturedTime = view.findViewById(R.id.tvFeaturedTime);
                            if (tvFeaturedTime != null) tvFeaturedTime.setText(featured.getDate());
                            TextView tvFeaturedDesc = view.findViewById(R.id.tvFeaturedDesc);
                            if (tvFeaturedDesc != null) tvFeaturedDesc.setText(featured.getDescription());
                            
                            ImageView ivFeaturedImage = view.findViewById(R.id.ivFeaturedImage);
                            if (ivFeaturedImage != null && featured.getImageUrl() != null && !featured.getImageUrl().isEmpty()) {
                                ImageRequest request = new ImageRequest.Builder(requireContext())
                                        .data(featured.getImageUrl())
                                        .target(ivFeaturedImage)
                                        .crossfade(true)
                                        .error(R.color.gray_light)
                                        .build();
                                Coil.imageLoader(requireContext()).enqueue(request);
                            }
                            
                            if (tvFeaturedTitle != null && tvFeaturedTitle.getParent() != null && tvFeaturedTitle.getParent().getParent() != null) {
                                View parentView = (View) tvFeaturedTitle.getParent().getParent();
                                parentView.setOnClickListener(v -> {
                                    getParentFragmentManager().beginTransaction()
                                            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                                            .replace(R.id.main_container, BlogDetailFragment.newInstance(featured))
                                            .addToBackStack(null)
                                            .commit();
                                });
                            }
                            
                            if (newPosts.size() > 1) {
                                allPosts.addAll(newPosts.subList(1, newPosts.size()));
                            }
                        } else {
                            allPosts.addAll(newPosts);
                        }
                        
                        adapter.updateData(allPosts);
                        currentOffset += newPosts.size();
                        
                        if (btnLoadMore != null) {
                            btnLoadMore.setVisibility(newPosts.size() == limit ? View.VISIBLE : View.GONE);
                        }
                    } else {
                        if (currentOffset == 0) {
                            TextView tvFeaturedTitle = view.findViewById(R.id.tvFeaturedTitle);
                            if (tvFeaturedTitle != null) tvFeaturedTitle.setText("Chưa có bài viết nào");
                            TextView tvFeaturedDesc = view.findViewById(R.id.tvFeaturedDesc);
                            if (tvFeaturedDesc != null) tvFeaturedDesc.setText("Vui lòng quay lại sau.");
                        }
                        if (btnLoadMore != null) btnLoadMore.setVisibility(View.GONE);
                    }
                    if (btnLoadMore != null) btnLoadMore.setEnabled(true);
                });
            } catch (Exception e) {
                e.printStackTrace();
                requireActivity().runOnUiThread(() -> {
                    if (btnLoadMore != null) btnLoadMore.setEnabled(true);
                });
            }
        });
    }
}
