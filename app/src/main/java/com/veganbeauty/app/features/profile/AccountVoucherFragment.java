package com.veganbeauty.app.features.profile;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwnerKt;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.veganbeauty.app.R;
import com.veganbeauty.app.core.base.RootieFragment;
import com.veganbeauty.app.data.local.LocalJsonReader;
import com.veganbeauty.app.data.local.RootieDatabase;
import com.veganbeauty.app.data.local.entities.UserGiftEntity;
import com.veganbeauty.app.data.repository.OrderRepository;
import com.veganbeauty.app.databinding.AccountVoucherBinding;
import com.veganbeauty.app.features.account.notification.AccountNotificationFragment;
import com.veganbeauty.app.features.home.BottomNavHelper;
import com.veganbeauty.app.features.profile.VoucherListAdapter.VoucherItem;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.Set;

import kotlin.Unit;
import kotlinx.coroutines.BuildersKt;
import kotlinx.coroutines.Dispatchers;

public class AccountVoucherFragment extends RootieFragment {

    private AccountVoucherBinding binding;

    private List<VoucherItem> allVouchers = new ArrayList<>();
    private List<VoucherItem> systemVouchers = new ArrayList<>();
    private final Set<String> deletedSystemVoucherIds = new HashSet<>();
    
    private VoucherListAdapter adapter;
    private OrderRepository repository;
    private int currentTab = 0; // 0: All, 1: Valid, 2: Expired

    private static final Set<String> deletedSystemVoucherIdsStatic = new HashSet<>();

    public static Set<String> getDeletedSystemVoucherIdsStatic() {
        return deletedSystemVoucherIdsStatic;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = AccountVoucherBinding.inflate(inflater, container, false);
        setupRepository();
        return binding.getRoot();
    }

    private void setupRepository() {
        RootieDatabase db = RootieDatabase.getDatabase(requireContext());
        repository = new OrderRepository(
                db.orderDao(),
                db.rewardPointDao(),
                db.userGiftDao(),
                new LocalJsonReader(requireContext())
        );
    }

    @Override
    public void setupUI(@NonNull View view) {
        Context context = requireContext();

        binding.btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        binding.btnNotification.setOnClickListener(v -> getParentFragmentManager().beginTransaction()
                .replace(R.id.main_container, new AccountNotificationFragment())
                .addToBackStack(null)
                .commit());

        systemVouchers = loadVouchersFromAssets(context);

        adapter = new VoucherListAdapter(new ArrayList<>());
        adapter.setOnDeleteClickListener(voucher -> {
            if (voucher.getId().startsWith("db_")) {
                String dbIdStr = voucher.getId().substring(3);
                try {
                    int dbId = Integer.parseInt(dbIdStr);
                    new Thread(() -> {
                        repository.deleteUserGiftById(dbId);
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                Toast.makeText(context, "Đã xoá voucher thành công", Toast.LENGTH_SHORT).show();
                            });
                        }
                    }).start();
                } catch (NumberFormatException e) {
                    // Ignore
                }
            } else {
                deletedSystemVoucherIds.add(voucher.getId());
                List<VoucherItem> activeSystem = new ArrayList<>();
                for (VoucherItem v : systemVouchers) {
                    if (!deletedSystemVoucherIds.contains(v.getId()) && !deletedSystemVoucherIdsStatic.contains(v.getId())) {
                        activeSystem.add(v);
                    }
                }
                
                List<VoucherItem> newAll = new ArrayList<>();
                for (VoucherItem v : allVouchers) {
                    if (!v.getId().equals(voucher.getId())) {
                        newAll.add(v);
                    }
                }
                allVouchers = newAll;
                Toast.makeText(context, "Đã xoá voucher thành công", Toast.LENGTH_SHORT).show();
                refreshVoucherList();
            }
        });
        
        adapter.setOnItemClickListener(voucher -> {
            AccountVoucherDetailFragment detailFragment = AccountVoucherDetailFragment.newInstance(voucher);
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, detailFragment)
                    .addToBackStack(null)
                    .commit();
        });

        adapter.setOnUseClickListener(voucher -> {
            BottomNavHelper.navigate(this, R.id.nav_shop);
        });

        binding.rvVouchers.setLayoutManager(new LinearLayoutManager(context));
        binding.rvVouchers.setAdapter(adapter);

        binding.tabAll.setOnClickListener(v -> {
            currentTab = 0;
            updateTabSelection(binding.tabAll, Arrays.asList(binding.tabValid, binding.tabExpired));
            refreshVoucherList();
        });

        binding.tabValid.setOnClickListener(v -> {
            currentTab = 1;
            updateTabSelection(binding.tabValid, Arrays.asList(binding.tabAll, binding.tabExpired));
            refreshVoucherList();
        });

        binding.tabExpired.setOnClickListener(v -> {
            currentTab = 2;
            updateTabSelection(binding.tabExpired, Arrays.asList(binding.tabAll, binding.tabValid));
            refreshVoucherList();
        });

        currentTab = 0;
        updateTabSelection(binding.tabAll, Arrays.asList(binding.tabValid, binding.tabExpired));

        highlightBottomTab(view);

        BottomNavHelper.setup(
                this,
                binding.getRoot(),
                R.id.nav_account,
                tabId -> {
                    BottomNavHelper.navigate(this, tabId);
                }
        );
    }

    @Override
    public void observeViewModel() {
        BuildersKt.launch(LifecycleOwnerKt.getLifecycleScope(getViewLifecycleOwner()), Dispatchers.getMain(), kotlinx.coroutines.CoroutineStart.DEFAULT, (coroutineScope, continuation) -> {
            kotlinx.coroutines.flow.FlowKt.launchIn(kotlinx.coroutines.flow.FlowKt.onEach(repository.getAllUserGifts(), new kotlin.jvm.functions.Function2<List<UserGiftEntity>, kotlin.coroutines.Continuation<? super kotlin.Unit>, Object>() {
                @Nullable
                @Override
                public Object invoke(List<UserGiftEntity> dbGifts, @NonNull kotlin.coroutines.Continuation<? super kotlin.Unit> continuation) {
                    List<VoucherItem> mappedDbVouchers = new ArrayList<>();
                    for (UserGiftEntity gift : dbGifts) {
                        if ("voucher_discount".equals(gift.getGiftType()) || "voucher_freeship".equals(gift.getGiftType())) {
                            String statusVal = computeStatusFromExpiry(gift.getExpiryDate());
                            mappedDbVouchers.add(new VoucherItem(
                                    "db_" + gift.getId(),
                                    gift.getTitle(),
                                    gift.getDescription(),
                                    gift.getCode(),
                                    statusVal,
                                    gift.getExpiryDate(),
                                    "voucher_freeship".equals(gift.getGiftType()) ? "free ship" : "discount",
                                    true,
                                    null,
                                    gift.getMinOrderValue(),
                                    gift.getApplicableProducts(),
                                    gift.getOfferType(),
                                    gift.getDiscountValue()
                            ));
                        }
                    }

                    List<VoucherItem> activeSystem = new ArrayList<>();
                    for (VoucherItem v : systemVouchers) {
                        if (!deletedSystemVoucherIds.contains(v.getId()) && !deletedSystemVoucherIdsStatic.contains(v.getId())) {
                            activeSystem.add(v);
                        }
                    }
                    
                    allVouchers = new ArrayList<>();
                    allVouchers.addAll(activeSystem);
                    allVouchers.addAll(mappedDbVouchers);
                    
                    refreshVoucherList();
                    return Unit.INSTANCE;
                }
            }), coroutineScope);
            return Unit.INSTANCE;
        });
    }

    private void refreshVoucherList() {
        List<VoucherItem> filteredList = new ArrayList<>();
        if (currentTab == 0) {
            filteredList.addAll(allVouchers);
        } else if (currentTab == 1) {
            for (VoucherItem v : allVouchers) {
                if ("valid".equals(v.getStatus()) || "expiring".equals(v.getStatus())) {
                    filteredList.add(v);
                }
            }
        } else if (currentTab == 2) {
            for (VoucherItem v : allVouchers) {
                if ("expired".equals(v.getStatus())) {
                    filteredList.add(v);
                }
            }
        } else {
            filteredList.addAll(allVouchers);
        }

        Collections.sort(filteredList, (v1, v2) -> {
            boolean e1 = "expiring".equals(v1.getStatus());
            boolean e2 = "expiring".equals(v2.getStatus());
            if (e1 && !e2) return -1;
            if (!e1 && e2) return 1;
            return 0;
        });

        adapter.updateList(filteredList);
        updateWarningBanner();
    }

    private void updateWarningBanner() {
        int expiringCount = 0;
        for (VoucherItem v : allVouchers) {
            if ("expiring".equals(v.getStatus())) {
                expiringCount++;
            }
        }
        
        if (expiringCount > 0) {
            binding.bannerWarning.setVisibility(View.VISIBLE);
            binding.tvWarningText.setText(expiringCount + " voucher sắp hết hạn");
        } else {
            binding.bannerWarning.setVisibility(View.GONE);
        }
    }

    private String computeStatusFromExpiry(String expiryStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            Date expiryDate = sdf.parse(expiryStr);
            if (expiryDate == null) return "valid";

            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);
            today.set(Calendar.MILLISECOND, 0);

            Calendar expiry = Calendar.getInstance();
            expiry.setTime(expiryDate);
            expiry.set(Calendar.HOUR_OF_DAY, 0);
            expiry.set(Calendar.MINUTE, 0);
            expiry.set(Calendar.SECOND, 0);
            expiry.set(Calendar.MILLISECOND, 0);

            if (expiry.before(today)) {
                return "expired";
            } else if (expiry.equals(today)) {
                return "expiring";
            } else {
                return "valid";
            }
        } catch (Exception e) {
            return "valid";
        }
    }

    private List<VoucherItem> loadVouchersFromAssets(Context ctx) {
        try {
            InputStream is = ctx.getAssets().open("vouchers.json");
            Scanner scanner = new Scanner(is).useDelimiter("\\A");
            String jsonString = scanner.hasNext() ? scanner.next() : "";
            is.close();

            JSONArray jsonArray = new JSONArray(jsonString);
            List<VoucherItem> list = new ArrayList<>();
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                String hsdStr = obj.optString("hsd", "");
                String statusVal = computeStatusFromExpiry(hsdStr);

                list.add(new VoucherItem(
                        obj.optString("id", ""),
                        obj.optString("title", ""),
                        obj.optString("description", ""),
                        obj.optString("code", ""),
                        statusVal,
                        hsdStr,
                        obj.optString("type", "discount"),
                        obj.optBoolean("from-gift", false),
                        obj.has("quantity") ? obj.getInt("quantity") : null,
                        obj.optInt("minOrderValue", 0),
                        obj.optString("applicableProducts", "Tất cả sản phẩm"),
                        obj.optString("offerType", "fixed_amount"),
                        obj.optInt("discountValue", 0)
                ));
            }
            return list;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    private void updateTabSelection(TextView selectedTab, List<TextView> inactiveTabs) {
        selectedTab.setBackgroundResource(R.drawable.tab_active_bg);
        selectedTab.setTextColor(Color.WHITE);
        selectedTab.setTypeface(null, Typeface.BOLD);

        for (TextView tab : inactiveTabs) {
            tab.setBackgroundResource(R.drawable.tab_inactive_bg);
            tab.setTextColor(Color.parseColor("#3E4D44"));
            tab.setTypeface(null, Typeface.NORMAL);
        }
    }

    private void highlightBottomTab(View view) {
        ViewGroup navAccount = view.findViewById(R.id.nav_account);
        if (navAccount != null) {
            if (navAccount.getChildAt(0) instanceof ImageView) {
                ((ImageView) navAccount.getChildAt(0)).setColorFilter(Color.parseColor("#677559"));
            }
            if (navAccount.getChildAt(1) instanceof TextView) {
                TextView label = (TextView) navAccount.getChildAt(1);
                label.setTextColor(Color.parseColor("#677559"));
                label.setTypeface(null, Typeface.BOLD);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        List<VoucherItem> activeSystem = new ArrayList<>();
        for (VoucherItem v : systemVouchers) {
            if (!deletedSystemVoucherIds.contains(v.getId()) && !deletedSystemVoucherIdsStatic.contains(v.getId())) {
                activeSystem.add(v);
            }
        }
        
        List<VoucherItem> dbVouchers = new ArrayList<>();
        for (VoucherItem v : allVouchers) {
            if (v.getId().startsWith("db_")) {
                dbVouchers.add(v);
            }
        }
        
        allVouchers = new ArrayList<>();
        allVouchers.addAll(activeSystem);
        allVouchers.addAll(dbVouchers);
        refreshVoucherList();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
