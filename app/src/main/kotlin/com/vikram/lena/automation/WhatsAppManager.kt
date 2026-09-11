package com.vikram.lena.automation

import android.content.Context
import android.content.Intent
import android.net.Uri

class WhatsAppManager(private val context: Context) {

    private val contactManager = ContactManager(context)

    /** Send WhatsApp message */
    fun sendWhatsAppMessage(contactName: String, message: String): String {
        val number = contactManager.findNumber(contactName)

        return if (number != null) {
            try {
                val cleanNumber = number.replace("[^\\d+]".toRegex(), "")
                val formattedNumber = if (cleanNumber.startsWith("+"))
                    cleanNumber
                else if (cleanNumber.length == 10)
                    "+91$cleanNumber"
                else
                    cleanNumber

                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse(
                        "https://api.whatsapp.com/send?phone=$formattedNumber" +
                                "&text=${Uri.encode(message)}"
                    )
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    setPackage("com.whatsapp")
                }
                context.startActivity(intent)
                "WhatsApp pe $contactName ko message bhej rahi hu! 💬"
            } catch (e: Exception) {
                try {
                    val cleanNumber = number.replace("[^\\d+]".toRegex(), "")
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        data = Uri.parse(
                            "https://api.whatsapp.com/send?phone=$cleanNumber" +
                                    "&text=${Uri.encode(message)}"
                        )
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    "WhatsApp khol rahi hu $contactName ke liye! 💬"
                } catch (e2: Exception) {
                    "WhatsApp nahi khul raha yaar! Check kar installed hai ya nahi."
                }
            }
        } else {
            "Yaar '$contactName' ka number nahi mila contacts mein!"
        }
    }

    /** Alias for backward compatibility */
    fun sendMessage(contactName: String, message: String): String {
        return sendWhatsAppMessage(contactName, message)
    }

    /** Open WhatsApp */
    fun openWhatsApp(): String {
        return try {
            val intent = context.packageManager
                .getLaunchIntentForPackage("com.whatsapp")
            if (intent != null) {
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
                "WhatsApp khol rahi hu! 💬"
            } else {
                "WhatsApp install nahi hai phone mein!"
            }
        } catch (e: Exception) {
            "WhatsApp kholne mein problem aa gayi!"
        }
    }
}