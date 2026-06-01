import firebase_admin
from firebase_admin import credentials, firestore
import json
import os
import time
import sys
from concurrent.futures import ThreadPoolExecutor

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

# 3. Tự động lấy tất cả các file .json trong assets làm collection
file_mapping = {}
for filename in os.listdir(raw_dir):
    if filename.endswith('.json'):
        collection_name = filename[:-5]
        file_mapping[filename] = collection_name

print(f"Found {len(file_mapping)} files to upload.", flush=True)

def commit_batch(batch, items_count, total_items, collection_name):
    try:
        batch.commit()
        # print(f"[{collection_name}] Uploaded batch...", flush=True)
        return items_count
    except Exception as e:
        print(f"Error committing batch in {collection_name}: {e}", flush=True)
        return 0

def upload_file(filename, collection_name):
    file_path = os.path.join(raw_dir, filename)
    if not os.path.exists(file_path):
        return

    print(f"\n--- Uploading file: {filename} to collection: {collection_name} ---", flush=True)
    print(f"Reading and parsing {filename}...", flush=True)
    
    with open(file_path, 'r', encoding='utf-8') as f:
        try:
            data = json.load(f)
        except Exception as e:
            print(f"Error reading file {filename}: {e}", flush=True)
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
            print(f"No data found in {filename}", flush=True)
            return

        print(f"Found {len(items)} items to upload. Starting parallel upload...", flush=True)

        batch_size = 100 
        batches = []
        current_batch = db.batch()
        count = 0
        
        for item in items:
            raw_id = str(item.get('id') or item.get('_id') or '').strip()
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

        # Sử dụng ThreadPoolExecutor để chạy song song các đợt (batches)
        # Tăng max_workers để đẩy nhanh tốc độ (mở rộng băng thông)
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

print("\n--- ALL DATA UPLOADED TO FIREBASE ---", flush=True)
