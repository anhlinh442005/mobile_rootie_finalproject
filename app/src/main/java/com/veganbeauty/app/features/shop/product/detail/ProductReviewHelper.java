package com.veganbeauty.app.features.shop.product.detail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class ProductReviewHelper {

    public static class ProductReview {
        private final String reviewerName;
        private final int rating;
        private final String comment;

        public ProductReview(String reviewerName, int rating, String comment) {
            this.reviewerName = reviewerName;
            this.rating = rating;
            this.comment = comment;
        }

        public String getReviewerName() {
            return reviewerName;
        }

        public int getRating() {
            return rating;
        }

        public String getComment() {
            return comment;
        }
    }

    private static final List<String> REVIEWERS = Arrays.asList(
            "Bảo Nguyên", "Minh Thư", "Hoàng Nam", "Khánh Linh", "Thanh Trúc",
            "Văn Hải", "Quỳnh Anh", "Tuấn Kiệt", "Hồng Ngọc", "Đức Minh",
            "Tuyết Mai", "Gia Bảo", "Như Quỳnh", "Quốc Anh", "Phương Thảo"
    );

    private static final List<String> HAIR_REVIEWS = Arrays.asList(
            "Dầu gội xài thơm mùi bưởi thảo mộc dã man, dùng mượt tóc cực kỳ!",
            "Sau 2 tuần dùng tóc con mọc nhiều hẳn, đỡ rụng tóc rõ rệt luôn.",
            "Sản phẩm thuần chay chất lượng tốt, chai to xài rất tiết kiệm.",
            "Tóc mềm mượt và bồng bềnh hơn hẳn, không bị bết rít hay ngứa da đầu.",
            "Mùi hương vỏ bưởi tự nhiên thư giãn cực kỳ, đánh giá 5 sao.",
            "Dầu xả dùng siêu thích, tóc tơi và bóng khỏe hẳn ra.",
            "Sản phẩm chân ái cho tóc rụng và xơ yếu, khuyên mọi người nên mua."
    );

    private static final List<String> LIP_REVIEWS = Arrays.asList(
            "Tẩy da chết môi hạt nhuyễn xài xong môi mịn hồng hẳn lên.",
            "Son dưỡng ẩm cực tốt, thoa một lớp trước khi đi ngủ sáng ra môi căng mọng.",
            "Mùi dừa Bến Tre ngọt dịu siêu thích, lành tính nuốt phải cũng không sao.",
            "Môi mình đỡ nứt nẻ và giảm thâm rõ rệt sau vài tuần dùng.",
            "Thiết kế nhỏ gọn tiện lợi mang đi học đi làm cực tiện.",
            "Dưỡng ẩm sâu nhưng không bị bóng mỡ quá mức, rất ưng ý."
    );

    private static final List<String> BODY_REVIEWS = Arrays.asList(
            "Tẩy tế bào chết cơ thể hạt cafe Đắk Lắk xay mịn không bị đau rát da tí nào.",
            "Sữa tắm thơm thoảng thoảng giữ ẩm da tốt không bị khô căng sau khi tắm.",
            "Dưỡng thể thấm cực nhanh, da sáng mịn màng sau vài ngày sử dụng.",
            "Mùi đường thốt nốt ngọt ngào rất thư giãn như đang đi spa.",
            "Sản phẩm thuần chay lành tính dùng rất an tâm cho da nhạy cảm.",
            "Da mình mịn màng và đỡ mụn lưng hẳn từ khi dùng combo này.",
            "Hạt scrub cafe siêu thơm, tắm xong da láng o thích cực kỳ."
    );

    private static final List<String> FACE_REVIEWS = Arrays.asList(
            "Chất gel rửa mặt rất dịu nhẹ, không gây khô căng da mặt sau khi dùng.",
            "Serum bí đao kiềm dầu siêu tốt và giảm mụn ẩn rõ rệt sau 1 tháng.",
            "Nước nghệ giúp mờ thâm mụn nhanh, da sáng khỏe đều màu hơn hẳn.",
            "Thạch hoa hồng dưỡng ẩm sâu tốt, chất thạch mỏng nhẹ thấm nhanh mát rượi.",
            "Da nhạy cảm mụn như mình xài hoàn toàn không bị kích ứng gì, siêu lành tính.",
            "Tẩy trang sạch sâu, dịu nhẹ không bị khô rát hay cay mắt tí nào.",
            "Xịt khoáng cấp ẩm tức thì, vòi xịt phun sương mịn màng rất thích.",
            "Mặt nạ nghệ đắp xong da sáng mịn mà không hề bị vàng da chút nào cả.",
            "Sản phẩm của hãng này thì lành tính miễn bàn rồi, thiết kế bao bì cũng đẹp nữa."
    );

    public static List<ProductReview> getReviews(String productId, String productName, String category) {
        String safeId = productId != null ? productId : "";
        Random rand = new Random(safeId.hashCode());
        String nameLower = productName != null ? productName.toLowerCase() : "";
        String catLower = category != null ? category.toLowerCase() : "";

        List<String> sourceList;
        if (nameLower.contains("tóc") || nameLower.contains("gội") || nameLower.contains("xả") || nameLower.contains("bưởi") || catLower.contains("tóc")) {
            sourceList = HAIR_REVIEWS;
        } else if (nameLower.contains("môi") || nameLower.contains("son") || catLower.contains("môi")) {
            sourceList = LIP_REVIEWS;
        } else if (nameLower.contains("body") || nameLower.contains("tắm") || nameLower.contains("cơ thể") || catLower.contains("thể") || catLower.contains("tắm")) {
            sourceList = BODY_REVIEWS;
        } else {
            sourceList = FACE_REVIEWS;
        }

        List<String> shuffledReviews = new ArrayList<>(sourceList);
        Collections.shuffle(shuffledReviews, rand);

        List<String> shuffledNames = new ArrayList<>(REVIEWERS);
        Collections.shuffle(shuffledNames, rand);

        int count = 6 + rand.nextInt(5);
        List<ProductReview> result = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            String name = shuffledNames.get(i % shuffledNames.size());
            double rVal = rand.nextDouble();
            int rating;
            if (rVal < 0.70) {
                rating = 5;
            } else if (rVal < 0.95) {
                rating = 4;
            } else {
                rating = 3;
            }
            String comment = shuffledReviews.get(i % shuffledReviews.size());
            result.add(new ProductReview(name, rating, comment));
        }

        return result;
    }

    public static List<ProductReview> getRandomReviews(String productName, String category, int count) {
        Random rand = new Random();
        String nameLower = productName != null ? productName.toLowerCase() : "";
        String catLower = category != null ? category.toLowerCase() : "";

        List<String> sourceList;
        if (nameLower.contains("tóc") || nameLower.contains("gội") || nameLower.contains("xả") || nameLower.contains("bưởi") || catLower.contains("tóc")) {
            sourceList = HAIR_REVIEWS;
        } else if (nameLower.contains("môi") || nameLower.contains("son") || catLower.contains("môi")) {
            sourceList = LIP_REVIEWS;
        } else if (nameLower.contains("body") || nameLower.contains("tắm") || nameLower.contains("cơ thể") || catLower.contains("thể") || catLower.contains("tắm")) {
            sourceList = BODY_REVIEWS;
        } else {
            sourceList = FACE_REVIEWS;
        }

        List<ProductReview> result = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String name = REVIEWERS.get(rand.nextInt(REVIEWERS.size()));
            double rVal = rand.nextDouble();
            int rating;
            if (rVal < 0.60) rating = 5;
            else if (rVal < 0.80) rating = 4;
            else if (rVal < 0.90) rating = 3;
            else if (rVal < 0.95) rating = 2;
            else rating = 1;

            String comment = sourceList.get(rand.nextInt(sourceList.size()));
            String modifiedComment;
            if (rating == 1) {
                modifiedComment = "Chất lượng không như mong đợi. " + comment;
            } else if (rating == 2) {
                modifiedComment = "Tạm ổn nhưng không thích lắm. " + comment;
            } else {
                modifiedComment = comment;
            }
            result.add(new ProductReview(name, rating, modifiedComment));
        }
        return result;
    }

    public static class RatingStats {
        public final double rating;
        public final int reviewCount;

        public RatingStats(double rating, int reviewCount) {
            this.rating = rating;
            this.reviewCount = reviewCount;
        }
    }

    public static RatingStats getRatingStats(String productId) {
        String safeId = productId != null ? productId : "";
        Random rand = new Random(safeId.hashCode() + 1);
        int reviewCount = 45 + rand.nextInt(320);
        double rating = 4.5 + rand.nextDouble() * 0.5;
        double formattedRating = Math.round(rating * 10.0) / 10.0;
        return new RatingStats(formattedRating, reviewCount);
    }
}
