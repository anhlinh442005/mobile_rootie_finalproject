package com.veganbeauty.app.features.community.profile;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.veganbeauty.app.R;

import java.util.List;

public class AffiliateBankAccountAdapter extends RecyclerView.Adapter<AffiliateBankAccountAdapter.ViewHolder> {

    public interface OnAccountClickListener {
        void onAccountClick(AffiliateBankAccount account);
    }

    private final Context context;
    private final List<AffiliateBankAccount> accounts;
    private final OnAccountClickListener onAccountClick;

    public AffiliateBankAccountAdapter(Context context, List<AffiliateBankAccount> accounts, OnAccountClickListener onAccountClick) {
        this.context = context;
        this.accounts = accounts;
        this.onAccountClick = onAccountClick;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final ImageView ivRadio;
        public final ImageView ivBankLogo;
        public final TextView tvBankName;
        public final TextView tvAccountNumber;
        public final TextView tvAccountHolder;
        public final TextView tvDefaultBadge;
        public final View rootView;

        public ViewHolder(View view) {
            super(view);
            ivRadio = view.findViewById(R.id.ivRadio);
            ivBankLogo = view.findViewById(R.id.ivBankLogo);
            tvBankName = view.findViewById(R.id.tvBankName);
            tvAccountNumber = view.findViewById(R.id.tvAccountNumber);
            tvAccountHolder = view.findViewById(R.id.tvAccountHolder);
            tvDefaultBadge = view.findViewById(R.id.tvDefaultBadge);
            rootView = view;
        }

        public void bind(AffiliateBankAccount account) {
            tvBankName.setText(account.getBankName());

            String maskedNumber = account.getAccountNumber().length() > 4 
                    ? "**** " + account.getAccountNumber().substring(account.getAccountNumber().length() - 4) 
                    : account.getAccountNumber();
            tvAccountNumber.setText(maskedNumber);

            tvAccountHolder.setText(account.getAccountHolder());

            if (account.isDefault()) {
                ivRadio.setImageResource(R.drawable.ic_radio_checked);
                tvDefaultBadge.setVisibility(View.VISIBLE);
            } else {
                ivRadio.setImageResource(R.drawable.ic_radio_unchecked);
                tvDefaultBadge.setVisibility(View.GONE);
            }

            int resId = context.getResources().getIdentifier(account.getLogo(), "drawable", context.getPackageName());
            if (resId != 0) {
                ivBankLogo.setImageResource(resId);
            } else {
                ivBankLogo.setImageResource(R.drawable.ic_wallet);
            }

            rootView.setOnClickListener(v -> onAccountClick.onAccountClick(account));
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.com_item_bank_account, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(accounts.get(position));
    }

    @Override
    public int getItemCount() {
        return accounts.size();
    }
}
