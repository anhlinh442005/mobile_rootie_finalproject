package com.veganbeauty.app.features.community.beauty_hub;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import coil.Coil;
import coil.request.ImageRequest;
import com.veganbeauty.app.R;
import com.veganbeauty.app.data.local.entities.YtVideoEntity;

import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HandbookVideoAdapter extends RecyclerView.Adapter<HandbookVideoAdapter.ViewHolder> {

    public interface IsSavedChecker {
        boolean isSaved(YtVideoEntity video);
    }

    public interface OnItemClickListener {
        void onItemClick(YtVideoEntity video);
    }

    public interface OnHeartClickListener {
        void onHeartClick(YtVideoEntity video);
    }

    public interface OnDeleteClickListener {
        void onDeleteClick(YtVideoEntity video);
    }

    private final List<YtVideoEntity> videos;
    private final boolean isHorizontal;
    private final IsSavedChecker isSaved;
    private final OnItemClickListener onItemClick;
    private final OnHeartClickListener onHeartClick;
    private final OnDeleteClickListener onDeleteClick;

    public HandbookVideoAdapter(List<YtVideoEntity> videos, boolean isHorizontal, IsSavedChecker isSaved, 
                                OnItemClickListener onItemClick, OnHeartClickListener onHeartClick, OnDeleteClickListener onDeleteClick) {
        this.videos = videos;
        this.isHorizontal = isHorizontal;
        this.isSaved = isSaved != null ? isSaved : v -> false;
        this.onItemClick = onItemClick;
        this.onHeartClick = onHeartClick;
        this.onDeleteClick = onDeleteClick;
    }

    public HandbookVideoAdapter(List<YtVideoEntity> videos, OnItemClickListener onItemClick, OnHeartClickListener onHeartClick) {
        this(videos, false, null, onItemClick, onHeartClick, null);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final ImageView ivThumbnail;
        public final TextView tvTitle;
        public final TextView tvLikes;
        public final TextView tvDuration;
        public final ImageView ivHeart;
        public final LinearLayout llHeartContainer;
        public final TextView tvDetails;
        public final ImageView ivDelete;

        public ViewHolder(View view) {
            super(view);
            ivThumbnail = view.findViewById(R.id.ivThumbnail);
            tvTitle = view.findViewById(R.id.tvTitle);
            tvLikes = view.findViewById(R.id.tvLikes);
            tvDuration = view.findViewById(R.id.tvDuration);
            ivHeart = view.findViewById(R.id.ivHeart);
            llHeartContainer = view.findViewById(R.id.llHeartContainer);
            tvDetails = view.findViewById(R.id.tvDetails);
            ivDelete = view.findViewById(R.id.ivDelete);

            view.setOnClickListener(v -> {
                if (onItemClick != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    onItemClick.onItemClick(videos.get(getAdapterPosition()));
                }
            });
            
            llHeartContainer.setOnClickListener(v -> {
                if (onHeartClick != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    onHeartClick.onHeartClick(videos.get(getAdapterPosition()));
                }
            });
            
            ivDelete.setOnClickListener(v -> {
                if (onDeleteClick != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    onDeleteClick.onDeleteClick(videos.get(getAdapterPosition()));
                }
            });
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.com_item_handbook_video, parent, false);
        if (isHorizontal) {
            float dp = parent.getContext().getResources().getDisplayMetrics().density;
            ViewGroup.LayoutParams params = view.getLayoutParams();
            params.width = (int) (160 * dp);
            view.setLayoutParams(params);
        }
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        YtVideoEntity video = videos.get(position);
        holder.tvTitle.setText(video.getTitle());
        
        if (isHorizontal && onDeleteClick != null) {
            holder.tvDetails.setVisibility(View.GONE);
            holder.ivDelete.setVisibility(View.VISIBLE);
        } else {
            holder.tvDetails.setVisibility(View.VISIBLE);
            holder.ivDelete.setVisibility(View.GONE);
        }
        
        Random random = new Random();
        float fakeLikes = (random.nextInt(491) + 10) / 10f;
        holder.tvLikes.setText(String.format(Locale.US, "%.1fk", fakeLikes).replace(".", ","));
        
        String videoId = extractYouTubeVideoId(video.getUrl());
        String thumbnailUrl = videoId != null ? "https://img.youtube.com/vi/" + videoId + "/hqdefault.jpg" : video.getUrl();
        
        ImageRequest request = new ImageRequest.Builder(holder.itemView.getContext())
                .data(thumbnailUrl)
                .target(holder.ivThumbnail)
                .crossfade(true)
                .build();
        Coil.imageLoader(holder.itemView.getContext()).enqueue(request);
        
        int min = random.nextInt(16) + 5;
        int sec = random.nextInt(50) + 10;
        holder.tvDuration.setText(String.format(Locale.US, "%d:%02d", min, sec));

        if (isSaved.isSaved(video)) {
            holder.ivHeart.setColorFilter(Color.RED);
            holder.ivHeart.setImageResource(R.drawable.ic_heart_filled);
        } else {
            holder.ivHeart.clearColorFilter();
            holder.ivHeart.setImageResource(R.drawable.ic_heart); // Optional, reset back to default empty heart
        }
    }

    @Override
    public int getItemCount() {
        return videos.size();
    }

    private String extractYouTubeVideoId(String url) {
        String pattern = "(?<=watch\\?v=|/videos/|embed\\/|youtu.be\\/|\\/v\\/|\\/e\\/|shorts\\/|watch\\?v%3D|watch\\?feature=player_embedded&v=|%2Fvideos%2F|embed%\\u200C\\u200B2F|youtu.be%2F|%2Fv%2F)[^#\\&\\?\\n]*";
        Matcher matcher = Pattern.compile(pattern).matcher(url);
        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }
}
