package com.veganbeauty.app.features.profile

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.veganbeauty.app.R
import com.veganbeauty.app.core.base.RootieFragment
import com.veganbeauty.app.data.local.ProfileSession
import com.veganbeauty.app.databinding.AccountProfileSecurityBinding

class AccountProfileSecurityFragment : RootieFragment() {

    private var _binding: AccountProfileSecurityBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = AccountProfileSecurityBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun setupUI(view: View) {
        val context = requireContext()

        // Back button
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Notification button
        binding.btnNotification.setOnClickListener {
            Toast.makeText(context, "Không có thông báo mới", Toast.LENGTH_SHORT).show()
        }

        // Load values from ProfileSession
        val username = ProfileSession.getUsername(context)
        val phone = ProfileSession.getPhone(context)
        val email = ProfileSession.getEmail(context)
        val isFastLogin = ProfileSession.isFastLoginEnabled(context)

        // Bind values to UI with security masking
        binding.tvUsernameVal.text = username
        binding.tvPhoneVal.text = maskPhone(phone)
        binding.tvEmailVal.text = maskEmail(email)

        var fastLoginState = isFastLogin
        fun updateSwitchUI(enabled: Boolean) {
            if (enabled) {
                binding.switchFastLoginContainer.setBackgroundResource(R.drawable.ic_switch_track_on)
                val lp = binding.switchFastLoginThumb.layoutParams as android.widget.FrameLayout.LayoutParams
                lp.gravity = android.view.Gravity.CENTER_VERTICAL or android.view.Gravity.END
                lp.marginStart = 0
                lp.marginEnd = (2 * resources.displayMetrics.density).toInt()
                binding.switchFastLoginThumb.layoutParams = lp
            } else {
                binding.switchFastLoginContainer.setBackgroundResource(R.drawable.ic_switch_track_off)
                val lp = binding.switchFastLoginThumb.layoutParams as android.widget.FrameLayout.LayoutParams
                lp.gravity = android.view.Gravity.CENTER_VERTICAL or android.view.Gravity.START
                lp.marginEnd = 0
                lp.marginStart = (2 * resources.displayMetrics.density).toInt()
                binding.switchFastLoginThumb.layoutParams = lp
            }
        }
        updateSwitchUI(fastLoginState)

        val prefs = context.getSharedPreferences("RootieQuizPrefs", android.content.Context.MODE_PRIVATE)
        var floatingChatState = prefs.getBoolean("SKIN_AI_FLOATING_CHAT_ENABLED", true)
        fun updateFloatingSwitchUI(enabled: Boolean) {
            if (enabled) {
                binding.switchFloatingChatContainer.setBackgroundResource(R.drawable.ic_switch_track_on)
                val lp = binding.switchFloatingChatThumb.layoutParams as android.widget.FrameLayout.LayoutParams
                lp.gravity = android.view.Gravity.CENTER_VERTICAL or android.view.Gravity.END
                lp.marginStart = 0
                lp.marginEnd = (2 * resources.displayMetrics.density).toInt()
                binding.switchFloatingChatThumb.layoutParams = lp
            } else {
                binding.switchFloatingChatContainer.setBackgroundResource(R.drawable.ic_switch_track_off)
                val lp = binding.switchFloatingChatThumb.layoutParams as android.widget.FrameLayout.LayoutParams
                lp.gravity = android.view.Gravity.CENTER_VERTICAL or android.view.Gravity.START
                lp.marginEnd = 0
                lp.marginStart = (2 * resources.displayMetrics.density).toInt()
                binding.switchFloatingChatThumb.layoutParams = lp
            }
        }
        updateFloatingSwitchUI(floatingChatState)

        // Listeners for Account items
        binding.btnMyProfile.setOnClickListener {
            // Navigate to Edit Profile screen
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_container, AccountProfileEditFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.btnUsername.setOnClickListener {
            val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_change_username, null)
            val dialog = android.app.AlertDialog.Builder(context)
                .setView(dialogView)
                .create()
            
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            
            val etUsername = dialogView.findViewById<android.widget.EditText>(R.id.etUsername)
            val btnCancel = dialogView.findViewById<TextView>(R.id.btnCancel)
            val btnSave = dialogView.findViewById<TextView>(R.id.btnSave)
            
            etUsername.setText(username)
            
            btnCancel.setOnClickListener {
                dialog.dismiss()
            }
            
            btnSave.setOnClickListener {
                val newName = etUsername.text.toString().trim()
                if (newName.isNotEmpty()) {
                    binding.tvUsernameVal.text = newName
                    ProfileSession.setUsername(context, newName)
                    Toast.makeText(context, "Đã cập nhật tên người dùng", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            
            dialog.show()
        }

        binding.btnPhone.setOnClickListener {
            Toast.makeText(context, "Số điện thoại: $phone", Toast.LENGTH_SHORT).show()
        }

        binding.btnEmail.setOnClickListener {
            Toast.makeText(context, "Email nhận hóa đơn: $email", Toast.LENGTH_SHORT).show()
        }

        binding.btnSocial.setOnClickListener {
            Toast.makeText(context, "Liên kết mạng xã hội (Đang phát triển)", Toast.LENGTH_SHORT).show()
        }

        binding.btnChangePassword.setOnClickListener {
            val sheet = AccountChangePasswordSheet()
            sheet.show(parentFragmentManager, "AccountChangePasswordSheet")
        }

        binding.btnPasskey.setOnClickListener {
            Toast.makeText(context, "Passkey (Đang phát triển)", Toast.LENGTH_SHORT).show()
        }

        // Fast login switch click listener
        binding.switchFastLoginContainer.setOnClickListener {
            fastLoginState = !fastLoginState
            updateSwitchUI(fastLoginState)
            ProfileSession.setFastLoginEnabled(context, fastLoginState)
            val msg = if (fastLoginState) "Đã bật đăng nhập nhanh" else "Đã tắt đăng nhập nhanh"
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }

        // Floating chat switch click listener
        binding.switchFloatingChatContainer.setOnClickListener {
            floatingChatState = !floatingChatState
            updateFloatingSwitchUI(floatingChatState)
            prefs.edit().putBoolean("SKIN_AI_FLOATING_CHAT_ENABLED", floatingChatState).apply()
            
            // Instantly update the visibility of the floating chat head in MainActivity
            activity?.findViewById<View>(R.id.skin_ai_floating_chat_head)?.let { chatHead ->
                chatHead.visibility = if (floatingChatState) View.VISIBLE else View.GONE
            }
            
            val msg = if (floatingChatState) "Đã bật Trợ lý Rootie AI nổi" else "Đã tắt Trợ lý Rootie AI nổi"
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }

        // Listeners for Security items
        binding.btnCheckActivity.setOnClickListener {
            Toast.makeText(context, "Kiểm tra hoạt động (Đang phát triển)", Toast.LENGTH_SHORT).show()
        }

        binding.btnManageDevices.setOnClickListener {
            Toast.makeText(context, "Quản lý thiết bị đăng nhập (Đang phát triển)", Toast.LENGTH_SHORT).show()
        }

        // Highlight the "Tài khoản" tab as active in bottom navigation
        view.findViewById<android.view.ViewGroup>(R.id.nav_account)?.let { navAccount ->
            val icon = navAccount.getChildAt(0) as? ImageView
            val label = navAccount.getChildAt(1) as? TextView

            // Set active green color tint to the icon (#677559)
            icon?.setColorFilter(Color.parseColor("#677559"))

            // Set active green color and bold style to the text label
            label?.setTextColor(Color.parseColor("#677559"))
            label?.setTypeface(null, Typeface.BOLD)
        }
    }

    private fun maskPhone(phone: String): String {
        if (phone.length < 2) return phone
        return "******" + phone.takeLast(2)
    }

    private fun maskEmail(email: String): String {
        val parts = email.split("@")
        if (parts.size != 2) return email
        val prefix = parts[0]
        val domain = parts[1]
        if (prefix.length <= 2) return email
        val maskedPrefix = prefix.first() + "*".repeat(prefix.length - 2) + prefix.last()
        return "$maskedPrefix@$domain"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
