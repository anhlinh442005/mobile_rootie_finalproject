import json
import random
import os

filepath = 'app/src/main/assets/products.json'
with open(filepath, 'r', encoding='utf-8') as f:
    data = json.load(f)

for p in data.get('products', []):
    # If no rating, randomly add a rating or leave it 0
    if 'rating' not in p or p['rating'] == 0:
        if random.random() < 0.6:  # 60% chance to have a rating
            p['rating'] = round(random.uniform(4.0, 5.0), 1)
        else:
            p['rating'] = 0.0
            
    # Optionally also ensure sold count exists if missing
    if 'sold' not in p:
        if p['rating'] > 0:
            p['sold'] = random.randint(100, 10000)
        else:
            p['sold'] = 0

with open(filepath, 'w', encoding='utf-8') as f:
    json.dump(data, f, ensure_ascii=False, indent=2)

print("Finished generating fake ratings")
