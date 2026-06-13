import json
import random

with open('app/src/main/assets/community_posts.json', 'r', encoding='utf-8') as f:
    posts = json.load(f).get('posts', [])

with open('app/src/main/assets/users.json', 'r', encoding='utf-8') as f:
    users = json.load(f)

# Common generic comments that fit beauty/lifestyle posts
comments_pool = [
    "Hữu ích lắm, mình đã học theo routine này và giờ da mình trộm vía vía hơn rất nhiều!!!",
    "Tuyệt vời quá, cảm ơn bạn đã chia sẻ nhé 💕",
    "Sản phẩm này mình cũng đang dùng, rất hợp với da dầu mụn.",
    "Lưu lại để áp dụng thử, dạo này da mình cũng đang biểu tình.",
    "Bài viết rất chi tiết, mình sẽ mua dùng thử xem sao.",
    "Đẹp quá bạn ơi! Xin info nha.",
    "Cho mình hỏi bạn mua sản phẩm này ở đâu vậy ạ?",
    "Mình da nhạy cảm dùng cái này được không bạn?",
    "Công nhận luôn, chỗ này view siêu đỉnh, đồ uống cũng ngon nữa.",
    "Bà review có tâm quá, tui cũng rinh 1 em về dùng rồi nè =)))",
    "Ui xịn xò quá, đúng thứ mình đang tìm kiếm.",
    "Da bạn đẹp quá, ước gì cũng được như vậy 😭",
    "Post chất lượng ghê, mong bạn ra thêm nhiều bài như này nữa.",
    "Cái này có tác dụng trị thâm mụn không mọi người?",
    "Mình dùng cái này bị kích ứng nhẹ, có ai giống mình không?",
    "Packaging xinh xỉu luôn á, nhìn là muốn mua rồi.",
    "Bài viết hữu ích, đã save!",
    "Bạn mua ở mall hay xách tay vậy ạ?"
]

replies_pool = [
    "Cảm ơn b nha 🥰",
    "@{} thiệt hông bà mai tui mua về xài luôn nè",
    "Mình mua ở mall chính hãng nhé b",
    "Da nhạy cảm nên test thử vùng nhỏ trước nha",
    "Đúng rồi nè, ưng xỉu luôn"
]

community_comments = {}

for post in posts:
    post_id = post['post_id']
    num_comments = post.get('comments_count', random.randint(2, 8))
    
    # We will generate up to 5 comments or `num_comments` whichever is smaller to keep the file size reasonable
    num_generate = min(num_comments, random.randint(2, 6))
    
    post_comments = []
    
    for i in range(num_generate):
        user = random.choice(users)
        content = random.choice(comments_pool)
        likes = random.randint(0, 50)
        
        has_reply = random.choice([True, False, False]) # 1/3 chance to have reply
        replies = []
        if has_reply:
            reply_user = random.choice(users)
            reply_content = random.choice(replies_pool)
            if "{}" in reply_content:
                reply_content = reply_content.format(user['username'])
            replies.append({
                "user_id": reply_user.get("user_id", ""),
                "username": reply_user.get("username", "user"),
                "avatar_url": reply_user.get("avatar", ""),
                "timeStr": f"{random.randint(1, 5)} ngày",
                "content": reply_content,
                "likes_count": random.randint(0, 20)
            })
            
        post_comments.append({
            "comment_id": f"cmt_{random.randint(1000, 9999)}",
            "user_id": user.get("user_id", ""),
            "username": user.get("username", "user"),
            "avatar_url": user.get("avatar", ""),
            "timeStr": f"{random.randint(1, 4)} tuần",
            "content": content,
            "likes_count": likes,
            "is_author": user.get("user_id") == post.get("author", {}).get("user_id"),
            "replies": replies
        })
        
    community_comments[post_id] = post_comments

# Update the community_posts.json to have some reups_count
for post in posts:
    if "reups_count" not in post:
        post["reups_count"] = random.randint(0, 15)

with open('app/src/main/assets/community_posts.json', 'w', encoding='utf-8') as f:
    json.dump({"posts": posts}, f, ensure_ascii=False, indent=2)

with open('app/src/main/assets/community_comments.json', 'w', encoding='utf-8') as f:
    json.dump(community_comments, f, ensure_ascii=False, indent=2)

print("Generated community_comments.json and updated community_posts.json")
