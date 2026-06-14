package com.veganbeauty.app.features.ai

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.veganbeauty.app.core.base.RootieFragment
import com.veganbeauty.app.data.local.LocalJsonReader
import com.veganbeauty.app.data.local.ProfileSession
import com.veganbeauty.app.data.local.entities.ProductEntity
import com.veganbeauty.app.databinding.SkinAiChatBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SkinAiChatFragment : RootieFragment() {

    private var _binding: SkinAiChatBinding? = null
    private val binding get() = _binding!!

    private lateinit var chatAdapter: RootieChatAdapter
    private val chatList = mutableListOf<RootieChatItem>()
    private lateinit var allProducts: List<ProductEntity>

    // Load Gemini API Key from BuildConfig
    private val GEMINI_API_KEY = com.veganbeauty.app.BuildConfig.GEMINI_API_KEY

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = SkinAiChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun setupUI(view: View) {
        allProducts = LocalJsonReader(requireContext()).getAllProducts()

        // Hide floating chatbot head in chat screen
        activity?.findViewById<View>(com.veganbeauty.app.R.id.skin_ai_floating_chat_head)?.visibility = View.GONE

        setupRecyclerView()
        setupListeners()

        val savedHistory = ChatHistoryHelper.loadChatHistory(requireContext())
        if (savedHistory.isEmpty()) {
            sendInitialGreeting()
        } else {
            chatAdapter.submitList(savedHistory)
            binding.rvChatList.scrollToPosition(chatAdapter.itemCount - 1)
        }
    }

    private fun setupRecyclerView() {
        chatAdapter = RootieChatAdapter(requireContext(), viewLifecycleOwner.lifecycleScope)
        binding.rvChatList.layoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true
        }
        binding.rvChatList.adapter = chatAdapter
        
        binding.rvChatList.addOnLayoutChangeListener { _, _, _, _, bottom, _, _, _, oldBottom ->
            if (bottom < oldBottom) {
                binding.rvChatList.postDelayed({
                    val count = chatAdapter.itemCount
                    if (count > 0) {
                        binding.rvChatList.smoothScrollToPosition(count - 1)
                    }
                }, 100)
            }
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.btnSend.setOnClickListener {
            sendMessage()
        }

        binding.etMessageInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage()
                true
            } else {
                false
            }
        }

        binding.chipWeatherAdvice.setOnClickListener {
            sendQuickPrompt("⛅ Thời tiết & Da hôm nay")
        }
        binding.chipWeatherRoutine.setOnClickListener {
            sendQuickPrompt("🧴 Routine theo thời tiết")
        }
        binding.chipMatchProducts.setOnClickListener {
            sendQuickPrompt("🎯 Sản phẩm Rootie của tôi")
        }
        binding.chipSkinDiagnosis.setOnClickListener {
            sendQuickPrompt("📋 Phác đồ chẩn đoán da")
        }

        binding.btnPlus.setOnClickListener {
            Toast.makeText(requireContext(), "Tính năng đính kèm tệp sẽ sớm ra mắt!", Toast.LENGTH_SHORT).show()
        }

        binding.btnMore.setOnClickListener {
            val popup = androidx.appcompat.widget.PopupMenu(requireContext(), it)
            popup.menu.add("Xóa lịch sử trò chuyện")
            popup.setOnMenuItemClickListener { menuItem ->
                if (menuItem.title == "Xóa lịch sử trò chuyện") {
                    ChatHistoryHelper.clearChatHistory(requireContext())
                    chatAdapter.submitList(emptyList())
                    sendInitialGreeting()
                    Toast.makeText(requireContext(), "Đã xóa lịch sử trò chuyện.", Toast.LENGTH_SHORT).show()
                    true
                } else {
                    false
                }
            }
            popup.show()
        }
    }

    private fun getCurrentTime(): String {
        return SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
    }

    private fun sendInitialGreeting() {
        val prefs = requireContext().getSharedPreferences("RootieQuizPrefs", Context.MODE_PRIVATE)
        val skinType = prefs.getString("SAVED_USER_SKIN_TYPE", "Da hỗn hợp thiên dầu") ?: "Da hỗn hợp thiên dầu"
        val fullName = ProfileSession.getFullName(requireContext()) ?: "bạn"

        // 1. Text greeting
        val greetingText = "Chào $fullName! Mình là Rootie AI. Dựa trên kết quả quiz da mới nhất của bạn ($skinType), mình đã thực hiện chẩn đoán chuyên sâu và thiết kế lộ trình chăm sóc thuần chay riêng cho bạn dưới đây:"
        val greetingItem = RootieChatItem(
            sender = RootieChatItem.Sender.AI,
            messageText = greetingText,
            timeStr = getCurrentTime()
        )
        chatAdapter.addMessage(greetingItem)

        // 2. Initial Diagnostic Card
        val diagnosticData = generateRuleBasedDiagnostic(skinType)
        val diagnosticItem = RootieChatItem(
            sender = RootieChatItem.Sender.AI,
            timeStr = getCurrentTime(),
            type = RootieChatItem.ItemType.DIAGNOSTIC,
            diagnosticData = diagnosticData
        )
        chatAdapter.addMessage(diagnosticItem)
        binding.rvChatList.scrollToPosition(chatAdapter.itemCount - 1)

        ChatHistoryHelper.saveChatHistory(requireContext(), chatAdapter.getItems())
    }

    private fun sendMessage() {
        val text = binding.etMessageInput.text.toString().trim()
        if (text.isEmpty()) return

        // 1. Add User Message
        val userMsg = RootieChatItem(
            sender = RootieChatItem.Sender.USER,
            messageText = text,
            timeStr = getCurrentTime()
        )
        chatAdapter.addMessage(userMsg)
        binding.etMessageInput.setText("")
        binding.rvChatList.scrollToPosition(chatAdapter.itemCount - 1)
        ChatHistoryHelper.saveChatHistory(requireContext(), chatAdapter.getItems())

        // 2. Add Thinking Message
        val thinkingMsg = RootieChatItem(
            sender = RootieChatItem.Sender.AI,
            messageText = "Rootie AI đang suy nghĩ...",
            timeStr = getCurrentTime()
        )
        chatAdapter.addMessage(thinkingMsg)
        val thinkingPos = chatAdapter.itemCount - 1
        binding.rvChatList.scrollToPosition(thinkingPos)

        // 3. Query AI (Gemini) or Fallback
        val prefs = requireContext().getSharedPreferences("RootieQuizPrefs", Context.MODE_PRIVATE)
        val skinType = prefs.getString("SAVED_USER_SKIN_TYPE", "Da hỗn hợp thiên dầu") ?: "Da hỗn hợp thiên dầu"
        val fullName = ProfileSession.getFullName(requireContext()) ?: "bạn"

        if (GEMINI_API_KEY.isBlank() || GEMINI_API_KEY == "YOUR_GEMINI_API_KEY_HERE") {
            // Fallback to Rule-based system reply
            viewLifecycleOwner.lifecycleScope.launch {
                kotlinx.coroutines.delay(1200)
                removeChatItemAt(thinkingPos)
                handleFallbackReply(text, skinType)
            }
        } else {
            // Call Gemini 1.5 Flash
            callGeminiApi(text, skinType, fullName, thinkingPos)
        }
    }

    private fun sendQuickPrompt(promptText: String) {
        // 1. Add User Message
        val userMsg = RootieChatItem(
            sender = RootieChatItem.Sender.USER,
            messageText = promptText,
            timeStr = getCurrentTime()
        )
        chatAdapter.addMessage(userMsg)
        binding.rvChatList.scrollToPosition(chatAdapter.itemCount - 1)
        ChatHistoryHelper.saveChatHistory(requireContext(), chatAdapter.getItems())

        // 2. Add Thinking Message
        val thinkingMsg = RootieChatItem(
            sender = RootieChatItem.Sender.AI,
            messageText = "Rootie AI đang suy nghĩ...",
            timeStr = getCurrentTime()
        )
        chatAdapter.addMessage(thinkingMsg)
        val thinkingPos = chatAdapter.itemCount - 1
        binding.rvChatList.scrollToPosition(thinkingPos)

        // 3. Query AI (Gemini) or Fallback
        val prefs = requireContext().getSharedPreferences("RootieQuizPrefs", Context.MODE_PRIVATE)
        val skinType = prefs.getString("SAVED_USER_SKIN_TYPE", "Da hỗn hợp thiên dầu") ?: "Da hỗn hợp thiên dầu"
        val fullName = ProfileSession.getFullName(requireContext()) ?: "bạn"

        if (GEMINI_API_KEY.isBlank() || GEMINI_API_KEY == "YOUR_GEMINI_API_KEY_HERE") {
            viewLifecycleOwner.lifecycleScope.launch {
                kotlinx.coroutines.delay(1200)
                removeChatItemAt(thinkingPos)
                handleFallbackReply(promptText, skinType)
            }
        } else {
            callGeminiApi(promptText, skinType, fullName, thinkingPos)
        }
    }

    private fun removeChatItemAt(position: Int) {
        if (position >= 0 && position < chatAdapter.itemCount) {
            chatAdapter.removeMessageAt(position)
        }
    }

    private fun callGeminiApi(userMessage: String, skinType: String, fullName: String, thinkingPos: Int) {
        val prefs = requireContext().getSharedPreferences("RootieQuizPrefs", Context.MODE_PRIVATE)
        val sensitivity = prefs.getInt("SAVED_SENSITIVITY", 50)
        val hydration = prefs.getInt("SAVED_HYDRATION", 50)
        val elasticity = prefs.getInt("SAVED_ELASTICITY", 75)
        val sebum = prefs.getInt("SAVED_SEBUM", 50)
        val skinAreas = prefs.getString("SAVED_SKIN_AREAS", "Chưa xác định chi tiết vùng da.")
        val flaggedGroups = prefs.getStringSet("SAVED_FLAGGED_GROUPS", emptySet()) ?: emptySet()

        val temp = prefs.getFloat("SAVED_WEATHER_TEMP", 32.0f)
        val humidityVal = prefs.getInt("SAVED_WEATHER_HUMIDITY", 68)
        val uv = prefs.getFloat("SAVED_WEATHER_UV", 9.2f)
        val pm25 = prefs.getInt("SAVED_WEATHER_PM25", 55)
        val city = prefs.getString("SAVED_WEATHER_CITY", "Thành phố Hồ Chí Minh") ?: "Thành phố Hồ Chí Minh"
        val weatherCondition = prefs.getString("SAVED_WEATHER_CONDITION", "NẮNG NHIỀU, OI NHẸ") ?: "NẮNG NHIỀU, OI NHẸ"

        // Construct products list summary for prompt
        val prodSummary = StringBuilder()
        allProducts.forEach { p ->
            prodSummary.append("- ID: ${p.id} | Tên: ${p.name} | Loại: ${p.category} | Thành phần chính: ${p.mainIngredientsSummary} | Giá: ${p.price}đ\n")
        }

        val systemPrompt = """
            Bạn là Rootie AI, chuyên gia tư vấn da liễu hữu cơ và thuần chay (Vegan Skincare) kiêm trợ lý thông minh đa nhiệm của ứng dụng ROOTIE.
            Bạn có khả năng trả lời chính xác, thông minh mọi câu hỏi của người dùng, bao gồm cả các chủ đề ngoài da liễu như lập trình (viết/sửa code), toán học, dịch thuật, tóm tắt văn bản, viết nội dung sáng tạo, và kiến thức tổng hợp, đồng thời luôn giữ thái độ thân thiện, ấm áp và cá nhân hóa phản hồi theo tên người dùng ($fullName).

            Thông tin khách hàng:
            - Tên: $fullName
            - Loại da hiện tại: $skinType
            - Độ ẩm da: $hydration%
            - Độ nhạy cảm: $sensitivity%
            - Độ đàn hồi: $elasticity%
            - Lượng bã nhờn: $sebum%
            - Tình trạng phân bố vùng da: $skinAreas
            - Các hoạt chất/thành phần dị ứng cần tránh hoàn toàn: ${if (flaggedGroups.isNotEmpty()) flaggedGroups.joinToString(", ") else "Không có"}

            Thông tin thời tiết hiện tại ngày/tuần tại địa điểm của khách hàng:
            - Địa điểm: $city
            - Thời tiết: $temp°C ($weatherCondition)
            - Độ ẩm không khí: $humidityVal%
            - Chỉ số UV: ${String.format("%.1f", uv)}
            - Chỉ số bụi mịn PM2.5: $pm25 μg/m³

            Các chức năng chính của ứng dụng Rootie mà bạn có thể tư vấn/hỗ trợ người dùng sử dụng:
            1. Quiz Da / Chẩn đoán da: Đánh giá 4 chỉ số chính của da để gợi ý routine phù hợp.
            2. Lộ trình Routine (Sáng/Tối): Thiết lập, tùy chỉnh các bước chăm sóc da và theo dõi việc thực hiện hàng ngày.
            3. Thống kê Streak: Theo dõi số ngày liên tục hoàn thành routine để tích điểm đổi quà.
            4. Cửa hàng mỹ phẩm: Cung cấp sản phẩm thuần chay 100% Việt Nam được chứng nhận của Cruelty-Free & Vegan (Gel rửa mặt Bí đao, Tinh chất Bí đao N15, Thạch hoa hồng hữu cơ Cao Bằng, Tinh chất nghệ Hưng Yên C10...).
            5. Đặt lịch Spa: Đặt hẹn các buổi spa chăm sóc chuyên sâu trực tiếp tại các cửa hàng của Rootie.
            6. Dự báo thời tiết & Da: Xem thời tiết, chỉ số UV, bụi mịn PM2.5 thời gian thực của khu vực để điều chỉnh cách chăm sóc da phù hợp.

            Danh sách sản phẩm chính thức của Rootie (Chỉ giới thiệu sản phẩm trong danh sách này để người dùng mua được trên app):
            $prodSummary

            Nhiệm vụ và hướng dẫn trả lời:
            1. Trả lời bằng tiếng Việt, giọng điệu ấm áp, chuyên nghiệp, thông minh.
            2. Nếu người dùng hỏi các câu hỏi chung ngoài lề (lập trình, dịch thuật, toán học, tóm tắt...), hãy thực hiện nhiệm vụ một cách xuất sắc, chính xác và đầy đủ nhất, sau đó có thể đính kèm một câu chào thân thiện liên quan tới Rootie ở cuối (ví dụ: "Nếu bạn cần tư vấn thêm về da hay các sản phẩm chăm sóc thuần chay, đừng ngần ngại hỏi Rootie AI nhé!").
            3. Nếu người dùng yêu cầu lộ trình routine, chẩn đoán da hoặc gợi ý sản phẩm phù hợp với da và thời tiết hôm nay, hãy trả về khối JSON hợp lệ nằm giữa dấu ```json và ``` để hiển thị Thẻ Chẩn đoán. JSON phải có cấu trúc chính xác:
            {
              "is_diagnostic": true,
              "assessment": "Nhận định ngắn gọn về màng Lipid/tình trạng da của họ dựa trên thời tiết hôm nay.",
              "detailExplanation": "Giải thích chi tiết nguyên nhân khoa học liên quan đến các chỉ số ẩm $hydration%, nhạy cảm $sensitivity%, đàn hồi $elasticity%, bã nhờn $sebum% kết hợp với thời tiết nóng ẩm $temp°C, chỉ số UV $uv hoặc bụi mịn $pm25.",
              "moistureVal": "$hydration%",
              "sensitivityVal": "${if (sensitivity >= 70) "Rất Cao" else if (sensitivity >= 40) "Trung Bình" else "Thấp"}",
              "barrierVal": "${if (elasticity >= 75) "Khỏe" else if (elasticity >= 50) "Ổn định" else "Yếu"}",
              "whyExplanation": "Tại sao lộ trình chăm sóc này hiệu quả đối với làn da của $fullName dưới thời tiết hiện tại.",
              "recommendedProductIds": ["id_sản_phẩm_1", "id_sản_phẩm_2"],
              "productPhases": ["GIAI ĐOẠN 1", "GIAI ĐOẠN 2"],
              "productSubcategories": ["LÀM SẠCH DỊU NHẸ", "PHỤC HỒI ĐA TẦNG"],
              "productExpertReasons": ["Lý do chuyên gia khuyên dùng sản phẩm 1 dưới thời tiết này...", "Lý do chuyên gia khuyên dùng sản phẩm 2 dưới thời tiết này..."]
            }
            4. Đối với các câu hỏi tư vấn thông thường, giải đáp thành phần hoặc trò chuyện tự do, hãy trả lời bằng văn bản thuần túy, KHÔNG đính kèm JSON.
        """.trimIndent()

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val urlString = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$GEMINI_API_KEY"
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                connection.doOutput = true

                val requestJson = JSONObject()
                
                // Build content array containing chat history for multi-turn conversational memory
                val contentsArray = org.json.JSONArray()
                val historyItems = withContext(Dispatchers.Main) { chatAdapter.getItems().toList() }
                val activeHistory = historyItems.filter { it.messageText != "Rootie AI đang suy nghĩ..." }
                val lastMessages = if (activeHistory.size > 10) {
                    activeHistory.takeLast(10)
                } else {
                    activeHistory
                }

                for (chatItem in lastMessages) {
                    val role = if (chatItem.sender == RootieChatItem.Sender.USER) "user" else "model"
                    val text = if (chatItem.type == RootieChatItem.ItemType.DIAGNOSTIC) {
                        val diag = chatItem.diagnosticData
                        if (diag != null) {
                            "Đã chẩn đoán da: ${diag.assessment}. Chi tiết: ${diag.detailExplanation}"
                        } else {
                            chatItem.messageText
                        }
                    } else {
                        chatItem.messageText
                    }

                    if (text.isNotBlank()) {
                        val partsArr = org.json.JSONArray().apply {
                            put(JSONObject().put("text", text))
                        }
                        val contentObj = JSONObject().apply {
                            put("role", role)
                            put("parts", partsArr)
                        }
                        contentsArray.put(contentObj)
                    }
                }
                requestJson.put("contents", contentsArray)

                val systemInstruction = JSONObject().apply {
                    val systemParts = org.json.JSONArray().apply {
                        put(JSONObject().put("text", systemPrompt))
                    }
                    put("parts", systemParts)
                }
                requestJson.put("systemInstruction", systemInstruction)

                val generationConfig = JSONObject().apply {
                    put("temperature", 0.7)
                    put("maxOutputTokens", 1500)
                }
                requestJson.put("generationConfig", generationConfig)

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
                            
                            withContext(Dispatchers.Main) {
                                // Remove thinking msg
                                removeThinkingAndShowResponse(thinkingPos, textResult, skinType)
                            }
                            return@launch
                        }
                    }
                }

                // Error response
                withContext(Dispatchers.Main) {
                    removeThinkingAndShowFallback(thinkingPos, userMessage, skinType)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    removeThinkingAndShowFallback(thinkingPos, userMessage, skinType)
                }
            }
        }
    }



    private fun removeThinkingAndShowResponse(thinkingPos: Int, responseText: String, skinType: String) {
        // Remove the thinking item
        if (thinkingPos < chatAdapter.itemCount) {
            chatAdapter.removeMessageAt(thinkingPos)
        }

        // Check if response contains JSON block
        var jsonStr: String? = null
        var matchedText: String? = null

        val cleanResponse = responseText.trim()
        if (cleanResponse.startsWith("{") && cleanResponse.endsWith("}")) {
            jsonStr = cleanResponse
            matchedText = responseText
        } else {
            val jsonPattern = "```(?:json)?([\\s\\S]*?)```".toRegex()
            val matchResult = jsonPattern.find(responseText)
            if (matchResult != null) {
                jsonStr = matchResult.groupValues[1].trim()
                matchedText = matchResult.value
            }
        }

        if (jsonStr != null && matchedText != null) {
            try {
                val jsonObject = JSONObject(jsonStr)
                val isDiagnostic = jsonObject.optBoolean("is_diagnostic", false)
                if (isDiagnostic) {
                    val assessment = jsonObject.getString("assessment")
                    val detail = jsonObject.getString("detailExplanation")
                    val moisture = jsonObject.getString("moistureVal")
                    val sensitivity = jsonObject.getString("sensitivityVal")
                    val barrier = jsonObject.getString("barrierVal")
                    val why = jsonObject.getString("whyExplanation")

                    val prodIdsArray = jsonObject.getJSONArray("recommendedProductIds")
                    val prodIds = mutableListOf<String>()
                    for (i in 0 until prodIdsArray.length()) {
                        prodIds.add(prodIdsArray.getString(i))
                    }

                    val phasesArray = jsonObject.getJSONArray("productPhases")
                    val phases = mutableListOf<String>()
                    for (i in 0 until phasesArray.length()) {
                        phases.add(phasesArray.getString(i))
                    }

                    val subcatsArray = jsonObject.getJSONArray("productSubcategories")
                    val subcats = mutableListOf<String>()
                    for (i in 0 until subcatsArray.length()) {
                        subcats.add(subcatsArray.getString(i))
                    }

                    val reasonsArray = jsonObject.getJSONArray("productExpertReasons")
                    val reasons = mutableListOf<String>()
                    for (i in 0 until reasonsArray.length()) {
                        reasons.add(reasonsArray.getString(i))
                    }

                    val diagnosticData = RootieChatItem.DiagnosticData(
                        assessment = assessment,
                        detailExplanation = detail,
                        moistureVal = moisture,
                        sensitivityVal = sensitivity,
                        barrierVal = barrier,
                        whyExplanation = why,
                        recommendedProductIds = prodIds,
                        productPhases = phases,
                        productSubcategories = subcats,
                        productExpertReasons = reasons
                    )

                    // Also print standard chat reply text if there was any text before/after the code block
                    val prefixText = responseText.replace(matchedText, "").trim()
                    if (prefixText.isNotEmpty()) {
                        chatAdapter.addMessage(RootieChatItem(
                            sender = RootieChatItem.Sender.AI,
                            messageText = prefixText,
                            timeStr = getCurrentTime()
                        ))
                    }

                    val diagnosticItem = RootieChatItem(
                        sender = RootieChatItem.Sender.AI,
                        timeStr = getCurrentTime(),
                        type = RootieChatItem.ItemType.DIAGNOSTIC,
                        diagnosticData = diagnosticData
                    )
                    chatAdapter.addMessage(diagnosticItem)
                } else {
                    // JSON but not diagnostic, treat as standard text
                    chatAdapter.addMessage(RootieChatItem(
                        sender = RootieChatItem.Sender.AI,
                        messageText = responseText,
                        timeStr = getCurrentTime()
                    ))
                }
            } catch (e: Exception) {
                // Fallback to text message if JSON parsing fails
                chatAdapter.addMessage(RootieChatItem(
                    sender = RootieChatItem.Sender.AI,
                    messageText = responseText,
                    timeStr = getCurrentTime()
                ))
            }
        } else {
            // Regular text response
            chatAdapter.addMessage(RootieChatItem(
                sender = RootieChatItem.Sender.AI,
                messageText = responseText,
                timeStr = getCurrentTime()
            ))
        }
        binding.rvChatList.scrollToPosition(chatAdapter.itemCount - 1)
        ChatHistoryHelper.saveChatHistory(requireContext(), chatAdapter.getItems())
    }

    private fun removeThinkingAndShowFallback(thinkingPos: Int, userMessage: String, skinType: String) {
        if (thinkingPos < chatAdapter.itemCount) {
            chatAdapter.removeMessageAt(thinkingPos)
        }
        handleFallbackReply(userMessage, skinType)
    }

    private fun handleFallbackReply(userMessage: String, skinType: String) {
        val context = requireContext()
        val prefs = context.getSharedPreferences("RootieQuizPrefs", Context.MODE_PRIVATE)
        val fullName = ProfileSession.getFullName(context) ?: "bạn"
        val flaggedGroups = prefs.getStringSet("SAVED_FLAGGED_GROUPS", emptySet()) ?: emptySet()
        val sensitivity = prefs.getInt("SAVED_SENSITIVITY", 50)
        val hydration = prefs.getInt("SAVED_HYDRATION", 50)
        val elasticity = prefs.getInt("SAVED_ELASTICITY", 75)
        val sebum = prefs.getInt("SAVED_SEBUM", 50)
        val query = userMessage.lowercase().trim()

        val temp = prefs.getFloat("SAVED_WEATHER_TEMP", 32.0f)
        val humidityVal = prefs.getInt("SAVED_WEATHER_HUMIDITY", 68)
        val uv = prefs.getFloat("SAVED_WEATHER_UV", 9.2f)
        val pm25 = prefs.getInt("SAVED_WEATHER_PM25", 55)
        val city = prefs.getString("SAVED_WEATHER_CITY", "Thành phố Hồ Chí Minh") ?: "Thành phố Hồ Chí Minh"
        val weatherCondition = prefs.getString("SAVED_WEATHER_CONDITION", "NẮNG NHIỀU, OI NHẸ") ?: "NẮNG NHIỀU, OI NHẸ"

        val replyText: String
        var isDiagnostic = false

        // Load all data from local databases
        val jsonReader = LocalJsonReader(context)
        val localProducts = jsonReader.getAllProducts()
        val allIngredients = jsonReader.getIngredients()
        val allStores = jsonReader.getStores()

        // 1. Check if it's a routine/diagnostic request
        if (query.contains("phác đồ") || query.contains("chẩn đoán") || query.contains("routine") || query.contains("lộ trình")) {
            isDiagnostic = true
            replyText = ""
        }
        // 2. Check for Store / Spa queries
        else if (query.contains("spa") || query.contains("đặt lịch") || query.contains("địa chỉ") || query.contains("cửa hàng") || query.contains("chi nhánh") || query.contains("vị trí")) {
            val sb = StringBuilder("📍 **Hệ thống Cửa hàng & Spa chăm sóc da thuần chay của Rootie:**\n\n")
            val storesToShow = allStores.take(3)
            if (storesToShow.isNotEmpty()) {
                storesToShow.forEachIndexed { index, s ->
                    sb.append("${index + 1}. **${s.tenCuaHang}** (${s.loaiHinh})\n")
                    sb.append("   • Địa chỉ: ${s.diaChiDayDu}\n")
                    sb.append("   • Số điện thoại: ${s.soDienThoai}\n")
                    sb.append("   • Giờ mở cửa: ${s.moCua} - ${s.dongCua}\n")
                    sb.append("   • Tiện nghi: ${s.tienNghi.replace(",", ", ")}\n")
                    sb.append("   • Trạng thái: ${s.trangThai}\n\n")
                }
                sb.append("💡 Bạn có thể bấm vào chức năng 'Đặt lịch Spa' ngoài màn hình để tiến hành đặt lịch hẹn nhanh nhất.")
            } else {
                sb.append("Rootie Spa có các chi nhánh phục vụ tại TP. Hồ Chí Minh chuyên sâu về chăm sóc và soi da thuần chay. Hãy mở bản đồ trong ứng dụng để tìm cơ sở gần nhất!")
            }
            replyText = sb.toString()
        }
        // 3. Check for specific ingredients queries (Bí đao, Nghệ, Rau má, Hoa hồng, etc.)
        else if (allIngredients.any { query.contains(it.name.lowercase()) || (it.scientificName.isNotBlank() && query.contains(it.scientificName.lowercase())) }) {
            val matchedIng = allIngredients.first { query.contains(it.name.lowercase()) || (it.scientificName.isNotBlank() && query.contains(it.scientificName.lowercase())) }
            
            // Find products containing this ingredient
            val matchedProds = localProducts.filter { 
                it.name.lowercase().contains(matchedIng.name.lowercase()) || 
                it.mainIngredientsSummary.lowercase().contains(matchedIng.name.lowercase()) ||
                it.description.lowercase().contains(matchedIng.name.lowercase())
            }.take(2)

            val prodRecommendation = if (matchedProds.isNotEmpty()) {
                "\n\n🛍️ **Sản phẩm Rootie chứa thành phần này:**\n" + matchedProds.joinToString("\n") { 
                    "- **${it.name}** (${java.text.NumberFormat.getNumberInstance(java.util.Locale.US).format(it.price)}đ): ${it.description.take(100)}..."
                }
            } else ""

            replyText = "🌿 **Kiến thức nguyên liệu Rootie AI:**\n" +
                    "• **Thành phần**: ${matchedIng.name} (${matchedIng.scientificName})\n" +
                    "• **Công dụng chính**: ${matchedIng.uses}\n" +
                    "• **Chi tiết**: ${matchedIng.description}" + prodRecommendation
        }
        // 4. Check for product-specific search queries (sản phẩm, mỹ phẩm, rửa mặt, kem chống nắng, toner, serum...)
        else if (query.contains("sản phẩm") || query.contains("mỹ phẩm") || query.contains("mã") || query.contains("bán") || query.contains("mua") ||
                 query.contains("rửa mặt") || query.contains("chống nắng") || query.contains("toner") || query.contains("nước hoa hồng") || query.contains("tẩy trang") || query.contains("serum") || query.contains("thạch nghệ")) {
            
            // Determine keyword
            val keyword = when {
                query.contains("bí đao") -> "bí đao"
                query.contains("nghệ") -> "nghệ"
                query.contains("hoa hồng") -> "hoa hồng"
                query.contains("cà phê") -> "cà phê"
                query.contains("rửa mặt") -> "rửa mặt"
                query.contains("chống nắng") -> "chống nắng"
                query.contains("toner") || query.contains("hoa hồng") -> "hoa hồng"
                query.contains("tẩy trang") -> "tẩy trang"
                query.contains("serum") || query.contains("tinh chất") -> "tinh chất"
                query.contains("dưỡng ẩm") || query.contains("thạch") -> "thạch"
                else -> null
            }

            val filteredProds = if (keyword != null) {
                localProducts.filter { it.name.lowercase().contains(keyword) || it.category.lowercase().contains(keyword) }
            } else {
                // Recommend based on skin type
                val typeKey = when {
                    skinType.contains("dầu", ignoreCase = true) || skinType.contains("hỗn hợp", ignoreCase = true) -> "bí đao"
                    skinType.contains("khô", ignoreCase = true) -> "hoa hồng"
                    else -> "nghệ"
                }
                localProducts.filter { it.name.lowercase().contains(typeKey) }
            }

            val sb = StringBuilder("🛍️ **Danh sách sản phẩm Rootie đề xuất cho bạn:**\n\n")
            val prodsToShow = filteredProds.take(3)
            if (prodsToShow.isNotEmpty()) {
                prodsToShow.forEachIndexed { index, p ->
                    val formattedPrice = java.text.NumberFormat.getNumberInstance(java.util.Locale.US).format(p.price)
                    sb.append("${index + 1}. **${p.name}**\n")
                    sb.append("   • Giá: ${formattedPrice}đ | Loại: ${p.category}\n")
                    sb.append("   • Thành phần chính: ${p.mainIngredientsSummary}\n")
                    
                    // Allergy warning check
                    val containsAllergen = flaggedGroups.any { allergen ->
                        p.detailedIngredients.any { it.lowercase().contains(allergen.lowercase()) } ||
                        p.allergyInformation.lowercase().contains(allergen.lowercase()) ||
                        p.mainIngredientsSummary.lowercase().contains(allergen.lowercase())
                    }
                    if (containsAllergen) {
                        sb.append("   • ⚠️ *Cảnh báo*: Sản phẩm chứa thành phần nhạy cảm với da bạn!\n")
                    } else {
                        sb.append("   • ✅ Phù hợp: ${p.suitableFor}\n")
                    }
                    sb.append("\n")
                }
                sb.append("💡 Bạn có thể tìm thấy các sản phẩm trên trực tiếp tại Cửa hàng của Rootie để được tư vấn soi da miễn phí.")
            } else {
                sb.append("Hiện tại Rootie đang cung cấp các dòng sản phẩm thuần chay 100% từ Bí Đao (ngừa mụn), Nghệ Hưng Yên (mờ thâm), và Hoa Hồng Cao Bằng (cấp ẩm). Vui lòng nói rõ hơn dòng sản phẩm bạn cần tìm nhé!")
            }
            replyText = sb.toString()
        }
        // 5. Check for Weather & Skin today
        else if (query.contains("thời tiết") || query.contains("hôm nay") || query.contains("nhiệt độ") || query.contains("bụi") || query.contains("nắng") || query.contains("uv") || query.contains("weather")) {
            // Find weather matching from weathers.json
            var matchedWeatherName = "Nắng ấm dễ chịu"
            var matchedWeatherDesc = "Thời tiết ôn hòa, phù hợp mọi loại da"
            var matchedWeatherIcon = "🌤️"
            try {
                val jsonString = context.assets.open("weathers.json").bufferedReader().use { it.readText() }
                val root = org.json.JSONObject(jsonString)
                val weathersArr = root.getJSONArray("weathers")
                for (i in 0 until weathersArr.length()) {
                    val w = weathersArr.getJSONObject(i)
                    val tempRange = w.getJSONObject("temperature_range")
                    val humRange = w.getJSONObject("humidity_range")
                    val minT = tempRange.getDouble("min")
                    val maxT = tempRange.getDouble("max")
                    val minH = humRange.getDouble("min")
                    val maxH = humRange.getDouble("max")
                    if (temp >= minT && temp <= maxT && humidityVal >= minH && humidityVal <= maxH) {
                        matchedWeatherName = w.getString("name")
                        matchedWeatherDesc = w.getString("description")
                        matchedWeatherIcon = w.getString("icon")
                        break
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }

            val uvLevelStr = when {
                uv < 3 -> "Thấp (An toàn)"
                uv < 6 -> "Trung bình (Cần che chắn)"
                uv < 8 -> "Cao (Nguy cơ gây hại)"
                else -> "Rất cao 🚨 (Nguy hiểm cho da)"
            }
            val dustLevelStr = when {
                pm25 < 25 -> "Tốt (Không khí sạch)"
                pm25 < 50 -> "Trung bình"
                else -> "Kém ⚠️ (Dễ gây bít tắc lỗ chân lông)"
            }

            val weatherImpact = when {
                skinType.contains("dầu", ignoreCase = true) || skinType.contains("hỗn hợp", ignoreCase = true) -> 
                    "Thời tiết '$matchedWeatherName' ($temp°C) làm tăng lượng dầu tiết ra ($sebum%), dễ gây bít tắc ở vùng chữ T. Rootie AI khuyên bạn dùng Gel rửa mặt Bí Đao và thoa serum Niacinamide mỏng nhẹ."
                skinType.contains("khô", ignoreCase = true) -> 
                    "Không khí có độ ẩm $humidityVal%. Làn da khô của bạn đang có độ ẩm khá thấp ($hydration%). Hãy cấp ẩm tầng sâu bằng Thạch hoa hồng hữu cơ Cao Bằng để tránh bong tróc và thô ráp."
                else -> 
                    "Nền da nhạy cảm ($sensitivity%) của bạn rất dễ bị tổn thương bởi tia UV hôm nay đang ở mức $uv ($uvLevelStr). Bụi mịn PM2.5 là $pm25 μg/m³ ($dustLevelStr). Hãy thoa Sữa chống nắng Bí Đao và che chắn thật kỹ."
            }

            replyText = "⛅ **Dự báo thời tiết & Phân tích da cùng Rootie:**\n" +
                    "• **Vị trí**: $city | **Thời tiết**: $temp°C ($weatherCondition) $matchedWeatherIcon\n" +
                    "• **Trạng thái**: $matchedWeatherName - $matchedWeatherDesc\n" +
                    "• **Chỉ số UV**: ${String.format("%.1f", uv)} ($uvLevelStr)\n" +
                    "• **Bụi mịn PM2.5**: $pm25 μg/m³ ($dustLevelStr)\n\n" +
                    "🎯 **Lời khuyên cá nhân hóa cho da $skinType của $fullName:**\n" +
                    "$weatherImpact\n\n" +
                    "💡 *Mẹo:* Gửi tin nhắn 'routine' để nhận ngay phác đồ các sản phẩm thuần chay tối ưu nhất cho ngày hôm nay!"
        }
        // 6. Specific concerns (mụn, dầu, khô, nhạy cảm)
        else if (query.contains("mụn") || query.contains("thâm") || query.contains("viêm")) {
            replyText = "Chào $fullName, đối với da bị mụn và thâm, Rootie khuyên bạn nên:\n" +
                    "1. Làm sạch sâu nhẹ nhàng với **Gel rửa mặt Bí đao** giúp ngừa khuẩn mụn sưng đỏ.\n" +
                    "2. Thoa **Tinh chất Bí đao N15** chứa Niacinamide và Tràm trà giúp gom cồi mụn ẩn nhanh chóng.\n" +
                    "3. Dùng **Tinh chất Nghệ Hưng Yên C10** làm mờ vết thâm sau khi mụn đã gom khô cồi.\n\n" +
                    "Lưu ý: Không tự ý nặn các nốt mụn viêm đang sưng to để tránh để lại sẹo lõm bạn nhé!"
        }
        else if (query.contains("dầu") || query.contains("nhờn") || query.contains("lỗ chân lông") || query.contains("bít tắc")) {
            replyText = "Lượng bã nhờn hiện tại của bạn là $sebum% (khá cao). Để điều tiết lượng dầu thừa:\n" +
                    "1. Cấp đủ nước: Thiếu nước sẽ kích thích da đổ dầu bù đắp. Hãy thoa **Toner Bí Đao** cấp nước mát da.\n" +
                    "2. Rửa mặt đúng cách: Không dùng sữa rửa mặt chứa chất tẩy tạo bọt mạnh quá 2 lần/ngày.\n" +
                    "3. Sử dụng tinh chất chứa chiết xuất Bí Đao giúp kháng viêm và se khít lỗ chân lông hiệu quả."
        }
        else if (query.contains("khô") || query.contains("bong tróc") || query.contains("căng")) {
            replyText = "Độ ẩm da hiện tại của bạn là $hydration%. Với mức ẩm thấp này, da rất dễ xuất hiện nếp nhăn và khô sạm. Hãy:\n" +
                    "1. Bổ sung ngay nước hoa hồng cấp ẩm dạng xịt/vỗ sau khi rửa mặt.\n" +
                    "2. Dưỡng khóa ẩm sâu bằng **Thạch hoa hồng hữu cơ Cao Bằng** giúp duy trì độ ẩm kéo dài.\n" +
                    "3. Tránh rửa mặt bằng nước quá nóng vì sẽ làm mất đi lớp dầu tự nhiên bảo vệ da."
        }
        else if (query.contains("nhạy cảm") || query.contains("đỏ") || query.contains("kích ứng") || query.contains("rộp")) {
            val allergyMsg = if (flaggedGroups.isNotEmpty()) "⚠️ Hãy tránh tuyệt đối các thành phần: ${flaggedGroups.joinToString(", ")}." else "Nên tránh cồn khô và hương liệu nhân tạo."
            replyText = "Da bạn có độ nhạy cảm cao ($sensitivity%). $allergyMsg\n" +
                    "Rootie khuyên bạn nên sử dụng **Gel rửa mặt Hoa Hồng** làm sạch dịu nhẹ kết hợp với **Thạch Hoa Hồng hữu cơ** để củng cố hàng rào bảo vệ da ($elasticity% đàn hồi) đang mỏng yếu."
        }
        // 7. Coding/Programming helpers
        else if (query.contains("code") || query.contains("lập trình") || query.contains("python") || query.contains("java") || query.contains("kotlin") || query.contains("html") || query.contains("javascript")) {
            replyText = "💻 **Trợ lý lập trình Rootie (Offline):**\n\n" +
                    "Hệ thống AI hiện đang chạy ở chế độ offline (Chưa cấu hình API Key). Dưới đây là mẫu hàm đệ quy Fibonacci trong Python:\n" +
                    "```python\n" +
                    "def fibonacci(n):\n" +
                    "    if n <= 1:\n" +
                    "        return n\n" +
                    "    return fibonacci(n-1) + fibonacci(n-2)\n" +
                    "```\n\n" +
                    "💡 *Mẹo:* Bạn chỉ cần thêm API Key thực tế từ Google AI Studio vào `local.properties` (dòng `gemini.api.key=...`) để kích hoạt Gemini 1.5 cực kỳ thông minh hỗ trợ code nâng cao!"
        }
        // 8. Translation helper
        else if (query.contains("dịch") || query.contains("translate") || query.contains("tiếng anh") || query.contains("tiếng việt")) {
            replyText = "🌐 **Dịch thuật Rootie (Offline):**\n\n" +
                    "Tôi dịch nhanh một số thuật ngữ da liễu cho bạn:\n" +
                    "- *Acne-prone skin* ➔ Da dễ nổi mụn\n" +
                    "- *Dehydrated skin* ➔ Da thiếu nước\n" +
                    "- *Moisturizer* ➔ Kem dưỡng ẩm\n" +
                    "- *Double cleansing* ➔ Làm sạch kép (Tẩy trang + Rửa mặt)\n\n" +
                    "💡 Hãy thêm API Key để dịch tự do các đoạn văn bản dài và đa ngôn ngữ!"
        }
        // 9. Greetings & Default identity
        else if (query.contains("chào") || query.contains("hi") || query.contains("hello") || query.contains("xin chào")) {
            replyText = "Chào $fullName! Mình là Rootie AI, chuyên gia tư vấn da liễu thuần chay của bạn. Hôm nay, làn da của bạn đang có độ ẩm $hydration%, bã nhờn $sebum% và độ nhạy cảm $sensitivity%.\n\nMình có thể giúp bạn kiểm tra sản phẩm, thiết kế lộ trình (hãy nhắn 'routine'), tư vấn về các nguyên liệu thiên nhiên hoặc các spa của Rootie. Bạn hãy hỏi nhé!"
        }
        else if (query.contains("bạn là ai") || query.contains("tên gì") || query.contains("rootie")) {
            replyText = "Mình là Rootie AI - trợ lý ảo tư vấn chăm sóc da thuần chay. Nhiệm vụ của mình là hỗ trợ bạn phân tích các chỉ số da, đề xuất lộ trình chăm sóc phù hợp với thời tiết ngày hôm nay, và hướng dẫn bạn chọn sản phẩm thuần chay tối ưu nhất."
        }
        // 10. Default fallback
        else {
            replyText = "Cảm ơn câu hỏi của $fullName. Mình ghi nhận làn da $skinType của bạn đang có Độ ẩm $hydration%, Nhạy cảm $sensitivity% và Bã nhờn $sebum%. Để trả lời chi tiết và đa nhiệm nhất về câu hỏi này, bạn hãy cấu hình API Key Gemini của Google nhé.\n\n" +
                    "💡 *Mẹo:* Nhập API Key của bạn vào dòng `gemini.api.key=...` trong tệp `local.properties` để kích hoạt bộ não thông minh vượt trội của Rootie AI!"
        }

        // Add to Chat list
        if (isDiagnostic) {
            val diagData = generateRuleBasedDiagnostic(skinType)
            val diagnosticItem = RootieChatItem(
                sender = RootieChatItem.Sender.AI,
                timeStr = getCurrentTime(),
                type = RootieChatItem.ItemType.DIAGNOSTIC,
                diagnosticData = diagData
            )
            chatAdapter.addMessage(diagnosticItem)
        } else {
            val textItem = RootieChatItem(
                sender = RootieChatItem.Sender.AI,
                messageText = replyText,
                timeStr = getCurrentTime()
            )
            chatAdapter.addMessage(textItem)
        }

        binding.rvChatList.scrollToPosition(chatAdapter.itemCount - 1)
        ChatHistoryHelper.saveChatHistory(context, chatAdapter.getItems())
    }

    private fun generateRuleBasedDiagnostic(skinType: String): RootieChatItem.DiagnosticData {
        val context = requireContext()
        val prefs = context.getSharedPreferences("RootieQuizPrefs", Context.MODE_PRIVATE)
        val sensitivity = prefs.getInt("SAVED_SENSITIVITY", 50)
        val hydration = prefs.getInt("SAVED_HYDRATION", 50)
        val elasticity = prefs.getInt("SAVED_ELASTICITY", 75)
        val sebum = prefs.getInt("SAVED_SEBUM", 50)
        val skinAreas = prefs.getString("SAVED_SKIN_AREAS", "Độ ẩm và dầu phân bố không đều.")
        val flaggedGroups = prefs.getStringSet("SAVED_FLAGGED_GROUPS", emptySet()) ?: emptySet()
        val fullName = ProfileSession.getFullName(context) ?: "bạn"

        val temp = prefs.getFloat("SAVED_WEATHER_TEMP", 32.0f)
        val humidityVal = prefs.getInt("SAVED_WEATHER_HUMIDITY", 68)
        val uv = prefs.getFloat("SAVED_WEATHER_UV", 9.2f)
        val pm25 = prefs.getInt("SAVED_WEATHER_PM25", 55)
        val city = prefs.getString("SAVED_WEATHER_CITY", "Thành phố Hồ Chí Minh") ?: "Thành phố Hồ Chí Minh"
        val weatherCondition = prefs.getString("SAVED_WEATHER_CONDITION", "NẮNG NHIỀU, OI NHẸ") ?: "NẮNG NHIỀU, OI NHẸ"

        val moistureStr = "$hydration%"
        val sensitivityStr = if (sensitivity >= 70) "Rất Cao" else if (sensitivity >= 40) "Trung Bình" else "Thấp"
        val barrierStr = if (elasticity >= 75) "Khỏe" else if (elasticity >= 50) "Ổn định" else "Yếu"

        // Dynamic weather match
        var matchedWeatherName = "Nắng ấm dễ chịu"
        var matchedWeatherDesc = "Thời tiết ôn hòa, phù hợp mọi loại da"
        var matchedWeatherIcon = "🌤️"
        try {
            val jsonString = context.assets.open("weathers.json").bufferedReader().use { it.readText() }
            val root = org.json.JSONObject(jsonString)
            val weathersArr = root.getJSONArray("weathers")
            for (i in 0 until weathersArr.length()) {
                val w = weathersArr.getJSONObject(i)
                val tempRange = w.getJSONObject("temperature_range")
                val humRange = w.getJSONObject("humidity_range")
                val minT = tempRange.getDouble("min")
                val maxT = tempRange.getDouble("max")
                val minH = humRange.getDouble("min")
                val maxH = humRange.getDouble("max")
                if (temp >= minT && temp <= maxT && humidityVal >= minH && humidityVal <= maxH) {
                    matchedWeatherName = w.getString("name")
                    matchedWeatherDesc = w.getString("description")
                    matchedWeatherIcon = w.getString("icon")
                    break
                }
            }
        } catch (e: Exception) { e.printStackTrace() }

        val weatherInfo = "Thời tiết hôm nay: $matchedWeatherName $matchedWeatherIcon ($temp°C, độ ẩm $humidityVal%, UV ${String.format("%.1f", uv)}, bụi mịn PM2.5 là $pm25)."

        return when {
            skinType.contains("dầu", ignoreCase = true) || skinType.contains("hỗn hợp", ignoreCase = true) -> {
                val recommendedIds = if (uv >= 6.0) {
                    listOf("5f29c9fa19873eb44aedced4", "641cdf538114d9cb102d9ab2", "1e499ed75a31e4a02af2d962")
                } else {
                    listOf("5f29c9fa19873eb44aedced4", "641cdf538114d9cb102d9ab2", "113023f9cf480dbe4182e96c")
                }
                
                val phases = if (uv >= 6.0) {
                    listOf("GIAI ĐOẠN 1: LÀM SẠCH", "GIAI ĐOẠN 2: KIỀM DẦU", "GIAI ĐOẠN 3: BẢO VỆ NẮNG")
                } else {
                    listOf("GIAI ĐOẠN 1: LÀM SẠCH", "GIAI ĐOẠN 2: KIỀM DẦU", "GIAI ĐOẠN 3: KHÓA ẨM DỊU NHẸ")
                }

                val subcats = if (uv >= 6.0) {
                    listOf("SẠCH SÂU BÃ NHỜN", "ĐIỀU TIẾT DẦU MỤN", "CHỐNG UV GAY GẮT")
                } else {
                    listOf("SẠCH SÂU BÃ NHỜN", "ĐIỀU TIẾT DẦU MỤN", "KHÓA NƯỚC DỊU MÁT")
                }

                val reasons = if (uv >= 6.0) {
                    listOf(
                        "Rửa mặt bằng Gel Bí Đao làm sạch dầu nhờn ở mức $sebum% mà không làm khô căng da.",
                        "Tinh chất Bí Đao N15 với Niacinamide điều tiết bã nhờn trong thời tiết nóng ẩm $temp°C.",
                        "Thời tiết UV đạt ${String.format("%.1f", uv)} (mức nguy cơ), bắt buộc dùng Sữa chống nắng Bí Đao để bảo vệ màng da."
                    )
                } else {
                    listOf(
                        "Gel rửa mặt Bí Đao giữ ẩm ở mức $moistureStr và loại bỏ bã nhờn vùng chữ T.",
                        "Tinh chất Bí Đao N15 giúp giảm mụn sưng viêm cực tốt dưới thời tiết $weatherCondition.",
                        "Thạch bí đao mỏng nhẹ cấp ẩm cho da khô mất nước do ngồi máy lạnh dưới trời oi nóng $temp°C."
                    )
                }

                RootieChatItem.DiagnosticData(
                    assessment = "Rootie nhận định: Da $skinType của bạn đang chịu áp lực lớn dưới thời tiết $temp°C của $city.",
                    detailExplanation = "$weatherInfo Với tình trạng da của $fullName (độ ẩm $moistureStr, độ dầu $sebum%), thời tiết này làm tăng tốc độ bài tiết bã nhờn, dễ gây bít tắc lỗ chân lông gây mụn.",
                    moistureVal = moistureStr,
                    sensitivityVal = sensitivityStr,
                    barrierVal = barrierStr,
                    whyExplanation = "Routine này tối ưu hóa khả năng kiểm soát bã nhờn ($sebum%) bằng Bí đao tự nhiên, đồng thời bảo vệ da khỏi tia UV ${String.format("%.1f", uv)} nguy hại hôm nay.",
                    recommendedProductIds = recommendedIds,
                    productPhases = phases,
                    productSubcategories = subcats,
                    productExpertReasons = reasons
                )
            }
            skinType.contains("khô", ignoreCase = true) -> {
                val recommendedIds = if (uv >= 6.0) {
                    listOf("7df6fb8720d3cc5566f0c4ca", "fdc1ca708d8cf2225c9d9697", "1e499ed75a31e4a02af2d962")
                } else {
                    listOf("7df6fb8720d3cc5566f0c4ca", "fdc1ca708d8cf2225c9d9697", "ca192eb70b03e780dc19d872")
                }

                val phases = if (uv >= 6.0) {
                    listOf("GIAI ĐOẠN 1: LÀM SẠCH", "GIAI ĐOẠN 2: DƯỠNG ẨM SÂU", "GIAI ĐOẠN 3: BẢO VỆ UV")
                } else {
                    listOf("GIAI ĐOẠN 1: LÀM SẠCH", "GIAI ĐOẠN 2: DƯỠNG ẨM SÂU", "GIAI ĐOẠN 3: KHÓA ẨM DÀY")
                }

                val subcats = if (uv >= 6.0) {
                    listOf("SẠCH DỊU NHẸ", "CẤP NƯỚC HOA HỒNG", "CHỐNG NẮNG BẢO VỆ")
                } else {
                    listOf("SẠCH DỊU NHẸ", "CẤP NƯỚC HOA HỒNG", "KHÓA ẨM BIỂU BÌ")
                }

                val reasons = if (uv >= 6.0) {
                    listOf(
                        "Gel rửa mặt Hoa Hồng giúp bảo vệ lớp màng Lipid đang thiếu nước ẩm ($moistureStr).",
                        "Thạch Hoa Hồng cấp ẩm và bù đắp nước nhanh chóng khi độ ẩm không khí thấp ($humidityVal%).",
                        "Tia UV hôm nay đạt mức nguy hại ${String.format("%.1f", uv)}, bắt buộc thoa Sữa chống nắng Bí Đao để ngăn ngừa sạm nám."
                    )
                } else {
                    listOf(
                        "Gel rửa mặt Hoa Hồng làm sạch nhẹ nhàng và giữ ẩm lý tưởng cho làn da khô ráp.",
                        "Thạch Hoa Hồng cấp ẩm sâu đưa độ ẩm từ $moistureStr lên mức tối ưu.",
                        "Thạch hoa hồng 30ml nhỏ gọn dùng khóa ẩm tăng cường độ đàn hồi biểu bì ($barrierStr)."
                    )
                }

                RootieChatItem.DiagnosticData(
                    assessment = "Rootie nhận định: Da khô của bạn đang mất nước nghiêm trọng ($moistureStr) dưới nhiệt độ $temp°C.",
                    detailExplanation = "$weatherInfo Độ ẩm không khí chỉ đạt $humidityVal%, làm đẩy nhanh hiện tượng mất nước qua biểu bì của da khô ráp. Chỉ số đàn hồi da hiện tại là $elasticity%.",
                    moistureVal = moistureStr,
                    sensitivityVal = sensitivityStr,
                    barrierVal = barrierStr,
                    whyExplanation = "Sử dụng tinh chất và thạch hoa hồng hữu cơ giúp bổ sung độ ẩm tầng sâu, tạo màng khóa nước vững chắc ngăn bong tróc da.",
                    recommendedProductIds = recommendedIds,
                    productPhases = phases,
                    productSubcategories = subcats,
                    productExpertReasons = reasons
                )
            }
            else -> {
                val recommendedIds = if (uv >= 6.0) {
                    listOf("13afaa472ab5642f72112123", "c4b7cebcaf1a27611a9395af", "1e499ed75a31e4a02af2d962")
                } else {
                    listOf("13afaa472ab5642f72112123", "c4b7cebcaf1a27611a9395af", "fdc1ca708d8cf2225c9d9697")
                }

                val phases = if (uv >= 6.0) {
                    listOf("GIAI ĐOẠN 1: LÀM SẠCH", "GIAI ĐOẠN 2: DƯỠNG SÁNG", "GIAI ĐOẠN 3: BẢO VỆ UV")
                } else {
                    listOf("GIAI ĐOẠN 1: LÀM SẠCH", "GIAI ĐOẠN 2: DƯỠNG SÁNG", "GIAI ĐOẠN 3: PHỤC HỒI DỊU NHẸ")
                }

                val subcats = if (uv >= 6.0) {
                    listOf("SẠCH MỊN NGHỆ", "TINH CHẤT SÁNG DA", "CHỐNG UV GAY GẮT")
                } else {
                    listOf("SẠCH MỊN NGHỆ", "TINH CHẤT SÁNG DA", "KHÓA ẨM CHỮA LÀNH")
                }

                val reasons = if (uv >= 6.0) {
                    listOf(
                        "Sữa rửa mặt Nghệ Hưng Yên làm sạch sâu tế bào xỉn màu dưới trời nắng $weatherCondition.",
                        "Tinh chất Nghệ Hưng Yên C22 làm sáng da và chống oxy hóa mạnh mẽ.",
                        "Tia UV ${String.format("%.1f", uv)} cực kỳ độc hại cho da nhạy cảm mỏng yếu, bắt buộc che chắn và bôi KCN."
                    )
                } else {
                    listOf(
                        "Sữa rửa mặt Nghệ làm sạch tế bào chết dịu nhẹ mà không gây kích ứng da.",
                        "Tinh chất Nghệ Hưng Yên giúp mờ thâm và tăng cường sức đề kháng da.",
                        "Thạch Hoa Hồng phục hồi hàng rào ẩm ($barrierStr) và làm dịu những vùng da mẩn đỏ dưới trời $temp°C."
                    )
                }

                RootieChatItem.DiagnosticData(
                    assessment = "Rootie nhận định: Làn da nhạy cảm của bạn cần bảo vệ và phục hồi tích cực dưới tia UV $uv hôm nay.",
                    detailExplanation = "$weatherInfo Chỉ số nhạy cảm của bạn đang ở mức $sensitivity% ($sensitivityStr). Tia UV cao và khói bụi mịn $pm25 dễ làm da bị kích ứng đỏ rát.",
                    moistureVal = moistureStr,
                    sensitivityVal = sensitivityStr,
                    barrierVal = barrierStr,
                    whyExplanation = "Sử dụng curcumin từ nghệ và thạch phục hồi giúp làm dịu kích ứng tức thì, đẩy lùi tổn thương gốc tự do gây ra bởi tia UV.",
                    recommendedProductIds = recommendedIds,
                    productPhases = phases,
                    productSubcategories = subcats,
                    productExpertReasons = reasons
                )
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Restore floating chatbot head if enabled
        activity?.let { act ->
            val prefs = act.getSharedPreferences("RootieQuizPrefs", Context.MODE_PRIVATE)
            val enabled = prefs.getBoolean("SKIN_AI_FLOATING_CHAT_ENABLED", true)
            if (enabled) {
                act.findViewById<View>(com.veganbeauty.app.R.id.skin_ai_floating_chat_head)?.visibility = View.VISIBLE
            }
        }
        _binding = null
    }
}
