package com.veganbeauty.app.utils

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.veganbeauty.app.R
import com.veganbeauty.app.features.community.com_feed.ComLoadingFragment
import com.veganbeauty.app.features.home.HomeFragment
import com.veganbeauty.app.features.profile.AccountProfileFragment
import com.veganbeauty.app.features.shop.home.ShopHomeFragment

object NavAppUtils {
    @JvmStatic
    fun setupNavApp(fragment: Fragment, view: View, activeTabId: Int) {
        val navIds = listOf(R.id.nav_home, R.id.nav_shop, R.id.nav_myskin, R.id.nav_community, R.id.nav_account)
        val context = fragment.requireContext()
        val activeColor = ContextCompat.getColor(context, R.color.primary)
        val inactiveColor = ContextCompat.getColor(context, R.color.nav_icon_color)

        navIds.forEach { id ->
            val navItem = view.findViewById<View>(id)
            if (navItem != null && navItem is ViewGroup) {
                // Colorize
                var imageView: ImageView? = null
                var textView: TextView? = null
                for (i in 0 until navItem.childCount) {
                    val child = navItem.getChildAt(i)
                    if (child is ImageView) imageView = child
                    if (child is TextView) textView = child
                }
                if (id == activeTabId) {
                    imageView?.setColorFilter(activeColor)
                    textView?.setTextColor(activeColor)
                } else {
                    imageView?.setColorFilter(inactiveColor)
                    textView?.setTextColor(inactiveColor)
                }
                
                // Set Click Listener
                navItem.setOnClickListener {
                    if (id != activeTabId) {
                        val transaction = fragment.parentFragmentManager.beginTransaction()
                        transaction.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                        when (id) {
                            R.id.nav_home -> transaction.replace(R.id.main_container, HomeFragment())
                            R.id.nav_shop -> transaction.replace(R.id.main_container, ShopHomeFragment())
                            R.id.nav_myskin -> transaction.replace(R.id.main_container, com.veganbeauty.app.features.myskin.MySkinFragment())
                            R.id.nav_community -> transaction.replace(R.id.main_container, ComLoadingFragment())
                            R.id.nav_account -> transaction.replace(R.id.main_container, AccountProfileFragment())
                        }
                        transaction.commit()
                    }
                }
            }
        }
    }
}
