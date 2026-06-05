package com.veganbeauty.app.features.profile

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.veganbeauty.app.R
import com.veganbeauty.app.core.base.RootieFragment
import com.veganbeauty.app.data.local.ProfileSession
import com.veganbeauty.app.databinding.AccountProfileAddressBinding

class AccountProfileAddressFragment : RootieFragment() {

    private var _binding: AccountProfileAddressBinding? = null
    private val binding get() = _binding!!

    private val PREFS_NAME = "rootie_profile_prefs"
    
    // Preference Keys
    private val KEY_HOME_NAME = "addr_home_name"
    private val KEY_HOME_PHONE = "addr_home_phone"
    private val KEY_HOME_ADDR = "addr_home_addr"
    
    private val KEY_OFFICE_NAME = "addr_office_name"
    private val KEY_OFFICE_PHONE = "addr_office_phone"
    private val KEY_OFFICE_ADDR = "addr_office_addr"
    
    private val KEY_DEFAULT_TYPE = "addr_default_type" // "HOME" or "OFFICE"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = AccountProfileAddressBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun setupUI(view: View) {
        val context = requireContext()

        // Back button action
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Notification button
        binding.btnNotification.setOnClickListener {
            Toast.makeText(context, "Không có thông báo mới", Toast.LENGTH_SHORT).show()
        }

        // Load & bind data
        loadAddressData()

        // Badge / Card Default Click Listeners to toggle Default state
        binding.badgeHomeDefault.setOnClickListener {
            setDefaultAddress("HOME")
        }
        binding.badgeOfficeDefault.setOnClickListener {
            setDefaultAddress("OFFICE")
        }

        // Edit button clicks
        binding.btnHomeEdit.setOnClickListener {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val currentName = prefs.getString(KEY_HOME_NAME, "Ánh Linh") ?: "Ánh Linh"
            val currentPhone = prefs.getString(KEY_HOME_PHONE, "0999 999 999") ?: "0999 999 999"
            val currentAddr = prefs.getString(KEY_HOME_ADDR, "123 Đường Bến Nghé, Phường Bến Nghé, TP.Hồ Chí Minh") ?: "123 Đường Bến Nghé, Phường Bến Nghé, TP.Hồ Chí Minh"
            
            showEditDialog("Nhà riêng", currentName, currentPhone, currentAddr) { name, phone, address ->
                prefs.edit().apply {
                    putString(KEY_HOME_NAME, name)
                    putString(KEY_HOME_PHONE, phone)
                    putString(KEY_HOME_ADDR, address)
                    apply()
                }
                // If this is default, sync with the central address
                if (prefs.getString(KEY_DEFAULT_TYPE, "HOME") == "HOME") {
                    ProfileSession.setAddress(context, address)
                }
                loadAddressData()
                Toast.makeText(context, "Đã cập nhật địa chỉ Nhà riêng", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnOfficeEdit.setOnClickListener {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val currentName = prefs.getString(KEY_OFFICE_NAME, "Khánh Xuân") ?: "Khánh Xuân"
            val currentPhone = prefs.getString(KEY_OFFICE_PHONE, "0868 888 888") ?: "0868 888 888"
            val currentAddr = prefs.getString(KEY_OFFICE_ADDR, "Bitexco Financial Tower, 2 Hải Triều, Phường Bến Nghé, TP.Hồ Chí Minh") ?: "Bitexco Financial Tower, 2 Hải Triều, Phường Bến Nghé, TP.Hồ Chí Minh"
            
            showEditDialog("Văn phòng", currentName, currentPhone, currentAddr) { name, phone, address ->
                prefs.edit().apply {
                    putString(KEY_OFFICE_NAME, name)
                    putString(KEY_OFFICE_PHONE, phone)
                    putString(KEY_OFFICE_ADDR, address)
                    apply()
                }
                // If this is default, sync with the central address
                if (prefs.getString(KEY_DEFAULT_TYPE, "HOME") == "OFFICE") {
                    ProfileSession.setAddress(context, address)
                }
                loadAddressData()
                Toast.makeText(context, "Đã cập nhật địa chỉ Văn phòng", Toast.LENGTH_SHORT).show()
            }
        }

        // Delete button clicks (simulates deletion and resets values)
        binding.btnHomeDelete.setOnClickListener {
            AlertDialog.Builder(context)
                .setTitle("Xóa địa chỉ")
                .setMessage("Bạn có chắc chắn muốn xóa địa chỉ Nhà riêng?")
                .setPositiveButton("Xóa") { _, _ ->
                    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    prefs.edit().apply {
                        remove(KEY_HOME_NAME)
                        remove(KEY_HOME_PHONE)
                        remove(KEY_HOME_ADDR)
                        // If it was default, switch default to Office
                        if (prefs.getString(KEY_DEFAULT_TYPE, "HOME") == "HOME") {
                            putString(KEY_DEFAULT_TYPE, "OFFICE")
                            val officeAddr = prefs.getString(KEY_OFFICE_ADDR, "Bitexco Financial Tower, 2 Hải Triều, Phường Bến Nghé, TP.Hồ Chí Minh")
                            if (officeAddr != null) {
                                ProfileSession.setAddress(context, officeAddr)
                            }
                        }
                        apply()
                    }
                    loadAddressData()
                    Toast.makeText(context, "Đã xóa địa chỉ Nhà riêng", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Hủy", null)
                .show()
        }

        binding.btnOfficeDelete.setOnClickListener {
            AlertDialog.Builder(context)
                .setTitle("Xóa địa chỉ")
                .setMessage("Bạn có chắc chắn muốn xóa địa chỉ Văn phòng?")
                .setPositiveButton("Xóa") { _, _ ->
                    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    prefs.edit().apply {
                        remove(KEY_OFFICE_NAME)
                        remove(KEY_OFFICE_PHONE)
                        remove(KEY_OFFICE_ADDR)
                        // If it was default, switch default to Home
                        if (prefs.getString(KEY_DEFAULT_TYPE, "HOME") == "OFFICE") {
                            putString(KEY_DEFAULT_TYPE, "HOME")
                            val homeAddr = prefs.getString(KEY_HOME_ADDR, "123 Đường Bến Nghé, Phường Bến Nghé, TP.Hồ Chí Minh")
                            if (homeAddr != null) {
                                ProfileSession.setAddress(context, homeAddr)
                            }
                        }
                        apply()
                    }
                    loadAddressData()
                    Toast.makeText(context, "Đã xóa địa chỉ Văn phòng", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Hủy", null)
                .show()
        }

        // Add new address button click
        binding.btnAddAddress.setOnClickListener {
            showEditDialog("Mới", "", "", "") { name, phone, address ->
                // Simulate adding a third address or just setting/replacing one of the cards
                Toast.makeText(context, "Đã thêm địa chỉ mới thành công!", Toast.LENGTH_LONG).show()
            }
        }

        // Highlight "Tài khoản" active tab in bottom navigation shell
        view.findViewById<android.view.ViewGroup>(R.id.nav_account)?.let { navAccount ->
            val icon = navAccount.getChildAt(0) as? ImageView
            val label = navAccount.getChildAt(1) as? TextView
            icon?.setColorFilter(Color.parseColor("#677559"))
            label?.setTextColor(Color.parseColor("#677559"))
            label?.setTypeface(null, Typeface.BOLD)
        }
    }

    private fun loadAddressData() {
        val context = requireContext()
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Read or initialize Nhà riêng data
        val homeName = prefs.getString(KEY_HOME_NAME, "Ánh Linh") ?: "Ánh Linh"
        val homePhone = prefs.getString(KEY_HOME_PHONE, "0999 999 999") ?: "0999 999 999"
        val homeAddr = prefs.getString(KEY_HOME_ADDR, "123 Đường Bến Nghé, Phường Bến Nghé, TP.Hồ Chí Minh") ?: "123 Đường Bến Nghé, Phường Bến Nghé, TP.Hồ Chí Minh"

        // Read or initialize Văn phòng data
        val officeName = prefs.getString(KEY_OFFICE_NAME, "Khánh Xuân") ?: "Khánh Xuân"
        val officePhone = prefs.getString(KEY_OFFICE_PHONE, "0868 888 888") ?: "0868 888 888"
        val officeAddr = prefs.getString(KEY_OFFICE_ADDR, "Bitexco Financial Tower, 2 Hải Triều, Phường Bến Nghé, TP.Hồ Chí Minh") ?: "Bitexco Financial Tower, 2 Hải Triều, Phường Bến Nghé, TP.Hồ Chí Minh"

        val defaultType = prefs.getString(KEY_DEFAULT_TYPE, "HOME") ?: "HOME"

        // Bind Card 1 UI
        binding.tvHomeName.text = homeName
        binding.tvHomePhone.text = homePhone
        binding.tvHomeAddress.text = homeAddr

        // Bind Card 2 UI
        binding.tvOfficeName.text = officeName
        binding.tvOfficePhone.text = officePhone
        binding.tvOfficeAddress.text = officeAddr

        // Bind Badge styling dynamically according to Default status
        if (defaultType == "HOME") {
            // Card Home is DEFAULT (Active)
            binding.badgeHomeDefault.setBackgroundResource(R.drawable.bg_badge_default_active)
            binding.badgeHomeDefault.setTextColor(Color.parseColor("#D9D9D9"))
            
            // Card Office is NOT DEFAULT (Inactive)
            binding.badgeOfficeDefault.setBackgroundResource(R.drawable.bg_badge_default_inactive)
            binding.badgeOfficeDefault.setTextColor(Color.parseColor("#000000"))
        } else {
            // Card Office is DEFAULT (Active)
            binding.badgeOfficeDefault.setBackgroundResource(R.drawable.bg_badge_default_active)
            binding.badgeOfficeDefault.setTextColor(Color.parseColor("#D9D9D9"))
            
            // Card Home is NOT DEFAULT (Inactive)
            binding.badgeHomeDefault.setBackgroundResource(R.drawable.bg_badge_default_inactive)
            binding.badgeHomeDefault.setTextColor(Color.parseColor("#000000"))
        }
    }

    private fun setDefaultAddress(type: String) {
        val context = requireContext()
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        
        prefs.edit().putString(KEY_DEFAULT_TYPE, type).apply()
        
        // Sync the main address stored in ProfileSession to the default one chosen
        val defaultAddr = if (type == "HOME") {
            prefs.getString(KEY_HOME_ADDR, "123 Đường Bến Nghé, Phường Bến Nghé, TP.Hồ Chí Minh")
        } else {
            prefs.getString(KEY_OFFICE_ADDR, "Bitexco Financial Tower, 2 Hải Triều, Phường Bến Nghé, TP.Hồ Chí Minh")
        }
        
        if (defaultAddr != null) {
            ProfileSession.setAddress(context, defaultAddr)
        }

        loadAddressData()
        val targetName = if (type == "HOME") "Nhà riêng" else "Văn phòng"
        Toast.makeText(context, "Đã đặt $targetName làm địa chỉ mặc định", Toast.LENGTH_SHORT).show()
    }

    private fun showEditDialog(
        title: String,
        currentName: String,
        currentPhone: String,
        currentAddress: String,
        onSave: (name: String, phone: String, address: String) -> Unit
    ) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.account_dialog_edit_address, null)
        val tvTitle = dialogView.findViewById<TextView>(R.id.tv_dialog_title)
        val etName = dialogView.findViewById<EditText>(R.id.et_dialog_name)
        val etPhone = dialogView.findViewById<EditText>(R.id.et_dialog_phone)
        val etAddress = dialogView.findViewById<EditText>(R.id.et_dialog_address)
        val btnCancel = dialogView.findViewById<View>(R.id.btn_dialog_cancel)
        val btnSave = dialogView.findViewById<View>(R.id.btn_dialog_save)

        tvTitle.text = "Chỉnh sửa địa chỉ: $title"
        etName.setText(currentName)
        etPhone.setText(currentPhone)
        etAddress.setText(currentAddress)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnSave.setOnClickListener {
            val name = etName.text.toString().trim()
            val phone = etPhone.text.toString().trim()
            val addr = etAddress.text.toString().trim()
            if (name.isNotEmpty() && phone.isNotEmpty() && addr.isNotEmpty()) {
                onSave(name, phone, addr)
                dialog.dismiss()
            } else {
                Toast.makeText(context, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
