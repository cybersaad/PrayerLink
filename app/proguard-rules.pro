# PrayerLink ProGuard Rules

# Keep Hilt-generated ViewModel factories
-keepnames @dagger.hilt.android.lifecycle.HiltViewModel class *

# Keep notification receivers (referenced in manifest)
-keep class com.prayerlink.app.notification.PrayerAlarmReceiver { *; }
-keep class com.prayerlink.app.notification.BootReceiver { *; }

# Keep WorkManager Worker
-keep class com.prayerlink.app.notification.DailyScheduleWorker { *; }
