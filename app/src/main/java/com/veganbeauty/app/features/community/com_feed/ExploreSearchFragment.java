package com.veganbeauty.app.features.community.com_feed;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import com.veganbeauty.app.R;
import com.veganbeauty.app.core.base.RootieFragment;
import com.veganbeauty.app.data.local.LocalJsonReader;
import com.veganbeauty.app.data.local.entities.YtVideoEntity;
import com.veganbeauty.app.utils.ComBottomNavHelper;

import com.veganbeauty.app.data.local.entities.CommunityPostEntity;
import com.veganbeauty.app.features.community.profile.ProfileGridAdapter;
import com.veganbeauty.app.features.community.profile.ProfilePostDetailFragment;
import com.veganbeauty.app.features.community.UserMemoryHelper;
import com.veganbeauty.app.features.community.com_feed.CommunityViewModel;
import com.veganbeauty.app.features.community.com_feed.CommunityViewModelFactory;
import com.veganbeauty.app.data.repository.CommunityRepository;
import com.veganbeauty.app.data.local.RootieDatabase;
import com.veganbeauty.app.data.remote.FirestoreService;
import androidx.lifecycle.ViewModelProvider;
import java.util.HashSet;
import java.util.Set;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExploreSearchFragment extends RootieFragment {

    private RecyclerView rvGrid;
    private ImageView ivBack;
    private EditText etSearch;
    private List<YtVideoEntity> videos = new ArrayList<>();
    private ExploreGridAdapter adapter;
    private View llResults;
    private View svSuggestions;
    private TextView tvSearchBtn;
    private LinearLayout llTrendingKeywords;
    private List<YtVideoEntity> allExploreVideos = new ArrayList<>();
    private CommunityViewModel viewModel;
    private List<CommunityPostEntity> allCommunityPosts = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.com_fragment_explore_search, container, false);
        rvGrid = view.findViewById(R.id.rvExploreGrid);
        ivBack = view.findViewById(R.id.ivBack);
        etSearch = view.findViewById(R.id.etSearch);
        llResults = view.findViewById(R.id.llResults);
        svSuggestions = view.findViewById(R.id.svSuggestions);
        tvSearchBtn = view.findViewById(R.id.tvSearchBtn);
        llTrendingKeywords = view.findViewById(R.id.llTrendingKeywords);
        return view;
    }

    @Override
    protected void setupUI(@NonNull View view) {
        ivBack.setOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            } else {
                getParentFragmentManager().beginTransaction()
                        .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                        .replace(R.id.main_container, new CommunityExploreFragment())
                        .commit();
            }
        });

        LocalJsonReader reader = new LocalJsonReader(requireContext());
        List<YtVideoEntity> allVideos = reader.getExploreVideos();
        allExploreVideos.clear();
        for (YtVideoEntity v : allVideos) {
            String t = v.getType().toLowerCase();
            if (!t.contains("notebook") && !t.contains("cẩm nang")) {
                allExploreVideos.add(v);
            }
        }

        adapter = new ExploreGridAdapter(videos, video -> {
            CommunityExploreFragment fragment = new CommunityExploreFragment();
            Bundle args = new Bundle();
            args.putString("target_video_id", video.getId());
            fragment.setArguments(args);

            getParentFragmentManager().beginTransaction()
                    .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                    .replace(R.id.main_container, fragment)
                    .commit();
        });
        rvGrid.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        rvGrid.setAdapter(adapter);

        boolean isSavedMode = getArguments() != null && getArguments().getBoolean("SAVED_MODE", false);

        if (isSavedMode) {
            etSearch.setVisibility(View.GONE);
            tvSearchBtn.setVisibility(View.GONE);
            svSuggestions.setVisibility(View.GONE);
            llResults.setVisibility(View.VISIBLE);
            
            LinearLayout llTopBar = view.findViewById(R.id.llTopBar);
            
            TextView tvTitle = new TextView(requireContext());
            tvTitle.setText("Mục đã lưu");
            tvTitle.setTextSize(18f);
            tvTitle.setTextColor(Color.parseColor("#3E4D44"));
            tvTitle.setGravity(android.view.Gravity.CENTER);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            tvTitle.setLayoutParams(lp);
            
            try {
                Typeface tf = androidx.core.content.res.ResourcesCompat.getFont(requireContext(), R.font.be_vietnam_pro);
                if (tf != null) tvTitle.setTypeface(tf, Typeface.BOLD);
            } catch (Exception e) {}
            
            View dummy = new View(requireContext());
            dummy.setLayoutParams(new LinearLayout.LayoutParams((int)(28 * getResources().getDisplayMetrics().density), 1));
            
            llTopBar.addView(tvTitle);
            llTopBar.addView(dummy);
            
            try {
                float density = getResources().getDisplayMetrics().density;
                
                // Hide old horizontal scroll view
                View horizontalScroll = ((ViewGroup) llResults).getChildAt(0);
                horizontalScroll.setVisibility(View.GONE);
                
                // 1. Full-width Segmented Control for Bài viết / Video
                LinearLayout segmentedContainer = new LinearLayout(requireContext());
                segmentedContainer.setOrientation(LinearLayout.HORIZONTAL);
                android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
                gd.setColor(Color.parseColor("#F4F6F2"));
                gd.setCornerRadius(48f * density);
                segmentedContainer.setBackground(gd);
                LinearLayout.LayoutParams segLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                segLp.setMargins((int)(16*density), (int)(16*density), (int)(16*density), 0);
                segmentedContainer.setLayoutParams(segLp);
                segmentedContainer.setPadding((int)(4*density), (int)(4*density), (int)(4*density), (int)(4*density));

                TextView tvArticle = new TextView(requireContext());
                tvArticle.setText("Bài viết");
                tvArticle.setGravity(android.view.Gravity.CENTER);
                tvArticle.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                tvArticle.setPadding(0, (int)(12*density), 0, (int)(12*density));
                tvArticle.setTextColor(Color.parseColor("#677559"));
                tvArticle.setTextSize(14f);
                tvArticle.setBackground(null);

                TextView tvVideo = new TextView(requireContext());
                tvVideo.setText("Video");
                tvVideo.setGravity(android.view.Gravity.CENTER);
                tvVideo.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                tvVideo.setPadding(0, (int)(12*density), 0, (int)(12*density));
                tvVideo.setTextColor(Color.WHITE);
                tvVideo.setTextSize(14f);
                
                android.graphics.drawable.GradientDrawable activeGd = new android.graphics.drawable.GradientDrawable();
                activeGd.setColor(Color.parseColor("#3E4D44"));
                activeGd.setCornerRadius(44f * density);
                tvVideo.setBackground(activeGd);

                try {
                    Typeface tf = androidx.core.content.res.ResourcesCompat.getFont(requireContext(), R.font.be_vietnam_pro);
                    if (tf != null) {
                        tvArticle.setTypeface(tf, Typeface.BOLD);
                        tvVideo.setTypeface(tf, Typeface.BOLD);
                    }
                } catch (Exception e) {}

                segmentedContainer.addView(tvArticle);
                segmentedContainer.addView(tvVideo);
                ((LinearLayout) llResults).addView(segmentedContainer, 0);

                // 2. Three Icons Tab Row: Save, Repost, Heart (declared here to be accessible in lambda)
                LinearLayout iconsRow = new LinearLayout(requireContext());
                iconsRow.setOrientation(LinearLayout.HORIZONTAL);

                // Segmented click listeners
                final boolean[] isVideoState = {true}; // default is Video
                View.OnClickListener segClick = v -> {
                    isVideoState[0] = (v == tvVideo);
                    tvVideo.setBackground(isVideoState[0] ? activeGd : null);
                    tvVideo.setTextColor(isVideoState[0] ? Color.WHITE : Color.parseColor("#677559"));
                    tvArticle.setBackground(!isVideoState[0] ? activeGd : null);
                    tvArticle.setTextColor(!isVideoState[0] ? Color.WHITE : Color.parseColor("#677559"));
                    
                    int iconTag = iconsRow.getTag() != null ? (int) iconsRow.getTag() : 0;
                    loadFilteredVideos(isVideoState[0], iconTag);
                };
                tvArticle.setOnClickListener(segClick);
                tvVideo.setOnClickListener(segClick);
                iconsRow.setWeightSum(3);
                iconsRow.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                iconsRow.setPadding(0, (int)(24*density), 0, (int)(16*density));
                
                int colorActive = Color.parseColor("#3E4D44");
                int colorInactive = Color.parseColor("#888888");
                
                // Arrays to hold references
                ImageView[] ivs = new ImageView[3];
                View[] lines = new View[3];
                int[] iconRes = {R.drawable.ic_save, R.drawable.ic_reup, R.drawable.ic_heart_filled};
                
                for (int i = 0; i < 3; i++) {
                    LinearLayout container = new LinearLayout(requireContext());
                    container.setOrientation(LinearLayout.VERTICAL);
                    container.setGravity(android.view.Gravity.CENTER);
                    container.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                    
                    ImageView iv = new ImageView(requireContext());
                    iv.setImageResource(iconRes[i]);
                    iv.setLayoutParams(new LinearLayout.LayoutParams((int)(24 * density), (int)(24 * density)));
                    iv.setColorFilter(i == 0 ? colorActive : colorInactive);
                    ivs[i] = iv;
                    
                    View line = new View(requireContext());
                    LinearLayout.LayoutParams lpLine = new LinearLayout.LayoutParams((int)(32 * density), (int)(2 * density));
                    lpLine.topMargin = (int)(8 * density);
                    line.setLayoutParams(lpLine);
                    line.setBackgroundColor(i == 0 ? colorActive : Color.TRANSPARENT);
                    lines[i] = line;
                    
                    container.addView(iv);
                    container.addView(line);
                    iconsRow.addView(container);
                    
                    final int idx = i;
                    container.setOnClickListener(v -> {
                        iconsRow.setTag(idx);
                        for (int j = 0; j < 3; j++) {
                            ivs[j].setColorFilter(j == idx ? colorActive : colorInactive);
                            lines[j].setBackgroundColor(j == idx ? colorActive : Color.TRANSPARENT);
                        }
                        loadFilteredVideos(isVideoState[0], idx); 
                    });
                }
                
                iconsRow.setTag(0); // Default to Save
                ((LinearLayout) llResults).addView(iconsRow, 1);
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            loadFilteredVideos(true, 0); // Default load: Video, Save
        } else {
            setupTrendingKeywords();

            tvSearchBtn.setOnClickListener(v -> performSearch(etSearch.getText().toString()));

            etSearch.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    performSearch(etSearch.getText().toString());
                    return true;
                }
                return false;
            });
        }

        View bottomNav = view.findViewById(R.id.comBottomNav);
        if (bottomNav != null) {
            setupBottomNav(bottomNav);
        }
    }

    private void loadFilteredVideos(boolean isVideo, int iconIndex) {
        List<YtVideoEntity> loadedList = new ArrayList<>();
        try {
            String prefsName = "saved_videos_prefs"; // fallback default
            String keyName = "saved_videos";
            
            if (iconIndex == 0) { // Save
                prefsName = "saved_videos_prefs";
                keyName = "saved_videos";
            } else if (iconIndex == 1) { // Repost
                prefsName = "reposted_videos_prefs";
                keyName = "reposted_videos";
            } else if (iconIndex == 2) { // Heart
                prefsName = "liked_videos_prefs";
                keyName = "liked_videos";
            }

            if (isVideo) {
                android.content.SharedPreferences prefs = requireContext().getSharedPreferences(prefsName, Context.MODE_PRIVATE);
                String savedData = prefs.getString(keyName, "[]");
                org.json.JSONArray savedArray = new org.json.JSONArray(savedData);
                for (int i = 0; i < savedArray.length(); i++) {
                    org.json.JSONObject obj = savedArray.getJSONObject(i);
                    
                    String type = obj.optString("type").toLowerCase();
                    boolean isArticleType = type.contains("notebook") || type.contains("cẩm nang");
                    
                    if (!isArticleType) {
                        YtVideoEntity v = new YtVideoEntity();
                        v.setId(obj.optString("id"));
                        v.setUrl(obj.optString("url"));
                        v.setTitle(obj.optString("title"));
                        v.setDescription(obj.optString("description"));
                        v.setAvatarUrl(obj.optString("avatar_url"));
                        v.setUsername(obj.optString("username"));
                        v.setType(obj.optString("type"));
                        v.setLikesCount(obj.optInt("likes_count"));
                        v.setCommentsCount(obj.optInt("comments_count"));
                        v.setShareCount(obj.optInt("share_count"));
                        loadedList.add(v);
                    }
                }
                videos.clear();
                videos.addAll(loadedList);
                rvGrid.setLayoutManager(new GridLayoutManager(requireContext(), 2));
                adapter = new ExploreGridAdapter(videos, video -> {
                    CommunityExploreFragment fragment = new CommunityExploreFragment();
                    Bundle args = new Bundle();
                    args.putString("target_video_id", video.getId());
                    fragment.setArguments(args);
        
                    getParentFragmentManager().beginTransaction()
                            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                            .replace(R.id.main_container, fragment)
                            .addToBackStack(null)
                            .commit();
                });
                rvGrid.setAdapter(adapter);
                adapter.notifyDataSetChanged();
            } else {
                List<CommunityPostEntity> loadedPosts = new ArrayList<>();
                Context ctx = requireContext();
                String ownUserId = com.veganbeauty.app.data.local.ProfileSession.getUserId(ctx);
                if (ownUserId == null || ownUserId.isEmpty()) ownUserId = "test_001";
                
                Set<String> filterIds = new HashSet<>();
                if (iconIndex == 0) {
                    filterIds = UserMemoryHelper.getSavedPostIds(ctx, ownUserId);
                } else if (iconIndex == 1) {
                    filterIds = UserMemoryHelper.getRepostedPostIds(ctx, ownUserId);
                } else if (iconIndex == 2) {
                    android.content.SharedPreferences prefs = ctx.getSharedPreferences("rootie_prefs", Context.MODE_PRIVATE);
                    for (CommunityPostEntity p : allCommunityPosts) {
                        if (prefs.getBoolean("liked_" + ownUserId + "_" + p.getPostId(), false)) {
                            filterIds.add(p.getPostId());
                        }
                    }
                }

                for (CommunityPostEntity p : allCommunityPosts) {
                    if (filterIds.contains(p.getPostId())) {
                        loadedPosts.add(p);
                    }
                }

                rvGrid.setLayoutManager(new GridLayoutManager(requireContext(), 3));
                String finalOwnUserId = ownUserId;
                ProfileGridAdapter postAdapter = new ProfileGridAdapter(loadedPosts, position -> {
                    ProfilePostDetailFragment fragment = ProfilePostDetailFragment.newInstance(finalOwnUserId, position, 0, loadedPosts.get(position).getPostId());
                    getParentFragmentManager().beginTransaction()
                            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                            .replace(R.id.main_container, fragment)
                            .addToBackStack(null)
                            .commit();
                });
                rvGrid.setAdapter(postAdapter);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupTrendingKeywords() {
        Map<String, Integer> keywordsMap = new HashMap<>();
        for (YtVideoEntity video : allExploreVideos) {
            List<String> tags = new ArrayList<>();
            String[] kws = video.getKeywords().split(",");
            for (String kw : kws) tags.add(kw);

            String[] hts = video.getHashtags().split("\\s+|,|#");
            for (String ht : hts) tags.add(ht);

            for (String kw : tags) {
                String cleaned = kw.replace("#", "").trim().toLowerCase();
                if (!cleaned.isEmpty()) {
                    keywordsMap.put(cleaned, keywordsMap.getOrDefault(cleaned, 0) + 1);
                }
            }
        }

        List<Map.Entry<String, Integer>> list = new ArrayList<>(keywordsMap.entrySet());
        list.sort((o1, o2) -> o2.getValue().compareTo(o1.getValue()));

        List<String> topKeywords = new ArrayList<>();
        for (int i = 0; i < Math.min(10, list.size()); i++) {
            topKeywords.add(list.get(i).getKey());
        }

        llTrendingKeywords.removeAllViews();
        for (String kw : topKeywords) {
            TextView tv = new TextView(requireContext());
            tv.setText("• " + kw);
            tv.setTextSize(14f);
            tv.setTextColor(Color.parseColor("#3E4D44"));
            tv.setPadding(0, 24, 0, 24);
            tv.setTypeface(null, Typeface.BOLD);
            tv.setOnClickListener(v -> {
                etSearch.setText(kw);
                etSearch.setSelection(kw.length());
                performSearch(kw);
            });
            llTrendingKeywords.addView(tv);
        }
    }

    private void performSearch(String query) {
        String q = query.toLowerCase().trim();
        if (q.isEmpty()) return;

        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);
        }

        List<YtVideoEntity> filtered = new ArrayList<>();
        for (YtVideoEntity v : allExploreVideos) {
            if (v.getTitle().toLowerCase().contains(q) ||
                    v.getKeywords().toLowerCase().contains(q) ||
                    v.getHashtags().toLowerCase().contains(q) ||
                    v.getDescription().toLowerCase().contains(q)) {
                filtered.add(v);
            }
        }

        videos.clear();
        videos.addAll(filtered);
        adapter.notifyDataSetChanged();

        svSuggestions.setVisibility(View.GONE);
        llResults.setVisibility(View.VISIBLE);
    }

    private void setupBottomNav(View bottomNav) {
        ComBottomNavHelper.setup(this, bottomNav, ComBottomNavHelper.TAB_EXPLORE);
    }

    @Override
    protected void observeViewModel() {
        RootieDatabase db = RootieDatabase.getDatabase(requireContext());
        CommunityRepository repository = new CommunityRepository(db.communityDao(), new LocalJsonReader(requireContext()), new FirestoreService());
        CommunityViewModelFactory factory = new CommunityViewModelFactory(repository);
        viewModel = new ViewModelProvider(requireActivity(), factory).get(CommunityViewModel.class);

        LocalJsonReader jsonReader = new LocalJsonReader(requireContext());
        viewModel.getPosts().observe(getViewLifecycleOwner(), dbPosts -> {
            List<CommunityPostEntity> newsList = jsonReader.getCommunityNews();
            allCommunityPosts.clear();
            if (dbPosts != null) allCommunityPosts.addAll(dbPosts);
            if (newsList != null) allCommunityPosts.addAll(newsList);
        });
    }

    class ExploreGridAdapter extends RecyclerView.Adapter<ExploreGridAdapter.ViewHolder> {
        private final List<YtVideoEntity> list;
        private final OnVideoClickListener listener;

        ExploreGridAdapter(List<YtVideoEntity> list, OnVideoClickListener listener) {
            this.list = list;
            this.listener = listener;
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView ivThumbnail;
            TextView tvTitle;
            ImageView ivAvatar;
            TextView tvUsername;
            TextView tvDate;
            TextView tvLikes;

            ViewHolder(View view) {
                super(view);
                ivThumbnail = view.findViewById(R.id.ivThumbnail);
                tvTitle = view.findViewById(R.id.tvTitle);
                ivAvatar = view.findViewById(R.id.ivAvatar);
                tvUsername = view.findViewById(R.id.tvUsername);
                tvDate = view.findViewById(R.id.tvDate);
                tvLikes = view.findViewById(R.id.tvLikes);

                view.setOnClickListener(v -> listener.onClick(list.get(getAdapterPosition())));
            }
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.com_item_explore_grid_video, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            YtVideoEntity video = list.get(position);
            holder.tvTitle.setText(video.getTitle());
            holder.tvUsername.setText(!video.getUsername().trim().isEmpty() ? video.getUsername() : "Cộng đồng Rootie");
            holder.tvDate.setText("18 tháng 3 2025");
            holder.tvLikes.setText((video.getLikesCount() / 1000) + "." + ((video.getLikesCount() % 1000) / 100) + "k");

            String videoId = extractYouTubeVideoId(video.getUrl());
            String thumbUrl;
            if (video.getUrl().toLowerCase().contains("cloudinary") && video.getUrl().toLowerCase().endsWith(".mp4")) {
                thumbUrl = video.getUrl().replaceAll("(?i)\\.mp4$", ".jpg");
            } else if (videoId != null) {
                thumbUrl = "https://img.youtube.com/vi/" + videoId + "/hqdefault.jpg";
            } else {
                thumbUrl = video.getUrl();
            }

            com.bumptech.glide.Glide.with(holder.ivThumbnail.getContext()).load(thumbUrl).into(holder.ivThumbnail);

            if (video.getAvatarUrl() == null || video.getAvatarUrl().trim().isEmpty()) {
                int[] avatarRes = {R.drawable.img_avatar, R.drawable.ic_account_outline, R.drawable.img_avatar, R.drawable.ic_account_outline};
                int randomAvatar = avatarRes[(int) (Math.random() * avatarRes.length)];
                com.bumptech.glide.Glide.with(holder.ivAvatar.getContext()).load(randomAvatar).circleCrop().into(holder.ivAvatar);
            } else {
                com.bumptech.glide.Glide.with(holder.ivAvatar.getContext()).load(video.getAvatarUrl()).circleCrop().into(holder.ivAvatar);
            }
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        private String extractYouTubeVideoId(String url) {
            String pattern = "(?<=watch\\?v=|/videos/|embed\\/|youtu.be\\/|\\/v\\/|\\/e\\/|watch\\?v%3D|watch\\?feature=player_embedded&v=|%2Fvideos%2F|embed%\\u200C\\u200B2F|youtu.be%2F|%2Fv%2F)[^#\\&\\?\\n]*";
            Pattern compiledPattern = Pattern.compile(pattern);
            Matcher matcher = compiledPattern.matcher(url);
            if (matcher.find()) {
                return matcher.group();
            }
            return null;
        }
    }

    interface OnVideoClickListener {
        void onClick(YtVideoEntity video);
    }
}
