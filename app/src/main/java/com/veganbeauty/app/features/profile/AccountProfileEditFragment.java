package com.veganbeauty.app.features.profile;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.text.Editable;
import android.text.TextWatcher;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointBackward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.veganbeauty.app.R;
import com.veganbeauty.app.core.base.RootieFragment;
import com.veganbeauty.app.data.local.ProfileSession;
import com.veganbeauty.app.databinding.AccountProfileEditBinding;
import com.veganbeauty.app.features.home.BottomNavHelper;
import com.veganbeauty.app.utils.AvatarLoader;
import com.veganbeauty.app.utils.NavAppUtils;
import com.veganbeauty.app.utils.ProfileSessionHelper;
import com.veganbeauty.app.utils.ProfileUpdateNotifier;
import com.veganbeauty.app.utils.SyncDataHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.Locale;

public class AccountProfileEditFragment extends RootieFragment {

    private AccountProfileEditBinding binding;
    private boolean isSavingProfile;
    private boolean hasUnsavedChanges;
    private boolean avatarUploadFinished;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    @Nullable
    private Runnable avatarUploadTimeoutRunnable;

    private final ProfileUpdateNotifier.Listener profileUpdateListener = () -> {
        if (binding != null && isAdded() && !hasUnsavedChanges) {
            reloadProfileFields(requireContext());
        }
    };

    private final ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    try {
                        InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
                        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                        if (inputStream != null) inputStream.close();
                        if (bitmap != null) {
                            showCropperDialog(bitmap);
                        } else {
                            Toast.makeText(getContext(), "Không thể đọc ảnh chọn", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(getContext(), "Lỗi khi tải ảnh từ thư viện", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    private final ActivityResultLauncher<Void> takePhotoLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicturePreview(), bitmap -> {
                if (bitmap != null) {
                    showCropperDialog(bitmap);
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = AccountProfileEditBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void setupUI(@NonNull View view) {
        Context ctx = requireContext();
        ProfileSessionHelper.restoreLocalAvatarIfPresent(ctx);
        loadAvatarImage(ProfileSessionHelper.getAccountProfileAvatarUrl(ctx));

        reloadProfileFields(ctx);

        TextWatcher unsavedWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                hasUnsavedChanges = true;
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        binding.etFullname.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                hasUnsavedChanges = true;
                binding.tvDisplayName.setText(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        binding.etEmail.addTextChangedListener(unsavedWatcher);
        binding.etUsername.addTextChangedListener(unsavedWatcher);
        binding.etPhone.addTextChangedListener(unsavedWatcher);

        binding.rgGender.setOnCheckedChangeListener((group, checkedId) -> hasUnsavedChanges = true);

        View.OnFocusChangeListener focusListener = (v, hasFocus) -> {
            View lineView = null;
            if (v.getId() == R.id.et_email) lineView = binding.viewEmailLine;
            else if (v.getId() == R.id.et_username) lineView = binding.viewUsernameLine;
            else if (v.getId() == R.id.et_fullname) lineView = binding.viewFullnameLine;
            else if (v.getId() == R.id.et_phone) lineView = binding.viewPhoneLine;

            if (lineView != null) {
                if (hasFocus) {
                    lineView.setBackgroundColor(Color.parseColor("#3E4D44"));
                    lineView.getLayoutParams().height = (int) (2 * getResources().getDisplayMetrics().density);
                } else {
                    lineView.setBackgroundColor(Color.parseColor("#E2E4E1"));
                    lineView.getLayoutParams().height = (int) (1 * getResources().getDisplayMetrics().density);
                }
                lineView.requestLayout();
            }
        };

        binding.etEmail.setOnFocusChangeListener(focusListener);
        binding.etUsername.setOnFocusChangeListener(focusListener);
        binding.etFullname.setOnFocusChangeListener(focusListener);
        binding.etPhone.setOnFocusChangeListener(focusListener);

        View bottomNav = view.findViewById(R.id.bottom_nav);
        if (bottomNav != null) {
            view.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
                Rect rect = new Rect();
                view.getWindowVisibleDisplayFrame(rect);
                int screenHeight = view.getRootView().getHeight();
                int keypadHeight = screenHeight - rect.bottom;
                if (keypadHeight > screenHeight * 0.15) {
                    bottomNav.setVisibility(View.GONE);
                } else {
                    bottomNav.setVisibility(View.VISIBLE);
                }
            });
        }

        binding.btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        binding.btnSave.setOnClickListener(v -> saveProfile());

        NavAppUtils.setupNavApp(this, view, R.id.nav_account);
        BottomNavHelper.highlightTab(view, R.id.nav_account);

        ProfileUpdateNotifier.addListener(profileUpdateListener);

        binding.btnChangeAvatar.setOnClickListener(v -> showAvatarSourcePicker());

        binding.btnSelectDob.setOnClickListener(v -> {
            String currentDob = binding.tvDob.getText().toString();
            String[] parts = currentDob.split("/");
            Long initialSelectionMs = null;
            if (parts.length == 3) {
                try {
                    Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                    cal.clear();
                    cal.set(Calendar.DAY_OF_MONTH, Integer.parseInt(parts[0]));
                    cal.set(Calendar.MONTH, Integer.parseInt(parts[1]) - 1);
                    cal.set(Calendar.YEAR, Integer.parseInt(parts[2]));
                    initialSelectionMs = cal.getTimeInMillis();
                } catch (Exception e) {}
            }

            MaterialDatePicker.Builder<Long> builder = MaterialDatePicker.Builder.datePicker();
            builder.setTitleText("Chọn ngày sinh");
            if (initialSelectionMs != null) {
                builder.setSelection(initialSelectionMs);
            }

            CalendarConstraints.Builder constraintsBuilder = new CalendarConstraints.Builder();
            constraintsBuilder.setValidator(DateValidatorPointBackward.now());
            builder.setCalendarConstraints(constraintsBuilder.build());

            MaterialDatePicker<Long> picker = builder.build();
            picker.addOnPositiveButtonClickListener(selection -> {
                Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                cal.setTimeInMillis(selection);
                int selectedDay = cal.get(Calendar.DAY_OF_MONTH);
                int selectedMonth = cal.get(Calendar.MONTH) + 1;
                int selectedYear = cal.get(Calendar.YEAR);

                String formattedDay = String.format(Locale.getDefault(), "%02d", selectedDay);
                String formattedMonth = String.format(Locale.getDefault(), "%02d", selectedMonth);
                binding.tvDob.setText(formattedDay + "/" + formattedMonth + "/" + selectedYear);
                hasUnsavedChanges = true;
            });
            picker.show(getParentFragmentManager(), "DATE_PICKER");
        });

        binding.btnLinkedAccounts.setOnClickListener(v -> Toast.makeText(ctx, "Quản lý tài khoản liên kết", Toast.LENGTH_SHORT).show());

        binding.btnPersonalInfo.setOnClickListener(v -> getParentFragmentManager().beginTransaction()
                .replace(R.id.main_container, new AccountProfilePersonalInfoFragment())
                .addToBackStack(null)
                .commit());

        binding.btnAccountSettings.setOnClickListener(v -> getParentFragmentManager().beginTransaction()
                .replace(R.id.main_container, new AccountProfileSetupFragment())
                .addToBackStack(null)
                .commit());

        binding.btnChangePassword.setOnClickListener(v -> Toast.makeText(ctx, "Thay đổi mật khẩu", Toast.LENGTH_SHORT).show());
    }

    private void reloadProfileFields(Context ctx) {
        if (binding == null) {
            return;
        }
        String fullName = ProfileSession.getFullName(ctx);
        String username = ProfileSession.getUsername(ctx);
        String email = ProfileSession.getEmail(ctx);
        String phone = ProfileSession.getPhone(ctx);
        String dob = ProfileSession.getDob(ctx);
        String gender = ProfileSession.getGender(ctx);

        binding.tvDisplayName.setText(fullName);
        binding.etEmail.setText(email);
        if (username != null) {
            binding.etUsername.setText(username.startsWith("@") ? username.substring(1) : username);
        }
        binding.etFullname.setText(fullName);
        binding.etPhone.setText(phone);
        binding.tvDob.setText(dob);

        if ("Nam".equals(gender)) {
            binding.rbMale.setChecked(true);
        } else if ("Khác".equals(gender)) {
            binding.rbOther.setChecked(true);
        } else {
            binding.rbFemale.setChecked(true);
        }
    }

    private void saveProfile() {
        if (isSavingProfile || binding == null) {
            return;
        }

        Context saveCtx = requireContext();
        String newFullName = binding.etFullname.getText().toString().trim();
        String newUsernameRaw = binding.etUsername.getText().toString().trim().replace("@", "");
        String newEmail = binding.etEmail.getText().toString().trim();
        String newPhone = binding.etPhone.getText().toString().trim();
        String newDob = binding.tvDob.getText().toString().trim();
        String newGender = "Nữ";
        if (binding.rbMale.isChecked()) {
            newGender = "Nam";
        } else if (binding.rbOther.isChecked()) {
            newGender = "Khác";
        }

        if (newFullName.isEmpty()) {
            Toast.makeText(saveCtx, "Họ và tên không được để trống", Toast.LENGTH_SHORT).show();
            binding.etFullname.requestFocus();
            return;
        }
        if (newUsernameRaw.isEmpty()) {
            Toast.makeText(saveCtx, "Tên người dùng không được để trống", Toast.LENGTH_SHORT).show();
            binding.etUsername.requestFocus();
            return;
        }

        String newUsername = "@" + newUsernameRaw;
        boolean savedLocally = ProfileSession.saveProfileEdits(
                saveCtx,
                newFullName,
                newUsername,
                newEmail,
                newPhone,
                newDob,
                newGender
        );
        if (!savedLocally) {
            Toast.makeText(saveCtx, "Không thể lưu hồ sơ trên thiết bị", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.tvDisplayName.setText(newFullName);
        hasUnsavedChanges = false;
        isSavingProfile = true;
        binding.btnSave.setEnabled(false);
        Toast.makeText(saveCtx, "Đang lưu hồ sơ...", Toast.LENGTH_SHORT).show();

        SyncDataHelper.syncUserProfileToFirebaseAndLocal(saveCtx, (localSuccess, cloudSynced) -> {
            isSavingProfile = false;
            if (binding == null || !isAdded()) {
                return;
            }
            binding.btnSave.setEnabled(true);
            if (!localSuccess) {
                Toast.makeText(saveCtx, "Không thể lưu hồ sơ. Vui lòng thử lại.", Toast.LENGTH_LONG).show();
                return;
            }
            if (!cloudSynced) {
                Toast.makeText(saveCtx, "Đã lưu trên máy. Chưa đồng bộ Firebase — kiểm tra mạng và thử lưu lại.", Toast.LENGTH_LONG).show();
            }
            showSaveSuccessDialog(saveCtx, cloudSynced);
        });
    }

    private void showSaveSuccessDialog(Context saveCtx, boolean cloudSynced) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_save_profile_success, null);
        AlertDialog dialog = new AlertDialog.Builder(saveCtx)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        View btnDismiss = dialogView.findViewById(R.id.btnDialogDismiss);
        if (btnDismiss != null) {
            btnDismiss.setOnClickListener(v1 -> {
                dialog.dismiss();
                getParentFragmentManager().popBackStack();
            });
        }
        dialog.setOnDismissListener(d -> {
            // btnDismiss already handles navigation when tapped.
        });
        dialog.show();
    }

    private void loadAvatarImage(String uri) {
        AvatarLoader.loadAvatar(binding.ivAvatar, uri);
    }

    private void showAvatarSourcePicker() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_avatar_source_picker, null);
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        dialogView.findViewById(R.id.btn_pick_camera).setOnClickListener(v -> {
            dialog.dismiss();
            try {
                takePhotoLauncher.launch(null);
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(getContext(), "Không thể mở camera", Toast.LENGTH_SHORT).show();
            }
        });

        dialogView.findViewById(R.id.btn_pick_gallery).setOnClickListener(v -> {
            dialog.dismiss();
            try {
                pickImageLauncher.launch("image/*");
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(getContext(), "Không thể mở thư viện", Toast.LENGTH_SHORT).show();
            }
        });

        dialogView.findViewById(R.id.btn_picker_cancel).setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private float startX = 0f;
    private float startY = 0f;

    private void showCropperDialog(Bitmap sourceBitmap) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_avatar_cropper, null);
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setCancelable(false)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        ImageView ivCropSource = dialogView.findViewById(R.id.iv_crop_source);
        SeekBar sbZoom = dialogView.findViewById(R.id.sb_zoom);
        View btnCancel = dialogView.findViewById(R.id.btn_crop_cancel);
        View btnConfirm = dialogView.findViewById(R.id.btn_crop_confirm);

        ivCropSource.setScaleType(ImageView.ScaleType.MATRIX);

        ivCropSource.post(() -> {
            float viewWidth = ivCropSource.getWidth();
            float viewHeight = ivCropSource.getHeight();
            if (viewWidth <= 0 || viewHeight <= 0) return;

            float imgWidth = sourceBitmap.getWidth();
            float imgHeight = sourceBitmap.getHeight();

            float scaleX = viewWidth / imgWidth;
            float scaleY = viewHeight / imgHeight;
            float initialScale = Math.max(scaleX, scaleY);

            float transX = (viewWidth - imgWidth * initialScale) / 2f;
            float transY = (viewHeight - imgHeight * initialScale) / 2f;

            Matrix matrix = new Matrix();
            matrix.postScale(initialScale, initialScale);
            matrix.postTranslate(transX, transY);
            ivCropSource.setImageMatrix(matrix);
            ivCropSource.setImageBitmap(sourceBitmap);

            Matrix savedMatrix = new Matrix();

            ivCropSource.setOnTouchListener((v, event) -> {
                switch (event.getAction() & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_DOWN:
                        savedMatrix.set(ivCropSource.getImageMatrix());
                        startX = event.getX();
                        startY = event.getY();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        float dx = event.getX() - startX;
                        float dy = event.getY() - startY;
                        Matrix newMatrix = new Matrix(savedMatrix);
                        newMatrix.postTranslate(dx, dy);
                        ivCropSource.setImageMatrix(newMatrix);
                        break;
                }
                return true;
            });

            sbZoom.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        float zoomFactor = 1f + (progress / 100f) * 3.5f;
                        Matrix newMatrix = new Matrix(savedMatrix);
                        newMatrix.postScale(zoomFactor, zoomFactor, viewWidth / 2f, viewHeight / 2f);
                        ivCropSource.setImageMatrix(newMatrix);
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    savedMatrix.set(ivCropSource.getImageMatrix());
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnConfirm.setOnClickListener(v -> {
            dialog.dismiss();
            try {
                int viewSize = ivCropSource.getWidth();
                if (viewSize <= 0) return;

                int targetSize = 500;
                Bitmap croppedBitmap = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(croppedBitmap);

                Matrix currentMatrix = ivCropSource.getImageMatrix();
                Matrix drawMatrix = new Matrix(currentMatrix);

                float scale = (float) targetSize / viewSize;
                drawMatrix.postScale(scale, scale, 0f, 0f);

                canvas.drawBitmap(sourceBitmap, drawMatrix, new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG));

                handleAvatarTaken(croppedBitmap);
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(getContext(), "Lỗi khi cắt ảnh", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private void handleAvatarPicked(Uri uri) {
        String path = saveAvatarToInternalStorage(uri);
        if (path != null) {
            String fileUri = "file://" + path;
            ProfileSession.setAvatar(requireContext(), fileUri);
            loadAvatarImage(fileUri);
            uploadAvatarToCloudinary(Uri.parse(fileUri));
        } else {
            Toast.makeText(getContext(), "Lỗi khi lưu ảnh", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleAvatarTaken(Bitmap bitmap) {
        String path = saveAvatarBitmapToInternalStorage(bitmap);
        if (path != null) {
            String fileUri = "file://" + path;
            ProfileSession.setAvatar(requireContext(), fileUri);
            loadAvatarImage(fileUri);
            uploadAvatarToCloudinary(Uri.parse(fileUri));
        } else {
            Toast.makeText(getContext(), "Lỗi khi lưu ảnh", Toast.LENGTH_SHORT).show();
        }
    }

    private void uploadAvatarToCloudinary(Uri fileUri) {
        Context context = requireContext();
        if (!com.veganbeauty.app.utils.CloudinaryConfig.isConfigured()) {
            Toast.makeText(
                            context,
                            "Chưa cấu hình Cloudinary. Sửa CloudinaryConfig.java.",
                            Toast.LENGTH_LONG)
                    .show();
            return;
        }

        Toast.makeText(context, "Đang tải ảnh đại diện lên Cloudinary...", Toast.LENGTH_LONG).show();

        avatarUploadFinished = false;
        if (avatarUploadTimeoutRunnable != null) {
            mainHandler.removeCallbacks(avatarUploadTimeoutRunnable);
        }
        avatarUploadTimeoutRunnable = () -> {
            if (!avatarUploadFinished) {
                Toast.makeText(
                        context.getApplicationContext(),
                        "Upload avatar quá lâu (60 giây). Kiểm tra Wi‑Fi và thử lại.",
                        Toast.LENGTH_LONG
                ).show();
            }
        };
        mainHandler.postDelayed(avatarUploadTimeoutRunnable, 60_000);

        SyncDataHelper.uploadAndSyncAvatar(
                context,
                fileUri,
                (success, secureUrl, errorMessage) -> {
                    avatarUploadFinished = true;
                    if (avatarUploadTimeoutRunnable != null) {
                        mainHandler.removeCallbacks(avatarUploadTimeoutRunnable);
                    }
                    notifyAvatarUploadResult(context, success, secureUrl, errorMessage);
                });
    }

    private void notifyAvatarUploadResult(
            Context context, boolean success, @Nullable String secureUrl, @Nullable String errorMessage) {
        Context toastCtx = context.getApplicationContext();
        if (success && secureUrl != null) {
            ProfileSession.setAvatar(context, secureUrl);
            if (binding != null && isAdded()) {
                loadAvatarImage(secureUrl);
            }
            Toast.makeText(toastCtx, "Cập nhật avatar thành công!", Toast.LENGTH_LONG).show();
            if (errorMessage != null && !errorMessage.isEmpty()) {
                Toast.makeText(toastCtx, errorMessage, Toast.LENGTH_LONG).show();
            }
        } else {
            String message = errorMessage != null && !errorMessage.isEmpty()
                    ? errorMessage
                    : "Không thể tải avatar lên cloud. Ảnh vẫn được lưu trên máy.";
            Toast.makeText(toastCtx, message, Toast.LENGTH_LONG).show();
            if (binding != null && isAdded()) {
                loadAvatarImage(ProfileSessionHelper.getAccountProfileAvatarUrl(context));
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (binding != null && !isSavingProfile) {
            Context ctx = requireContext();
            String avatarUrl = ProfileSessionHelper.getAccountProfileAvatarUrl(ctx);
            loadAvatarImage(avatarUrl);
        }
    }

    @Override
    public void onDestroyView() {
        if (avatarUploadTimeoutRunnable != null) {
            mainHandler.removeCallbacks(avatarUploadTimeoutRunnable);
        }
        ProfileUpdateNotifier.removeListener(profileUpdateListener);
        super.onDestroyView();
        binding = null;
    }

    private String saveAvatarToInternalStorage(Uri uri) {
        try {
            Context context = requireContext();
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            if (inputStream == null) return null;
            File file = new File(context.getFilesDir(), "user_avatar.jpg");
            FileOutputStream outputStream = new FileOutputStream(file);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            outputStream.close();
            inputStream.close();
            return file.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String saveAvatarBitmapToInternalStorage(Bitmap bitmap) {
        try {
            Context context = requireContext();
            File file = new File(context.getFilesDir(), "user_avatar.jpg");
            FileOutputStream outputStream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream);
            outputStream.flush();
            outputStream.close();
            return file.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void observeViewModel() {
        // Not used
    }
}
