package com.mawjatrend.pulse

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION || !Prefs.enabled(context)) return
        val parts = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        if (parts.isEmpty()) return
        val sender = parts[0].originatingAddress ?: ""
        val body = parts.joinToString("") { it.messageBody ?: "" }
        if (!Prefs.shouldForward(context, sender, body)) return
        val ts = parts[0].timestampMillis
        // Instant path: send immediately on its own thread (goAsync keeps the process alive ~10 s). Each SMS is independent,
        // so a burst of messages is delivered in parallel. If it fails, the retrying worker takes over: nothing is lost.
        val pending = goAsync()
        Thread {
            try {
                if (Sender.send(context.applicationContext, body, ts, true) == Sender.Outcome.RETRY) enqueue(context.applicationContext, body, ts)
            } catch (e: Throwable) { enqueue(context.applicationContext, body, ts) }
            finally { pending.finish() }
        }.start()
    }

    companion object {
        fun enqueue(context: Context, text: String, ts: Long = System.currentTimeMillis()) {
            val req = OneTimeWorkRequestBuilder<SendWorker>()
                .setInputData(workDataOf("text" to text, "ts" to ts))
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context).enqueue(req)
        }
    }
}
