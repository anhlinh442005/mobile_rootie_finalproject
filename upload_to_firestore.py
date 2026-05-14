import firebase_admin
from firebase_admin import credentials, firestore
import json
import os

# 1. Khởi tạo Firebase
cred_path = 'serviceAccountKey.json'
if not os.path.exists(cred_path):
    print("Lỗi: Không tìm thấy file serviceAccountKey.json!")
    exit()

cred = credentials.Certificate(cred_path)
firebase_admin.initialize_app(cred)
db = firestore.client()

# 2. Cấu hình thư mục dữ liệu (Đã chuyển sang assets)
raw_dir = 'app/src/main/assets'

# Danh sách các file và tên collection tương ứng
file_mapping = {
    # 'categories.json': 'categories',
    # 'community_blog.json': 'blogs',
    # 'community_news.json': 'news',
    # 'community_posts.json': 'posts',
    # 'community_reels_fb.json': 'reels',
    # 'community_video_yt.json': 'videos',
    # 'ingredient.json': 'ingredients',
    # 'policy.json': 'policies',
    'products.json': 'products',
# 'product_weather.json': 'product_weather',
    # 'rootie_stores.json': 'stores',
    # 'users.json': 'users',
    # 'user_bonus.json': 'user_bonus',
    # 'weathers.json': 'weathers'
}

import time

def upload_file(filename, collection_name):
    file_path = os.path.join(raw_dir, filename)
    if not os.path.exists(file_path):
        return

    print(f"\n--- Đang tải file: {filename} vào collection: {collection_name} ---")
    
    with open(file_path, 'r', encoding='utf-8') as f:
        try:
            data = json.load(f)
        except Exception as e:
            print(f"Lỗi đọc file {filename}: {e}")
            return
        
        items = []
        if isinstance(data, list):
            items = data
        elif isinstance(data, dict):
            for key in data:
                if isinstance(data[key], list):
                    items = data[key]
                    break
        
        if not items:
            print(f"Không tìm thấy danh sách dữ liệu trong {filename}")
            return

        # Giảm batch size xuống 50 cho các file lớn
        batch_size = 50 
        batch = db.batch()
        count = 0
        
        for item in items:
            raw_id = str(item.get('id') or item.get('_id') or '').strip()
            doc_id = raw_id.replace('/', '-') 
            
            if doc_id and doc_id != 'None':
                doc_ref = db.collection(collection_name).document(doc_id)
            else:
                doc_ref = db.collection(collection_name).document()
            
            batch.set(doc_ref, item)
            
            count += 1
            if count % batch_size == 0:
                try:
                    batch.commit()
                    print(f"Đã tải được {count}/{len(items)} items...")
                    batch = db.batch()
                    time.sleep(1) # Nghỉ 1 giây để tránh timeout
                except Exception as e:
                    print(f"Lỗi khi commit batch tại item {count}: {e}")
                    # Thử lại hoặc tiếp tục batch mới
                    batch = db.batch()
                    time.sleep(2)

        try:
            batch.commit()
            print(f"Hoàn tất! Tổng cộng {count} items.")
        except Exception as e:
            print(f"Lỗi commit cuối cùng: {e}")

# Các file đã tải xong (categories) bạn có thể xóa khỏi file_mapping để tiết kiệm thời gian
# file_mapping = { ... } 

# Chạy tải tất cả
for filename, coll_name in file_mapping.items():
    upload_file(filename, coll_name)

print("\n--- TẤT CẢ DỮ LIỆU ĐÃ ĐƯỢC TẢI LÊN FIREBASE ---")
