package com.veganbeauty.app.features.profile;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.veganbeauty.app.R;
import com.veganbeauty.app.core.base.RootieFragment;
import com.veganbeauty.app.data.local.ProfileSession;
import com.veganbeauty.app.databinding.AccountProfileNotiSettingBinding;
import com.veganbeauty.app.features.home.BottomNavHelper;
import com.veganbeauty.app.features.myskin.SkinDetailHeaderScrollHelper;
import com.veganbeauty.app.features.weather.DailySkinWeatherScheduler;

import java.util.Arrays;
import java.util.List;

public class AccountProfileNotiSettingFragment extends RootieFragment {

    private AccountProfileNotiSettingBinding binding;
    private SkinDetailHeaderScrollHelper headerScrollHelper;
    private ActivityResultLauncher<String> requestNotiPermLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestNotiPermLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            Context ctx = requireContext();
            if (isGranted) {
                ProfileSession.setNotiEnabled(ctx, true);
                updateSwitchUI(binding.switchNotiEnabledContainer, binding.switchNotiEnabledThumb, true);
                syncAllSettingsEnabledState();
                triggerTestNotification("Đã bật thông báo", "Bạn sẽ nhận được các thông báo mới nhất từ Rootie.");
            } else {
                Toast.makeText(ctx, "Quyền thông báo bị từ chối", Toast.LENGTH_SHORT).show();
                ProfileSession.setNotiEnabled(ctx, false);
                updateSwitchUI(binding.switchNotiEnabledContainer, binding.switchNotiEnabledThumb, false);
                syncAllSettingsEnabledState();
                showSystemNotificationSettingsDialog();
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = AccountProfileNotiSettingBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void setupUI(View view) {
        Context ctx = requireContext();
        binding.btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        binding.layoutNotification.getRoot().setOnClickListener(v ->
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.main_container, new com.veganbeauty.app.features.account.notification.AccountNotificationFragment())
                        .addToBackStack(null).commit());

        List<String> freqs = Arrays.asList("Mỗi ngày","Mỗi tuần","Mỗi tháng");
        ArrayAdapter<String> freqAdapter = new ArrayAdapter<>(ctx, android.R.layout.simple_spinner_item, freqs);
        freqAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerFrequency.setAdapter(freqAdapter);

        List<String> timeRanges = Arrays.asList("08:00 - 21:00","09:00 - 18:00","07:00 - 22:00","18:00 - 23:00","Bất cứ lúc nào");
        ArrayAdapter<String> timeAdapter = new ArrayAdapter<>(ctx, android.R.layout.simple_spinner_item, timeRanges);
        timeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerTimeRange.setAdapter(timeAdapter);

        /*
        int freqIdx = freqs.indexOf(ProfileSession.getPromotionFrequency(ctx));
        if (freqIdx >= 0) binding.spinnerFrequency.setSelection(freqIdx);
        int timeIdx = timeRanges.indexOf(ProfileSession.getPromotionTimeRange(ctx));
        if (timeIdx >= 0) binding.spinnerTimeRange.setSelection(timeIdx);

        binding.spinnerFrequency.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) { ProfileSession.setPromotionFrequency(requireContext(), freqs.get(pos)); }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });
        binding.spinnerTimeRange.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) { ProfileSession.setPromotionTimeRange(requireContext(), timeRanges.get(pos)); }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });
        */

        loadSwitchesState();
        setupSwitchListeners();
        BottomNavHelper.setup(this, view, R.id.nav_account, tabId -> BottomNavHelper.navigate(this, tabId));
        setupScrollHideHeader();
        syncAllSettingsEnabledState();
    }

    private void setupScrollHideHeader() {
        int bottomPadding = (int) requireContext().getResources().getDimension(R.dimen.home_nav_bar_height);
        headerScrollHelper = new SkinDetailHeaderScrollHelper(
                binding.rlHeader,
                binding.scrollContent,
                bottomPadding
        );
        headerScrollHelper.attachToNestedScrollView(binding.scrollContent);
    }

    private void loadSwitchesState() {
        Context ctx = requireContext();
        updateSwitchUI(binding.switchNotiEnabledContainer, binding.switchNotiEnabledThumb, ProfileSession.isNotiEnabled(ctx));
        updateSwitchUI(binding.switchSoundContainer, binding.switchSoundThumb, ProfileSession.isSoundEnabled(ctx));
        updateSwitchUI(binding.switchVibrateContainer, binding.switchVibrateThumb, ProfileSession.isVibrateEnabled(ctx));
        updateSwitchUI(binding.switchLockScreenContainer, binding.switchLockScreenThumb, ProfileSession.isLockScreenEnabled(ctx));
        updateSwitchUI(binding.switchOrderStatusContainer, binding.switchOrderStatusThumb, ProfileSession.isOrderStatusEnabled(ctx));
        updateSwitchUI(binding.switchPromotionContainer, binding.switchPromotionThumb, ProfileSession.isPromotionEnabled(ctx));
        updateSwitchUI(binding.switchStaffMessageContainer, binding.switchStaffMessageThumb, ProfileSession.isStaffMessageEnabled(ctx));
        updateSwitchUI(binding.switchSkinWeatherContainer, binding.switchSkinWeatherThumb, ProfileSession.isSkinWeatherNotiEnabled(ctx));
        updateSwitchUI(binding.switchExpiryNotiContainer, binding.switchExpiryNotiThumb, ProfileSession.isNotiExpiryEnabled(ctx));
        updateSwitchUI(binding.switchExpiryWeek1Container, binding.switchExpiryWeek1Thumb, ProfileSession.isNotiExpiryWeek1(ctx));
        updateSwitchUI(binding.switchExpiryWeek2Container, binding.switchExpiryWeek2Thumb, ProfileSession.isNotiExpiryWeek2(ctx));
    }

    private void setupSwitchListeners() {
        Context ctx = requireContext();
        binding.switchNotiEnabledContainer.setOnClickListener(v -> {
            boolean next = !ProfileSession.isNotiEnabled(ctx);
            if (next) {
                if (!areNotificationsAllowed()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) requestNotiPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                    else showSystemNotificationSettingsDialog();
                } else {
                    ProfileSession.setNotiEnabled(ctx, true);
                    updateSwitchUI(binding.switchNotiEnabledContainer, binding.switchNotiEnabledThumb, true);
                    syncAllSettingsEnabledState();
                    triggerTestNotification("Thông báo đã bật", "Bạn sẽ nhận được các thông báo mới nhất từ Rootie.");
                }
            } else {
                ProfileSession.setNotiEnabled(ctx, false);
                updateSwitchUI(binding.switchNotiEnabledContainer, binding.switchNotiEnabledThumb, false);
                syncAllSettingsEnabledState();
            }
        });
        setupSimpleSwitch(binding.switchSoundContainer, binding.switchSoundThumb, () -> ProfileSession.isSoundEnabled(ctx), val -> { ProfileSession.setSoundEnabled(ctx, val); if(val) triggerTestNotification("Âm thanh thông báo","Đã bật âm thanh thông báo."); });
        setupSimpleSwitch(binding.switchVibrateContainer, binding.switchVibrateThumb, () -> ProfileSession.isVibrateEnabled(ctx), val -> { ProfileSession.setVibrateEnabled(ctx, val); if(val) triggerTestNotification("Rung phản hồi","Đã bật rung cho thông báo."); });
        setupSimpleSwitch(binding.switchLockScreenContainer, binding.switchLockScreenThumb, () -> ProfileSession.isLockScreenEnabled(ctx), val -> { ProfileSession.setLockScreenEnabled(ctx, val); if(val) triggerTestNotification("Hiển thị Lock Screen","Thông báo sẽ hiển thị trên màn hình khóa."); });
        setupSimpleSwitch(binding.switchOrderStatusContainer, binding.switchOrderStatusThumb, () -> ProfileSession.isOrderStatusEnabled(ctx), val -> { ProfileSession.setOrderStatusEnabled(ctx, val); if(val) triggerTestNotification("Cập nhật đơn hàng","Trạng thái đơn hàng của bạn sẽ liên tục được cập nhật."); });
        setupSimpleSwitch(binding.switchStaffMessageContainer, binding.switchStaffMessageThumb, () -> ProfileSession.isStaffMessageEnabled(ctx), val -> { ProfileSession.setStaffMessageEnabled(ctx, val); if(val) triggerTestNotification("Tin nhắn nhân viên","Bạn có tin nhắn mới từ tư vấn viên."); });
        setupSimpleSwitch(binding.switchPromotionContainer, binding.switchPromotionThumb, () -> ProfileSession.isPromotionEnabled(ctx), val -> {
            ProfileSession.setPromotionEnabled(ctx, val);
            binding.layoutPromotionOptions.setVisibility(val ? View.VISIBLE : View.GONE);
            if (val) triggerTestNotification("Khuyến mãi cá nhân","Bạn sẽ nhận được voucher ưu đãi phù hợp nhất.");
        });
        binding.switchSkinWeatherContainer.setOnClickListener(v -> {
            if (!ProfileSession.isNotiEnabled(ctx)) return;
            boolean next = !ProfileSession.isSkinWeatherNotiEnabled(ctx);
            ProfileSession.setSkinWeatherNotiEnabled(ctx, next);
            updateSwitchUI(binding.switchSkinWeatherContainer, binding.switchSkinWeatherThumb, next);
            if (next) {
                DailySkinWeatherScheduler.enableAndSync(ctx);
                Toast.makeText(ctx, "Đã bật nhắc thời tiết & da (07:00 mỗi sáng)", Toast.LENGTH_SHORT).show();
            }
            else DailySkinWeatherScheduler.cancelDailyNotification(ctx);
        });
        setupSimpleSwitch(binding.switchExpiryNotiContainer, binding.switchExpiryNotiThumb, () -> ProfileSession.isNotiExpiryEnabled(ctx), val -> {
            ProfileSession.setNotiExpiryEnabled(ctx, val);
            binding.layoutExpiryOptions.setVisibility(val ? View.VISIBLE : View.GONE);
            if (val) triggerTestNotification("Thông báo hết hạn","Đã bật nhắc nhở hạn sử dụng sản phẩm.");
        });
        binding.switchExpiryWeek1Container.setOnClickListener(v -> {
            if (!ProfileSession.isNotiEnabled(ctx) || !ProfileSession.isNotiExpiryEnabled(ctx)) return;
            boolean next = !ProfileSession.isNotiExpiryWeek1(ctx);
            ProfileSession.setNotiExpiryWeek1(ctx, next);
            updateSwitchUI(binding.switchExpiryWeek1Container, binding.switchExpiryWeek1Thumb, next);
        });
        binding.switchExpiryWeek2Container.setOnClickListener(v -> {
            if (!ProfileSession.isNotiEnabled(ctx) || !ProfileSession.isNotiExpiryEnabled(ctx)) return;
            boolean next = !ProfileSession.isNotiExpiryWeek2(ctx);
            ProfileSession.setNotiExpiryWeek2(ctx, next);
            updateSwitchUI(binding.switchExpiryWeek2Container, binding.switchExpiryWeek2Thumb, next);
        });
    }

    private interface BooleanGetter { boolean get(); }
    private interface BooleanSetter { void set(boolean val); }

    private void setupSimpleSwitch(FrameLayout container, ImageView thumb, BooleanGetter getter, BooleanSetter setter) {
        container.setOnClickListener(v -> {
            if (!ProfileSession.isNotiEnabled(requireContext())) return;
            boolean next = !getter.get();
            setter.set(next);
            updateSwitchUI(container, thumb, next);
        });
    }

    private void syncAllSettingsEnabledState() {
        boolean enabled = ProfileSession.isNotiEnabled(requireContext());
        float alpha = enabled ? 1.0f : 0.5f;
        FrameLayout[] switches = {binding.switchSoundContainer,binding.switchVibrateContainer,binding.switchLockScreenContainer,binding.switchOrderStatusContainer,binding.switchPromotionContainer,binding.switchStaffMessageContainer,binding.switchSkinWeatherContainer,binding.switchExpiryNotiContainer};
        for (FrameLayout sw : switches) { sw.setEnabled(enabled); sw.setAlpha(alpha); }
        boolean promoVisible = enabled && ProfileSession.isPromotionEnabled(requireContext());
        binding.layoutPromotionOptions.setVisibility(promoVisible ? View.VISIBLE : View.GONE);
        boolean expiryEnabled = ProfileSession.isNotiExpiryEnabled(requireContext());
        boolean expiryVisible = enabled && expiryEnabled;
        binding.layoutExpiryOptions.setVisibility(expiryVisible ? View.VISIBLE : View.GONE);
        binding.switchExpiryWeek1Container.setEnabled(expiryVisible);
        binding.switchExpiryWeek1Container.setAlpha(expiryVisible ? 1.0f : 0.5f);
        binding.switchExpiryWeek2Container.setEnabled(expiryVisible);
        binding.switchExpiryWeek2Container.setAlpha(expiryVisible ? 1.0f : 0.5f);
    }

    private void updateSwitchUI(FrameLayout container, ImageView thumb, boolean enabled) {
        int margin = (int)(2 * getResources().getDisplayMetrics().density);
        if (enabled) {
            container.setBackgroundResource(container == binding.switchSkinWeatherContainer ? R.drawable.ic_switch_track_on : R.drawable.ic_switch_track_on);
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) thumb.getLayoutParams();
            lp.gravity = Gravity.CENTER_VERTICAL | Gravity.END;
            lp.setMarginStart(0); lp.setMarginEnd(margin);
            thumb.setLayoutParams(lp);
        } else {
            container.setBackgroundResource(R.drawable.ic_switch_track_off);
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) thumb.getLayoutParams();
            lp.gravity = Gravity.CENTER_VERTICAL | Gravity.START;
            lp.setMarginEnd(0); lp.setMarginStart(margin);
            thumb.setLayoutParams(lp);
        }
    }

    private boolean areNotificationsAllowed() {
        Context ctx = requireContext();
        if (!NotificationManagerCompat.from(ctx).areNotificationsEnabled()) return false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            return ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        return true;
    }

    private void showSystemNotificationSettingsDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Cho phép thông báo")
                .setMessage("Thông báo hiện đang bị tắt cho ứng dụng này. Vui lòng bật thông báo trong Cài đặt hệ thống để nhận cập nhật từ Rootie.")
                .setPositiveButton("Cài đặt", (d,w) -> openSystemNotificationSettings())
                .setNegativeButton("Hủy", (d,w) -> d.dismiss()).show();
    }

    private void openSystemNotificationSettings() {
        Context ctx = requireContext();
        Intent intent = new Intent();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, ctx.getPackageName());
        } else {
            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.fromParts("package", ctx.getPackageName(), null));
        }
        try { startActivity(intent); }
        catch (Exception e) {
            Intent fb = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            fb.setData(Uri.fromParts("package", ctx.getPackageName(), null));
            startActivity(fb);
        }
    }

    private void triggerTestNotification(String title, String content) {
        Context ctx = requireContext();
        if (!ProfileSession.isNotiEnabled(ctx) || !areNotificationsAllowed()) return;
        boolean sound = ProfileSession.isSoundEnabled(ctx), vibrate = ProfileSession.isVibrateEnabled(ctx);
        String channelId = "rootie_channel_" + (sound ? "s1" : "s0") + "_" + (vibrate ? "v1" : "v0");
        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(channelId, "Cài đặt thông báo Rootie", sound ? NotificationManager.IMPORTANCE_DEFAULT : NotificationManager.IMPORTANCE_LOW);
            ch.setDescription("Kênh gửi thông báo thử nghiệm từ cài đặt thông báo");
            if (!sound) ch.setSound(null, null);
            ch.enableVibration(vibrate);
            if (!vibrate) ch.setVibrationPattern(new long[]{0L});
            nm.createNotificationChannel(ch);
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx, channelId)
                .setSmallIcon(R.drawable.ic_bell).setContentTitle(title).setContentText(content).setAutoCancel(true);
        if (!sound) { builder.setSound(null); builder.setPriority(NotificationCompat.PRIORITY_LOW); }
        else { builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)); builder.setPriority(NotificationCompat.PRIORITY_DEFAULT); }
        if (!vibrate) builder.setVibrate(new long[]{0L});
        else builder.setDefaults(NotificationCompat.DEFAULT_VIBRATE);
        try { nm.notify(101, builder.build()); } catch (SecurityException e) { e.printStackTrace(); }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (binding != null) {
            loadSwitchesState();
            syncAllSettingsEnabledState();
        }
        Context ctx = getContext();
        if (ctx != null
                && ProfileSession.isNotiEnabled(ctx)
                && ProfileSession.isSkinWeatherNotiEnabled(ctx)) {
            DailySkinWeatherScheduler.scheduleOnly(ctx);
        }
    }

    @Override
    public void observeViewModel() {}

    @Override
    public void onDestroyView() { super.onDestroyView(); binding = null; }
}
