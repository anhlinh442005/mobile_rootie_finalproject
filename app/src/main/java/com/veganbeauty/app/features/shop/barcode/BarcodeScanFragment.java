package com.veganbeauty.app.features.shop.barcode;

import android.Manifest;
import android.content.pm.PackageManager;
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
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;
import com.veganbeauty.app.R;
import com.veganbeauty.app.core.base.RootieFragment;
import com.veganbeauty.app.data.local.LocalJsonReader;
import com.veganbeauty.app.data.local.RootieDatabase;
import com.veganbeauty.app.data.repository.ProductRepository;
import com.veganbeauty.app.databinding.ShopBarcodeScanBinding;
import com.veganbeauty.app.features.shop.product.detail.ShopDetailFragment;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BarcodeScanFragment extends RootieFragment {

    private ShopBarcodeScanBinding _binding;

    private BarcodeScanViewModel viewModel;
    private ExecutorService cameraExecutor;

    private String lastScannedValue = null;
    private ProcessCameraProvider cameraProvider = null;

    private final BarcodeScanner barcodeScanner = BarcodeScanning.getClient(
            new BarcodeScannerOptions.Builder()
                    .setBarcodeFormats(
                            Barcode.FORMAT_EAN_13,
                            Barcode.FORMAT_EAN_8,
                            Barcode.FORMAT_UPC_A,
                            Barcode.FORMAT_UPC_E,
                            Barcode.FORMAT_CODE_128,
                            Barcode.FORMAT_CODE_39,
                            Barcode.FORMAT_QR_CODE
                    )
                    .build()
    );

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    startCamera();
                } else {
                    Toast.makeText(
                            requireContext(),
                            "Cần cấp quyền camera để quét mã vạch sản phẩm",
                            Toast.LENGTH_LONG
                    ).show();
                    getParentFragmentManager().popBackStack();
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        _binding = ShopBarcodeScanBinding.inflate(inflater, container, false);
        setupViewModel();
        return _binding.getRoot();
    }

    private void setupViewModel() {
        RootieDatabase db = RootieDatabase.getDatabase(requireContext());
        ProductRepository repository = new ProductRepository(db.productDao(), new LocalJsonReader(requireContext()));

        viewModel = new ViewModelProvider(this, new ViewModelProvider.Factory() {
            @NonNull
            @Override
            public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
                return (T) new BarcodeScanViewModel(repository);
            }
        }).get(BarcodeScanViewModel.class);
    }

    @Override
    protected void setupUI(@NonNull View view) {
        cameraExecutor = Executors.newSingleThreadExecutor();

        _binding.barcodeScanBtnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        checkPermissionAndStart();
    }

    @Override
    protected void observeViewModel() {
        viewModel.getScanState().observe(getViewLifecycleOwner(), state -> {
            if (state instanceof BarcodeScanState.Idle) {
                _binding.barcodeScanLoading.setVisibility(View.GONE);
                _binding.barcodeScanStatus.setVisibility(View.GONE);
            } else if (state instanceof BarcodeScanState.Loading) {
                _binding.barcodeScanLoading.setVisibility(View.VISIBLE);
                _binding.barcodeScanStatus.setVisibility(View.VISIBLE);
                _binding.barcodeScanStatus.setText("Đang tìm sản phẩm...");
            } else if (state instanceof BarcodeScanState.Found) {
                _binding.barcodeScanLoading.setVisibility(View.GONE);
                _binding.barcodeScanStatus.setVisibility(View.GONE);
                openProductDetail(((BarcodeScanState.Found) state).getProduct());
            } else if (state instanceof BarcodeScanState.NotFound) {
                _binding.barcodeScanLoading.setVisibility(View.GONE);
                _binding.barcodeScanStatus.setVisibility(View.VISIBLE);
                _binding.barcodeScanStatus.setText("Không tìm thấy sản phẩm với mã " + ((BarcodeScanState.NotFound) state).getBarcode());
                Toast.makeText(requireContext(), "Không tìm thấy sản phẩm tương ứng", Toast.LENGTH_SHORT).show();
                _binding.getRoot().postDelayed(() -> {
                    lastScannedValue = null;
                    viewModel.resetToScanning();
                }, 2000);
            }
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
                cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(_binding.barcodeScanPreview.getSurfaceProvider());

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(cameraExecutor, imageProxy -> {
                    @androidx.camera.core.ExperimentalGetImage
                    android.media.Image mediaImage = imageProxy.getImage();
                    if (mediaImage == null) {
                        imageProxy.close();
                        return;
                    }

                    InputImage image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());

                    barcodeScanner.process(image)
                            .addOnSuccessListener(barcodes -> {
                                String rawValue = null;
                                if (!barcodes.isEmpty()) {
                                    String val = barcodes.get(0).getRawValue();
                                    if (val != null) {
                                        rawValue = val.trim();
                                    }
                                }

                                if (rawValue != null && !rawValue.isEmpty() && !rawValue.equals(lastScannedValue)) {
                                    lastScannedValue = rawValue;
                                    String finalRawValue = rawValue;
                                    requireActivity().runOnUiThread(() -> viewModel.lookupBarcode(finalRawValue));
                                }
                            })
                            .addOnCompleteListener(task -> imageProxy.close());
                });

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(getViewLifecycleOwner(), CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis);

            } catch (Exception e) {
                Toast.makeText(requireContext(), "Không thể mở camera: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void openProductDetail(com.veganbeauty.app.data.local.entities.ProductEntity product) {
        ShopDetailFragment detailFragment = new ShopDetailFragment();
        detailFragment.setProduct(product);

        getParentFragmentManager().popBackStackImmediate();

        getParentFragmentManager().beginTransaction()
                .setCustomAnimations(
                        android.R.anim.fade_in,
                        android.R.anim.fade_out,
                        android.R.anim.fade_in,
                        android.R.anim.fade_out
                )
                .replace(R.id.main_container, detailFragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onDestroyView() {
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
        barcodeScanner.close();
        cameraExecutor.shutdown();
        super.onDestroyView();
        _binding = null;
    }
}
