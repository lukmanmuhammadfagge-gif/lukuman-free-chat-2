package com.lukuman.freechat.util

object PhoneUtils {
    private val STRIP_REGEX = Regex("[\\s\\-().]+")
    private val NIGERIAN_E164_REGEX = Regex("^\\+234[1-9]\\d{9}$")

    /**
     * Normalizes Nigerian phone numbers into canonical E.164 format (+234XXXXXXXXXX).
     * Accepted inputs include +2348012345678, +234 801 234 5678, 2348012345678,
     * 08012345678, and 10-digit mobile numbers starting with 7/8/9 (8012345678).
     * Returns null when the input is not a valid Nigerian phone number.
     */
    fun normalizeNigerianPhone(raw: String): String? {
        val cleaned = raw.trim().replace(STRIP_REGEX, "")
        if (cleaned.isEmpty()) return null

        val candidate = when {
            cleaned.startsWith("+2340") && cleaned.length == 15 -> "+234" + cleaned.substring(5)
            cleaned.startsWith("+234") -> cleaned
            cleaned.startsWith("2340") && cleaned.length == 14 -> "+234" + cleaned.substring(4)
            cleaned.startsWith("234") && cleaned.length == 13 -> "+$cleaned"
            cleaned.startsWith("0") && cleaned.length == 11 -> "+234" + cleaned.substring(1)
            cleaned.length == 10 && (cleaned[0] == '7' || cleaned[0] == '8' || cleaned[0] == '9') ->
                "+234$cleaned"
            else -> return null
        }

        return if (NIGERIAN_E164_REGEX.matches(candidate)) candidate else null
    }
}
