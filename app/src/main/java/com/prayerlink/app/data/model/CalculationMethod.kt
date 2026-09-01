package com.prayerlink.app.data.model

import com.prayerlink.app.R

/**
 * Major Islamic prayer-time calculation methods used worldwide.
 *
 * Each method specifies the sun altitude angles for Fajr and Isha.
 * Some methods (e.g. Umm al-Qura) define Isha as a fixed interval
 * after Maghrib instead of using an angle.
 *
 * @property nameResId   String resource ID for the display name.
 * @property fajrAngle   Sun depression angle for Fajr (degrees).
 * @property ishaAngle   Sun depression angle for Isha (degrees). Ignored when [ishaInterval] > 0.
 * @property ishaInterval Minutes after Maghrib to use for Isha (0 means use [ishaAngle]).
 */
enum class CalculationMethod(
    val nameResId: Int,
    val fajrAngle: Double,
    val ishaAngle: Double,
    val ishaInterval: Int = 0
) {
    MUSLIM_WORLD_LEAGUE(R.string.method_mwl, 18.0, 17.0),
    ISNA(R.string.method_isna, 15.0, 15.0),
    EGYPTIAN(R.string.method_egypt, 19.5, 17.5),
    UMM_AL_QURA(R.string.method_umm_al_qura, 18.5, 0.0, ishaInterval = 90),
    KARACHI(R.string.method_karachi, 18.0, 18.0),
    TEHRAN(R.string.method_tehran, 17.7, 14.0)
}
