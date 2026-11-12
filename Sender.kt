package com.mawjatrend.pulse

import android.content.Context
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/** One HTTP delivery of an SMS to the site. Used right away from the receiver (fast timeouts) and by SendWorker (retries). */
object Sender {
    enum class Outcome { OK, RETRY, FAIL }

    fun send(c: Context, text: String, ts: Long, fast: Boolean): Outcome {
        val url = Prefs.url(c); val secret = Prefs.secret(c)
        if (!url.startsWith("https://") || secret.isBlank()) { Prefs.log(c, "✗ الإعدادات ناقصة (الرابط يجب أن يبدأ بـ https والسر مطلوب)"); return Outcome.FAIL }
        return try {
            val body = "secret=" + URLEncoder.encode(secret, "UTF-8") + "&ts=" + ts + "&text=" + URLEncoder.encode(text, "UTF-8")
            val sp = c.getSharedPreferences("hostguard", Context.MODE_PRIVATE)
            var cookie = sp.getString("test_cookie", "") ?: ""
            var target = if (cookie.isNotEmpty()) withI(url) else url
            var (code, resp) = post(target, body, cookie, fast)
            // InfinityFree browser-check page: solve it (AES), keep the cookie, resend once.
            if (code in 200..299 && resp.contains("aes.js")) {
                val solved = solveChallenge(resp)
                if (solved != null) {
                    cookie = solved; sp.edit().putString("test_cookie", cookie).apply()
                    target = withI(url)
                    val r2 = post(target, body, cookie, fast); code = r2.first; resp = r2.second
                }
            }
            val s = resp.take(300)
            when {
                code in 200..299 && Regex("\"ok\"\\s*:\\s*true").containsMatchIn(s) -> {
                    val matched = if (Regex("\"matched\"\\s*:\\s*null").containsMatchIn(s)) "لم تُطابق طلباً" else "تمت مطابقتها وإضافة الرصيد ✓"
                    Prefs.log(c, "✓ أُرسلت — $matched"); Outcome.OK
                }
                code in 200..299 -> { Prefs.log(c, "⚠ رد غير متوقع (قد تكون حماية الاستضافة): ${s.take(80)}"); Outcome.FAIL }
                code == 403 -> { Prefs.log(c, "✗ السر غير صحيح (403)"); Outcome.FAIL }
                code == 429 -> { Prefs.log(c, "↻ الموقع مشغول (429) — سأعيد المحاولة"); Outcome.RETRY }
                code in 400..499 -> { Prefs.log(c, "✗ رفض الموقع الطلب (HTTP $code)"); Outcome.FAIL }
                else -> { Prefs.log(c, "↻ تعذر الإرسال (HTTP $code) — سأعيد المحاولة"); Outcome.RETRY }
            }
        } catch (e: Exception) { Prefs.log(c, "↻ تعذر الإرسال (${e.javaClass.simpleName}) — سأعيد المحاولة"); Outcome.RETRY }
    }

    private fun withI(u: String) = if (u.contains("i=1")) u else u + (if (u.contains("?")) "&" else "?") + "i=1"

    private fun post(url: String, body: String, cookie: String, fast: Boolean): Pair<Int, String> {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"; connectTimeout = if (fast) 4000 else 15000; readTimeout = if (fast) 5000 else 20000; doOutput = true
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=utf-8")
            setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 13) DafaTilqaee/2.0")
            if (cookie.isNotEmpty()) setRequestProperty("Cookie", "__test=$cookie")
        }
        conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        val code = conn.responseCode
        val resp = (if (code in 200..299) conn.inputStream else conn.errorStream)?.bufferedReader()?.use { it.readText() } ?: ""
        conn.disconnect()
        return code to resp
    }

    private fun hex(s: String) = ByteArray(s.length / 2) { s.substring(it * 2, it * 2 + 2).toInt(16).toByte() }
    private fun solveChallenge(html: String): String? = try {
        val m = Regex("toNumbers\\(\"([0-9a-fA-F]+)\"\\)").findAll(html).map { it.groupValues[1] }.toList()
        if (m.size < 3) null else {
            val ci = Cipher.getInstance("AES/CBC/NoPadding")
            ci.init(Cipher.DECRYPT_MODE, SecretKeySpec(hex(m[0]), "AES"), IvParameterSpec(hex(m[1])))
            ci.doFinal(hex(m[2])).joinToString("") { "%02x".format(it) }
        }
    } catch (e: Exception) { null }
}
