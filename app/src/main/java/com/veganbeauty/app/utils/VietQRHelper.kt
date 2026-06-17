package com.veganbeauty.app.utils

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Helper for building a dynamic VietQR (QR code) URL that can be loaded into
 * an [android.widget.ImageView] via any image-loading library (e.g. Coil).
 *
 * VietQR is a Vietnamese open standard that lets merchant bank accounts
 * generate a unique payment QR containing the exact amount and a content
 * note. A popular free public image API for embedding the QR PNG is
 * `img.vietqr.io` (operated by VietQR). The URL is purely declarative so
 * no API key is required.
 *
 * Example:
 * ```
 * val url = VietQRHelper.buildImageUrl(
 *     bankCode = "VCB",
 *     accountNumber = "0123456789",
 *     amount = 241000,
 *     addInfo = "RDH14062026184300"
 * )
 * // Load `url` into an ImageView with Coil
 * ```
 */
object VietQRHelper {

    private const val IMAGE_BASE = "https://img.vietqr.io/image"

    /**
     * Build the full image URL for a given VietQR configuration.
     *
     * @param bankCode Short bank code (e.g. "VCB" for Vietcombank, "BIDV", "MB", "TCB", ...).
     * @param accountNumber Beneficiary account number / card number.
     * @param amount Amount in VND (must be > 0 to be encoded in the QR).
     * @param addInfo Transfer content/note (typically the order code).
     * @param template Template style: "compact" | "qr_only" | "print" (defaults to "compact").
     */
    fun buildImageUrl(
        bankCode: String,
        accountNumber: String,
        amount: Long,
        addInfo: String,
        template: String = "compact"
    ): String {
        val encodedAccount = URLEncoder.encode(accountNumber, StandardCharsets.UTF_8.name())
        val encodedTemplate = URLEncoder.encode(template, StandardCharsets.UTF_8.name())
        val safeAmount = if (amount < 0) 0 else amount

        val builder = StringBuilder(IMAGE_BASE)
            .append('/')
            .append(bankCode)
            .append('-')
            .append(encodedAccount)
            .append("-")
            .append(encodedTemplate)
            .append(".png")
            .append("?amount=")
            .append(safeAmount)

        if (addInfo.isNotBlank()) {
            builder
                .append("&addInfo=")
                .append(URLEncoder.encode(addInfo, StandardCharsets.UTF_8.name()))
        }
        return builder.toString()
    }
}
