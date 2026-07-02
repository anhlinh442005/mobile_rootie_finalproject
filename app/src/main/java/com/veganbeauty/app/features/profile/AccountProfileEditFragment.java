package com.veganbeauty.app.features.profile;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
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
import com.veganbeauty.app.utils.AvatarLoader;
import com.veganbeauty.app.utils.SyncDataHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.Locale;

public class AccountProfileEditFragment extends RootieFragment {

    private AccountProfileEditBinding binding;

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
        String avatarUrl = ProfileSession.INSTANCE.getAvatar(ctx);
        loadAvatarImage(avatarUrl);

        String fullName = ProfileSession.INSTANCE.getFullName(ctx);
        String email = ProfileSession.INSTANCE.getEmail(ctx);
        String phone = ProfileSession.INSTANCE.getPhone(ctx);
        String dob = ProfileSession.INSTANCE.getDob(ctx);
        String gender = ProfileSession.INSTANCE.getGender(ctx);

        binding.tvUsername.setText(fullName);
        binding.etEmail.setText(email);
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

        View.OnFocusChangeListener focusListener = (v, hasFocus) -> {
            View lineView = null;
            if (v.getId() == R.id.et_email) lineView = binding.viewEmailLine;
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

        binding.btnSave.setOnClickListener(v -> {
            Context saveCtx = requireContext();
            String newFullName = binding.etFullname.getText().toString();
            String newEmail = binding.etEmail.getText().toString();
            String newPhone = binding.etPhone.getText().toString();
            String newDob = binding.tvDob.getText().toString();
            String newGender = "Nữ";
            if (binding.rbMale.isChecked()) newGender = "Nam";
            else if (binding.rbOther.isChecked()) newGender = "Khác";

            ProfileSession.INSTANCE.setFullName(saveCtx, newFullName);
            ProfileSession.INSTANCE.setEmail(saveCtx, newEmail);
            ProfileSession.INSTANCE.setPhone(saveCtx, newPhone);
            ProfileSession.INSTANCE.setDob(saveCtx, newDob);
            ProfileSession.INSTANCE.setGender(saveCtx, newGender);

            SyncDataHelper.syncUserProfileToFirebaseAndLocal(saveCtx);

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
            dialog.setOnDismissListener(d -> getParentFragmentManager().popBackStack());
            dialog.show();
        });

        com.veganbeauty.app.features.home.BottomNavHelper.highlightTab(view, R.id.nav_account);

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
            ProfileSession.INSTANCE.setAvatar(requireContext(), fileUri);
            loadAvatarImage(fileUri);
            Toast.makeText(getContext(), "Đã cập nhật ảnh đại diện cục bộ", Toast.LENGTH_SHORT).show();
            uploadAvatarToFirebase(Uri.parse(fileUri));
        } else {
            Toast.makeText(getContext(), "Lỗi khi lưu ảnh", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleAvatarTaken(Bitmap bitmap) {
        String path = saveAvatarBitmapToInternalStorage(bitmap);
        if (path != null) {
            String fileUri = "file://" + path;
            ProfileSession.INSTANCE.setAvatar(requireContext(), fileUri);
            loadAvatarImage(fileUri);
            Toast.makeText(getContext(), "Đã cập nhật ảnh đại diện cục bộ", Toast.LENGTH_SHORT).show();
            uploadAvatarToFirebase(Uri.parse(fileUri));
        } else {
            Toast.makeText(getContext(), "Lỗi khi lưu ảnh", Toast.LENGTH_SHORT).show();
        }
    }

    private void uploadAvatarToFirebase(Uri fileUri) {
        Context context = requireContext();
        Toast progressToast = Toast.makeText(context, "Đang tải ảnh đại diện lên Firebase...", Toast.LENGTH_LONG);
        progressToast.show();

        SyncDataHelper.uploadAvatarToFirebase(context, fileUri, downloadUrl -> {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (downloadUrl != null) {
                        loadAvatarImage(downloadUrl);
                        Toast.makeText(context, "Đồng bộ ảnh đại diện lên Firebase thành công!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(context, "Không thể đồng bộ ảnh đại diện lên Firebase. Đã lưu cục bộ.", Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (binding != null) {
            String avatarUrl = ProfileSession.INSTANCE.getAvatar(requireContext());
            loadAvatarImage(avatarUrl);
        }
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
