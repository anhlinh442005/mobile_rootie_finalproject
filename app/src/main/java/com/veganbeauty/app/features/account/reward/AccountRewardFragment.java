package com.veganbeauty.app.features.account.reward;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.veganbeauty.app.R;
import com.veganbeauty.app.core.base.RootieFragment;
import com.veganbeauty.app.data.local.LocalJsonReader;
import com.veganbeauty.app.data.local.RootieDatabase;
import com.veganbeauty.app.data.local.entities.RewardPointEntity;
import com.veganbeauty.app.data.local.entities.UserGiftEntity;
import com.veganbeauty.app.data.local.dao.RewardPointDao;
import com.veganbeauty.app.data.repository.OrderRepository;
import com.veganbeauty.app.databinding.AccountRewardFragmentBinding;
import com.veganbeauty.app.features.home.BottomNavHelper;

import org.json.JSONArray;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.lifecycle.FlowLiveDataConversions;

public class AccountRewardFragment extends RootieFragment {

    private AccountRewardFragmentBinding binding;
    private OrderRepository repository;
    private int currentPoints = 8500;
    private final List<UserGiftEntity> myGiftsList = new ArrayList<>();
    private final List<UserGiftEntity> filteredGiftsList = new ArrayList<>();
    private String activeFilter = "Tất cả";
    private String activeExchangeFilter = "Tất cả";
    private ExchangeGiftsAdapter exchangeAdapter;
    private MyGiftsAdapter giftsAdapter;
    private HistoryAdapter historyAdapter;
    private List<RedeemableGift> redeemableGifts = new ArrayList<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = AccountRewardFragmentBinding.inflate(inflater, container, false);
        setupRepository();
        return binding.getRoot();
    }

    private void setupRepository() {
        RootieDatabase db = RootieDatabase.getDatabase(requireContext());
        repository = new OrderRepository(db.orderDao(), db.rewardPointDao(), db.userGiftDao(), new LocalJsonReader(requireContext()));
    }

    @Override
    public void setupUI(View view) {
        redeemableGifts = loadGiftsFromAssets(requireContext());

        binding.btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        binding.btnNotification.setOnClickListener(v ->
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.main_container, new com.veganbeauty.app.features.account.notification.AccountNotificationFragment())
                        .addToBackStack(null).commit());
        binding.btnCheckIn.setOnClickListener(v ->
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.main_container, new com.veganbeauty.app.features.account.checkin.AccountCheckinFragment())
                        .addToBackStack(null).commit());

        binding.tvAlertWarning.setText(Html.fromHtml("Bạn có <b>500 điểm</b> sắp hết hạn vào 30/11.", Html.FROM_HTML_MODE_COMPACT));

        setupTabs();
        setupExchangeChips();
        setupFilterChips();

        exchangeAdapter = new ExchangeGiftsAdapter(gift -> {
            UserGiftEntity ownedItem = null;
            for (UserGiftEntity g : myGiftsList) { if (g.getGiftId().equals(gift.getGiftId())) { ownedItem = g; break; } }
            openGiftDetail(gift, ownedItem != null, ownedItem != null ? ownedItem.getId() : -1);
        }, this::performRedeemDirectly);

        binding.rvExchangeGifts.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvExchangeGifts.setAdapter(exchangeAdapter);
        applyExchangeFilter();

        giftsAdapter = new MyGiftsAdapter(userGift -> {
            RedeemableGift gift = new RedeemableGift(userGift.getGiftId(), userGift.getTitle(), userGift.getDescription(),
                    userGift.getCost(), userGift.getExpiryDate(), userGift.getCode(), userGift.getGiftType(),
                    "redeemed", userGift.getProductId(), userGift.getMinOrderValue(),
                    userGift.getApplicableProducts(), userGift.getOfferType(), userGift.getDiscountValue(), "Đồng");
            openGiftDetail(gift, true, userGift.getId());
        }, userGift -> {
            copyToClipboard(requireContext(), userGift.getCode());
            executor.execute(() -> {
                try {
                    boolean success = repository.deleteUserGiftById(userGift.getId());
                    if (isAdded()) requireActivity().runOnUiThread(() -> {
                        if (success) Toast.makeText(getContext(), "Mã quà tặng " + userGift.getCode() + " đã được áp dụng thành công!", Toast.LENGTH_SHORT).show();
                        else Toast.makeText(getContext(), "Sử dụng quà tặng thất bại!", Toast.LENGTH_SHORT).show();
                        BottomNavHelper.navigate(this, R.id.nav_shop);
                    });
                } catch (Exception e) { e.printStackTrace(); }
            });
        });
        binding.rvMyGifts.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvMyGifts.setAdapter(giftsAdapter);

        historyAdapter = new HistoryAdapter();
        binding.rvHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvHistory.setAdapter(historyAdapter);
    }

    @Override
    public void observeViewModel() {
        RootieDatabase db = RootieDatabase.getDatabase(requireContext());

        FlowLiveDataConversions.asLiveData(db.rewardPointDao().getTotalPointsFlow())
                .observe(getViewLifecycleOwner(), ptsList -> {
                    if (binding == null || !isAdded()) return;
                    int pts = (ptsList != null && !ptsList.isEmpty()) ? ptsList.get(0).total : 0;
                    currentPoints = pts;
                    binding.tvCurrentPoints.setText(String.format("%,d", pts).replace(',', '.'));

                    String rank, progressText;
                    int pct;
                    if (pts >= 20000) { rank = "Hạng VIP"; progressText = "Đạt hạng cao nhất"; pct = 100; }
                    else if (pts >= 10000) { rank = "Hạng Vàng"; progressText = "Còn " + String.format("%,d", 20000 - pts).replace(',', '.') + " xu để đến VIP"; pct = (pts - 10000) * 100 / 10000; }
                    else if (pts >= 5000) { rank = "Hạng Bạc"; progressText = "Còn " + String.format("%,d", 10000 - pts).replace(',', '.') + " xu đến Vàng"; pct = (pts - 5000) * 100 / 5000; }
                    else { rank = "Hạng Thường"; progressText = "Còn " + String.format("%,d", 5000 - pts).replace(',', '.') + " xu để đến Bạc"; pct = pts * 100 / 5000; }

                    binding.tvCurrentRank.setText(rank);
                    binding.tvNextRankHint.setText(progressText);
                    binding.pbRankProgress.setProgress(pct);
                    applyExchangeFilter();
                });

        FlowLiveDataConversions.asLiveData(repository.getAllUserGifts())
                .observe(getViewLifecycleOwner(), gifts -> {
                    if (binding == null || !isAdded()) return;
                    myGiftsList.clear();
                    if (gifts != null) myGiftsList.addAll(gifts);
                    List<String> ownedIds = new ArrayList<>();
                    for (UserGiftEntity g : myGiftsList) ownedIds.add(g.getGiftId());
                    for (RedeemableGift g : redeemableGifts) g.setStatus(ownedIds.contains(g.getGiftId()) ? "redeemed" : "unredeemed");
                    applyGiftsFilter();
                    applyExchangeFilter();
                });

        FlowLiveDataConversions.asLiveData(db.rewardPointDao().getAllRewardHistory())
                .observe(getViewLifecycleOwner(), history -> {
                    if (binding == null || !isAdded()) return;
                    List<HistoryItem> histItems = new ArrayList<>();
                    if (history == null) {
                        historyAdapter.submitList(histItems);
                        return;
                    }
                    SimpleDateFormat sdfGroup = new SimpleDateFormat("'Tháng' MM, yyyy", Locale.getDefault());
                    Map<String, List<RewardPointEntity>> grouped = new LinkedHashMap<>();
                    for (RewardPointEntity item : history) {
                        if ("SYSTEM".equals(item.getOrderId())) continue;
                        String key = sdfGroup.format(new Date(item.getTimestamp()));
                        if (!grouped.containsKey(key)) grouped.put(key, new ArrayList<>());
                        grouped.get(key).add(item);
                    }
                    for (Map.Entry<String, List<RewardPointEntity>> e : grouped.entrySet()) {
                        histItems.add(new HistoryItem.Header(e.getKey()));
                        for (RewardPointEntity item : e.getValue()) histItems.add(new HistoryItem.Transaction(item));
                    }
                    historyAdapter.submitList(histItems);
                });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (AccountRewardDetailFragment.selectRewardTabOnResume == 1) {
            binding.btnTabMyGifts.performClick();
            AccountRewardDetailFragment.selectRewardTabOnResume = 0;
        }
    }

    private void setupTabs() {
        View[] tabs = {binding.btnTabExchange, binding.btnTabMyGifts, binding.btnTabHistory};
        TextView[] tvs = {binding.tabExchange, binding.tabMyGifts, binding.tabHistory};
        View[] inds = {binding.indicatorExchange, binding.indicatorMyGifts, binding.indicatorHistory};
        View[] layouts = {binding.layoutExchangeTab, binding.layoutMyGiftsTab, binding.layoutHistoryTab};
        for (int i = 0; i < tabs.length; i++) {
            final int idx = i;
            tabs[i].setOnClickListener(v -> {
                for (int j = 0; j < tabs.length; j++) {
                    boolean active = j == idx;
                    tvs[j].setTextColor(ContextCompat.getColor(requireContext(), active ? R.color.primary : R.color.gray_dark));
                    tvs[j].setTypeface(null, active ? Typeface.BOLD : Typeface.NORMAL);
                    inds[j].setVisibility(active ? View.VISIBLE : View.INVISIBLE);
                    layouts[j].setVisibility(active ? View.VISIBLE : View.GONE);
                }
            });
        }
    }

    private void setupExchangeChips() {
        TextView[] chips = {binding.chipAllExchange, binding.chipVoucherExchange, binding.chipProductExchange, binding.chipGiftExchange};
        String[] names = {"Tất cả","Voucher giảm giá","Sản phẩm","Quà tặng"};
        for (int i = 0; i < chips.length; i++) {
            final int idx = i;
            chips[i].setOnClickListener(v -> {
                activeExchangeFilter = names[idx];
                for (int j = 0; j < chips.length; j++) {
                    chips[j].setBackgroundResource(j == idx ? R.drawable.tab_active_bg : R.drawable.tab_inactive_bg);
                    chips[j].setTextColor(ContextCompat.getColor(requireContext(), j == idx ? R.color.white : R.color.primary));
                }
                applyExchangeFilter();
            });
        }
    }

    private void setupFilterChips() {
        TextView[] chips = {binding.chipAll, binding.chipValid, binding.chipExpired};
        String[] names = {"Tất cả","Còn hạn","Hết hạn"};
        for (int i = 0; i < chips.length; i++) {
            final int idx = i;
            chips[i].setOnClickListener(v -> {
                activeFilter = names[idx];
                for (int j = 0; j < chips.length; j++) {
                    chips[j].setBackgroundResource(j == idx ? R.drawable.tab_active_bg : R.drawable.tab_inactive_bg);
                    chips[j].setTextColor(ContextCompat.getColor(requireContext(), j == idx ? R.color.white : R.color.primary));
                }
                applyGiftsFilter();
            });
        }
    }

    private void applyExchangeFilter() {
        List<RedeemableGift> filtered = new ArrayList<>();
        for (RedeemableGift g : redeemableGifts) {
            boolean include = false;
            switch (activeExchangeFilter) {
                case "Voucher giảm giá": include = g.getGiftType().equals("voucher_discount") || g.getGiftType().equals("voucher_freeship"); break;
                case "Sản phẩm": include = g.getGiftType().equals("product"); break;
                case "Quà tặng": include = g.getGiftType().equals("gift"); break;
                default: include = true; break;
            }
            if (include) filtered.add(g);
        }
        if (exchangeAdapter != null) exchangeAdapter.submitList(filtered, currentPoints);
    }

    private void applyGiftsFilter() {
        filteredGiftsList.clear();
        for (UserGiftEntity g : myGiftsList) {
            String status = computeStatusFromExpiry(g.getExpiryDate());
            boolean include = false;
            switch (activeFilter) {
                case "Còn hạn": include = status.equals("valid") || status.equals("expiring"); break;
                case "Hết hạn": include = status.equals("expired"); break;
                default: include = true; break;
            }
            if (include) filteredGiftsList.add(g);
        }
        if (giftsAdapter != null) giftsAdapter.submitList(filteredGiftsList);
    }

    private String computeStatusFromExpiry(String expiryStr) {
        try {
            Date expiryDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(expiryStr);
            if (expiryDate == null) return "valid";
            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0); today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0); today.set(Calendar.MILLISECOND, 0);
            Calendar expiry = Calendar.getInstance(); expiry.setTime(expiryDate);
            expiry.set(Calendar.HOUR_OF_DAY, 0); expiry.set(Calendar.MINUTE, 0);
            expiry.set(Calendar.SECOND, 0); expiry.set(Calendar.MILLISECOND, 0);
            if (expiry.before(today)) return "expired";
            if (expiry.equals(today)) return "expiring";
            return "valid";
        } catch (Exception e) { return "valid"; }
    }

    private List<RedeemableGift> loadGiftsFromAssets(Context ctx) {
        List<RedeemableGift> list = new ArrayList<>();
        try {
            java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(ctx.getAssets().open("gifts.json")));
            StringBuilder sb = new StringBuilder(); String line;
            while ((line = br.readLine()) != null) sb.append(line);
            JSONArray arr = new JSONArray(sb.toString());
            for (int i = 0; i < arr.length(); i++) {
                org.json.JSONObject o = arr.getJSONObject(i);
                list.add(new RedeemableGift(o.optString("giftId"), o.optString("title"), o.optString("description"),
                        o.optInt("cost"), o.optString("expiryDate"), o.optString("code"), o.optString("giftType","gift"),
                        o.optString("status","unredeemed"), o.has("product_id") ? o.getString("product_id") : null,
                        o.optInt("minOrderValue"), o.optString("applicableProducts","Tất cả sản phẩm"),
                        o.optString("offerType","fixed_amount"), o.optInt("discountValue"), o.optString("rankRequired","Đồng")));
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    private void openGiftDetail(RedeemableGift gift, boolean isOwned, int dbId) {
        AccountRewardDetailFragment frag = AccountRewardDetailFragment.newInstance(
                gift.getGiftId(), gift.getTitle(), gift.getDescription(), gift.getCost(),
                gift.getExpiryDate(), gift.getCode(), gift.getGiftType(), isOwned, dbId,
                gift.getRankRequired(), gift.getMinOrderValue(), gift.getApplicableProducts(),
                gift.getOfferType(), gift.getProductId(), gift.getDiscountValue());
        getParentFragmentManager().beginTransaction().replace(R.id.main_container, frag).addToBackStack(null).commit();
    }

    private void performRedeemDirectly(RedeemableGift gift) {
        if (currentPoints < gift.getCost()) {
            Toast.makeText(requireContext(), "Bạn không đủ xu để đổi quà tặng này!", Toast.LENGTH_SHORT).show();
            return;
        }
        executor.execute(() -> {
            try {
                boolean success = repository.redeemGift(gift.getGiftId(), gift.getTitle(), gift.getDescription(),
                        gift.getCost(), gift.getExpiryDate(), gift.getCode(), gift.getGiftType(),
                        gift.getMinOrderValue(), gift.getApplicableProducts(), gift.getOfferType(),
                        gift.getProductId(), gift.getDiscountValue());
                if (isAdded()) requireActivity().runOnUiThread(() -> {
                    if (success) {
                        new MaterialAlertDialogBuilder(requireContext())
                                .setTitle("Đổi quà thành công!")
                                .setMessage("Chúc mừng! Bạn đã đổi thành công " + gift.getTitle() + ". Quà tặng đã được lưu vào mục 'Quà của tôi'.")
                                .setPositiveButton("Xem danh sách quà", (d, w) -> binding.btnTabMyGifts.performClick())
                                .setCancelable(false).show();
                    } else {
                        Toast.makeText(requireContext(), "Đổi quà thất bại. Vui lòng thử lại!", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) { e.printStackTrace(); }
        });
    }

    private void copyToClipboard(Context context, String text) {
        ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("Rootie Voucher Code", text));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        executor.shutdown();
    }
}
