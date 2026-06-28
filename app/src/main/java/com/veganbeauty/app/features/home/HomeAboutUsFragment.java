package com.veganbeauty.app.features.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.veganbeauty.app.R;
import com.veganbeauty.app.core.base.RootieFragment;
import com.veganbeauty.app.utils.NavAppUtils;

public class HomeAboutUsFragment extends RootieFragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.home_about_us_fragment, container, false);
    }

    @Override
    public void setupUI(@NonNull View view) {
        NavAppUtils.setupNavApp(this, view, R.id.nav_home);
        HomeHeaderHelper.setup(this, view);
    }

    @Override
    protected void observeViewModel() {
        // Not used
    }
}
