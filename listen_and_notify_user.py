#!/usr/bin/env python3
import firebase_admin
from firebase_admin import credentials, firestore, messaging
import time
import os
import sys

def main():
    cred_path = 'serviceAccountKey.json'
    if not os.path.exists(cred_path):
        print(f"Lỗi: Không tìm thấy file {cred_path} ở thư mục gốc!")
        print("Đảm bảo file serviceAccountKey.json nằm ở thư mục gốc của dự án.")
        sys.exit(1)

    try:
        cred = credentials.Certificate(cred_path)
        firebase_admin.initialize_app(cred)
        db = firestore.client()
        print("Đã khởi tạo Firebase Admin SDK và đang lắng nghe tin nhắn cho User App...")
    except Exception as e:
        print(f"Lỗi khởi tạo Firebase: {e}")
        sys.exit(1)

    # Dictionary to keep track of last message ID to avoid duplicates
    last_processed_ids = {}
    is_initial = True

    def on_snapshot(col_snapshot, changes, read_time):
        nonlocal is_initial
        
        # If it's the initial snapshot, populate history and return
        if is_initial:
            for change in changes:
                doc = change.document
                doc_data = doc.to_dict()
                conv_id = doc.id
                messages = doc_data.get('messages', [])
                if messages:
                    try:
                        sorted_messages = sorted(messages, key=lambda x: x.get('sent_at', ''))
                    except Exception:
                        sorted_messages = messages
                    last_processed_ids[conv_id] = sorted_messages[-1].get('id', '')
            is_initial = False
            print("Đã nạp lịch sử tin nhắn ban đầu. Đang lắng nghe tin nhắn mới...")
            return

        for change in changes:
            doc = change.document
            doc_data = doc.to_dict()
            conv_id = doc.id
            
            members = doc_data.get('members', [])
            messages = doc_data.get('messages', [])
            if not messages:
                continue
                
            # Get the latest message
            try:
                sorted_messages = sorted(messages, key=lambda x: x.get('sent_at', ''))
            except Exception:
                sorted_messages = messages

            latest_msg = sorted_messages[-1]
            sender_id = latest_msg.get('sender_id')
            text = latest_msg.get('text', '')
            sent_at = latest_msg.get('sent_at', '')
            msg_id = latest_msg.get('id', '')

            # Find the receiver (the member who is not the sender)
            receivers = [m for m in members if m != sender_id]
            if not receivers:
                continue
            receiver_id = receivers[0]

            # Avoid notifying about the same message multiple times
            if last_processed_ids.get(conv_id) == msg_id:
                continue
            
            last_processed_ids[conv_id] = msg_id
            
            # Get sender info from member_info
            member_info = doc_data.get('member_info', {})
            sender_info = member_info.get(sender_id, {})
            sender_name = sender_info.get('name', 'Người dùng Rootie')
            sender_avatar = sender_info.get('avatar', 'https://i.pinimg.com/736x/1a/d8/4b/1ad84b9ab4a1e2ab17c7aab37fcff0a5.jpg')

            # Fetch receiver's FCM token from Firestore users collection
            try:
                receiver_doc = db.collection('users').document(receiver_id).get()
                if not receiver_doc.exists:
                    print(f"Không tìm thấy tài khoản người nhận {receiver_id} trên Firestore.")
                    continue
                receiver_token = receiver_doc.to_dict().get('fcm_token')
                if not receiver_token:
                    print(f"Người nhận {receiver_id} chưa cập nhật FCM Token lên Firestore.")
                    continue
            except Exception as e:
                print(f"Lỗi khi đọc token của {receiver_id} từ Firestore: {e}")
                continue

            # Send push notification based on conversation type (Admin <-> User or User <-> User)
            if sender_id == 'rootie_vn':
                # Admin-to-User chat notification
                title = "💬 Chuyên gia Rootie Việt Nam"
                body = text if len(text) < 100 else text[:97] + "..."
                data_payload = {
                    "id": msg_id,
                    "title": title,
                    "content": body,
                    "category": "Tin nhắn",
                    "tag": "TƯ VẤN",
                    "notificationType": "skin_chat",
                    "iconResName": "ic_chat",
                    "extra_notification_action": "open_skin_chat"
                }
                print(f"Phát hiện tin nhắn từ Admin ('rootie_vn') gửi tới {receiver_id}: '{text}'")
                send_fcm_message(receiver_token, title, body, data_payload)
            else:
                # User-to-User Community chat notification
                title = f"💬 Tin nhắn mới từ {sender_name}"
                body = f"{sender_name}: {text}"
                data_payload = {
                    "isCommunity": "true",
                    "id": msg_id,
                    "userId": sender_id,
                    "userName": sender_name,
                    "userAvatar": sender_avatar,
                    "type": "CHAT",
                    "actionType": "MESSAGE",
                    "content": f"đã gửi tin nhắn: '{text}'",
                    "postId": "",
                    "title": title,
                    "body": body,
                    "extra_notification_action": "open_community_message_list"
                }
                print(f"Phát hiện tin nhắn từ {sender_name} gửi tới {receiver_id}: '{text}'")
                send_fcm_message(receiver_token, title, body, data_payload)

    def send_fcm_message(token, title, body, data_payload):
        message = messaging.Message(
            notification=messaging.Notification(
                title=title,
                body=body,
            ),
            data=data_payload,
            token=token,
        )
        try:
            response = messaging.send(message)
            print(f"Đã gửi thông báo đẩy thành công! ID: {response}")
        except Exception as e:
            print(f"Gửi thông báo đẩy thất bại: {e}")

    # Watch the community_message collection on Firestore
    col_query = db.collection('community_message')
    query_watch = col_query.on_snapshot(on_snapshot)

    # Keep script running
    try:
        while True:
            time.sleep(1)
    except KeyboardInterrupt:
        print("\nĐang dừng listener...")

if __name__ == '__main__':
    main()
