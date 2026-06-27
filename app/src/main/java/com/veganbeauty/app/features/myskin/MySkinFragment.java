package com.veganbeauty.app.features.myskin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.veganbeauty.app.R;
import com.veganbeauty.app.core.base.RootieFragment;
import com.veganbeauty.app.data.local.LocalJsonReader;
import com.veganbeauty.app.data.local.ProfileSession;
import com.veganbeauty.app.data.local.entities.StoreEntity;
import com.veganbeauty.app.databinding.SkinFragmentHomeBinding;
import com.veganbeauty.app.features.home.BottomNavHelper;

import java.util.List;

public class MySkinFragment extends RootieFragment {

    private SkinFragmentHomeBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = SkinFragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void setupUI(@NonNull View view) {
        String fullName = ProfileSession.getFullName(requireContext());
        if (fullName != null && !fullName.trim().isEmpty()) {
            String[] parts = fullName.trim().split(" ");
            String firstName = parts[parts.length - 1];
            binding.skinGreetingName.setText("Xin chào, " + firstName);
        }

        BottomNavHelper.setup(
                this,
                view,
                R.id.nav_myskin,
                tabId -> {
                    BottomNavHelper.navigate(this, tabId);
                    return null;
                }
        );

        binding.skinBannerContainer.setOnClickListener(v -> openScanFragment());
        binding.skinShortcutScanAi.setOnClickListener(v -> openScanFragment());
        
        binding.skinShortcutProfile.setOnClickListener(v -> {
            if (!ProfileSession.isLoggedIn(requireContext())) {
                BottomNavHelper.showLoginRequiredDialog(requireContext());
                return;
            }
            getParentFragmentManager().beginTransaction()
                    .setCustomAnimations(
                            android.R.anim.slide_in_left,
                            android.R.anim.fade_out,
                            android.R.anim.fade_in,
                            android.R.anim.slide_out_right
                    )
                    .replace(R.id.main_container, new com.veganbeauty.app.features.profile.SkinAllergyProfileFragment())
                    .addToBackStack(null)
                    .commit();
        });

        binding.skinShortcutRoutine.setOnClickListener(v -> {
            if (!ProfileSession.isLoggedIn(requireContext())) {
                BottomNavHelper.showLoginRequiredDialog(requireContext());
                return;
            }
            getParentFragmentManager().beginTransaction()
                    .setCustomAnimations(
                            android.R.anim.slide_in_left,
                            android.R.anim.fade_out,
                            android.R.anim.fade_in,
                            android.R.anim.slide_out_right
                    )
                    .replace(R.id.main_container, new com.veganbeauty.app.features.routine.SkinTimeRoutineFragment())
                    .addToBackStack(null)
                    .commit();
        });

        binding.skinShortcutReminder.setOnClickListener(v -> {
            if (!ProfileSession.isLoggedIn(requireContext())) {
                BottomNavHelper.showLoginRequiredDialog(requireContext());
                return;
            }
            getParentFragmentManager().beginTransaction()
                    .setCustomAnimations(
                            android.R.anim.slide_in_left,
                            android.R.anim.fade_out,
                            android.R.anim.fade_in,
                            android.R.anim.slide_out_right
                    )
                    .replace(R.id.main_container, new com.veganbeauty.app.features.routine.SkinReminderFragment())
                    .addToBackStack(null)
                    .commit();
        });

        binding.skinShortcutWeather.setOnClickListener(v -> {
            if (!ProfileSession.isLoggedIn(requireContext())) {
                BottomNavHelper.showLoginRequiredDialog(requireContext());
                return;
            }
            getParentFragmentManager().beginTransaction()
                    .setCustomAnimations(
                            android.R.anim.slide_in_left,
                            android.R.anim.fade_out,
                            android.R.anim.fade_in,
                            android.R.anim.slide_out_right
                    )
                    .replace(R.id.main_container, new com.veganbeauty.app.features.weather.SkinWeatherForecastFragment())
                    .addToBackStack(null)
                    .commit();
        });

        LocalJsonReader jsonReader = new LocalJsonReader(requireContext());
        List<StoreEntity> allStores = jsonReader.getAllStores();
        List<StoreEntity> topStores = allStores.size() > 5 ? allStores.subList(0, 5) : allStores;

        binding.rvSkinStores.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        SkinStoreAdapter adapter = new SkinStoreAdapter(topStores, store -> {
            BookingFragment bookingFragment = BookingFragment.newInstance(
                    store.getTenCuaHang(),
                    store.getDiaChiDayDu(),
                    ""
            );
            getParentFragmentManager().beginTransaction()
                    .setCustomAnimations(
                            android.R.anim.slide_in_left,
                            android.R.anim.fade_out,
                            android.R.anim.fade_in,
                            android.R.anim.slide_out_right
                    )
                    .replace(R.id.main_container, bookingFragment)
                    .addToBackStack(null)
                    .commit();
        });
        binding.rvSkinStores.setAdapter(adapter);

        binding.skinStoreSeeAll.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .setCustomAnimations(
                            android.R.anim.slide_in_left,
                            android.R.anim.fade_out,
                            android.R.anim.fade_in,
                            android.R.anim.slide_out_right
                    )
                    .replace(R.id.main_container, new ChooseBranchFragment())
                    .addToBackStack(null)
                    .commit();
        });

        binding.skinBtnBookingHistory.setOnClickListener(v -> {
            if (!ProfileSession.isLoggedIn(requireContext())) {
                BottomNavHelper.showLoginRequiredDialog(requireContext());
                return;
            }
            getParentFragmentManager().beginTransaction()
                    .setCustomAnimations(
                            android.R.anim.slide_in_left,
                            android.R.anim.fade_out,
                            android.R.anim.fade_in,
                            android.R.anim.slide_out_right
                    )
                    .replace(R.id.main_container, new BookingHistoryFragment())
                    .addToBackStack(null)
                    .commit();
        });
    }

    @Override
    protected void observeViewModel() {
        // Not used yet
    }

    private void openScanFragment() {
        getParentFragmentManager().beginTransaction()
                .setCustomAnimations(
                        android.R.anim.slide_in_left,
                        android.R.anim.fade_out,
                        android.R.anim.fade_in,
                        android.R.anim.slide_out_right
                )
                .replace(R.id.main_container, new SkinScanFragment())
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
