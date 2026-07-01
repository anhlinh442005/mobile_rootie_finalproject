package com.veganbeauty.app.features.auth;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.veganbeauty.app.data.local.entities.UserEntity;
import com.veganbeauty.app.data.repository.AuthRepository;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AuthViewModel extends ViewModel {

    private final AuthRepository authRepository;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final MutableLiveData<AuthState> _loginState = new MutableLiveData<>();
    public final LiveData<AuthState> loginState = _loginState;

    private final MutableLiveData<AuthState> _registerState = new MutableLiveData<>();
    public final LiveData<AuthState> registerState = _registerState;

    public AuthViewModel(AuthRepository authRepository) {
        this.authRepository = authRepository;
    }

    public void login(String emailOrPhone, String password) {
        if (emailOrPhone == null || emailOrPhone.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            _loginState.setValue(new AuthState.Error("Vui lòng nhập đầy đủ thông tin."));
            return;
        }

        _loginState.setValue(new AuthState.Loading());
        executor.execute(() -> {
            try {
                UserEntity user = authRepository.login(emailOrPhone, password);
                if (user != null) {
                    _loginState.postValue(new AuthState.Success(user));
                } else {
                    _loginState.postValue(new AuthState.Error("Tài khoản hoặc mật khẩu không đúng."));
                }
            } catch (Exception e) {
                _loginState.postValue(new AuthState.Error("Đã xảy ra lỗi: " + e.getMessage()));
            }
        });
    }

    public void register(String fullName, String email, String phone, String password) {
        if (fullName == null || fullName.trim().isEmpty() ||
            email == null || email.trim().isEmpty() ||
            phone == null || phone.trim().isEmpty() ||
            password == null || password.trim().isEmpty()) {
            _registerState.setValue(new AuthState.Error("Vui lòng nhập đầy đủ thông tin."));
            return;
        }

        _registerState.setValue(new AuthState.Loading());
        executor.execute(() -> {
            try {
                Object resultObj = authRepository.register(fullName, email, phone, password);
                if (resultObj instanceof UserEntity) {
                    _registerState.postValue(new AuthState.Success((UserEntity) resultObj));
                } else {
                    _registerState.postValue(new AuthState.Error("Đăng ký thất bại."));
                }
            } catch (Exception e) {
                _registerState.postValue(new AuthState.Error("Đã xảy ra lỗi: " + e.getMessage()));
            }
        });
    }

    public static abstract class AuthState {
        public static class Loading extends AuthState {}
        public static class Success extends AuthState {
            private final UserEntity user;
            public Success(UserEntity user) { this.user = user; }
            public UserEntity getUser() { return user; }
        }
        public static class Error extends AuthState {
            private final String message;
            public Error(String message) { this.message = message; }
            public String getMessage() { return message; }
        }
    }
}
