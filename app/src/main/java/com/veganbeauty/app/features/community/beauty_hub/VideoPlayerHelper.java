package com.veganbeauty.app.features.community.beauty_hub;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.veganbeauty.app.R;
import com.veganbeauty.app.data.local.LocalJsonReader;
import com.veganbeauty.app.data.local.UserMemoryManager;
import com.veganbeauty.app.data.local.entities.YtVideoEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VideoPlayerHelper {

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static void showVideoPlayerDialog(Context context, YtVideoEntity video, Runnable onSavedCallback) {
        Dialog dialog = new Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.com_dialog_video_player);

        WebView webView = dialog.findViewById(R.id.webViewPlayer);
        ImageView ivClose = dialog.findViewById(R.id.ivClosePlayer);
        TextView tvDescription = dialog.findViewById(R.id.tvVideoDescription);
        RecyclerView rvRelated = dialog.findViewById(R.id.rvRelatedVideos);

        if (ivClose != null) ivClose.setOnClickListener(v -> dialog.dismiss());

        MaterialButton btnSave = dialog.findViewById(R.id.btnSaveFavorite);
        MaterialButton btnYoutube = dialog.findViewById(R.id.btnOpenYoutube);

        if (btnSave != null) {
            btnSave.setOnClickListener(v -> {
                UserMemoryManager memoryManager = new UserMemoryManager(context);
                showSaveVideoDialog(context, video, memoryManager, onSavedCallback);
            });
        }

        if (btnYoutube != null) {
            btnYoutube.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(video.getUrl()));
                context.startActivity(intent);
            });
        }

        TextView tvVideoTitle = dialog.findViewById(R.id.tvVideoTitle);
        TextView tvHashtags = dialog.findViewById(R.id.tvHashtags);
        TextView tvCategory = dialog.findViewById(R.id.tvCategory);
        TextView tvSource = dialog.findViewById(R.id.tvSource);
        TextView tvKeywords = dialog.findViewById(R.id.tvKeywords);

        if (tvVideoTitle != null) tvVideoTitle.setText(video.getTitle());
        
        if (tvHashtags != null) {
            tvHashtags.setText(video.getHashtags() != null && !video.getHashtags().trim().isEmpty() ? video.getHashtags() : "");
            tvHashtags.setVisibility(video.getHashtags() != null && !video.getHashtags().trim().isEmpty() ? View.VISIBLE : View.GONE);
        }

        if (tvCategory != null) tvCategory.setText(video.getType() != null && !video.getType().trim().isEmpty() ? video.getType().toUpperCase() : "CHUNG");
        if (tvSource != null) tvSource.setText(video.getUsername() != null && !video.getUsername().trim().isEmpty() ? video.getUsername() : "Cộng đồng Rootie");
        if (tvKeywords != null) tvKeywords.setText(video.getKeywords() != null && !video.getKeywords().trim().isEmpty() ? video.getKeywords() : "#rootie #lamdep #skincare");

        if (tvDescription != null) tvDescription.setText(video.getDescription() != null && !video.getDescription().trim().isEmpty() ? video.getDescription() : "Không có mô tả cho video này.");

        if (webView != null) {
            webView.getSettings().setJavaScriptEnabled(true);
            webView.getSettings().setDomStorageEnabled(true);
            webView.getSettings().setMediaPlaybackRequiresUserGesture(false);
            webView.getSettings().setUserAgentString("Mozilla/5.0 (Linux; Android 13; SM-G991U) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Mobile Safari/537.36");
            webView.getSettings().setUseWideViewPort(true);
            webView.getSettings().setLoadWithOverviewMode(true);
            webView.setWebChromeClient(new WebChromeClient());

            String videoId = extractYouTubeVideoId(video.getUrl());
            if (videoId != null) {
                webView.setWebViewClient(new WebViewClient() {
                    @Override
                    public void onPageFinished(WebView view, String url) {
                        String metaJs = "javascript:(function() { " +
                                "var meta = document.createElement('meta'); " +
                                "meta.name = 'viewport'; " +
                                "meta.content = 'width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no'; " +
                                "document.getElementsByTagName('head')[0].appendChild(meta); " +
                                "})();";
                        view.evaluateJavascript(metaJs, null);

                        String js = "javascript:(function() { " +
                                "setInterval(function() { " +
                                "  var elems = document.querySelectorAll('ytm-header-bar, .mobile-topbar-header, ytm-mobile-topbar-renderer, ytm-item-section-renderer, ytm-engagement-panel, ytm-comment-section-renderer, ytm-pivot-bar-renderer, ytm-promoted-sparkles-web-renderer, ytm-companion-ad-renderer, ytm-slim-video-metadata-section-renderer, .player-placeholder'); " +
                                "  for (var i=0; i<elems.length; i++) { elems[i].style.setProperty('display', 'none', 'important'); } " +
                                "  if(document.body) { " +
                                "    document.body.style.setProperty('background-color', '#000', 'important'); " +
                                "    document.body.style.setProperty('overflow', 'hidden', 'important'); " +
                                "    document.body.style.setProperty('margin', '0', 'important'); " +
                                "    document.body.style.setProperty('padding', '0', 'important'); " +
                                "  } " +
                                "  var players = document.querySelectorAll('.player-size, ytm-custom-control-renderer'); " +
                                "  for (var i=0; i<players.length; i++) { " +
                                "    players[i].style.setProperty('width', '100vw', 'important'); " +
                                "    players[i].style.setProperty('height', '100%', 'important'); " +
                                "  } " +
                                "}, 500); " +
                                "})();";
                        view.evaluateJavascript(js, null);
                    }
                });
                webView.loadUrl("https://m.youtube.com/watch?v=" + videoId);
            } else {
                String html = "<!DOCTYPE html>\n" +
                        "<html>\n" +
                        "<body style=\"margin:0;padding:0;background:#000;display:flex;align-items:center;justify-content:center;\">\n" +
                        "<video width=\"100%\" height=\"100%\" controls autoplay playsinline>\n" +
                        "    <source src=\"" + video.getUrl() + "\" type=\"video/mp4\">\n" +
                        "    Your browser does not support the video tag.\n" +
                        "</video>\n" +
                        "</body>\n" +
                        "</html>";
                webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
            }
        }

        Context appContext = context.getApplicationContext();
        executor.execute(() -> {
            List<YtVideoEntity> notebookList = new ArrayList<>();
            for (YtVideoEntity v : new LocalJsonReader(appContext).getNotebookVideos()) {
                if (!v.getId().equals(video.getId())) {
                    notebookList.add(v);
                }
            }
            java.util.Collections.shuffle(notebookList);
            List<YtVideoEntity> related = notebookList.subList(0, Math.min(4, notebookList.size()));
            mainHandler.post(() -> {
                if (rvRelated != null) {
                    rvRelated.setLayoutManager(new LinearLayoutManager(context));
                    rvRelated.setAdapter(new RelatedVideoAdapter(related, clickedVideo -> {
                        dialog.dismiss();
                        showVideoPlayerDialog(context, clickedVideo, onSavedCallback);
                    }));
                }
            });
        });

        dialog.show();
    }

    public static void showSaveVideoDialog(Context context, YtVideoEntity video, UserMemoryManager memoryManager, Runnable onSavedCallback) {
        Dialog dialog = new Dialog(context);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        dialog.setContentView(R.layout.com_dialog_save_handbook);

        ImageView ivCloseSaveDialog = dialog.findViewById(R.id.ivCloseSaveDialog);
        if (ivCloseSaveDialog != null) ivCloseSaveDialog.setOnClickListener(v -> dialog.dismiss());

        RecyclerView rvSaveCategories = dialog.findViewById(R.id.rvSaveCategories);
        LinearLayout llCreateNew = dialog.findViewById(R.id.llCreateNew);
        LinearLayout llNewCategoryInput = dialog.findViewById(R.id.llNewCategoryInput);
        EditText etNewCategory = dialog.findViewById(R.id.etNewCategory);
        Button btnSaveNew = dialog.findViewById(R.id.btnSaveNew);

        List<UserMemoryManager.HandbookCategory> categories = memoryManager.getCategories();

        if (rvSaveCategories != null) {
            rvSaveCategories.setLayoutManager(new LinearLayoutManager(context));
            rvSaveCategories.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                @NonNull
                @Override
                public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                    LinearLayout ll = new LinearLayout(parent.getContext());
                    ll.setOrientation(LinearLayout.HORIZONTAL);
                    ll.setGravity(Gravity.CENTER_VERTICAL);
                    ll.setPadding(0, 24, 0, 24);
                    ll.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

                    RadioButton rb = new RadioButton(parent.getContext());
                    rb.setButtonTintList(ColorStateList.valueOf(Color.parseColor("#677559")));
                    rb.setClickable(false);
                    rb.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));

                    TextView tv = new TextView(parent.getContext());
                    tv.setTextSize(16f);
                    tv.setTextColor(Color.parseColor("#3E4D44"));
                    tv.setPadding(24, 0, 0, 0);
                    tv.setTypeface(ResourcesCompat.getFont(parent.getContext(), R.font.be_vietnam_pro));
                    tv.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

                    ll.addView(rb);
                    ll.addView(tv);
                    return new RecyclerView.ViewHolder(ll) {};
                }

                @Override
                public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                    String catName = categories.get(position).getName();
                    LinearLayout ll = (LinearLayout) holder.itemView;
                    RadioButton rb = (RadioButton) ll.getChildAt(0);
                    TextView tv = (TextView) ll.getChildAt(1);

                    tv.setText(catName);
                    rb.setChecked(false);

                    holder.itemView.setOnClickListener(v -> {
                        rb.setChecked(true);
                        showCustomConfirmDialog(context, "Xác nhận", "Lưu video này vào sổ '" + catName + "'?", "Lưu", "Huỷ",
                                () -> {
                                    memoryManager.addVideoToCategory(catName, video);
                                    Toast.makeText(context, "Đã lưu vào " + catName, Toast.LENGTH_SHORT).show();
                                    dialog.dismiss();
                                    if (onSavedCallback != null) onSavedCallback.run();
                                },
                                () -> rb.setChecked(false)
                        );
                    });
                }

                @Override
                public int getItemCount() {
                    return categories.size();
                }
            });
        }

        if (llCreateNew != null && llNewCategoryInput != null) {
            llCreateNew.setOnClickListener(v -> {
                llCreateNew.setVisibility(View.GONE);
                llNewCategoryInput.setVisibility(View.VISIBLE);
            });
        }

        if (btnSaveNew != null && etNewCategory != null) {
            btnSaveNew.setOnClickListener(v -> {
                String newCat = etNewCategory.getText().toString().trim();
                if (!newCat.isEmpty()) {
                    memoryManager.addVideoToCategory(newCat, video);
                    Toast.makeText(context, "Đã lưu vào " + newCat, Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    if (onSavedCallback != null) onSavedCallback.run();
                }
            });
        }

        Button btnGoToMyHandbook = dialog.findViewById(R.id.btnGoToMyHandbook);
        if (btnGoToMyHandbook != null) {
            btnGoToMyHandbook.setOnClickListener(v -> {
                dialog.dismiss();
                if (context instanceof androidx.fragment.app.FragmentActivity) {
                    ((androidx.fragment.app.FragmentActivity) context).getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.main_container, new HandbookFragment())
                            .addToBackStack(null)
                            .commit();
                }
            });
        }

        dialog.show();
    }

    public static void showCustomConfirmDialog(Context context, String title, String message, String positiveText, String negativeText, Runnable onPositive, Runnable onNegative) {
        Dialog confirmDialog = new Dialog(context);
        if (confirmDialog.getWindow() != null) {
            confirmDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            confirmDialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        confirmDialog.setContentView(R.layout.com_dialog_confirm);

        TextView tvTitle = confirmDialog.findViewById(R.id.tvConfirmTitle);
        if (tvTitle != null) tvTitle.setText(title);

        TextView tvMessage = confirmDialog.findViewById(R.id.tvConfirmMessage);
        if (tvMessage != null) tvMessage.setText(message);

        Button btnPositive = confirmDialog.findViewById(R.id.btnConfirmPositive);
        if (btnPositive != null) {
            btnPositive.setText(positiveText);
            btnPositive.setOnClickListener(v -> {
                confirmDialog.dismiss();
                if (onPositive != null) onPositive.run();
            });
        }

        Button btnNegative = confirmDialog.findViewById(R.id.btnConfirmNegative);
        if (btnNegative != null) {
            btnNegative.setText(negativeText);
            btnNegative.setOnClickListener(v -> {
                confirmDialog.dismiss();
                if (onNegative != null) onNegative.run();
            });
        }
        
        confirmDialog.show();
    }

    private static String extractYouTubeVideoId(String url) {
        String pattern = "(?<=watch\\?v=|/videos/|embed\\/|youtu.be\\/|\\/v\\/|\\/e\\/|shorts\\/|watch\\?v%3D|watch\\?feature=player_embedded&v=|%2Fvideos%2F|embed%\\u200C\\u200B2F|youtu.be%2F|%2Fv%2F)[^#\\&\\?\\n]*";
        Pattern compiledPattern = Pattern.compile(pattern);
        Matcher matcher = compiledPattern.matcher(url);
        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }
}
