package com.mawjatrend.pulse

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

/** Retry path: runs only when the instant send from SmsReceiver failed (no internet, site busy). Keeps trying up to 30 times. */
class SendWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {
    override fun doWork(): Result {
        val text = inputData.getString("text") ?: return Result.failure()
        val ts = inputData.getLong("ts", 0L)
        return when (Sender.send(applicationContext, text, ts, false)) {
            Sender.Outcome.OK -> Result.success()
            Sender.Outcome.FAIL -> Result.failure()
            Sender.Outcome.RETRY -> if (runAttemptCount < 30) Result.retry() else { Prefs.log(applicationContext, "✗ فشل نهائي بعد عدة محاولات"); Result.failure() }
        }
    }
}
