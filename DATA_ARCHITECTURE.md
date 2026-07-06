# TÀI LIỆU KIẾN TRÚC VÀ CƠ CHẾ DỮ LIỆU DỰ ÁN ROOTIE

Tài liệu này quy định rõ ràng về cơ chế hoạt động của dữ liệu (Data Flow) hiện tại trong dự án Rootie và các nguyên tắc cốt lõi mà toàn bộ nhóm lập trình cần tuân thủ khi viết code mới.

---

## 1. MỤC TIÊU CỐT LÕI (CORE PRINCIPLES)

Do giới hạn về tài nguyên (Firebase Quota) và mục đích test app cục bộ mượt mà, dự án áp dụng kiến trúc **"LOCAL-FIRST" (Ưu tiên lưu trữ tại máy)**. 

**3 Nguyên tắc tối thượng:**
1. **Tuyệt đối KHÔNG gọi Firebase / Cloudinary cho những thay đổi của riêng cá nhân người dùng** (VD: Đổi tên, đổi avatar, lịch sử dưỡng da hằng ngày, thích/lưu bài viết). Tất cả phải ghi thẳng xuống SQLite hoặc SharedPreferences.
2. **Firebase chỉ dùng cho giao tiếp 2 chiều (Client - Server - Client):** Chỉ những tính năng yêu cầu người khác phải thấy ngay lập tức hoặc cần Admin xử lý thì mới được gọi API Firebase.
3. **App phải chạy được khi Offline (hoặc Firebase bị lỗi):** Nếu mất mạng hoặc Firebase quá tải, người dùng vẫn phải xem được hồ sơ, làm xong chu trình skincare và xem lại bài viết đã lưu từ SQLite.

---

## 2. PHÂN BỔ DỮ LIỆU (DATA ALLOCATION)

### 🔴 A. NHÓM DỮ LIỆU ĐỒNG BỘ ĐÁM MÂY (FIREBASE - CLOUD)
*Chỉ sử dụng Firestore / Realtime DB cho các tính năng sau:*

*   **Quản lý Đơn hàng (Orders):** Quy trình tạo đơn checkout, cập nhật trạng thái đơn (Chờ xác nhận, Đang giao). Cần lưu lên Firebase để Admin nắm thông tin.
*   **Tin nhắn / Chat (Messages):** Hoạt động nhắn tin realtime giữa các người dùng hoặc người dùng - Admin.
*   **Xác thực (Authentication):** Firebase Auth cho đăng nhập/đăng ký.
*   *(Hạn chế)* **Đẩy bài viết/video mới:** Tính năng lấy nguồn bài viết từ server về máy (tuy nhiên tương tác trên bài viết thì chuyển về Local).

### 🟢 B. NHÓM DỮ LIỆU LOCAL (SQLITE / SHAREDPREFERENCES / JSON)
*Tất cả phải lưu tại máy, KHÔNG tốn request Firebase:*

*   **Hồ sơ người dùng (User Profile):**
    *   Sửa tên, Bio, sđt... -> `UserDao` (SQLite) & `ProfileSession` (SharedPreferences).
    *   Đổi Avatar -> Lưu ảnh trực tiếp vào bộ nhớ trong (`file://...`) -> không gọi Cloudinary.
*   **Lịch sử & Theo dõi Skincare (Skincare History):**
    *   Tick các bước Skincare sáng/tối, phần thưởng, Streak -> Ghi vào bảng `SkinHistoryEntity` bằng `SkinHistoryDao` (SQLite).
*   **Tương tác Cộng đồng (Community Interactions):**
    *   Thả tim, Lưu (Save), Đăng lại (Repost) bài viết hoặc Video -> Ghi qua `SharedPreferences` (vd: `liked_videos_prefs`) và `UserMemoryHelper` (JSON).
*   **Dữ liệu Mẫu (Seed Data):**
    *   Dữ liệu Sản phẩm, Bài viết mẫu lúc cài app sẽ được load từ file `.json` trong thư mục `assets/` vào thẳng SQLite khi Database khởi tạo.

---

## 3. QUY TRÌNH TEST VÀ CẬP NHẬT DATABASE CỦA TEAM

Do dự án sử dụng cơ chế `fallbackToDestructiveMigration()` của Room Database (tự xóa DB cũ và tạo DB mới khi thay đổi cấu trúc bảng):

1. **Khi thêm/sửa bảng trong SQLite (`Entity`):**
   - Phải tăng `version` trong file `RootieDatabase.java` lên 1 bậc (Ví dụ: từ 38 -> 39).
   - *Lưu ý:* Việc tăng version sẽ làm **mất toàn bộ dữ liệu Local** đang có trên máy ảo/điện thoại.

2. **Cách để đồng bộ Data khi Test:**
   - Sau khi kéo (pull) code mới chứa cập nhật Database, hãy chạy App 1 lần để máy tự động tạo cấu trúc DB mới nhất.
   - Mở **SQLite Studio**, kết nối với App và thêm lại dữ liệu giả (Fake Data) nếu cần.

---

## 4. REVIEW CHECKLIST (DÀNH CHO NGƯỜI DUYỆT CODE)

Trước khi Push code hoặc Merge Pull Request, hãy tự hỏi:
- [ ] Tính năng này có làm thay đổi dữ liệu của người khác không? Nếu **KHÔNG**, hãy dùng `RootieDatabase` thay vì `FirestoreService`.
- [ ] Nút "Lưu" hoặc "Tương tác" có đang ngầm gọi `Tasks.await(db.collection(...).set(...))` không? Nếu có, hãy xóa bỏ hoặc chặn lại.
- [ ] Đã thêm đầy đủ hàm đọc/ghi vào `Dao` tương ứng chưa?

> **Tâm niệm của Team:** "Mỗi một request Firebase tiết kiệm được là một cơ hội cho App sống sót khi mang đi chấm điểm!"
