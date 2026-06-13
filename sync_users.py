import json
import random
import os

assets_dir = r"d:\ANHLINH\AndroidProjects\Rootie\app\src\main\assets"
users_file = os.path.join(assets_dir, "users.json")

with open(users_file, "r", encoding="utf-8") as f:
    users = json.load(f)

existing_user_ids = {u["user_id"] for u in users if "user_id" in u}

avatars = [
    "https://i.pinimg.com/736x/1a/d8/4b/1ad84b9ab4a1e2ab17c7aab37fcff0a5.jpg",
    "https://i.pinimg.com/736x/b6/d7/cc/b6d7ccd0c1e842825d1976a4ba25df8e.jpg",
    "https://i.pinimg.com/736x/f5/bd/79/f5bd792ccdf6d003c25e1019fd0f5c31.jpg",
    "https://i.pinimg.com/736x/75/60/c5/7560c5e421f79c98d2f072ceaa8bfd59.jpg",
    "https://i.pinimg.com/736x/03/a8/8f/03a88fe45d52512cc174c08f605074e7.jpg",
    "https://api.dicebear.com/7.x/avataaars/svg?seed=458",
    "https://i.pinimg.com/736x/c3/a1/13/c3a113f28c3be85ca7e20a99367a1482.jpg",
    "https://i.pinimg.com/736x/39/a9/3e/39a93e60ff13bc3d4b2f393c891bfc76.jpg"
]

files_to_check = ["community_posts.json", "community_news.json", "community_reels_fb.json"]

new_users = {}

for fname in files_to_check:
    fpath = os.path.join(assets_dir, fname)
    if not os.path.exists(fpath): continue
    
    with open(fpath, "r", encoding="utf-8") as f:
        data = json.load(f)
        
    posts = data if isinstance(data, list) else data.get("posts", [])
    
    for post in posts:
        comments = post.get("comments", [])
        for cmt in comments:
            author = cmt.get("author", {})
            uid = author.get("user_id")
            uname = author.get("username", "User")
            
            if uid and uid not in existing_user_ids and uid not in new_users:
                new_users[uid] = {
                    "username": uname,
                    "user_id": uid,
                    "avatar": author.get("avatar") or random.choice(avatars),
                    "email": f"user{uid}@example.com",
                    "phone": "0123456789",
                    "password": "ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f",
                    "full_name": uname
                }

if new_users:
    users.extend(new_users.values())
    with open(users_file, "w", encoding="utf-8") as f:
        json.dump(users, f, ensure_ascii=False, indent=2)
    print(f"Added {len(new_users)} users.")
else:
    print("No new users to add.")
