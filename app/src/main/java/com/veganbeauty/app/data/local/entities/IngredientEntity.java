package com.veganbeauty.app.data.local.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.veganbeauty.app.data.local.ProductConverters;

import java.util.List;

@Entity(tableName = "ingredients")
@TypeConverters(ProductConverters.class)
public class IngredientEntity {
    @PrimaryKey
    @NonNull
    private String slug;
    @NonNull
    private String name;
    @NonNull
    private String scientificName;
    @NonNull
    private String image;
    @NonNull
    private String origin;
    @NonNull
    private String description;
    @NonNull
    private String uses;
    @NonNull
    private List<String> types;

    public IngredientEntity(@NonNull String slug, @NonNull String name, @NonNull String scientificName, @NonNull String image, @NonNull String origin, @NonNull String description, @NonNull String uses, @NonNull List<String> types) {
        this.slug = slug;
        this.name = name;
        this.scientificName = scientificName;
        this.image = image;
        this.origin = origin;
        this.description = description;
        this.uses = uses;
        this.types = types;
    }

    @NonNull
    public String getSlug() { return slug; }
    public void setSlug(@NonNull String slug) { this.slug = slug; }

    @NonNull
    public String getName() { return name; }
    public void setName(@NonNull String name) { this.name = name; }

    @NonNull
    public String getScientificName() { return scientificName; }
    public void setScientificName(@NonNull String scientificName) { this.scientificName = scientificName; }

    @NonNull
    public String getImage() { return image; }
    public void setImage(@NonNull String image) { this.image = image; }

    @NonNull
    public String getOrigin() { return origin; }
    public void setOrigin(@NonNull String origin) { this.origin = origin; }

    @NonNull
    public String getDescription() { return description; }
    public void setDescription(@NonNull String description) { this.description = description; }

    @NonNull
    public String getUses() { return uses; }
    public void setUses(@NonNull String uses) { this.uses = uses; }

    @NonNull
    public List<String> getTypes() { return types; }
    public void setTypes(@NonNull List<String> types) { this.types = types; }
}
