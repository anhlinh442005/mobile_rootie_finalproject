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
      root.findViewById<LinearLayout>(viewId)?.setOnClickListener {
        if (viewId != activeTabId) {
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
      val tab = root.findViewById<LinearLayout>(tabId) ?: return@forEach
      val icon = tab.getChildAt(0) as? ImageView
      val label = tab.getChildAt(1) as? TextView
      val isActive = tabId == activeTabId
      val color = if (isActive) ACTIVE_COLOR else INACTIVE_COLOR

      icon?.setColorFilter(Color.parseColor(color))
      label?.setTextColor(Color.parseColor(color))
      label?.setTypeface(null, if (isActive) Typeface.BOLD else Typeface.NORMAL)
    }
  }

  fun navigate(fragment: Fragment, tabId: Int) {
    val target =
        when (tabId) {
          R.id.nav_home -> HomeFragment()
          R.id.nav_shop -> ShopListFragment()
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
