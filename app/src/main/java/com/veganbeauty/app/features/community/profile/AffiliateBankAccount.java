package com.veganbeauty.app.features.community.profile;

import androidx.annotation.NonNull;
import java.util.Objects;

public class AffiliateBankAccount {
    private int id;
    @NonNull
    private String bankName;
    @NonNull
    private String accountNumber;
    @NonNull
    private String accountHolder;
    @NonNull
    private String logo;
    private boolean isDefault;

    public AffiliateBankAccount(int id, @NonNull String bankName, @NonNull String accountNumber, @NonNull String accountHolder, @NonNull String logo, boolean isDefault) {
        this.id = id;
        this.bankName = bankName;
        this.accountNumber = accountNumber;
        this.accountHolder = accountHolder;
        this.logo = logo;
        this.isDefault = isDefault;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    @NonNull
    public String getBankName() { return bankName; }
    public void setBankName(@NonNull String bankName) { this.bankName = bankName; }

    @NonNull
    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(@NonNull String accountNumber) { this.accountNumber = accountNumber; }

    @NonNull
    public String getAccountHolder() { return accountHolder; }
    public void setAccountHolder(@NonNull String accountHolder) { this.accountHolder = accountHolder; }

    @NonNull
    public String getLogo() { return logo; }
    public void setLogo(@NonNull String logo) { this.logo = logo; }

    public boolean isDefault() { return isDefault; }
    public void setDefault(boolean aDefault) { isDefault = aDefault; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AffiliateBankAccount that = (AffiliateBankAccount) o;
        return id == that.id && isDefault == that.isDefault && bankName.equals(that.bankName) && accountNumber.equals(that.accountNumber) && accountHolder.equals(that.accountHolder) && logo.equals(that.logo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, bankName, accountNumber, accountHolder, logo, isDefault);
    }

    @Override
    public String toString() {
        return "AffiliateBankAccount{" + "id=" + id + ", bankName='" + bankName + '\'' + ", accountNumber='" + accountNumber + '\'' + ", accountHolder='" + accountHolder + '\'' + ", logo='" + logo + '\'' + ", isDefault=" + isDefault + '}';
    }
}
