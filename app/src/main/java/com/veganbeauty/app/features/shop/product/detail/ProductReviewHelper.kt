package com.veganbeauty.app.features.shop.product.detail

import java.util.Random

data class ProductReview(
    val reviewerName: String,
    val rating: Int,
    val comment: String
)

object ProductReviewHelper {

    private val REVIEWERS = listOf(
        "Bảo Nguyên", "Minh Thư", "Hoàng Nam", "Khánh Linh", "Thanh Trúc", 
        "Văn Hải", "Quỳnh Anh", "Tuấn Kiệt", "Hồng Ngọc", "Đức Minh", 
        "Tuyết Mai", "Gia Bảo", "Như Quỳnh", "Quốc Anh", "Phương Thảo"
    )

    private val HAIR_REVIEWS = listOf(
        "Dầu gội xài thơm mùi bưởi thảo mộc dã man, dùng mượt tóc cực kỳ!",
        "Sau 2 tuần dùng tóc con mọc nhiều hẳn, đỡ rụng tóc rõ rệt luôn.",
        "Sản phẩm thuần chay chất lượng tốt, chai to xài rất tiết kiệm.",
        "Tóc mềm mượt và bồng bềnh hơn hẳn, không bị bết rít hay ngứa da đầu.",
        "Mùi hương vỏ bưởi tự nhiên thư giãn cực kỳ, đánh giá 5 sao.",
        "Dầu xả dùng siêu thích, tóc tơi và bóng khỏe hẳn ra.",
        "Sản phẩm chân ái cho tóc rụng và xơ yếu, khuyên mọi người nên mua."
    )

    private val LIP_REVIEWS = listOf(
        "Tẩy da chết môi hạt nhuyễn xài xong môi mịn hồng hẳn lên.",
        "Son dưỡng ẩm cực tốt, thoa một lớp trước khi đi ngủ sáng ra môi căng mọng.",
        "Mùi dừa Bến Tre ngọt dịu siêu thích, lành tính nuốt phải cũng không sao.",
        "Môi mình đỡ nứt nẻ và giảm thâm rõ rệt sau vài tuần dùng.",
        "Thiết kế nhỏ gọn tiện lợi mang đi học đi làm cực tiện.",
        "Dưỡng ẩm sâu nhưng không bị bóng mỡ quá mức, rất ưng ý."
    )

    private val BODY_REVIEWS = listOf(
        "Tẩy tế bào chết cơ thể hạt cafe Đắk Lắk xay mịn không bị đau rát da tí nào.",
        "Sữa tắm thơm thoang thoảng giữ ẩm da tốt không bị khô căng sau khi tắm.",
        "Dưỡng thể thấm cực nhanh, da sáng mịn màng sau vài ngày sử dụng.",
        "Mùi đường thốt nốt ngọt ngào rất thư giãn như đang đi spa.",
        "Sản phẩm thuần chay lành tính dùng rất an tâm cho da nhạy cảm.",
        "Da mình mịn màng và đỡ mụn lưng hẳn từ khi dùng combo này.",
        "Hạt scrub cafe siêu thơm, tắm xong da láng o thích cực kỳ."
    )

    private val FACE_REVIEWS = listOf(
        "Chất gel rửa mặt rất dịu nhẹ, không gây khô căng da mặt sau khi dùng.",
        "Serum bí đao kiềm dầu siêu tốt và giảm mụn ẩn rõ rệt sau 1 tháng.",
        "Nước nghệ giúp mờ thâm mụn nhanh, da sáng khỏe đều màu hơn hẳn.",
        "Thạch hoa hồng dưỡng ẩm sâu tốt, chất thạch mỏng nhẹ thấm nhanh mát rượi.",
        "Da nhạy cảm mụn như mình xài hoàn toàn không bị kích ứng gì, siêu lành tính.",
        "Tẩy trang sạch sâu, dịu nhẹ không bị khô rát hay cay mắt tí nào.",
        "Xịt khoáng cấp ẩm tức thì, vòi xịt phun sương mịn màng rất thích.",
        "Mặt nạ nghệ đắp xong da sáng mịn mà không hề bị vàng da chút nào cả.",
        "Sản phẩm của hãng này thì lành tính miễn bàn rồi, thiết kế bao bì cũng đẹp nữa."
    )

    fun getReviews(productId: String, productName: String, category: String): List<ProductReview> {
        val rand = Random(productId.hashCode().toLong())
        val nameLower = productName.lowercase()
        val catLower = category.lowercase()
        
        // Choose list based on product category
        val sourceList = when {
            nameLower.contains("tóc") || nameLower.contains("gội") || nameLower.contains("xả") || nameLower.contains("bưởi") || catLower.contains("tóc") -> HAIR_REVIEWS
            nameLower.contains("môi") || nameLower.contains("son") || catLower.contains("môi") -> LIP_REVIEWS
            nameLower.contains("body") || nameLower.contains("tắm") || nameLower.contains("cơ thể") || catLower.contains("thể") || catLower.contains("tắm") -> BODY_REVIEWS
            else -> FACE_REVIEWS
        }

        val shuffledReviews = sourceList.shuffled(rand)
        val shuffledNames = REVIEWERS.shuffled(rand)
        
        val count = 6 + rand.nextInt(5) // Generate 6 to 10 reviews
        val result = mutableListOf<ProductReview>()
        
        for (i in 0 until count) {
            val name = shuffledNames[i % shuffledNames.size]
            // Pick a rating: 5 stars (70% chance), 4 stars (25% chance), 3 stars (5% chance)
            val rVal = rand.nextDouble()
            val rating = when {
                rVal < 0.70 -> 5
                rVal < 0.95 -> 4
                else -> 3
            }
            val comment = shuffledReviews[i % shuffledReviews.size]
            result.add(ProductReview(name, rating, comment))
        }
        
        return result
    }

    fun getRatingStats(productId: String): Pair<Double, Int> {
        val rand = Random(productId.hashCode().toLong() + 1)
        val reviewCount = 45 + rand.nextInt(320)
        // Rating between 4.5 and 5.0
        val rating = 4.5 + rand.nextDouble() * 0.5
        val formattedRating = Math.round(rating * 10.0) / 10.0
        return Pair(formattedRating, reviewCount)
    }
}
