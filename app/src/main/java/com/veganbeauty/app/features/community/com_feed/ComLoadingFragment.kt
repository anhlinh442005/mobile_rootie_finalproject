package com.veganbeauty.app.features.community.com_feed

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.veganbeauty.app.R

class ComLoadingFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.com_fragment_loading, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Show loading screen for 1500ms, then replace with CommunityFeedFragment
        Handler(Looper.getMainLooper()).postDelayed({
            if (isAdded) {
                parentFragmentManager.beginTransaction()
                    .setCustomAnimations(
                        android.R.anim.fade_in,
                        android.R.anim.fade_out
                    )
                    .replace(R.id.main_container, CommunityFeedFragment())
                    .commit()
            }
        }, 1500)
    }
}
