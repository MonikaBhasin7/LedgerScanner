package com.example.ledgerscanner.base.utils

import android.text.format.DateUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

// ===========================================================================
// 👤 Author: Monika Bhasin
// 📅 Created: 01/01/26
// ===========================================================================


object DateAndTimeUtils {

    // Date Format Patterns
    private const val PATTERN_FULL_DATE = "MMM dd, yyyy"
    private const val PATTERN_FULL_DATE_TIME = "MMM dd, yyyy hh:mm a"
    private const val PATTERN_FULL_DATE_TIME_24 = "MMM dd, yyyy HH:mm"
    private const val PATTERN_SHORT_DATE = "MMM dd"
    private const val PATTERN_SHORT_DATE_YEAR = "dd/MM/yyyy"
    private const val PATTERN_TIME_12 = "hh:mm a"
    private const val PATTERN_TIME_24 = "HH:mm"
    private const val PATTERN_DAY_MONTH = "dd MMM"
    private const val PATTERN_MONTH_YEAR = "MMM yyyy"
    private const val PATTERN_ISO_8601 = "yyyy-MM-dd'T'HH:mm:ss'Z'"
    private const val PATTERN_FILE_NAME = "yyyyMMdd_HHmmss"

    // ===================================================================
    // Time Ago Functions
    // ===================================================================

    /**
     * Format timestamp as relative time (e.g., "2 mins ago", "3 hours ago")
     */
    fun formatTimeAgo(timestamp: Long): String {
        val diff = System.currentTimeMillis() - timestamp
        return when {
            diff < 60_000 -> "just now"
            diff < 3600_000 -> "${diff / 60_000} mins ago"
            diff < 86400_000 -> "${diff / 3600_000} hours ago"
            diff < 172800_000 -> "yesterday" // 2 days
            diff < 604800_000 -> "${diff / 86400_000} days ago" // 7 days
            else -> formatDate(timestamp, PATTERN_SHORT_DATE)
        }
    }

    /**
     * Format timestamp as detailed relative time
     */
    fun formatDetailedTimeAgo(timestamp: Long): String {
        val diff = System.currentTimeMillis() - timestamp
        return when {
            diff < 60_000 -> "just now"
            diff < 3600_000 -> {
                val mins = diff / 60_000
                "${mins} ${if (mins == 1L) "minute" else "minutes"} ago"
            }

            diff < 86400_000 -> {
                val hours = diff / 3600_000
                "${hours} ${if (hours == 1L) "hour" else "hours"} ago"
            }

            diff < 604800_000 -> {
                val days = diff / 86400_000
                "${days} ${if (days == 1L) "day" else "days"} ago"
            }

            diff < 2592000_000 -> {
                val weeks = diff / 604800_000
                "${weeks} ${if (weeks == 1L) "week" else "weeks"} ago"
            }

            else -> formatDate(timestamp, PATTERN_FULL_DATE)
        }
    }

    /**
     * Smart time formatting: shows time if today, date if recent, full date if old
     */
    fun formatSmartTime(timestamp: Long): String {
        return when {
            isToday(timestamp) -> "Today ${formatTime(timestamp)}"
            isYesterday(timestamp) -> "Yesterday ${formatTime(timestamp)}"
            isThisWeek(timestamp) -> "${getDayOfWeek(timestamp)} ${formatTime(timestamp)}"
            isThisYear(timestamp) -> formatDate(timestamp, PATTERN_DAY_MONTH)
            else -> formatDate(timestamp, PATTERN_FULL_DATE)
        }
    }

    // ===================================================================
    // Date Formatting Functions
    // ===================================================================

    /**
     * Format timestamp with custom pattern
     */
    fun formatDate(timestamp: Long, pattern: String = PATTERN_FULL_DATE): String {
        return try {
            val sdf = SimpleDateFormat(pattern, Locale.getDefault())
            sdf.format(Date(timestamp))
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Format date as "MMM dd, yyyy" (e.g., "Dec 31, 2024")
     */
    fun formatFullDate(timestamp: Long): String {
        return formatDate(timestamp, PATTERN_FULL_DATE)
    }

    /**
     * Format date as "MMM dd" (e.g., "Dec 31")
     */
    fun formatShortDate(timestamp: Long): String {
        return formatDate(timestamp, PATTERN_SHORT_DATE)
    }

    /**
     * Format date as "dd/MM/yyyy" (e.g., "31/12/2024")
     */
    fun formatNumericDate(timestamp: Long): String {
        return formatDate(timestamp, PATTERN_SHORT_DATE_YEAR)
    }

    /**
     * Format time as "hh:mm a" (e.g., "02:30 PM")
     */
    fun formatTime(timestamp: Long, use24Hour: Boolean = false): String {
        val pattern = if (use24Hour) PATTERN_TIME_24 else PATTERN_TIME_12
        return formatDate(timestamp, pattern)
    }

    /**
     * Format date and time as "MMM dd, yyyy hh:mm a"
     */
    fun formatDateTime(timestamp: Long, use24Hour: Boolean = false): String {
        val pattern = if (use24Hour) PATTERN_FULL_DATE_TIME_24 else PATTERN_FULL_DATE_TIME
        return formatDate(timestamp, pattern)
    }

    /**
     * Format for file names (e.g., "20241231_143025")
     */
    fun formatForFileName(timestamp: Long = System.currentTimeMillis()): String {
        return formatDate(timestamp, PATTERN_FILE_NAME)
    }

    /**
     * Format as ISO 8601 (e.g., "2024-12-31T14:30:25Z")
     */
    fun formatISO8601(timestamp: Long): String {
        val sdf = SimpleDateFormat(PATTERN_ISO_8601, Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date(timestamp))
    }

    // ===================================================================
    // Date Comparison Functions
    // ===================================================================

    /**
     * Check if timestamp is today
     */
    fun isToday(timestamp: Long): Boolean {
        return DateUtils.isToday(timestamp)
    }

    /**
     * Check if timestamp is yesterday
     */
    fun isYesterday(timestamp: Long): Boolean {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        return isSameDay(timestamp, calendar.timeInMillis)
    }

    /**
     * Check if timestamp is this week
     */
    fun isThisWeek(timestamp: Long): Boolean {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply { timeInMillis = timestamp }
        return now.get(Calendar.WEEK_OF_YEAR) == target.get(Calendar.WEEK_OF_YEAR) &&
                now.get(Calendar.YEAR) == target.get(Calendar.YEAR)
    }

    /**
     * Check if timestamp is this month
     */
    fun isThisMonth(timestamp: Long): Boolean {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply { timeInMillis = timestamp }
        return now.get(Calendar.MONTH) == target.get(Calendar.MONTH) &&
                now.get(Calendar.YEAR) == target.get(Calendar.YEAR)
    }

    /**
     * Check if timestamp is this year
     */
    fun isThisYear(timestamp: Long): Boolean {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply { timeInMillis = timestamp }
        return now.get(Calendar.YEAR) == target.get(Calendar.YEAR)
    }

    /**
     * Check if two timestamps are on the same day
     */
    fun isSameDay(timestamp1: Long, timestamp2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = timestamp1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = timestamp2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    /**
     * Check if timestamp is within last N minutes
     */
    fun isWithinLastMinutes(timestamp: Long, minutes: Int): Boolean {
        val diff = System.currentTimeMillis() - timestamp
        return diff < TimeUnit.MINUTES.toMillis(minutes.toLong())
    }

    /**
     * Check if timestamp is within last N hours
     */
    fun isWithinLastHours(timestamp: Long, hours: Int): Boolean {
        val diff = System.currentTimeMillis() - timestamp
        return diff < TimeUnit.HOURS.toMillis(hours.toLong())
    }

    /**
     * Check if timestamp is within last N days
     */
    fun isWithinLastDays(timestamp: Long, days: Int): Boolean {
        val diff = System.currentTimeMillis() - timestamp
        return diff < TimeUnit.DAYS.toMillis(days.toLong())
    }

    // ===================================================================
    // Date Calculation Functions
    // ===================================================================

    /**
     * Get start of day (00:00:00)
     */
    fun getStartOfDay(timestamp: Long = System.currentTimeMillis()): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    /**
     * Get end of day (23:59:59)
     */
    fun getEndOfDay(timestamp: Long = System.currentTimeMillis()): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return calendar.timeInMillis
    }

    /**
     * Get start of week
     */
    fun getStartOfWeek(timestamp: Long = System.currentTimeMillis()): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    /**
     * Get end of week
     */
    fun getEndOfWeek(timestamp: Long = System.currentTimeMillis()): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = getStartOfWeek(timestamp)
            add(Calendar.DAY_OF_YEAR, 6)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return calendar.timeInMillis
    }

    /**
     * Get start of month
     */
    fun getStartOfMonth(timestamp: Long = System.currentTimeMillis()): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    /**
     * Get end of month
     */
    fun getEndOfMonth(timestamp: Long = System.currentTimeMillis()): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return calendar.timeInMillis
    }

    /**
     * Add days to timestamp
     */
    fun addDays(timestamp: Long, days: Int): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = timestamp
            add(Calendar.DAY_OF_YEAR, days)
        }
        return calendar.timeInMillis
    }

    /**
     * Add hours to timestamp
     */
    fun addHours(timestamp: Long, hours: Int): Long {
        return timestamp + TimeUnit.HOURS.toMillis(hours.toLong())
    }

    /**
     * Add minutes to timestamp
     */
    fun addMinutes(timestamp: Long, minutes: Int): Long {
        return timestamp + TimeUnit.MINUTES.toMillis(minutes.toLong())
    }

    /**
     * Get difference in days between two timestamps
     */
    fun getDaysDifference(startTimestamp: Long, endTimestamp: Long): Long {
        val diff = endTimestamp - startTimestamp
        return TimeUnit.MILLISECONDS.toDays(diff)
    }

    /**
     * Get difference in hours between two timestamps
     */
    fun getHoursDifference(startTimestamp: Long, endTimestamp: Long): Long {
        val diff = endTimestamp - startTimestamp
        return TimeUnit.MILLISECONDS.toHours(diff)
    }

    /**
     * Get difference in minutes between two timestamps
     */
    fun getMinutesDifference(startTimestamp: Long, endTimestamp: Long): Long {
        val diff = endTimestamp - startTimestamp
        return TimeUnit.MILLISECONDS.toMinutes(diff)
    }

    // ===================================================================
    // Helper Functions
    // ===================================================================

    /**
     * Get day of week as string (e.g., "Monday", "Tuesday")
     */
    fun getDayOfWeek(timestamp: Long): String {
        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        return when (dayOfWeek) {
            Calendar.SUNDAY -> "Sunday"
            Calendar.MONDAY -> "Monday"
            Calendar.TUESDAY -> "Tuesday"
            Calendar.WEDNESDAY -> "Wednesday"
            Calendar.THURSDAY -> "Thursday"
            Calendar.FRIDAY -> "Friday"
            Calendar.SATURDAY -> "Saturday"
            else -> ""
        }
    }

    /**
     * Get short day of week (e.g., "Mon", "Tue")
     */
    fun getShortDayOfWeek(timestamp: Long): String {
        return formatDate(timestamp, "EEE")
    }

    /**
     * Get month name (e.g., "January", "February")
     */
    fun getMonthName(timestamp: Long): String {
        return formatDate(timestamp, "MMMM")
    }

    /**
     * Get short month name (e.g., "Jan", "Feb")
     */
    fun getShortMonthName(timestamp: Long): String {
        return formatDate(timestamp, "MMM")
    }

    /**
     * Parse date string to timestamp
     */
    fun parseDate(dateString: String, pattern: String = PATTERN_FULL_DATE): Long? {
        return try {
            val sdf = SimpleDateFormat(pattern, Locale.getDefault())
            sdf.parse(dateString)?.time
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get current timestamp
     */
    fun getCurrentTimestamp(): Long = System.currentTimeMillis()

    /**
     * Get timestamp for specific date
     */
    fun getTimestamp(year: Int, month: Int, day: Int, hour: Int = 0, minute: Int = 0): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1) // Month is 0-indexed
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    /**
     * Format duration in milliseconds to readable string (e.g., "2h 30m")
     */
    fun formatDuration(durationMillis: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(durationMillis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMillis) % 60

        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m ${seconds}s"
            else -> "${seconds}s"
        }
    }

    /**
     * Get age from birthdate timestamp
     */
    fun getAge(birthdateTimestamp: Long): Int {
        val birthdate = Calendar.getInstance().apply { timeInMillis = birthdateTimestamp }
        val today = Calendar.getInstance()

        var age = today.get(Calendar.YEAR) - birthdate.get(Calendar.YEAR)

        if (today.get(Calendar.DAY_OF_YEAR) < birthdate.get(Calendar.DAY_OF_YEAR)) {
            age--
        }

        return age
    }
}