package com.veganbeauty.app.features.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import coil.load
import coil.transform.CircleCropTransformation
import com.veganbeauty.app.core.base.RootieFragment
import com.veganbeauty.app.databinding.AccountProfileEditBinding

class AccountProfileEditFragment : RootieFragment() {

    private var _binding: AccountProfileEditBinding? = null
    private val binding get() = _binding!!

    private val pickImageLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let {
            try {
                val inputStream = requireContext().contentResolver.openInputStream(it)
                val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                if (bitmap != null) {
                    showCropperDialog(bitmap)
                } else {
                    Toast.makeText(context, "Không thể đọc ảnh chọn", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Lỗi khi tải ảnh từ thư viện", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val takePhotoLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.TakePicturePreview()
    ) { bitmap: android.graphics.Bitmap? ->
        bitmap?.let {
            showCropperDialog(it)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = AccountProfileEditBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun setupUI(view: View) {
        // Load the persistent avatar from ProfileSession using Coil with CircleCrop
        val ctx = requireContext()
        val avatarUrl = com.veganbeauty.app.data.local.ProfileSession.getAvatar(ctx)
        loadAvatarImage(avatarUrl)

        // Load values from ProfileSession
        val fullName = com.veganbeauty.app.data.local.ProfileSession.getFullName(ctx)
        val email = com.veganbeauty.app.data.local.ProfileSession.getEmail(ctx)
        val phone = com.veganbeauty.app.data.local.ProfileSession.getPhone(ctx)
        val dob = com.veganbeauty.app.data.local.ProfileSession.getDob(ctx)
        val gender = com.veganbeauty.app.data.local.ProfileSession.getGender(ctx)

        binding.tvUsername.text = fullName
        binding.etEmail.setText(email)
        binding.etFullname.setText(fullName)
        binding.etPhone.setText(phone)
        binding.tvDob.text = dob

        when (gender) {
            "Nam" -> binding.rbMale.isChecked = true
            "Nữ" -> binding.rbFemale.isChecked = true
            "Khác" -> binding.rbOther.isChecked = true
            else -> binding.rbFemale.isChecked = true
        }

        // Separator lines focus highlighting
        val focusListener = android.view.View.OnFocusChangeListener { v, hasFocus ->
            val lineView = when (v.id) {
                com.veganbeauty.app.R.id.et_email -> binding.viewEmailLine
                com.veganbeauty.app.R.id.et_fullname -> binding.viewFullnameLine
                com.veganbeauty.app.R.id.et_phone -> binding.viewPhoneLine
                else -> null
            }
            lineView?.let { line ->
                if (hasFocus) {
                    line.setBackgroundColor(android.graphics.Color.parseColor("#3E4D44")) // Brand primary color
                    line.layoutParams.height = (2 * resources.displayMetrics.density).toInt() // Thicker line
                } else {
                    line.setBackgroundColor(android.graphics.Color.parseColor("#E2E4E1")) // Default gray color
                    line.layoutParams.height = (1 * resources.displayMetrics.density).toInt() // Normal 1dp
                }
                line.requestLayout()
            }
        }
        binding.etEmail.onFocusChangeListener = focusListener
        binding.etFullname.onFocusChangeListener = focusListener
        binding.etPhone.onFocusChangeListener = focusListener

        // Automatically hide Bottom Navigation when keyboard is open
        val bottomNav = view.findViewById<android.view.View>(com.veganbeauty.app.R.id.bottom_nav)
        if (bottomNav != null) {
            val rootView = view
            rootView.viewTreeObserver.addOnGlobalLayoutListener {
                val rect = android.graphics.Rect()
                rootView.getWindowVisibleDisplayFrame(rect)
                val screenHeight = rootView.rootView.height
                val keypadHeight = screenHeight - rect.bottom
                if (keypadHeight > screenHeight * 0.15) {
                    bottomNav.visibility = android.view.View.GONE
                } else {
                    bottomNav.visibility = android.view.View.VISIBLE
                }
            }
        }

        // Back button action - just pop backstack without saving (discard changes)
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Save button action - saves current fields and syncs to Firebase
        binding.btnSave.setOnClickListener {
            val saveCtx = requireContext()
            val newFullName = binding.etFullname.text.toString()
            val newEmail = binding.etEmail.text.toString()
            val newPhone = binding.etPhone.text.toString()
            val newDob = binding.tvDob.text.toString()
            val newGender = when {
                binding.rbMale.isChecked -> "Nam"
                binding.rbFemale.isChecked -> "Nữ"
                binding.rbOther.isChecked -> "Khác"
                else -> "Nữ"
            }

            com.veganbeauty.app.data.local.ProfileSession.setFullName(saveCtx, newFullName)
            com.veganbeauty.app.data.local.ProfileSession.setEmail(saveCtx, newEmail)
            com.veganbeauty.app.data.local.ProfileSession.setPhone(saveCtx, newPhone)
            com.veganbeauty.app.data.local.ProfileSession.setDob(saveCtx, newDob)
            com.veganbeauty.app.data.local.ProfileSession.setGender(saveCtx, newGender)

            com.veganbeauty.app.utils.SyncDataHelper.syncUserProfileToFirebaseAndLocal(saveCtx)
            
            // Show custom success dialog instead of standard Toast
            val dialogView = layoutInflater.inflate(com.veganbeauty.app.R.layout.dialog_save_profile_success, null)
            val dialog = androidx.appcompat.app.AlertDialog.Builder(saveCtx)
                .setView(dialogView)
                .setCancelable(true)
                .create()
            
            dialog.getWindow()?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
            
            dialogView.findViewById<android.view.View>(com.veganbeauty.app.R.id.btnDialogDismiss)?.setOnClickListener {
                dialog.dismiss()
                parentFragmentManager.popBackStack()
            }
            dialog.setOnDismissListener {
                parentFragmentManager.popBackStack()
            }
            dialog.show()
        }

        // Highlight the "Tài khoản" tab as active in the bottom navigation menu
        view.findViewById<android.view.ViewGroup>(com.veganbeauty.app.R.id.nav_account)?.let { navAccount ->
            val icon = navAccount.getChildAt(0) as? android.widget.ImageView
            val label = navAccount.getChildAt(1) as? android.widget.TextView
            
            // Set active green color tint to the icon (#677559)
            icon?.setColorFilter(android.graphics.Color.parseColor("#677559"))
            
            // Set active green color and bold style to the text label
            label?.setTextColor(android.graphics.Color.parseColor("#677559"))
            label?.setTypeface(null, android.graphics.Typeface.BOLD)
        }

        // Change Avatar button action -> shows source picker dialog
        binding.btnChangeAvatar.setOnClickListener {
            showAvatarSourcePicker()
        }

        // MaterialDatePicker for selecting date of birth
        binding.btnSelectDob.setOnClickListener {
            val currentDob = binding.tvDob.text.toString()
            val parts = currentDob.split("/")
            var initialSelectionMs: Long? = null
            if (parts.size == 3) {
                try {
                    val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
                    cal.clear()
                    cal.set(java.util.Calendar.DAY_OF_MONTH, parts[0].toInt())
                    cal.set(java.util.Calendar.MONTH, parts[1].toInt() - 1)
                    cal.set(java.util.Calendar.YEAR, parts[2].toInt())
                    initialSelectionMs = cal.timeInMillis
                } catch (e: Exception) {}
            }

            val builder = com.google.android.material.datepicker.MaterialDatePicker.Builder.datePicker()
            builder.setTitleText("Chọn ngày sinh")
            if (initialSelectionMs != null) {
                builder.setSelection(initialSelectionMs)
            }

            val constraintsBuilder = com.google.android.material.datepicker.CalendarConstraints.Builder()
            constraintsBuilder.setValidator(com.google.android.material.datepicker.DateValidatorPointBackward.now())
            builder.setCalendarConstraints(constraintsBuilder.build())

            val picker = builder.build()
            picker.addOnPositiveButtonClickListener { selection ->
                val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
                cal.timeInMillis = selection
                val selectedDay = cal.get(java.util.Calendar.DAY_OF_MONTH)
                val selectedMonth = cal.get(java.util.Calendar.MONTH) + 1
                val selectedYear = cal.get(java.util.Calendar.YEAR)
                
                val formattedDay = String.format("%02d", selectedDay)
                val formattedMonth = String.format("%02d", selectedMonth)
                binding.tvDob.text = "$formattedDay/$formattedMonth/$selectedYear"
            }
            picker.show(parentFragmentManager, "DATE_PICKER")
        }

        binding.btnLinkedAccounts.setOnClickListener {
            Toast.makeText(context, "Quản lý tài khoản liên kết", Toast.LENGTH_SHORT).show()
        }

        binding.btnPersonalInfo.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(com.veganbeauty.app.R.id.main_container, AccountProfilePersonalInfoFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.btnAccountSettings.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(com.veganbeauty.app.R.id.main_container, AccountProfileSetupFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.btnChangePassword.setOnClickListener {
            Toast.makeText(context, "Thay đổi mật khẩu", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadAvatarImage(uri: String) {
        com.veganbeauty.app.utils.AvatarLoader.loadAvatar(binding.ivAvatar, uri)
    }

    private fun showAvatarSourcePicker() {
        val dialogView = layoutInflater.inflate(com.veganbeauty.app.R.layout.dialog_avatar_source_picker, null)
        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))

        dialogView.findViewById<View>(com.veganbeauty.app.R.id.btn_pick_camera).setOnClickListener {
            dialog.dismiss()
            try {
                takePhotoLauncher.launch(null)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Không thể mở camera", Toast.LENGTH_SHORT).show()
            }
        }

        dialogView.findViewById<View>(com.veganbeauty.app.R.id.btn_pick_gallery).setOnClickListener {
            dialog.dismiss()
            try {
                pickImageLauncher.launch("image/*")
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Không thể mở thư viện", Toast.LENGTH_SHORT).show()
            }
        }

        dialogView.findViewById<View>(com.veganbeauty.app.R.id.btn_picker_cancel).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showCropperDialog(sourceBitmap: android.graphics.Bitmap) {
        val dialogView = layoutInflater.inflate(com.veganbeauty.app.R.layout.dialog_avatar_cropper, null)
        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))

        val ivCropSource = dialogView.findViewById<android.widget.ImageView>(com.veganbeauty.app.R.id.iv_crop_source)
        val sbZoom = dialogView.findViewById<android.widget.SeekBar>(com.veganbeauty.app.R.id.sb_zoom)
        val btnCancel = dialogView.findViewById<android.view.View>(com.veganbeauty.app.R.id.btn_crop_cancel)
        val btnConfirm = dialogView.findViewById<android.view.View>(com.veganbeauty.app.R.id.btn_crop_confirm)

        ivCropSource.scaleType = android.widget.ImageView.ScaleType.MATRIX

        ivCropSource.post {
            val viewWidth = ivCropSource.width.toFloat()
            val viewHeight = ivCropSource.height.toFloat()
            if (viewWidth <= 0 || viewHeight <= 0) return@post

            val imgWidth = sourceBitmap.width.toFloat()
            val imgHeight = sourceBitmap.height.toFloat()

            // Fit-center fill scale
            val scaleX = viewWidth / imgWidth
            val scaleY = viewHeight / imgHeight
            val initialScale = Math.max(scaleX, scaleY)

            val transX = (viewWidth - imgWidth * initialScale) / 2f
            val transY = (viewHeight - imgHeight * initialScale) / 2f

            val matrix = android.graphics.Matrix()
            matrix.postScale(initialScale, initialScale)
            matrix.postTranslate(transX, transY)
            ivCropSource.imageMatrix = matrix
            ivCropSource.setImageBitmap(sourceBitmap)

            var startX = 0f
            var startY = 0f
            val savedMatrix = android.graphics.Matrix()

            ivCropSource.setOnTouchListener { _, event ->
                when (event.action and android.view.MotionEvent.ACTION_MASK) {
                    android.view.MotionEvent.ACTION_DOWN -> {
                        savedMatrix.set(ivCropSource.imageMatrix)
                        startX = event.x
                        startY = event.y
                    }
                    android.view.MotionEvent.ACTION_MOVE -> {
                        val dx = event.x - startX
                        val dy = event.y - startY
                        val newMatrix = android.graphics.Matrix(savedMatrix)
                        newMatrix.postTranslate(dx, dy)
                        ivCropSource.imageMatrix = newMatrix
                    }
                }
                true
            }

            sbZoom.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        val zoomFactor = 1f + (progress / 100f) * 3.5f
                        val newMatrix = android.graphics.Matrix(savedMatrix)
                        newMatrix.postScale(zoomFactor, zoomFactor, viewWidth / 2f, viewHeight / 2f)
                        ivCropSource.imageMatrix = newMatrix
                    }
                }

                override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {
                    savedMatrix.set(ivCropSource.imageMatrix)
                }

                override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
            })
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnConfirm.setOnClickListener {
            dialog.dismiss()
            try {
                val viewSize = ivCropSource.width
                if (viewSize <= 0) return@setOnClickListener

                val targetSize = 500
                val croppedBitmap = android.graphics.Bitmap.createBitmap(targetSize, targetSize, android.graphics.Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(croppedBitmap)

                val currentMatrix = ivCropSource.imageMatrix
                val drawMatrix = android.graphics.Matrix(currentMatrix)

                val scale = targetSize.toFloat() / viewSize.toFloat()
                drawMatrix.postScale(scale, scale, 0f, 0f)

                canvas.drawBitmap(sourceBitmap, drawMatrix, android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG or android.graphics.Paint.ANTI_ALIAS_FLAG))

                handleAvatarTaken(croppedBitmap)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Lỗi khi cắt ảnh", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun handleAvatarPicked(uri: android.net.Uri) {
        val path = saveAvatarToInternalStorage(uri)
        if (path != null) {
            val fileUri = "file://$path"
            com.veganbeauty.app.data.local.ProfileSession.setAvatar(requireContext(), fileUri)
            loadAvatarImage(fileUri)
            Toast.makeText(context, "Đã cập nhật ảnh đại diện cục bộ", Toast.LENGTH_SHORT).show()
            uploadAvatarToFirebase(android.net.Uri.parse(fileUri))
        } else {
            Toast.makeText(context, "Lỗi khi lưu ảnh", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleAvatarTaken(bitmap: android.graphics.Bitmap) {
        val path = saveAvatarBitmapToInternalStorage(bitmap)
        if (path != null) {
            val fileUri = "file://$path"
            com.veganbeauty.app.data.local.ProfileSession.setAvatar(requireContext(), fileUri)
            loadAvatarImage(fileUri)
            Toast.makeText(context, "Đã cập nhật ảnh đại diện cục bộ", Toast.LENGTH_SHORT).show()
            uploadAvatarToFirebase(android.net.Uri.parse(fileUri))
        } else {
            Toast.makeText(context, "Lỗi khi lưu ảnh", Toast.LENGTH_SHORT).show()
        }
    }

    private fun uploadAvatarToFirebase(fileUri: android.net.Uri) {
        val context = requireContext()
        val progressToast = Toast.makeText(context, "Đang tải ảnh đại diện lên Firebase...", Toast.LENGTH_LONG)
        progressToast.show()

        com.veganbeauty.app.utils.SyncDataHelper.uploadAvatarToFirebase(context, fileUri) { downloadUrl ->
            activity?.runOnUiThread {
                if (downloadUrl != null) {
                    loadAvatarImage(downloadUrl)
                    Toast.makeText(context, "Đồng bộ ảnh đại diện lên Firebase thành công!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Không thể đồng bộ ảnh đại diện lên Firebase. Đã lưu cục bộ.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (_binding != null) {
            val avatarUrl = com.veganbeauty.app.data.local.ProfileSession.getAvatar(requireContext())
            loadAvatarImage(avatarUrl)
        }
    }

    private fun saveAvatarToInternalStorage(uri: android.net.Uri): String? {
        return try {
            val context = requireContext()
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val file = java.io.File(context.filesDir, "user_avatar.jpg")
            val outputStream = java.io.FileOutputStream(file)
            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun saveAvatarBitmapToInternalStorage(bitmap: android.graphics.Bitmap): String? {
        return try {
            val context = requireContext()
            val file = java.io.File(context.filesDir, "user_avatar.jpg")
            val outputStream = java.io.FileOutputStream(file)
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, outputStream)
            outputStream.flush()
            outputStream.close()
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
