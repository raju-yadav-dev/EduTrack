package com.raju.edutrack

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import org.json.JSONArray
import org.json.JSONObject

enum class AutoBatchMode {
    NONE,
    CLASS,
    CLASS_SCHOOL
}

enum class MessageChannel {
    SMS,
    WHATSAPP,
    BOTH
}

enum class MessageSendMode {
    COMPOSER,
    AUTO
}

data class ClassFeeEntry(
    val className: String,
    val amountText: String
)

object AppSettings {

    val countFeeFromJoinDate = mutableStateOf(true)
    val defaultFeeDueAmountText = mutableStateOf("")
    val defaultFeeDueDaysText = mutableStateOf("")
    val currencySymbol = mutableStateOf("₹")
    val autoBatchMode = mutableStateOf(AutoBatchMode.CLASS)
    val defaultBatchName = mutableStateOf("")
    val autoAdvanceFeeDueDate = mutableStateOf(true)
    val feePaymentMessagesEnabled = mutableStateOf(false)
    val batchReminderMessagesEnabled = mutableStateOf(false)
    val messageChannel = mutableStateOf(MessageChannel.SMS)
    val messageSendMode = mutableStateOf(MessageSendMode.COMPOSER)
    val feePaidMessageTemplate = mutableStateOf(
        "Hi, fee payment received for {studentName}. Amount paid: {amountPaid}. Pending due: {dueAmount}."
    )
    val batchMessageTemplate = mutableStateOf(
        "Hi {studentName}, your {batchName} batch is scheduled at {time}."
    )
    val classFeeEntries = mutableStateListOf<ClassFeeEntry>()
    val customClassOptions = mutableStateListOf<String>()

    private val baseClassOptions = listOf(
        "IV",
        "V",
        "VI",
        "VII",
        "VIII",
        "IX",
        "X",
        "XI",
        "XII",
        "1st Year",
        "2nd Year",
        "3rd Year",
        "4th Year"
    )

    fun parseDefaultFeeDueAmount(): Double? {
        val value = defaultFeeDueAmountText.value
            .trim()
            .toDoubleOrNull()
        return value?.takeIf { amount -> amount > 0.0 }
    }

    fun parseDefaultFeeDueDays(): Int? {
        val value = defaultFeeDueDaysText.value
            .trim()
            .toIntOrNull()
        return value?.takeIf { days -> days > 0 }
    }

    fun load(context: Context) {
        val prefs = context.getSharedPreferences(
            "app_settings",
            Context.MODE_PRIVATE
        )
        countFeeFromJoinDate.value =
            prefs.getBoolean("countFeeFromJoinDate", true)
        defaultFeeDueAmountText.value =
            prefs.getString("defaultFeeDueAmountText", "") ?: ""
        defaultFeeDueDaysText.value =
            prefs.getString("defaultFeeDueDaysText", "") ?: ""
        currencySymbol.value =
            prefs.getString("currencySymbol", "₹") ?: "₹"
        defaultBatchName.value =
            prefs.getString("defaultBatchName", "") ?: ""
        autoAdvanceFeeDueDate.value =
            prefs.getBoolean("autoAdvanceFeeDueDate", true)
        feePaymentMessagesEnabled.value =
            prefs.getBoolean("feePaymentMessagesEnabled", false)
        batchReminderMessagesEnabled.value =
            prefs.getBoolean("batchReminderMessagesEnabled", false)
        feePaidMessageTemplate.value =
            prefs.getString(
                "feePaidMessageTemplate",
                feePaidMessageTemplate.value
            ) ?: feePaidMessageTemplate.value
        batchMessageTemplate.value =
            prefs.getString(
                "batchMessageTemplate",
                batchMessageTemplate.value
            ) ?: batchMessageTemplate.value

        val modeValue =
            prefs.getString("autoBatchMode", AutoBatchMode.CLASS.name)
                ?: AutoBatchMode.CLASS.name
        autoBatchMode.value =
            AutoBatchMode.values()
                .firstOrNull { mode -> mode.name == modeValue }
                ?: AutoBatchMode.NONE

        val channelValue =
            prefs.getString("messageChannel", MessageChannel.SMS.name)
                ?: MessageChannel.SMS.name
        messageChannel.value =
            MessageChannel.values()
                .firstOrNull { channel -> channel.name == channelValue }
                ?: MessageChannel.SMS

        val sendModeValue =
            prefs.getString("messageSendMode", MessageSendMode.COMPOSER.name)
                ?: MessageSendMode.COMPOSER.name
        messageSendMode.value =
            MessageSendMode.values()
                .firstOrNull { sendMode -> sendMode.name == sendModeValue }
                ?: MessageSendMode.COMPOSER

        classFeeEntries.clear()
        val rawFees = prefs.getString("classFees", "[]") ?: "[]"
        val array = JSONArray(rawFees)
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            classFeeEntries.add(
                ClassFeeEntry(
                    className = item.optString("className"),
                    amountText = item.optString("amountText")
                )
            )
        }

        customClassOptions.clear()
        val rawClasses = prefs.getString("customClassOptions", "[]") ?: "[]"
        val classArray = JSONArray(rawClasses)
        for (index in 0 until classArray.length()) {
            val value = classArray.optString(index)
            if (value.isNotBlank()) {
                customClassOptions.add(value)
            }
        }
    }

    fun save(context: Context) {
        val prefs = context.getSharedPreferences(
            "app_settings",
            Context.MODE_PRIVATE
        )
        val feesArray = JSONArray()
        classFeeEntries.forEach { entry ->
            val item = JSONObject()
            item.put("className", entry.className)
            item.put("amountText", entry.amountText)
            feesArray.put(item)
        }
        val classArray = JSONArray()
        customClassOptions.forEach { option ->
            classArray.put(option)
        }
        prefs.edit()
            .putBoolean(
                "countFeeFromJoinDate",
                countFeeFromJoinDate.value
            )
            .putString(
                "defaultFeeDueAmountText",
                defaultFeeDueAmountText.value
            )
            .putString(
                "defaultFeeDueDaysText",
                defaultFeeDueDaysText.value
            )
            .putString(
                "currencySymbol",
                currencySymbol.value
            )
            .putString(
                "autoBatchMode",
                autoBatchMode.value.name
            )
            .putString(
                "defaultBatchName",
                defaultBatchName.value
            )
            .putBoolean(
                "autoAdvanceFeeDueDate",
                autoAdvanceFeeDueDate.value
            )
            .putBoolean(
                "feePaymentMessagesEnabled",
                feePaymentMessagesEnabled.value
            )
            .putBoolean(
                "batchReminderMessagesEnabled",
                batchReminderMessagesEnabled.value
            )
            .putString(
                "messageChannel",
                messageChannel.value.name
            )
            .putString(
                "messageSendMode",
                messageSendMode.value.name
            )
            .putString(
                "feePaidMessageTemplate",
                feePaidMessageTemplate.value
            )
            .putString(
                "batchMessageTemplate",
                batchMessageTemplate.value
            )
            .putString(
                "classFees",
                feesArray.toString()
            )
            .putString(
                "customClassOptions",
                classArray.toString()
            )
            .apply()
    }

    fun parseClassFeeAmount(className: String): Double? {
        val entry = classFeeEntries
            .firstOrNull { fee ->
                fee.className.equals(className, ignoreCase = true)
            }
        return entry?.amountText
            ?.trim()
            ?.toDoubleOrNull()
            ?.takeIf { amount -> amount > 0.0 }
    }

    fun getClassOptions(): List<String> {
        return (baseClassOptions + customClassOptions)
            .map { option -> option.trim() }
            .filter { option -> option.isNotBlank() }
            .distinctBy { option -> option.lowercase() }
    }

}
