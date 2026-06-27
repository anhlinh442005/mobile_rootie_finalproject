package com.veganbeauty.app.features.community.blog;

import androidx.annotation.NonNull;
import java.io.Serializable;

public class BlogPost implements Serializable {
    @NonNull
    private String title;
    @NonNull
    private String description;
    @NonNull
    private String date;
    @NonNull
    private String imageUrl;
    @NonNull
    private String category;
    @NonNull
    private String doctorName;
    @NonNull
    private String doctorAvatar;
    @NonNull
    private String doctorBio;
    @NonNull
    private String content;

    public BlogPost(@NonNull String title, @NonNull String description, @NonNull String date, @NonNull String imageUrl, @NonNull String category, @NonNull String doctorName, @NonNull String doctorAvatar, @NonNull String doctorBio, @NonNull String content) {
        this.title = title;
        this.description = description;
        this.date = date;
        this.imageUrl = imageUrl;
        this.category = category;
        this.doctorName = doctorName;
        this.doctorAvatar = doctorAvatar;
        this.doctorBio = doctorBio;
        this.content = content;
    }

    @NonNull
    public String getTitle() { return title; }
    public void setTitle(@NonNull String title) { this.title = title; }

    @NonNull
    public String getDescription() { return description; }
    public void setDescription(@NonNull String description) { this.description = description; }

    @NonNull
    public String getDate() { return date; }
    public void setDate(@NonNull String date) { this.date = date; }

    @NonNull
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(@NonNull String imageUrl) { this.imageUrl = imageUrl; }

    @NonNull
    public String getCategory() { return category; }
    public void setCategory(@NonNull String category) { this.category = category; }

    @NonNull
    public String getDoctorName() { return doctorName; }
    public void setDoctorName(@NonNull String doctorName) { this.doctorName = doctorName; }

    @NonNull
    public String getDoctorAvatar() { return doctorAvatar; }
    public void setDoctorAvatar(@NonNull String doctorAvatar) { this.doctorAvatar = doctorAvatar; }

    @NonNull
    public String getDoctorBio() { return doctorBio; }
    public void setDoctorBio(@NonNull String doctorBio) { this.doctorBio = doctorBio; }

    @NonNull
    public String getContent() { return content; }
    public void setContent(@NonNull String content) { this.content = content; }
}
