import json
import random
import uuid
import sys

def main():
    file_path = 'app/src/main/assets/community_posts.json'
    
    with open(file_path, 'r', encoding='utf-8') as f:
        data = json.load(f)
        
    comments_pool = [
        {"username": "Hương Ly", "user_id": "101", "content": "Sản phẩm này dùng thích lắm nha!"},
        {"username": "Ngọc Mai", "user_id": "102", "content": "Giá hơi cao xíu nhưng chất lượng ổn."},
        {"username": "Hải Yến", "user_id": "103", "content": "Mình dùng được 2 tuần rồi, thấy da sáng lên hẳn."},
        {"username": "Minh Anh", "user_id": "104", "content": "Bao bì đẹp, giao hàng nhanh."},
        {"username": "Thảo Vy", "user_id": "105", "content": "Chất kem mỏng nhẹ, dễ thấm. Chấm 9/10!"},
        {"username": "Lan Hương", "user_id": "106", "content": "Mùi hương rất dễ chịu, relax cực kỳ."},
        {"username": "Quỳnh Nga", "user_id": "107", "content": "Da nhạy cảm dùng có bị kích ứng không ạ?"},
        {"username": "Tuyết Mai", "user_id": "108", "content": "Mình mua lần thứ 3 rồi, rất ưng ý."},
        {"username": "Hồng Ngọc", "user_id": "109", "content": "Gói hàng cẩn thận, cho shop 5 sao."},
        {"username": "Cẩm Tú", "user_id": "110", "content": "Tẩy trang sạch sâu, không làm khô da."},
        {"username": "Kiều Trang", "user_id": "111", "content": "Có ai review chi tiết em này không ạ?"},
        {"username": "Mai Thy", "user_id": "112", "content": "Chất xịn, xài êm, đáng tiền."},
        {"username": "Thanh Vân", "user_id": "113", "content": "Lên tone nhẹ nhàng tự nhiên."},
        {"username": "Bảo Hân", "user_id": "114", "content": "Sẽ ủng hộ shop thêm nhiều lần nữa."},
        {"username": "Thúy Kiều", "user_id": "115", "content": "Thấy nhiều người khen nên mua thử, ai ngờ ngon thật."}
    ]
    
    for post in data.get('posts', []):
        num_comments = random.randint(1, 4)
        selected_comments = random.sample(comments_pool, num_comments)
        
        new_comments = []
        for c in selected_comments:
            new_comment = {
                "comment_id": str(uuid.uuid4()),
                "post_id": post.get("post_id"),
                "author": {
                    "username": c["username"],
                    "user_id": c["user_id"],
                    "avatar": f"https://api.dicebear.com/7.x/avataaars/svg?seed={c['user_id']}",
                    "is_anonymous": False
                },
                "content": c["content"],
                "created_at": "2026-06-12T10:00:00Z",
                "likes_count": random.randint(0, 50),
                "parent_id": None,
                "depth": 0
            }
            new_comments.append(new_comment)
            
        post["comments"] = new_comments
        post["comments_count"] = num_comments
        
    with open(file_path, 'w', encoding='utf-8') as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
        
    print("Updated comments successfully.")

if __name__ == '__main__':
    main()
