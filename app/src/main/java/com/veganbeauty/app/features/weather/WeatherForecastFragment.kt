package com.veganbeauty.app.features.weather

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import coil.load
import coil.transform.CircleCropTransformation
import com.veganbeauty.app.R
import com.veganbeauty.app.core.base.RootieFragment
import com.veganbeauty.app.databinding.WeatherForecastBinding
import com.veganbeauty.app.features.quiz.QuizTestIntroFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class WeatherForecastFragment : RootieFragment() {

    private var _binding: WeatherForecastBinding? = null
    private val binding get() = _binding!!

    // API key for Google Gemini (Free tier). Replace with your own key.
    // If empty or equals placeholder, falls back to Rule-based skin/weather expert system.
    private val GEMINI_API_KEY = "YOUR_GEMINI_API_KEY_HERE"

    // Ho Chi Minh City coordinates fallback
    private val defaultLat = 10.8231
    private val defaultLng = 106.6297

    // Permission request launcher
    private val requestLocationLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineGranted || coarseGranted) {
            Toast.makeText(context, "Đã cấp quyền định vị thành công!", Toast.LENGTH_SHORT).show()
            getCurrentLocationAndLoadWeather()
        } else {
            Toast.makeText(context, "Quyền định vị bị từ chối. Sử dụng vị trí mặc định: TP. Hồ Chí Minh.", Toast.LENGTH_LONG).show()
            loadWeatherForCoordinates(defaultLat, defaultLng)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = WeatherForecastBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun setupUI(view: View) {
        setupToolbar()
        setupBottomNavigation()
        loadUserProfileData()
        checkLocationPermissionsAndLoad()
        setupFeedbackButtons()
    }

    private fun setupToolbar() {
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
        binding.btnNotification.setOnClickListener {
            Toast.makeText(context, "Hôm nay UV cao, hãy nhớ thoa kem chống nắng nhé!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadUserProfileData() {
        val prefs = requireContext().getSharedPreferences("RootieQuizPrefs", Context.MODE_PRIVATE)
        val username = com.veganbeauty.app.data.local.ProfileSession.getFullName(requireContext())
        val skinType = prefs.getString("SAVED_USER_SKIN_TYPE", "Da dầu nhạy cảm") ?: "Da dầu nhạy cảm"

        // Set dynamic greeting based on hour of the day
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        binding.tvGreeting.text = when (hour) {
            in 5..10 -> "CHÀO BUỔI SÁNG"
            in 11..13 -> "CHÀO BUỔI TRƯA"
            in 14..17 -> "CHÀO BUỔI CHIỀU"
            else -> "CHÀO BUỔI TỐI"
        }

        binding.tvUsername.text = username

        // Load avatar profile photo from unsplash (sync with account profile)
        binding.ivAvatar.load("https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=256&q=80") {
            crossfade(true)
            transformations(CircleCropTransformation())
            placeholder(android.R.color.darker_gray)
        }

        // Customise AI Insight text base on user skin type (with cached Gemini AI Insight fallback)
        val cachedInsight = prefs.getString("SAVED_CACHED_AI_INSIGHT", null)
        val insightText = if (!cachedInsight.isNullOrBlank()) {
            cachedInsight
        } else {
            when {
                skinType.contains("dầu", ignoreCase = true) -> 
                    "“Lượng dầu tiết ra hôm nay có thể tăng cao do nhiệt độ. Bạn nên dùng sữa rửa mặt tạo bọt dịu nhẹ và dưỡng ẩm dạng gel.”"
                skinType.contains("khô", ignoreCase = true) -> 
                    "“Thời tiết hôm nay dễ làm da mất ẩm, tạo cảm giác căng chặt. Hãy ưu tiên bổ sung serum HA và kem dưỡng khóa ẩm dày hơn.”"
                skinType.contains("nhạy cảm", ignoreCase = true) || skinType.contains("kích ứng", ignoreCase = true) -> 
                    "“Hàng rào bảo vệ da của bạn đang nhạy cảm, dễ đỏ rát dưới nắng. Nên thoa kem chống nắng vật lý và hạn chế sản phẩm chứa cồn/hương liệu.”"
                skinType.contains("hỗn hợp", ignoreCase = true) -> 
                    "“Vùng chữ T sẽ đổ dầu nhiều hơn dưới nắng nóng, trong khi hai bên má vẫn cần cấp ẩm nhẹ. Bạn nên dưỡng ẩm mỏng nhẹ toàn mặt.”"
                skinType.contains("thường", ignoreCase = true) -> 
                    "“Làn da của bạn ở trạng thái cân bằng lý tưởng. Hãy duy trì bảo vệ da bằng kem chống nắng phổ rộng và cấp ẩm nhẹ nhàng.”"
                skinType.contains("lão hóa", ignoreCase = true) -> 
                    "“Tia UV cao hôm nay đẩy nhanh lão hóa da. Hãy dùng thêm serum chống oxy hóa như Vitamin C dưới lớp kem chống nắng.”"
                skinType.contains("mất nước", ignoreCase = true) -> 
                    "“Da đang thiếu nước sâu bên dưới dù có thể đổ dầu bề mặt. Hãy cấp ẩm đa tầng bằng xịt khoáng và toner dưỡng ẩm.”"
                else -> 
                    "“Làn da $skinType cần được bảo vệ cẩn thận hôm nay. Hãy tập trung làm sạch dịu nhẹ và chống nắng kỹ càng.”"
            }
        }
        binding.tvAiInsightDesc.text = insightText

        // Customise the three metrics
        val (uvImpact, hydrationImpact, dustImpact) = when {
            skinType.contains("dầu", ignoreCase = true) -> 
                Triple("Tăng tiết bã nhờn", "Mất cân bằng ẩm", "Nguy cơ nổi mụn")
            skinType.contains("khô", ignoreCase = true) -> 
                Triple("Dễ bong tróc", "Thiếu nước trầm trọng", "Hàng rào da suy giảm")
            skinType.contains("nhạy cảm", ignoreCase = true) || skinType.contains("kích ứng", ignoreCase = true) -> 
                Triple("Rủi ro đỏ da", "Hàng rào yếu", "Kích ứng mẩn đỏ")
            skinType.contains("hỗn hợp", ignoreCase = true) -> 
                Triple("Nhờn vùng chữ T", "Khô hai bên má", "Tắc nghẽn nhẹ")
            skinType.contains("lão hóa", ignoreCase = true) -> 
                Triple("Tăng sắc tố UV", "Mất đàn hồi", "Tổn thương gốc tự do")
            skinType.contains("mất nước", ignoreCase = true) -> 
                Triple("Nếp nhăn giả", "Mất nước tế bào", "Bít tắc tuyến nhờn")
            else -> 
                Triple("Bảo vệ tối đa", "Duy trì ẩm tốt", "Làm sạch bình thường")
        }
        binding.tvUvImpact.text = uvImpact
        binding.tvHydrationImpact.text = hydrationImpact
        binding.tvDustImpact.text = dustImpact

        // Customise routine base on skin type
        updateRoutineLabels(skinType)
    }

    private fun updateRoutineLabels(skinType: String) {
        val isOily = skinType.contains("dầu", ignoreCase = true)
        val isSensitive = skinType.contains("nhạy cảm", ignoreCase = true) || skinType.contains("kích ứng", ignoreCase = true)
        val isDry = skinType.contains("khô", ignoreCase = true)
        val isCombination = skinType.contains("hỗn hợp", ignoreCase = true)
        val isAging = skinType.contains("lão hóa", ignoreCase = true)
        val isDehydrated = skinType.contains("mất nước", ignoreCase = true)

        val (cleanserName, cleanserSub) = when {
            isOily -> Pair("Gel rửa mặt BHA/Salicylic", "Kiềm dầu & làm sạch sâu bã nhờn")
            isSensitive -> Pair("Sữa rửa mặt không bọt", "Cực kỳ dịu nhẹ cho da nhạy cảm")
            isDry -> Pair("Sữa rửa mặt cấp ẩm", "Làm sạch dịu nhẹ, giữ ẩm tự nhiên")
            isCombination -> Pair("Gel rửa mặt cân bằng", "Sạch sâu vùng chữ T, dịu nhẹ vùng má")
            isAging -> Pair("Sữa rửa mặt chống lão hóa", "Sạch sâu nhẹ nhàng kèm tinh chất phục hồi")
            isDehydrated -> Pair("Sữa rửa mặt Amino Acid", "Không làm khô căng da sau khi rửa")
            else -> Pair("Sữa rửa mặt cân bằng pH", "Duy trì độ pH sinh lý lý tưởng cho da")
        }

        val (serumName, serumSub) = when {
            isOily -> Pair("Niacinamide 10% Serum", "Thu nhỏ lỗ chân lông, điều tiết dầu")
            isSensitive -> Pair("Serum phục hồi B5", "Làm dịu kích ứng, phục hồi hàng rào da")
            isDry -> Pair("Hyaluronic Acid (HA) Serum", "Cấp nước đa tầng, căng mọng da")
            isCombination -> Pair("Serum HA + Niacinamide", "Cân bằng dầu nước tối ưu")
            isAging -> Pair("Retinol / Peptide Serum", "Tăng sinh collagen, mờ nếp nhăn")
            isDehydrated -> Pair("Serum HA cấp nước sâu", "Bơm nước căng mọng tế bào da")
            else -> Pair("Vitamin C Serum", "Làm sáng và đều màu da tự nhiên")
        }

        val (moisturizerName, moisturizerSub) = when {
            isOily -> Pair("Dưỡng ẩm dạng Gel", "Thấm nhanh, mỏng nhẹ, không bóng nhờn")
            isSensitive -> Pair("Kem dưỡng Ceramide phục hồi", "Củng cố lớp màng bảo vệ tự nhiên")
            isDry -> Pair("Kem dưỡng ẩm Cream", "Khóa ẩm sâu, ngăn ngừa bong tróc da")
            isCombination -> Pair("Lotion dưỡng ẩm mỏng nhẹ", "Cấp ẩm đầy đủ mà không gây bí da")
            isAging -> Pair("Kem dưỡng săn chắc da", "Nuôi dưỡng sâu, tăng cường độ đàn hồi")
            isDehydrated -> Pair("Gel-cream khóa nước", "Khóa nước dưới da mà không bết dính")
            else -> Pair("Lotion dưỡng ẩm nhẹ", "Cấp ẩm vừa đủ cho làn da khỏe mạnh")
        }

        val (sunscreenName, sunscreenSub) = when {
            isOily -> Pair("Kem chống nắng kiềm dầu", "Finish khô thoáng, không bít tắc")
            isSensitive -> Pair("KCN vật lý thuần chay", "Chống nắng dịu nhẹ, không cay mắt")
            isDry -> Pair("Kem chống nắng dưỡng ẩm", "Bảo vệ da khô khỏi mất nước")
            isCombination -> Pair("KCN kiềm dầu dịu nhẹ", "Không gây mụn vùng chữ T")
            isAging -> Pair("Kem chống nắng chống lão hóa", "Ngăn ngừa sạm nám do tia UV")
            isDehydrated -> Pair("Kem chống nắng cấp nước", "Vừa bảo vệ vừa cấp nước làm dịu da")
            else -> Pair("Kem chống nắng phổ rộng", "Bảo vệ toàn diện trước tia UVA & UVB")
        }

        binding.layoutRoutineItem1.let { card ->
            val titleLayout = card.getChildAt(1) as? LinearLayout
            (titleLayout?.getChildAt(0) as? TextView)?.text = cleanserName
            (titleLayout?.getChildAt(1) as? TextView)?.text = cleanserSub
        }

        binding.layoutRoutineItem2.let { card ->
            val titleLayout = card.getChildAt(1) as? LinearLayout
            (titleLayout?.getChildAt(0) as? TextView)?.text = serumName
            (titleLayout?.getChildAt(1) as? TextView)?.text = serumSub
        }

        binding.layoutRoutineItem3.let { card ->
            val titleLayout = card.getChildAt(1) as? LinearLayout
            (titleLayout?.getChildAt(0) as? TextView)?.text = moisturizerName
            (titleLayout?.getChildAt(1) as? TextView)?.text = moisturizerSub
        }

        binding.layoutRoutineItem4.let { card ->
            val titleLayout = card.getChildAt(1) as? LinearLayout
            (titleLayout?.getChildAt(0) as? TextView)?.text = sunscreenName
            (titleLayout?.getChildAt(1) as? TextView)?.text = sunscreenSub
        }
    }

    private fun checkLocationPermissionsAndLoad() {
        val finePermission = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        val coarsePermission = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (finePermission == PackageManager.PERMISSION_GRANTED ||
            coarsePermission == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocationAndLoadWeather()
        } else {
            requestLocationLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun getCurrentLocationAndLoadWeather() {
        try {
            val locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            var location: Location? = null
            if (isNetworkEnabled) {
                location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            }
            if (location == null && isGpsEnabled) {
                location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            }

            if (location != null) {
                loadWeatherForCoordinates(location.latitude, location.longitude)
            } else {
                // First load fallback to avoid empty screen
                loadWeatherForCoordinates(defaultLat, defaultLng)

                val locationListener = object : android.location.LocationListener {
                    override fun onLocationChanged(loc: Location) {
                        loadWeatherForCoordinates(loc.latitude, loc.longitude)
                        locationManager.removeUpdates(this)
                    }
                    override fun onStatusChanged(p: String?, s: Int, e: Bundle?) {}
                    override fun onProviderEnabled(p: String) {}
                    override fun onProviderDisabled(p: String) {}
                }

                val providers = locationManager.getProviders(true)
                for (provider in providers) {
                    locationManager.requestLocationUpdates(
                        provider,
                        0L,
                        0f,
                        locationListener,
                        android.os.Looper.getMainLooper()
                    )
                }
            }
        } catch (e: SecurityException) {
            loadWeatherForCoordinates(defaultLat, defaultLng)
        }
    }

    private fun getCityName(lat: Double, lng: Double): String {
        return try {
            val geocoder = android.location.Geocoder(requireContext(), java.util.Locale.getDefault())
            val addresses = geocoder.getFromLocation(lat, lng, 1)
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                address.locality ?: address.subAdminArea ?: address.adminArea ?: "Thành phố Hồ Chí Minh"
            } else {
                "Thành phố Hồ Chí Minh"
            }
        } catch (e: Exception) {
            "Thành phố Hồ Chí Minh"
        }
    }

    private fun loadWeatherForCoordinates(lat: Double, lng: Double) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val cityName = getCityName(lat, lng)
            try {
                val urlString = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lng&current=temperature_2m,relative_humidity_2m&daily=uv_index_max&timezone=auto"
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                var temp = 32.0
                var humidity = 68
                var uv = 9.2
                var success = false

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(response)

                    val current = json.getJSONObject("current")
                    temp = current.getDouble("temperature_2m")
                    humidity = current.getInt("relative_humidity_2m")

                    val daily = json.getJSONObject("daily")
                    val uvArray = daily.getJSONArray("uv_index_max")
                    uv = if (uvArray.length() > 0) uvArray.getDouble(0) else 5.0
                    success = true
                }

                // Fetch real PM2.5 from Open-Meteo Air Quality API
                var realPm25 = -1.0
                if (success) {
                    try {
                        val aqUrlString = "https://air-quality-api.open-meteo.com/v1/air-quality?latitude=$lat&longitude=$lng&current=pm2_5&timezone=auto"
                        val aqUrl = URL(aqUrlString)
                        val aqConnection = aqUrl.openConnection() as HttpURLConnection
                        aqConnection.requestMethod = "GET"
                        aqConnection.connectTimeout = 5000
                        aqConnection.readTimeout = 5000
                        if (aqConnection.responseCode == HttpURLConnection.HTTP_OK) {
                            val aqResponse = aqConnection.inputStream.bufferedReader().use { it.readText() }
                            val aqJson = JSONObject(aqResponse)
                            val aqCurrent = aqJson.getJSONObject("current")
                            realPm25 = aqCurrent.getDouble("pm2_5")
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                withContext(Dispatchers.Main) {
                    if (success) {
                        updateWeatherUI(temp, humidity, uv, realPm25, cityName)
                    } else {
                        useFallbackWeatherData(cityName)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    useFallbackWeatherData(cityName)
                }
            }
        }
    }

    private fun useFallbackWeatherData(cityName: String) {
        updateWeatherUI(32.0, 68, 9.2, -1.0, cityName)
    }

    private fun updateWeatherUI(temp: Double, humidity: Int, uv: Double, pm25: Double, cityName: String) {
        if (_binding == null) return

        // Set city name
        binding.tvLocation.text = cityName

        // 1. Temp & Status text
        binding.tvTemperature.text = temp.toInt().toString()
        val weatherCondition = when {
            temp >= 33 -> "NẮNG NÓNG GAY GẮT"
            temp >= 28 -> "NẮNG NHIỀU, OI NHẸ"
            else -> "MÁT MẺ, DỄ CHỊU"
        }
        binding.tvWeatherCondition.text = weatherCondition
        binding.tvWeatherCondition.setTextColor(when {
            temp >= 33 -> Color.parseColor("#EB5757")
            temp >= 28 -> Color.parseColor("#E2B93B")
            else -> Color.parseColor("#677559")
        })

        // 2. Humidity level
        binding.tvHumidity.text = "$humidity%"
        val humidityLevel = when {
            humidity < 40 -> "Thấp"
            humidity <= 65 -> "Trung bình"
            else -> "Cao"
        }
        binding.tvHumidityLevel.text = humidityLevel
        binding.tvHumidityLevel.setTextColor(if (humidity < 40) Color.parseColor("#E2B93B") else Color.parseColor("#3E4D44"))

        // 3. UV index level
        binding.tvUvIndex.text = String.format("%.1f", uv)
        val uvLevel = when {
            uv < 3 -> "Thấp"
            uv < 6 -> "Trung bình"
            uv < 8 -> "Cao"
            else -> "Nguy hiểm"
        }
        binding.tvUvLevel.text = uvLevel
        binding.tvUvLevel.setTextColor(if (uv >= 8) Color.parseColor("#EB5757") else Color.parseColor("#E2B93B"))

        // 4. Dust level PM2.5 (Use real PM2.5 or simulate if failed/offline)
        val pm25Val = if (pm25 >= 0) pm25.toInt() else (15 + (temp * 0.4) + (humidity * 0.1)).toInt()
        binding.tvDustIndex.text = pm25Val.toString()
        val dustLevel = when {
            pm25Val < 25 -> "Tốt"
            pm25Val < 50 -> "Trung bình"
            else -> "Kém"
        }
        binding.tvDustLevel.text = dustLevel
        binding.tvDustLevel.setTextColor(if (pm25Val >= 50) Color.parseColor("#EB5757") else Color.parseColor("#3E4D44"))

        // 5. Dynamic Warning Banner
        val warningMsg = when {
            uv >= 8 -> "Hôm nay chỉ số UV ở mức nguy hiểm (${String.format("%.1f", uv)}). Bạn hãy bôi kem chống nắng kỹ và che chắn khi ra ngoài!"
            humidity < 40 -> "Độ ẩm không khí hôm nay khá thấp ($humidity%). Da bạn sẽ mất nước nhanh, hãy chú ý cấp khóa ẩm!"
            else -> "Hôm nay thời tiết khá nóng và độ ẩm trung bình. Hãy bảo vệ da khỏi tia UV hại và uống đủ nước!"
        }
        binding.tvWarningText.text = warningMsg

        // 6. Dynamic alert banner text
        binding.tvAlertText.text = when {
            uv >= 8 -> "Chỉ số UV hôm nay cực cao! Nhớ thoa lại kem chống nắng sau mỗi 2 giờ ra ngoài."
            temp >= 33 -> "Nhiệt độ nóng gay gắt, hãy dùng gel dưỡng ẩm mỏng nhẹ để tránh bít tắc lỗ chân lông."
            else -> "Hôm nay da bạn cần chống nắng và cấp ẩm nhiều hơn. Đừng quên nhé!"
        }

        // 7. Update skin state progress values base on temperature
        val oilyPercent = when {
            temp >= 33 -> 85
            temp >= 28 -> 75
            else -> 60
        }
        val hydrationPercent = when {
            humidity < 40 -> 40
            humidity <= 65 -> 55
            else -> 70
        }
        val sensitivityPercent = when {
            uv >= 8 -> 35
            temp >= 33 -> 30
            else -> 20
        }

        binding.tvOilyValue.text = "$oilyPercent%"
        binding.progressOily.progress = oilyPercent

        binding.tvHydrationValue.text = "$hydrationPercent%"
        binding.progressHydration.progress = hydrationPercent

        binding.tvSensitivityValue.text = "$sensitivityPercent%"
        binding.progressSensitivity.progress = sensitivityPercent

        // 8. Update AI Insight based on both user skin type and weather factors
        val prefs = requireContext().getSharedPreferences("RootieQuizPrefs", Context.MODE_PRIVATE)
        val skinType = prefs.getString("SAVED_USER_SKIN_TYPE", "Da dầu nhạy cảm") ?: "Da dầu nhạy cảm"
        fetchGeminiInsight(temp, humidity, uv, pm25Val, skinType)
    }

    private fun fetchGeminiInsight(temp: Double, humidity: Int, uv: Double, pm25: Int, skinType: String) {
        val prefs = requireContext().getSharedPreferences("RootieQuizPrefs", Context.MODE_PRIVATE)
        if (GEMINI_API_KEY.isBlank() || GEMINI_API_KEY == "YOUR_GEMINI_API_KEY_HERE") {
            updateRuleBasedAiInsight(temp, humidity, uv, pm25, skinType)
            return
        }

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val urlString = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$GEMINI_API_KEY"
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.connectTimeout = 8000
                connection.readTimeout = 8000
                connection.doOutput = true

                // Build request JSON
                val requestJson = JSONObject()
                
                val partsArray = org.json.JSONArray().apply {
                    put(JSONObject().put("text", "Đưa ra lời khuyên da liễu cho người dùng có làn da: $skinType. Thời tiết hiện tại: Nhiệt độ $temp°C, Độ ẩm $humidity%, Chỉ số UV ${String.format("%.1f", uv)}, Bụi mịn PM2.5 $pm25."))
                }
                val contentsArray = org.json.JSONArray().apply {
                    put(JSONObject().put("parts", partsArray))
                }
                requestJson.put("contents", contentsArray)

                val systemInstruction = JSONObject().apply {
                    val systemParts = org.json.JSONArray().apply {
                        put(JSONObject().put("text", "Bạn là trợ lý bác sĩ da liễu thông minh của ứng dụng ROOTIE - ứng dụng tư vấn chăm sóc da hữu cơ và thuần chay (Vegan Skincare). Nhiệm vụ của bạn là đưa ra 1 lời khuyên ngắn gọn, thiết thực và khoa học (tối đa 2-3 câu ngắn) bằng tiếng Việt cho người dùng dựa trên loại da và thời tiết thực tế được gửi. Khuyên dùng các giải pháp lành tính, tự nhiên, thuần chay, bảo vệ màng ẩm của da và khuyên thoa kem chống nắng/che chắn nếu UV cao. Hãy bắt đầu câu trả lời bằng ký tự mở ngoặc kép kép và kết thúc bằng đóng ngoặc kép kép (ví dụ: “Lời khuyên...”)."))
                    }
                    put("parts", systemParts)
                }
                requestJson.put("systemInstruction", systemInstruction)

                val generationConfig = JSONObject().apply {
                    put("temperature", 0.7)
                    put("maxOutputTokens", 250)
                }
                requestJson.put("generationConfig", generationConfig)

                // Write body
                connection.outputStream.use { os ->
                    val input = requestJson.toString().toByteArray(Charsets.UTF_8)
                    os.write(input, 0, input.size)
                }

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(response)
                    val candidates = json.getJSONArray("candidates")
                    if (candidates.length() > 0) {
                        val content = candidates.getJSONObject(0).getJSONObject("content")
                        val parts = content.getJSONArray("parts")
                        if (parts.length() > 0) {
                            val textResult = parts.getJSONObject(0).getString("text").trim()
                            
                            // Save successful AI insight to cache
                            prefs.edit().putString("SAVED_CACHED_AI_INSIGHT", textResult).apply()

                            withContext(Dispatchers.Main) {
                                if (_binding != null) {
                                    binding.tvAiInsightDesc.text = textResult
                                }
                            }
                            return@launch
                        }
                    }
                }
                
                // If response code is not OK or parsing failed, check cache before running rule-based fallback
                withContext(Dispatchers.Main) {
                    val cachedInsight = prefs.getString("SAVED_CACHED_AI_INSIGHT", null)
                    if (cachedInsight.isNullOrBlank()) {
                        updateRuleBasedAiInsight(temp, humidity, uv, pm25, skinType)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    val cachedInsight = prefs.getString("SAVED_CACHED_AI_INSIGHT", null)
                    if (cachedInsight.isNullOrBlank()) {
                        updateRuleBasedAiInsight(temp, humidity, uv, pm25, skinType)
                    }
                }
            }
        }
    }

    private fun updateRuleBasedAiInsight(temp: Double, humidity: Int, uv: Double, pm25: Int, skinType: String) {
        val baseInsight = when {
            skinType.contains("dầu nhạy cảm", ignoreCase = true) -> {
                when {
                    uv >= 8 -> "Thời tiết nắng gắt với tia UV cực cao (${String.format("%.1f", uv)}). Làn da dầu nhạy cảm rất dễ đỏ rát và kích ứng. Hãy ưu tiên bôi kem chống nắng vật lý mỏng nhẹ và che chắn kỹ."
                    temp >= 33 -> "Nhiệt độ cao ($temp°C) làm tăng tiết bã nhờn trong khi hàng rào bảo vệ da nhạy cảm mỏng yếu. Nên dùng gel dưỡng ẩm phục hồi dịu nhẹ và tránh chà xát da."
                    pm25 >= 50 -> "Chỉ số bụi mịn cao ($pm25) có thể gây ngứa rát và bít tắc. Hãy chú ý làm sạch dịu nhẹ với sữa rửa mặt không bọt hoặc gel dịu nhẹ."
                    else -> "Lượng dầu tiết ra ổn định nhưng da vẫn nhạy cảm. Hãy củng cố hàng rào da bằng serum B5 và kem dưỡng phục hồi Ceramide."
                }
            }
            skinType.contains("dầu", ignoreCase = true) -> {
                when {
                    temp >= 33 -> "Nhiệt độ cao ($temp°C) kích thích tuyến bã nhờn hoạt động cực mạnh. Hãy dùng sữa rửa mặt kiềm dầu dịu nhẹ và gel dưỡng ẩm mỏng nhẹ dạng nước để tránh bít tắc mụn."
                    humidity < 40 -> "Độ ẩm không khí thấp ($humidity%) làm da mất nước bề mặt, dễ tiết dầu bù nhiều hơn. Hãy uống nhiều nước và cấp ẩm nhẹ nhàng bằng toner hyaluronic acid."
                    else -> "Lượng bã nhờn có thể tăng nhẹ. Ưu tiên serum Niacinamide để điều tiết dầu thừa và se khít lỗ chân lông."
                }
            }
            skinType.contains("khô", ignoreCase = true) -> {
                when {
                    humidity < 40 -> "Độ ẩm không khí rất thấp ($humidity%), dễ gây khô căng, bong tróc rát da. Hãy dùng serum HA cấp nước đa tầng kết hợp kem dưỡng ẩm dạng đặc để khóa ẩm sâu."
                    temp >= 33 -> "Nhiệt độ cao ($temp°C) làm tăng sự mất nước qua da. Đừng quên xịt khoáng làm dịu da và thoa kem chống nắng cấp ẩm để bảo vệ da."
                    else -> "Da khô cần duy trì độ đàn hồi tốt. Hãy đắp mặt nạ dưỡng ẩm và dùng kem dưỡng khóa ẩm dày hơn vào buổi tối."
                }
            }
            skinType.contains("nhạy cảm", ignoreCase = true) || skinType.contains("kích ứng", ignoreCase = true) -> {
                when {
                    uv >= 6 -> "Tia UV cao (${String.format("%.1f", uv)}) rất dễ làm tổn hại hàng rào bảo vệ da mỏng yếu. Hãy luôn thoa kem chống nắng vật lý dành cho da nhạy cảm trước khi ra ngoài."
                    pm25 >= 50 -> "Không khí nhiều khói bụi ($pm25) dễ gây dị ứng mẩn đỏ. Nên làm sạch da nhẹ nhàng bằng nước tẩy trang micellar không chứa cồn ngay khi về nhà."
                    else -> "Da nhạy cảm cần sự tối giản. Tránh dùng sản phẩm chứa cồn khô, hương liệu hay retinol nồng độ cao vào hôm nay."
                }
            }
            skinType.contains("mụn", ignoreCase = true) -> {
                when {
                    temp >= 33 || pm25 >= 50 -> "Nắng nóng ($temp°C) và bụi mịn ($pm25) là tác nhân hàng đầu gây tắc nghẽn và sưng mụn. Hãy làm sạch sâu bằng gel rửa mặt chứa BHA/Salicylic Acid dịu nhẹ."
                    else -> "Tập trung kiểm soát dầu thừa và kháng viêm cho nốt mụn. Tránh sử dụng kem dưỡng ẩm quá dày gây bí bách da."
                }
            }
            skinType.contains("hỗn hợp", ignoreCase = true) -> {
                when {
                    temp >= 33 -> "Vùng chữ T sẽ bóng nhờn nhiều do nhiệt độ cao ($temp°C), trong khi hai bên má vẫn cần giữ ẩm. Hãy thoa lotion mỏng nhẹ toàn mặt và dùng giấy thấm dầu khi cần."
                    else -> "Cân bằng ẩm cho da hỗn hợp. Dùng toner cấp nước nhẹ nhàng và kem dưỡng ẩm mỏng để vùng chữ T thoáng sạch còn vùng má đủ ẩm."
                }
            }
            else -> {
                "Làn da của bạn tương đối ổn định hôm nay. Hãy duy trì thói quen làm sạch dịu nhẹ, cấp ẩm vừa đủ và bôi kem chống nắng bảo vệ da khỏi tia UV."
            }
        }
        binding.tvAiInsightDesc.text = "“$baseInsight”"
    }

    private fun setupFeedbackButtons() {
        val activeBg = ContextCompat.getDrawable(requireContext(), R.drawable.bg_btn_solid_feedback)
        val inactiveBg = ContextCompat.getDrawable(requireContext(), R.drawable.bg_btn_outlined_feedback)

        val activeColor = Color.parseColor("#F8FDF0")
        val inactiveColor = Color.parseColor("#67814D")

        val btnAppropriate = binding.btnSuitAppropriate
        val btnLighter = binding.btnSuitLighter
        val btnUnsuitable = binding.btnSuitUnsuitable

        val tvAppropriate = binding.tvSuitAppropriate
        val tvLighter = binding.tvSuitLighter
        val tvUnsuitable = binding.tvSuitUnsuitable

        val imgAppropriate = btnAppropriate.getChildAt(0) as ImageView
        val imgLighter = btnLighter.getChildAt(0) as ImageView
        val imgUnsuitable = btnUnsuitable.getChildAt(0) as ImageView

        fun resetButtons() {
            btnAppropriate.background = inactiveBg
            btnAppropriate.backgroundTintList = null
            tvAppropriate.setTextColor(inactiveColor)
            imgAppropriate.setColorFilter(inactiveColor)

            btnLighter.background = inactiveBg
            btnLighter.backgroundTintList = null
            tvLighter.setTextColor(inactiveColor)
            imgLighter.setColorFilter(inactiveColor)

            btnUnsuitable.background = inactiveBg
            btnUnsuitable.backgroundTintList = null
            tvUnsuitable.setTextColor(inactiveColor)
            imgUnsuitable.setColorFilter(inactiveColor)
        }

        btnAppropriate.setOnClickListener {
            resetButtons()
            btnAppropriate.background = activeBg
            btnAppropriate.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.primary)
            tvAppropriate.setTextColor(activeColor)
            imgAppropriate.setColorFilter(activeColor)
            Toast.makeText(context, "Cảm ơn bạn đã phản hồi! Rootie sẽ tiếp tục duy trì chu trình này.", Toast.LENGTH_SHORT).show()
        }

        btnLighter.setOnClickListener {
            resetButtons()
            btnLighter.background = activeBg
            btnLighter.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.primary)
            tvLighter.setTextColor(activeColor)
            imgLighter.setColorFilter(activeColor)
            Toast.makeText(context, "Ghi nhận! Chu trình chăm da tiếp theo sẽ mỏng nhẹ và tối giản hơn.", Toast.LENGTH_SHORT).show()
        }

        btnUnsuitable.setOnClickListener {
            resetButtons()
            btnUnsuitable.background = activeBg
            btnUnsuitable.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.primary)
            tvUnsuitable.setTextColor(activeColor)
            imgUnsuitable.setColorFilter(activeColor)
            Toast.makeText(context, "Rootie AI đang ghi nhận và điều chỉnh lại sản phẩm phù hợp hơn.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupBottomNavigation() {
        // Highlight middle tab "My skin" as active in weather screen
        binding.layoutBottomNav.navMyskin.let { navMySkin ->
            val icon = navMySkin.getChildAt(0) as? android.widget.ImageView
            val label = navMySkin.getChildAt(1) as? android.widget.TextView

            icon?.setColorFilter(Color.parseColor("#677559"))
            label?.setTextColor(Color.parseColor("#677559"))
            label?.setTypeface(null, Typeface.BOLD)
        }

        // Add other navigation links
        binding.layoutBottomNav.navAccount.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_container, com.veganbeauty.app.features.profile.AccountProfileFragment())
                .commit()
        }

        binding.layoutBottomNav.navShop.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_container, com.veganbeauty.app.features.shop.home.ShopHomeFragment())
                .commit()
        }

        binding.layoutBottomNav.navHome.setOnClickListener {
            // Back to quiz/home
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_container, QuizTestIntroFragment())
                .commit()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
