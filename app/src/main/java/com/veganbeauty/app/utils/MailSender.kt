package com.veganbeauty.app.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.GlobalScope
import java.util.Properties
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

object MailSender {
    private const val EMAIL = "khanhxuannguyen05@gmail.com"
    private const val PASSWORD = "uqrgrcraohvjschx"

    interface MailCallback {
        fun onSuccess()
        fun onError(e: Exception)
    }

    fun sendOtpEmailAsync(toEmail: String, otp: String, callback: MailCallback) {
        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
            try {
                val success = sendOtpEmail(toEmail, otp)
                withContext(Dispatchers.Main) {
                    if (success) callback.onSuccess() else callback.onError(Exception("Failed to send email"))
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback.onError(e)
                }
            }
        }
    }

    suspend fun sendOtpEmail(toEmail: String, otp: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val props = Properties()
            props["mail.smtp.auth"] = "true"
            props["mail.smtp.starttls.enable"] = "true"
            props["mail.smtp.host"] = "smtp.gmail.com"
            props["mail.smtp.port"] = "587"
            props["mail.smtp.ssl.protocols"] = "TLSv1.2"
            
            val session = Session.getInstance(props, object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication(EMAIL, PASSWORD)
                }
            })

            val message = MimeMessage(session)
            message.setFrom(InternetAddress(EMAIL))
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail))
            message.subject = "Mã xác nhận OTP từ Rootie"
            
            val content = """
                <html>
                <body>
                    <h3>Xin chào,</h3>
                    <p>Bạn đã yêu cầu đặt lại mật khẩu cho tài khoản tại Rootie.</p>
                    <p>Mã OTP của bạn là: <strong style="font-size: 24px; color: #1B5E20;">$otp</strong></p>
                    <p>Mã này có hiệu lực trong 5 phút. Vui lòng không chia sẻ mã này cho bất kỳ ai.</p>
                    <br/>
                    <p>Trân trọng,<br/><b>Nguyễn Khánh Xuân</b> - Customer Manager<br/>Đội ngũ Rootie</p>
                </body>
                </html>
            """.trimIndent()
            
            message.setContent(content, "text/html; charset=utf-8")
            Transport.send(message)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
