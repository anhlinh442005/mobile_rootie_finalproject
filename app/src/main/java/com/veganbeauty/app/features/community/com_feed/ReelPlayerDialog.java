package com.veganbeauty.app.features.community.com_feed;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import coil.Coil;
import coil.decode.SvgDecoder;
import coil.request.ImageRequest;

import com.veganbeauty.app.R;
import com.veganbeauty.app.data.local.entities.ReelEntity;
import com.veganbeauty.app.databinding.ComDialogReelPlayerBinding;
import com.veganbeauty.app.databinding.ComItemReelPlayerBinding;

import java.security.MessageDigest;
import java.util.Arrays;
import java.util.List;

public class ReelPlayerDialog extends DialogFragment {

    private final List<ReelEntity> reels;
    private final int initialPosition;

    private ComDialogReelPlayerBinding _binding;

    private final List<String> videoUrls = Arrays.asList(
            "https://res.cloudinary.com/dpjkzxjl2/video/upload/v1779423179/tiktok_nwm_7231472377438276870_n5xk8h.mp4",
            "https://res.cloudinary.com/dpjkzxjl2/video/upload/v1779423182/tiktok_nwm_7559978021822713106_ojcm1u.mp4",
            "https://res.cloudinary.com/dpjkzxjl2/video/upload/v1779423181/tiktok_nwm_7538058081125633298_c9sifg.mp4",
            "https://res.cloudinary.com/dpjkzxjl2/video/upload/v1779423180/tiktok_nwm_7487926346300148998_l8eetu.mp4",
            "https://res.cloudinary.com/dpjkzxjl2/video/upload/v1779423178/tiktok_nwm_7641962033369337096_nkiv9h.mp4",
            "https://res.cloudinary.com/dpjkzxjl2/video/upload/v1779423178/tiktok_nwm_7478916826060164370_uficoo.mp4",
            "https://res.cloudinary.com/dpjkzxjl2/video/upload/v1779423177/tiktok_nwm_7603600256147737876_xwwweq.mp4",
            "https://res.cloudinary.com/dpjkzxjl2/video/upload/v1779423177/tiktok_nwm_7504330582579563784_rtlvxf.mp4"
    );

    private final Handler progressHandler = new Handler(Looper.getMainLooper());
    private ReelPlayerViewHolder currentPlayingHolder = null;

    public ReelPlayerDialog(List<ReelEntity> reels, int initialPosition) {
        this.reels = reels;
        this.initialPosition = initialPosition;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NORMAL, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        _binding = ComDialogReelPlayerBinding.inflate(inflater, container, false);
        return _binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        _binding.ivBack.setOnClickListener(v -> dismiss());

        ReelPagerAdapter adapter = new ReelPagerAdapter(reels);
        _binding.viewPagerReels.setAdapter(adapter);
        _binding.viewPagerReels.setCurrentItem(initialPosition, false);

        _binding.viewPagerReels.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                if (currentPlayingHolder != null) {
                    currentPlayingHolder.stopVideo();
                }

                RecyclerView rv = (RecyclerView) _binding.viewPagerReels.getChildAt(0);
                if (rv != null) {
                    RecyclerView.ViewHolder holder = rv.findViewHolderForAdapterPosition(position);
                    if (holder instanceof ReelPlayerViewHolder) {
                        ((ReelPlayerViewHolder) holder).playVideo();
                        currentPlayingHolder = (ReelPlayerViewHolder) holder;
                    }
                }
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.BLACK));
        }
    }

    class ReelPagerAdapter extends RecyclerView.Adapter<ReelPlayerViewHolder> {
        private final List<ReelEntity> items;

        public ReelPagerAdapter(List<ReelEntity> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public ReelPlayerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ComItemReelPlayerBinding itemBinding = ComItemReelPlayerBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new ReelPlayerViewHolder(itemBinding);
        }

        @Override
        public void onBindViewHolder(@NonNull ReelPlayerViewHolder holder, int position) {
            holder.bind(items.get(position));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        @Override
        public void onViewAttachedToWindow(@NonNull ReelPlayerViewHolder holder) {
            super.onViewAttachedToWindow(holder);
            if (holder.getBindingAdapterPosition() == _binding.viewPagerReels.getCurrentItem()) {
                holder.playVideo();
                currentPlayingHolder = holder;
            }
        }

        @Override
        public void onViewDetachedFromWindow(@NonNull ReelPlayerViewHolder holder) {
            super.onViewDetachedFromWindow(holder);
            holder.stopVideo();
            if (currentPlayingHolder == holder) {
                currentPlayingHolder = null;
            }
        }
    }

    class ReelPlayerViewHolder extends RecyclerView.ViewHolder {
        private final ComItemReelPlayerBinding itemBinding;
        private MediaPlayer mediaPlayer = null;
        private boolean isLiked = false;
        private boolean isFollowing = false;

        public ReelPlayerViewHolder(ComItemReelPlayerBinding itemBinding) {
            super(itemBinding.getRoot());
            this.itemBinding = itemBinding;
        }

        public void bind(ReelEntity reel) {
            itemBinding.tvAuthorName.setText("@" + reel.getAuthorUsername());
            itemBinding.tvCaption.setText(reel.getCaption());
            itemBinding.tvLikesCount.setText(formatCount(reel.getLikesCount()));
            itemBinding.tvCommentsCount.setText(formatCount(reel.getCommentsCount()));
            itemBinding.tvShareCount.setText(formatCount(reel.getShareCount()));

            if (reel.getAuthorAvatarUrl() != null && !reel.getAuthorAvatarUrl().isEmpty()) {
                ImageRequest request = new ImageRequest.Builder(itemView.getContext())
                        .data(reel.getAuthorAvatarUrl())
                        .decoderFactory(new SvgDecoder.Factory(false))
                        .crossfade(true)
                        .placeholder(android.R.color.darker_gray)
                        .error(R.drawable.logo)
                        .target(itemBinding.ivAuthorAvatarRight)
                        .build();
                Coil.imageLoader(itemView.getContext()).enqueue(request);
            } else {
                itemBinding.ivAuthorAvatarRight.setImageResource(android.R.color.darker_gray);
            }

            itemBinding.ivLike.setOnClickListener(v -> {
                isLiked = !isLiked;
                if (isLiked) {
                    itemBinding.ivLike.setImageResource(R.drawable.ic_heart_filled);
                    itemBinding.ivLike.setColorFilter(Color.parseColor("#E53935"));
                    itemBinding.tvLikesCount.setText(formatCount(reel.getLikesCount() + 1));
                } else {
                    itemBinding.ivLike.setImageResource(R.drawable.ic_heart_outline);
                    itemBinding.ivLike.setColorFilter(Color.WHITE);
                    itemBinding.tvLikesCount.setText(formatCount(reel.getLikesCount()));
                }
            });

            itemBinding.btnFollow.setOnClickListener(v -> {
                isFollowing = !isFollowing;
                if (isFollowing) {
                    itemBinding.btnFollow.setText("Đang theo dõi");
                    itemBinding.btnFollow.setBackgroundResource(R.drawable.com_bg_filter_normal);
                    itemBinding.btnFollow.setTextColor(Color.WHITE);
                } else {
                    itemBinding.btnFollow.setText("Theo dõi");
                    itemBinding.btnFollow.setBackgroundResource(R.drawable.com_bg_filter_selected);
                    itemBinding.btnFollow.setBackgroundColor(Color.parseColor("#E53935"));
                    itemBinding.btnFollow.setTextColor(Color.WHITE);
                }
            });

            itemBinding.viewClickZone.setOnClickListener(v -> {
                if (itemBinding.videoView.isPlaying()) {
                    itemBinding.videoView.pause();
                } else {
                    itemBinding.videoView.start();
                }
            });

            int h = intFromMd5(reel.getVideoId());
            String selectedVideoUrl = videoUrls.get(h % videoUrls.size());
            itemBinding.videoView.setZOrderMediaOverlay(true);
            itemBinding.videoView.setVideoPath(selectedVideoUrl);

            itemBinding.videoView.setOnPreparedListener(mp -> {
                mediaPlayer = mp;
                itemBinding.pbLoading.setVisibility(View.GONE);
                mp.setLooping(true);
                if (getBindingAdapterPosition() == _binding.viewPagerReels.getCurrentItem()) {
                    itemBinding.videoView.start();
                }
            });

            itemBinding.videoView.setOnErrorListener((mp, what, extra) -> {
                itemBinding.pbLoading.setVisibility(View.GONE);
                return true;
            });
        }

        public void playVideo() {
            if (mediaPlayer != null) {
                itemBinding.videoView.start();
            } else {
                itemBinding.pbLoading.setVisibility(View.VISIBLE);
            }
        }

        public void stopVideo() {
            try {
                if (itemBinding.videoView.isPlaying()) {
                    itemBinding.videoView.pause();
                }
                itemBinding.videoView.seekTo(0);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private String formatCount(int count) {
        if (count >= 1_000_000) {
            return String.format(java.util.Locale.US, "%.1fM", count / 1_000_000.0);
        } else if (count >= 1_000) {
            return String.format(java.util.Locale.US, "%.1fK", count / 1_000.0);
        } else {
            return String.valueOf(count);
        }
    }

    private int intFromMd5(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(s.getBytes());
            int value = 0;
            for (int i = 0; i < 4; i++) {
                value = (value << 8) | (digest[i] & 0xFF);
            }
            return Math.abs(value);
        } catch (Exception e) {
            return Math.abs(s.hashCode());
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        progressHandler.removeCallbacksAndMessages(null);
        if (currentPlayingHolder != null) {
            currentPlayingHolder.stopVideo();
        }
        _binding = null;
    }
}
