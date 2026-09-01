package com.prayerlink.app.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.prayerlink.app.MainActivity
import com.prayerlink.app.R
import com.prayerlink.app.data.model.Prayer

/**
 * Creates the notification channel and shows prayer-time notifications.
 */
class PrayerNotificationManager(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "prayer_reminders"
        const val UPDATE_CHANNEL_ID = "app_updates"
        private const val NOTIFICATION_BASE_ID = 2000
        private const val UPDATE_NOTIFICATION_ID = 3000
    }

    init {
        createChannel()
    }

    private fun createChannel() {
        val nm = context.getSystemService(NotificationManager::class.java)

        val prayerChannel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.notification_channel_description)
            enableVibration(true)
            enableLights(true)
        }
        nm.createNotificationChannel(prayerChannel)

        val updateChannel = NotificationChannel(
            UPDATE_CHANNEL_ID,
            "App Updates",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notifications for new PrayerLink versions"
        }
        nm.createNotificationChannel(updateChannel)
    }

    /**
     * Show a prayer notification to the user (e.g. for Adhan).
     */
    fun show(prayerName: String, withSound: Boolean, withVibrate: Boolean) {
        val displayName = prayerDisplayName(prayerName)

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingTap = PendingIntent.getActivity(
            context, 0, tapIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setLargeIcon(android.graphics.BitmapFactory.decodeResource(context.resources, R.drawable.ic_logo))
            .setContentTitle(context.getString(R.string.notification_title, displayName))
            .setContentText(context.getString(R.string.notification_text, displayName))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingTap)

        if (!withSound) builder.setSilent(true)
        if (!withVibrate) builder.setVibrate(longArrayOf(0))

        if (ActivityCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(context)
                .notify(NOTIFICATION_BASE_ID + prayerName.hashCode(), builder.build())
        }
    }
    
    fun showMissedPrayerReminder(prayerName: String, iteration: Int) {
        val displayName = prayerDisplayName(prayerName)
        val text = if (iteration == 1) {
            "You have not marked your $displayName prayer as completed.\n\nDid you perform your prayer?"
        } else {
            "You still have not marked your $displayName prayer as completed.\n\nIf you are free, please perform your prayer.\n\nDid you perform it?"
        }
        showReminderNotification(prayerName, "Prayer Reminder", text, iteration)
    }

    fun showFinalQazaWarning(prayerName: String) {
        val displayName = prayerDisplayName(prayerName)
        val text = "You haven't performed $displayName yet. It will become Qaza in 10 minutes. Please perform your prayer."
        showReminderNotification(prayerName, "Final Qaza Warning", text, 0)
    }

    private fun showReminderNotification(prayerName: String, title: String, text: String, iteration: Int) {
        val prayerEnum = runCatching { Prayer.valueOf(prayerName) }.getOrNull() ?: return
        
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingTap = PendingIntent.getActivity(
            context, 0, tapIntent, PendingIntent.FLAG_IMMUTABLE
        )

        // Yes Action
        val yesIntent = Intent(context, PrayerActionReceiver::class.java).apply {
            action = PrayerActionReceiver.ACTION_MARK_COMPLETED
            putExtra(PrayerActionReceiver.EXTRA_PRAYER_NAME, prayerName)
        }
        val pendingYes = PendingIntent.getBroadcast(
            context, prayerEnum.hashCode() + 100, yesIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // No Action
        val noIntent = Intent(context, PrayerActionReceiver::class.java).apply {
            action = PrayerActionReceiver.ACTION_REMIND_LATER
            putExtra(PrayerActionReceiver.EXTRA_PRAYER_NAME, prayerName)
            putExtra(PrayerActionReceiver.EXTRA_ITERATION, iteration)
        }
        val pendingNo = PendingIntent.getBroadcast(
            context, prayerEnum.hashCode() + 200, noIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setLargeIcon(android.graphics.BitmapFactory.decodeResource(context.resources, R.drawable.ic_logo))
            .setContentTitle(title)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingTap)
            .addAction(0, "YES — I PRAYED", pendingYes)
            .addAction(0, "NO — NOT YET", pendingNo)

        if (ActivityCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val notificationId = PrayerActionReceiver.NOTIFICATION_REMINDER_BASE_ID + prayerEnum.hashCode()
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        }
    }

    private fun prayerDisplayName(enumName: String): String = when (enumName) {
        Prayer.FAJR.name    -> context.getString(R.string.prayer_fajr)
        Prayer.DHUHR.name   -> context.getString(R.string.prayer_dhuhr)
        Prayer.ASR.name     -> context.getString(R.string.prayer_asr)
        Prayer.MAGHRIB.name -> context.getString(R.string.prayer_maghrib)
        Prayer.ISHA.name    -> context.getString(R.string.prayer_isha)
        else -> enumName
    }

    fun showUpdateAvailableNotification(version: String) {
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            // We could pass an extra to tell MainActivity to open settings, but it's optional.
        }
        val pendingTap = PendingIntent.getActivity(
            context, 0, tapIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val text = "PrayerLink $version is now available. Tap to see what's new."

        val builder = NotificationCompat.Builder(context, UPDATE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification) // Ensure this is just a silhouette, transparent background
            .setLargeIcon(android.graphics.BitmapFactory.decodeResource(context.resources, R.drawable.ic_logo))
            .setContentTitle("PrayerLink Update Available")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingTap)

        if (ActivityCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(context).notify(UPDATE_NOTIFICATION_ID, builder.build())
        }
    }
}
