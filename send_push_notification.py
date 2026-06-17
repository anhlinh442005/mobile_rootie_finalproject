import sys
import argparse
import firebase_admin
from firebase_admin import credentials, messaging

def main():
    parser = argparse.ArgumentParser(description="Gửi thông báo đẩy FCM test đến máy ảo Android.")
    parser.add_argument("--token", required=True, help="FCM Registration Token của máy ảo Android.")
    parser.add_argument("--type", choices=["regular", "community"], default="regular", help="Loại thông báo: 'regular' hoặc 'community'.")
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
    else:
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

    try:
        response = messaging.send(message)
        print(f"Gửi thông báo thành công! Message ID: {response}")
        print("Hãy kiểm tra trên máy ảo Android của bạn.")
    except Exception as e:
        print(f"Gửi thông báo thất bại: {e}")

if __name__ == "__main__":
    main()
