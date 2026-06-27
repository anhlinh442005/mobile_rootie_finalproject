package com.veganbeauty.app.data.local.entities;

import androidx.annotation.NonNull;

public class KeyIngredient {
    @NonNull private String name;
    @NonNull private String description;

    public KeyIngredient(@NonNull String name, @NonNull String description) {
        this.name = name;
        this.description = description;
    }

    @NonNull public String getName() { return name; }
    @NonNull public String getDescription() { return description; }
}
