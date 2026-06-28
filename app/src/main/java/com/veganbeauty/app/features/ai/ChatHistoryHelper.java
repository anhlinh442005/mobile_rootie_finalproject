package com.veganbeauty.app.features.ai;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.veganbeauty.app.features.ai.RootieChatAdapter.RootieChatItem;

public class ChatHistoryHelper {

    private static final String FILE_NAME = "rootie_chat_history.json";

    public static void saveChatHistory(Context context, List<RootieChatItem> chatList) {
        try {
            JSONArray jsonArray = new JSONArray();
            for (RootieChatItem item : chatList) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("id", item.getId());
                jsonObject.put("sender", item.getSender().name());
                jsonObject.put("messageText", item.getMessageText());
                jsonObject.put("timeStr", item.getTimeStr());
                jsonObject.put("type", item.getType().name());

                if (item.getDiagnosticData() != null) {
                    RootieChatItem.DiagnosticData diag = item.getDiagnosticData();
                    JSONObject diagJson = new JSONObject();
                    diagJson.put("assessment", diag.assessment);
                    diagJson.put("detailExplanation", diag.detailExplanation);
                    diagJson.put("moistureVal", diag.moistureVal);
                    diagJson.put("sensitivityVal", diag.sensitivityVal);
                    diagJson.put("barrierVal", diag.barrierVal);
                    diagJson.put("whyExplanation", diag.whyExplanation);

                    if (diag.recommendedProductIds != null) {
                        diagJson.put("recommendedProductIds", new JSONArray(diag.recommendedProductIds));
                    }
                    if (diag.productPhases != null) {
                        diagJson.put("productPhases", new JSONArray(diag.productPhases));
                    }
                    if (diag.productSubcategories != null) {
                        diagJson.put("productSubcategories", new JSONArray(diag.productSubcategories));
                    }
                    if (diag.productExpertReasons != null) {
                        diagJson.put("productExpertReasons", new JSONArray(diag.productExpertReasons));
                    }

                    jsonObject.put("diagnosticData", diagJson);
                }
                jsonArray.put(jsonObject);
            }

            try (FileOutputStream fos = context.openFileOutput(FILE_NAME, Context.MODE_PRIVATE)) {
                fos.write(jsonArray.toString().getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static List<RootieChatItem> loadChatHistory(Context context) {
        List<RootieChatItem> list = new ArrayList<>();
        File file = new File(context.getFilesDir(), FILE_NAME);
        if (!file.exists()) return list;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            String jsonString = sb.toString();
            if (jsonString.trim().isEmpty()) return list;

            JSONArray jsonArray = new JSONArray(jsonString);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                String id = obj.optString("id", UUID.randomUUID().toString());
                RootieChatItem.Sender sender = RootieChatItem.Sender.valueOf(obj.optString("sender", "USER"));
                String messageText = obj.optString("messageText", "");
                String timeStr = obj.optString("timeStr", "");
                RootieChatItem.ItemType type = RootieChatItem.ItemType.valueOf(obj.optString("type", "TEXT"));

                RootieChatItem.DiagnosticData diagnosticData = null;
                if (obj.has("diagnosticData")) {
                    JSONObject diagObj = obj.getJSONObject("diagnosticData");
                    String assessment = diagObj.optString("assessment", "");
                    String detailExplanation = diagObj.optString("detailExplanation", "");
                    String moistureVal = diagObj.optString("moistureVal", "");
                    String sensitivityVal = diagObj.optString("sensitivityVal", "");
                    String barrierVal = diagObj.optString("barrierVal", "");
                    String whyExplanation = diagObj.optString("whyExplanation", "");

                    List<String> recommendedProductIds = new ArrayList<>();
                    JSONArray prodIdsArray = diagObj.optJSONArray("recommendedProductIds");
                    if (prodIdsArray != null) {
                        for (int j = 0; j < prodIdsArray.length(); j++) {
                            recommendedProductIds.add(prodIdsArray.getString(j));
                        }
                    }

                    List<String> productPhases = new ArrayList<>();
                    JSONArray phasesArray = diagObj.optJSONArray("productPhases");
                    if (phasesArray != null) {
                        for (int j = 0; j < phasesArray.length(); j++) {
                            productPhases.add(phasesArray.getString(j));
                        }
                    }

                    List<String> productSubcategories = new ArrayList<>();
                    JSONArray subcatsArray = diagObj.optJSONArray("productSubcategories");
                    if (subcatsArray != null) {
                        for (int j = 0; j < subcatsArray.length(); j++) {
                            productSubcategories.add(subcatsArray.getString(j));
                        }
                    }

                    List<String> productExpertReasons = new ArrayList<>();
                    JSONArray reasonsArray = diagObj.optJSONArray("productExpertReasons");
                    if (reasonsArray != null) {
                        for (int j = 0; j < reasonsArray.length(); j++) {
                            productExpertReasons.add(reasonsArray.getString(j));
                        }
                    }

                    diagnosticData = new RootieChatItem.DiagnosticData(
                            assessment,
                            detailExplanation,
                            moistureVal,
                            sensitivityVal,
                            barrierVal,
                            whyExplanation,
                            recommendedProductIds,
                            productPhases,
                            productSubcategories,
                            productExpertReasons
                    );
                }

                list.add(new RootieChatItem(id, sender, messageText, timeStr, type, diagnosticData));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public static void clearChatHistory(Context context) {
        try {
            File file = new File(context.getFilesDir(), FILE_NAME);
            if (file.exists()) {
                file.delete();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
