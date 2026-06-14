import json
import random
from datetime import datetime, timedelta

# Read products
with open('app/src/main/assets/products.json', encoding='utf-8') as f:
    products_data = json.load(f)
    products = products_data.get('products', products_data)

product_dict = {p.get('id', p.get('_id')): p for p in products if isinstance(p, dict)}

# Read user orders to get bought products
bought_product_ids = set()
bought_products_info = {}
with open('app/src/main/assets/orders.json', encoding='utf-8') as f:
    orders_data = json.load(f)
    for order in orders_data.get('orders', []):
        if order.get('userId') == 'test_001':
            for item in order.get('items', []):
                pid = item.get('productId')
                bought_product_ids.add(pid)
                bought_products_info[pid] = {
                    'name': item.get('productName'),
                    'price': item.get('price'),
                    'image': item.get('productImage')
                }

bought_product_ids = list(bought_product_ids)

# Update user_pro_display.json so the showcase only shows bought products
with open('app/src/main/assets/user_pro_display.json', encoding='utf-8') as f:
    displays = json.load(f)

for d in displays:
    if d['user_id'] == 'test_001':
        d['product_ids'] = bought_product_ids

with open('app/src/main/assets/user_pro_display.json', 'w', encoding='utf-8') as f:
    json.dump(displays, f, ensure_ascii=False, indent=2)

customers = ["phamth***", "ngoctran***", "linhchi***", "haianh***", "minhtu***"]
statuses = ["Thành công", "Thành công", "Đang xử lý", "Đang xử lý", "Thành công"]

orders = []
base_date = datetime(2026, 5, 20)

for i in range(5):
    pid = bought_product_ids[i % len(bought_product_ids)]
    prod = product_dict.get(pid, bought_products_info.get(pid, {"name": "Sản phẩm", "price": 500000, "image": ""}))
    qty = random.randint(1, 2)
    val = int(prod.get('price', 500000)) * qty
    comm = int(val * 0.08)
    
    orders.append({
        "order_id": f"RT245{678 - i * 100}",
        "order_date": (base_date - timedelta(days=i)).strftime("%d/%m/%Y"),
        "customer": f"{customers[i]}@gmail.com",
        "product_id": pid,
        "product_name": prod.get('name', 'Sản phẩm'),
        "product_image": prod.get('image', prod.get('images', [''])[0]) if 'image' in prod or 'images' in prod else "",
        "order_value": val,
        "commission": comm,
        "status": statuses[i]
    })

affiliate_data = {
    "user_id": "test_001",
    "total_revenue": sum(o['order_value'] for o in orders if o['status'] == 'Thành công'),
    "total_commission": sum(o['commission'] for o in orders if o['status'] == 'Thành công'),
    "pending_commission": sum(o['commission'] for o in orders if o['status'] == 'Đang xử lý'),
    "successful_orders": sum(1 for o in orders if o['status'] == 'Thành công'),
    "new_customers": 16,
    "orders": orders
}

with open('app/src/main/assets/affiliate.json', 'w', encoding='utf-8') as f:
    json.dump([affiliate_data], f, ensure_ascii=False, indent=2)

print("affiliate.json and user_pro_display.json generated/updated.")
