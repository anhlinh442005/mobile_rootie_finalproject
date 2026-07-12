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
                chip.setBackgroundResource(R.drawable.bg_btn_buy);
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
        VideoPlayerHelper.showSaveVideoDialog(requireContext(), video, memoryManager, () -> {
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
        });
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
        VideoPlayerHelper.showCustomConfirmDialog(requireContext(), title, message, positiveText, negativeText, onPositive, onNegative);
    }

    private void showVideoPlayerDialog(YtVideoEntity video) {
        VideoPlayerHelper.showVideoPlayerDialog(requireContext(), video, () -> {
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
        });
    }
}
