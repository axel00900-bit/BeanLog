package com.example.beanlog

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

private const val CHANNEL_ID = "shot_feedback"
private const val PREFS = "beanlog_settings"
private const val PREF_REMIND = "shot_reminders"

fun scheduleShotReminder(context: Context, shotId: Long) {
    if (!context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(PREF_REMIND, false)) return
    val request = OneTimeWorkRequestBuilder<ShotReminderWorker>()
        .setInitialDelay(30, TimeUnit.MINUTES)
        .setInputData(Data.Builder().putLong("shot_id", shotId).build())
        .build()
    WorkManager.getInstance(context).enqueue(request)
}

class ShotReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val shotId = inputData.getLong("shot_id", -1L)
        if (shotId <= 0L) return Result.failure()
        val shot = AppDb.create(applicationContext).dao().shotById(shotId) ?: return Result.success()
        if (!applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(PREF_REMIND, false)) return Result.success()
        createShotChannel(applicationContext)
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return Result.success()

        val likeIntent = feedbackIntent(applicationContext, shotId, true)
        val dislikeIntent = feedbackIntent(applicationContext, shotId, false)
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("How was that espresso?")
            .setContentText("Did you like this shot? Tap Like or Dislike to save your feedback.")
            .setAutoCancel(true)
            .addAction(android.R.drawable.btn_star_big_on, "Like", likeIntent)
            .addAction(android.R.drawable.ic_delete, "Dislike", dislikeIntent)
            .build()
        try { NotificationManagerCompat.from(applicationContext).notify(shotId.toInt(), notification) }
        catch (_: SecurityException) { }
        return Result.success()
    }
}

private fun createShotChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= 26) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, "Shot feedback", NotificationManager.IMPORTANCE_DEFAULT))
    }
}

private fun feedbackIntent(context: Context, shotId: Long, liked: Boolean): PendingIntent {
    val intent = Intent(context, ShotFeedbackReceiver::class.java)
        .setAction("com.example.beanlog.SHOT_FEEDBACK")
        .putExtra("shot_id", shotId)
        .putExtra("liked", liked)
    val requestCode = (shotId * 2 + if (liked) 1 else 0).toInt()
    return PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
}

class ShotFeedbackReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "com.example.beanlog.SHOT_FEEDBACK") return
        val shotId = intent.getLongExtra("shot_id", -1L)
        val liked = intent.getBooleanExtra("liked", false)
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = AppDb.create(context.applicationContext).dao()
                dao.shotById(shotId)?.let { dao.saveShot(it.copy(liked = liked)) }
                NotificationManagerCompat.from(context).cancel(shotId.toInt())
            } finally { pending.finish() }
        }
    }
}

fun shotRemindersEnabled(context: Context): Boolean =
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(PREF_REMIND, false)

fun setShotRemindersEnabled(context: Context, enabled: Boolean) {
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(PREF_REMIND, enabled).apply()
    if (!enabled) WorkManager.getInstance(context).cancelAllWork()
}
