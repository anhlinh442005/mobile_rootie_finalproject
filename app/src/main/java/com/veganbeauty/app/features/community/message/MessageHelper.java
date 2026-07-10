package com.veganbeauty.app.features.community.message;

import android.content.Context;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import com.veganbeauty.app.data.local.ProfileSession;
import com.veganbeauty.app.utils.RootieBrandHelper;
import com.veganbeauty.app.data.local.entities.ChatMessageEntity;
import com.veganbeauty.app.data.local.entities.ConversationEntity;
import com.veganbeauty.app.data.local.entities.MemberInfoEntity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.google.firebase.firestore.SetOptions;

public class MessageHelper {
    private static final String FILE_NAME = "community_message.json";
    public static final String MESSAGE_LIKE = "__like__";
    
    private static final Map<String, ListenerRegistration> conversationListeners = new ConcurrentHashMap<>();
    private static final Map<String, ListenerRegistration> allConversationsListeners = new ConcurrentHashMap<>();
    
    public static boolean isLikeMessage(String text) {
        return MESSAGE_LIKE.equals(text);
    }

    public static String formatPreviewText(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        return isLikeMessage(text) ? "👍" : text;
    }

    private static void pushToFirebase(ConversationEntity conv) {
        try {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            JsonElement jsonTree = new Gson().toJsonTree(conv);
            Map<String, Object> map = new Gson().fromJson(jsonTree, Map.class);
            db.collection("community_message").document(conv.getId()).set(map);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void listenToConversation(Context context, String conversationId, Runnable onUpdate) {
        removeConversationListener(conversationId);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        ListenerRegistration listener = db.collection("community_message").document(conversationId)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) return;
                    if (snapshot != null && snapshot.exists()) {
                        try {
                            Map<String, Object> map = snapshot.getData();
                            if (map == null) return;
                            JsonElement jsonTree = new Gson().toJsonTree(map);
                            ConversationEntity conv = new Gson().fromJson(jsonTree, ConversationEntity.class);
                            
                            List<ConversationEntity> data = readData(context);
                            int index = -1;
                            for (int i = 0; i < data.size(); i++) {
                                if (data.get(i).getId().equals(conversationId)) {
                                    index = i;
                                    break;
                                }
                            }
                            ConversationEntity local = index != -1 ? data.get(index) : null;
                            ConversationEntity merged = mergeConversations(local, conv);
                            if (index != -1) {
                                data.set(index, merged);
                            } else {
                                data.add(merged);
                            }
                            writeData(context, data);
                            if (onUpdate != null) onUpdate.run();
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                });
        conversationListeners.put(conversationId, listener);
    }

    public static void removeConversationListener(String conversationId) {
        ListenerRegistration listener = conversationListeners.remove(conversationId);
        if (listener != null) {
            listener.remove();
        }
    }
    
    public static void listenToAllConversations(Context context, String currentUserId, Runnable onUpdate) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        ListenerRegistration listener = db.collection("community_message")
                .whereArrayContains("members", currentUserId)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) return;
                    if (snapshot != null && !snapshot.isEmpty()) {
                        try {
                            List<ConversationEntity> data = readData(context);
                            boolean changed = false;
                            for (com.google.firebase.firestore.DocumentSnapshot doc : snapshot.getDocuments()) {
                                Map<String, Object> map = doc.getData();
                                if (map == null) continue;
                                JsonElement jsonTree = new Gson().toJsonTree(map);
                                ConversationEntity conv = new Gson().fromJson(jsonTree, ConversationEntity.class);
                                
                                int index = -1;
                                for (int i = 0; i < data.size(); i++) {
                                    if (data.get(i).getId().equals(conv.getId())) {
                                        index = i;
                                        break;
                                    }
                                }
                                ConversationEntity local = index != -1 ? data.get(index) : null;
                                ConversationEntity merged = mergeConversations(local, conv);
                                if (index != -1) {
                                    data.set(index, merged);
                                } else {
                                    data.add(merged);
                                }
                                changed = true;
                            }
                            if (changed) {
                                writeData(context, data);
                                if (onUpdate != null) onUpdate.run();
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                });
        allConversationsListeners.put(currentUserId, listener);
    }
    
    public static void removeAllConversationsListener(String currentUserId) {
        ListenerRegistration listener = allConversationsListeners.remove(currentUserId);
        if (listener != null) {
            listener.remove();
        }
    }

    public static void forceResetFirebaseFromAssets(Context context) {
        new Thread(() -> {
            try {
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                db.collection("community_message").get().addOnSuccessListener(snapshot -> {
                    for (com.google.firebase.firestore.DocumentSnapshot doc : snapshot.getDocuments()) {
                        doc.getReference().delete();
                    }
                    
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(context.getAssets().open("community_message.json")))) {
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = br.readLine()) != null) {
                            sb.append(line);
                        }
                        String jsonString = sb.toString();
                        Type type = new TypeToken<List<ConversationEntity>>() {}.getType();
                        List<ConversationEntity> conversations = new Gson().fromJson(jsonString, type);
                        
                        if (conversations != null) {
                            for (ConversationEntity conv : conversations) {
                                pushToFirebase(conv);
                            }
                        }
                        
                        File file = new File(context.getFilesDir(), FILE_NAME);
                        try (FileOutputStream fos = new FileOutputStream(file)) {
                            fos.write(jsonString.getBytes());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public static void initDataIfNeed(Context context) {
        File file = new File(context.getFilesDir(), FILE_NAME);
        if (!file.exists()) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(context.getAssets().open("community_message.json")));
                 FileOutputStream fos = new FileOutputStream(file)) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
                fos.write(sb.toString().getBytes());
            } catch (Exception e) {
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    fos.write("[]".getBytes());
                } catch (Exception ignored) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static List<ConversationEntity> readData(Context context) {
        initDataIfNeed(context);
        File file = new File(context.getFilesDir(), FILE_NAME);
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            Type type = new TypeToken<List<ConversationEntity>>() {}.getType();
            List<ConversationEntity> list = new Gson().fromJson(sb.toString(), type);
            return list != null ? new ArrayList<>(list) : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private static void writeData(Context context, List<ConversationEntity> data) {
        try {
            File file = new File(context.getFilesDir(), FILE_NAME);
            String json = new Gson().toJson(data);
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(json.getBytes());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static List<ConversationEntity> getConversations(Context context) {
        String currentUserId = ProfileSession.getCurrentUserId(context);
        return getConversations(context, currentUserId);
    }

    public static List<ConversationEntity> getConversations(Context context, String currentUserId) {
        syncConversationsFromAssets(context, currentUserId);
        List<ConversationEntity> all = readData(context);
        List<ConversationEntity> filtered = new ArrayList<>();
        for (ConversationEntity c : all) {
            normalizeBrandAvatars(c);
            if (c.getMembers() != null && c.getMembers().contains(currentUserId)) {
                filtered.add(c);
            }
        }
        filtered.sort((a, b) -> {
            String ta = a.getUpdatedAt() != null ? a.getUpdatedAt() : "";
            String tb = b.getUpdatedAt() != null ? b.getUpdatedAt() : "";
            return tb.compareTo(ta);
        });
        return filtered;
    }

    public static void syncConversationsFromAssets(Context context) {
        syncConversationsFromAssets(context, ProfileSession.getCurrentUserId(context));
    }

    public static void syncConversationsFromAssets(Context context, String currentUserId) {
        initDataIfNeed(context);
        List<ConversationEntity> local = readData(context);
        List<ConversationEntity> fromAssets = readAssetsData(context, currentUserId);
        if (fromAssets.isEmpty()) {
            return;
        }

        Map<String, ConversationEntity> merged = new HashMap<>();
        for (ConversationEntity conversation : fromAssets) {
            merged.put(conversation.getId(), conversation);
        }
        for (ConversationEntity conversation : local) {
            ConversationEntity existing = merged.get(conversation.getId());
            if (existing == null) {
                merged.put(conversation.getId(), conversation);
            } else {
                merged.put(conversation.getId(), mergeConversations(existing, conversation));
            }
        }

        List<ConversationEntity> result = new ArrayList<>(merged.values());
        for (ConversationEntity conversation : result) {
            normalizeBrandAvatars(conversation);
        }
        writeData(context, result);
    }

    public static void fetchAndMergeFirebaseConversations(Context context, String currentUserId, Runnable onComplete) {
        if (currentUserId == null || currentUserId.trim().isEmpty()) {
            if (onComplete != null) {
                onComplete.run();
            }
            return;
        }
        FirebaseFirestore.getInstance().collection("community_message")
                .whereArrayContains("members", currentUserId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    try {
                        List<ConversationEntity> data = readData(context);
                        Map<String, ConversationEntity> merged = new HashMap<>();
                        for (ConversationEntity conversation : data) {
                            merged.put(conversation.getId(), conversation);
                        }
                        for (com.google.firebase.firestore.DocumentSnapshot doc : snapshot.getDocuments()) {
                            Map<String, Object> map = doc.getData();
                            if (map == null) {
                                continue;
                            }
                            JsonElement jsonTree = new Gson().toJsonTree(map);
                            ConversationEntity conv = new Gson().fromJson(jsonTree, ConversationEntity.class);
                            if (conv == null || conv.getId() == null) {
                                continue;
                            }
                            ConversationEntity existing = merged.get(conv.getId());
                            merged.put(conv.getId(), existing == null ? conv : mergeConversations(existing, conv));
                        }
                        List<ConversationEntity> result = new ArrayList<>(merged.values());
                        for (ConversationEntity conversation : result) {
                            normalizeBrandAvatars(conversation);
                        }
                        writeData(context, result);
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        if (onComplete != null) {
                            onComplete.run();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    e.printStackTrace();
                    if (onComplete != null) {
                        onComplete.run();
                    }
                });
    }

    private static List<ConversationEntity> readAssetsData(Context context, String currentUserId) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(context.getAssets().open("community_message.json")))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            return parsePersonalizedTemplates(
                    sb.toString(),
                    currentUserId != null ? currentUserId : "",
                    ProfileSession.getFullName(context) != null ? ProfileSession.getFullName(context) : "Bạn",
                    ProfileSession.getAvatar(context) != null ? ProfileSession.getAvatar(context) : ""
            );
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public static List<ConversationEntity> parsePersonalizedTemplates(
            String templateJson,
            String userId,
            String userName,
            String userAvatar
    ) {
        if (templateJson == null || templateJson.trim().isEmpty() || userId == null || userId.isEmpty()) {
            return new ArrayList<>();
        }
        // Skip official accounts to prevent duplicate key exceptions in Gson when {{USER_ID}} is replaced with their own ID
        if (userId.equals("rootie_vn") || userId.equals("72400102") || userId.equals("86237409") || 
            userId.equals("75675216") || userId.equals("39751498")) {
            return new ArrayList<>();
        }
        String safeName = userName != null ? userName.replace("\"", "\\\"") : "Bạn";
        String safeAvatar = userAvatar != null ? userAvatar.replace("\"", "\\\"") : "";
        String personalizedJson = templateJson
                .replace("{{USER_ID}}", userId)
                .replace("{{USER_NAME}}", safeName)
                .replace("{{USER_AVATAR}}", safeAvatar);
        Type type = new TypeToken<List<ConversationEntity>>() {}.getType();
        List<ConversationEntity> list = new Gson().fromJson(personalizedJson, type);
        return list != null ? new ArrayList<>(list) : new ArrayList<>();
    }

    private static ConversationEntity pickNewerConversation(ConversationEntity first, ConversationEntity second) {
        String firstUpdatedAt = first.getUpdatedAt() != null ? first.getUpdatedAt() : "";
        String secondUpdatedAt = second.getUpdatedAt() != null ? second.getUpdatedAt() : "";
        return secondUpdatedAt.compareTo(firstUpdatedAt) >= 0 ? second : first;
    }

    private static ConversationEntity mergeConversations(ConversationEntity first, ConversationEntity second) {
        if (first == null) {
            return second;
        }
        if (second == null) {
            return first;
        }

        ConversationEntity base = pickNewerConversation(first, second);
        ConversationEntity other = base == first ? second : first;

        ConversationEntity merged = new ConversationEntity();
        merged.setId(base.getId());
        merged.setChatType(base.getChatType() != null ? base.getChatType() : other.getChatType());
        merged.setMembers(base.getMembers() != null ? base.getMembers() : other.getMembers());

        Map<String, MemberInfoEntity> memberInfo = new HashMap<>();
        if (other.getMemberInfo() != null) {
            memberInfo.putAll(other.getMemberInfo());
        }
        if (base.getMemberInfo() != null) {
            memberInfo.putAll(base.getMemberInfo());
        }
        merged.setMemberInfo(memberInfo);
        merged.setActiveBy(mergeStringLists(first.getActiveBy(), second.getActiveBy()));
        merged.setTypingBy(mergeStringLists(first.getTypingBy(), second.getTypingBy()));
        merged.setUnreadBy(mergeStringLists(first.getUnreadBy(), second.getUnreadBy()));
        merged.setCreatedAt(earliestTimestamp(first.getCreatedAt(), second.getCreatedAt()));

        List<ChatMessageEntity> mergedMessages = mergeMessages(first.getMessages(), second.getMessages());
        merged.setMessages(mergedMessages);
        if (!mergedMessages.isEmpty()) {
            ChatMessageEntity last = mergedMessages.get(mergedMessages.size() - 1);
            merged.setLastMessage(last.getText());
            merged.setLastMessageAt(last.getSentAt());
            merged.setUpdatedAt(last.getSentAt());
        } else {
            merged.setLastMessage(base.getLastMessage() != null ? base.getLastMessage() : other.getLastMessage());
            merged.setLastMessageAt(base.getLastMessageAt() != null ? base.getLastMessageAt() : other.getLastMessageAt());
            merged.setUpdatedAt(base.getUpdatedAt() != null ? base.getUpdatedAt() : other.getUpdatedAt());
        }
        return merged;
    }

    private static List<String> mergeStringLists(List<String> first, List<String> second) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        if (first != null) {
            values.addAll(first);
        }
        if (second != null) {
            values.addAll(second);
        }
        return new ArrayList<>(values);
    }

    private static List<ChatMessageEntity> mergeMessages(List<ChatMessageEntity> first, List<ChatMessageEntity> second) {
        Map<String, ChatMessageEntity> byId = new LinkedHashMap<>();
        if (first != null) {
            for (ChatMessageEntity message : first) {
                if (message.getId() != null) {
                    byId.put(message.getId(), message);
                }
            }
        }
        if (second != null) {
            for (ChatMessageEntity message : second) {
                if (message.getId() == null) {
                    continue;
                }
                ChatMessageEntity existing = byId.get(message.getId());
                byId.put(message.getId(), existing == null ? message : preferMessage(existing, message));
            }
        }
        List<ChatMessageEntity> merged = new ArrayList<>(byId.values());
        merged.sort((left, right) -> {
            String leftSentAt = left.getSentAt() != null ? left.getSentAt() : "";
            String rightSentAt = right.getSentAt() != null ? right.getSentAt() : "";
            return leftSentAt.compareTo(rightSentAt);
        });
        return merged;
    }

    private static ChatMessageEntity preferMessage(ChatMessageEntity first, ChatMessageEntity second) {
        String firstSentAt = first.getSentAt() != null ? first.getSentAt() : "";
        String secondSentAt = second.getSentAt() != null ? second.getSentAt() : "";
        if (secondSentAt.compareTo(firstSentAt) > 0) {
            return second;
        }
        if (firstSentAt.compareTo(secondSentAt) > 0) {
            return first;
        }
        if (first.getSeenAt() != null && second.getSeenAt() == null) {
            return first;
        }
        if (second.getSeenAt() != null && first.getSeenAt() == null) {
            return second;
        }
        return second;
    }

    private static String earliestTimestamp(String first, String second) {
        if (first == null || first.isEmpty()) {
            return second;
        }
        if (second == null || second.isEmpty()) {
            return first;
        }
        return first.compareTo(second) <= 0 ? first : second;
    }

    private static void normalizeBrandAvatars(ConversationEntity conversation) {
        if (conversation == null || conversation.getMemberInfo() == null) {
            return;
        }
        for (Map.Entry<String, MemberInfoEntity> entry : conversation.getMemberInfo().entrySet()) {
            if (RootieBrandHelper.isRootieUser(entry.getKey()) && entry.getValue() != null) {
                entry.getValue().setAvatar(RootieBrandHelper.AVATAR_URL);
            }
        }
    }

    public static ConversationEntity getConversationById(Context context, String conversationId) {
        List<ConversationEntity> all = readData(context);
        for (ConversationEntity c : all) {
            if (c.getId().equals(conversationId)) return c;
        }
        return null;
    }

    public static List<ChatMessageEntity> getMessages(Context context, String conversationId) {
        ConversationEntity conv = getConversationById(context, conversationId);
        if (conv == null || conv.getMessages() == null) return new ArrayList<>();
        List<ChatMessageEntity> messages = new ArrayList<>(conv.getMessages());
        messages.sort((a, b) -> {
            String ta = a.getSentAt() != null ? a.getSentAt() : "";
            String tb = b.getSentAt() != null ? b.getSentAt() : "";
            return ta.compareTo(tb);
        });
        return messages;
    }

    private static String getCurrentTimeString() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format.format(new Date());
    }

    private static void pushMessageToFirebase(String conversationId, ChatMessageEntity newMsg, String receiverId, String text, String timeStr) {
        try {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            com.google.firebase.firestore.DocumentReference docRef = db.collection("community_message").document(conversationId);
            JsonElement msgTree = new Gson().toJsonTree(newMsg);
            Map<String, Object> msgMap = new Gson().fromJson(msgTree, Map.class);
            
            Map<String, Object> updates = new HashMap<>();
            updates.put("last_message", text);
            updates.put("last_message_at", timeStr);
            updates.put("updated_at", timeStr);
            updates.put("unread_by", FieldValue.arrayUnion(receiverId));
            updates.put("messages", FieldValue.arrayUnion(msgMap));
            
            docRef.set(updates, SetOptions.merge());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void pushReadStatusToFirebase(String conversationId, String userId) {
        try {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            com.google.firebase.firestore.DocumentReference docRef = db.collection("community_message").document(conversationId);
            docRef.get().addOnSuccessListener(snapshot -> {
                if (snapshot.exists()) {
                    String timeStr = getCurrentTimeString();
                    List<String> unreadBy = (List<String>) snapshot.get("unread_by");
                    if (unreadBy == null) unreadBy = new ArrayList<>();
                    List<String> updatedUnread = new ArrayList<>();
                    for (String u : unreadBy) {
                        if (!u.equals(userId)) updatedUnread.add(u);
                    }
                    
                    List<Map<String, Object>> messagesRaw = (List<Map<String, Object>>) snapshot.get("messages");
                    if (messagesRaw == null) messagesRaw = new ArrayList<>();
                    List<Map<String, Object>> updatedMessages = new ArrayList<>();
                    for (Map<String, Object> msgMap : messagesRaw) {
                        String senderId = msgMap.get("sender_id") != null ? msgMap.get("sender_id").toString() : "";
                        if (!senderId.equals(userId) && msgMap.get("seen_at") == null) {
                            Map<String, Object> newMap = new HashMap<>(msgMap);
                            newMap.put("seen_at", timeStr);
                            updatedMessages.add(newMap);
                        } else {
                            updatedMessages.add(msgMap);
                        }
                    }
                    
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("unread_by", updatedUnread);
                    updates.put("messages", updatedMessages);
                    docRef.update(updates);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void markAsRead(Context context, String conversationId, String userId) {
        List<ConversationEntity> data = readData(context);
        int index = -1;
        for (int i = 0; i < data.size(); i++) {
            if (data.get(i).getId().equals(conversationId)) {
                index = i;
                break;
            }
        }
        if (index != -1) {
            ConversationEntity conv = data.get(index);
            List<String> unreadBy = conv.getUnreadBy() != null ? new ArrayList<>(conv.getUnreadBy()) : new ArrayList<>();
            unreadBy.remove(userId);
            conv.setUnreadBy(unreadBy);
            
            List<ChatMessageEntity> newMessages = new ArrayList<>();
            if (conv.getMessages() != null) {
                for (ChatMessageEntity msg : conv.getMessages()) {
                    if (!userId.equals(msg.getSenderId()) && msg.getSeenAt() == null) {
                        msg.setSeenAt(getCurrentTimeString());
                    }
                    newMessages.add(msg);
                }
            }
            conv.setMessages(newMessages);
            data.set(index, conv);
            writeData(context, data);
            pushReadStatusToFirebase(conversationId, userId);
        }
    }

    public static void sendMessage(Context context, String conversationId, String senderId, String text) {
        ConversationEntity conv = getConversationById(context, conversationId);
        if (conv == null) return;
        String receiverId = "";
        if (conv.getMembers() != null) {
            for (String m : conv.getMembers()) {
                if (!m.equals(senderId)) {
                    receiverId = m;
                    break;
                }
            }
        }
        sendMessage(context, conversationId, senderId, receiverId, text);
    }

    public static void sendMessage(Context context, String conversationId, String senderId, String receiverId, String text) {
        List<ConversationEntity> data = readData(context);
        int index = -1;
        for (int i = 0; i < data.size(); i++) {
            if (data.get(i).getId().equals(conversationId)) {
                index = i;
                break;
            }
        }
        if (index != -1) {
            ConversationEntity conv = data.get(index);
            String msgId = "m_" + UUID.randomUUID().toString().substring(0, 8);
            String timeStr = getCurrentTimeString();
            
            ChatMessageEntity newMsg = new ChatMessageEntity(
                    msgId,
                    senderId,
                    text,
                    timeStr,
                    timeStr,
                    null
            );
            
            List<String> unreadBy = conv.getUnreadBy() != null ? new ArrayList<>(conv.getUnreadBy()) : new ArrayList<>();
            if (!unreadBy.contains(receiverId)) {
                unreadBy.add(receiverId);
            }
            
            List<ChatMessageEntity> updatedMessages = conv.getMessages() != null ? new ArrayList<>(conv.getMessages()) : new ArrayList<>();
            updatedMessages.add(newMsg);
            
            conv.setLastMessage(text);
            conv.setLastMessageAt(timeStr);
            conv.setUpdatedAt(timeStr);
            conv.setUnreadBy(unreadBy);
            conv.setMessages(updatedMessages);
            
            data.set(index, conv);
            writeData(context, data);
            pushToFirebase(conv);
        }
    }

    public static String getOrCreateConversation(Context context, String currentUserId, String partnerId, String partnerName, String partnerAvatar) {
        if (RootieBrandHelper.isRootieUser(partnerId)) {
            partnerAvatar = RootieBrandHelper.AVATAR_URL;
        }

        List<ConversationEntity> data = readData(context);
        for (ConversationEntity c : data) {
            if (c.getMembers() != null && c.getMembers().contains(currentUserId) && c.getMembers().contains(partnerId)) {
                if (RootieBrandHelper.isRootieUser(partnerId) && c.getMemberInfo() != null) {
                    MemberInfoEntity info = c.getMemberInfo().get(partnerId);
                    if (info == null || !RootieBrandHelper.AVATAR_URL.equals(info.getAvatar())) {
                        c.getMemberInfo().put(partnerId, new MemberInfoEntity(partnerName, RootieBrandHelper.AVATAR_URL));
                        writeData(context, data);
                    }
                }
                return c.getId();
            }
        }
        
        String currentUserName = ProfileSession.getFullName(context) != null ? ProfileSession.getFullName(context) : "Unknown";
        String currentUserAvatar = ProfileSession.getAvatar(context) != null ? ProfileSession.getAvatar(context) : "";
        
        String newConvId = "chat_" + partnerId + "_" + currentUserId;
        String timeStr = getCurrentTimeString();
        
        List<String> members = new ArrayList<>();
        members.add(currentUserId);
        members.add(partnerId);
        
        Map<String, MemberInfoEntity> memberInfo = new HashMap<>();
        memberInfo.put(currentUserId, new MemberInfoEntity(currentUserName, currentUserAvatar));
        memberInfo.put(partnerId, new MemberInfoEntity(partnerName, partnerAvatar));
        
        ConversationEntity newConv = new ConversationEntity(
                newConvId,
                "private",
                members,
                memberInfo,
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                timeStr,
                timeStr,
                "",
                timeStr,
                new ArrayList<>()
        );
        
        data.add(newConv);
        writeData(context, data);
        pushToFirebase(newConv);
        return newConvId;
    }

    public static void updateMessage(Context context, String conversationId, String messageId, String newText) {
        List<ConversationEntity> data = readData(context);
        int index = -1;
        for (int i = 0; i < data.size(); i++) {
            if (data.get(i).getId().equals(conversationId)) {
                index = i;
                break;
            }
        }
        if (index != -1) {
            ConversationEntity conv = data.get(index);
            List<ChatMessageEntity> updatedMessages = new ArrayList<>();
            if (conv.getMessages() != null) {
                for (ChatMessageEntity msg : conv.getMessages()) {
                    if (msg.getId().equals(messageId)) {
                        msg.setText(newText);
                    }
                    updatedMessages.add(msg);
                }
            }
            
            String lastMsg = conv.getLastMessage() != null ? conv.getLastMessage() : "";
            if (!updatedMessages.isEmpty() && updatedMessages.get(updatedMessages.size() - 1).getId().equals(messageId)) {
                lastMsg = newText;
            }
            
            conv.setMessages(updatedMessages);
            conv.setLastMessage(lastMsg);
            
            data.set(index, conv);
            writeData(context, data);
            pushToFirebase(conv);
        }
    }

    public static void deleteMessage(Context context, String conversationId, String messageId) {
        List<ConversationEntity> data = readData(context);
        int index = -1;
        for (int i = 0; i < data.size(); i++) {
            if (data.get(i).getId().equals(conversationId)) {
                index = i;
                break;
            }
        }
        if (index != -1) {
            ConversationEntity conv = data.get(index);
            List<ChatMessageEntity> updatedMessages = new ArrayList<>();
            if (conv.getMessages() != null) {
                for (ChatMessageEntity msg : conv.getMessages()) {
                    if (!msg.getId().equals(messageId)) {
                        updatedMessages.add(msg);
                    }
                }
            }
            
            String lastMsg = conv.getLastMessage() != null ? conv.getLastMessage() : "";
            String lastMsgAt = conv.getLastMessageAt() != null ? conv.getLastMessageAt() : "";
            
            if (conv.getMessages() != null && !conv.getMessages().isEmpty() && conv.getMessages().get(conv.getMessages().size() - 1).getId().equals(messageId)) {
                lastMsg = !updatedMessages.isEmpty() ? updatedMessages.get(updatedMessages.size() - 1).getText() : "";
                lastMsgAt = !updatedMessages.isEmpty() ? updatedMessages.get(updatedMessages.size() - 1).getSentAt() : (conv.getCreatedAt() != null ? conv.getCreatedAt() : "");
            }
            
            conv.setMessages(updatedMessages);
            conv.setLastMessage(lastMsg);
            conv.setLastMessageAt(lastMsgAt);
            
            data.set(index, conv);
            writeData(context, data);
            pushToFirebase(conv);
        }
    }
}
