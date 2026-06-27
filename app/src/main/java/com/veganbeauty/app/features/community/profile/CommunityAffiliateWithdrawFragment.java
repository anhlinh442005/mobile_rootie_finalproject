package com.veganbeauty.app.features.community.profile;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.veganbeauty.app.R;
import com.veganbeauty.app.data.local.LocalJsonReader;
import com.veganbeauty.app.data.local.entities.OrderEntity;
import com.veganbeauty.app.features.community.affiliate.AffiliateBankAccount;
import com.veganbeauty.app.features.community.affiliate.AffiliateBankAccountAdapter;
import com.veganbeauty.app.features.community.affiliate.AffiliateHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CommunityAffiliateWithdrawFragment extends Fragment {

    private long availableBalance = 0L;
    private List<AffiliateBankAccount> bankAccounts = new ArrayList<>();
    private AffiliateBankAccountAdapter adapter = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.com_fragment_affiliate_withdraw, container, false);

        LinearLayout navOverview = view.findViewById(R.id.navOverview);
        if (navOverview != null) {
            navOverview.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        }

        LinearLayout navOrders = view.findViewById(R.id.navOrders);
        if (navOrders != null) {
            navOrders.setOnClickListener(v -> getParentFragmentManager().beginTransaction().replace(R.id.main_container, new CommunityAffiliateOrdersFragment()).commit());
        }

        LinearLayout navProducts = view.findViewById(R.id.navProducts);
        if (navProducts != null) {
            navProducts.setOnClickListener(v -> getParentFragmentManager().beginTransaction().replace(R.id.main_container, new CommunityAffiliateProductsFragment()).commit());
        }

        LinearLayout navWithdraw = view.findViewById(R.id.navWithdraw);
        if (navWithdraw != null) {
            navWithdraw.setOnClickListener(v -> getParentFragmentManager().beginTransaction().replace(R.id.main_container, new CommunityAffiliateWithdrawFragment()).commit());
        }

        LinearLayout llTotalWithdrawn = view.findViewById(R.id.llTotalWithdrawn);
        if (llTotalWithdrawn != null) {
            llTotalWithdrawn.setOnClickListener(v -> showHistoryDialog());
        }

        View tvGuideWithdraw = view.findViewById(R.id.tvGuideWithdraw);
        if (tvGuideWithdraw != null) {
            tvGuideWithdraw.setOnClickListener(v -> showGuideDialog());
        }

        loadWithdrawData(view);

        return view;
    }

    private void loadWithdrawData(View view) {
        try {
            DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("vi", "VN"));
            symbols.setGroupingSeparator('.');
            DecimalFormat format = new DecimalFormat("#,###đ", symbols);
            
            JSONArray jsonArray = AffiliateHelper.INSTANCE.getAffiliateData(requireContext());
            if (jsonArray.length() == 0) return;

            JSONObject data = jsonArray.getJSONObject(0);

            LocalJsonReader jsonReader = new LocalJsonReader(requireContext());
            List<OrderEntity> allOrders = jsonReader.getAllOrders();
            String currentUserId = "test_001"; // Or get from session
            
            long successCommission = 0L;
            long pendingCommission = 0L;

            for (OrderEntity order : allOrders) {
                if (order.isAffiliate() && order.getAffiliate() != null && currentUserId.equals(order.getAffiliate().getReferrerUserId())) {
                    long commission = order.getAffiliate().getCommissionAmount();
                    if ("confirmed".equals(order.getAffiliate().getCommissionStatus())) {
                        successCommission += commission;
                    } else if ("pending".equals(order.getAffiliate().getCommissionStatus())) {
                        pendingCommission += commission;
                    }
                }
            }

            long totalWithdrawn = 0L;
            JSONArray withdrawals = data.optJSONArray("withdrawals");
            if (withdrawals != null) {
                for (int i = 0; i < withdrawals.length(); i++) {
                    JSONObject wd = withdrawals.getJSONObject(i);
                    long amt = wd.optLong("amount", 0);
                    if ("Đã chuyển".equals(wd.optString("status"))) {
                        totalWithdrawn += amt;
                    }
                }
            }
            
            availableBalance = Math.max(0L, successCommission - totalWithdrawn);

            TextView tvAvailableBalance = view.findViewById(R.id.tvAvailableBalance);
            if (tvAvailableBalance != null) tvAvailableBalance.setText(format.format(availableBalance));

            TextView tvTotalWithdrawn = view.findViewById(R.id.tvTotalWithdrawn);
            if (tvTotalWithdrawn != null) tvTotalWithdrawn.setText(format.format(totalWithdrawn));

            TextView tvPendingAmount = view.findViewById(R.id.tvPendingAmount);
            if (tvPendingAmount != null) tvPendingAmount.setText(format.format(pendingCommission));

            setupBankAccounts(view);
            setupWithdrawInput(view);

            View btnAddBank = view.findViewById(R.id.btnAddBank);
            if (btnAddBank != null) {
                btnAddBank.setOnClickListener(v -> showAddBankDialog());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupBankAccounts(View view) {
        RecyclerView rvBankAccounts = view.findViewById(R.id.rvBankAccounts);
        if (rvBankAccounts == null) return;

        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(requireContext().getAssets().open("affiliate_bankaccount.json")));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            
            JSONArray jsonArray = new JSONArray(sb.toString());
            bankAccounts.clear();
            if (jsonArray.length() > 0) {
                JSONObject userObj = jsonArray.getJSONObject(0);
                JSONArray bankArr = userObj.optJSONArray("bank_accounts");
                if (bankArr == null) bankArr = new JSONArray();
                for (int i = 0; i < bankArr.length(); i++) {
                    JSONObject obj = bankArr.getJSONObject(i);
                    AffiliateBankAccount acc = new AffiliateBankAccount(
                            obj.getInt("id"),
                            obj.getString("bank_name"),
                            obj.getString("account_number"),
                            obj.getString("account_holder"),
                            obj.getString("logo"),
                            obj.getBoolean("is_default")
                    );
                    bankAccounts.add(acc);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        adapter = new AffiliateBankAccountAdapter(requireContext(), bankAccounts, selectedAcc -> {
            for (AffiliateBankAccount acc : bankAccounts) {
                acc.setDefault(false);
            }
            selectedAcc.setDefault(true);
            if (adapter != null) adapter.notifyDataSetChanged();
            return kotlin.Unit.INSTANCE;
        });

        rvBankAccounts.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvBankAccounts.setAdapter(adapter);
    }

    private void setupWithdrawInput(View view) {
        EditText etAmount = view.findViewById(R.id.etWithdrawAmount);
        if (etAmount == null) return;

        TextView tvWarning = view.findViewById(R.id.tvWarningMessage);
        TextView tvReceive = view.findViewById(R.id.tvReceiveAmountValue);
        TextView btnSubmit = view.findViewById(R.id.btnSubmitWithdraw);

        TextView tv100k = view.findViewById(R.id.tvAmount100k);
        TextView tv200k = view.findViewById(R.id.tvAmount200k);
        TextView tv500k = view.findViewById(R.id.tvAmount500k);
        TextView tvAll = view.findViewById(R.id.tvAmountAll);

        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("vi", "VN"));
        symbols.setGroupingSeparator('.');
        DecimalFormat format = new DecimalFormat("#,###", symbols);

        if (tv100k != null) tv100k.setOnClickListener(v -> etAmount.setText("100.000"));
        if (tv200k != null) tv200k.setOnClickListener(v -> etAmount.setText("200.000"));
        if (tv500k != null) tv500k.setOnClickListener(v -> etAmount.setText("500.000"));
        if (tvAll != null) tvAll.setOnClickListener(v -> etAmount.setText(format.format(availableBalance)));

        etAmount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                etAmount.removeTextChangedListener(this);
                try {
                    String cleanString = s.toString().replace(".", "");
                    if (!cleanString.isEmpty()) {
                        long parsed = Long.parseLong(cleanString);
                        String formatted = format.format(parsed);
                        etAmount.setText(formatted);
                        etAmount.setSelection(formatted.length());

                        if (tvReceive != null) tvReceive.setText(formatted + "đ");

                        if (parsed > availableBalance) {
                            if (tvWarning != null) tvWarning.setVisibility(View.VISIBLE);
                            if (btnSubmit != null) {
                                btnSubmit.setEnabled(false);
                                btnSubmit.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#9E9E9E")));
                            }
                        } else {
                            if (tvWarning != null) tvWarning.setVisibility(View.GONE);
                            if (btnSubmit != null) {
                                btnSubmit.setEnabled(true);
                                btnSubmit.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#56694E")));
                            }
                        }
                    } else {
                        if (tvReceive != null) tvReceive.setText("0đ");
                        if (tvWarning != null) tvWarning.setVisibility(View.GONE);
                        if (btnSubmit != null) {
                            btnSubmit.setEnabled(false);
                            btnSubmit.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#9E9E9E")));
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                etAmount.addTextChangedListener(this);
            }
        });
    }

    private void showAddBankDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_bank_account, null);
        AlertDialog dialog = new AlertDialog.Builder(requireContext(), R.style.CustomDialogTheme)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        EditText inputBank = dialogView.findViewById(R.id.etBankName);
        EditText inputName = dialogView.findViewById(R.id.etAccountHolder);
        EditText inputNumber = dialogView.findViewById(R.id.etAccountNumber);

        View btnCancel = dialogView.findViewById(R.id.btnCancel);
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }

        View btnAdd = dialogView.findViewById(R.id.btnAdd);
        if (btnAdd != null) {
            btnAdd.setOnClickListener(v -> {
                int newId = 1;
                for (AffiliateBankAccount acc : bankAccounts) {
                    if (acc.getId() >= newId) {
                        newId = acc.getId() + 1;
                    }
                }
                AffiliateBankAccount newAccount = new AffiliateBankAccount(
                        newId,
                        inputBank.getText().toString(),
                        inputNumber.getText().toString(),
                        inputName.getText().toString(),
                        "ic_wallet",
                        bankAccounts.isEmpty()
                );
                bankAccounts.add(newAccount);
                if (adapter != null) adapter.notifyDataSetChanged();
                dialog.dismiss();
            });
        }

        dialog.show();
    }

    private void showGuideDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_withdrawal_guide, null);
        AlertDialog dialog = new AlertDialog.Builder(requireContext(), R.style.CustomDialogTheme)
                .setView(dialogView)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        View btnClose = dialogView.findViewById(R.id.btnClose);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }
        dialog.show();
    }

    private void showHistoryDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_withdrawal_history, null);
        AlertDialog dialog = new AlertDialog.Builder(requireContext(), R.style.CustomDialogTheme)
                .setView(dialogView)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        View btnClose = dialogView.findViewById(R.id.btnClose);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        LinearLayout llContainer = dialogView.findViewById(R.id.llWithdrawalsContainer);
        try {
            JSONArray jsonArray = AffiliateHelper.INSTANCE.getAffiliateData(requireContext());
            if (jsonArray.length() > 0) {
                JSONObject data = jsonArray.getJSONObject(0);
                JSONArray withdrawals = data.optJSONArray("withdrawals");
                if (withdrawals != null && llContainer != null) {
                    DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("vi", "VN"));
                    symbols.setGroupingSeparator('.');
                    DecimalFormat format = new DecimalFormat("#,###đ", symbols);
                    for (int i = 0; i < withdrawals.length(); i++) {
                        JSONObject wd = withdrawals.optJSONObject(i);
                        if (wd == null) continue;
                        
                        View wdView = LayoutInflater.from(requireContext()).inflate(R.layout.com_item_revenue_withdrawal, llContainer, false);
                        
                        TextView tvWithdrawDate = wdView.findViewById(R.id.tvWithdrawDate);
                        if (tvWithdrawDate != null) tvWithdrawDate.setText(wd.optString("date"));
                        
                        TextView tvWithdrawAmount = wdView.findViewById(R.id.tvWithdrawAmount);
                        if (tvWithdrawAmount != null) tvWithdrawAmount.setText(format.format(wd.optLong("amount")));
                        
                        TextView tvStatus = wdView.findViewById(R.id.tvWithdrawStatus);
                        if (tvStatus != null) {
                            String status = wd.optString("status");
                            if ("Đã chuyển".equals(status)) {
                                tvStatus.setText("Đã chuyển");
                                tvStatus.setTextColor(Color.parseColor("#6E846A"));
                                tvStatus.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#EAF1E7")));
                            } else {
                                tvStatus.setText(status);
                                tvStatus.setTextColor(Color.parseColor("#FF9800"));
                                tvStatus.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFF3E0")));
                            }
                        }
                        llContainer.addView(wdView);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        dialog.show();
    }
}
