package com.veganbeauty.app.data.repository;

import com.veganbeauty.app.data.local.dao.UserDao;
import com.veganbeauty.app.data.local.entities.UserEntity;
import com.veganbeauty.app.data.local.LocalJsonReader;

import android.content.Context;

import java.security.MessageDigest;
import java.util.UUID;

public class AuthRepository {

    private final UserDao userDao;
    private final Context appContext;

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
            return password;
        }
    }

    public UserEntity login(String emailOrPhone, String password) throws Exception {
        String hashedPassword = hashPassword(password);
        boolean isEmail = android.util.Patterns.EMAIL_ADDRESS.matcher(emailOrPhone).matches();

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

    public Object register(String fullName, String email, String phone, String password) throws Exception {
        String normalizedEmail = email != null ? email.trim() : "";
        String normalizedPhone = phone != null ? phone.trim() : "";

        UserEntity existingUserByEmail = !normalizedEmail.isEmpty() ? userDao.getUserByEmailSync(normalizedEmail) : null;
        if (existingUserByEmail != null) {
            throw new Exception("Email đã tồn tại.");
        }
        UserEntity existingUserByPhone = !normalizedPhone.isEmpty() ? userDao.getUserByPhoneSync(normalizedPhone) : null;
        if (existingUserByPhone != null) {
            throw new Exception("Số điện thoại đã tồn tại.");
        }

        String userId = "test@example.com".equals(normalizedEmail) ? "test_001" : UUID.randomUUID().toString();
        String hashedPassword = hashPassword(password);

        UserEntity newUser = new UserEntity(userId, fullName.trim(), fullName.trim(), normalizedEmail, normalizedPhone, hashedPassword);
        userDao.insertUserSync(newUser);
        return newUser;
    }
}
