import firebase_admin
from firebase_admin import credentials, firestore
import json
import os
import hashlib
from concurrent.futures import ThreadPoolExecutor

# =====================================================================
# 📋 DANH SÁCH CÁC FILE CẦN UPLOAD LÊN FIREBASE (CẤU HÌNH TẠI ĐÂY)
# =====================================================================
# Bạn muốn tải lên tệp tin nào thì chỉ cần bỏ dấu thăng (#) trước tên tệp tin đó.
# Hiện tại, chỉ mở (uncomment) 'users.json' và 'community_posts.json' theo yêu cầu của bạn.
TARGET_FILES = [
    'users.json',
    'community_posts.json',
    # 'categories.json',
    # 'community_blog.json',
    # 'community_news.json',
    # 'community_reels_fb.json',
    # 'community_video_yt.json',
    # 'ingredient.json',
    # 'policy.json',
    # 'product_weather.json',
    # 'products.json',
    # 'quiz_cauhoi.json',
    # 'quiz_ketqua.json',
    # 'quiz_loaida.json',
    # 'quiz_thanhphan.json',
    # 'rootie_stores.json',
    # 'weathers.json',
]

# Thư viện ảnh mẫu skincare/beauty tuyệt đẹp từ Pinterest do bạn cung cấp
PINTEREST_MOCK_IMAGES = [
    # 10 ảnh mới cung cấp:
    "https://i.pinimg.com/1200x/55/9e/a3/559ea3d02a013a44f58c707282a65cea.jpg",
    "https://i.pinimg.com/736x/80/15/7a/80157a2579c936412b0756b03ad25a9b.jpg",
    "https://i.pinimg.com/736x/d9/78/0f/d9780f5ac35ce38cf5ce62b4ed7511c5.jpg",
    "https://i.pinimg.com/1200x/5c/2b/30/5c2b30854a6c95f0268e8a4b19788077.jpg",
    "https://i.pinimg.com/736x/78/37/cc/7837ccfca5696ec725b4cd23093901f0.jpg",
    "https://i.pinimg.com/1200x/8e/dc/e6/8edce672b512dbf0c1f056f4ce096e5b.jpg",
    "https://i.pinimg.com/736x/f7/90/e1/f790e15d86b0179f27c192d2a4838eba.jpg",
    "https://i.pinimg.com/1200x/89/4a/7e/894a7e1927a64e6bf72f3a577b634443.jpg",
    "https://i.pinimg.com/1200x/e4/81/5a/e4815a44c2153e192aebe314f30460b5.jpg",
    "https://i.pinimg.com/736x/a2/0b/42/a20b42989e4532945c58e8f18b5e6757.jpg",
    # 8 ảnh cũ tuyệt đẹp:
    "https://i.pinimg.com/736x/3a/5d/37/3a5d37628fd57be40f22185d4211d809.jpg",
    "https://i.pinimg.com/736x/9b/23/6e/9b236e9261a53c184c455047b9233b19.jpg",
    "https://i.pinimg.com/736x/92/15/dd/9215dddc32b9d6d5bf6124f4342d2333.jpg",
    "https://i.pinimg.com/736x/2a/0e/94/2a0e94dacdac3cd2f72f576a245ee598.jpg",
    "https://i.pinimg.com/736x/6c/69/f0/6c69f0634ae7f90bd43142a05c401b63.jpg",
    "https://i.pinimg.com/1200x/e2/c0/90/e2c090d9b8ce2ff273352a3c7163908a.jpg",
    "https://i.pinimg.com/1200x/a6/01/4a/a6014a6684c9c49dc0ad02412cbe2815.jpg",
    "https://i.pinimg.com/1200x/fe/75/84/fe75846bb1d1a4b81b657c0529a18250.jpg"
]

AVATAR_IMAGES = [
    "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150",
    "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150",
    "https://images.unsplash.com/photo-1570295999919-56ceb5ecca61?w=150",
    "https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=150",
    "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150"
]

def resolve_post_media(post):
    """Thay thế link ảnh cũ đã hết hạn bằng các link Pinterest chất lượng cao"""
    post_id = post.get('post_id') or post.get('id') or ''
    media = post.get('media')
    if not media or not isinstance(media, list):
        return
    
    h = int(hashlib.md5(str(post_id).encode('utf-8')).hexdigest(), 16)
    for index, m in enumerate(media):
        if isinstance(m, dict) and 'url' in m:
            m['url'] = PINTEREST_MOCK_IMAGES[(h + index) % len(PINTEREST_MOCK_IMAGES)]

def resolve_user_avatar(user):
    """Thay thế link avatar hỏng/hết hạn bằng avatar sắc nét thực tế từ Unsplash"""
    user_id = user.get('user_id') or user.get('id') or ''
    avatar = user.get('avatar')
    if not avatar or 'fbcdn' in avatar or 'scontent' in avatar:
        h = int(hashlib.md5(str(user_id).encode('utf-8')).hexdigest(), 16)
        user['avatar'] = AVATAR_IMAGES[h % len(AVATAR_IMAGES)]

# 1. Khởi tạo Firebase
cred_path = 'serviceAccountKey.json'
if not os.path.exists(cred_path):
    print("Error: serviceAccountKey.json not found!", flush=True)
    exit()

cred = credentials.Certificate(cred_path)
firebase_admin.initialize_app(cred)
db = firestore.client()

# 2. Cấu hình thư mục dữ liệu
raw_dir = 'app/src/main/assets'

# Tạo ánh xạ tự động từ tên tệp tin sang tên Collection tương ứng
file_mapping = {}
for filename in TARGET_FILES:
    if filename.endswith('.json'):
        collection_name = filename[:-5] # Ví dụ: 'users.json' -> 'users'
        file_mapping[filename] = collection_name

print(f"Targeting {len(file_mapping)} collections for upload: {list(file_mapping.values())}", flush=True)

def delete_collection(collection_name, batch_size=100):
    """Xóa sạch toàn bộ tài liệu cũ trong collection để nạp mới hoàn toàn (tránh trùng lặp dữ liệu cũ)"""
    print(f"[{collection_name}] Deleting all existing documents to perform a fresh upload...", flush=True)
    coll_ref = db.collection(collection_name)
    
    deleted_count = 0
    while True:
        docs = coll_ref.limit(batch_size).get()
        deleted_in_batch = 0
        
        batch = db.batch()
        for doc in docs:
            batch.delete(doc.reference)
            deleted_in_batch += 1
            
        if deleted_in_batch == 0:
            break
            
        batch.commit()
        deleted_count += deleted_in_batch
        
    print(f"[{collection_name}] Successfully deleted {deleted_count} old documents.", flush=True)

def commit_batch(batch, items_count, total_items, collection_name):
    try:
        batch.commit()
        return items_count
    except Exception as e:
        print(f"Error committing batch in {collection_name}: {e}", flush=True)
        return 0

def upload_file(filename, collection_name):
    file_path = os.path.join(raw_dir, filename)
    if not os.path.exists(file_path):
        print(f"Warning: File {file_path} not found!", flush=True)
        return

    print(f"\n--- Processing: {filename} -> Collection: {collection_name} ---", flush=True)
    
    # Bước 1: Xóa sạch dữ liệu cũ trong Collection trước khi tải dữ liệu mới lên
    delete_collection(collection_name)

    # Bước 2: Đọc dữ liệu từ file JSON cục bộ
    with open(file_path, 'r', encoding='utf-8') as f:
        try:
            data = json.load(f)
        except Exception as e:
            print(f"Error reading file {filename}: {e}", flush=True)
            return
        
        items = []
        is_dict_root = False
        dict_key = None
        
        if isinstance(data, list):
            items = data
        elif isinstance(data, dict):
            is_dict_root = True
            for key in data:
                if isinstance(data[key], list):
                    items = data[key]
                    dict_key = key
                    break
        
        if not items:
            print(f"No data found in {filename}", flush=True)
            return

        # Bước 3: Tự động bổ sung và chuẩn hóa link ảnh trực tiếp trên bộ nhớ dữ liệu
        print("Cleaning expired media/avatar links and replacing with valid high-quality URLs...", flush=True)
        for item in items:
            if collection_name == 'community_posts':
                resolve_post_media(item)
            elif collection_name == 'users':
                resolve_user_avatar(item)

        # Bước 4: Lưu đè dữ liệu đã cập nhật sạch ngược trở lại assets cục bộ
        print(f"Updating local {filename} asset on disk...", flush=True)
        with open(file_path, 'w', encoding='utf-8') as outfile:
            if is_dict_root and dict_key:
                data[dict_key] = items
            else:
                data = items
            json.dump(data, outfile, ensure_ascii=False, indent=2)

        print(f"Found {len(items)} items to upload. Starting parallel upload...", flush=True)

        batch_size = 100 
        batches = []
        current_batch = db.batch()
        count = 0
        
        for item in items:
            raw_id = str(item.get('id') or item.get('post_id') or item.get('user_id') or '').strip()
            doc_id = raw_id.replace('/', '-') 
            
            if doc_id and doc_id != 'None' and doc_id != '':
                doc_ref = db.collection(collection_name).document(doc_id)
            else:
                doc_ref = db.collection(collection_name).document()
            
            current_batch.set(doc_ref, item)
            count += 1
            
            if count % batch_size == 0:
                batches.append(current_batch)
                current_batch = db.batch()
        
        if count % batch_size != 0:
            batches.append(current_batch)

        max_workers = 10 
        total_uploaded = 0
        with ThreadPoolExecutor(max_workers=max_workers) as executor:
            futures = [executor.submit(commit_batch, b, batch_size if i < len(batches)-1 else (count % batch_size or batch_size), count, collection_name) for i, b in enumerate(batches)]
            
            done_count = 0
            for future in futures:
                res = future.result()
                total_uploaded += res
                done_count += 1
                if done_count % 5 == 0 or done_count == len(batches):
                    print(f"Progress: {done_count}/{len(batches)} batches committed ({total_uploaded}/{count} items)", flush=True)

        print(f"Completed! Total {total_uploaded} items in collection {collection_name}.", flush=True)

# Chạy tải tất cả
for filename, coll_name in file_mapping.items():
    upload_file(filename, coll_name)

print("\n--- TARGETED DATA SUCCESSFULLY UPLOADED TO FIREBASE ---", flush=True)
