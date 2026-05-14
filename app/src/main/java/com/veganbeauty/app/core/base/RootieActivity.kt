package com.veganbeauty.app.core.base

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

abstract class RootieActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupUI()
        observeViewModel()
    }

    abstract fun setupUI()
    
    open fun observeViewModel() {
        // Observe common LiveData like error messages or loading state
    }
}
