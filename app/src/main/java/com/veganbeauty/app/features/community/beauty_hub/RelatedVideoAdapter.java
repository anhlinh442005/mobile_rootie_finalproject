package com.veganbeauty.app.features.community.beauty_hub;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import coil.Coil;
import coil.request.ImageRequest;
import com.veganbeauty.app.R;
import com.veganbeauty.app.data.local.entities.YtVideoEntity;

import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RelatedVideoAdapter extends RecyclerView.Adapter<RelatedVideoAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(YtVideoEntity video);
    }

    private List<YtVideoEntity> videos;
    private final OnItemClickListener onItemClick;

    public RelatedVideoAdapter(List<YtVideoEntity> videos, OnItemClickListener onItemClick) {
        this.videos = videos;
        this.onItemClick = onItemClick;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final ImageView ivThumbnail;
        public final TextView tvTitle;
        public final TextView tvChannel;
        public final TextView tvStats;

        public ViewHolder(View view) {
            super(view);
            ivThumbnail = view.findViewById(R.id.ivRelatedThumbnail);
            tvTitle = view.findViewById(R.id.tvRelatedTitle);
            tvChannel = view.findViewById(R.id.tvRelatedChannel);
            tvStats = view.findViewById(R.id.tvRelatedStats);

            view.setOnClickListener(v -> {
                if (getAdapterPosition() != RecyclerView.NO_POSITION) {
                    onItemClick.onItemClick(videos.get(getAdapterPosition()));
                }
            });
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.com_item_related_video, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        YtVideoEntity video = videos.get(position);
        holder.tvTitle.setText(video.getTitle());
        holder.tvChannel.setText(video.getUsername());

        Random random = new Random();
        float fakeViews = (random.nextInt(491) + 10) / 10f;
        int months = random.nextInt(11) + 1;
        
        String statsStr = String.format(java.util.Locale.US, "%.1fk lượt xem • %d tháng trước", fakeViews, months).replace(".", ",");
        holder.tvStats.setText(statsStr);

        String videoId = extractYouTubeVideoId(video.getUrl());
        String thumbnailUrl = (videoId != null) 
                ? "https://img.youtube.com/vi/" + videoId + "/hqdefault.jpg" 
                : video.getUrl();

        ImageRequest request = new ImageRequest.Builder(holder.itemView.getContext())
                .data(thumbnailUrl)
                .target(holder.ivThumbnail)
                .crossfade(true)
                .build();
        Coil.imageLoader(holder.itemView.getContext()).enqueue(request);
    }

    @Override
    public int getItemCount() {
        return videos.size();
    }

    public void updateData(List<YtVideoEntity> newVideos) {
        this.videos = newVideos;
        notifyDataSetChanged();
    }

    private String extractYouTubeVideoId(String url) {
        String pattern = "(?<=watch\\?v=|/videos/|embed\\/|youtu.be\\/|\\/v\\/|\\/e\\/|shorts\\/|watch\\?v%3D|watch\\?feature=player_embedded&v=|%2Fvideos%2F|embed%\\u200C\\u200B2F|youtu.be%2F|%2Fv%2F)[^#\\&\\?\\n]*";
        Pattern compiledPattern = Pattern.compile(pattern);
        Matcher matcher = compiledPattern.matcher(url);
        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }
}
