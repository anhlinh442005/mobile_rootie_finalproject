package com.veganbeauty.app.utils

import android.widget.ImageView
import coil.load
import coil.transform.CircleCropTransformation

object AvatarLoader {
    fun loadAvatar(imageView: ImageView, avatarUrl: String?) {
        if (avatarUrl.isNullOrEmpty()) {
            imageView.load(com.veganbeauty.app.R.drawable.img_avatar) {
                transformations(CircleCropTransformation())
            }
            return
        }
        val finalUrl = if (avatarUrl.startsWith("file://")) {
            val path = avatarUrl.substring(7)
            val file = java.io.File(path)
            if (file.exists()) {
                "file://${file.absolutePath}?t=${file.lastModified()}"
            } else {
                avatarUrl
            }
        } else {
            avatarUrl
        }
        imageView.load(finalUrl) {
            crossfade(true)
            transformations(CircleCropTransformation())
            placeholder(android.R.color.darker_gray)
            error(com.veganbeauty.app.R.drawable.img_avatar)
        }
    }
}
