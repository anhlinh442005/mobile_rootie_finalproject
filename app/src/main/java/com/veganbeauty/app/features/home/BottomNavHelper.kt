package com.veganbeauty.app.features.home

import android.graphics.Color
import android.graphics.Typeface
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.veganbeauty.app.R
import com.veganbeauty.app.features.profile.AccountProfileFragment
import com.veganbeauty.app.features.shop.product.list.ShopListFragment

object BottomNavHelper {

  private const val ACTIVE_COLOR = "#677559"
  private const val INACTIVE_COLOR = "#DDDFC4"

  fun setup(
      fragment: Fragment,
      root: View,
      activeTabId: Int,
      onTabSelected: (Int) -> Unit
  ) {
    val tabs = listOf(
        R.id.nav_home to R.id.nav_home,
        R.id.nav_shop to R.id.nav_shop,
        R.id.nav_myskin to R.id.nav_myskin,
        R.id.nav_community to R.id.nav_community,
        R.id.nav_account to R.id.nav_account
    )

    tabs.forEach { (viewId, _) ->
      root.findViewById<android.view.ViewGroup>(viewId)?.setOnClickListener {
        if (viewId != activeTabId) {
            val isLoggedIn = com.veganbeauty.app.data.local.ProfileSession.isLoggedIn(root.context)
            if (!isLoggedIn && viewId == R.id.nav_account) {
                showLoginRequiredDialog(root.context)
                return@setOnClickListener
            }
            onTabSelected(viewId)
        }
      }
    }

    highlightTab(root, activeTabId)
  }

  fun highlightTab(root: View, activeTabId: Int) {
    val tabIds = listOf(
        R.id.nav_home,
        R.id.nav_shop,
        R.id.nav_myskin,
        R.id.nav_community,
        R.id.nav_account
    )

    tabIds.forEach { tabId ->
      val tab = root.findViewById<android.view.ViewGroup>(tabId) ?: return@forEach
      val icon = tab.getChildAt(0) as? ImageView
      val label = tab.getChildAt(1) as? TextView
      val isActive = tabId == activeTabId
      val color = if (isActive) ACTIVE_COLOR else INACTIVE_COLOR

      if (tabId == R.id.nav_myskin) {
          icon?.clearColorFilter()
          val ivMySkin = tab.findViewById<ImageView>(R.id.ivMySkin)
          val vMySkinShadow = tab.findViewById<View>(R.id.vMySkinShadow)
          
          // Cancel previous animators if any
          (ivMySkin?.getTag(R.id.nav_myskin) as? android.animation.Animator)?.cancel()
          (vMySkinShadow?.getTag(R.id.nav_myskin) as? android.animation.Animator)?.cancel()

          if (isActive) {
              ivMySkin?.setImageResource(R.drawable.ic_skin_mainbar)
              ivMySkin?.scaleX = 1f
              ivMySkin?.scaleY = 1f
              
              vMySkinShadow?.visibility = View.VISIBLE
              val shadowAnim = android.animation.ObjectAnimator.ofPropertyValuesHolder(
                  vMySkinShadow,
                  android.animation.PropertyValuesHolder.ofFloat(View.SCALE_X, 1.0f, 1.3f, 1.0f),
                  android.animation.PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.0f, 1.3f, 1.0f),
                  android.animation.PropertyValuesHolder.ofFloat(View.ALPHA, 0.6f, 0.1f, 0.6f)
              ).apply {
                  duration = 2000
                  repeatCount = android.animation.ObjectAnimator.INFINITE
                  start()
              }
              vMySkinShadow?.setTag(R.id.nav_myskin, shadowAnim)
          } else {
              ivMySkin?.setImageResource(R.drawable.ic_skin_mainbar_nonactive)
              vMySkinShadow?.visibility = View.GONE
              vMySkinShadow?.scaleX = 1f
              vMySkinShadow?.scaleY = 1f
              
              val iconAnim = android.animation.ObjectAnimator.ofPropertyValuesHolder(
                  ivMySkin,
                  android.animation.PropertyValuesHolder.ofFloat(View.SCALE_X, 1.0f, 1.1f, 1.0f),
                  android.animation.PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.0f, 1.1f, 1.0f)
              ).apply {
                  duration = 2000
                  repeatCount = android.animation.ObjectAnimator.INFINITE
                  start()
              }
              ivMySkin?.setTag(R.id.nav_myskin, iconAnim)
          }
          label?.setTextColor(Color.parseColor(color))
          label?.setTypeface(null, if (isActive) Typeface.BOLD else Typeface.NORMAL)
      } else {
          icon?.setColorFilter(Color.parseColor(color))
          if (isActive) {
              icon?.setBackgroundResource(R.drawable.shape_nav_active_bg)
          } else {
              icon?.background = null
          }
          label?.setTextColor(Color.parseColor(color))
          label?.setTypeface(null, if (isActive) Typeface.BOLD else Typeface.NORMAL)
      }
    }
  }

  fun showLoginRequiredDialog(context: android.content.Context) {
      val dialogView = android.view.LayoutInflater.from(context).inflate(R.layout.dialog_login_required, null)
      val dialog = androidx.appcompat.app.AlertDialog.Builder(context)
          .setView(dialogView)
          .create()
          
      val btnConfirm = dialogView.findViewById<android.view.View>(R.id.btnConfirmLogin)
      val btnCancel = dialogView.findViewById<android.view.View>(R.id.btnCancelLogin)
      
      btnConfirm.setOnClickListener {
          dialog.dismiss()
          val intent = android.content.Intent(context, com.veganbeauty.app.features.home.welcome.HomeWelcomeActivity::class.java)
          intent.putExtra("DIRECT_LOGIN", true)
          intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
          context.startActivity(intent)
      }
      
      btnCancel.setOnClickListener {
          dialog.dismiss()
      }
      
      dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
      dialog.show()
  }

  fun navigate(fragment: Fragment, tabId: Int) {
    val isLoggedIn = com.veganbeauty.app.data.local.ProfileSession.isLoggedIn(fragment.requireContext())
    val target =
        when (tabId) {
          R.id.nav_home -> HomeFragment()
          R.id.nav_shop -> com.veganbeauty.app.features.shop.home.ShopHomeFragment()
          R.id.nav_myskin -> com.veganbeauty.app.features.myskin.MySkinFragment()
          R.id.nav_community -> if (isLoggedIn) com.veganbeauty.app.features.community.com_feed.ComLoadingFragment() else com.veganbeauty.app.features.community.beauty_hub.CommunityBeautyHubFragment()
          R.id.nav_account -> AccountProfileFragment()
          else -> null
        }

        target?.let {
          fragment.parentFragmentManager.beginTransaction()
              .replace(R.id.main_container, it)
              .commit()
        }
  }
}
