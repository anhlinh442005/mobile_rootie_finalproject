import os
import json

assets_dir = r"d:\ANHLINH\AndroidProjects\ROOTIE\app\src\main\assets"
entities_dir = r"d:\ANHLINH\AndroidProjects\ROOTIE\app\src\main\java\com\veganbeauty\app\data\local\entities"
db_file = r"d:\ANHLINH\AndroidProjects\ROOTIE\app\src\main\java\com\veganbeauty\app\data\local\RootieDatabase.java"

json_files = [f for f in os.listdir(assets_dir) if f.endswith(".json")]

# Map json files to existing tables
json_to_table = {
    "community_blog.json": "community_blogs",
    "community_posts.json": "community_posts",
    "community_reels_fb.json": "reels",
    "community_video_yt.json": "explore_videos",
    "ingredient.json": "ingredients",
    "orders.json": "orders",
    "products.json": "products",
    "rootie_stores.json": "stores",
    "skin_history.json": "skin_history",
    "users.json": "users",
    "User_com_post_memory.json": "user_memory"
}

new_entities = []

for json_file in json_files:
    if json_file not in json_to_table:
        table_name = json_file.replace(".json", "").lower()
        entity_name = "".join([word.capitalize() for word in table_name.split("_")]) + "Entity"
        
        entity_path = os.path.join(entities_dir, f"{entity_name}.java")
        if not os.path.exists(entity_path):
            code = f"""package com.veganbeauty.app.data.local.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "{table_name}")
public class {entity_name} {{
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public String rawData;
}}
"""
            with open(entity_path, "w") as f:
                f.write(code)
            print(f"Created {entity_name}")
        
        new_entities.append(entity_name)

print("New entities:", new_entities)
