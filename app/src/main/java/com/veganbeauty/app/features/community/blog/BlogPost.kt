package com.veganbeauty.app.features.community.blog

import java.io.Serializable

data class BlogPost(
    val title: String,
    val description: String,
    val date: String,
    val imageUrl: String,
    val category: String,
    val doctorName: String = "",
    val doctorAvatar: String = "",
    val doctorBio: String = "",
    val content: String = ""
) : Serializable
