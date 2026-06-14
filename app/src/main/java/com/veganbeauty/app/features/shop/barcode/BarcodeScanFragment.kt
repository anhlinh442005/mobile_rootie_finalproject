package com.veganbeauty.app.features.shop.barcode

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.veganbeauty.app.R
import com.veganbeauty.app.core.base.RootieFragment
import com.veganbeauty.app.data.local.LocalJsonReader
import com.veganbeauty.app.data.local.RootieDatabase
import com.veganbeauty.app.data.repository.ProductRepository
import com.veganbeauty.app.databinding.ShopBarcodeScanBinding
import com.veganbeauty.app.features.shop.product.detail.ShopDetailFragment
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class BarcodeScanFragment : RootieFragment() {

    private var _binding: ShopBarcodeScanBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: BarcodeScanViewModel
    private lateinit var cameraExecutor: ExecutorService

    private var lastScannedValue: String? = null
    private var cameraProvider: ProcessCameraProvider? = null

    private val barcodeScanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
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
    )

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startCamera()
            else {
                Toast.makeText(
                    requireContext(),
                    "Cần cấp quyền camera để quét mã vạch sản phẩm",
                    Toast.LENGTH_LONG
                ).show()
                parentFragmentManager.popBackStack()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ShopBarcodeScanBinding.inflate(inflater, container, false)
        setupViewModel()
        return binding.root
    }

    private fun setupViewModel() {
        val db = RootieDatabase.getDatabase(requireContext())
        val repository = ProductRepository(db.productDao(), LocalJsonReader(requireContext()))

        viewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return BarcodeScanViewModel(repository) as T
            }
        })[BarcodeScanViewModel::class.java]
    }

    override fun setupUI(view: View) {
        cameraExecutor = Executors.newSingleThreadExecutor()

        binding.barcodeScanBtnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        checkPermissionAndStart()
    }

    override fun observeViewModel() {
        viewModel.scanState.observe(viewLifecycleOwner) { state ->
            when (state) {
                BarcodeScanState.Idle -> {
                    binding.barcodeScanLoading.visibility = View.GONE
                    binding.barcodeScanStatus.visibility = View.GONE
                }

                BarcodeScanState.Loading -> {
                    binding.barcodeScanLoading.visibility = View.VISIBLE
                    binding.barcodeScanStatus.visibility = View.VISIBLE
                    binding.barcodeScanStatus.text = "Đang tìm sản phẩm..."
                }

                is BarcodeScanState.Found -> {
                    binding.barcodeScanLoading.visibility = View.GONE
                    binding.barcodeScanStatus.visibility = View.GONE
                    openProductDetail(state.product)
                }

                is BarcodeScanState.NotFound -> {
                    binding.barcodeScanLoading.visibility = View.GONE
                    binding.barcodeScanStatus.visibility = View.VISIBLE
                    binding.barcodeScanStatus.text = "Không tìm thấy sản phẩm với mã ${state.barcode}"
                    Toast.makeText(
                        requireContext(),
                        "Không tìm thấy sản phẩm tương ứng",
                        Toast.LENGTH_SHORT
                    ).show()
                    binding.root.postDelayed({
                        lastScannedValue = null
                        viewModel.resetToScanning()
                    }, 2000)
                }
            }
        }
    }

    private fun checkPermissionAndStart() {
        when {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED -> startCamera()

            else -> requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.barcodeScanPreview.surfaceProvider)
            }

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { analysis ->
                    analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                        @androidx.camera.core.ExperimentalGetImage
                        val mediaImage = imageProxy.image
                        if (mediaImage == null) {
                            imageProxy.close()
                            return@setAnalyzer
                        }

                        val image = InputImage.fromMediaImage(
                            mediaImage,
                            imageProxy.imageInfo.rotationDegrees
                        )

                        barcodeScanner.process(image)
                            .addOnSuccessListener { barcodes ->
                                val rawValue = barcodes.firstOrNull()?.rawValue?.trim().orEmpty()
                                if (rawValue.isNotEmpty() && rawValue != lastScannedValue) {
                                    lastScannedValue = rawValue
                                    requireActivity().runOnUiThread {
                                        viewModel.lookupBarcode(rawValue)
                                    }
                                }
                            }
                            .addOnCompleteListener {
                                imageProxy.close()
                            }
                    }
                }

            try {
                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(
                    viewLifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalysis
                )
            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "Không thể mở camera: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun openProductDetail(product: com.veganbeauty.app.data.local.entities.ProductEntity) {
        val detailFragment = ShopDetailFragment()
        detailFragment.setProduct(product)

        // Pop màn quét khỏi back stack để nút Back ở chi tiết quay về shop_home
        parentFragmentManager.popBackStackImmediate()

        parentFragmentManager.beginTransaction()
            .setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out,
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
            .replace(R.id.main_container, detailFragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {
        cameraProvider?.unbindAll()
        barcodeScanner.close()
        cameraExecutor.shutdown()
        super.onDestroyView()
        _binding = null
    }
}
