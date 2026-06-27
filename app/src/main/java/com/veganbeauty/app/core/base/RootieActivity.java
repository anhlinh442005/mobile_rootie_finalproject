package com.veganbeauty.app.core.base;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public abstract class RootieActivity extends AppCompatActivity {
    
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupUI();
        observeViewModel();
    }

    protected abstract void setupUI();
    
    protected void observeViewModel() {
        // Observe common LiveData like error messages or loading state
    }
}
