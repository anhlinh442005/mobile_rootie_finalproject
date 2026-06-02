# BÁO CÁO CÁC FILE ĐÃ LÀM & TÍNH NĂNG ĐƯỢC THÊM MỚI
*Dự án: Mobile Rootie Final Project*

---

> ⚠️ **Lưu ý đặc biệt dành cho Nhóm trưởng:**
> * Bạn vui lòng **bỏ qua (discard / revert)** các thay đổi trong các file cấu hình hệ thống bao gồm: `.gitignore`, `build.gradle.kts`, `gradle.properties`, `gradle/libs.versions.toml` hoặc file `MainActivity.java`. Các tệp cấu hình này bị lỗi do quá trình merge tự động của tool và đã được bạn sửa lại. Thành thật xin lỗi bạn vì sự bất tiện này!
> * Toàn bộ phần **Giao diện (XML)**, **Hình ảnh & Icon (Drawable)**, và **Mã nguồn chức năng (Kotlin/Java)** của Module Trang cá nhân dưới đây là phần tôi đã làm hoàn chỉnh và **cần được giữ lại** để tích hợp vào ứng dụng.

---

## 📌 1. TỔNG QUAN TÍNH NĂNG HỒ SƠ CÁ NHÂN (ACCOUNT PROFILE)
Tôi đã thiết kế và lập trình hoàn chỉnh Module **Trang Cá nhân & Quản lý Hồ sơ người dùng** với giao diện UI/UX hiện đại, đồng bộ theo phong cách xanh organic của App Rootie. Module bao gồm 3 màn hình (Fragment) chính:

1. **Màn hình chính - Hồ sơ cá nhân (`AccountProfileFragment`)**:
   - **Header**: Tích hợp thanh tìm kiếm nhanh, nút quét mã QR đăng nhập/thanh toán và nút thông báo có hiển thị badge thông báo mới.
   - **Thông tin cơ bản**: Avatar bo tròn có viền xanh lá cá tính, tên người dùng, email, badge thành viên ("Thành viên vàng"), badge loại da ("Da khô") và điểm tích lũy Rootie xu.
   - **Thao tác nhanh**: 3 nút tròn truy cập nhanh gồm: *Lịch sử đặt hẹn Spa*, *Lịch sử liệu trình chăm sóc da*, và *Hồ sơ tình trạng da*.
   - **Quản lý đơn hàng**: Hiển thị trạng thái đơn hàng trực quan qua 5 bước: *Chờ xác nhận*, *Đang xử lý*, *Đang giao*, *Hoàn tất*, và *Đã hủy*.
   - **Lưới tính năng mở rộng**: Gồm 9 tiện ích phân bổ thành 2 dòng:
     - Dòng 1 (5 tiện ích): *Đã xem*, *Yêu thích*, *Đánh giá*, *Hỏi đáp*, *Sản phẩm mới*.
     - Dòng 2 (4 tiện ích): *Sản phẩm bán chạy*, *Rootie deal*, *Tuyển dụng*, *Thiết lập tài khoản*.
   - **Thông tin liên hệ (Banner đôi)**: Hiển thị giờ mở cửa (8:00 - 22:00) và hotline hỗ trợ miễn phí (1800 9999).
   - **Banner quảng cáo đặc quyền**: Quảng bá chương trình "Thành viên Rootie" với hình nền mỹ phẩm xanh organic bắt mắt.
   - **Footer**: Logo chứng nhận Bộ Công Thương và thông tin bản quyền.
   
2. **Màn hình chỉnh sửa thông tin (`AccountProfileEditFragment`)**:
   - Giao diện form nhập liệu mượt mà, cho phép người dùng thay đổi ảnh đại diện (avatar), cập nhật Họ tên, Email, Số điện thoại và Địa chỉ.
   
3. **Màn hình chi tiết bảo mật (`AccountProfilePersonalInfoFragment`)**:
   - Hiển thị các thông tin cá nhân bảo mật của khách hàng một cách an toàn.

---

## 📂 2. CHI TIẾT CÁC TỆP MÃ NGUỒN (KOTLIN/JAVA)
Được viết mới hoàn toàn và đặt trong thư mục: `app/src/main/java/com/veganbeauty/app/features/profile/`

* **`AccountProfileFragment.kt`**: Xử lý hiển thị thông tin trang cá nhân chính, sự kiện click vào các nút chức năng (như đặt hẹn, đơn hàng, mở màn hình edit...).
* **`AccountProfileEditFragment.kt`**: Xử lý form cập nhật thông tin cá nhân, cho phép người dùng sửa ảnh đại diện, kiểm tra tính hợp lệ của dữ liệu trước khi lưu.
* **`AccountProfilePersonalInfoFragment.kt`**: Hiển thị thông tin bảo mật của người dùng.
* **`educational/AccountProfileFragmentJava.java`**: Tệp tham khảo viết bằng Java phục vụ học tập/đối chiếu.

---

## 🎨 3. CHI TIẾT CÁC FILE THIẾT KẾ GIAO DIỆN (XML LAYOUTS)
Được tạo mới 100% trong thư mục: `app/src/main/res/layout/`

* **`account_profile.xml`**: Layout trang cá nhân (dài 1084 dòng) được thiết kế chi tiết bằng RelativeLayout và LinearLayout lồng ghép, sử dụng CardView để tạo độ nổi khối hiện đại, ScrollView giúp cuộn trang mượt mà không bị tràn màn hình.
* **`account_profile_edit.xml`**: Layout màn hình chỉnh sửa thông tin cá nhân với các trường TextInput, nút bấm Lưu được thiết kế chuyên nghiệp.
* **`account_profile_personal_info.xml`**: Layout trang thông tin chi tiết bảo mật của tài khoản.

---

## 🖼️ 4. TÀI NGUYÊN HÌNH ẢNH & BACKGROUND (DRAWABLES MỚI)
Để phục vụ cho giao diện trang cá nhân và cả app, tôi đã thiết kế và thêm mới các tài nguyên sau:

### A. Hình ảnh minh họa (PNG/JPG)
* `bo_cong_thuong.png`: Ảnh mộc đỏ chứng nhận của Bộ Công Thương ở footer.
* `ic_logo_rootie.png`: Logo Mascot đầu ếch hoặc chữ R của thương hiệu Rootie.
* `myphamxanh.jpg`: Ảnh chụp sản phẩm mỹ phẩm organic làm nền cho banner quảng cáo ưu đãi.

### B. File nền thiết kế Vector XML (Bo góc, Gradients, Shapes)
* `bg_header_gradient.xml`: Tạo màu chuyển tiếp xanh lá đặc trưng cho header.
* `bg_search_bar.xml`: Khung nền bo tròn màu trắng đục của thanh tìm kiếm.
* `bg_button_light_green.xml`: Nền của nút bấm màu xanh nhạt có bo viền.
* `bg_banner_left.xml` & `bg_banner_right.xml`: Khung nền thiết kế bo góc trái/phải cho banner hotline và giờ làm việc.
* Các nền bo tròn khác: `bg_circle_edit_button.xml`, `bg_circle_green.xml`, `bg_circle_light_green.xml`, `bg_pill_green.xml`, `bg_pill_grey.xml`.

### C. Bộ Vector Icon XML Chuyên Nghiệp (Thêm mới 100%)
Bộ icon vector sắc nét, tự co giãn tốt theo các kích thước màn hình:
* **Nhóm Đơn hàng**: `ic_bag.xml` (Túi), `ic_box.xml` (Hộp đơn hàng), `ic_truck.xml` (Xe giao hàng), `ic_check.xml` (Hoàn thành), `ic_cancel.xml` (Hủy đơn).
* **Nhóm Dịch vụ & Spa**: `ic_calendar.xml` (Lịch hẹn), `ic_checklist.xml` (Liệu trình), `ic_face.xml` (Phân tích da).
* **Nhóm Tiện ích**: `ic_coin.xml` (Xu tích lũy), `ic_settings.xml` (Cài đặt), `ic_qr_code.xml` (Quét mã), `ic_notification.xml` (Chuông thông báo), `ic_location.xml` (Vị trí), `ic_phone.xml` (Điện thoại), `ic_clock.xml` (Đồng hồ), `ic_edit.xml` (Bút sửa), `ic_eye.xml` (Mắt xem), `ic_heart.xml` (Yêu thích), `ic_star.xml` & `ic_star_gold.xml` (Sao đánh giá).
* **Nhóm Khác**: `ic_help.xml` (Trợ giúp), `ic_briefcase.xml` (Tuyển dụng), `ic_tag.xml` (Khuyến mãi), `ic_grid.xml` (Lưới sản phẩm), `ic_water_drop.xml` (Cấp ẩm), `ic_gov_seal.xml` (Mộc pháp lý), `ic_warning_red.xml` (Cảnh báo).

---

## 🛠️ 5. SCRIPT CÔNG CỤ BỔ TRỢ
* **`scratch/convert_svgs.py`**: Script viết bằng Python giúp tự động chuyển đổi hàng loạt file thiết kế SVG của nhóm sang định dạng XML Vector của Android một cách chính xác, tiết kiệm rất nhiều thời gian thiết kế thủ công.
