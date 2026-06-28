package com.veganbeauty.app.utils;



import android.view.View;



import androidx.fragment.app.Fragment;



import com.veganbeauty.app.features.home.BottomNavHelper;



public class NavAppUtils {

    public static void setupNavApp(Fragment fragment, View view, int activeTabId) {

        BottomNavHelper.setup(fragment, view, activeTabId, tabId -> BottomNavHelper.navigate(fragment, tabId));

    }

}


