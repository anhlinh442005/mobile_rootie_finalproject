package com.veganbeauty.app.features.weather

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.abs

object SkinWeatherProductMatcher {

    data class ProductMatch(
        val id: String,
        val name: String,
        val suitabilityScore: Int,
        val notes: String,
        val subcategory: String,
        val mainImage: String = ""
    )

    fun matchProductsForWeatherAndSkin(
        context: Context,
        temp: Double,
        humidity: Int,
        skinType: String
    ): Map<String, ProductMatch> {
        val result = mutableMapOf<String, ProductMatch>()

        try {
            // 1. Match weather_id from weathers.json
            val weathersJson = context.assets.open("weathers.json").bufferedReader().use { it.readText() }
            val weathersArray = JSONObject(weathersJson).getJSONArray("weathers")
            var matchedWeatherId = "weather_008" // Default: Mát mẻ dễ chịu
            var minDistance = Double.MAX_VALUE

            for (i in 0 until weathersArray.length()) {
                val item = weathersArray.getJSONObject(i)
                if (!item.optBoolean("is_active", true)) continue

                val tempRange = item.getJSONObject("temperature_range")
                val tMin = tempRange.getDouble("min")
                val tMax = tempRange.getDouble("max")
                val tMid = (tMin + tMax) / 2.0

                val humRange = item.getJSONObject("humidity_range")
                val hMin = humRange.getDouble("min")
                val hMax = humRange.getDouble("max")
                val hMid = (hMin + hMax) / 2.0

                val distance = abs(temp - tMid) + abs(humidity.toDouble() - hMid)
                if (distance < minDistance) {
                    minDistance = distance
                    matchedWeatherId = item.getString("id")
                }
            }

            // 2. Load mappings from product_weather.json for the matched weather_id
            val mappingsJson = context.assets.open("product_weather.json").bufferedReader().use { it.readText() }
            val mappingsArray = JSONObject(mappingsJson).getJSONArray("mappings")
            val productScoreMap = mutableMapOf<String, Pair<Int, String>>() // productId -> Pair(score, notes)

            for (i in 0 until mappingsArray.length()) {
                val mapping = mappingsArray.getJSONObject(i)
                if (mapping.getString("weather_id") == matchedWeatherId) {
                    val pId = mapping.getString("product_id")
                    val score = mapping.optInt("suitability_score", 0)
                    val notes = mapping.optString("notes", "")
                    productScoreMap[pId] = Pair(score, notes)
                }
            }

            // 3. Load all products from products.json
            val productsJson = context.assets.open("products.json").bufferedReader().use { it.readText() }
            val productsArray = JSONObject(productsJson).getJSONArray("products")

            val matchedProducts = mutableListOf<JSONObject>()
            for (i in 0 until productsArray.length()) {
                val product = productsArray.getJSONObject(i)
                val pId = product.getString("id")
                if (productScoreMap.containsKey(pId)) {
                    matchedProducts.add(product)
                }
            }

            // 4. Group products by skin routine steps
            val cleansers = mutableListOf<JSONObject>()
            val serums = mutableListOf<JSONObject>()
            val moisturizers = mutableListOf<JSONObject>()
            val sunscreens = mutableListOf<JSONObject>()

            for (product in matchedProducts) {
                val name = product.getString("name").lowercase()
                
                val subcategories = mutableListOf<String>()
                val subcatRaw = product.opt("subcategory")
                if (subcatRaw is JSONArray) {
                    for (j in 0 until subcatRaw.length()) {
                        subcategories.add(subcatRaw.getString(j).lowercase())
                    }
                } else if (subcatRaw is String) {
                    subcategories.add(subcatRaw.lowercase())
                }

                // Check matches
                val isCleanser = name.contains("rửa mặt") || name.contains("tẩy trang") || name.contains("làm sạch") ||
                        subcategories.any { it.contains("rửa mặt") || it.contains("tẩy trang") || it.contains("làm sạch") }
                
                val isSerum = (name.contains("tinh chất") && !name.contains("tắm") && !name.contains("gội")) || name.contains("serum") || name.contains("essence") || name.contains("ampoule") ||
                        subcategories.any { it.contains("tinh chất") || it.contains("serum") || it.contains("essence") }

                val isMoisturizer = name.contains("kem dưỡng") || name.contains("thạch dưỡng") || name.contains("thạch bí đao") || name.contains("gel dưỡng") || name.contains("lotion dưỡng") || name.contains("cream") ||
                        subcategories.any { it.contains("dưỡng ẩm") || it.contains("kem dưỡng") || it.contains("thạch") }

                val isSunscreen = name.contains("chống nắng") || name.contains("sunscreen") ||
                        subcategories.any { it.contains("chống nắng") || it.contains("sunscreen") }

                if (isCleanser) {
                    cleansers.add(product)
                } else if (isSerum) {
                    serums.add(product)
                } else if (isMoisturizer) {
                    moisturizers.add(product)
                } else if (isSunscreen) {
                    sunscreens.add(product)
                }
            }

            // Helper to score compatibility of product with skinType
            fun getSkinCompatibilityScore(product: JSONObject, skinType: String): Int {
                val suitableFor = product.optString("suitableFor", "").lowercase()
                val skinConcerns = product.optString("skinConcerns", "").lowercase()
                val description = product.optString("description", "").lowercase()

                var score = 0
                val st = skinType.lowercase()

                if (st.contains("dầu")) {
                    if (suitableFor.contains("dầu") || suitableFor.contains("hỗn hợp")) score += 20
                    if (skinConcerns.contains("dầu") || skinConcerns.contains("mụn")) score += 20
                    if (description.contains("kiềm dầu") || description.contains("bã nhờn")) score += 10
                }
                if (st.contains("khô")) {
                    if (suitableFor.contains("khô")) score += 20
                    if (suitableFor.contains("mọi loại da") || suitableFor.contains("mọi người")) score += 10
                    if (description.contains("cấp ẩm") || description.contains("khóa ẩm") || description.contains("khô căng")) score += 15
                }
                if (st.contains("nhạy cảm")) {
                    if (suitableFor.contains("nhạy cảm") || suitableFor.contains("dịu nhẹ")) score += 20
                    if (description.contains("nhạy cảm") || description.contains("lành tính") || description.contains("dịu nhẹ")) score += 10
                }
                if (st.contains("mụn")) {
                    if (suitableFor.contains("mụn")) score += 20
                    if (skinConcerns.contains("mụn")) score += 20
                    if (description.contains("giảm mụn") || description.contains("kháng viêm")) score += 10
                }
                if (suitableFor.contains("mọi loại da") || suitableFor.contains("mọi người") || suitableFor.isBlank()) {
                    score += 5
                }
                return score
            }

            // Find best product using a combined score: weather suitability_score + skin compatibility score
            fun getBestProduct(list: List<JSONObject>): ProductMatch? {
                if (list.isEmpty()) return null

                var bestProduct: JSONObject? = null
                var bestTotalScore = -1
                var bestWeatherScore = 0
                var bestNotes = ""

                for (prod in list) {
                    val pId = prod.getString("id")
                    val mappingPair = productScoreMap[pId] ?: continue
                    val weatherScore = mappingPair.first
                    val notes = mappingPair.second

                    val skinScore = getSkinCompatibilityScore(prod, skinType)
                    val totalScore = weatherScore + skinScore

                    if (totalScore > bestTotalScore) {
                        bestTotalScore = totalScore
                        bestProduct = prod
                        bestWeatherScore = weatherScore
                        bestNotes = notes
                    }
                }

                return bestProduct?.let {
                    val subcatRaw = it.opt("subcategory")
                    val subcatStr = if (subcatRaw is JSONArray && subcatRaw.length() > 0) subcatRaw.getString(0) else if (subcatRaw is String) subcatRaw else ""
                    ProductMatch(
                        id = it.getString("id"),
                        name = it.getString("name"),
                        suitabilityScore = bestWeatherScore,
                        notes = bestNotes,
                        subcategory = subcatStr,
                        mainImage = it.optString("mainImage", "")
                    )
                }
            }

            getBestProduct(cleansers)?.let { result["Cleanser"] = it }
            getBestProduct(serums)?.let { result["Serum"] = it }
            getBestProduct(moisturizers)?.let { result["Moisturizer"] = it }
            getBestProduct(sunscreens)?.let { result["Sunscreen"] = it }

        } catch (e: Exception) {
            e.printStackTrace()
        }

        return result
    }
}
