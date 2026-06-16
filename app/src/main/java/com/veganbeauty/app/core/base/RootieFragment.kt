package com.veganbeauty.app.core.base

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.view.ViewGroup
import android.view.Gravity
import android.util.TypedValue
import android.graphics.Typeface
import android.graphics.Color
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import com.veganbeauty.app.R
import com.veganbeauty.app.data.repository.NotificationRepository
import com.veganbeauty.app.features.account.notification.AccountNotificationFragment

abstract class RootieFragment : Fragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI(view)
        
        if (!shouldSkipNotificationSync()) {
            injectNotificationButtonIfNeeded(view)
            try {
                setupNotificationBellAndBadge(view)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        observeViewModel()
    }

    abstract fun setupUI(view: View)

    open fun observeViewModel() {
        // Observe ViewModel data
    }

    private fun shouldSkipNotificationSync(): Boolean {
        val className = this.javaClass.simpleName
        val fullName = this.javaClass.name
        
        // Skip community fragments (com_ / community)
        if (className.startsWith("Com", ignoreCase = true) || 
            fullName.contains("community", ignoreCase = true)) {
            return true
        }
        
        // Skip shop fragments (shop_ / shop)
        if (className.startsWith("Shop", ignoreCase = true) || 
            fullName.contains("shop", ignoreCase = true)) {
            return true
        }
        
        // Skip AccountNotificationFragment itself, to avoid infinite self-navigation/redundant UI
        if (className == "AccountNotificationFragment") {
            return true
        }
        
        return false
    }

    private fun injectNotificationButtonIfNeeded(view: View) {
        val headerIds = listOf(
            "clHeader", "topBar", "toolbar", "header", 
            "skin_header", "skin_history_header", "skin_scan_header", 
            "branch_header", "booking_header", "skin_detail_header", 
            "skin_scan_result_header", "rl_header"
        )
        var headerView: ViewGroup? = null
        for (idStr in headerIds) {
            val resId = resources.getIdentifier(idStr, "id", requireContext().packageName)
            if (resId != 0) {
                val found = view.findViewById<View>(resId)
                if (found is ViewGroup) {
                    headerView = found
                    break
                }
            }
        }
        
        if (headerView == null) return
        
        // Check if there is already a notification button in the view tree
        val notificationButtonIds = listOf(
            "home_header_notification_btn",
            "btnNotification",
            "btn_notification",
            "ivNotification",
            "iv_notification"
        )
        
        var hasNotificationBtn = false
        for (idStr in notificationButtonIds) {
            val resId = resources.getIdentifier(idStr, "id", requireContext().packageName)
            if (resId != 0) {
                if (view.findViewById<View>(resId) != null) {
                    hasNotificationBtn = true
                    break
                }
            }
        }
        
        if (hasNotificationBtn) return
        
        // No notification button found, let's inject one!
        val ctx = requireContext()
        val density = ctx.resources.displayMetrics.density
        
        val notificationContainer = FrameLayout(ctx).apply {
            id = R.id.btnNotification
            layoutParams = ViewGroup.LayoutParams(
                (40 * density).toInt(),
                (40 * density).toInt()
            )
            val outValue = TypedValue()
            if (ctx.theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, outValue, true) && outValue.resourceId != 0) {
                setBackgroundResource(outValue.resourceId)
            }
            isClickable = true
            isFocusable = true
            clipChildren = false
            clipToPadding = false
        }
        
        val bellIcon = ImageView(ctx).apply {
            layoutParams = FrameLayout.LayoutParams(
                (26 * density).toInt(),
                (26 * density).toInt()
            ).apply {
                gravity = Gravity.CENTER
            }
            setImageResource(R.drawable.home_ic_notification)
            val tintColor = Color.parseColor("#3E4D44")
            setColorFilter(tintColor)
        }
        notificationContainer.addView(bellIcon)
        
        val size = (14 * density).toInt()
        val badge = TextView(ctx).apply {
            id = R.id.viewNotificationBadge
            layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, size).apply {
                gravity = Gravity.TOP or Gravity.END
                topMargin = (2 * density).toInt()
                rightMargin = (2 * density).toInt()
            }
            setMinWidth(size)
            setPadding((4 * density).toInt(), 0, (4 * density).toInt(), 0)
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 8f)
            setTypeface(null, Typeface.BOLD)
            setBackgroundResource(R.drawable.home_bg_notification_badge)
            visibility = View.GONE
        }
        notificationContainer.addView(badge)
        
        // Add to header view
        if (headerView is ConstraintLayout) {
            try {
                // Ensure all children of the ConstraintLayout have a valid ID to prevent ConstraintSet.clone crash
                for (i in 0 until headerView.childCount) {
                    val child = headerView.getChildAt(i)
                    if (child.id == View.NO_ID) {
                        child.id = View.generateViewId()
                    }
                }

                val cartContainer = headerView.findViewById<View>(R.id.flCartContainer)
                
                headerView.addView(notificationContainer)
                val set = ConstraintSet()
                set.clone(headerView)
                
                if (cartContainer != null) {
                    // For ShopHomeFragment: insert notification button to the left of Cart
                    notificationContainer.layoutParams = ConstraintLayout.LayoutParams(
                        (40 * density).toInt(),
                        (40 * density).toInt()
                    ).apply {
                        marginEnd = (8 * density).toInt()
                    }
                    
                    set.connect(notificationContainer.id, ConstraintSet.END, cartContainer.id, ConstraintSet.START)
                    set.connect(notificationContainer.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
                    set.connect(notificationContainer.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
                } else {
                    // Standard header: align to end of parent
                    notificationContainer.layoutParams = ConstraintLayout.LayoutParams(
                        (40 * density).toInt(),
                        (40 * density).toInt()
                    ).apply {
                        marginEnd = (16 * density).toInt()
                    }
                    
                    set.connect(notificationContainer.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
                    set.connect(notificationContainer.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
                    set.connect(notificationContainer.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
                }
                set.applyTo(headerView)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else if (headerView is LinearLayout) {
            // Adjust layout_marginEnd of TextView to center the title
            for (i in 0 until headerView.childCount) {
                val child = headerView.getChildAt(i)
                if (child is TextView) {
                    val lp = child.layoutParams as? LinearLayout.LayoutParams
                    if (lp != null && lp.weight > 0) {
                        lp.marginEnd = 0
                        child.layoutParams = lp
                    }
                }
            }
            headerView.addView(notificationContainer)
        } else if (headerView is RelativeLayout) {
            val lp = RelativeLayout.LayoutParams(
                (40 * density).toInt(),
                (40 * density).toInt()
            ).apply {
                addRule(RelativeLayout.ALIGN_PARENT_END)
                addRule(RelativeLayout.CENTER_VERTICAL)
                marginEnd = (16 * density).toInt()
            }
            notificationContainer.layoutParams = lp
            headerView.addView(notificationContainer)
        }
    }

    private fun setupNotificationBellAndBadge(view: View) {
        // Find existing notification button in the view tree
        val notificationButtonIds = listOf(
            "home_header_notification_btn",
            "btnNotification",
            "btn_notification",
            "ivNotification",
            "iv_notification"
        )
        
        var notificationBtn: View? = null
        for (idStr in notificationButtonIds) {
            val resId = resources.getIdentifier(idStr, "id", requireContext().packageName)
            if (resId != 0) {
                val found = view.findViewById<View>(resId)
                if (found != null) {
                    notificationBtn = found
                    break
                }
            }
        }
        
        if (notificationBtn == null) return
        
        // Find pre-existing notification badge in the view tree
        val badgeIds = listOf(
            "home_header_notification_badge",
            "viewNotificationBadge",
            "view_notification_badge",
            "notificationBadge",
            "notification_badge"
        )
        
        var badgeView: View? = null
        for (idStr in badgeIds) {
            val resId = resources.getIdentifier(idStr, "id", requireContext().packageName)
            if (resId != 0) {
                val found = view.findViewById<View>(resId)
                if (found != null) {
                    badgeView = found
                    break
                }
            }
        }
        
        var badgeTextView: TextView? = null
        
        if (badgeView is TextView) {
            badgeTextView = badgeView
        } else if (badgeView != null) {
            // Found a badge view but it's a plain View/dot. Let's replace it with a TextView badge.
            val parent = badgeView.parent as? ViewGroup
            if (parent != null) {
                val index = parent.indexOfChild(badgeView)
                val layoutParams = badgeView.layoutParams
                
                val newBadge = TextView(requireContext()).apply {
                    id = badgeView.id
                    this.layoutParams = layoutParams
                    gravity = Gravity.CENTER
                    setTextColor(Color.WHITE)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 8f)
                    setTypeface(null, Typeface.BOLD)
                    setBackgroundResource(R.drawable.home_bg_notification_badge)
                }
                
                parent.removeViewAt(index)
                parent.addView(newBadge, index)
                badgeTextView = newBadge
            }
        } else {
            // No badge view found.
            // If the button is not a ViewGroup (e.g. it is a simple ImageView), let's wrap it in a FrameLayout.
            if (notificationBtn !is ViewGroup) {
                val parent = notificationBtn.parent as? ViewGroup
                if (parent != null) {
                    val index = parent.indexOfChild(notificationBtn)
                    val originalParams = notificationBtn.layoutParams
                    
                    val ctx = requireContext()
                    val density = ctx.resources.displayMetrics.density
                    
                    val originalWidth = originalParams.width
                    val originalHeight = originalParams.height
                    
                    val containerWidth = if (originalWidth <= 0 || originalWidth < (40 * density).toInt()) (40 * density).toInt() else originalWidth
                    val containerHeight = if (originalHeight <= 0 || originalHeight < (40 * density).toInt()) (40 * density).toInt() else originalHeight
                    
                    val container = FrameLayout(ctx).apply {
                        id = View.generateViewId()
                        layoutParams = originalParams.apply {
                            width = containerWidth
                            height = containerHeight
                        }
                        clipChildren = false
                        clipToPadding = false
                    }
                    
                    notificationBtn.layoutParams = FrameLayout.LayoutParams(
                        if (originalWidth > 0) originalWidth else (24 * density).toInt(),
                        if (originalHeight > 0) originalHeight else (24 * density).toInt()
                    ).apply {
                        gravity = Gravity.CENTER
                    }
                    
                    parent.removeViewAt(index)
                    container.addView(notificationBtn)
                    parent.addView(container, index)
                    notificationBtn = container
                }
            }
            
            // Now that notificationBtn is a ViewGroup (either original or wrapped FrameLayout), let's add the badge
            if (notificationBtn is ViewGroup) {
                val ctx = requireContext()
                val density = ctx.resources.displayMetrics.density
                
                // If it is a FrameLayout, let's make sure it is at least 40dp x 40dp and center its child to prevent badge clipping.
                if (notificationBtn is FrameLayout) {
                    val lp = notificationBtn.layoutParams
                    if (lp.width == ViewGroup.LayoutParams.WRAP_CONTENT || lp.width < (40 * density).toInt()) {
                        lp.width = (40 * density).toInt()
                    }
                    if (lp.height == ViewGroup.LayoutParams.WRAP_CONTENT || lp.height < (40 * density).toInt()) {
                        lp.height = (40 * density).toInt()
                    }
                    notificationBtn.layoutParams = lp
                    
                    if (notificationBtn.childCount > 0) {
                        val child = notificationBtn.getChildAt(0)
                        val childLp = child.layoutParams
                        if (childLp is FrameLayout.LayoutParams) {
                            childLp.gravity = Gravity.CENTER
                            child.layoutParams = childLp
                        }
                    }
                }
                
                val size = (14 * density).toInt()
                val newBadge = TextView(ctx).apply {
                    id = R.id.viewNotificationBadge
                    layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, size).apply {
                        gravity = Gravity.TOP or Gravity.END
                        topMargin = (2 * density).toInt()
                        rightMargin = (2 * density).toInt()
                    }
                    setMinWidth(size)
                    setPadding((4 * density).toInt(), 0, (4 * density).toInt(), 0)
                    gravity = Gravity.CENTER
                    setTextColor(Color.WHITE)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 8f)
                    setTypeface(null, Typeface.BOLD)
                    setBackgroundResource(R.drawable.home_bg_notification_badge)
                }
                notificationBtn.addView(newBadge)
                badgeTextView = newBadge
            }
        }
        
        // Prevent clipping by setting clipChildren and clipToPadding to false on all parents and the button container itself
        if (notificationBtn is ViewGroup) {
            notificationBtn.clipChildren = false
            notificationBtn.clipToPadding = false
        }
        var p = notificationBtn.parent
        while (p is ViewGroup) {
            p.clipChildren = false
            p.clipToPadding = false
            p = p.parent
        }
        
        // Bind click listener to navigate to AccountNotificationFragment
        notificationBtn.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_container, AccountNotificationFragment())
                .addToBackStack(null)
                .commit()
        }
        
        if (badgeTextView != null) {
            val finalBadge = badgeTextView
            viewLifecycleOwner.lifecycleScope.launch {
                NotificationRepository.getInstance(requireContext())
                    .unreadCount
                    .collect { count ->
                        if (count > 0) {
                            finalBadge.text = count.toString()
                            finalBadge.visibility = View.VISIBLE
                        } else {
                            finalBadge.visibility = View.GONE
                        }
                    }
            }
        }
    }
}
