package com.prayerlink.app.data.model

/**
 * A city with geographic coordinates for prayer-time calculation.
 */
data class City(
    val nameEn: String,
    val nameAr: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val timeZoneId: String
) {
    /** Returns a display label like "Makkah, SA". */
    fun displayName(isArabic: Boolean): String =
        if (isArabic) "$nameAr, $country" else "$nameEn, $country"
}

/**
 * Curated list of major cities covering key Muslim populations worldwide.
 * Sorted alphabetically by English name.
 */
object CityData {
    val cities: List<City> = listOf(
        City("Abu Dhabi", "أبو ظبي", "AE", 24.4539, 54.3773, "Asia/Dubai"),
        City("Alexandria", "الإسكندرية", "EG", 31.2001, 29.9187, "Africa/Cairo"),
        City("Algiers", "الجزائر", "DZ", 36.7538, 3.0588, "Africa/Algiers"),
        City("Amman", "عمان", "JO", 31.9454, 35.9284, "Asia/Amman"),
        City("Amsterdam", "أمستردام", "NL", 52.3676, 4.9041, "Europe/Amsterdam"),
        City("Ankara", "أنقرة", "TR", 39.9334, 32.8597, "Europe/Istanbul"),
        City("Baghdad", "بغداد", "IQ", 33.3152, 44.3661, "Asia/Baghdad"),
        City("Baku", "باكو", "AZ", 40.4093, 49.8671, "Asia/Baku"),
        City("Beirut", "بيروت", "LB", 33.8938, 35.5018, "Asia/Beirut"),
        City("Berlin", "برلين", "DE", 52.5200, 13.4050, "Europe/Berlin"),
        City("Brunei", "بروناي", "BN", 4.9031, 114.9398, "Asia/Brunei"),
        City("Brussels", "بروكسل", "BE", 50.8503, 4.3517, "Europe/Brussels"),
        City("Cairo", "القاهرة", "EG", 30.0444, 31.2357, "Africa/Cairo"),
        City("Casablanca", "الدار البيضاء", "MA", 33.5731, -7.5898, "Africa/Casablanca"),
        City("Chicago", "شيكاغو", "US", 41.8781, -87.6298, "America/Chicago"),
        City("Damascus", "دمشق", "SY", 33.5138, 36.2765, "Asia/Damascus"),
        City("Dar es Salaam", "دار السلام", "TZ", -6.7924, 39.2083, "Africa/Dar_es_Salaam"),
        City("Delhi", "دلهي", "IN", 28.7041, 77.1025, "Asia/Kolkata"),
        City("Dhaka", "دكا", "BD", 23.8103, 90.4125, "Asia/Dhaka"),
        City("Doha", "الدوحة", "QA", 25.2867, 51.5333, "Asia/Qatar"),
        City("Dubai", "دبي", "AE", 25.2048, 55.2708, "Asia/Dubai"),
        City("Houston", "هيوستن", "US", 29.7604, -95.3698, "America/Chicago"),
        City("Islamabad", "إسلام آباد", "PK", 33.6844, 73.0479, "Asia/Karachi"),
        City("Istanbul", "إسطنبول", "TR", 41.0082, 28.9784, "Europe/Istanbul"),
        City("Jakarta", "جاكرتا", "ID", -6.2088, 106.8456, "Asia/Jakarta"),
        City("Jeddah", "جدة", "SA", 21.5433, 39.1728, "Asia/Riyadh"),
        City("Jerusalem", "القدس", "PS", 31.7683, 35.2137, "Asia/Jerusalem"),
        City("Johannesburg", "جوهانسبرغ", "ZA", -26.2041, 28.0473, "Africa/Johannesburg"),
        City("Karachi", "كراتشي", "PK", 24.8607, 67.0011, "Asia/Karachi"),
        City("Khartoum", "الخرطوم", "SD", 15.5007, 32.5599, "Africa/Khartoum"),
        City("Kuala Lumpur", "كوالالمبور", "MY", 3.1390, 101.6869, "Asia/Kuala_Lumpur"),
        City("Kuwait City", "مدينة الكويت", "KW", 29.3759, 47.9774, "Asia/Kuwait"),
        City("Lagos", "لاغوس", "NG", 6.5244, 3.3792, "Africa/Lagos"),
        City("Lahore", "لاهور", "PK", 31.5204, 74.3587, "Asia/Karachi"),
        City("London", "لندن", "GB", 51.5074, -0.1278, "Europe/London"),
        City("Los Angeles", "لوس أنجلوس", "US", 34.0522, -118.2437, "America/Los_Angeles"),
        City("Madinah", "المدينة المنورة", "SA", 24.4672, 39.6024, "Asia/Riyadh"),
        City("Madrid", "مدريد", "ES", 40.4168, -3.7038, "Europe/Madrid"),
        City("Makkah", "مكة المكرمة", "SA", 21.4225, 39.8262, "Asia/Riyadh"),
        City("Manama", "المنامة", "BH", 26.2285, 50.5860, "Asia/Bahrain"),
        City("Melbourne", "ملبورن", "AU", -37.8136, 144.9631, "Australia/Melbourne"),
        City("Moscow", "موسكو", "RU", 55.7558, 37.6173, "Europe/Moscow"),
        City("Mumbai", "مومباي", "IN", 19.0760, 72.8777, "Asia/Kolkata"),
        City("Muscat", "مسقط", "OM", 23.5880, 58.3829, "Asia/Muscat"),
        City("Nairobi", "نيروبي", "KE", -1.2864, 36.8172, "Africa/Nairobi"),
        City("New York", "نيويورك", "US", 40.7128, -74.0060, "America/New_York"),
        City("Paris", "باريس", "FR", 48.8566, 2.3522, "Europe/Paris"),
        City("Riyadh", "الرياض", "SA", 24.7136, 46.6753, "Asia/Riyadh"),
        City("Rome", "روما", "IT", 41.9028, 12.4964, "Europe/Rome"),
        City("Sarajevo", "سراييفو", "BA", 43.8563, 18.4131, "Europe/Sarajevo"),
        City("Singapore", "سنغافورة", "SG", 1.3521, 103.8198, "Asia/Singapore"),
        City("Stockholm", "ستوكهولم", "SE", 59.3293, 18.0686, "Europe/Stockholm"),
        City("Sydney", "سيدني", "AU", -33.8688, 151.2093, "Australia/Sydney"),
        City("Tashkent", "طشقند", "UZ", 41.2995, 69.2401, "Asia/Tashkent"),
        City("Tehran", "طهران", "IR", 35.6892, 51.3890, "Asia/Tehran"),
        City("Toronto", "تورنتو", "CA", 43.6532, -79.3832, "America/Toronto"),
        City("Tripoli", "طرابلس", "LY", 32.8872, 13.1913, "Africa/Tripoli"),
        City("Tunis", "تونس", "TN", 36.8065, 10.1815, "Africa/Tunis"),
        City("Vienna", "فيينا", "AT", 48.2082, 16.3738, "Europe/Vienna")
    )
}
