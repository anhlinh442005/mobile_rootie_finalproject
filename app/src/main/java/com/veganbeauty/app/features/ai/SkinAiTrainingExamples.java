package com.veganbeauty.app.features.ai;

/** Ví dụ hội thoại — “training” miễn phí qua few-shot prompt cho Gemini. */
public final class SkinAiTrainingExamples {

    private SkinAiTrainingExamples() {
    }

    public static String getFewShotBlock() {
        return "VÍ DỤ HỘI THOẠI (bắt chước cách trả lời):\n"
                + "Q: Hôm nay được cộng bao nhiêu xu?\n"
                + "A: Hôm nay bạn nhận [X] xu, tổng số dư [Y] xu. (từ mục XU)\n\n"
                + "Q: Hiện tại đang có voucher gì á?\n"
                + "A: Trong ví: [liệt kê mã voucher user]. Khuyến mãi hệ thống: [mã]. Xem Tài khoản → Voucher.\n\n"
                + "Q: Đơn hàng gần nhất của tôi?\n"
                + "A: Đơn [mã] — [trạng thái] — [tiền] — [ngày]. Xem Cửa hàng → Đơn hàng của tôi.\n\n"
                + "Q: Giỏ hàng có gì?\n"
                + "A: [tên SP x số lượng] hoặc giỏ trống. (từ GIỎ HÀNG)\n\n"
                + "Q: Tôi là loại da gì?\n"
                + "A: Bạn là [loại da], cấp ẩm [x]%, bã nhờn [y]%, nhạy cảm [z]%. (từ HỒ SƠ DA)\n\n"
                + "Q: Tôi nên tránh thành phần gì?\n"
                + "A: Theo quiz, tránh: [danh sách]. Ưu tiên lành tính, không cồn/hương liệu nếu da nhạy cảm.\n\n"
                + "Q: Routine sáng tôi nên làm gì?\n"
                + "A: [bước 1 → bước 2...] với sản phẩm cụ thể. (từ ROUTINE GỢI Ý THEO QUIZ)\n\n"
                + "Q: Thời tiết hôm nay ảnh hưởng da thế nào?\n"
                + "A: [thành phố] [°C], độ ẩm [%], UV [x]. Với [loại da], nên [lời khuyên cụ thể].\n\n"
                + "Q: Gợi ý sản phẩm cho da tôi?\n"
                + "A: Với [loại da], gợi ý: [SP từ catalog/context]. Tránh [thành phần tránh].\n\n"
                + "Q: Tôi muốn xem phác đồ da\n"
                + "A: [loại da + chỉ số]. Mình gửi phác đồ chi tiết bên dưới nhé.\n\n"
                + "Q: Lịch soi da của tôi?\n"
                + "A: [dịch vụ — trạng thái — cửa hàng — ngày]. My Skin → Lịch sử đặt lịch.\n\n"
                + "Q: Quà tôi đã đổi có gì?\n"
                + "A: [liệt kê quà từ QUÀ CỦA USER]. Tài khoản → Đổi quà.\n\n"
                + "Q: Cửa hàng Rootie ở đâu?\n"
                + "A: [tên — địa chỉ — giờ mở cửa]. Cửa hàng → Hệ thống cửa hàng.\n\n"
                + "Q: Làm sao đổi quà bằng xu?\n"
                + "A: Vào Tài khoản → Đổi quà → chọn quà → đổi bằng xu. Số dư hiện tại: [xu].\n\n"
                + "Q: Làm sao làm bài test da?\n"
                + "A: My Skin → Bài test da → làm quiz → xem kết quả và routine gợi ý.\n\n"
                + "Q: Có thông báo gì không?\n"
                + "A: [số chưa đọc] thông báo. [tiêu đề 1-2 cái gần nhất]. Chuông góc phải → Thông báo.\n\n"
                + "SAI — KHÔNG LÀM: Hỏi voucher mà trả lời routine da. Hỏi xu mà nói UV. Bịa số không có trong ngữ cảnh.\n";
    }

    public static String getAppNavigationMap() {
        return "BẢN ĐỒ APP ROOTIE:\n"
                + "- My Skin: bài test da, lịch sử quiz, đặt lịch soi da, sản phẩm đang dùng\n"
                + "- Da × Thời tiết: cập nhật thời tiết, tư vấn da theo UV/độ ẩm\n"
                + "- Rootie AI (chat): hỏi xu, voucher, routine, sản phẩm, đơn hàng\n"
                + "- Cửa hàng: mua SP, giỏ hàng, đơn hàng, voucher shop\n"
                + "- Tài khoản: hồ sơ, voucher ví, đổi quà/xu, thông báo, đơn hàng\n"
                + "- Routine: nhắc sáng/tối, các bước chăm da\n";
    }
}
