import sys
import argparse
import firebase_admin
from firebase_admin import credentials, messaging

def main():
    parser = argparse.ArgumentParser(description="Gửi thông báo đẩy FCM test đến máy ảo Android.")
    parser.add_argument("--token", required=True, help="FCM Registration Token của máy ảo Android.")
    parser.add_argument("--type", choices=["regular", "community", "admin_chat", "user_chat"], default="regular", help="Loại thông báo: 'regular', 'community', 'admin_chat' hoặc 'user_chat'.")
    args = parser.parse_args()

    cred_path = "serviceAccountKey.json"
    if not firebase_admin._apps:
        try:
            cred = credentials.Certificate(cred_path)
            firebase_admin.initialize_app(cred)
        except Exception as e:
            print(f"Lỗi khởi tạo Firebase Admin SDK: {e}")
            print("Đảm bảo file serviceAccountKey.json nằm ở thư mục gốc của dự án.")
            sys.exit(1)

    if args.type == "regular":
        # Regular notification
        data_payload = {
            "id": "noti_voucher_fcm_test",
            "title": "🎁 Siêu Deal 50% chỉ hôm nay!",
            "content": "Rootie tặng bạn voucher giảm 50% cho toàn bộ sản phẩm thuần chay. Dùng ngay mã VEGAN50 nhé!",
            "category": "Khuyến mãi",
            "tag": "SIÊU DEAL",
            "voucherCode": "VEGAN50",
            "actionText": "COPY MÃ",
            "notificationType": "voucher",
            "iconResName": "ic_voucher"
        }
        
        message = messaging.Message(
            data=data_payload,
            token=args.token
        )
        print("Đang gửi thông báo tài khoản/hệ thống...")
    elif args.type == "community":
        # Community notification
        data_payload = {
            "isCommunity": "true",
            "id": "noti_com_fcm_test",
            "userId": "test_001",
            "userName": "Linh Nguyễn",
            "userAvatar": "https://i.pinimg.com/736x/1a/d8/4b/1ad84b9ab4a1e2ab17c7aab37fcff0a5.jpg",
            "type": "POST",
            "actionType": "COMMENT",
            "content": "đã bình luận về bài viết của bạn: 'Bài chia sẻ về routine dưỡng ẩm thuần chay của bạn siêu chi tiết luôn, mình đã thử áp dụng và da đỡ khô hẳn đó!'",
            "postId": "post_001",
            "title": "💬 Tương tác mới trong Cộng đồng",
            "body": "Linh Nguyễn đã bình luận về bài viết của bạn."
        }
        
        message = messaging.Message(
            data=data_payload,
            token=args.token
        )
        print("Đang gửi thông báo cộng đồng...")
    elif args.type == "admin_chat":
        # Admin-to-User Chat message notification
        data_payload = {
            "id": "noti_admin_chat_fcm_test",
            "title": "💬 Chuyên gia Rootie Việt Nam",
            "content": "Chào bạn! Tôi có thể giúp gì cho làn da của bạn hôm nay?",
            "category": "Tin nhắn",
            "tag": "TƯ VẤN",
            "notificationType": "skin_chat",
            "iconResName": "ic_chat",
            "extra_notification_action": "open_skin_chat"
        }
        
        message = messaging.Message(
            notification=messaging.Notification(
                title="💬 Chuyên gia Rootie Việt Nam",
                body="Chào bạn! Tôi có thể giúp gì cho làn da của bạn hôm nay?"
            ),
            data=data_payload,
            token=args.token
        )
        print("Đang gửi thông báo tin nhắn Admin -> User...")
    else:
        # User-to-User Community Chat notification
        data_payload = {
            "isCommunity": "true",
            "id": "noti_user_chat_fcm_test",
            "userId": "user_456",
            "userName": "Khánh An",
            "userAvatar": "https://i.pinimg.com/736x/ab/32/b1/ab32b13edefed48f94d93ee4b6f12f6b.jpg",
            "type": "CHAT",
            "actionType": "MESSAGE",
            "content": "đã gửi tin nhắn: 'Routine này có dùng được cho da nhạy cảm không bạn?'",
            "postId": "",
            "title": "💬 Tin nhắn mới từ Khánh An",
            "body": "Khánh An: Routine này có dùng được cho da nhạy cảm không bạn?",
            "extra_notification_action": "open_community_message_list"
        }
        
        message = messaging.Message(
            notification=messaging.Notification(
                title="💬 Tin nhắn mới từ Khánh An",
                body="Khánh An: Routine này có dùng được cho da nhạy cảm không bạn?"
            ),
            data=data_payload,
            token=args.token
        )
        print("Đang gửi thông báo tin nhắn User -> User (Cộng đồng)...")

    try:
        response = messaging.send(message)
        print(f"Gửi thông báo thành công! Message ID: {response}")
        print("Hãy kiểm tra trên máy ảo Android của bạn.")
    except Exception as e:
        print(f"Gửi thông báo thất bại: {e}")

if __name__ == "__main__":
    main()
