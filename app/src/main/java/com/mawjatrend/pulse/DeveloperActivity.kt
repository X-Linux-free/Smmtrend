package com.mawjatrend.pulse

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

/** "المطور" page: dark and gold, built in code like the main screen. */
class DeveloperActivity : Activity() {
    private val phone = "01554479023"
    private val gold = Color.parseColor("#E8B830")
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun btn(t: String, fill: Boolean, textColor: Int, bg: Int, stroke: Int, onClick: () -> Unit) = Button(this).apply {
        text = t; isAllCaps = false; setTextColor(textColor); textSize = 16f; setTypeface(null, Typeface.BOLD)
        background = GradientDrawable().apply {
            cornerRadius = dp(16).toFloat()
            if (fill) setColors(intArrayOf(Color.parseColor("#FFE08A"), Color.parseColor("#D9A21B"))) else setColor(bg)
            if (!fill) setStroke(dp(1), stroke)
        }
        setOnClickListener { onClick() }
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(52)).apply { topMargin = dp(10) }
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        window.statusBarColor = Color.parseColor("#0A0806"); window.navigationBarColor = Color.parseColor("#0A0806")
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL; layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(22), dp(34), dp(22), dp(26))
            background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(Color.parseColor("#2A2113"), Color.parseColor("#120F0A"))).apply {
                cornerRadius = dp(28).toFloat(); setStroke(dp(1), Color.parseColor("#6B5420"))
            }
        }
        card.addView(TextView(this).apply {
            text = "أ"; gravity = Gravity.CENTER; textSize = 44f; setTypeface(null, Typeface.BOLD); setTextColor(Color.parseColor("#2A1C00"))
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(Color.parseColor("#FFF1B8"), gold, Color.parseColor("#A87408"))).apply { shape = GradientDrawable.OVAL; setStroke(dp(3), Color.parseColor("#C9961A")) }
            layoutParams = LinearLayout.LayoutParams(dp(112), dp(112))
        })
        card.addView(TextView(this).apply { text = "المطور"; setTextColor(Color.parseColor("#A89A78")); textSize = 13f; setPadding(0, dp(16), 0, 0) })
        card.addView(TextView(this).apply { text = "أحمد محمد ابراهيم"; setTextColor(gold); textSize = 26f; setTypeface(null, Typeface.BOLD); gravity = Gravity.CENTER; setPadding(0, dp(4), 0, dp(6)) })
        card.addView(TextView(this).apply {
            text = "مطوّر نظام دفع تلقائي"; setTextColor(Color.parseColor("#E8C860")); textSize = 14f; setPadding(dp(16), dp(5), dp(16), dp(5))
            background = GradientDrawable().apply { cornerRadius = dp(30).toFloat(); setStroke(dp(1), Color.parseColor("#6B5420")) }
        })
        card.addView(TextView(this).apply { text = "دقّة في التحقق، سرعة في التنفيذ، وأمان في كل عملية."; setTextColor(Color.parseColor("#CDBF9F")); textSize = 14f; gravity = Gravity.CENTER; setPadding(0, dp(16), 0, dp(10)) })
        card.addView(View(this).apply { setBackgroundColor(Color.parseColor("#6B5420")); layoutParams = LinearLayout.LayoutParams(dp(180), dp(1)).apply { topMargin = dp(8); bottomMargin = dp(14) } })
        card.addView(TextView(this).apply { text = "للتواصل والطلبات"; setTextColor(Color.parseColor("#A89A78")); textSize = 13f })
        card.addView(TextView(this).apply { text = phone; setTextColor(Color.parseColor("#FFE08A")); textSize = 32f; setTypeface(null, Typeface.BOLD); layoutDirection = View.LAYOUT_DIRECTION_LTR; setPadding(0, dp(4), 0, dp(10)) })
        card.addView(btn("اتصال مباشر", true, Color.parseColor("#2A1C00"), 0, 0) { startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))) })
        card.addView(btn("واتساب", false, Color.parseColor("#7DF0A8"), Color.parseColor("#12301F"), Color.parseColor("#1F6B3F")) { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/20" + phone.removePrefix("0")))) })
        card.addView(btn("نسخ الرقم", false, Color.parseColor("#FFE08A"), Color.TRANSPARENT, Color.parseColor("#C9961A")) {
            (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("phone", phone)); Toast.makeText(this, "تم النسخ", Toast.LENGTH_SHORT).show()
        })
        card.addView(btn("رجوع", false, Color.WHITE, Color.TRANSPARENT, Color.parseColor("#3A3126")) { finish() })
        val wrap = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(24), dp(16), dp(24)); addView(card) }
        setContentView(ScrollView(this).apply { setBackgroundColor(Color.parseColor("#0A0806")); isFillViewport = true; addView(wrap) })
    }
}
