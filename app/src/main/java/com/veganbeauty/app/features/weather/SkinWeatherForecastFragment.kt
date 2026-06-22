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
import com.veganbeauty.app.databinding.SkinWeatherForecastBinding
import com.veganbeauty.app.features.quiz.QuizTestIntroFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.flow.first

class SkinWeatherForecastFragment : RootieFragment() {

    private var _binding: SkinWeatherForecastBinding? = null
    private val binding get() = _binding!!

    // API key for Google Gemini (Free tier). Replace with your own key.
    // If empty or equals placeholder, falls back to Rule-based skin/weather expert system.
    private val GEMINI_API_KEY = com.veganbeauty.app.BuildConfig.GEMINI_API_KEY

    // Ho Chi Minh City coordinates fallback
    private val defaultLat = 10.8231
    private val defaultLng = 106.6297

    private var isViewingHistory = false

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
        _binding = SkinWeatherForecastBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun setupUI(view: View) {
        setupToolbar()
        setupBottomNavigation()
        loadUserProfileData()
        checkLocationPermissionsAndLoad()
        setupFeedbackButtons()

        binding.cardAiInsight.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_container, com.veganbeauty.app.features.ai.SkinAiChatFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.btnExitHistory.setOnClickListener {
            isViewingHistory = false
            binding.layoutHistoryBanner.visibility = View.GONE
            checkLocationPermissionsAndLoad()
        }

        setupSkinWeatherNotificationSwitch()
    }

    private fun setupSkinWeatherNotificationSwitch() {
        val context = requireContext()
        val isEnabled = com.veganbeauty.app.data.local.ProfileSession.isSkinWeatherNotiEnabled(context)
        updateSwitchUI(binding.switchSkinWeatherForecast, binding.switchSkinWeatherForecastThumb, isEnabled)

        binding.btnToggleSkinWeatherNoti.setOnClickListener {
            val ctx = requireContext()
            val nextState = !com.veganbeauty.app.data.local.ProfileSession.isSkinWeatherNotiEnabled(ctx)
            com.veganbeauty.app.data.local.ProfileSession.setSkinWeatherNotiEnabled(ctx, nextState)
            updateSwitchUI(binding.switchSkinWeatherForecast, binding.switchSkinWeatherForecastThumb, nextState)
            if (nextState) {
                com.veganbeauty.app.features.weather.DailySkinWeatherScheduler.scheduleDailyNotification(ctx)
                Toast.makeText(ctx, "Đã bật thông báo thời tiết và da lúc 06:30 sáng", Toast.LENGTH_SHORT).show()
            } else {
                com.veganbeauty.app.features.weather.DailySkinWeatherScheduler.cancelDailyNotification(ctx)
                Toast.makeText(ctx, "Đã tắt thông báo thời tiết và da hàng ngày", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateSwitchUI(container: android.widget.FrameLayout, thumb: ImageView, enabled: Boolean) {
        if (enabled) {
            container.setBackgroundResource(R.drawable.ic_switch_track_on_yellow)
            val lp = thumb.layoutParams as android.widget.FrameLayout.LayoutParams
            lp.gravity = android.view.Gravity.CENTER_VERTICAL or android.view.Gravity.END
            lp.marginStart = 0
            lp.marginEnd = (2 * resources.displayMetrics.density).toInt()
            thumb.layoutParams = lp
        } else {
            container.setBackgroundResource(R.drawable.ic_switch_track_off)
            val lp = thumb.layoutParams as android.widget.FrameLayout.LayoutParams
            lp.gravity = android.view.Gravity.CENTER_VERTICAL or android.view.Gravity.START
            lp.marginEnd = 0
            lp.marginStart = (2 * resources.displayMetrics.density).toInt()
            thumb.layoutParams = lp
        }
    }

    override fun onResume() {
        super.onResume()
        if (_binding != null) {
            val isEnabled = com.veganbeauty.app.data.local.ProfileSession.isSkinWeatherNotiEnabled(requireContext())
            updateSwitchUI(binding.switchSkinWeatherForecast, binding.switchSkinWeatherForecastThumb, isEnabled)
            loadUserProfileData()
        }
    }

    private fun setupToolbar() {
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
        binding.btnHistory.setOnClickListener {
            showHistoryDialog()
        }
        binding.btnNotification.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_container, com.veganbeauty.app.features.account.notification.AccountNotificationFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    private fun loadUserProfileData() {
        val username = com.veganbeauty.app.data.local.ProfileSession.getFullName(requireContext())

        // Set dynamic greeting based on hour of the day
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        binding.tvGreeting.text = when (hour) {
            in 5..10 -> "CHÀO BUỔI SÁNG"
            in 11..13 -> "CHÀO BUỔI TRƯA"
            in 14..17 -> "CHÀO BUỔI CHIỀU"
            else -> "CHÀO BUỔI TỐI"
        }

        binding.tvUsername.text = username

        // Load avatar profile photo using AvatarLoader
        val avatarUrl = com.veganbeauty.app.data.local.ProfileSession.getAvatar(requireContext())
        com.veganbeauty.app.utils.AvatarLoader.loadAvatar(binding.ivAvatar, avatarUrl)
    }

    private fun updateRoutineLabels(skinType: String, temp: Double, humidity: Int, uv: Double, pm25: Int) {
        val isOily = skinType.contains("dầu", ignoreCase = true)
        val isSensitive = skinType.contains("nhạy cảm", ignoreCase = true) || skinType.contains("kích ứng", ignoreCase = true)
        val isDry = skinType.contains("khô", ignoreCase = true)
        val isCombination = skinType.contains("hỗn hợp", ignoreCase = true)
        val isAging = skinType.contains("lão hóa", ignoreCase = true)
        val isDehydrated = skinType.contains("mất nước", ignoreCase = true)

        val matchedProducts = SkinWeatherProductMatcher.matchProductsForWeatherAndSkin(requireContext(), temp, humidity, skinType)

        // 1. Cleanser
        val matchedCleanser = matchedProducts["Cleanser"]
        val cleanserName = matchedCleanser?.name ?: when {
            isOily -> "Gel rửa mặt BHA/Salicylic"
            isSensitive -> "Sữa rửa mặt không bọt"
            isDry -> "Sữa rửa mặt cấp ẩm"
            isCombination -> "Gel rửa mặt cân bằng"
            isAging -> "Sữa rửa mặt chống lão hóa"
            isDehydrated -> "Sữa rửa mặt Amino Acid"
            else -> "Sữa rửa mặt cân bằng pH"
        }
        val cleanserSub = matchedCleanser?.let { "${it.notes} (Phù hợp: ${it.suitabilityScore}%)" } ?: when {
            isOily -> "Kiềm dầu & làm sạch sâu bã nhờn"
            isSensitive -> "Cực kỳ dịu nhẹ cho da nhạy cảm"
            isDry -> "Làm sạch dịu nhẹ, giữ ẩm tự nhiên"
            isCombination -> "Sạch sâu vùng chữ T, dịu nhẹ vùng má"
            isAging -> "Sạch sâu nhẹ nhàng kèm tinh chất phục hồi"
            isDehydrated -> "Không làm khô căng da sau khi rửa"
            else -> "Duy trì độ pH sinh lý lý tưởng cho da"
        }
        val finalCleanserSub = if (pm25 >= 50) {
            "$cleanserSub & sạch sâu bụi mịn PM2.5"
        } else {
            cleanserSub
        }

        // 2. Serum
        val matchedSerum = matchedProducts["Serum"]
        val serumName = matchedSerum?.name ?: when {
            isOily -> "Niacinamide 10% Serum"
            isSensitive -> "Serum phục hồi B5"
            isDry -> "Hyaluronic Acid (HA) Serum"
            isCombination -> "Serum HA + Niacinamide"
            isAging -> "Retinol / Peptide Serum"
            isDehydrated -> "Serum HA cấp nước sâu"
            else -> "Vitamin C Serum"
        }
        val serumSub = matchedSerum?.let { "${it.notes} (Phù hợp: ${it.suitabilityScore}%)" } ?: when {
            isOily -> "Thu nhỏ lỗ chân lông, điều tiết dầu"
            isSensitive -> "Làm dịu kích ứng, phục hồi hàng rào da"
            isDry -> "Cấp nước đa tầng, căng mọng da"
            isCombination -> "Cân bằng dầu nước tối ưu"
            isAging -> "Tăng sinh collagen, mờ nếp nhăn"
            isDehydrated -> "Bơm nước căng mọng tế bào da"
            else -> "Làm sáng và đều màu da tự nhiên"
        }

        // 3. Moisturizer
        val matchedMoisturizer = matchedProducts["Moisturizer"]
        val moisturizerName = matchedMoisturizer?.name ?: when {
            isOily -> "Dưỡng ẩm dạng Gel"
            isSensitive -> "Kem dưỡng Ceramide phục hồi"
            isDry -> "Kem dưỡng ẩm Cream"
            isCombination -> "Lotion dưỡng ẩm mỏng nhẹ"
            isAging -> "Kem dưỡng săn chắc da"
            isDehydrated -> "Gel-cream khóa nước"
            else -> "Lotion dưỡng ẩm nhẹ"
        }
        val moisturizerSub = matchedMoisturizer?.let { "${it.notes} (Phù hợp: ${it.suitabilityScore}%)" } ?: when {
            isOily -> "Thấm nhanh, mỏng nhẹ, không bóng nhờn"
            isSensitive -> "Củng cố lớp màng bảo vệ tự nhiên"
            isDry -> "Khóa ẩm sâu, ngăn ngừa bong tróc da"
            isCombination -> "Cấp ẩm đầy đủ mà không gây bí da"
            isAging -> "Nuôi dưỡng sâu, tăng cường độ đàn hồi"
            isDehydrated -> "Khóa nước dưới da mà không bết dính"
            else -> "Lotion dưỡng ẩm nhẹ"
        }
        val finalMoisturizerSub = when {
            humidity < 40 -> "$moisturizerSub (Khóa ẩm tăng cường vì thời tiết khô)"
            temp >= 33 -> "$moisturizerSub (Thoa lớp mỏng nhẹ tránh bí tắc ngày nóng)"
            else -> moisturizerSub
        }

        // 4. Sunscreen
        val matchedSunscreen = matchedProducts["Sunscreen"]
        val sunscreenName = matchedSunscreen?.name ?: when {
            isOily -> "Kem chống nắng kiềm dầu"
            isSensitive -> "KCN vật lý thuần chay"
            isDry -> "Kem chống nắng dưỡng ẩm"
            isCombination -> "KCN kiềm dầu dịu nhẹ"
            isAging -> "Kem chống nắng chống lão hóa"
            isDehydrated -> "Kem chống nắng cấp nước"
            else -> "Kem chống nắng phổ rộng"
        }
        val sunscreenSub = matchedSunscreen?.let { "${it.notes} (Phù hợp: ${it.suitabilityScore}%)" } ?: when {
            isOily -> "Finish khô thoáng, không bít tắc"
            isSensitive -> "Chống nắng dịu nhẹ, không cay mắt"
            isDry -> "Bảo vệ da khô khỏi mất nước"
            isCombination -> "Không gây mụn vùng chữ T"
            isAging -> "Ngăn ngừa sạm nám do tia UV"
            isDehydrated -> "Vừa bảo vệ vừa cấp nước làm dịu da"
            else -> "Bảo vệ toàn diện trước tia UVA & UVB"
        }
        val finalSunscreenSub = when {
            uv >= 8.0 -> "$sunscreenSub (Tia UV nguy hiểm: ${String.format("%.1f", uv)} - thoa lại sau mỗi 2h)"
            uv >= 5.0 -> "$sunscreenSub (Tia UV cao: ${String.format("%.1f", uv)} - thoa lại sau mỗi 3h)"
            else -> sunscreenSub
        }

        binding.layoutRoutineItem1.let { card ->
            val titleLayout = card.getChildAt(1) as? LinearLayout
            (titleLayout?.getChildAt(0) as? TextView)?.text = cleanserName
            (titleLayout?.getChildAt(1) as? TextView)?.text = finalCleanserSub
            card.setOnClickListener {
                navigateToProductDetail(matchedProducts["Cleanser"]?.id, cleanserName)
            }
        }

        binding.layoutRoutineItem2.let { card ->
            val titleLayout = card.getChildAt(1) as? LinearLayout
            (titleLayout?.getChildAt(0) as? TextView)?.text = serumName
            (titleLayout?.getChildAt(1) as? TextView)?.text = serumSub
            card.setOnClickListener {
                navigateToProductDetail(matchedProducts["Serum"]?.id, serumName)
            }
        }

        binding.layoutRoutineItem3.let { card ->
            val titleLayout = card.getChildAt(1) as? LinearLayout
            (titleLayout?.getChildAt(0) as? TextView)?.text = moisturizerName
            (titleLayout?.getChildAt(1) as? TextView)?.text = finalMoisturizerSub
            card.setOnClickListener {
                navigateToProductDetail(matchedProducts["Moisturizer"]?.id, moisturizerName)
            }
        }

        binding.layoutRoutineItem4.let { card ->
            val titleLayout = card.getChildAt(1) as? LinearLayout
            (titleLayout?.getChildAt(0) as? TextView)?.text = sunscreenName
            (titleLayout?.getChildAt(1) as? TextView)?.text = finalSunscreenSub
            card.setOnClickListener {
                navigateToProductDetail(matchedProducts["Sunscreen"]?.id, sunscreenName)
            }
        }
    }

    private fun navigateToProductDetail(productId: String?, productName: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            val db = com.veganbeauty.app.data.local.RootieDatabase.getDatabase(requireContext())
            var product: com.veganbeauty.app.data.local.entities.ProductEntity? = null
            if (!productId.isNullOrEmpty()) {
                product = db.productDao().getProductById(productId)
            }
            if (product == null && productName.isNotEmpty()) {
                try {
                    val all = db.productDao().getAllProducts().first()
                    product = all.find { it.name.equals(productName, ignoreCase = true) }
                        ?: all.find { it.name.contains(productName, ignoreCase = true) }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            if (product != null) {
                val detailFragment = com.veganbeauty.app.features.shop.product.detail.ShopDetailFragment().apply {
                    setProduct(product)
                }
                parentFragmentManager.beginTransaction()
                    .replace(R.id.main_container, detailFragment)
                    .addToBackStack(null)
                    .commit()
            } else {
                Toast.makeText(context, "Sản phẩm không có sẵn trên cửa hàng!", Toast.LENGTH_SHORT).show()
            }
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
                        updateWeatherUI(temp, humidity, uv, realPm25, cityName, lat, lng)
                    } else {
                        useFallbackWeatherData(cityName, lat, lng)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    useFallbackWeatherData(cityName, lat, lng)
                }
            }
        }
    }

    private fun useFallbackWeatherData(cityName: String, lat: Double, lng: Double) {
        updateWeatherUI(32.0, 68, 9.2, -1.0, cityName, lat, lng)
    }

    private fun updateWeatherUI(temp: Double, humidity: Int, uv: Double, pm25: Double, cityName: String, lat: Double, lng: Double) {
        if (_binding == null) return

        val weatherCondition = when {
            temp >= 33 -> "NẮNG NÓNG GAY GẮT"
            temp >= 28 -> "NẮNG NHIỀU, OI NHẸ"
            else -> "MÁT MẺ, DỄ CHỊU"
        }
        val pm25Val = if (pm25 >= 0) pm25.toInt() else (15 + (temp * 0.4) + (humidity * 0.1)).toInt()

        // Save weather details to SharedPreferences for AI Chat context
        try {
            val prefs = requireContext().getSharedPreferences("RootieQuizPrefs", Context.MODE_PRIVATE)
            prefs.edit().apply {
                putFloat("SAVED_WEATHER_TEMP", temp.toFloat())
                putInt("SAVED_WEATHER_HUMIDITY", humidity)
                putFloat("SAVED_WEATHER_UV", uv.toFloat())
                putInt("SAVED_WEATHER_PM25", pm25Val)
                putString("SAVED_WEATHER_CITY", cityName)
                putString("SAVED_WEATHER_CONDITION", weatherCondition)
                putFloat("SAVED_WEATHER_LAT", lat.toFloat())
                putFloat("SAVED_WEATHER_LNG", lng.toFloat())
                apply()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Set city name
        binding.tvLocation.text = cityName

        // 1. Temp & Status text
        binding.tvTemperature.text = temp.toInt().toString()
        binding.tvWeatherCondition.text = weatherCondition
        binding.tvWeatherCondition.setTextColor(when {
            temp >= 33 -> androidx.core.content.ContextCompat.getColor(requireContext(), R.color.status_level_red)
            temp >= 28 -> androidx.core.content.ContextCompat.getColor(requireContext(), R.color.status_level_yellow)
            else -> androidx.core.content.ContextCompat.getColor(requireContext(), R.color.secondary)
        })

        // Set Sun icon for Day (6:00 AM - 5:59 PM) and Moon icon for Night (6:00 PM - 5:59 AM)
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val isNight = hour < 6 || hour >= 18
        if (isNight) {
            binding.ivWeatherIcon.setImageResource(R.drawable.ic_skin_moon)
            binding.ivWeatherIcon.setColorFilter(Color.parseColor("#90A4AE")) // Soft silver-blue
        } else {
            binding.ivWeatherIcon.setImageResource(R.drawable.ic_weather_sun)
            binding.ivWeatherIcon.setColorFilter(Color.parseColor("#F0C43D")) // Sun yellow
        }

        // 2. Humidity level
        binding.tvHumidity.text = "$humidity%"
        val humidityLevel = when {
            humidity < 40 -> "Thấp"
            humidity <= 65 -> "Trung bình"
            else -> "Cao"
        }
        binding.tvHumidityLevel.text = humidityLevel

        val humidityColorRes = when {
            humidity < 40 -> R.color.status_level_orange
            humidity <= 65 -> R.color.status_level_blue
            else -> R.color.status_level_blue
        }
        val humidityBgRes = when {
            humidity < 40 -> R.drawable.bg_card_status_orange
            humidity <= 65 -> R.drawable.bg_card_status_blue
            else -> R.drawable.bg_card_status_blue
        }
        val humidityColorVal = androidx.core.content.ContextCompat.getColor(requireContext(), humidityColorRes)
        binding.tvHumidity.setTextColor(humidityColorVal)
        binding.tvHumidityLevel.setTextColor(humidityColorVal)
        binding.layoutHumidityBox.setBackgroundResource(humidityBgRes)

        // 3. UV index level
        binding.tvUvIndex.text = String.format("%.1f", uv)
        val uvLevel = when {
            uv < 3 -> "Thấp"
            uv < 6 -> "Trung bình"
            uv < 8 -> "Cao"
            else -> "Nguy hiểm"
        }
        binding.tvUvLevel.text = uvLevel

        val uvColorRes = when {
            uv < 3 -> R.color.status_level_green
            uv < 6 -> R.color.status_level_yellow
            uv < 8 -> R.color.status_level_orange
            else -> R.color.status_level_red
        }
        val uvBgRes = when {
            uv < 3 -> R.drawable.bg_card_status_green
            uv < 6 -> R.drawable.bg_card_status_yellow
            uv < 8 -> R.drawable.bg_card_status_orange
            else -> R.drawable.bg_card_status_red
        }
        val uvColorVal = androidx.core.content.ContextCompat.getColor(requireContext(), uvColorRes)
        binding.tvUvIndex.setTextColor(uvColorVal)
        binding.tvUvLevel.setTextColor(uvColorVal)
        binding.layoutUvBox.setBackgroundResource(uvBgRes)

        // 4. Dust level PM2.5 (Use real PM2.5 or simulate if failed/offline)
        binding.tvDustIndex.text = pm25Val.toString()
        val dustLevel = when {
            pm25Val < 25 -> "Tốt"
            pm25Val < 50 -> "Trung bình"
            else -> "Kém"
        }
        binding.tvDustLevel.text = dustLevel

        val dustColorRes = when {
            pm25Val < 25 -> R.color.status_level_green
            pm25Val < 50 -> R.color.status_level_yellow
            else -> R.color.status_level_red
        }
        val dustBgRes = when {
            pm25Val < 25 -> R.drawable.bg_card_status_green
            pm25Val < 50 -> R.drawable.bg_card_status_yellow
            else -> R.drawable.bg_card_status_red
        }
        val dustColorVal = androidx.core.content.ContextCompat.getColor(requireContext(), dustColorRes)
        binding.tvDustIndex.setTextColor(dustColorVal)
        binding.tvDustLevel.setTextColor(dustColorVal)
        binding.layoutDustBox.setBackgroundResource(dustBgRes)

        // 5. Dynamic Warning Banner
        val warningMsg = when {
            uv >= 8 -> "Hôm nay chỉ số UV ở mức nguy hiểm (${String.format("%.1f", uv)}). Bạn hãy bôi kem chống nắng kỹ và che chắn khi ra ngoài!"
            humidity < 40 -> "Độ ẩm không khí hôm nay khá thấp ($humidity%). Da bạn sẽ mất nước nhanh, hãy chú ý cấp khóa ẩm!"
            else -> "Hôm nay thời tiết khá nóng và độ ẩm trung bình. Hãy bảo vệ da khỏi tia UV hại và uống đủ nước!"
        }
        binding.tvWarningText.text = warningMsg

        val (warningBgRes, warningTextColRes) = when {
            uv >= 8 || pm25Val >= 50 -> Pair(R.drawable.bg_warning_red, R.color.warning_text_red)
            humidity < 40 || uv >= 5.0 || pm25Val >= 25 -> Pair(R.drawable.bg_warning_orange, R.color.warning_text_orange)
            else -> Pair(R.drawable.bg_warning_yellow, R.color.warning_text_yellow)
        }
        binding.layoutWarningBox.setBackgroundResource(warningBgRes)
        val warningTextColorVal = androidx.core.content.ContextCompat.getColor(requireContext(), warningTextColRes)
        binding.tvWarningText.setTextColor(warningTextColorVal)
        binding.ivWarningIcon.setColorFilter(warningTextColorVal)

        // 6. Dynamic alert banner text
        binding.tvAlertText.text = when {
            uv >= 8 -> "Chỉ số UV hôm nay cực cao! Nhớ thoa lại kem chống nắng sau mỗi 2 giờ ra ngoài."
            temp >= 33 -> "Nhiệt độ nóng gay gắt, hãy dùng gel dưỡng ẩm mỏng nhẹ để tránh bít tắc lỗ chân lông."
            else -> "Hôm nay da bạn cần chống nắng và cấp ẩm nhiều hơn. Đừng quên nhé!"
        }

        // 7. Update skin state progress values base on skinType and weather factors
        val prefs = requireContext().getSharedPreferences("RootieQuizPrefs", Context.MODE_PRIVATE)
        val skinType = prefs.getString("SAVED_USER_SKIN_TYPE", "Da dầu nhạy cảm") ?: "Da dầu nhạy cảm"

        val isOily = skinType.contains("dầu", ignoreCase = true)
        val isSensitive = skinType.contains("nhạy cảm", ignoreCase = true) || skinType.contains("kích ứng", ignoreCase = true)
        val isDry = skinType.contains("khô", ignoreCase = true)
        val isCombination = skinType.contains("hỗn hợp", ignoreCase = true)
        val isNormal = skinType.contains("thường", ignoreCase = true)
        val isDehydrated = skinType.contains("mất nước", ignoreCase = true)

        var baseOily = when {
            isOily -> 70
            isCombination -> 55
            isDehydrated -> 50
            isNormal -> 40
            isDry -> 20
            else -> 45
        }
        baseOily += when {
            temp >= 33 -> 15
            temp >= 28 -> 8
            temp < 22 -> -5
            else -> 0
        }
        val oilyPercent = baseOily.coerceIn(10, 95)

        var baseHydration = when {
            isNormal -> 70
            isOily -> 60
            isCombination -> 55
            isSensitive -> 50
            isDry -> 35
            isDehydrated -> 25
            else -> 50
        }
        baseHydration += when {
            humidity < 40 -> -15
            humidity < 55 -> -5
            humidity > 75 -> 10
            else -> 0
        }
        if (uv >= 8.0) baseHydration -= 5
        val hydrationPercent = baseHydration.coerceIn(10, 95)

        var baseSensitivity = when {
            isSensitive -> 55
            isDehydrated -> 35
            isDry -> 30
            isCombination -> 20
            isOily -> 20
            else -> 15
        }
        baseSensitivity += when {
            uv >= 8.0 -> 20
            uv >= 5.0 -> 10
            else -> 0
        }
        baseSensitivity += if (temp >= 33) 10 else 0
        baseSensitivity += if (pm25Val >= 50) 15 else if (pm25Val >= 25) 5 else 0
        val sensitivityPercent = baseSensitivity.coerceIn(5, 95)

        binding.tvOilyValue.text = "$oilyPercent%"
        binding.progressOily.progress = oilyPercent
        val oilyColor = when {
            oilyPercent < 40 -> ContextCompat.getColor(requireContext(), R.color.status_level_green)
            oilyPercent <= 70 -> ContextCompat.getColor(requireContext(), R.color.status_level_yellow)
            else -> ContextCompat.getColor(requireContext(), R.color.status_level_red)
        }
        binding.tvOilyValue.setTextColor(oilyColor)
        binding.progressOily.progressTintList = android.content.res.ColorStateList.valueOf(oilyColor)

        binding.tvHydrationValue.text = "$hydrationPercent%"
        binding.progressHydration.progress = hydrationPercent
        val hydrationColor = when {
            hydrationPercent < 40 -> ContextCompat.getColor(requireContext(), R.color.status_level_red)
            hydrationPercent <= 65 -> ContextCompat.getColor(requireContext(), R.color.status_level_yellow)
            else -> ContextCompat.getColor(requireContext(), R.color.status_level_green)
        }
        binding.tvHydrationValue.setTextColor(hydrationColor)
        binding.progressHydration.progressTintList = android.content.res.ColorStateList.valueOf(hydrationColor)

        binding.tvSensitivityValue.text = "$sensitivityPercent%"
        binding.progressSensitivity.progress = sensitivityPercent
        val sensitivityColor = when {
            sensitivityPercent < 30 -> ContextCompat.getColor(requireContext(), R.color.status_level_green)
            sensitivityPercent <= 60 -> ContextCompat.getColor(requireContext(), R.color.status_level_yellow)
            else -> ContextCompat.getColor(requireContext(), R.color.status_level_red)
        }
        binding.tvSensitivityValue.setTextColor(sensitivityColor)
        binding.progressSensitivity.progressTintList = android.content.res.ColorStateList.valueOf(sensitivityColor)

        // Customise the three metrics dynamically based on skinType and weather
        val (uvImpact, hydrationImpact, dustImpact) = when {
            skinType.contains("dầu", ignoreCase = true) -> 
                Triple(
                    if (uv >= 6.0) "Tăng bã nhờn & sạm da" else "Tăng tiết bã nhờn",
                    if (humidity < 55) "Dầu nước mất cân bằng" else "Độ ẩm cân bằng",
                    if (pm25Val >= 50) "Nguy cơ tắc nghẽn mụn" else "Bám dính dầu nhờn"
                )
            skinType.contains("khô", ignoreCase = true) -> 
                Triple(
                    if (uv >= 6.0) "Dễ rát & sạm da khô" else "Dễ mất ẩm bong tróc",
                    if (humidity < 55) "Thiếu nước nghiêm trọng" else "Giảm khô căng nhẹ",
                    if (pm25Val >= 50) "Hàng rào bảo vệ yếu" else "Khô ngứa do khói bụi"
                )
            skinType.contains("nhạy cảm", ignoreCase = true) || skinType.contains("kích ứng", ignoreCase = true) -> 
                Triple(
                    if (uv >= 5.0) "Nguy cơ đỏ rát cao" else "Dễ kích ứng nhẹ",
                    if (humidity < 55) "Màng ẩm tổn thương" else "Đủ ẩm dễ chịu",
                    if (pm25Val >= 50) "Mẩn ngứa bít tắc" else "Bám dính bụi bẩn"
                )
            skinType.contains("hỗn hợp", ignoreCase = true) -> 
                Triple(
                    if (uv >= 6.0) "Vùng chữ T nhờn rát" else "Vùng chữ T tiết dầu",
                    if (humidity < 55) "Hai bên má khô rát" else "Cân bằng vùng má",
                    if (pm25Val >= 50) "Bít tắc vùng chữ T" else "Tích tụ bụi nhẹ"
                )
            skinType.contains("lão hóa", ignoreCase = true) -> 
                Triple(
                    if (uv >= 6.0) "Tia UV gây sạm nám" else "Đẩy nhanh lão hóa",
                    if (humidity < 55) "Mất nước nếp nhăn sâu" else "Giữ ẩm tế bào",
                    if (pm25Val >= 50) "Tổn thương gốc tự do" else "Tác nhân ô nhiễm"
                )
            skinType.contains("mất nước", ignoreCase = true) -> 
                Triple(
                    if (uv >= 6.0) "Rát sạm khô căng" else "Nếp nhăn giả do UV",
                    if (humidity < 45) "Mất nước tế bào sâu" else "Cải thiện tình trạng khô",
                    if (pm25Val >= 50) "Tắc tuyến bã nhờn" else "Bụi gây khô bề mặt"
                )
            else -> 
                Triple(
                    if (uv >= 6.0) "Chống nắng tối đa" else "Bảo vệ dịu nhẹ",
                    if (humidity < 55) "Cần cấp ẩm nhẹ" else "Duy trì ẩm tốt",
                    if (pm25Val >= 50) "Làm sạch sâu bụi mịn" else "Làm sạch bình thường"
                )
        }
        binding.tvUvImpact.text = uvImpact
        binding.tvHydrationImpact.text = hydrationImpact
        binding.tvDustImpact.text = dustImpact

        // 8. Update recommended routine labels
        updateRoutineLabels(skinType, temp, humidity, uv, pm25Val)

        // 9. Update AI Insight based on both user skin type and weather factors
        fetchGeminiInsight(temp, humidity, uv, pm25Val, cityName, skinType, oilyPercent, hydrationPercent, sensitivityPercent)
    }

    private fun fetchGeminiInsight(
        temp: Double,
        humidity: Int,
        uv: Double,
        pm25: Int,
        cityName: String,
        skinType: String,
        oily: Int,
        hydration: Int,
        sensitivity: Int
    ) {
        val prefs = requireContext().getSharedPreferences("RootieQuizPrefs", Context.MODE_PRIVATE)
        if (GEMINI_API_KEY.isBlank() || GEMINI_API_KEY == "YOUR_GEMINI_API_KEY_HERE") {
            updateRuleBasedAiInsight(temp, humidity, uv, pm25, cityName, skinType, oily, hydration, sensitivity)
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
                                    saveDiagnosticRecord(temp, humidity, uv, pm25, cityName, skinType, oily, hydration, sensitivity, textResult)
                                }
                            }
                            return@launch
                        }
                    }
                }
                
                withContext(Dispatchers.Main) {
                    val cachedInsight = prefs.getString("SAVED_CACHED_AI_INSIGHT", null)
                    if (!cachedInsight.isNullOrBlank()) {
                        binding.tvAiInsightDesc.text = cachedInsight
                        saveDiagnosticRecord(temp, humidity, uv, pm25, cityName, skinType, oily, hydration, sensitivity, cachedInsight)
                    } else {
                        updateRuleBasedAiInsight(temp, humidity, uv, pm25, cityName, skinType, oily, hydration, sensitivity)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    val cachedInsight = prefs.getString("SAVED_CACHED_AI_INSIGHT", null)
                    if (!cachedInsight.isNullOrBlank()) {
                        binding.tvAiInsightDesc.text = cachedInsight
                        saveDiagnosticRecord(temp, humidity, uv, pm25, cityName, skinType, oily, hydration, sensitivity, cachedInsight)
                    } else {
                        updateRuleBasedAiInsight(temp, humidity, uv, pm25, cityName, skinType, oily, hydration, sensitivity)
                    }
                }
            }
        }
    }

    private fun updateRuleBasedAiInsight(
        temp: Double,
        humidity: Int,
        uv: Double,
        pm25: Int,
        cityName: String,
        skinType: String,
        oily: Int,
        hydration: Int,
        sensitivity: Int
    ) {
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
        val finalInsight = "“$baseInsight”"
        binding.tvAiInsightDesc.text = finalInsight
        saveDiagnosticRecord(temp, humidity, uv, pm25, cityName, skinType, oily, hydration, sensitivity, finalInsight)
    }

    private fun saveDiagnosticRecord(
        temp: Double,
        humidity: Int,
        uv: Double,
        pm25: Int,
        cityName: String,
        skinType: String,
        oily: Int,
        hydration: Int,
        sensitivity: Int,
        insightText: String
    ) {
        if (isViewingHistory) return
        try {
            val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
            val dateStr = sdf.format(java.util.Date())

            val routineItems = mutableListOf<SkinWeatherDiagnostic.RoutineItem>()
            val categoryNames = listOf("Cleanser", "Serum", "Moisturizer", "Sunscreen")
            val layoutIds = listOf(
                binding.layoutRoutineItem1,
                binding.layoutRoutineItem2,
                binding.layoutRoutineItem3,
                binding.layoutRoutineItem4
            )

            for (i in layoutIds.indices) {
                val card = layoutIds[i]
                val titleLayout = card.getChildAt(1) as? LinearLayout
                val pName = (titleLayout?.getChildAt(0) as? TextView)?.text?.toString() ?: ""
                val pDesc = (titleLayout?.getChildAt(1) as? TextView)?.text?.toString() ?: ""
                routineItems.add(
                    SkinWeatherDiagnostic.RoutineItem(
                        category = categoryNames[i],
                        productName = pName,
                        description = pDesc
                    )
                )
            }

            val diagnostic = SkinWeatherDiagnostic(
                id = java.util.UUID.randomUUID().toString(),
                timestamp = System.currentTimeMillis(),
                date = dateStr,
                city = cityName,
                temperature = temp,
                humidity = humidity,
                uv = uv,
                pm25 = pm25,
                skinType = skinType,
                oilyPercent = oily,
                hydrationPercent = hydration,
                sensitivityPercent = sensitivity,
                insight = insightText,
                recommendedRoutine = routineItems
            )

            SkinWeatherHistoryManager.saveDiagnostic(requireContext(), diagnostic)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupFeedbackButtons() {
        val activeBg = ContextCompat.getDrawable(requireContext(), R.drawable.bg_btn_solid_feedback)
        val inactiveBg = ContextCompat.getDrawable(requireContext(), R.drawable.bg_btn_outlined_feedback)

        val colorAppropriate = Color.parseColor("#67814D")
        val colorLighter = Color.parseColor("#D88B2A")
        val colorUnsuitable = Color.parseColor("#E35B5B")

        val bgAppropriate = Color.parseColor("#EAF3E8")
        val bgLighter = Color.parseColor("#FDF7ED")
        val bgUnsuitable = Color.parseColor("#FEEFEE")

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
            btnAppropriate.backgroundTintList = android.content.res.ColorStateList.valueOf(bgAppropriate)
            tvAppropriate.setTextColor(colorAppropriate)
            imgAppropriate.setColorFilter(colorAppropriate)

            btnLighter.background = inactiveBg
            btnLighter.backgroundTintList = android.content.res.ColorStateList.valueOf(bgLighter)
            tvLighter.setTextColor(colorLighter)
            imgLighter.setColorFilter(colorLighter)

            btnUnsuitable.background = inactiveBg
            btnUnsuitable.backgroundTintList = android.content.res.ColorStateList.valueOf(bgUnsuitable)
            tvUnsuitable.setTextColor(colorUnsuitable)
            imgUnsuitable.setColorFilter(colorUnsuitable)
        }

        btnAppropriate.setOnClickListener {
            resetButtons()
            btnAppropriate.background = activeBg
            btnAppropriate.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.status_level_green)
            tvAppropriate.setTextColor(Color.WHITE)
            imgAppropriate.setColorFilter(Color.WHITE)
            Toast.makeText(context, "Cảm ơn bạn đã phản hồi! Rootie sẽ tiếp tục duy trì chu trình này.", Toast.LENGTH_SHORT).show()
        }

        btnLighter.setOnClickListener {
            resetButtons()
            btnLighter.background = activeBg
            btnLighter.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.status_level_yellow)
            tvLighter.setTextColor(Color.WHITE)
            imgLighter.setColorFilter(Color.WHITE)
            Toast.makeText(context, "Ghi nhận! Chu trình chăm da tiếp theo sẽ mỏng nhẹ và tối giản hơn.", Toast.LENGTH_SHORT).show()
        }

        btnUnsuitable.setOnClickListener {
            resetButtons()
            btnUnsuitable.background = activeBg
            btnUnsuitable.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.status_level_red)
            tvUnsuitable.setTextColor(Color.WHITE)
            imgUnsuitable.setColorFilter(Color.WHITE)
            Toast.makeText(context, "Rootie AI đang ghi nhận và điều chỉnh lại sản phẩm phù hợp hơn.", Toast.LENGTH_SHORT).show()
        }

        // Set initial state: Appropriate is highlighted green
        resetButtons()
        btnAppropriate.background = activeBg
        btnAppropriate.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.status_level_green)
        tvAppropriate.setTextColor(Color.WHITE)
        imgAppropriate.setColorFilter(Color.WHITE)
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
            // Back to home
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_container, com.veganbeauty.app.features.home.HomeFragment())
                .commit()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun showHistoryDialog() {
        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(requireContext())
        val dialogView = layoutInflater.inflate(R.layout.dialog_skin_weather_history, null)
        dialog.setContentView(dialogView)

        val rvHistory = dialogView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rv_weather_history)
        val btnClose = dialogView.findViewById<TextView>(R.id.btn_close_history)

        rvHistory.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())

        // Fetch history (Local first)
        val localHistory = SkinWeatherHistoryManager.getHistory(requireContext())
        val adapter = WeatherHistoryAdapter(localHistory) { selectedDiagnostic ->
            displayHistoricalDiagnostic(selectedDiagnostic)
            dialog.dismiss()
        }
        rvHistory.adapter = adapter

        // Sync from Firestore to get latest
        SkinWeatherHistoryManager.syncFromFirestore(requireContext()) { syncedList ->
            if (isAdded && _binding != null) {
                rvHistory.adapter = WeatherHistoryAdapter(syncedList) { selectedDiagnostic ->
                    displayHistoricalDiagnostic(selectedDiagnostic)
                    dialog.dismiss()
                }
            }
        }

        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun displayHistoricalDiagnostic(diagnostic: SkinWeatherDiagnostic) {
        isViewingHistory = true
        
        // 1. Show history banner
        binding.layoutHistoryBanner.visibility = View.VISIBLE
        binding.tvHistoryBannerText.text = "Bạn đang xem dữ liệu lịch sử ngày ${diagnostic.date}"

        // 2. Set city name
        binding.tvLocation.text = diagnostic.city

        // 3. Temp & Status
        binding.tvTemperature.text = diagnostic.temperature.toInt().toString()
        val weatherCondition = when {
            diagnostic.temperature >= 33 -> "NẮNG NÓNG GAY GẮT"
            diagnostic.temperature >= 28 -> "NẮNG NHIỀU, OI NHẸ"
            else -> "MÁT MẺ, DỄ CHỊU"
        }
        binding.tvWeatherCondition.text = weatherCondition
        binding.tvWeatherCondition.setTextColor(when {
            diagnostic.temperature >= 33 -> ContextCompat.getColor(requireContext(), R.color.status_level_red)
            diagnostic.temperature >= 28 -> ContextCompat.getColor(requireContext(), R.color.status_level_yellow)
            else -> ContextCompat.getColor(requireContext(), R.color.secondary)
        })

        // Sun/Moon Icon
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val isNight = hour < 6 || hour >= 18
        if (isNight) {
            binding.ivWeatherIcon.setImageResource(R.drawable.ic_skin_moon)
            binding.ivWeatherIcon.setColorFilter(Color.parseColor("#90A4AE"))
        } else {
            binding.ivWeatherIcon.setImageResource(R.drawable.ic_weather_sun)
            binding.ivWeatherIcon.setColorFilter(Color.parseColor("#F0C43D"))
        }

        // 4. Humidity
        binding.tvHumidity.text = "${diagnostic.humidity}%"
        val humidityLevel = when {
            diagnostic.humidity < 40 -> "Thấp"
            diagnostic.humidity <= 65 -> "Trung bình"
            else -> "Cao"
        }
        binding.tvHumidityLevel.text = humidityLevel

        val humidityColorRes = when {
            diagnostic.humidity < 40 -> R.color.status_level_orange
            diagnostic.humidity <= 65 -> R.color.status_level_blue
            else -> R.color.status_level_blue
        }
        val humidityBgRes = when {
            diagnostic.humidity < 40 -> R.drawable.bg_card_status_orange
            diagnostic.humidity <= 65 -> R.drawable.bg_card_status_blue
            else -> R.drawable.bg_card_status_blue
        }
        val humidityColorVal = ContextCompat.getColor(requireContext(), humidityColorRes)
        binding.tvHumidity.setTextColor(humidityColorVal)
        binding.tvHumidityLevel.setTextColor(humidityColorVal)
        binding.layoutHumidityBox.setBackgroundResource(humidityBgRes)

        // 5. UV Index
        binding.tvUvIndex.text = String.format("%.1f", diagnostic.uv)
        val uvLevel = when {
            diagnostic.uv < 3 -> "Thấp"
            diagnostic.uv < 6 -> "Trung bình"
            diagnostic.uv < 8 -> "Cao"
            else -> "Nguy hiểm"
        }
        binding.tvUvLevel.text = uvLevel

        val uvColorRes = when {
            diagnostic.uv < 3 -> R.color.status_level_green
            diagnostic.uv < 6 -> R.color.status_level_yellow
            diagnostic.uv < 8 -> R.color.status_level_orange
            else -> R.color.status_level_red
        }
        val uvBgRes = when {
            diagnostic.uv < 3 -> R.drawable.bg_card_status_green
            diagnostic.uv < 6 -> R.drawable.bg_card_status_yellow
            diagnostic.uv < 8 -> R.drawable.bg_card_status_orange
            else -> R.drawable.bg_card_status_red
        }
        val uvColorVal = ContextCompat.getColor(requireContext(), uvColorRes)
        binding.tvUvIndex.setTextColor(uvColorVal)
        binding.tvUvLevel.setTextColor(uvColorVal)
        binding.layoutUvBox.setBackgroundResource(uvBgRes)

        // 6. PM2.5 Dust
        binding.tvDustIndex.text = diagnostic.pm25.toString()
        val dustLevel = when {
            diagnostic.pm25 < 25 -> "Tốt"
            diagnostic.pm25 < 50 -> "Trung bình"
            else -> "Kém"
        }
        binding.tvDustLevel.text = dustLevel

        val dustColorRes = when {
            diagnostic.pm25 < 25 -> R.color.status_level_green
            diagnostic.pm25 < 50 -> R.color.status_level_yellow
            else -> R.color.status_level_red
        }
        val dustBgRes = when {
            diagnostic.pm25 < 25 -> R.drawable.bg_card_status_green
            diagnostic.pm25 < 50 -> R.drawable.bg_card_status_yellow
            else -> R.drawable.bg_card_status_red
        }
        val dustColorVal = ContextCompat.getColor(requireContext(), dustColorRes)
        binding.tvDustIndex.setTextColor(dustColorVal)
        binding.tvDustLevel.setTextColor(dustColorVal)
        binding.layoutDustBox.setBackgroundResource(dustBgRes)

        // 7. Dynamic Warning Box
        val warningMsg = when {
            diagnostic.uv >= 8 -> "Hôm nay chỉ số UV ở mức nguy hiểm (${String.format("%.1f", diagnostic.uv)}). Bạn hãy bôi kem chống nắng kỹ và che chắn khi ra ngoài!"
            diagnostic.humidity < 40 -> "Độ ẩm không khí hôm nay khá thấp (${diagnostic.humidity}%). Da bạn sẽ mất nước nhanh, hãy chú ý cấp khóa ẩm!"
            else -> "Hôm nay thời tiết khá nóng và độ ẩm trung bình. Hãy bảo vệ da khỏi tia UV hại và uống đủ nước!"
        }
        binding.tvWarningText.text = warningMsg

        val (warningBgRes, warningTextColRes) = when {
            diagnostic.uv >= 8 || diagnostic.pm25 >= 50 -> Pair(R.drawable.bg_warning_red, R.color.warning_text_red)
            diagnostic.humidity < 40 || diagnostic.uv >= 5.0 || diagnostic.pm25 >= 25 -> Pair(R.drawable.bg_warning_orange, R.color.warning_text_orange)
            else -> Pair(R.drawable.bg_warning_yellow, R.color.warning_text_yellow)
        }
        binding.layoutWarningBox.setBackgroundResource(warningBgRes)
        val warningTextColorVal = ContextCompat.getColor(requireContext(), warningTextColRes)
        binding.tvWarningText.setTextColor(warningTextColorVal)
        binding.ivWarningIcon.setColorFilter(warningTextColorVal)

        // 8. General Alert Text
        binding.tvAlertText.text = when {
            diagnostic.uv >= 8 -> "Chỉ số UV hôm nay cực cao! Nhớ thoa lại kem chống nắng sau mỗi 2 giờ ra ngoài."
            diagnostic.temperature >= 33 -> "Nhiệt độ nóng gay gắt, hãy dùng gel dưỡng ẩm mỏng nhẹ để tránh bít tắc lỗ chân lông."
            else -> "Hôm nay da bạn cần chống nắng và cấp ẩm nhiều hơn. Đừng quên nhé!"
        }

        // 9. Progress values
        binding.tvOilyValue.text = "${diagnostic.oilyPercent}%"
        binding.progressOily.progress = diagnostic.oilyPercent
        val oilyColor = when {
            diagnostic.oilyPercent < 40 -> ContextCompat.getColor(requireContext(), R.color.status_level_green)
            diagnostic.oilyPercent <= 70 -> ContextCompat.getColor(requireContext(), R.color.status_level_yellow)
            else -> ContextCompat.getColor(requireContext(), R.color.status_level_red)
        }
        binding.tvOilyValue.setTextColor(oilyColor)
        binding.progressOily.progressTintList = android.content.res.ColorStateList.valueOf(oilyColor)

        binding.tvHydrationValue.text = "${diagnostic.hydrationPercent}%"
        binding.progressHydration.progress = diagnostic.hydrationPercent
        val hydrationColor = when {
            diagnostic.hydrationPercent < 40 -> ContextCompat.getColor(requireContext(), R.color.status_level_red)
            diagnostic.hydrationPercent <= 65 -> ContextCompat.getColor(requireContext(), R.color.status_level_yellow)
            else -> ContextCompat.getColor(requireContext(), R.color.status_level_green)
        }
        binding.tvHydrationValue.setTextColor(hydrationColor)
        binding.progressHydration.progressTintList = android.content.res.ColorStateList.valueOf(hydrationColor)

        binding.tvSensitivityValue.text = "${diagnostic.sensitivityPercent}%"
        binding.progressSensitivity.progress = diagnostic.sensitivityPercent
        val sensitivityColor = when {
            diagnostic.sensitivityPercent < 30 -> ContextCompat.getColor(requireContext(), R.color.status_level_green)
            diagnostic.sensitivityPercent <= 60 -> ContextCompat.getColor(requireContext(), R.color.status_level_yellow)
            else -> ContextCompat.getColor(requireContext(), R.color.status_level_red)
        }
        binding.tvSensitivityValue.setTextColor(sensitivityColor)
        binding.progressSensitivity.progressTintList = android.content.res.ColorStateList.valueOf(sensitivityColor)

        // 10. AI Metric rows (impacts)
        val isOily = diagnostic.skinType.contains("dầu", ignoreCase = true)
        val isDry = diagnostic.skinType.contains("khô", ignoreCase = true)
        val isSensitive = diagnostic.skinType.contains("nhạy cảm", ignoreCase = true) || diagnostic.skinType.contains("kích ứng", ignoreCase = true)
        val isCombination = diagnostic.skinType.contains("hỗn hợp", ignoreCase = true)
        val isAging = diagnostic.skinType.contains("lão hóa", ignoreCase = true)
        val isDehydrated = diagnostic.skinType.contains("mất nước", ignoreCase = true)

        val (uvImpact, hydrationImpact, dustImpact) = when {
            isOily -> 
                Triple(
                    if (diagnostic.uv >= 6.0) "Tăng bã nhờn & sạm da" else "Tăng tiết bã nhờn",
                    if (diagnostic.humidity < 55) "Dầu nước mất cân bằng" else "Độ ẩm cân bằng",
                    if (diagnostic.pm25 >= 50) "Nguy cơ tắc nghẽn mụn" else "Bám dính dầu nhờn"
                )
            isDry -> 
                Triple(
                    if (diagnostic.uv >= 6.0) "Dễ rát & sạm da khô" else "Dễ mất ẩm bong tróc",
                    if (diagnostic.humidity < 55) "Thiếu nước nghiêm trọng" else "Giảm khô căng nhẹ",
                    if (diagnostic.pm25 >= 50) "Hàng rào bảo vệ yếu" else "Khô ngứa do khói bụi"
                )
            isSensitive -> 
                Triple(
                    if (diagnostic.uv >= 5.0) "Nguy cơ đỏ rát cao" else "Dễ kích ứng nhẹ",
                    if (diagnostic.humidity < 55) "Màng ẩm tổn thương" else "Đủ ẩm dễ chịu",
                    if (diagnostic.pm25 >= 50) "Mẩn ngứa bít tắc" else "Bám dính bụi bẩn"
                )
            isCombination -> 
                Triple(
                    if (diagnostic.uv >= 6.0) "Vùng chữ T nhờn rát" else "Vùng chữ T tiết dầu",
                    if (diagnostic.humidity < 55) "Hai bên má khô rát" else "Cân bằng vùng má",
                    if (diagnostic.pm25 >= 50) "Bít tắc vùng chữ T" else "Tích tụ bụi nhẹ"
                )
            isAging -> 
                Triple(
                    if (diagnostic.uv >= 6.0) "Tia UV gây sạm nám" else "Đẩy nhanh lão hóa",
                    if (diagnostic.humidity < 55) "Mất nước nếp nhăn sâu" else "Giữ ẩm tế bào",
                    if (diagnostic.pm25 >= 50) "Tổn thương gốc tự do" else "Tác nhân ô nhiễm"
                )
            isDehydrated -> 
                Triple(
                    if (diagnostic.uv >= 6.0) "Rát sạm khô căng" else "Nếp nhăn giả do UV",
                    if (diagnostic.humidity < 45) "Mất nước tế bào sâu" else "Cải thiện tình trạng khô",
                    if (diagnostic.pm25 >= 50) "Tắc tuyến bã nhờn" else "Bụi gây khô bề mặt"
                )
            else -> 
                Triple(
                    if (diagnostic.uv >= 6.0) "Chống nắng tối đa" else "Bảo vệ dịu nhẹ",
                    if (diagnostic.humidity < 55) "Cần cấp ẩm nhẹ" else "Duy trì ẩm tốt",
                    if (diagnostic.pm25 >= 50) "Làm sạch sâu bụi mịn" else "Làm sạch bình thường"
                )
        }
        binding.tvUvImpact.text = uvImpact
        binding.tvHydrationImpact.text = hydrationImpact
        binding.tvDustImpact.text = dustImpact

        // 11. Routine Steps
        val routineItems = diagnostic.recommendedRoutine
        val layoutIds = listOf(
            binding.layoutRoutineItem1,
            binding.layoutRoutineItem2,
            binding.layoutRoutineItem3,
            binding.layoutRoutineItem4
        )
        for (i in layoutIds.indices) {
            if (i < routineItems.size) {
                val card = layoutIds[i]
                val item = routineItems[i]
                val titleLayout = card.getChildAt(1) as? LinearLayout
                (titleLayout?.getChildAt(0) as? TextView)?.text = item.productName
                (titleLayout?.getChildAt(1) as? TextView)?.text = item.description
                card.setOnClickListener {
                    navigateToProductDetail(null, item.productName)
                }
            }
        }

        // 12. AI Insight Description
        binding.tvAiInsightDesc.text = diagnostic.insight
    }

    private inner class WeatherHistoryAdapter(
        private val items: List<SkinWeatherDiagnostic>,
        private val onItemClick: (SkinWeatherDiagnostic) -> Unit
    ) : androidx.recyclerview.widget.RecyclerView.Adapter<WeatherHistoryAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(view) {
            val tvDate: TextView = view.findViewById(R.id.tv_history_item_date)
            val tvCity: TextView = view.findViewById(R.id.tv_history_item_city)
            val tvTemp: TextView = view.findViewById(R.id.tv_history_item_temp)
            val tvHumidity: TextView = view.findViewById(R.id.tv_history_item_humidity)
            val tvUv: TextView = view.findViewById(R.id.tv_history_item_uv)
            val tvInsight: TextView = view.findViewById(R.id.tv_history_item_insight)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_skin_weather_history, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.tvDate.text = item.date
            holder.tvCity.text = item.city
            holder.tvTemp.text = "${item.temperature.toInt()}°C"
            holder.tvHumidity.text = "Độ ẩm: ${item.humidity}%"
            holder.tvUv.text = "UV: ${String.format("%.1f", item.uv)}"
            holder.tvInsight.text = item.insight

            holder.itemView.setOnClickListener {
                onItemClick(item)
            }
        }

        override fun getItemCount(): Int = items.size
    }
}
