package com.veganbeauty.app.features.shop.product;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.LifecycleOwnerKt;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.gson.Gson;
import com.veganbeauty.app.R;
import com.veganbeauty.app.core.base.RootieFragment;
import com.veganbeauty.app.data.local.ProfileSession;
import com.veganbeauty.app.data.local.RootieDatabase;
import com.veganbeauty.app.data.remote.FirestoreService;
import com.veganbeauty.app.data.local.entities.CartItemEntity;
import com.veganbeauty.app.data.local.entities.OrderEntity;
import com.veganbeauty.app.data.local.entities.OrderEntity.OrderItem;
import com.veganbeauty.app.data.local.entities.ProductEntity;
import com.veganbeauty.app.databinding.ShopFragmentCheckoutBinding;
import com.veganbeauty.app.utils.AffiliateTrackingHelper;
import com.veganbeauty.app.features.shop.store.ShopStoreSelectionFragment;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import kotlinx.coroutines.BuildersKt;
import kotlinx.coroutines.Dispatchers;
import kotlinx.coroutines.flow.FlowCollector;
import kotlinx.coroutines.tasks.TasksKt;

public class ShopCheckoutFragment extends RootieFragment {

    public static final String TAG = "ShopCheckoutFragment";
    public static final String ARG_CHECKOUT_ITEMS = "checkout_items";
    public static final String ARG_INITIAL_VOUCHER_CODE = "initial_voucher_code";
    public static final String ARG_INITIAL_VOUCHER_DISCOUNT = "initial_voucher_discount";
    public static final String SUCCESS_TAG = "shop_checkout_success";

    private ShopFragmentCheckoutBinding binding;

    private ArrayList<CartItemEntity> checkoutItems = new ArrayList<>();
    private ShopCheckoutProductAdapter adapter;
    private RootieDatabase database;

    private boolean isLoggedIn = false;
    private BuyerInfo lastGuestBuyerInfo = null;
    private String deliveryType = "Giao hàng tận nơi";

    private int selectedAddressIndex = 0;
    private ArrayList<CheckoutAddress> memberAddresses = new ArrayList<>();

    private String selectedStoreId = "CH001";
    private String selectedStoreName = "Cửa hàng mỹ phẩm Rootie - Cơ sở 1";
    private String selectedStoreAddress = "235 Nguyễn Thị Minh Khai, P. Nguyễn Cư Trinh, Q.1, TP.HCM";

    private String recipientName = "Bảo Nguyên";
    private String recipientPhone = "0123456789";
    private String recipientAddress = "123 Lê Lợi, phường 5, quận 1, Hồ Chí Minh";
    private String paymentMethod = "Thanh toán tiền mặt khi nhận hàng";
    private boolean isVoucherApplied = false;
    private long voucherDiscountAmount = 0L;
    private String selectedVoucherCode = null;
    private boolean isInvoiceChecked = false;
    private boolean isPointsChecked = false;
    private boolean isHideProductInfoChecked = false;

    private static class CheckoutAddress {
        String name;
        String phone;
        String details;
        boolean isDefault;

        CheckoutAddress(String name, String phone, String details, boolean isDefault) {
            this.name = name;
            this.phone = phone;
            this.details = details;
            this.isDefault = isDefault;
        }
    }

    private static class BuyerInfo {
        String name;
        String phone;
        String email;
        String address;

        BuyerInfo(String name, String phone, String email, String address) {
            this.name = name;
            this.phone = phone;
            this.email = email;
            this.address = address;
        }
    }

    public static ShopCheckoutFragment newInstance(ArrayList<CartItemEntity> items, String initialVoucherCode, long initialVoucherDiscount) {
        ShopCheckoutFragment fragment = new ShopCheckoutFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_CHECKOUT_ITEMS, items);
        args.putString(ARG_INITIAL_VOUCHER_CODE, initialVoucherCode);
        args.putLong(ARG_INITIAL_VOUCHER_DISCOUNT, initialVoucherDiscount);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        database = RootieDatabase.getDatabase(requireContext());
        isLoggedIn = ProfileSession.isLoggedIn(requireContext());
        if (getArguments() != null) {
            checkoutItems = (ArrayList<CartItemEntity>) getArguments().getSerializable(ARG_CHECKOUT_ITEMS);
            if (checkoutItems == null) checkoutItems = new ArrayList<>();
            selectedVoucherCode = getArguments().getString(ARG_INITIAL_VOUCHER_CODE);
            voucherDiscountAmount = getArguments().getLong(ARG_INITIAL_VOUCHER_DISCOUNT, 0L);
            isVoucherApplied = (selectedVoucherCode != null && !selectedVoucherCode.isEmpty());
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = ShopFragmentCheckoutBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void setupUI(View view) {
        setupRecyclerView();
        setupListeners();
        applyCheckoutMode();
        if (isLoggedIn) {
            syncMemberAddressIntoSelectionSheet();
        } else {
            updateDeliveryUI();
        }
        updatePaymentMethodUI();

        if (isVoucherApplied) {
            NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
            binding.tvVoucherDesc.setText("Đã áp dụng mã: " + selectedVoucherCode + " (-" + formatter.format(voucherDiscountAmount) + ")");
            binding.llVoucherRow.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#E5F2FF")));
        } else {
            binding.tvVoucherDesc.setText("Áp dụng ưu đãi để được giảm giá");
            binding.llVoucherRow.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#EDF3ED")));
        }

        calculatePrices();
    }

    @Override
    public void observeViewModel() {}

    private void applyCheckoutMode() {
        if (isLoggedIn) {
            binding.llGuestForm.setVisibility(View.GONE);
            binding.llGuestStorePickup.setVisibility(View.GONE);
            binding.llMemberAddress.setVisibility(View.VISIBLE);
            binding.btnChangeAddress.setVisibility(View.VISIBLE);
            binding.llPointsRow.setVisibility(View.VISIBLE);

            String name = ProfileSession.getFullName(requireContext());
            if (!name.isEmpty()) recipientName = name;
            String phone = ProfileSession.getPhone(requireContext());
            if (!phone.isEmpty()) recipientPhone = phone;
            String savedAddress = ProfileSession.getAddress(requireContext());
            if (!savedAddress.isEmpty()) recipientAddress = savedAddress;
        } else {
            binding.llGuestForm.setVisibility(View.VISIBLE);
            if (deliveryType.equals("Nhận tại cửa hàng")) {
                binding.tvGuestAddressLabel.setVisibility(View.GONE);
                binding.etGuestAddress.setVisibility(View.GONE);
            } else {
                binding.tvGuestAddressLabel.setVisibility(View.VISIBLE);
                binding.etGuestAddress.setVisibility(View.VISIBLE);
            }
            binding.llMemberAddress.setVisibility(View.GONE);
            binding.btnChangeAddress.setVisibility(View.GONE);
            binding.llPointsRow.setVisibility(View.GONE);
            isPointsChecked = false;
            updateGuestStorePickupUI();
            updateGuestStoreNotice();
        }
    }

    private ArrayList<CheckoutAddress> loadSavedAddresses() {
        SharedPreferences prefs = requireContext().getSharedPreferences("rootie_profile_prefs", Context.MODE_PRIVATE);
        String jsonStr = prefs.getString("saved_addresses_list_json", null);
        ArrayList<CheckoutAddress> list = new ArrayList<>();

        if (jsonStr != null && !jsonStr.trim().isEmpty()) {
            try {
                JSONArray array = new JSONArray(jsonStr);
                for (int i = 0; i < array.length(); i++) {
                    JSONObject obj = array.getJSONObject(i);
                    list.add(new CheckoutAddress(
                            obj.getString("name"),
                            obj.getString("phone"),
                            obj.getString("address"),
                            obj.optBoolean("isDefault", false)
                    ));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (list.isEmpty()) {
            String homeName = prefs.getString("addr_home_name", "Ánh Linh");
            String homePhone = prefs.getString("addr_home_phone", "0999 999 999");
            String homeAddr = prefs.getString("addr_home_addr", "123 Đường Bến Nghé, Phường Bến Nghé, TP.Hồ Chí Minh");
            String defaultType = prefs.getString("addr_default_type", "HOME");

            list.add(new CheckoutAddress(homeName, homePhone, homeAddr, "HOME".equals(defaultType)));

            String officeName = prefs.getString("addr_office_name", "Khánh Xuân");
            String officePhone = prefs.getString("addr_office_phone", "0868 888 888");
            String officeAddr = prefs.getString("addr_office_addr", "Bitexco Financial Tower, 2 Hải Triều, Phường Bến Nghé, TP.Hồ Chí Minh");

            list.add(new CheckoutAddress(officeName, officePhone, officeAddr, "OFFICE".equals(defaultType)));
            saveAddressesList(list);
        }
        return list;
    }

    private void saveAddressesList(List<CheckoutAddress> list) {
        SharedPreferences prefs = requireContext().getSharedPreferences("rootie_profile_prefs", Context.MODE_PRIVATE);
        try {
            JSONArray array = new JSONArray();
            CheckoutAddress defaultAddr = null;
            for (CheckoutAddress addr : list) {
                JSONObject obj = new JSONObject();
                obj.put("name", addr.name);
                obj.put("phone", addr.phone);
                obj.put("address", addr.details);
                obj.put("isDefault", addr.isDefault);
                array.put(obj);
                if (addr.isDefault && defaultAddr == null) defaultAddr = addr;
            }
            prefs.edit().putString("saved_addresses_list_json", array.toString()).apply();
            if (defaultAddr == null && !list.isEmpty()) defaultAddr = list.get(0);
            if (defaultAddr != null) {
                ProfileSession.setAddress(requireContext(), defaultAddr.details);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void syncMemberAddressIntoSelectionSheet() {
        memberAddresses = loadSavedAddresses();
        int defaultIdx = 0;
        for (int i = 0; i < memberAddresses.size(); i++) {
            if (memberAddresses.get(i).isDefault) {
                defaultIdx = i; break;
            }
        }
        selectedAddressIndex = defaultIdx;

        if (selectedAddressIndex < memberAddresses.size()) {
            CheckoutAddress active = memberAddresses.get(selectedAddressIndex);
            recipientName = active.name;
            recipientPhone = active.phone;
            recipientAddress = active.details;
        }
        updateDeliveryUI();
    }

    private void setupRecyclerView() {
        adapter = new ShopCheckoutProductAdapter();
        binding.rvCheckoutItems.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvCheckoutItems.setAdapter(adapter);
        adapter.submitList(checkoutItems);
    }

    private void setupListeners() {
        binding.btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        binding.llDeliveryType.setOnClickListener(v -> showDeliveryTypeBottomSheet());

        binding.btnChangeAddress.setOnClickListener(v -> {
            if ("Nhận tại cửa hàng".equals(deliveryType)) {
                ShopStoreSelectionFragment storeFragment = ShopStoreSelectionFragment.newInstance(true, selectedStoreId, false, null, null);
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.main_container, storeFragment)
                        .addToBackStack(null)
                        .commit();
            } else {
                showAddressSelectionBottomSheet();
            }
        });

        binding.btnChangeStore.setOnClickListener(v -> {
            ShopStoreSelectionFragment storeFragment = ShopStoreSelectionFragment.newInstance(true, selectedStoreId, false, null, null);
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, storeFragment)
                    .addToBackStack(null)
                    .commit();
        });

        getParentFragmentManager().setFragmentResultListener(ShopStoreSelectionFragment.REQUEST_KEY, getViewLifecycleOwner(), (requestKey, bundle) -> {
            String storeId = bundle.getString(ShopStoreSelectionFragment.RESULT_STORE_ID);
            String storeName = bundle.getString(ShopStoreSelectionFragment.RESULT_STORE_NAME);
            String storeAddress = bundle.getString(ShopStoreSelectionFragment.RESULT_STORE_ADDRESS);
            if (storeId != null && storeName != null && storeAddress != null) {
                selectedStoreId = storeId;
                selectedStoreName = storeName;
                selectedStoreAddress = storeAddress;
                updateDeliveryUI();
                if (!isLoggedIn && "Nhận tại cửa hàng".equals(deliveryType)) {
                    updateGuestStorePickupUI();
                }
            }
        });

        binding.btnChangePaymentMethod.setOnClickListener(v -> showPaymentMethodBottomSheet());

        binding.llVoucherRow.setOnClickListener(v -> {
            ShopVoucherFragment voucherFragment = ShopVoucherFragment.newInstance(selectedVoucherCode);
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, voucherFragment)
                    .addToBackStack(null)
                    .commit();
        });

        getParentFragmentManager().setFragmentResultListener(ShopVoucherFragment.REQUEST_KEY, getViewLifecycleOwner(), (requestKey, bundle) -> {
            String code = bundle.getString(ShopVoucherFragment.RESULT_VOUCHER_CODE);
            long discount = bundle.getLong(ShopVoucherFragment.RESULT_VOUCHER_DISCOUNT, 0L);

            selectedVoucherCode = code;
            voucherDiscountAmount = discount;
            isVoucherApplied = (code != null && !code.isEmpty());

            if (isVoucherApplied) {
                NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
                binding.tvVoucherDesc.setText("Đã áp dụng mã: " + code + " (-" + formatter.format(discount) + ")");
                binding.llVoucherRow.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#E5F2FF")));
            } else {
                binding.tvVoucherDesc.setText("Áp dụng ưu đãi để được giảm giá");
                binding.llVoucherRow.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#EDF3ED")));
            }
            calculatePrices();
        });

        binding.switchPoints.setOnClickListener(v -> {
            isPointsChecked = !isPointsChecked;
            updateSwitchUI(binding.switchPoints, binding.switchPointsThumb, isPointsChecked);
            calculatePrices();
        });

        binding.switchInvoice.setOnClickListener(v -> {
            isInvoiceChecked = !isInvoiceChecked;
            updateSwitchUI(binding.switchInvoice, binding.switchInvoiceThumb, isInvoiceChecked);
        });

        binding.switchHideProductInfo.setOnClickListener(v -> {
            isHideProductInfoChecked = !isHideProductInfoChecked;
            updateSwitchUI(binding.switchHideProductInfo, binding.switchHideProductInfoThumb, isHideProductInfoChecked);
        });

        binding.btnCheckout.setOnClickListener(v -> performCheckout());
    }

    private void updateDeliveryUI() {
        binding.tvDeliveryTypeValue.setText(deliveryType);
        if ("Nhận tại cửa hàng".equals(deliveryType)) {
            binding.tvAddressTitle.setText("Nhận hàng tại");
            binding.tvDefaultBadge.setVisibility(View.GONE);
            binding.tvRecipientNamePhone.setText(selectedStoreName);
            binding.tvRecipientAddress.setText(selectedStoreAddress);
        } else {
            binding.tvAddressTitle.setText("Giao hàng tới");
            if (isLoggedIn) {
                CheckoutAddress active = null;
                if (selectedAddressIndex >= 0 && selectedAddressIndex < memberAddresses.size()) {
                    active = memberAddresses.get(selectedAddressIndex);
                }
                binding.tvDefaultBadge.setVisibility((active != null && active.isDefault) ? View.VISIBLE : View.GONE);
                binding.tvRecipientNamePhone.setText((active != null) ? (active.name + " - " + active.phone) : (recipientName + " - " + recipientPhone));
                binding.tvRecipientAddress.setText((active != null) ? active.details : recipientAddress);
            } else {
                binding.tvDefaultBadge.setVisibility(View.GONE);
                binding.tvRecipientNamePhone.setText(recipientName + " - " + recipientPhone);
                binding.tvRecipientAddress.setText(recipientAddress);
            }
        }
        applyCheckoutMode();
        updateGuestStoreNotice();
    }

    private void updateGuestStoreNotice() {
        if (!isLoggedIn && "Nhận tại cửa hàng".equals(deliveryType)) {
            binding.tvGuestStorePickupNotice.setVisibility(View.VISIBLE);
            binding.tvGuestStorePickupNotice.setText("Bạn đã chọn nhận hàng tại cửa hàng. Vui lòng chọn cửa hàng bên dưới.");
        } else {
            binding.tvGuestStorePickupNotice.setVisibility(View.GONE);
        }
    }

    private void updateGuestStorePickupUI() {
        if (!isLoggedIn && "Nhận tại cửa hàng".equals(deliveryType)) {
            binding.llGuestStorePickup.setVisibility(View.VISIBLE);
            binding.tvGuestStoreName.setText(selectedStoreName);
            binding.tvGuestStoreAddress.setText(selectedStoreAddress);
        } else {
            binding.llGuestStorePickup.setVisibility(View.GONE);
        }
    }

    private void updatePaymentMethodUI() {
        binding.tvPaymentMethod.setText(paymentMethod);
        if ("Thanh toán tiền mặt khi nhận hàng".equals(paymentMethod)) {
            binding.ivPaymentMethodIcon.setImageResource(R.drawable.ic_cash);
            binding.ivPaymentMethodIcon.setImageTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#3E4D44")));
        } else if ("Thẻ ATM nội địa/Internet Banking".equals(paymentMethod)) {
            binding.ivPaymentMethodIcon.setImageResource(R.drawable.ic_credit_card);
            binding.ivPaymentMethodIcon.setImageTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#3E4D44")));
        } else if ("Thanh toán trực tuyến MoMo".equals(paymentMethod)) {
            binding.ivPaymentMethodIcon.setImageResource(R.drawable.ic_logo_momo);
            binding.ivPaymentMethodIcon.setImageTintList(null);
        } else if ("Thanh toán trực tuyến VNPay".equals(paymentMethod)) {
            binding.ivPaymentMethodIcon.setImageResource(R.drawable.ic_vnpay);
            binding.ivPaymentMethodIcon.setImageTintList(null);
        } else {
            binding.ivPaymentMethodIcon.setImageResource(R.drawable.ic_cash);
            binding.ivPaymentMethodIcon.setImageTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#3E4D44")));
        }
    }

    private void calculatePrices() {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

        long originalPriceSum = 0;
        long finalPriceSum = 0;
        for (CartItemEntity ci : checkoutItems) {
            originalPriceSum += (long) (ci.getPrice() * 1.2) * ci.getQuantity();
            finalPriceSum += ci.getPrice() * ci.getQuantity();
        }

        long directDiscount = originalPriceSum - finalPriceSum;
        long pointsDiscount = 0L;
        if (isPointsChecked && finalPriceSum > 2400) {
            pointsDiscount = 2400L;
            finalPriceSum -= pointsDiscount;
        }

        if (isVoucherApplied && finalPriceSum > voucherDiscountAmount) {
            finalPriceSum -= voucherDiscountAmount;
        }

        long totalSavings = originalPriceSum - finalPriceSum;

        binding.tvDetailSubtotal.setText(formatter.format(originalPriceSum));
        binding.tvDetailDirectDiscount.setText((directDiscount + pointsDiscount > 0) ? "-" + formatter.format(directDiscount + pointsDiscount) : "0đ");
        binding.tvDetailVoucherDiscount.setText((voucherDiscountAmount > 0) ? "-" + formatter.format(voucherDiscountAmount) : "0đ");

        binding.tvOriginalTotalPrice.setText(formatter.format(originalPriceSum));
        binding.tvOriginalTotalPrice.setPaintFlags(binding.tvOriginalTotalPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        binding.tvTotalValue.setText(formatter.format(finalPriceSum));
        binding.tvSavingsValue.setText(formatter.format(totalSavings));
    }

    private void showDeliveryTypeBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View sheetView = LayoutInflater.from(requireContext()).inflate(R.layout.shop_bottom_sheet_delivery_type, null);
        dialog.setContentView(sheetView);

        ImageView ivRadioDelivery = sheetView.findViewById(R.id.ivRadioDelivery);
        ImageView ivRadioStore = sheetView.findViewById(R.id.ivRadioStore);
        View clOptionDelivery = sheetView.findViewById(R.id.clOptionDelivery);
        View clOptionStore = sheetView.findViewById(R.id.clOptionStore);

        if ("Giao hàng tận nơi".equals(deliveryType)) {
            ivRadioDelivery.setImageResource(R.drawable.ic_circle_checked);
            ivRadioDelivery.setImageTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#3E4D44")));
            ivRadioStore.setImageResource(R.drawable.ic_circle);
            ivRadioStore.setImageTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#807F7F")));
        } else {
            ivRadioDelivery.setImageResource(R.drawable.ic_circle);
            ivRadioDelivery.setImageTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#807F7F")));
            ivRadioStore.setImageResource(R.drawable.ic_circle_checked);
            ivRadioStore.setImageTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#3E4D44")));
        }

        clOptionDelivery.setOnClickListener(v -> {
            deliveryType = "Giao hàng tận nơi";
            updateDeliveryUI();
            dialog.dismiss();
        });

        clOptionStore.setOnClickListener(v -> {
            deliveryType = "Nhận tại cửa hàng";
            updateDeliveryUI();
            dialog.dismiss();
        });

        dialog.show();
    }

    private int tempSelectedIndex = 0;
    private void showAddressSelectionBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View sheetView = LayoutInflater.from(requireContext()).inflate(R.layout.shop_bottom_sheet_select_address, null);
        dialog.setContentView(sheetView);

        LinearLayout llAddressContainer = sheetView.findViewById(R.id.llAddressContainer);
        View btnConfirmAddress = sheetView.findViewById(R.id.btnConfirmAddress);
        View btnAddAddress = sheetView.findViewById(R.id.btnAddAddress);

        tempSelectedIndex = selectedAddressIndex;

        Runnable populateAddressViews = new Runnable() {
            @Override
            public void run() {
                llAddressContainer.removeAllViews();
                for (int i = 0; i < memberAddresses.size(); i++) {
                    CheckoutAddress addr = memberAddresses.get(i);
                    int index = i;
                    View itemView = LayoutInflater.from(requireContext()).inflate(R.layout.shop_item_select_address, llAddressContainer, false);

                    TextView tvNamePhone = itemView.findViewById(R.id.tvNamePhone);
                    TextView tvAddressDetails = itemView.findViewById(R.id.tvAddressDetails);
                    TextView tvTagDefault = itemView.findViewById(R.id.tvTagDefault);
                    ImageView ivRadioAddress = itemView.findViewById(R.id.ivRadioAddress);
                    TextView tvEditAddress = itemView.findViewById(R.id.tvEditAddress);

                    tvNamePhone.setText(addr.name + " | " + addr.phone);
                    tvAddressDetails.setText(addr.details);
                    tvTagDefault.setVisibility(addr.isDefault ? View.VISIBLE : View.GONE);

                    if (index == tempSelectedIndex) {
                        ivRadioAddress.setImageResource(R.drawable.ic_circle_checked);
                        ivRadioAddress.setImageTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#3E4D44")));
                    } else {
                        ivRadioAddress.setImageResource(R.drawable.ic_circle);
                        ivRadioAddress.setImageTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#807F7F")));
                    }

                    itemView.setOnClickListener(v -> {
                        tempSelectedIndex = index;
                        this.run();
                    });

                    tvEditAddress.setOnClickListener(v -> showEditAddressForm(index, this));

                    llAddressContainer.addView(itemView);

                    if (index < memberAddresses.size() - 1) {
                        View divider = new View(requireContext());
                        divider.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1));
                        divider.setBackgroundColor(Color.parseColor("#E0E0E0"));
                        llAddressContainer.addView(divider);
                    }
                }
            }
        };

        populateAddressViews.run();

        btnAddAddress.setOnClickListener(v -> showAddAddressForm(populateAddressViews));

        btnConfirmAddress.setOnClickListener(v -> {
            selectedAddressIndex = tempSelectedIndex;
            if (selectedAddressIndex >= 0 && selectedAddressIndex < memberAddresses.size()) {
                CheckoutAddress active = memberAddresses.get(selectedAddressIndex);
                recipientName = active.name;
                recipientPhone = active.phone;
                recipientAddress = active.details;
            }
            updateDeliveryUI();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void showEditAddressForm(int index, Runnable onUpdated) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.shop_dialog_edit_address, null);
        TextView tvTitle = dialogView.findViewById(R.id.tv_dialog_title);
        EditText etName = dialogView.findViewById(R.id.et_dialog_name);
        EditText etPhone = dialogView.findViewById(R.id.et_dialog_phone);
        EditText etAddress = dialogView.findViewById(R.id.et_dialog_address);
        FrameLayout switchDefault = dialogView.findViewById(R.id.switch_dialog_default);
        ImageView switchDefaultThumb = dialogView.findViewById(R.id.switch_dialog_default_thumb);
        View btnCancel = dialogView.findViewById(R.id.btn_dialog_cancel);
        View btnSave = dialogView.findViewById(R.id.btn_dialog_save);

        if (index < 0 || index >= memberAddresses.size()) return;
        CheckoutAddress addrItem = memberAddresses.get(index);

        tvTitle.setText("Chỉnh sửa địa chỉ");
        etName.setText(addrItem.name);
        etPhone.setText(addrItem.phone);
        etAddress.setText(addrItem.details);

        final boolean[] isDefaultChecked = {addrItem.isDefault};
        updateSwitchUI(switchDefault, switchDefaultThumb, isDefaultChecked[0]);

        switchDefault.setOnClickListener(v -> {
            isDefaultChecked[0] = !isDefaultChecked[0];
            updateSwitchUI(switchDefault, switchDefaultThumb, isDefaultChecked[0]);
        });

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();
            String addr = etAddress.getText().toString().trim();
            if (!name.isEmpty() && !phone.isEmpty() && !addr.isEmpty()) {
                addrItem.name = name;
                addrItem.phone = phone;
                addrItem.details = addr;

                if (isDefaultChecked[0]) {
                    for (int i = 0; i < memberAddresses.size(); i++) memberAddresses.get(i).isDefault = (i == index);
                } else if (addrItem.isDefault) {
                    addrItem.isDefault = false;
                }

                saveAddressesList(memberAddresses);
                onUpdated.run();
                dialog.dismiss();
            } else {
                Toast.makeText(requireContext(), "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private void showAddAddressForm(Runnable onUpdated) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.shop_dialog_edit_address, null);
        TextView tvTitle = dialogView.findViewById(R.id.tv_dialog_title);
        EditText etName = dialogView.findViewById(R.id.et_dialog_name);
        EditText etPhone = dialogView.findViewById(R.id.et_dialog_phone);
        EditText etAddress = dialogView.findViewById(R.id.et_dialog_address);
        FrameLayout switchDefault = dialogView.findViewById(R.id.switch_dialog_default);
        ImageView switchDefaultThumb = dialogView.findViewById(R.id.switch_dialog_default_thumb);
        View btnCancel = dialogView.findViewById(R.id.btn_dialog_cancel);
        View btnSave = dialogView.findViewById(R.id.btn_dialog_save);

        tvTitle.setText("Thêm địa chỉ mới");
        final boolean[] isDefaultChecked = {false};
        updateSwitchUI(switchDefault, switchDefaultThumb, isDefaultChecked[0]);

        switchDefault.setOnClickListener(v -> {
            isDefaultChecked[0] = !isDefaultChecked[0];
            updateSwitchUI(switchDefault, switchDefaultThumb, isDefaultChecked[0]);
        });

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();
            String addr = etAddress.getText().toString().trim();
            if (!name.isEmpty() && !phone.isEmpty() && !addr.isEmpty()) {
                CheckoutAddress newAddr = new CheckoutAddress(name, phone, addr, isDefaultChecked[0]);

                if (isDefaultChecked[0]) {
                    for (CheckoutAddress a : memberAddresses) a.isDefault = false;
                }

                memberAddresses.add(newAddr);
                saveAddressesList(memberAddresses);
                onUpdated.run();
                dialog.dismiss();
            } else {
                Toast.makeText(requireContext(), "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private void showPaymentMethodBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View sheetView = LayoutInflater.from(requireContext()).inflate(R.layout.shop_bottom_sheet_payment_method, null);
        dialog.setContentView(sheetView);

        ImageView ivRadioCash = sheetView.findViewById(R.id.ivRadioCash);
        ImageView ivRadioATM = sheetView.findViewById(R.id.ivRadioATM);
        ImageView ivRadioMoMo = sheetView.findViewById(R.id.ivRadioMoMo);
        ImageView ivRadioVNPay = sheetView.findViewById(R.id.ivRadioVNPay);

        View llOptionCash = sheetView.findViewById(R.id.llOptionCash);
        View llOptionATM = sheetView.findViewById(R.id.llOptionATM);
        View llOptionMoMo = sheetView.findViewById(R.id.llOptionMoMo);
        View llOptionVNPay = sheetView.findViewById(R.id.llOptionVNPay);

        Runnable updateRadios = () -> {
            boolean cash = "Thanh toán tiền mặt khi nhận hàng".equals(paymentMethod);
            boolean atm = "Thẻ ATM nội địa/Internet Banking".equals(paymentMethod);
            boolean momo = "Thanh toán trực tuyến MoMo".equals(paymentMethod);
            boolean vnpay = "Thanh toán trực tuyến VNPay".equals(paymentMethod);

            ivRadioCash.setImageResource(cash ? R.drawable.ic_circle_checked : R.drawable.ic_circle);
            ivRadioCash.setImageTintList(cash ? android.content.res.ColorStateList.valueOf(Color.parseColor("#3E4D44")) : android.content.res.ColorStateList.valueOf(Color.parseColor("#BDBDBD")));

            ivRadioATM.setImageResource(atm ? R.drawable.ic_circle_checked : R.drawable.ic_circle);
            ivRadioATM.setImageTintList(atm ? android.content.res.ColorStateList.valueOf(Color.parseColor("#3E4D44")) : android.content.res.ColorStateList.valueOf(Color.parseColor("#BDBDBD")));

            ivRadioMoMo.setImageResource(momo ? R.drawable.ic_circle_checked : R.drawable.ic_circle);
            ivRadioMoMo.setImageTintList(momo ? android.content.res.ColorStateList.valueOf(Color.parseColor("#3E4D44")) : android.content.res.ColorStateList.valueOf(Color.parseColor("#BDBDBD")));

            ivRadioVNPay.setImageResource(vnpay ? R.drawable.ic_circle_checked : R.drawable.ic_circle);
            ivRadioVNPay.setImageTintList(vnpay ? android.content.res.ColorStateList.valueOf(Color.parseColor("#3E4D44")) : android.content.res.ColorStateList.valueOf(Color.parseColor("#BDBDBD")));
        };

        updateRadios.run();

        llOptionCash.setOnClickListener(v -> { paymentMethod = "Thanh toán tiền mặt khi nhận hàng"; updatePaymentMethodUI(); dialog.dismiss(); });
        llOptionATM.setOnClickListener(v -> { paymentMethod = "Thẻ ATM nội địa/Internet Banking"; updatePaymentMethodUI(); dialog.dismiss(); });
        llOptionMoMo.setOnClickListener(v -> { paymentMethod = "Thanh toán trực tuyến MoMo"; updatePaymentMethodUI(); dialog.dismiss(); });
        llOptionVNPay.setOnClickListener(v -> { paymentMethod = "Thanh toán trực tuyến VNPay"; updatePaymentMethodUI(); dialog.dismiss(); });

        dialog.show();
    }

    private void performCheckout() {
        BuyerInfo buyerInfo = collectAndValidateBuyerInfo();
        if (buyerInfo == null) return;

        long finalTotal = calculateFinalTotal();
        final boolean memberCheckout = isLoggedIn;
        final BuyerInfo guestInfo = buyerInfo;

        binding.btnCheckout.setEnabled(false);

        new Thread(() -> {
            int maxIdNum = 1460;
            try {
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                QuerySnapshot snapshot = com.google.android.gms.tasks.Tasks.await(db.collection("orders").get());
                for (DocumentSnapshot doc : snapshot.getDocuments()) {
                    String idStr = doc.getId();
                    if (idStr.startsWith("ORD-")) {
                        try {
                            int num = Integer.parseInt(idStr.substring(4));
                            if (num < 5000 && num > maxIdNum) maxIdNum = num;
                        } catch (Exception ignored) {}
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to query Firestore for max order code", e);
            }

            int nextNum = maxIdNum + 1;
            String mockOrderCode = "ORD-" + nextNum;

            android.app.Activity activity = getActivity();
            if (activity == null) return;
            activity.runOnUiThread(() -> {
                if (binding != null) {
                    binding.btnCheckout.setEnabled(true);
                }
                if (!isAdded() || binding == null) return;
                if (memberCheckout) {
                    handleMemberCheckout(mockOrderCode, finalTotal);
                } else {
                    handleGuestCheckout(mockOrderCode, finalTotal, guestInfo);
                }
            });
        }).start();
    }

    private BuyerInfo collectAndValidateBuyerInfo() {
        if (isLoggedIn) {
            String email = ProfileSession.getEmail(requireContext());
            if (email.trim().isEmpty()) email = null;
            return new BuyerInfo(recipientName, recipientPhone, email, recipientAddress);
        } else {
            String guestName = binding.etGuestName.getText().toString().trim();
            String guestPhone = binding.etGuestPhone.getText().toString().trim();
            String guestEmail = binding.etGuestEmail.getText().toString().trim();

            if (guestName.isEmpty()) { Toast.makeText(requireContext(), "Vui lòng nhập họ và tên người nhận.", Toast.LENGTH_SHORT).show(); return null; }
            if (guestPhone.isEmpty()) { Toast.makeText(requireContext(), "Vui lòng nhập số điện thoại.", Toast.LENGTH_SHORT).show(); return null; }
            if (guestPhone.length() < 9) { Toast.makeText(requireContext(), "Số điện thoại không hợp lệ.", Toast.LENGTH_SHORT).show(); return null; }
            if (!guestEmail.isEmpty() && !Patterns.EMAIL_ADDRESS.matcher(guestEmail).matches()) {
                Toast.makeText(requireContext(), "Email không đúng định dạng.", Toast.LENGTH_SHORT).show(); return null;
            }

            if ("Nhận tại cửa hàng".equals(deliveryType)) {
                if (selectedStoreId.trim().isEmpty() || "Chưa chọn cửa hàng".equals(selectedStoreName)) {
                    Toast.makeText(requireContext(), "Vui lòng chọn cửa hàng để nhận hàng.", Toast.LENGTH_SHORT).show(); return null;
                }
                recipientName = guestName;
                recipientPhone = guestPhone;
                recipientAddress = selectedStoreAddress;
                return new BuyerInfo(guestName, guestPhone, guestEmail.isEmpty() ? null : guestEmail, selectedStoreAddress);
            } else {
                String guestAddress = binding.etGuestAddress.getText().toString().trim();
                if (guestAddress.isEmpty()) { Toast.makeText(requireContext(), "Vui lòng nhập địa chỉ giao hàng.", Toast.LENGTH_SHORT).show(); return null; }
                recipientName = guestName;
                recipientPhone = guestPhone;
                recipientAddress = guestAddress;
                return new BuyerInfo(guestName, guestPhone, guestEmail.isEmpty() ? null : guestEmail, guestAddress);
            }
        }
    }

    private void handleMemberCheckout(String mockOrderCode, long finalTotal) {
        new Thread(() -> {
            try {
                for (CartItemEntity item : checkoutItems) {
                    database.cartDao().deleteCartItem(item);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to clear cart after checkout", e);
            }
        }).start();

        if ("Thanh toán tiền mặt khi nhận hàng".equals(paymentMethod)) {
            navigateToOrderSuccess(mockOrderCode, finalTotal, paymentMethod, false, false);
        } else if ("Thẻ ATM nội địa/Internet Banking".equals(paymentMethod)) {
            ShopPaymentDialogs.showAtmVnpayQrDialog(this, mockOrderCode, finalTotal, false);
        } else if ("Thanh toán trực tuyến VNPay".equals(paymentMethod)) {
            ShopPaymentDialogs.showAtmVnpayQrDialog(this, mockOrderCode, finalTotal, true);
        } else if ("Thanh toán trực tuyến MoMo".equals(paymentMethod)) {
            ShopPaymentDialogs.showMomoRedirectDialog(this, mockOrderCode, finalTotal);
        } else {
            navigateToOrderSuccess(mockOrderCode, finalTotal, paymentMethod, false, false);
        }
    }

    public void showSuccessBannerAndNavigate(String orderCode, boolean isEmailNotification, Runnable onFinished) {
        if (binding == null) { onFinished.run(); return; }
        String greeting = (recipientName != null && !recipientName.trim().isEmpty()) ? recipientName + " ơi, " : "";
        boolean storePickup = isStorePickup();
        String titleText, messageText;

        if (isEmailNotification) {
            if (storePickup) {
                titleText = "Xác nhận nhận tại cửa hàng qua Email";
                messageText = greeting + "chúng tôi đã nhận được đơn hàng #" + orderCode + ". Xác nhận nhận tại cửa hàng đã được gửi qua email của bạn.";
            } else {
                titleText = "Xác nhận đặt hàng qua Email";
                messageText = greeting + "chúng tôi đã nhận được đơn hàng #" + orderCode + ". Xác nhận đã được gửi qua email của bạn.";
            }
        } else {
            if (storePickup) {
                titleText = "Xác nhận nhận tại cửa hàng qua SMS";
                messageText = greeting + "cảm ơn bạn đã đặt hàng tại Rootie. Mã đơn hàng: #" + orderCode + ". Vui lòng đến cửa hàng đã chọn để nhận hàng.";
            } else {
                titleText = "Xác nhận đặt hàng qua SMS";
                messageText = greeting + "cảm ơn bạn đã đặt hàng tại Rootie. Mã đơn hàng: #" + orderCode + ". Đơn hàng đang được chuẩn bị.";
            }
        }

        binding.tvNotiTitle.setText(titleText);
        binding.tvNotiMessage.setText(messageText);
        binding.cvNotificationBanner.setVisibility(View.VISIBLE);
        binding.cvNotificationBanner.setTranslationY(-300f);

        binding.cvNotificationBanner.animate()
                .translationY(0f)
                .setDuration(1200)
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .withEndAction(() -> {
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                        if (binding != null) {
                            binding.cvNotificationBanner.animate()
                                    .translationY(-300f)
                                    .setDuration(800)
                                    .withEndAction(() -> {
                                        if (binding != null) {
                                            binding.cvNotificationBanner.setVisibility(View.GONE);
                                        }
                                        onFinished.run();
                                    }).start();
                        } else onFinished.run();
                    }, 2500);
                }).start();
    }

    private void handleGuestCheckout(String mockOrderCode, long finalTotal, BuyerInfo buyerInfo) {
        lastGuestBuyerInfo = buyerInfo;
        ProfileSession.setGuestPhone(requireContext(), buyerInfo.phone);
        boolean hasEmail = (buyerInfo.email != null && !buyerInfo.email.trim().isEmpty());

        if ("Thanh toán tiền mặt khi nhận hàng".equals(paymentMethod)) {
            navigateToOrderSuccess(mockOrderCode, finalTotal, paymentMethod, true, hasEmail);
        } else if ("Thẻ ATM nội địa/Internet Banking".equals(paymentMethod)) {
            ShopPaymentDialogs.showAtmVnpayQrDialog(this, mockOrderCode, finalTotal, false);
        } else if ("Thanh toán trực tuyến VNPay".equals(paymentMethod)) {
            ShopPaymentDialogs.showAtmVnpayQrDialog(this, mockOrderCode, finalTotal, true);
        } else if ("Thanh toán trực tuyến MoMo".equals(paymentMethod)) {
            ShopPaymentDialogs.showMomoRedirectDialog(this, mockOrderCode, finalTotal);
        } else {
            navigateToOrderSuccess(mockOrderCode, finalTotal, paymentMethod, true, hasEmail);
        }
    }

    private void navigateToOrderSuccess(String orderCode, long totalAmount, String method, boolean isGuest, boolean isEmailNotification) {
        OrderEntity persistedOrder = buildOrderEntitySnapshot(orderCode, totalAmount, method, isGuest);
        persistOrderSync(persistedOrder);

        String notificationType = isEmailNotification ? "email" : "sms";

        if (isGuest) {
            PushNotiDialog.show(
                    requireContext(),
                    orderCode,
                    recipientName,
                    isEmailNotification,
                    () -> commitSuccessNavigation(orderCode, totalAmount, method, isGuest, notificationType),
                    () -> commitSuccessNavigation(orderCode, totalAmount, method, isGuest, notificationType),
                    this
            );
        } else {
            commitSuccessNavigation(orderCode, totalAmount, method, isGuest, notificationType);
        }
    }

    private OrderEntity buildOrderEntitySnapshot(String orderCode, long totalAmount, String method, boolean isGuest) {
        String finalUserId = isLoggedIn ? com.veganbeauty.app.utils.ProfileSessionHelper.getEffectiveUserId(requireContext()) : "";
        if (finalUserId == null) finalUserId = "";
        
        List<OrderItem> orderItems = new ArrayList<>();
        long subTotal = 0;
        for (CartItemEntity ci : checkoutItems) {
            orderItems.add(new OrderItem(ci.getId(), ci.getName(), ci.getImage(), ci.getQuantity(), ci.getPrice()));
            subTotal += ci.getPrice() * ci.getQuantity();
        }
        long now = System.currentTimeMillis();
        String nowDate = new SimpleDateFormat("dd/MM/yyyy", new Locale("vi", "VN")).format(new Date(now));
        String nowTime = new SimpleDateFormat("HH:mm", new Locale("vi", "VN")).format(new Date(now));

        String billingEmail = isGuest ? binding.etGuestEmail.getText().toString().trim() : ProfileSession.getEmail(requireContext());
        if (billingEmail == null || billingEmail.trim().isEmpty()) billingEmail = "";

        String safeRecipientName = recipientName != null ? recipientName : "";
        String safeRecipientPhone = recipientPhone != null ? recipientPhone : "";
        String safeRecipientAddress = recipientAddress != null ? recipientAddress : "";

        OrderEntity order = new OrderEntity(
                orderCode, nowDate, nowTime, "Chờ xác nhận", totalAmount, subTotal, orderItems,
                finalUserId, isGuest, safeRecipientName, safeRecipientPhone, safeRecipientAddress, 0L,
                voucherDiscountAmount, method, "", false, 0, "", "", false, false,
                safeRecipientName, safeRecipientPhone, billingEmail
        );
        order.setDeliveryDate("");
        order.setOrderNote(binding.etNote.getText().toString().trim());
        AffiliateTrackingHelper.applyAffiliateAttribution(requireContext(), order, checkoutItems);
        return order;
    }

    private void persistOrderSync(OrderEntity order) {
        final android.content.Context appContext = requireContext().getApplicationContext();
        new Thread(() -> {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            try {
                database.orderDao().insertOrder(order);
                
                // Update local product stock and remote stock in Firestore using atomic decrement
                FirestoreService firestore = new FirestoreService();
                for (OrderItem item : order.getItems()) {
                    // Use atomic decrement for Firestore to avoid race conditions
                    try {
                        boolean success = firestore.decrementProductStock(item.getProductId(), item.getQuantity());
                        if (success) {
                            Log.d(TAG, "Atomic stock decrement for " + item.getProductId() + " by " + item.getQuantity());
                            // Fetch updated product from Firestore to sync local DB
                            ProductEntity updatedProduct = firestore.fetchProductById(item.getProductId());
                            if (updatedProduct != null) {
                                database.productDao().insertProducts(java.util.Collections.singletonList(updatedProduct));
                            }
                        } else {
                            Log.w(TAG, "Atomic decrement failed for " + item.getProductId() + ", trying fallback");
                            // Fallback: manual fetch and update
                            ProductEntity product = firestore.fetchProductById(item.getProductId());
                            if (product == null) {
                                product = database.productDao().getProductById(item.getProductId());
                            }
                            if (product != null) {
                                int newStock = Math.max(0, product.getStock() - item.getQuantity());
                                product.setStock(newStock);
                                database.productDao().insertProducts(java.util.Collections.singletonList(product));
                                firestore.updateProductStock(item.getProductId(), newStock);
                            }
                        }
                    } catch (Exception fe) {
                        Log.e(TAG, "Error during stock decrement for " + item.getProductId(), fe);
                        // Fallback: update local only
                        ProductEntity product = database.productDao().getProductById(item.getProductId());
                        if (product != null) {
                            int newStock = Math.max(0, product.getStock() - item.getQuantity());
                            product.setStock(newStock);
                            database.productDao().insertProducts(java.util.Collections.singletonList(product));
                        }
                    }
                }
                
                AffiliateTrackingHelper.recordAffiliateSideEffects(appContext, order);
                AffiliateTrackingHelper.clearAttributionForItems(appContext, checkoutItems);
            } catch (Exception e) {
                Log.e(TAG, "persistOrderSync local DB write failed", e);
            }

            try {
                Map<String, Object> orderMap = new Gson().fromJson(new Gson().toJsonTree(order), Map.class);
                com.google.android.gms.tasks.Tasks.await(db.collection("orders").document(order.getId()).set(orderMap));

                if (order.getUserId() != null && !order.getUserId().trim().isEmpty()) {
                    new com.veganbeauty.app.data.remote.FirestoreService().sendCommunityNotificationEvent(
                            order.getUserId(),
                            "rootie_system",
                            "Rootie System",
                            "",
                            "ORDER_SUCCESS",
                            "CREATE",
                            "Đơn hàng #" + order.getId() + " của bạn đã được tạo thành công.",
                            order.getId(),
                            null
                    );
                }

                Map<String, Object> newNotification = new HashMap<>();
                newNotification.put("title", "Có đơn hàng mới! \uD83D\uDED2");
                newNotification.put("message", "Khách hàng " + order.getShippingName() + " vừa đặt đơn hàng trị giá " + String.format(Locale.US, "%,d", order.getTotalAmount()) + "đ.");
                newNotification.put("type", "NEW_ORDER");
                newNotification.put("isRead", false);
                newNotification.put("createdAt", System.currentTimeMillis());
                newNotification.put("orderId", order.getId());
                com.google.android.gms.tasks.Tasks.await(db.collection("notification_admin").add(newNotification));
                Log.d(TAG, "Firebase sync success for order " + order.getId());
            } catch (Exception e) {
                Log.e(TAG, "Firebase sync failed", e);
            }
        }).start();
    }

    public void persistOrderFromDialog(String orderCode, long totalAmount, String method, boolean isGuest, boolean isEmailNotification) {
        OrderEntity snapshot = buildOrderEntitySnapshot(orderCode, totalAmount, method, isGuest);
        persistOrderSync(snapshot);

        String notificationType = isEmailNotification ? "email" : "sms";

        if (isGuest) {
            PushNotiDialog.show(
                    requireContext(),
                    orderCode,
                    recipientName,
                    isEmailNotification,
                    () -> commitSuccessNavigation(orderCode, totalAmount, method, isGuest, notificationType),
                    () -> commitSuccessNavigation(orderCode, totalAmount, method, isGuest, notificationType),
                    this
            );
        } else {
            commitSuccessNavigation(orderCode, totalAmount, method, isGuest, notificationType);
        }
    }

    public boolean hasGuestEmail() {
        if (!isLoggedIn) {
            String email = binding.etGuestEmail.getText().toString().trim();
            return !email.isEmpty();
        }
        return false;
    }

    private void commitSuccessNavigation(String orderCode, long totalAmount, String method, boolean isGuest, String notificationType) {
        ShopOrderSuccessFragment successFragment = ShopOrderSuccessFragment.newInstance(
                orderCode, totalAmount, method, isGuest, notificationType,
                "Nhận tại cửa hàng".equals(deliveryType), selectedStoreName, selectedStoreAddress, recipientName
        );
        try { getParentFragmentManager().popBackStack(); } catch (Exception ignored) {}
        getParentFragmentManager().beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.main_container, successFragment)
                .commit();
    }

    public boolean isStorePickup() { return "Nhận tại cửa hàng".equals(deliveryType); }
    public String getSelectedStoreName() { return selectedStoreName; }
    public String getSelectedStoreAddress() { return selectedStoreAddress; }

    private long calculateFinalTotal() {
        long finalPriceSum = 0;
        for (CartItemEntity ci : checkoutItems) finalPriceSum += ci.getPrice() * ci.getQuantity();
        long pointsDiscount = (isPointsChecked && finalPriceSum > 2400) ? 2400L : 0L;
        finalPriceSum -= pointsDiscount;
        if (isVoucherApplied && finalPriceSum > voucherDiscountAmount) finalPriceSum -= voucherDiscountAmount;
        return finalPriceSum;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void updateSwitchUI(FrameLayout container, ImageView thumb, boolean enabled) {
        if (enabled) {
            container.setBackgroundResource(R.drawable.ic_switch_track_on);
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) thumb.getLayoutParams();
            lp.gravity = Gravity.CENTER_VERTICAL | Gravity.END;
            lp.setMarginStart(0);
            lp.setMarginEnd((int) (2 * getResources().getDisplayMetrics().density));
            thumb.setLayoutParams(lp);
        } else {
            container.setBackgroundResource(R.drawable.ic_switch_track_off);
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) thumb.getLayoutParams();
            lp.gravity = Gravity.CENTER_VERTICAL | Gravity.START;
            lp.setMarginEnd(0);
            lp.setMarginStart((int) (2 * getResources().getDisplayMetrics().density));
            thumb.setLayoutParams(lp);
        }
    }
}
