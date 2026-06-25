#!/usr/bin/env python3
import firebase_admin
from firebase_admin import credentials, firestore, messaging
import time
import os
import sys

# Đảm bảo in tiếng Việt có dấu không bị lỗi trên terminal Windows
if sys.platform.startswith('win'):
    try:
        sys.stdout.reconfigure(encoding='utf-8')
        sys.stderr.reconfigure(encoding='utf-8')
    except AttributeError:
        pass

import smtplib
from email.mime.text import MIMEText
from email.mime.multipart import MIMEMultipart

# ==================== CONFIG CONFIGURATION FOR EMAIL SENDING ====================
# Cấu hình tài khoản email gửi thư của Rootie tại đây
SMTP_SERVER = "smtp.office365.com"          # Thay đổi nếu dùng nhà cung cấp khác (ví dụ: smtp.zoho.com, mail.rootie.vn)
SMTP_PORT = 587                       # Cổng kết nối (thường là 465 cho SSL hoặc 587 cho TLS)
SENDER_EMAIL = "rootie.beauty@outlook.com.vn"   # Địa chỉ email gửi thư của Rootie
SENDER_PASSWORD = "phymuvcjrelzfkfs"   # Mật khẩu ứng dụng (App Password nếu dùng Gmail) hoặc mật khẩu SMTP
# =================================================================================

def send_confirmation_email(recipient_email, order_id, recipient_name, total_amount, items_summary, payment_method, address):
    if not SENDER_EMAIL or "your_email" in SENDER_EMAIL:
        print("\n[Email] Bỏ qua gửi email xác nhận: Chưa cấu hình SENDER_EMAIL / SENDER_PASSWORD trong file listen_and_notify_user.py!")
        return

    msg = MIMEMultipart()
    msg['From'] = f"Rootie Việt Nam <{SENDER_EMAIL}>"
    msg['To'] = recipient_email
    msg['Subject'] = f"[Rootie] Xác nhận đặt hàng thành công #{order_id}"

    html_content = f"""
    <html>
    <head>
        <meta charset="utf-8">
        <style>
            body {{ font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; line-height: 1.6; color: #333333; margin: 0; padding: 0; }}
            .container {{ max-width: 600px; margin: 20px auto; padding: 25px; border: 1px solid #e2e8f0; border-radius: 12px; background-color: #ffffff; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1); }}
            .header {{ background-color: #4A7C59; padding: 20px; text-align: center; border-radius: 8px 8px 0 0; color: #ffffff; }}
            .header h2 {{ margin: 0; font-size: 22px; }}
            .content {{ padding: 20px 10px; }}
            .footer {{ text-align: center; font-size: 12px; color: #718096; margin-top: 30px; border-top: 1px solid #edf2f7; padding-top: 15px; }}
            .order-details {{ background-color: #f7fafc; padding: 18px; border-radius: 8px; margin: 20px 0; border-left: 4px solid #4A7C59; }}
            .order-details p {{ margin: 8px 0; font-size: 14px; }}
            .price {{ color: #C53030; font-weight: bold; font-size: 16px; }}
            .product-list {{ margin-left: 15px; padding-left: 0; list-style-type: none; }}
            .product-item {{ padding: 4px 0; border-bottom: 1px dashed #e2e8f0; }}
            .product-item:last-child {{ border-bottom: none; }}
        </style>
    </head>
    <body>
        <div class="container">
            <div class="header">
                <h2>CẢM ƠN BẠN ĐÃ MUA SẮM TẠI ROOTIE!</h2>
            </div>
            <div class="content">
                <p>Chào bạn <b>{recipient_name}</b>,</p>
                <p>Cảm ơn bạn đã lựa chọn các sản phẩm thuần chay tại Rootie. Đơn hàng của bạn đã được ghi nhận thành công và đang được xử lý.</p>
                
                <div class="order-details">
                    <p><b>Mã đơn hàng:</b> <span style="font-family: monospace; font-size: 15px; font-weight: bold; color: #2d3748;">{order_id}</span></p>
                    <p><b>Phương thức thanh toán:</b> {payment_method}</p>
                    <p><b>Địa chỉ nhận hàng:</b> {address}</p>
                    <p><b>Sản phẩm đã chọn:</b></p>
                    <ul class="product-list">
                        {items_summary}
                    </ul>
                    <p style="margin-top: 15px; border-top: 1px solid #e2e8f0; padding-top: 10px;">
                        <b>Tổng thanh toán:</b> <span class="price">{total_amount}đ</span>
                    </p>
                </div>
                
                <p>Trạng thái hành trình giao hàng sẽ được cập nhật trực tiếp trên ứng dụng <b>Rootie</b>. Nếu bạn có bất kỳ thắc mắc nào, vui lòng liên hệ hotline chăm sóc khách hàng của chúng tôi.</p>
            </div>
            <div class="footer">
                <p>Đây là thư điện tử được gửi tự động từ hệ thống Rootie Việt Nam. Vui lòng không phản hồi trực tiếp email này.</p>
                <p><b>Rootie - Mỹ phẩm thuần chay & Chăm sóc da chuyên sâu</b></p>
            </div>
        </div>
    </body>
    </html>
    """
    
    msg.attach(MIMEText(html_content, 'html', 'utf-8'))

    try:
        if SMTP_PORT == 465:
            server = smtplib.SMTP_SSL(SMTP_SERVER, SMTP_PORT, timeout=10)
        else:
            server = smtplib.SMTP(SMTP_SERVER, SMTP_PORT, timeout=10)
            server.starttls()
            
        server.login(SENDER_EMAIL, SENDER_PASSWORD)
        server.sendmail(SENDER_EMAIL, recipient_email, msg.as_string())
        server.quit()
        print(f"\n[Email] Đã gửi thư xác nhận đặt hàng thành công đến địa chỉ: {recipient_email} cho đơn hàng {order_id}!")
    except Exception as e:
        print(f"\n[Email] Lỗi gửi email đến {recipient_email}: {e}")

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

    # Watch the orders collection on Firestore
    last_processed_orders = {}
    is_initial_orders = True

    def on_order_snapshot(col_snapshot, changes, read_time):
        nonlocal is_initial_orders
        
        if is_initial_orders:
            for change in changes:
                doc = change.document
                last_processed_orders[doc.id] = True
            is_initial_orders = False
            print("Đã nạp danh sách đơn hàng ban đầu để tránh gửi trùng.")
            return

        for change in changes:
            if change.type.name == 'ADDED':
                doc = change.document
                order_data = doc.to_dict()
                order_id = doc.id
                
                if order_id in last_processed_orders:
                    continue
                last_processed_orders[order_id] = True

                # Check if it's a guest order and has email
                is_guest = order_data.get('isGuest', False)
                billing_email = order_data.get('billingEmail')
                
                if is_guest and billing_email:
                    print(f"\nPhát hiện đơn hàng mới của khách vãng lai: {order_id} - Bắt đầu gửi email tới {billing_email}...")
                    recipient_name = order_data.get('shippingName', 'Khách hàng')
                    total_amount = order_data.get('totalAmount', 0)
                    formatted_total = f"{total_amount:,}"
                    payment_method = order_data.get('paymentMethod', 'Thanh toán khi nhận hàng')
                    address = order_data.get('shippingAddress', 'Nhận tại cửa hàng')
                    
                    # Generate items list HTML
                    items = order_data.get('items', [])
                    items_html = ""
                    for item in items:
                        name = item.get('productName', 'Sản phẩm')
                        qty = item.get('quantity', 1)
                        price = item.get('price', 0)
                        items_html += f"<li class='product-item'><b>{name}</b> x{qty} - {price:,}đ</li>"
                    
                    send_confirmation_email(
                        recipient_email=billing_email,
                        order_id=order_id,
                        recipient_name=recipient_name,
                        total_amount=formatted_total,
                        items_summary=items_html,
                        payment_method=payment_method,
                        address=address
                    )

    orders_query = db.collection('orders')
    orders_watch = orders_query.on_snapshot(on_order_snapshot)

    # Keep script running
    try:
        while True:
            time.sleep(1)
    except KeyboardInterrupt:
        print("\nĐang dừng listener...")

if __name__ == '__main__':
    main()
