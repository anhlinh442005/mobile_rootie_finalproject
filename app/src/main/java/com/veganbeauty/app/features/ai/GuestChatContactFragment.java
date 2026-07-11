package com.veganbeauty.app.features.ai;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.FirebaseFirestore;
import com.veganbeauty.app.R;
import com.veganbeauty.app.data.local.ProfileSession;
import com.veganbeauty.app.features.home.welcome.HomeWelcomeActivity;

import java.util.HashMap;
import java.util.Map;

/**
 * Guest (not logged in) cannot chat. Shows contact form only:
 * name + phone and/or email so Rootie can reach them without creating an account.
 */
public class GuestChatContactFragment extends Fragment {

    private EditText etName;
    private EditText etPhone;
    private EditText etEmail;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_guest_chat_contact, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etName = view.findViewById(R.id.etGuestContactName);
        etPhone = view.findViewById(R.id.etGuestContactPhone);
        etEmail = view.findViewById(R.id.etGuestContactEmail);

        etName.setText(ProfileSession.getGuestName(requireContext()));
        etPhone.setText(ProfileSession.getGuestPhone(requireContext()));
        etEmail.setText(ProfileSession.getGuestEmail(requireContext()));

        view.findViewById(R.id.btnSubmitGuestContact).setOnClickListener(v -> submitContact());
        view.findViewById(R.id.btnGuestLoginInstead).setOnClickListener(v -> openLogin());
    }

    private void submitContact() {
        String name = etName.getText() != null ? etName.getText().toString().trim() : "";
        String phone = etPhone.getText() != null ? etPhone.getText().toString().trim() : "";
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";

        if (TextUtils.isEmpty(name)) {
            etName.setError("Vui lòng nhập họ và tên");
            etName.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(phone) && TextUtils.isEmpty(email)) {
            Toast.makeText(requireContext(),
                    "Vui lòng nhập số điện thoại hoặc email", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!TextUtils.isEmpty(phone) && phone.length() < 9) {
            etPhone.setError("Số điện thoại không hợp lệ");
            etPhone.requestFocus();
            return;
        }
        if (!TextUtils.isEmpty(email) && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Email không hợp lệ");
            etEmail.requestFocus();
            return;
        }

        ProfileSession.setGuestName(requireContext(), name);
        ProfileSession.setGuestPhone(requireContext(), phone);
        ProfileSession.setGuestEmail(requireContext(), email);

        pushContactRequest(name, phone, email);

        Toast.makeText(requireContext(),
                "Đã gửi thông tin. Rootie sẽ liên hệ sớm nhất có thể!",
                Toast.LENGTH_LONG).show();

        dismissParentDialog();
    }

    private void pushContactRequest(String name, String phone, String email) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("name", name);
            data.put("phone", phone);
            data.put("email", email);
            data.put("source", "guest_chat_popup");
            data.put("createdAt", System.currentTimeMillis());
            FirebaseFirestore.getInstance()
                    .collection("guest_contact_requests")
                    .add(data);
        } catch (Exception ignored) {
            // Local save already done; Firestore is best-effort.
        }
    }

    private void openLogin() {
        Intent intent = new Intent(requireContext(), HomeWelcomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        dismissParentDialog();
    }

    private void dismissParentDialog() {
        if (getParentFragment() instanceof DialogFragment) {
            ((DialogFragment) getParentFragment()).dismissAllowingStateLoss();
        }
    }
}
