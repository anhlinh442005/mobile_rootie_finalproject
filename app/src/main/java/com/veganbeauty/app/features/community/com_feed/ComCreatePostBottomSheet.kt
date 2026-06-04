package com.veganbeauty.app.features.community.com_feed

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.veganbeauty.app.R

class ComCreatePostBottomSheet : BottomSheetDialogFragment() {
    
    companion object {
        const val TAG = "ComCreatePostBottomSheet"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.com_bottom_sheet_create_post, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        view.findViewById<View>(R.id.llCreateReel).setOnClickListener {
            Toast.makeText(context, "Thước phim đang được phát triển", Toast.LENGTH_SHORT).show()
            dismiss()
        }
        
        view.findViewById<View>(R.id.llCreateArticle).setOnClickListener {
            dismiss()
            activity?.supportFragmentManager?.beginTransaction()
                ?.setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right, android.R.anim.slide_in_left, android.R.anim.slide_out_right)
                ?.replace(R.id.main_container, CommunityCreatePostFragment())
                ?.addToBackStack(null)
                ?.commit()
        }
        
        view.findViewById<View>(R.id.llCreateStory).setOnClickListener {
            Toast.makeText(context, "Tin đang được phát triển", Toast.LENGTH_SHORT).show()
            dismiss()
        }
        
        view.findViewById<View>(R.id.llCreateHighlight).setOnClickListener {
            Toast.makeText(context, "Tin nổi bật đang được phát triển", Toast.LENGTH_SHORT).show()
            dismiss()
        }
        
        view.findViewById<View>(R.id.llCreateLive).setOnClickListener {
            Toast.makeText(context, "Phát trực tiếp đang được phát triển", Toast.LENGTH_SHORT).show()
            dismiss()
        }
        
        view.findViewById<View>(R.id.llCreateAi).setOnClickListener {
            Toast.makeText(context, "Tính năng AI đang được phát triển", Toast.LENGTH_SHORT).show()
            dismiss()
        }
    }
}
