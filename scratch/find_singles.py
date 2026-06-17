import json

with open("app/src/main/assets/products.json", "r", encoding="utf-8") as f:
    data = json.load(f)

products = data.get("products", [])
output = []
for p in products:
    category = p.get("category", "")
    if "Combo" not in category:
        name = p.get("name", "")
        output.append(f"ID: {p['id']} | Name: {name} | Price: {p['price']} | Cat: {category}")

with open("scratch/single_products.txt", "w", encoding="utf-8") as f_out:
    f_out.write("\n".join(output))
print(f"Dumped {len(output)} single products.")
