import json
import random

file_path = "app/src/main/assets/community_video_yt.json"

with open(file_path, "r", encoding="utf-8") as f:
    data = json.load(f)

for item in data:
    if "likes" not in item:
        item["likes"] = random.randint(1000, 500000)

with open(file_path, "w", encoding="utf-8") as f:
    json.dump(data, f, ensure_ascii=False, indent=2)

print("Added likes to", len(data), "videos.")
