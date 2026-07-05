package com.veganbeauty.app.data.repository;

import com.veganbeauty.app.data.local.dao.UserDao;
import com.veganbeauty.app.data.local.entities.UserEntity;
import com.veganbeauty.app.data.local.LocalJsonReader;
import com.veganbeauty.app.data.remote.FirestoreService;

import android.content.Context;

import java.security.MessageDigest;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

public class AuthRepository {

    private final UserDao userDao;
    private final Context appContext;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public AuthRepository(UserDao userDao, Context context) {
        this.userDao = userDao;
        this.appContext = context.getApplicationContext();
    }

    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(password.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return password; // Fallback, not secure but to keep it running if digest fails
        }
    }

    // Making this synchronous for Java compatibility, you should call this in a background thread
    public UserEntity login(String emailOrPhone, String password) throws Exception {
        String hashedPassword = hashPassword(password);
        boolean isEmail = android.util.Patterns.EMAIL_ADDRESS.matcher(emailOrPhone).matches();

        // 1) Local Room trước — nhanh, hoạt động cả khi mạng chậm/offline
        UserEntity localUser = isEmail
                ? userDao.getUserByEmailAndPasswordSync(emailOrPhone, hashedPassword)
                : userDao.getUserByPhoneAndPasswordSync(emailOrPhone, hashedPassword);
        if (localUser == null) {
            localUser = isEmail
                    ? userDao.getUserByEmailAndPasswordSync(emailOrPhone, password)
                    : userDao.getUserByPhoneAndPasswordSync(emailOrPhone, password);
        }
        if (localUser != null) {
            return localUser;
        }

        UserEntity assetUser = authenticateFromAssets(emailOrPhone, hashedPassword, password, isEmail);
        if (assetUser != null) {
            try {
                userDao.insertUserSync(assetUser);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return assetUser;
        }

        // 2) Firestore — fallback khi local chưa có hoặc mật khẩu đổi trên cloud
        try {
            FirestoreService firestoreService = new FirestoreService();
            UserEntity firebaseUser = firestoreService.authenticateUser(emailOrPhone, hashedPassword, password, isEmail);
            if (firebaseUser != null) {
                try {
                    userDao.insertUserSync(firebaseUser);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return firebaseUser;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private UserEntity authenticateFromAssets(String emailOrPhone, String hashedPassword, String passwordPlain, boolean isEmail) {
        for (UserEntity candidate : new LocalJsonReader(appContext).getUsers()) {
            boolean identityMatch = isEmail
                    ? emailOrPhone.equalsIgnoreCase(candidate.getEmail())
                    : emailOrPhone.equals(candidate.getPhone());
            if (!identityMatch) {
                continue;
            }
            String stored = candidate.getPassword() != null ? candidate.getPassword() : "";
            if (stored.equals(hashedPassword) || stored.equals(passwordPlain)) {
                return candidate;
            }
        }
        return null;
    }

    // Making this synchronous for Java compatibility, you should call this in a background thread
    public Object register(String fullName, String email, String phone, String password) throws Exception {
        UserEntity existingUserByEmail = !email.isEmpty() ? userDao.getUserByEmailSync(email) : null;
        if (existingUserByEmail != null) {
            throw new Exception("Email đã tồn tại.");
        }
        UserEntity existingUserByPhone = !phone.isEmpty() ? userDao.getUserByPhoneSync(phone) : null;
        if (existingUserByPhone != null) {
            throw new Exception("Số điện thoại đã tồn tại.");
        }

        String userId = "test@example.com".equals(email) ? "test_001" : UUID.randomUUID().toString();
        String hashedPassword = hashPassword(password);

        UserEntity newUser = new UserEntity(userId, fullName, fullName, email, phone, hashedPassword);

        userDao.insertUserSync(newUser);
        
        executor.execute(() -> {
            try {
                new FirestoreService().saveUser(newUser);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        
        return newUser; // Using Object return type to mimic Kotlin's Result wrapping if needed, but returning UserEntity directly here
    }
}
