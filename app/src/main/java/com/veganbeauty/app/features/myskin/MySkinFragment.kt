package com.veganbeauty.app.features.myskin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.veganbeauty.app.R
import com.veganbeauty.app.core.base.RootieFragment
import com.veganbeauty.app.utils.NavAppUtils

class MySkinFragment : RootieFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_my_skin, container, false)
    }

    override fun setupUI(view: View) {
        NavAppUtils.setupNavApp(this, view, R.id.nav_myskin)
    }

    override fun observeViewModel() {
        // Not used
    }
}
