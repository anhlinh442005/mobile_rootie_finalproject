package com.veganbeauty.app.features.myskin;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.lifecycle.LifecycleOwnerKt;

import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.veganbeauty.app.R;
import com.veganbeauty.app.core.base.RootieFragment;
import com.veganbeauty.app.data.local.ProfileSession;
import com.veganbeauty.app.data.remote.FirestoreService;
import com.veganbeauty.app.databinding.SkinFragmentHistoryBinding;
import com.veganbeauty.app.features.home.BottomNavHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import kotlinx.coroutines.BuildersKt;
import kotlinx.coroutines.Dispatchers;

import java.util.ArrayList;
import java.util.List;

public class SkinHistoryFragment extends RootieFragment {

    private SkinFragmentHistoryBinding _binding;
    private JSONArray allHistory;
    private JSONArray currentHistory;
    private SkinHistoryAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        _binding = SkinFragmentHistoryBinding.inflate(inflater, container, false);
        return _binding.getRoot();
    }

    @Override
    protected void setupUI(@NonNull View view) {
        if (!ProfileSession.isLoggedIn(requireContext())) {
            BottomNavHelper.showLoginRequiredDialog(requireContext());
            getParentFragmentManager().popBackStack();
            return;
        }

        allHistory = new JSONArray();
        currentHistory = new JSONArray();

        setupRecyclerView();
        setupListeners();
        setupFilters();

        loadDataFromFirestore();
    }

    private void loadDataFromFirestore() {
        new Thread(() -> {
            try {
                String currentUserId = ProfileSession.getUserId(requireContext());
                FirestoreService firestoreService = new FirestoreService();
                allHistory = firestoreService.getSkinHistory(currentUserId);
                currentHistory = allHistory;

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        updateChartAndList(currentHistory);
                        String selectedFilter = null;
                        if (_binding.skinHistoryFilterAi.getCurrentTextColor() == ContextCompat.getColor(requireContext(), R.color.white)) {
                            selectedFilter = "Quét AI";
                        } else if (_binding.skinHistoryFilterOffline.getCurrentTextColor() == ContextCompat.getColor(requireContext(), R.color.white)) {
                            selectedFilter = "Soi da offline";
                        }
                        if (selectedFilter != null) {
                            filterData(selectedFilter);
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void filterData(String type) {
        if (type == null) {
            currentHistory = allHistory;
        } else {
            JSONArray filtered = new JSONArray();
            for (int i = 0; i < allHistory.length(); i++) {
                try {
                    JSONObject item = allHistory.getJSONObject(i);
                    if (type.equals(item.optString("scanType"))) {
                        filtered.put(item);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            currentHistory = filtered;
        }
        updateChartAndList(currentHistory);
    }

    private void setupRecyclerView() {
        adapter = new SkinHistoryAdapter(currentHistory, item -> {
            SkinScanResultDialogFragment dialog = SkinScanResultDialogFragment.newInstance(item.toString());
            dialog.show(getParentFragmentManager(), "SkinScanResultDialog");
        });
        _binding.skinHistoryRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        _binding.skinHistoryRecyclerView.setAdapter(adapter);
    }

    private void setupListeners() {
        _binding.skinHistoryBtnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        _binding.skinHistoryBtnRoutine.setOnClickListener(v -> Toast.makeText(requireContext(), "Gợi ý Routine clicked", Toast.LENGTH_SHORT).show());
    }

    private void setupFilters() {
        List<TextView> filters = new ArrayList<>();
        filters.add(_binding.skinHistoryFilterAll);
        filters.add(_binding.skinHistoryFilterAi);
        filters.add(_binding.skinHistoryFilterOffline);

        View.OnClickListener listener = v -> {
            TextView selected = (TextView) v;
            String type = null;
            if (selected == _binding.skinHistoryFilterAi) {
                type = "Quét AI";
            } else if (selected == _binding.skinHistoryFilterOffline) {
                type = "Soi da offline";
            }

            for (TextView f : filters) {
                if (f == selected) {
                    f.setBackgroundResource(R.drawable.skin_bg_btn_solid_dark);
                    f.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
                } else {
                    f.setBackgroundResource(R.drawable.skin_bg_card_outline);
                    f.setTextColor(ContextCompat.getColor(requireContext(), R.color.content));
                }
            }
            filterData(type);
        };

        _binding.skinHistoryFilterAll.setOnClickListener(listener);
        _binding.skinHistoryFilterAi.setOnClickListener(listener);
        _binding.skinHistoryFilterOffline.setOnClickListener(listener);
    }

    private List<JSONObject> sortJsonArray(JSONArray array, boolean descending) {
        List<JSONObject> list = new ArrayList<>();
        if (array != null) {
            for (int i = 0; i < array.length(); i++) {
                try {
                    list.add(array.getJSONObject(i));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.US);
        list.sort((o1, o2) -> {
            try {
                String d1 = o1.optString("date", "") + " " + o1.optString("time", "00:00");
                String d2 = o2.optString("date", "") + " " + o2.optString("time", "00:00");
                java.util.Date date1 = null;
                java.util.Date date2 = null;
                try {
                    date1 = sdf.parse(d1);
                } catch (Exception e) {
                    try {
                        date1 = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.US).parse(o1.optString("date", ""));
                    } catch (Exception ex) {}
                }
                try {
                    date2 = sdf.parse(d2);
                } catch (Exception e) {
                    try {
                        date2 = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.US).parse(o2.optString("date", ""));
                    } catch (Exception ex) {}
                }
                if (date1 != null && date2 != null) {
                    return descending ? date2.compareTo(date1) : date1.compareTo(date2);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return 0;
        });
        return list;
    }

    private void updateChartAndList(JSONArray data) {
        List<JSONObject> listSorted = sortJsonArray(data, true);
        JSONArray listData = new JSONArray();
        for (JSONObject item : listSorted) {
            listData.put(item);
        }
        adapter.updateData(listData);

        ArrayList<Entry> entries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();

        List<JSONObject> chartSorted = sortJsonArray(data, false);
        int len = chartSorted.size();
        for (int i = 0; i < len; i++) {
            try {
                JSONObject item = chartSorted.get(i);
                float score = (float) item.optInt("score", 0);
                String dateStr = item.optString("date", "");

                String shortDate = dateStr.length() >= 5 ? dateStr.substring(0, 5) : dateStr;

                entries.add(new Entry((float) i, score));
                labels.add(shortDate);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        LineDataSet dataSet = new LineDataSet(entries, "Điểm số da");
        dataSet.setColor(Color.parseColor("#4B5541"));
        dataSet.setCircleColor(Color.parseColor("#4B5541"));
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(5f);
        dataSet.setDrawCircleHole(false);
        dataSet.setValueTextSize(13f);
        dataSet.setValueTextColor(Color.parseColor("#333333"));
        dataSet.setValueTypeface(ResourcesCompat.getFont(requireContext(), R.font.be_vietnam_pro_bold));
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.parseColor("#E6EBE6"));
        dataSet.setFillAlpha(50);
        dataSet.setMode(LineDataSet.Mode.LINEAR);

        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getPointLabel(Entry entry) {
                return entry != null ? String.valueOf((int) entry.getY()) : "";
            }
        });

        LineData lineData = new LineData(dataSet);

        _binding.skinHistoryLineChart.setData(lineData);
        _binding.skinHistoryLineChart.getDescription().setEnabled(false);
        _binding.skinHistoryLineChart.getLegend().setEnabled(false);
        _binding.skinHistoryLineChart.setTouchEnabled(false);

        XAxis xAxis = _binding.skinHistoryLineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setDrawGridLines(true);
        xAxis.setGridColor(Color.parseColor("#EAEAEA"));
        xAxis.setTextColor(Color.parseColor("#555555"));
        xAxis.setTextSize(12f);
        xAxis.setTypeface(ResourcesCompat.getFont(requireContext(), R.font.be_vietnam_pro_bold));
        xAxis.setGranularity(1f);
        xAxis.setAxisLineColor(Color.TRANSPARENT);
        xAxis.setYOffset(10f);

        YAxis axisLeft = _binding.skinHistoryLineChart.getAxisLeft();
        axisLeft.setAxisMinimum(0f);
        axisLeft.setAxisMaximum(100f);
        axisLeft.setLabelCount(5, true);
        axisLeft.setTextColor(Color.parseColor("#555555"));
        axisLeft.setTextSize(12f);
        axisLeft.setTypeface(ResourcesCompat.getFont(requireContext(), R.font.be_vietnam_pro_bold));
        axisLeft.setDrawGridLines(true);
        axisLeft.setGridColor(Color.parseColor("#EAEAEA"));
        axisLeft.setAxisLineColor(Color.TRANSPARENT);
        axisLeft.setXOffset(12f);

        _binding.skinHistoryLineChart.getAxisRight().setEnabled(false);
        _binding.skinHistoryLineChart.setDrawBorders(false);

        _binding.skinHistoryLineChart.setExtraOffsets(0f, 20f, 16f, 12f);

        _binding.skinHistoryLineChart.animateX(500);
        _binding.skinHistoryLineChart.invalidate();
    }

    @Override
    protected void observeViewModel() {
        // Not used
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        _binding = null;
    }
}
