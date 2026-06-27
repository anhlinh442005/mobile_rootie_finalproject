package com.veganbeauty.app.features.community.beauty_hub;

import android.content.Intent;
import android.net.Uri;
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
import java.util.Locale;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NotebookVideoAdapter extends RecyclerView.Adapter<NotebookVideoAdapter.NotebookViewHolder> {

    private List<YtVideoEntity> items;

    public NotebookVideoAdapter(List<YtVideoEntity> items) {
        this.items = items;
    }

    public static class NotebookViewHolder extends RecyclerView.ViewHolder {
        public final ImageView ivThumbnail;
        public final TextView tvTitle;
        public final TextView tvDuration;
        public final TextView tvLikes;

        public NotebookViewHolder(View view) {
            super(view);
            ivThumbnail = view.findViewById(R.id.ivThumbnail);
            tvTitle = view.findViewById(R.id.tvTitle);
            tvDuration = view.findViewById(R.id.tvDuration);
            tvLikes = view.findViewById(R.id.tvLikes);
        }
    }

    @NonNull
    @Override
    public NotebookViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.com_item_notebook_video, parent, false);
        return new NotebookViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotebookViewHolder holder, int position) {
        YtVideoEntity item = items.get(position);
        holder.tvTitle.setText(item.getTitle());
        
        String videoId = extractYouTubeVideoId(item.getUrl());
        String thumbnailUrl = item.getUrl();
        if (videoId != null) {
            thumbnailUrl = "https://img.youtube.com/vi/" + videoId + "/hqdefault.jpg";
        } else if (item.getUrl().contains("cloudinary.com") && item.getUrl().toLowerCase().endsWith(".mp4")) {
            int lastDotIndex = item.getUrl().lastIndexOf('.');
            if (lastDotIndex != -1) {
                thumbnailUrl = item.getUrl().substring(0, lastDotIndex) + ".jpg";
            }
        }
        
        ImageRequest request = new ImageRequest.Builder(holder.itemView.getContext())
                .data(thumbnailUrl)
                .target(holder.ivThumbnail)
                .crossfade(true)
                .build();
        Coil.imageLoader(holder.itemView.getContext()).enqueue(request);
            
        holder.tvDuration.setText("14:25");
        
        Random random = new Random();
        int randomLikes = (random.nextInt(901) + 100) * 10;
        holder.tvLikes.setText(String.format(Locale.US, "%.1fk", randomLikes / 1000.0));
        
        holder.itemView.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(item.getUrl()));
                holder.itemView.getContext().startActivity(intent);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void updateData(List<YtVideoEntity> newItems) {
        this.items = newItems;
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
