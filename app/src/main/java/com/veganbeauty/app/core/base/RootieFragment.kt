package com.veganbeauty.app.core.base

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment

abstract class RootieFragment : Fragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI(view)
        observeViewModel()
    }

    abstract fun setupUI(view: View)

    open fun observeViewModel() {
        // Observe ViewModel data
    }
}
