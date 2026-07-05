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

        setupTrendingKeywords();

        tvSearchBtn.setOnClickListener(v -> performSearch(etSearch.getText().toString()));

        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch(etSearch.getText().toString());
                return true;
            }
            return false;
        });

        View bottomNav = view.findViewById(R.id.comBottomNav);
        if (bottomNav != null) {
            setupBottomNav(bottomNav);
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
    protected void observeViewModel() { }

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
