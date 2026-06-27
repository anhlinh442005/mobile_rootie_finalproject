package com.veganbeauty.app.utils;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.veganbeauty.app.R;
import com.veganbeauty.app.features.community.com_feed.ComLoadingFragment;
import com.veganbeauty.app.features.home.HomeFragment;
import com.veganbeauty.app.features.myskin.MySkinFragment;
import com.veganbeauty.app.features.profile.AccountProfileFragment;
import com.veganbeauty.app.features.shop.home.ShopHomeFragment;

import java.util.Arrays;
import java.util.List;

public class NavAppUtils {
    public static void setupNavApp(Fragment fragment, View view, int activeTabId) {
        List<Integer> navIds = Arrays.asList(R.id.nav_home, R.id.nav_shop, R.id.nav_myskin, R.id.nav_community, R.id.nav_account);
        Context context = fragment.requireContext();
        int activeColor = ContextCompat.getColor(context, R.color.primary);
        int inactiveColor = ContextCompat.getColor(context, R.color.nav_icon_color);

        for (int id : navIds) {
            View navItem = view.findViewById(id);
            if (navItem instanceof ViewGroup) {
                ViewGroup group = (ViewGroup) navItem;
                ImageView imageView = null;
                TextView textView = null;
                
                for (int i = 0; i < group.getChildCount(); i++) {
                    View child = group.getChildAt(i);
                    if (child instanceof ImageView) imageView = (ImageView) child;
                    if (child instanceof TextView) textView = (TextView) child;
                }
                
                if (id == activeTabId) {
                    if (imageView != null) imageView.setColorFilter(activeColor);
                    if (textView != null) textView.setTextColor(activeColor);
                } else {
                    if (imageView != null) imageView.setColorFilter(inactiveColor);
                    if (textView != null) textView.setTextColor(inactiveColor);
                }
                
                navItem.setOnClickListener(v -> {
                    if (id != activeTabId) {
                        FragmentTransaction transaction = fragment.getParentFragmentManager().beginTransaction();
                        transaction.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out);
                        
                        if (id == R.id.nav_home) {
                            transaction.replace(R.id.main_container, new HomeFragment());
                        } else if (id == R.id.nav_shop) {
                            transaction.replace(R.id.main_container, new ShopHomeFragment());
                        } else if (id == R.id.nav_myskin) {
                            transaction.replace(R.id.main_container, new MySkinFragment());
                        } else if (id == R.id.nav_community) {
                            transaction.replace(R.id.main_container, new ComLoadingFragment());
                        } else if (id == R.id.nav_account) {
                            transaction.replace(R.id.main_container, new AccountProfileFragment());
                        }
                        
                        transaction.commit();
                    }
                });
            }
        }
    }
}
