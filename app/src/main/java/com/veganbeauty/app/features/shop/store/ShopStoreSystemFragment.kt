package com.veganbeauty.app.features.shop.store

import android.Manifest
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.veganbeauty.app.R
import com.veganbeauty.app.core.base.RootieFragment
import com.veganbeauty.app.data.local.LocalJsonReader
import com.veganbeauty.app.data.local.RootieDatabase
import com.veganbeauty.app.data.local.entities.StoreEntity
import com.veganbeauty.app.data.repository.StoreRepository
import com.veganbeauty.app.databinding.DialogLocationPermissionBinding
import com.veganbeauty.app.databinding.ShopFragmentStoreSystemBinding
import com.veganbeauty.app.databinding.ShopItemStoreCardHorizontalBinding
import kotlinx.coroutines.launch

class ShopStoreSystemFragment : RootieFragment() {

    private var _binding: ShopFragmentStoreSystemBinding? = null
    private val binding get() = _binding!!

    private lateinit var database: RootieDatabase
    private lateinit var repository: StoreRepository

    private var allStoresList = listOf<StoreEntity>()
    private var displayedStoresList = listOf<StoreEntity>()

    private lateinit var storeCardAdapter: StoreCardAdapter
    private var userLocation: Location? = null
    private var notificationTriggered = false
    private var selectedIndex = 0
    private var hasCheckedPermissionOnStart = false

    // Core Android Location Manager
    private var locationManager: LocationManager? = null

    // System location permissions launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            onPermissionGranted()
        } else {
            onPermissionDenied()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        database = RootieDatabase.getDatabase(requireContext())
        repository = StoreRepository(database.storeDao(), LocalJsonReader(requireContext()))
        locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ShopFragmentStoreSystemBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun setupUI(view: View) {
        // Toolbar Back Click
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Search Bar Click -> Opens ShopStoreSelectionFragment directly in search history mode
        binding.btnSearch.setOnClickListener {
            val selectionFragment = ShopStoreSelectionFragment.newInstance(
                isSelectionMode = false,
                startInSearchMode = true
            )
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_container, selectionFragment)
                .addToBackStack(null)
                .commit()
        }

        // Filter Icon Click -> Opens ShopAddressSelectionFragment directly to select region
        binding.ivFilterIcon.setOnClickListener {
            val addressSelectionFragment = ShopAddressSelectionFragment.newInstance()
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_container, addressSelectionFragment)
                .addToBackStack(null)
                .commit()
        }

        // Listen for address selection result directly from ShopAddressSelectionFragment
        parentFragmentManager.setFragmentResultListener(
            ShopAddressSelectionFragment.REQUEST_KEY,
            viewLifecycleOwner
        ) { _, bundle ->
            val province = bundle.getString(ShopAddressSelectionFragment.RESULT_PROVINCE)
            val district = bundle.getString(ShopAddressSelectionFragment.RESULT_DISTRICT)
            if (!province.isNullOrEmpty() && !district.isNullOrEmpty()) {
                val selectionFragment = ShopStoreSelectionFragment.newInstance(
                    isSelectionMode = false,
                    initialProvince = province,
                    initialDistrict = district
                )
                parentFragmentManager.beginTransaction()
                    .replace(R.id.main_container, selectionFragment)
                    .addToBackStack(null)
                    .commit()
            }
        }

        // Bottom view list text click -> Opens ShopStoresBottomSheetFragment
        binding.btnViewList.setOnClickListener {
            if (displayedStoresList.isNotEmpty()) {
                val bottomSheet = ShopStoresBottomSheetFragment.newInstance(ArrayList(displayedStoresList))
                bottomSheet.show(parentFragmentManager, ShopStoresBottomSheetFragment.TAG)
            }
        }

        // Option 2: Image Map clicked opens Google Maps centered on current/selected location
        binding.ivMapBackground.setOnClickListener {
            val lat = if (displayedStoresList.isNotEmpty() && selectedIndex < displayedStoresList.size) {
                displayedStoresList[selectedIndex].lat
            } else {
                userLocation?.latitude ?: 10.775
            }
            val lng = if (displayedStoresList.isNotEmpty() && selectedIndex < displayedStoresList.size) {
                displayedStoresList[selectedIndex].lng
            } else {
                userLocation?.longitude ?: 106.701
            }
            openGoogleMaps(lat, lng)
        }

        // Uses My Location Overlay Button (Image 2 style)
        binding.btnUseMyLocation.setOnClickListener {
            showCustomPermissionDialog()
        }

        // GPS Target floating button (Image 1 style)
        binding.btnGpsTarget.setOnClickListener {
            // Re-fetch location and center/refresh map
            fetchLocation()
        }

        setupRecyclerView()
        loadStoresAndCheckPermission()
    }

    private fun setupRecyclerView() {
        storeCardAdapter = StoreCardAdapter()
        binding.rvStoreCards.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvStoreCards.adapter = storeCardAdapter

        // SnapHelper makes swipe snap to center like card pager
        val snapHelper = LinearSnapHelper()
        snapHelper.attachToRecyclerView(binding.rvStoreCards)

        binding.rvStoreCards.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val centerView = snapHelper.findSnapView(layoutManager)
                    if (centerView != null) {
                        val position = layoutManager.getPosition(centerView)
                        if (position != RecyclerView.NO_POSITION && position != selectedIndex) {
                            selectedIndex = position
                            highlightPin(position)
                        }
                    }
                }
            }
        })
    }

    private fun loadStoresAndCheckPermission() {
        viewLifecycleOwner.lifecycleScope.launch {
            repository.allStores.collect { stores ->
                allStoresList = stores
                if (allStoresList.isEmpty()) {
                    repository.refreshStores()
                } else {
                    if (!hasCheckedPermissionOnStart) {
                        hasCheckedPermissionOnStart = true
                        checkAndPromptPermission(autoPrompt = true)
                    } else {
                        checkAndPromptPermission(autoPrompt = false)
                    }
                }
            }
        }
    }

    private fun checkAndPromptPermission(autoPrompt: Boolean = false) {
        val finePermission = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
        val coarsePermission = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
        
        if (finePermission == PackageManager.PERMISSION_GRANTED || coarsePermission == PackageManager.PERMISSION_GRANTED) {
            onPermissionGranted()
        } else {
            if (autoPrompt) {
                showCustomPermissionDialog()
            } else {
                onPermissionDenied()
            }
        }
    }

    private fun showCustomPermissionDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_location_permission, null)
        val dialogBinding = DialogLocationPermissionBinding.bind(dialogView)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialogBinding.btnAllow.setOnClickListener {
            dialog.dismiss()
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
            onPermissionDenied()
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    private fun onPermissionGranted() {
        // Toggle Map View state to Image 1
        binding.btnUseMyLocation.visibility = View.GONE
        binding.btnGpsTarget.visibility = View.VISIBLE
        fetchLocation()
    }

    private fun onPermissionDenied() {
        // Toggle Map View state to Image 2
        binding.btnUseMyLocation.visibility = View.VISIBLE
        binding.btnGpsTarget.visibility = View.GONE
        binding.pinUser.visibility = View.GONE
        userLocation = null
        
        // Show default list (top 4 stores in Hanoi to make it distinct from user location in HCMC)
        val defaultStores = allStoresList.filter { 
            it.tinhThanh.contains("Hà Nội", ignoreCase = true) || it.diaChiDayDu.contains("Hà Nội", ignoreCase = true) 
        }.take(4)
        val finalStores = if (defaultStores.size == 4) defaultStores else allStoresList.take(4)
        
        updateMapWithPins(null, finalStores)
    }

    private fun fetchLocation() {
        try {
            val hasFine = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            val hasCoarse = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            
            if (!hasFine && !hasCoarse) {
                onPermissionDenied()
                return
            }

            var location: Location? = null
            
            // Try GPS provider
            if (locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true) {
                location = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            }
            
            // Try Network provider if GPS is null
            if (location == null && locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true) {
                location = locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            }

            // Try Passive provider
            if (location == null && locationManager?.isProviderEnabled(LocationManager.PASSIVE_PROVIDER) == true) {
                location = locationManager?.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
            }

            val finalLoc = location ?: Location("Mock").apply {
                latitude = 10.775
                longitude = 106.701
            }
            
            userLocation = finalLoc
            processStoresWithLocation(finalLoc)

            // Register background location updates in case location changes
            try {
                val listener = object : LocationListener {
                    override fun onLocationChanged(loc: Location) {
                        if (_binding == null) return
                        userLocation = loc
                        processStoresWithLocation(loc)
                        locationManager?.removeUpdates(this)
                    }
                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                    override fun onProviderEnabled(provider: String) {}
                    override fun onProviderDisabled(provider: String) {}
                }
                
                if (locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true) {
                    locationManager?.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000L, 5f, listener)
                } else if (locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true) {
                    locationManager?.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000L, 5f, listener)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
            onPermissionDenied()
        } catch (e: Exception) {
            e.printStackTrace()
            onPermissionDenied()
        }
    }

    private fun processStoresWithLocation(location: Location) {
        if (allStoresList.isEmpty()) return

        // Compute distances and sort by closest
        val sortedList = allStoresList.map { store ->
            val results = FloatArray(1)
            Location.distanceBetween(location.latitude, location.longitude, store.lat, store.lng, results)
            store to results[0] // distance in meters
        }.sortedBy { it.second }

        // Filter stores: take stores within 12.0 km, up to 6 stores.
        // Fall back to top 4 closest if there are no stores within 12km.
        val nearbyStores = sortedList.filter { it.second < 12000f }.map { it.first }.take(6)
        val finalStores = if (nearbyStores.isNotEmpty()) nearbyStores else sortedList.map { it.first }.take(4)
        
        updateMapWithPins(location, finalStores)
    }

    private fun updateMapWithPins(location: Location?, stores: List<StoreEntity>) {
        displayedStoresList = stores
        selectedIndex = 0

        // Gather coordinates of displayed pins and user
        val points = mutableListOf<Pair<Double, Double>>()
        stores.forEach { points.add(it.lat to it.lng) }
        location?.let { points.add(it.latitude to it.longitude) }

        if (points.isEmpty()) return

        val lats = points.map { it.first }
        val lngs = points.map { it.second }

        val minLat = lats.minOrNull() ?: 10.7
        val maxLat = lats.maxOrNull() ?: 10.8
        val minLng = lngs.minOrNull() ?: 106.6
        val maxLng = lngs.maxOrNull() ?: 106.7

        val latRange = maxLat - minLat
        val lngRange = maxLng - minLng

        val storePins = listOf(binding.pinStore1, binding.pinStore2, binding.pinStore3, binding.pinStore4, binding.pinStore5, binding.pinStore6)

        // Clone ConstraintSet for clPinsOverlay
        val constraintSet = ConstraintSet()
        constraintSet.clone(binding.clPinsOverlay)

        // Hide all pins first via ConstraintSet
        storePins.forEach { constraintSet.setVisibility(it.id, View.GONE) }
        constraintSet.setVisibility(binding.pinUser.id, View.GONE)

        // Positioning store pins dynamically via ConstraintLayout Bias
        stores.forEachIndexed { idx, store ->
            if (idx < storePins.size) {
                val pin = storePins[idx]
                constraintSet.setVisibility(pin.id, View.VISIBLE)

                val xBias = if (lngRange > 0.0) {
                    0.15f + 0.70f * ((store.lng - minLng) / lngRange).toFloat()
                } else {
                    0.5f
                }

                val yBias = if (latRange > 0.0) {
                    0.15f + 0.70f * ((maxLat - store.lat) / latRange).toFloat()
                } else {
                    0.5f
                }

                constraintSet.setHorizontalBias(pin.id, xBias.coerceIn(0.05f, 0.95f))
                constraintSet.setVerticalBias(pin.id, yBias.coerceIn(0.05f, 0.95f))

                pin.setOnClickListener {
                    binding.rvStoreCards.smoothScrollToPosition(idx)
                    selectedIndex = idx
                    highlightPin(idx)
                }
            }
        }

        // Positioning user location pin dynamically
        location?.let { loc ->
            val userPin = binding.pinUser
            constraintSet.setVisibility(userPin.id, View.VISIBLE)

            val xBias = if (lngRange > 0.0) {
                0.15f + 0.70f * ((loc.longitude - minLng) / lngRange).toFloat()
            } else {
                0.5f
            }

            val yBias = if (latRange > 0.0) {
                0.15f + 0.70f * ((maxLat - loc.latitude) / latRange).toFloat()
            } else {
                0.5f
            }

            constraintSet.setHorizontalBias(userPin.id, xBias.coerceIn(0.05f, 0.95f))
            constraintSet.setVerticalBias(userPin.id, yBias.coerceIn(0.05f, 0.95f))
        }

        // Apply ConstraintSet
        constraintSet.applyTo(binding.clPinsOverlay)

        // Update Bottom Panels
        binding.tvCountLabel.text = "Tìm kiếm ${stores.size} cửa hàng"
        storeCardAdapter.updateItems(stores)

        // Highlight initial pin
        highlightPin(0)

        // Proximity Notification Check
        location?.let { loc ->
            checkProximityAndNotify(loc, stores)
        }
    }

    private fun highlightPin(position: Int) {
        val storePins = listOf(binding.pinStore1, binding.pinStore2, binding.pinStore3, binding.pinStore4, binding.pinStore5, binding.pinStore6)
        storePins.forEachIndexed { index, frameLayout ->
            if (index == position) {
                // Highlight color
                frameLayout.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.primary)
                frameLayout.elevation = 8f
            } else {
                // Normal color
                frameLayout.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#7E8A83"))
                frameLayout.elevation = 2f
            }
        }
    }

    private fun checkProximityAndNotify(location: Location, stores: List<StoreEntity>) {
        if (notificationTriggered) return

        val closestStore = stores.firstOrNull() ?: return
        val results = FloatArray(1)
        Location.distanceBetween(location.latitude, location.longitude, closestStore.lat, closestStore.lng, results)
        val distance = results[0]

        // Trigger deal notification if within 1.5 km (or if using mock location for easy emulator/testing validation)
        val isMockLocation = (location.latitude == 10.775 && location.longitude == 106.701)
        if (distance < 1500f || isMockLocation) {
            notificationTriggered = true
            showDealNotifications(closestStore)
        }
    }

    private fun showDealNotifications(store: StoreEntity) {
        val ctx = context ?: return
        val titleText = "Ưu đãi gần bạn!"
        val messageText = "Bạn đang ở gần cửa hàng ${store.tenCuaHang}, vào săn deal ngay!"

        // 1. Show In-App Notification slide-down (Image 4 design)
        _binding?.let { b ->
            b.tvNotiMessage.text = messageText
            b.cvNotificationBanner.visibility = View.VISIBLE
            b.cvNotificationBanner.translationY = -300f
            
            b.cvNotificationBanner.animate()
                .translationY(0f)
                .setDuration(500)
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .start()
        }

        // Automatically hide in-app banner after 5 seconds
        Handler(Looper.getMainLooper()).postDelayed({
            _binding?.let { b ->
                b.cvNotificationBanner.animate()
                    .translationY(-300f)
                    .setDuration(500)
                    .withEndAction {
                        b.cvNotificationBanner.visibility = View.GONE
                    }
                    .start()
            }
        }, 5000)

        // 2. Trigger System Notification
        try {
            val channelId = "proximity_deal_channel"
            val channelName = "Ưu đãi gần bạn"
            val notiManager = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Thông báo khi bạn ở gần cửa hàng Rootie"
                }
                notiManager.createNotificationChannel(channel)
            }

            // Create notification layout intent
            val mapUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=${store.lat},${store.lng}")
            val intent = Intent(Intent.ACTION_VIEW, mapUri)
            val pendingIntent = android.app.PendingIntent.getActivity(
                ctx, 0, intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(ctx, channelId)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(titleText)
                .setContentText(messageText)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)

            notiManager.notify(101, builder.build())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun openGoogleMaps(lat: Double, lng: Double) {
        try {
            val mapUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$lat,$lng")
            val mapIntent = Intent(Intent.ACTION_VIEW, mapUri)
            startActivity(mapIntent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Không thể mở Google Maps", Toast.LENGTH_SHORT).show()
        }
    }

    private fun formatDistance(meters: Float): String {
        return if (meters < 1000f) {
            "${meters.toInt()}m"
        } else {
            String.format(java.util.Locale.US, "%.1fkm", meters / 1000f)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // RecyclerView Card Adapter (Inner class)
    inner class StoreCardAdapter : RecyclerView.Adapter<StoreCardAdapter.ViewHolder>() {

        private var items = listOf<StoreEntity>()

        inner class ViewHolder(val itemBinding: ShopItemStoreCardHorizontalBinding) :
            RecyclerView.ViewHolder(itemBinding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ShopItemStoreCardHorizontalBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val store = items[position]
            val b = holder.itemBinding

            b.tvStoreName.text = store.tenCuaHang
            b.tvStoreHours.text = "Mở cửa từ ${store.moCua} đến ${store.dongCua}"
            b.tvStoreAddress.text = store.diaChiDayDu

            // Click listener for directions to open Google Maps
            b.btnDirections.setOnClickListener {
                openGoogleMaps(store.lat, store.lng)
            }

            // Clicking the card highlights its pin on map
            // Clicking the card opens details page
            b.root.setOnClickListener {
                selectedIndex = position
                highlightPin(position)
                val detailFragment = ShopStoreDetailFragment.newInstance(store)
                parentFragmentManager.beginTransaction()
                    .replace(R.id.main_container, detailFragment)
                    .addToBackStack(null)
                    .commit()
            }
        }

        override fun getItemCount(): Int = items.size

        fun updateItems(newItems: List<StoreEntity>) {
            items = newItems
            notifyDataSetChanged()
        }
    }
}
