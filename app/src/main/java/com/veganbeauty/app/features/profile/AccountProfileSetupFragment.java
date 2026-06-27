package com.veganbeauty.app.features.profile;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.veganbeauty.app.R;
import com.veganbeauty.app.core.base.RootieFragment;
import com.veganbeauty.app.data.local.ProfileSession;
import com.veganbeauty.app.databinding.AccountProfileSetupBinding;
import com.veganbeauty.app.features.home.HomePolicyFragment;
import com.veganbeauty.app.features.home.welcome.HomeWelcomeActivity;

import java.util.ArrayList;
import java.util.List;

public class AccountProfileSetupFragment extends RootieFragment {

    private AccountProfileSetupBinding _binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        _binding = AccountProfileSetupBinding.inflate(inflater, container, false);
        return _binding.getRoot();
    }

    @Override
    protected void setupUI(@NonNull View view) {
        Context context = requireContext();

        _binding.btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        ViewGroup navAccount = view.findViewById(R.id.nav_account);
        if (navAccount != null) {
            ImageView icon = (ImageView) navAccount.getChildAt(0);
            TextView label = (TextView) navAccount.getChildAt(1);

            if (icon != null) {
                icon.setColorFilter(Color.parseColor("#677559"));
            }

            if (label != null) {
                label.setTextColor(Color.parseColor("#677559"));
                label.setTypeface(null, Typeface.BOLD);
            }
        }

        _binding.btnNotification.setOnClickListener(v -> Toast.makeText(context, "Không có thông báo mới", Toast.LENGTH_SHORT).show());

        _binding.btnAccountSecurity.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, new AccountProfileSecurityFragment())
                    .addToBackStack(null)
                    .commit();
        });

        _binding.btnAddress.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, new AccountProfileAddressFragment())
                    .addToBackStack(null)
                    .commit();
        });

        _binding.btnBankAccount.setOnClickListener(v -> Toast.makeText(context, "Tài khoản / Thẻ ngân hàng (Đang phát triển)", Toast.LENGTH_SHORT).show());

        updateSwitchUI(_binding.switchChatContainer, _binding.switchChatThumb, isChatBubbleEnabled(context));
        _binding.switchChatContainer.setOnClickListener(v -> {
            boolean nextState = !isChatBubbleEnabled(context);
            setChatBubbleEnabled(context, nextState);
            updateSwitchUI(_binding.switchChatContainer, _binding.switchChatThumb, nextState);
            Toast.makeText(context, nextState ? "Đã bật bong bóng trợ lý AI nổi" : "Đã tắt bong bóng trợ lý AI", Toast.LENGTH_SHORT).show();
        });

        _binding.btnNotificationSettings.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, new AccountProfileNotiSettingFragment())
                    .addToBackStack(null)
                    .commit();
        });

        updateSwitchUI(_binding.switchPrivacyContainer, _binding.switchPrivacyThumb, isPrivateModeEnabled(context));
        _binding.switchPrivacyContainer.setOnClickListener(v -> {
            boolean nextState = !isPrivateModeEnabled(context);
            setPrivateModeEnabled(context, nextState);
            updateSwitchUI(_binding.switchPrivacyContainer, _binding.switchPrivacyThumb, nextState);
            Toast.makeText(context, nextState ? "Đã kích hoạt chế độ riêng tư" : "Đã tắt chế độ riêng tư", Toast.LENGTH_SHORT).show();
        });

        final boolean[] isLanguageExpanded = {false};
        _binding.btnLanguage.setOnClickListener(v -> {
            isLanguageExpanded[0] = !isLanguageExpanded[0];
            _binding.expandableLanguage.setVisibility(isLanguageExpanded[0] ? View.VISIBLE : View.GONE);
            _binding.ivLanguageChevron.animate().rotation(isLanguageExpanded[0] ? 90f : 0f).setDuration(200).start();
        });

        SharedPreferences prefs = context.getSharedPreferences("rootie_profile_prefs", Context.MODE_PRIVATE);
        String savedLang = prefs.getString("app_language", "Tiếng Việt");
        if (savedLang == null) savedLang = "Tiếng Việt";

        _binding.tvCurrentLanguage.setText(savedLang);
        if ("English".equals(savedLang)) {
            _binding.rbLangEn.setChecked(true);
        } else {
            _binding.rbLangVi.setChecked(true);
        }

        _binding.rgLanguage.setOnCheckedChangeListener((group, checkedId) -> {
            String lang = (checkedId == R.id.rb_lang_vi) ? "Tiếng Việt" : "English";
            _binding.tvCurrentLanguage.setText(lang);
            prefs.edit().putString("app_language", lang).apply();
            Toast.makeText(context, "Đã chuyển ngôn ngữ sang " + lang, Toast.LENGTH_SHORT).show();
        });

        _binding.btnHelpCenter.setOnClickListener(v -> Toast.makeText(context, "Trung tâm hỗ trợ (Đang phát triển)", Toast.LENGTH_SHORT).show());
        _binding.btnCommunityGuidelines.setOnClickListener(v -> Toast.makeText(context, "Tiêu chuẩn cộng đồng (Đang phát triển)", Toast.LENGTH_SHORT).show());
        _binding.btnTerms.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, new HomePolicyFragment())
                    .addToBackStack(null)
                    .commit();
        });

        final boolean[] isRatingExpanded = {false};
        _binding.btnRateApp.setOnClickListener(v -> {
            isRatingExpanded[0] = !isRatingExpanded[0];
            _binding.expandableRating.setVisibility(isRatingExpanded[0] ? View.VISIBLE : View.GONE);
            _binding.ivRateChevron.animate().rotation(isRatingExpanded[0] ? 90f : 0f).setDuration(200).start();
        });

        List<ImageView> stars = new ArrayList<>();
        stars.add(_binding.star1);
        stars.add(_binding.star2);
        stars.add(_binding.star3);
        stars.add(_binding.star4);
        stars.add(_binding.star5);

        for (int index = 0; index < stars.size(); index++) {
            int finalIndex = index;
            stars.get(index).setOnClickListener(v -> {
                for (int i = 0; i < 5; i++) {
                    if (i <= finalIndex) {
                        stars.get(i).setColorFilter(Color.parseColor("#FFD700"));
                    } else {
                        stars.get(i).setColorFilter(Color.parseColor("#D1D6D2"));
                    }
                }
                Toast.makeText(context, "Cảm ơn bạn đã đánh giá " + (finalIndex + 1) + " sao cho Rootie!", Toast.LENGTH_SHORT).show();
            });
        }

        _binding.btnLogout.setOnClickListener(v -> {
            ProfileSession.setLoggedIn(context, false);
            prefs.edit().putLong("last_login", 0L).apply();

            Toast.makeText(context, "Đã đăng xuất thành công", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(getActivity(), HomeWelcomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            if (getActivity() != null) {
                getActivity().finish();
            }
        });
    }

    private void updateSwitchUI(FrameLayout container, ImageView thumb, boolean enabled) {
        if (enabled) {
            container.setBackgroundResource(R.drawable.ic_switch_track_on);
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) thumb.getLayoutParams();
            lp.gravity = Gravity.CENTER_VERTICAL | Gravity.END;
            lp.setMarginStart(0);
            lp.setMarginEnd((int) (2 * getResources().getDisplayMetrics().density));
            thumb.setLayoutParams(lp);
        } else {
            container.setBackgroundResource(R.drawable.ic_switch_track_off);
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) thumb.getLayoutParams();
            lp.gravity = Gravity.CENTER_VERTICAL | Gravity.START;
            lp.setMarginEnd(0);
            lp.setMarginStart((int) (2 * getResources().getDisplayMetrics().density));
            thumb.setLayoutParams(lp);
        }
    }

    private boolean isChatBubbleEnabled(Context context) {
        return context.getSharedPreferences("rootie_profile_prefs", Context.MODE_PRIVATE)
                .getBoolean("chat_bubble_enabled", true);
    }

    private void setChatBubbleEnabled(Context context, boolean enabled) {
        context.getSharedPreferences("rootie_profile_prefs", Context.MODE_PRIVATE)
                .edit()
                .putBoolean("chat_bubble_enabled", enabled)
                .apply();
    }

    private boolean isPrivateModeEnabled(Context context) {
        return context.getSharedPreferences("rootie_profile_prefs", Context.MODE_PRIVATE)
                .getBoolean("private_mode_enabled", false);
    }

    private void setPrivateModeEnabled(Context context, boolean enabled) {
        context.getSharedPreferences("rootie_profile_prefs", Context.MODE_PRIVATE)
                .edit()
                .putBoolean("private_mode_enabled", enabled)
                .apply();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        _binding = null;
    }
}
