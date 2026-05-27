package com.raju.edutrack

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.telephony.SmsManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import java.net.URLEncoder
import java.util.Locale

object MessageSender {

    fun sendFeePaidMessage(
        context: Context,
        student: Student,
        amountPaid: Double,
        dueAmount: Double?,
        batch: Batch?
    ) {
        if (!AppSettings.feePaymentMessagesEnabled.value) {
            return
        }
        val message = applyTemplate(
            template = AppSettings.feePaidMessageTemplate.value,
            student = student,
            amountPaid = amountPaid,
            dueAmount = dueAmount,
            batch = batch
        )
        sendToStudentContacts(context, student, message)
    }

    fun sendBatchMessage(
        context: Context,
        batch: Batch,
        students: List<Student>
    ) {
        if (!AppSettings.batchReminderMessagesEnabled.value) {
            return
        }
        val template = batch.messageTemplate
            .ifBlank { AppSettings.batchMessageTemplate.value }
        students.forEach { student ->
            val message = applyTemplate(
                template = template,
                student = student,
                amountPaid = null,
                dueAmount = null,
                batch = batch
            )
            sendToStudentContacts(context, student, message)
        }
    }

    private fun sendToStudentContacts(
        context: Context,
        student: Student,
        message: String
    ) {
        student.contacts
            .map { contact -> contact.number.trim() }
            .filter { number -> number.isNotBlank() }
            .forEach { number ->
                when (AppSettings.messageChannel.value) {
                    MessageChannel.SMS -> sendSms(context, number, message)
                    MessageChannel.WHATSAPP -> openWhatsApp(context, number, message)
                    MessageChannel.BOTH -> {
                        sendSms(context, number, message)
                        openWhatsApp(context, number, message)
                    }
                }
            }
    }

    private fun sendSms(
        context: Context,
        number: String,
        message: String
    ) {
        if (AppSettings.messageSendMode.value == MessageSendMode.AUTO) {
            if (
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.SEND_SMS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                val manager = SmsManager.getDefault()
                val parts = manager.divideMessage(message)
                manager.sendMultipartTextMessage(number, null, parts, null, null)
            } else {
                openAppSettings(context)
            }
            return
        }

        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:$number")
            putExtra("sms_body", message)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private fun openWhatsApp(
        context: Context,
        number: String,
        message: String
    ) {
        if (AppSettings.messageSendMode.value == MessageSendMode.AUTO) {
            Toast.makeText(
                context,
                "WhatsApp does not allow auto-send. Review and tap send in WhatsApp.",
                Toast.LENGTH_LONG
            ).show()
        }
        val encoded = URLEncoder.encode(message, "UTF-8")
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://wa.me/${number.onlyDigits()}?text=$encoded")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private fun openAppSettings(context: Context) {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:${context.packageName}")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private fun applyTemplate(
        template: String,
        student: Student,
        amountPaid: Double?,
        dueAmount: Double?,
        batch: Batch?
    ): String {
        return template
            .replace("{studentName}", student.studentName)
            .replace("{amountPaid}", amountPaid.formatAmount())
            .replace("{dueAmount}", dueAmount.formatAmount())
            .replace("{batchName}", batch?.name ?: student.batchName.orEmpty())
            .replace("{time}", batch?.timeText.orEmpty())
    }

    private fun Double?.formatAmount(): String {
        if (this == null) {
            return ""
        }
        return "${AppSettings.currencySymbol.value}${String.format(Locale.US, "%.2f", this)}"
    }

    private fun String.onlyDigits(): String {
        return filter { character -> character.isDigit() }
    }
}
