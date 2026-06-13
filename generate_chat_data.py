import json
import random

# Read friends
with open('app/src/main/assets/User_com_friend.json', encoding='utf-8') as f:
    social_data = json.load(f)

friends = []
for item in social_data:
    if item['user_id'] == 'test_001':
        friends = item.get('friends', [])
        break

# Read users
with open('app/src/main/assets/users.json', encoding='utf-8') as f:
    users = json.load(f)

friend_users = [u for u in users if u['user_id'] in friends]

messages_data = []
chat_samples = [
    [
        {"sender_id": "test_001", "text": "Bạn dùng kem dưỡng ẩm mới thấy sao?", "timestamp": "Hôm qua", "is_mine": True, "status": "Đã xem"},
        {"sender_id": "{partner_id}", "text": "Mình thấy khá ổn, da mềm mịn hơn hẳn đó", "timestamp": "Vừa xong", "is_mine": False, "status": ""}
    ],
    [
        {"sender_id": "{partner_id}", "text": "Chị ơi, cho em hỏi về serum mờ thâm ạ", "timestamp": "14:00 8 THG 4", "is_mine": False, "status": ""},
        {"sender_id": "test_001", "text": "Chào em, em đang thắc mắc về serum nghệ hay vitamin C vậy?", "timestamp": "14:15 8 THG 4", "is_mine": True, "status": ""},
        {"sender_id": "{partner_id}", "text": "Dạ em cảm ơn chị nhiều", "timestamp": "10 ngày trước", "is_mine": False, "status": ""}
    ],
    [
        {"sender_id": "test_001", "text": "Hôm bữa đi sự kiện Rootie vui ghê luôn", "timestamp": "11 ngày trước", "is_mine": True, "status": ""},
        {"sender_id": "{partner_id}", "text": "Công nhận, để hôm nào rảnh hẹn đi cafe nha", "timestamp": "11 ngày trước", "is_mine": False, "status": ""}
    ],
    [
        {"sender_id": "{partner_id}", "text": "Bà ơi hôm trước bà mua sữa rửa mặt ở đâu dợ", "timestamp": "12 ngày trước", "is_mine": False, "status": ""}
    ],
    [
        {"sender_id": "test_001", "text": "Cảm ơn bạn đã theo dõi nha!", "timestamp": "14 ngày trước", "is_mine": True, "status": "Đã xem"}
    ]
]

for idx, user in enumerate(friend_users[:10]):
    partner_id = user['user_id']
    partner_name = user.get('full_name', '')
    if not partner_name:
        partner_name = user.get('username', '')
    partner_avatar = user.get('avatar', 'https://api.dicebear.com/7.x/avataaars/svg?seed=' + partner_id)
    
    convo_msgs = []
    sample = chat_samples[idx % len(chat_samples)]
    for i, m in enumerate(sample):
        new_m = dict(m)
        new_m['id'] = f"m{i+1}"
        new_m['sender_id'] = partner_id if m['sender_id'] == "{partner_id}" else "test_001"
        convo_msgs.append(new_m)
        
    convo = {
        "id": str(idx + 1),
        "user_id": "test_001",
        "partner_id": partner_id,
        "partner_name": partner_name,
        "partner_avatar": partner_avatar,
        "is_active": idx % 2 == 0,
        "is_unread": not convo_msgs[-1]['is_mine'] and idx % 3 == 0,
        "is_typing": False,
        "messages": convo_msgs
    }
    messages_data.append(convo)

with open('app/src/main/assets/community_message.json', 'w', encoding='utf-8') as f:
    json.dump(messages_data, f, ensure_ascii=False, indent=2)

print("community_message.json generated successfully")
