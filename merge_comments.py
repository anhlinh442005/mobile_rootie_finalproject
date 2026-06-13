import json
import os

posts_file = r"d:\ANHLINH\AndroidProjects\Rootie\app\src\main\assets\community_posts.json"
comments_file = r"d:\ANHLINH\AndroidProjects\Rootie\app\src\main\assets\community_comments.json"

with open(posts_file, 'r', encoding='utf-8') as f:
    posts_data = json.load(f)

with open(comments_file, 'r', encoding='utf-8') as f:
    comments_data = json.load(f)

posts = posts_data.get("posts", [])
for post in posts:
    post_id = post.get("post_id")
    if post_id in comments_data:
        # Convert the comments structure to the desired format
        # The old structure:
        # "comment_id": "cmt_6977", "user_id": "63214138", "username": "Hùng Bùi", "avatar_url": "...", "timeStr": "4 tuần", "content": "...", "likes_count": 5, "is_author": false, "replies": []
        
        # New structure needed:
        # "comment_id": "...", "post_id": "...", "author": {"username": "...", "user_id": "...", "avatar": "...", "is_anonymous": false}, "content": "...", "created_at": "...", "likes_count": ..., "parent_id": null, "depth": 0
        
        new_comments = []
        old_comments = comments_data[post_id]
        for old_c in old_comments:
            new_c = {
                "comment_id": old_c.get("comment_id", ""),
                "post_id": post_id,
                "author": {
                    "username": old_c.get("username", ""),
                    "user_id": old_c.get("user_id", ""),
                    "avatar": old_c.get("avatar_url", ""),
                    "is_anonymous": False
                },
                "content": old_c.get("content", ""),
                "created_at": "2026-06-12T10:00:00Z", # Fake standard date, or derive from timeStr
                "likes_count": old_c.get("likes_count", 0),
                "parent_id": None,
                "depth": 0
            }
            new_comments.append(new_c)
            # Add replies if any
            for r in old_c.get("replies", []):
                new_r = {
                    "comment_id": old_c.get("comment_id", "") + "_reply_" + r.get("user_id", ""),
                    "post_id": post_id,
                    "author": {
                        "username": r.get("username", ""),
                        "user_id": r.get("user_id", ""),
                        "avatar": r.get("avatar_url", ""),
                        "is_anonymous": False
                    },
                    "content": r.get("content", ""),
                    "created_at": "2026-06-12T11:00:00Z",
                    "likes_count": r.get("likes_count", 0),
                    "parent_id": old_c.get("comment_id", ""),
                    "depth": 1
                }
                new_comments.append(new_r)
        
        post["comments"] = new_comments
        post["comments_count"] = len(new_comments)
    else:
        post["comments"] = []
        post["comments_count"] = 0

with open(posts_file, 'w', encoding='utf-8') as f:
    json.dump({"posts": posts}, f, ensure_ascii=False, indent=2)

os.remove(comments_file)
print("Merge complete and comments file deleted.")
