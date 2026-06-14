import json

with open("app/src/main/assets/products.json", "r", encoding="utf-8") as f:
    data = json.load(f)

products = data.get("products", [])
output = []
for p in products:
    subcategory = p.get("subcategory", "")
    if isinstance(subcategory, list):
        sub_str = ", ".join(subcategory)
    else:
        sub_str = str(subcategory)
        
    # We want Chăm Sóc Da Mặt or similar
    if "Chăm Sóc Da Mặt" in sub_str or "Mặt" in p.get("name", ""):
        output.append(f"ID: {p['id']} | Name: {p['name']} | Price: {p['price']} | Subcategory: {subcategory}")

with open("scratch/skincare_products.txt", "w", encoding="utf-8") as f_out:
    f_out.write("\n".join(output))
print(f"Dumped {len(output)} skincare products.")
