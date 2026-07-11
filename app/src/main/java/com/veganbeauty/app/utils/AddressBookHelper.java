package com.veganbeauty.app.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.veganbeauty.app.data.local.ProfileSession;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Sổ địa chỉ theo từng tài khoản.
 * Tài khoản mới = trống. Nhãn (Nhà riêng / Văn phòng / Trường học / …) do người dùng tự đặt.
 */
public final class AddressBookHelper {

    public static final String[] SUGGESTED_LABELS = {
            "Nhà riêng", "Văn phòng", "Trường học", "Khác"
    };

    private static final String PREFS_NAME = "rootie_profile_prefs";
    private static final String KEY_SAVED_LIST = "saved_addresses_list_json";
    private static final String KEY_USER_PREFIX = "user_addresses_json_";
    private static final String KEY_EMPTY_DEFAULT_MIGRATION = "address_book_dynamic_v2";

    // Legacy keys (xóa khi migrate)
    private static final String KEY_HOME_NAME = "addr_home_name";
    private static final String KEY_HOME_PHONE = "addr_home_phone";
    private static final String KEY_HOME_ADDR = "addr_home_addr";
    private static final String KEY_OFFICE_NAME = "addr_office_name";
    private static final String KEY_OFFICE_PHONE = "addr_office_phone";
    private static final String KEY_OFFICE_ADDR = "addr_office_addr";
    private static final String KEY_DEFAULT_TYPE = "addr_default_type";
    private static final String KEY_OLD_MIGRATION = "address_book_empty_default_v1";

    private AddressBookHelper() {
    }

    public static final class AddressEntry {
        @NonNull public String id;
        @NonNull public String label;
        @NonNull public String name;
        @NonNull public String phone;
        @NonNull public String address;
        public boolean isDefault;

        public AddressEntry(@NonNull String id, @NonNull String label, @NonNull String name,
                            @NonNull String phone, @NonNull String address, boolean isDefault) {
            this.id = id;
            this.label = label;
            this.name = name;
            this.phone = phone;
            this.address = address;
            this.isDefault = isDefault;
        }

        @NonNull
        public static AddressEntry create(@NonNull String label, @NonNull String name,
                                         @NonNull String phone, @NonNull String address,
                                         boolean isDefault) {
            return new AddressEntry(UUID.randomUUID().toString(), label, name, phone, address, isDefault);
        }
    }

    public static void loadForUser(@Nullable Context context, @Nullable String userId) {
        if (context == null || userId == null || userId.trim().isEmpty()) {
            return;
        }
        Context app = context.getApplicationContext();
        migrateToDynamicOnce(app);
        String uid = userId.trim();

        SharedPreferences prefs = app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if (prefs.contains(KEY_USER_PREFIX + uid)) {
            applyToSession(app, readUserStoredAddresses(app, uid));
            return;
        }

        List<AddressEntry> empty = new ArrayList<>();
        applyToSession(app, empty);
        writeUserStoredAddresses(app, uid, empty);
    }

    public static void ensureLoadedForCurrentUser(@Nullable Context context) {
        if (context == null || !ProfileSession.isLoggedIn(context)) {
            return;
        }
        migrateToDynamicOnce(context);
        String userId = ProfileSession.getUserId(context);
        if (userId == null || userId.trim().isEmpty()) {
            userId = ProfileSession.getCurrentUserId(context);
        }
        if (userId == null || userId.trim().isEmpty()) {
            return;
        }
        String uid = userId.trim();
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if (prefs.contains(KEY_USER_PREFIX + uid)) {
            List<AddressEntry> current = getSessionAddresses(context);
            if (current.isEmpty()) {
                applyToSession(context, readUserStoredAddresses(context, uid));
            }
            return;
        }
        List<AddressEntry> current = getSessionAddresses(context);
        if (!current.isEmpty()) {
            writeUserStoredAddresses(context, uid, current);
            return;
        }
        loadForUser(context, uid);
    }

    /** Reset sổ địa chỉ mẫu / slot cố định cũ → trống hoàn toàn. */
    private static void migrateToDynamicOnce(@NonNull Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if (prefs.getBoolean(KEY_EMPTY_DEFAULT_MIGRATION, false)) {
            return;
        }
        SharedPreferences.Editor editor = prefs.edit();
        for (Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
            String key = entry.getKey();
            if (key != null && key.startsWith(KEY_USER_PREFIX)) {
                editor.remove(key);
            }
        }
        editor.remove(KEY_HOME_NAME)
                .remove(KEY_HOME_PHONE)
                .remove(KEY_HOME_ADDR)
                .remove(KEY_OFFICE_NAME)
                .remove(KEY_OFFICE_PHONE)
                .remove(KEY_OFFICE_ADDR)
                .remove(KEY_DEFAULT_TYPE)
                .remove(KEY_SAVED_LIST)
                .remove(KEY_OLD_MIGRATION)
                .putBoolean(KEY_EMPTY_DEFAULT_MIGRATION, true);
        editor.apply();
        ProfileSession.setAddress(context, "");
    }

    @NonNull
    public static List<AddressEntry> getSessionAddresses(@Nullable Context context) {
        List<AddressEntry> list = new ArrayList<>();
        if (context == null) return list;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String jsonStr = prefs.getString(KEY_SAVED_LIST, null);
        if (jsonStr == null || jsonStr.trim().isEmpty()) {
            return list;
        }
        try {
            JSONArray array = new JSONArray(jsonStr);
            for (int i = 0; i < array.length(); i++) {
                AddressEntry entry = parseEntry(array.getJSONObject(i), i);
                if (entry != null) list.add(entry);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        ensureSingleDefault(list);
        return list;
    }

    public static void saveSessionAddresses(@Nullable Context context, @Nullable List<AddressEntry> list) {
        if (context == null) return;
        List<AddressEntry> safe = list != null ? new ArrayList<>(list) : new ArrayList<>();
        ensureSingleDefault(safe);
        applyToSession(context, safe);

        String userId = ProfileSession.getUserId(context);
        if (userId == null || userId.trim().isEmpty()) {
            userId = ProfileSession.getCurrentUserId(context);
        }
        if (userId != null && !userId.trim().isEmpty()) {
            writeUserStoredAddresses(context, userId.trim(), safe);
        }
    }

    @Nullable
    public static AddressEntry findById(@Nullable Context context, @Nullable String id) {
        if (id == null || id.trim().isEmpty()) return null;
        for (AddressEntry entry : getSessionAddresses(context)) {
            if (id.equals(entry.id)) return entry;
        }
        return null;
    }

    /** Thêm địa chỉ mới. Trả về entry đã lưu. */
    @NonNull
    public static AddressEntry addAddress(@Nullable Context context, @NonNull String label,
                                          @NonNull String name, @NonNull String phone,
                                          @NonNull String address, boolean makeDefault) {
        List<AddressEntry> list = getSessionAddresses(context);
        boolean isDefault = makeDefault || list.isEmpty();
        if (isDefault) {
            for (AddressEntry e : list) e.isDefault = false;
        }
        AddressEntry entry = AddressEntry.create(
                label.trim().isEmpty() ? "Địa chỉ" : label.trim(),
                name.trim(), phone.trim(), address.trim(), isDefault
        );
        list.add(entry);
        saveSessionAddresses(context, list);
        return entry;
    }

    public static void updateAddress(@Nullable Context context, @NonNull AddressEntry updated) {
        if (context == null) return;
        List<AddressEntry> list = getSessionAddresses(context);
        for (int i = 0; i < list.size(); i++) {
            if (updated.id.equals(list.get(i).id)) {
                if (updated.isDefault) {
                    for (AddressEntry e : list) e.isDefault = false;
                }
                list.set(i, updated);
                saveSessionAddresses(context, list);
                return;
            }
        }
    }

    public static void deleteById(@Nullable Context context, @Nullable String id) {
        if (context == null || id == null) return;
        List<AddressEntry> list = getSessionAddresses(context);
        AddressEntry removed = null;
        for (int i = 0; i < list.size(); i++) {
            if (id.equals(list.get(i).id)) {
                removed = list.remove(i);
                break;
            }
        }
        if (removed != null && removed.isDefault && !list.isEmpty()) {
            list.get(0).isDefault = true;
        }
        saveSessionAddresses(context, list);
    }

    public static void setDefaultById(@Nullable Context context, @Nullable String id) {
        if (context == null || id == null) return;
        List<AddressEntry> list = getSessionAddresses(context);
        boolean found = false;
        for (AddressEntry e : list) {
            if (id.equals(e.id)) {
                found = true;
                break;
            }
        }
        if (!found) return;
        for (AddressEntry e : list) {
            e.isDefault = id.equals(e.id);
        }
        saveSessionAddresses(context, list);
    }

    private static void applyToSession(@NonNull Context context, @NonNull List<AddressEntry> list) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        // Dọn slot cố định cũ
        editor.remove(KEY_HOME_NAME)
                .remove(KEY_HOME_PHONE)
                .remove(KEY_HOME_ADDR)
                .remove(KEY_OFFICE_NAME)
                .remove(KEY_OFFICE_PHONE)
                .remove(KEY_OFFICE_ADDR)
                .remove(KEY_DEFAULT_TYPE);

        if (list.isEmpty()) {
            editor.remove(KEY_SAVED_LIST).commit();
            ProfileSession.setAddress(context, "");
            return;
        }

        try {
            JSONArray array = new JSONArray();
            AddressEntry defaultEntry = null;
            for (AddressEntry entry : list) {
                JSONObject obj = new JSONObject();
                obj.put("id", entry.id);
                obj.put("label", entry.label);
                obj.put("name", entry.name);
                obj.put("phone", entry.phone);
                obj.put("address", entry.address);
                obj.put("isDefault", entry.isDefault);
                array.put(obj);
                if (entry.isDefault) defaultEntry = entry;
            }
            if (defaultEntry == null) {
                list.get(0).isDefault = true;
                defaultEntry = list.get(0);
            }
            editor.putString(KEY_SAVED_LIST, array.toString()).commit();
            ProfileSession.setAddress(context, defaultEntry.address);
        } catch (Exception e) {
            e.printStackTrace();
            editor.commit();
        }
    }

    @Nullable
    private static AddressEntry parseEntry(@NonNull JSONObject obj, int index) {
        String address = obj.optString("address", "").trim();
        if (address.isEmpty()) return null;

        String id = obj.optString("id", "").trim();
        if (id.isEmpty()) id = UUID.randomUUID().toString();

        String label = obj.optString("label", "").trim();
        if (label.isEmpty()) {
            String type = obj.optString("type", "").trim();
            if ("HOME".equals(type)) label = "Nhà riêng";
            else if ("OFFICE".equals(type)) label = "Văn phòng";
            else label = "Địa chỉ";
        }

        return new AddressEntry(
                id,
                label,
                obj.optString("name", "").trim(),
                obj.optString("phone", "").trim(),
                address,
                obj.optBoolean("isDefault", index == 0)
        );
    }

    @NonNull
    private static List<AddressEntry> readUserStoredAddresses(@NonNull Context context, @NonNull String userId) {
        List<AddressEntry> list = new ArrayList<>();
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if (!prefs.contains(KEY_USER_PREFIX + userId)) return list;
        String json = prefs.getString(KEY_USER_PREFIX + userId, "[]");
        if (json == null || json.trim().isEmpty()) return list;
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                AddressEntry entry = parseEntry(array.getJSONObject(i), i);
                if (entry != null) list.add(entry);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        ensureSingleDefault(list);
        return list;
    }

    private static void writeUserStoredAddresses(@NonNull Context context, @NonNull String userId,
                                                 @NonNull List<AddressEntry> list) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        try {
            JSONArray array = new JSONArray();
            for (AddressEntry entry : list) {
                JSONObject obj = new JSONObject();
                obj.put("id", entry.id);
                obj.put("label", entry.label);
                obj.put("name", entry.name);
                obj.put("phone", entry.phone);
                obj.put("address", entry.address);
                obj.put("isDefault", entry.isDefault);
                array.put(obj);
            }
            prefs.edit().putString(KEY_USER_PREFIX + userId, array.toString()).commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void ensureSingleDefault(@NonNull List<AddressEntry> list) {
        if (list.isEmpty()) return;
        int defaultCount = 0;
        for (AddressEntry e : list) {
            if (e.isDefault) defaultCount++;
        }
        if (defaultCount == 1) return;
        for (int i = 0; i < list.size(); i++) {
            list.get(i).isDefault = (i == 0);
        }
    }
}
