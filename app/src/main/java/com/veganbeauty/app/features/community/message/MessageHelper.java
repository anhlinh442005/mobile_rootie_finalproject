package com.veganbeauty.app.features.community.message;

import android.content.Context;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import com.veganbeauty.app.data.local.ProfileSession;
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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MessageHelper {
    private static final String FILE_NAME = "community_message.json";
    
    private static final Map<String, ListenerRegistration> conversationListeners = new ConcurrentHashMap<>();
    private static final Map<String, ListenerRegistration> allConversationsListeners = new ConcurrentHashMap<>();
    
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
                            if (index != -1) {
                                data.set(index, conv);
                            } else {
                                data.add(conv);
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
                                if (index != -1) {
                                    data.set(index, conv);
                                } else {
                                    data.add(conv);
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
                                JsonElement jsonTree = new Gson().toJsonTree(conv);
                                Map<String, Object> map = new Gson().fromJson(jsonTree, Map.class);
                                db.collection("community_message").document(conv.getId()).set(map);
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
                e.printStackTrace();
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

    public static List<ConversationEntity> getConversations(Context context, String currentUserId) {
        List<ConversationEntity> all = readData(context);
        List<ConversationEntity> filtered = new ArrayList<>();
        for (ConversationEntity c : all) {
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

    public static ConversationEntity getConversation(Context context, String conversationId) {
        List<ConversationEntity> all = readData(context);
        for (ConversationEntity c : all) {
            if (c.getId().equals(conversationId)) return c;
        }
        return null;
    }

    public static List<ChatMessageEntity> getMessages(Context context, String conversationId) {
        ConversationEntity conv = getConversation(context, conversationId);
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
            
            docRef.update(updates);
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
            pushMessageToFirebase(conversationId, newMsg, receiverId, text, timeStr);
        }
    }

    public static String getOrCreateConversation(Context context, String currentUserId, String partnerId, String partnerName, String partnerAvatar) {
        List<ConversationEntity> data = readData(context);
        for (ConversationEntity c : data) {
            if (c.getMembers() != null && c.getMembers().contains(currentUserId) && c.getMembers().contains(partnerId)) {
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
                members,
                memberInfo,
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                timeStr,
                timeStr,
                "",
                timeStr,
                "private",
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
