package com.veganbeauty.app.utils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class VietQRHelper {

    private static final String IMAGE_BASE = "https://img.vietqr.io/image";

    public static String buildImageUrl(String bankCode, String accountNumber, long amount, String addInfo, String template) {
        try {
            String encodedAccount = URLEncoder.encode(accountNumber, StandardCharsets.UTF_8.name());
            String encodedTemplate = URLEncoder.encode(template, StandardCharsets.UTF_8.name());
            long safeAmount = Math.max(amount, 0);

            StringBuilder builder = new StringBuilder(IMAGE_BASE)
                    .append('/')
                    .append(bankCode)
                    .append('-')
                    .append(encodedAccount)
                    .append("-")
                    .append(encodedTemplate)
                    .append(".png")
                    .append("?amount=")
                    .append(safeAmount);

            if (addInfo != null && !addInfo.trim().isEmpty()) {
                builder.append("&addInfo=")
                       .append(URLEncoder.encode(addInfo, StandardCharsets.UTF_8.name()));
            }
            return builder.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public static String buildImageUrl(String bankCode, String accountNumber, long amount, String addInfo) {
        return buildImageUrl(bankCode, accountNumber, amount, addInfo, "compact");
    }
}
