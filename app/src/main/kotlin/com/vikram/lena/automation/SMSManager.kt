package com.vikram.lena.automation

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.telephony.SmsManager

class SMSManager(private val context: Context) {

    private val phoneCallManager = PhoneCallManager(context)

    /** SMS bhejo contact name se */
    fun sendSMS(contactName: String, message: String): String {
        val number = phoneCallManager.findContactNumber(contactName)
        
        return if (number != null) {
            try {
                val smsManager = SmsManager.getDefault()
                smsManager.sendTextMessage(number, null, message, null, null)
                "Done yaar! $contactName ko SMS bhej diya: \"$message\" ✉️"
            } catch (e: Exception) {
                "SMS bhejne mein problem aa gayi: ${e.message}"
            }
        } else {
            "Yaar '$contactName' ka number nahi mila!"
        }
    }

    /** Last N SMS padho */
    fun readRecentSMS(count: Int = 5): String {
        val smsList = mutableListOf<String>()
        
        try {
            val cursor: Cursor? = context.contentResolver.query(
                Uri.parse("content://sms/inbox"),
                arrayOf("address", "body", "date"),
                null, null,
                "date DESC"
            )

            cursor?.use {
                var i = 0
                while (it.moveToNext() && i < count) {
                    val address = it.getString(it.getColumnIndexOrThrow("address"))
                    val body = it.getString(it.getColumnIndexOrThrow("body"))
                    val shortBody = if (body.length > 50) 
                        body.substring(0, 50) + "..." else body
                    smsList.add("📩 $address: $shortBody")
                    i++
                }
            }
        } catch (e: Exception) {
            return "SMS padhne mein problem aa gayi yaar!"
        }

        return if (smsList.isNotEmpty()) {
            "Tere last ${smsList.size} messages:\n\n${smsList.joinToString("\n\n")}"
        } else {
            "Koi SMS nahi hai inbox mein!"
        }
    }
}