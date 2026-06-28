package com.veganbeauty.app.features.myskin;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.veganbeauty.app.R;
import com.veganbeauty.app.core.base.RootieFragment;
import com.veganbeauty.app.databinding.SkinFragmentScanBinding;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SkinScanFragment extends RootieFragment {

    private SkinFragmentScanBinding binding;

    private ImageCapture imageCapture;
    private CameraSelector cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;
    private ExecutorService cameraExecutor;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    startCamera();
                } else {
                    Toast.makeText(requireContext(), "Cần cấp quyền camera để sử dụng tính năng này", Toast.LENGTH_LONG).show();
                }
            });

    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    openResultFragment(uri.toString());
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = SkinFragmentScanBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void setupUI(@NonNull View view) {
        cameraExecutor = Executors.newSingleThreadExecutor();

        setupListeners();
        checkPermissionAndStart();
    }

    private void setupListeners() {
        binding.skinScanBtnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        binding.skinScanBtnCapture.setOnClickListener(v -> takePhoto());

        binding.skinScanBtnFlip.setOnClickListener(v -> {
            if (cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA) {
                cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
            } else {
                cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;
            }
            startCamera();
        });

        binding.skinScanBtnAlbum.setOnClickListener(v -> pickImageLauncher.launch("image/*"));

        binding.skinScanBtnGuide.setOnClickListener(v -> {
            Dialog dialog = new Dialog(requireContext());
            dialog.setContentView(R.layout.skin_dialog_scan_instruction);
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            }

            View btnClose = dialog.findViewById(R.id.btn_close);
            if (btnClose != null) btnClose.setOnClickListener(v1 -> dialog.dismiss());

            View dialogContainer = dialog.findViewById(R.id.dialog_container);
            if (dialogContainer != null) dialogContainer.setOnClickListener(v1 -> dialog.dismiss());

            View dialogContent = dialog.findViewById(R.id.dialog_content);
            if (dialogContent != null) dialogContent.setOnClickListener(v1 -> {
                // Intercept click
            });

            dialog.show();
        });
    }

    private void checkPermissionAndStart() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext());
        cameraProviderFuture.addListener(() -> {
            try {
                if (binding == null || !isAdded()) return;
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(binding.skinScanPreview.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build();

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(
                        getViewLifecycleOwner(),
                        cameraSelector,
                        preview,
                        imageCapture
                );
            } catch (Exception e) {
                Toast.makeText(requireContext(), "Không thể mở camera: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void takePhoto() {
        if (imageCapture == null) return;

        File photoFile = new File(
                requireContext().getCacheDir(),
                new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis()) + ".jpg"
        );

        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(requireContext()),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onError(@NonNull ImageCaptureException exc) {
                        Toast.makeText(requireContext(), "Chụp ảnh thất bại: " + exc.getMessage(), Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults output) {
                        openResultFragment(photoFile.getAbsolutePath());
                    }
                }
        );
    }

    private void openResultFragment(String imageUri) {
        getParentFragmentManager().beginTransaction()
                .setCustomAnimations(
                        android.R.anim.fade_in,
                        android.R.anim.fade_out,
                        android.R.anim.fade_in,
                        android.R.anim.slide_out_right
                )
                .replace(R.id.main_container, SkinScanResultFragment.newInstance(imageUri))
                .addToBackStack(null)
                .commit();
    }

    @Override
    protected void observeViewModel() {
        // Not used
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        cameraExecutor.shutdown();
        binding = null;
    }
}
