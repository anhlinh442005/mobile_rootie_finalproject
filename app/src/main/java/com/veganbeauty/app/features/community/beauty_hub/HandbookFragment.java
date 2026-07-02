package com.veganbeauty.app.features.community.beauty_hub;

import android.content.Context;
import android.app.Dialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
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
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.veganbeauty.app.R;
import com.veganbeauty.app.data.local.LocalJsonReader;
import com.veganbeauty.app.data.local.UserMemoryManager;
import com.veganbeauty.app.data.local.entities.YtVideoEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HandbookFragment extends Fragment {

    private HandbookVideoAdapter currentAdapter;
    private List<YtVideoEntity> notebookVideos = new ArrayList<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.com_fragment_handbook, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ImageView ivBack = view.findViewById(R.id.ivBack);
        if (ivBack != null) {
            ivBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        }

        RecyclerView rvVideos = view.findViewById(R.id.rvVideos);
        if (rvVideos != null) {
            rvVideos.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        }
        LinearLayout llFilters = view.findViewById(R.id.llFilters);
        EditText etSearch = view.findViewById(R.id.etSearch);
        LinearLayout llEmptyState = view.findViewById(R.id.llEmptyState);
        LinearLayout llHasDataState = view.findViewById(R.id.llHasDataState);
        TextView tvMyHandbookTitle = view.findViewById(R.id.tvMyHandbookTitle);

        String[] filters = {"Tất cả", "mụn ẩn", "trị thâm", "Sạch sâu", "Da dầu mụn"};
        int dp = (int) getResources().getDisplayMetrics().density;

        for (int i = 0; i < filters.length; i++) {
            TextView chip = new TextView(requireContext());
            chip.setText(filters[i]);
            chip.setTextSize(12f);
            chip.setPadding(16 * dp, 6 * dp, 16 * dp, 6 * dp);
            
            if (i == 0) {
                chip.setBackgroundResource(R.drawable.com_bg_chip_selected);
                chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
            } else {
                chip.setBackgroundResource(R.drawable.com_bg_chip_default);
                chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary));
            }

            LinearLayout.LayoutParams marginParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            marginParams.setMarginEnd(8 * dp);
            if (i == 0) marginParams.setMarginStart(16 * dp);
            if (i == filters.length - 1) marginParams.setMarginEnd(16 * dp);
            chip.setLayoutParams(marginParams);

            if (llFilters != null) llFilters.addView(chip);
        }

        UserMemoryManager memoryManager = new UserMemoryManager(requireContext());
        List<UserMemoryManager.HandbookCategory> categories = memoryManager.getCategories();
        
        boolean hasVideos = false;
        for (UserMemoryManager.HandbookCategory cat : categories) {
            if (!cat.getVideos().isEmpty()) {
                hasVideos = true;
                break;
            }
        }

        if (hasVideos) {
            if (llEmptyState != null) llEmptyState.setVisibility(View.GONE);
            if (llHasDataState != null) llHasDataState.setVisibility(View.VISIBLE);
            if (tvMyHandbookTitle != null) tvMyHandbookTitle.setVisibility(View.GONE);
        } else {
            if (llEmptyState != null) llEmptyState.setVisibility(View.VISIBLE);
            if (llHasDataState != null) llHasDataState.setVisibility(View.GONE);
            if (tvMyHandbookTitle != null) tvMyHandbookTitle.setVisibility(View.VISIBLE);
        }

        View btnViewMyHandbook = view.findViewById(R.id.btnViewMyHandbook);
        if (btnViewMyHandbook != null) {
            btnViewMyHandbook.setOnClickListener(v -> showMyHandbookDialog(memoryManager));
        }

        loadNotebookVideos(rvVideos, etSearch, memoryManager);
    }

    private void loadNotebookVideos(RecyclerView rvVideos, EditText etSearch, UserMemoryManager memoryManager) {
        Context appContext = requireContext().getApplicationContext();
        executor.execute(() -> {
            List<YtVideoEntity> loaded = new LocalJsonReader(appContext).getNotebookVideos();
            mainHandler.post(() -> {
                if (!isAdded()) return;
                notebookVideos = loaded;
                bindNotebookAdapter(rvVideos, etSearch, memoryManager, notebookVideos);
            });
        });
    }

    private void bindNotebookAdapter(RecyclerView rvVideos, EditText etSearch,
                                     UserMemoryManager memoryManager, List<YtVideoEntity> videos) {
        currentAdapter = new HandbookVideoAdapter(
                videos,
                false,
                video -> {
                    for (UserMemoryManager.HandbookCategory cat : memoryManager.getCategories()) {
                        for (YtVideoEntity cv : cat.getVideos()) {
                            if (cv.getUrl().equals(video.getUrl())) return true;
                        }
                    }
                    return false;
                },
                this::showVideoPlayerDialog,
                video -> showSaveVideoDialog(video, memoryManager),
                null
        );
        if (rvVideos != null) {
            rvVideos.setAdapter(currentAdapter);
        }

        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    String query = s != null ? s.toString().toLowerCase() : "";
                    List<YtVideoEntity> filtered = new ArrayList<>();
                    if (query.isEmpty()) {
                        filtered.addAll(notebookVideos);
                    } else {
                        for (YtVideoEntity v : notebookVideos) {
                            if ((v.getTitle() != null && v.getTitle().toLowerCase().contains(query))
                                    || (v.getDescription() != null && v.getDescription().toLowerCase().contains(query))) {
                                filtered.add(v);
                            }
                        }
                    }
                    bindNotebookAdapter(rvVideos, null, memoryManager, filtered);
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }

    private void showSaveVideoDialog(YtVideoEntity video, UserMemoryManager memoryManager) {
        Dialog dialog = new Dialog(requireContext());
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
            rvSaveCategories.setLayoutManager(new LinearLayoutManager(requireContext()));
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
                        showCustomConfirmDialog(
                                "Xác nhận",
                                "Lưu video này vào sổ '" + catName + "'?",
                                "Lưu",
                                "Huỷ",
                                () -> {
                                    memoryManager.addVideoToCategory(catName, video);
                                    Toast.makeText(requireContext(), "Đã lưu vào " + catName, Toast.LENGTH_SHORT).show();
                                    
                                    View root = getView();
                                    if (root != null) {
                                        View llEmpty = root.findViewById(R.id.llEmptyState);
                                        if (llEmpty != null) llEmpty.setVisibility(View.GONE);
                                        View llHas = root.findViewById(R.id.llHasDataState);
                                        if (llHas != null) llHas.setVisibility(View.VISIBLE);
                                        View tvTitle = root.findViewById(R.id.tvMyHandbookTitle);
                                        if (tvTitle != null) tvTitle.setVisibility(View.GONE);
                                        RecyclerView rv = root.findViewById(R.id.rvVideos);
                                        if (rv != null && rv.getAdapter() != null) rv.getAdapter().notifyDataSetChanged();
                                    }
                                    dialog.dismiss();
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
                    Toast.makeText(requireContext(), "Đã lưu vào " + newCat, Toast.LENGTH_SHORT).show();
                    
                    View root = getView();
                    if (root != null) {
                        View llEmpty = root.findViewById(R.id.llEmptyState);
                        if (llEmpty != null) llEmpty.setVisibility(View.GONE);
                        View llHas = root.findViewById(R.id.llHasDataState);
                        if (llHas != null) llHas.setVisibility(View.VISIBLE);
                        View tvTitle = root.findViewById(R.id.tvMyHandbookTitle);
                        if (tvTitle != null) tvTitle.setVisibility(View.GONE);
                        RecyclerView rv = root.findViewById(R.id.rvVideos);
                        if (rv != null && rv.getAdapter() != null) rv.getAdapter().notifyDataSetChanged();
                    }
                    dialog.dismiss();
                }
            });
        }

        dialog.show();
    }

    private void showMyHandbookDialog(UserMemoryManager memoryManager) {
        Dialog dialog = new Dialog(requireContext(), R.style.CustomDialogTheme);
        dialog.setContentView(R.layout.com_dialog_my_handbook);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            int screenWidth = getResources().getDisplayMetrics().widthPixels;
            int horizontalInset = (int) (10 * getResources().getDisplayMetrics().density);
            dialog.getWindow().setLayout(screenWidth - horizontalInset * 2, ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().getDecorView().setPadding(0, 0, 0, 0);
        }

        ImageView ivClose = dialog.findViewById(R.id.ivClose);
        if (ivClose != null) ivClose.setOnClickListener(v -> dialog.dismiss());

        RecyclerView rvCategories = dialog.findViewById(R.id.rvCategories);
        if (rvCategories != null) {
            rvCategories.setLayoutManager(new LinearLayoutManager(requireContext()));
            List<UserMemoryManager.HandbookCategory> categories = memoryManager.getCategories();
            rvCategories.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                @NonNull
                @Override
                public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                    View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.com_item_handbook_category, parent, false);
                    return new RecyclerView.ViewHolder(view) {};
                }

                @Override
                public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                    UserMemoryManager.HandbookCategory cat = categories.get(position);
                    TextView tvName = holder.itemView.findViewById(R.id.tvCategoryName);
                    if (tvName != null) tvName.setText(cat.getName());

                    RecyclerView rvCatVideos = holder.itemView.findViewById(R.id.rvCategoryVideos);
                    if (rvCatVideos != null) {
                        rvCatVideos.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
                        rvCatVideos.setAdapter(new HandbookVideoAdapter(
                                cat.getVideos(),
                                true,
                                video -> true,
                                HandbookFragment.this::showVideoPlayerDialog,
                                video -> {},
                                video -> {
                                    showCustomConfirmDialog(
                                            "Xoá video",
                                            "Xoá video này khỏi sổ '" + cat.getName() + "'?",
                                            "Xoá",
                                            "Huỷ",
                                            () -> {
                                                memoryManager.removeVideoFromCategory(cat.getName(), video);
                                                Toast.makeText(requireContext(), "Đã xoá", Toast.LENGTH_SHORT).show();
                                                
                                                int idx = -1;
                                                for (int i = 0; i < cat.getVideos().size(); i++) {
                                                    if (cat.getVideos().get(i).getUrl().equals(video.getUrl()) ||
                                                            cat.getVideos().get(i).getId().equals(video.getId())) {
                                                        idx = i;
                                                        break;
                                                    }
                                                }
                                                if (idx != -1) {
                                                    cat.getVideos().remove(idx);
                                                    if (rvCatVideos.getAdapter() != null) rvCatVideos.getAdapter().notifyItemRemoved(idx);
                                                }
                                                
                                                View root = getView();
                                                if (root != null) {
                                                    RecyclerView mainRv = root.findViewById(R.id.rvVideos);
                                                    if (mainRv != null && mainRv.getAdapter() != null) mainRv.getAdapter().notifyDataSetChanged();
                                                    
                                                    List<UserMemoryManager.HandbookCategory> currentCats = memoryManager.getCategories();
                                                    boolean isEmpty = true;
                                                    for (UserMemoryManager.HandbookCategory c : currentCats) {
                                                        if (!c.getVideos().isEmpty()) {
                                                            isEmpty = false;
                                                            break;
                                                        }
                                                    }
                                                    
                                                    if (isEmpty) {
                                                        View llEmpty = root.findViewById(R.id.llEmptyState);
                                                        if (llEmpty != null) llEmpty.setVisibility(View.VISIBLE);
                                                        View llHas = root.findViewById(R.id.llHasDataState);
                                                        if (llHas != null) llHas.setVisibility(View.GONE);
                                                        View tvTitle = root.findViewById(R.id.tvMyHandbookTitle);
                                                        if (tvTitle != null) tvTitle.setVisibility(View.VISIBLE);
                                                        dialog.dismiss();
                                                    }
                                                }
                                            },
                                            () -> {}
                                    );
                                }
                        ));
                    }
                }

                @Override
                public int getItemCount() {
                    return categories.size();
                }
            });
        }

        dialog.show();
    }

    private void showCustomConfirmDialog(String title, String message, String positiveText, String negativeText, Runnable onPositive, Runnable onNegative) {
        Dialog confirmDialog = new Dialog(requireContext());
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

    private void showVideoPlayerDialog(YtVideoEntity video) {
        Dialog dialog = new Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen);
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
                UserMemoryManager memoryManager = new UserMemoryManager(requireContext());
                showSaveVideoDialog(video, memoryManager);
            });
        }

        if (btnYoutube != null) {
            btnYoutube.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(video.getUrl()));
                startActivity(intent);
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

        Context appContext = requireContext().getApplicationContext();
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
                    rvRelated.setLayoutManager(new LinearLayoutManager(requireContext()));
                    rvRelated.setAdapter(new RelatedVideoAdapter(related, clickedVideo -> {
                        dialog.dismiss();
                        showVideoPlayerDialog(clickedVideo);
                    }));
                }
            });
        });

        dialog.show();
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
