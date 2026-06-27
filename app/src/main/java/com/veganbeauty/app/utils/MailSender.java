package com.veganbeauty.app.utils;

import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class MailSender {
    private static final String EMAIL = "khanhxuannguyen05@gmail.com";
    private static final String PASSWORD = "uqrgrcraohvjschx";
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    public interface MailCallback {
        void onSuccess();
        void onError(Exception e);
    }

    public static void sendOtpEmailAsync(String toEmail, String otp, MailCallback callback) {
        executor.execute(() -> {
            try {
                boolean success = sendOtpEmail(toEmail, otp);
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    if (success) {
                        callback.onSuccess();
                    } else {
                        callback.onError(new Exception("Failed to send email"));
                    }
                });
            } catch (Exception e) {
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> callback.onError(e));
            }
        });
    }

    public static boolean sendOtpEmail(String toEmail, String otp) {
        try {
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", "smtp.gmail.com");
            props.put("mail.smtp.port", "587");
            props.put("mail.smtp.ssl.protocols", "TLSv1.2");
            
            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(EMAIL, PASSWORD);
                }
            });

            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(EMAIL));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject("Mã xác nhận OTP từ Rootie");
            
            String content = "<html>\n" +
                "<body>\n" +
                "    <h3>Xin chào,</h3>\n" +
                "    <p>Bạn đã yêu cầu đặt lại mật khẩu cho tài khoản tại Rootie.</p>\n" +
                "    <p>Mã OTP của bạn là: <strong style=\"font-size: 24px; color: #1B5E20;\">" + otp + "</strong></p>\n" +
                "    <p>Mã này có hiệu lực trong 5 phút. Vui lòng không chia sẻ mã này cho bất kỳ ai.</p>\n" +
                "    <br/>\n" +
                "    <p>Trân trọng,<br/><b>Nguyễn Khánh Xuân</b> - Customer Manager<br/>Đội ngũ Rootie</p>\n" +
                "</body>\n" +
                "</html>";
            
            message.setContent(content, "text/html; charset=utf-8");
            Transport.send(message);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
