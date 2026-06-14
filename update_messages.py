import json

file_path = "app/src/main/assets/community_message.json"

with open(file_path, "r", encoding="utf-8") as f:
    data = json.load(f)

for convo in data:
    convo["user_id"] = "test_001"
    for msg in convo.get("messages", []):
        if msg.get("sender_id") == "00000000":
            msg["sender_id"] = "test_001"

with open(file_path, "w", encoding="utf-8") as f:
    json.dump(data, f, ensure_ascii=False, indent=2)

print("Updated community_message.json")
