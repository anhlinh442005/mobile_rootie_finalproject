package com.veganbeauty.app.features.shop.product;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwnerKt;
import androidx.lifecycle.FlowLiveDataConversions;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.veganbeauty.app.R;
import com.veganbeauty.app.core.base.RootieFragment;
import com.veganbeauty.app.data.local.LocalJsonReader;
import com.veganbeauty.app.data.local.RootieDatabase;
import com.veganbeauty.app.data.local.entities.UserGiftEntity;
import com.veganbeauty.app.data.repository.OrderRepository;
import com.veganbeauty.app.data.repository.VoucherRepository;
import com.veganbeauty.app.databinding.ShopFragmentVoucherBinding;
import com.veganbeauty.app.features.profile.AccountVoucherFragment;
import com.veganbeauty.app.features.profile.VoucherItem;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import kotlinx.coroutines.BuildersKt;
import kotlinx.coroutines.Dispatchers;
import kotlinx.coroutines.flow.FlowCollector;

public class ShopVoucherFragment extends RootieFragment {

    public static final String TAG = "ShopVoucherFragment";
    public static final String ARG_SELECTED_VOUCHER_CODE = "selected_voucher_code";

    public static final String REQUEST_KEY = "voucher_selection_request";
    public static final String RESULT_VOUCHER_CODE = "result_voucher_code";
    public static final String RESULT_VOUCHER_DISCOUNT = "result_voucher_discount";
    public static final String RESULT_VOUCHER_TITLE = "result_voucher_title";

    private ShopFragmentVoucherBinding binding;
    private String selectedVoucherCode = null;
    private ShopVoucherAdapter adapter;
    private OrderRepository repository;

    private final List<VoucherUiModel> voucherList = new ArrayList<>();

    public static ShopVoucherFragment newInstance(String selectedCode) {
        ShopVoucherFragment fragment = new ShopVoucherFragment();
        Bundle args = new Bundle();
        args.putString(ARG_SELECTED_VOUCHER_CODE, selectedCode);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            selectedVoucherCode = getArguments().getString(ARG_SELECTED_VOUCHER_CODE);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = ShopFragmentVoucherBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void setupUI(View view) {
        binding.btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        setupRecyclerView();
        setupListeners();
        VoucherRepository.loadActiveVouchers(requireContext(), entities -> {
            if (!isAdded()) return;
            cachedSystemVouchers = VoucherRepository.toParcelableItems(entities);
            loadVouchers();
        });
        loadVouchers();
    }

    @Override
    public void observeViewModel() {}

    private void setupRecyclerView() {
        adapter = new ShopVoucherAdapter(voucherList, selectedVoucherCode, voucher -> {
            selectedVoucherCode = (voucher != null) ? voucher.code : null;
            binding.etCouponCode.setText(voucher != null ? voucher.code : "");
        });
        binding.rvVouchers.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvVouchers.setAdapter(adapter);

        if (selectedVoucherCode != null) {
            binding.etCouponCode.setText(selectedVoucherCode);
        }
    }

    private void setupListeners() {
        binding.etCouponCode.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                applyCodeInput();
                return true;
            }
            return false;
        });

        binding.etCouponCode.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                String input = s.toString().trim().toUpperCase();
                VoucherUiModel matched = null;
                for (VoucherUiModel v : voucherList) {
                    if (v.code.equals(input)) { matched = v; break; }
                }
                if (matched != null && !matched.code.equals(selectedVoucherCode)) {
                    selectedVoucherCode = matched.code;
                    adapter.setSelectedCode(matched.code);
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        binding.btnSelectVoucher.setOnClickListener(v -> {
            Bundle result = new Bundle();
            result.putString(RESULT_VOUCHER_CODE, selectedVoucherCode);
            VoucherUiModel selectedVoucher = null;
            for (VoucherUiModel uiModel : voucherList) {
                if (uiModel.code.equals(selectedVoucherCode)) { selectedVoucher = uiModel; break; }
            }

            if (selectedVoucher != null) {
                result.putLong(RESULT_VOUCHER_DISCOUNT, selectedVoucher.discountAmount);
                result.putString(RESULT_VOUCHER_TITLE, selectedVoucher.title);
            } else {
                result.putLong(RESULT_VOUCHER_DISCOUNT, 0L);
                result.putString(RESULT_VOUCHER_TITLE, "");
            }
            getParentFragmentManager().setFragmentResult(REQUEST_KEY, result);
            getParentFragmentManager().popBackStack();
        });
    }

    private void applyCodeInput() {
        String code = binding.etCouponCode.getText().toString().trim().toUpperCase();
        VoucherUiModel matched = null;
        for (VoucherUiModel v : voucherList) {
            if (v.code.equals(code)) { matched = v; break; }
        }

        if (matched != null) {
            selectedVoucherCode = matched.code;
            adapter.setSelectedCode(matched.code);
            Toast.makeText(requireContext(), "Đã áp dụng mã: " + matched.code, Toast.LENGTH_SHORT).show();
        } else if (!code.isEmpty()) {
            Toast.makeText(requireContext(), "Mã giảm giá không hợp lệ", Toast.LENGTH_SHORT).show();
        }

        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(binding.etCouponCode.getWindowToken(), 0);
    }

    private List<VoucherItem> cachedSystemVouchers = new ArrayList<>();

    private void loadVouchers() {
        Context context = requireContext();
        RootieDatabase db = RootieDatabase.getDatabase(context);
        repository = new OrderRepository(db.orderDao(), db.rewardPointDao(), db.userGiftDao(), new LocalJsonReader(context));

        FlowLiveDataConversions.asLiveData(repository.getAllUserGifts())
            .observe(getViewLifecycleOwner(), dbGifts -> {
                if (dbGifts == null) return;
                List<VoucherItem> systemVouchers = cachedSystemVouchers.isEmpty()
                        ? loadVouchersFromAssets(context)
                        : cachedSystemVouchers;
                List<VoucherItem> mappedDbVouchers = new ArrayList<>();

                for (UserGiftEntity gift : dbGifts) {
                    if ("voucher_discount".equals(gift.getGiftType()) || "voucher_freeship".equals(gift.getGiftType())) {
                        String statusVal = computeStatusFromExpiry(gift.getExpiryDate());
                        mappedDbVouchers.add(new VoucherItem(
                                "db_" + gift.getId(), gift.getTitle(), gift.getDescription(),
                                gift.getCode(), statusVal, gift.getExpiryDate(),
                                "voucher_freeship".equals(gift.getGiftType()) ? "free ship" : "discount",
                                true, 0, gift.getMinOrderValue(), gift.getApplicableProducts(),
                                gift.getOfferType(), gift.getDiscountValue()
                        ));
                    }
                }

                List<VoucherItem> activeSystem = new ArrayList<>();
                for (VoucherItem sys : systemVouchers) {
                    if (!AccountVoucherFragment.getDeletedSystemVoucherIdsStatic().contains(sys.getId())) {
                        activeSystem.add(sys);
                    }
                }

                List<VoucherItem> allVouchers = new ArrayList<>(activeSystem);
                allVouchers.addAll(mappedDbVouchers);

                List<VoucherItem> activeVouchers = new ArrayList<>();
                for (VoucherItem item : allVouchers) {
                    if ("valid".equals(item.getStatus()) || "expiring".equals(item.getStatus())) {
                        activeVouchers.add(item);
                    }
                }

                voucherList.clear();
                for (VoucherItem item : activeVouchers) {
                    String minSpendText = "Đơn tối thiểu: " + formatCurrency(item.getMinOrderValue()) + "đ";
                    String expiryText = "Hết hạn: " + formatHsd(item.getHsd());
                    voucherList.add(new VoucherUiModel(
                            item.getCode(), item.getTitle(), minSpendText, expiryText,
                            item.getDiscountValue(), item.getMinOrderValue()
                    ));
                }
                adapter.notifyDataSetChanged();

                if (selectedVoucherCode != null) {
                    VoucherUiModel matched = null;
                    for (VoucherUiModel v : voucherList) {
                        if (v.code.equals(selectedVoucherCode)) { matched = v; break; }
                    }
                    if (matched != null) adapter.setSelectedCode(selectedVoucherCode);
                }
            });
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

            if (expiry.before(today)) return "expired";
            else if (expiry.equals(today)) return "expiring";
            else return "valid";
        } catch (Exception e) {
            return "valid";
        }
    }

    private String formatCurrency(int amount) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setGroupingSeparator('.');
        DecimalFormat df = new DecimalFormat("#,###", symbols);
        return df.format(amount);
    }

    private String formatHsd(String hsdStr) {
        try {
            SimpleDateFormat inputSdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            Date date = inputSdf.parse(hsdStr);
            if (date == null) return hsdStr;
            SimpleDateFormat outputSdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            return outputSdf.format(date);
        } catch (Exception e) {
            if (hsdStr.contains(" ")) return hsdStr.split(" ")[0];
            return hsdStr;
        }
    }

    private List<VoucherItem> loadVouchersFromAssets(Context ctx) {
        try {
            java.io.InputStream is = ctx.getAssets().open("vouchers.json");
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            is.close();
            String jsonString = new String(buffer, "UTF-8");

            JSONArray jsonArray = new JSONArray(jsonString);
            List<VoucherItem> list = new ArrayList<>();
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                String hsdStr = obj.optString("hsd", "");
                String statusVal = computeStatusFromExpiry(hsdStr);

                list.add(new VoucherItem(
                        obj.optString("id", ""), obj.optString("title", ""), obj.optString("description", ""),
                        obj.optString("code", ""), statusVal, hsdStr, obj.optString("type", "discount"),
                        obj.optBoolean("from-gift", false), obj.has("quantity") ? obj.getInt("quantity") : null,
                        obj.optInt("minOrderValue", 0), obj.optString("applicableProducts", "Tất cả sản phẩm"),
                        obj.optString("offerType", "fixed_amount"), obj.optInt("discountValue", 0)
                ));
            }
            return list;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    public static class VoucherUiModel {
        public final String code;
        public final String title;
        public final String minSpendText;
        public final String expiryText;
        public final long discountAmount;
        public final long minSpendAmount;

        public VoucherUiModel(String code, String title, String minSpendText, String expiryText, long discountAmount, long minSpendAmount) {
            this.code = code;
            this.title = title;
            this.minSpendText = minSpendText;
            this.expiryText = expiryText;
            this.discountAmount = discountAmount;
            this.minSpendAmount = minSpendAmount;
        }
    }

    public static class ShopVoucherAdapter extends RecyclerView.Adapter<ShopVoucherAdapter.ViewHolder> {
        private final List<VoucherUiModel> items;
        private String selectedCode;
        private final OnSelectionChangedListener listener;

        public interface OnSelectionChangedListener {
            void onSelectionChanged(VoucherUiModel voucher);
        }

        public ShopVoucherAdapter(List<VoucherUiModel> items, String initialSelectedCode, OnSelectionChangedListener listener) {
            this.items = items;
            this.selectedCode = initialSelectedCode;
            this.listener = listener;
        }

        public void setSelectedCode(String code) {
            this.selectedCode = code;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.shop_item_voucher_selection, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            VoucherUiModel item = items.get(position);
            holder.bind(item, item.code.equals(selectedCode), () -> {
                if (item.code.equals(selectedCode)) {
                    selectedCode = null;
                    listener.onSelectionChanged(null);
                } else {
                    selectedCode = item.code;
                    listener.onSelectionChanged(item);
                }
                notifyDataSetChanged();
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            private final TextView title;
            private final TextView minSpend;
            private final TextView expiry;
            private final ImageView selectIndicator;

            public ViewHolder(View view) {
                super(view);
                title = view.findViewById(R.id.tvVoucherTitle);
                minSpend = view.findViewById(R.id.tvVoucherMinSpend);
                expiry = view.findViewById(R.id.tvVoucherExpiry);
                selectIndicator = view.findViewById(R.id.ivSelectIndicator);
            }

            public void bind(VoucherUiModel item, boolean isSelected, Runnable onClick) {
                title.setText(item.title);
                minSpend.setText(item.minSpendText);
                expiry.setText(item.expiryText);

                if (isSelected) {
                    selectIndicator.setImageResource(R.drawable.ic_circle_checked);
                    selectIndicator.setImageTintList(ColorStateList.valueOf(Color.parseColor("#3E4D44")));
                } else {
                    selectIndicator.setImageResource(R.drawable.ic_circle);
                    selectIndicator.setImageTintList(ColorStateList.valueOf(Color.parseColor("#807F7F")));
                }

                itemView.setOnClickListener(v -> onClick.run());
            }
        }
    }
}
