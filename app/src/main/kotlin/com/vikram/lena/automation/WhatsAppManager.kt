package com.vikram.lena.automation

import android.content.Context
import android.content.Intent
import android.net.Uri

class WhatsAppManager(private val context: Context) {

    private val contactManager = ContactManager(context)

    /** Send WhatsApp message to contact */
    fun sendMessage(contactName: String, message: String): String {
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
                // Try without package restriction
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
                    "WhatsApp khol rahi hu $contactName ke liye! Send button daba dena! 💬"
                } catch (e2: Exception) {
                    "WhatsApp nahi khul raha yaar! Check kar installed hai ya nahi."
                }
            }
        } else {
            "Yaar '$contactName' ka number nahi mila contacts mein!"
        }
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

    /** Make WhatsApp voice call */
    fun makeWhatsAppCall(contactName: String): String {
        val number = contactManager.findNumber(contactName)

        return if (number != null) {
            try {
                val cleanNumber = number.replace("[^\\d+]".toRegex(), "")
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse(
                        "https://api.whatsapp.com/send?phone=$cleanNumber"
                    )
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    setPackage("com.whatsapp")
                }
                context.startActivity(intent)
                "WhatsApp pe $contactName ka chat khol rahi hu. " +
                    "Wahan se call kar lena yaar! 📞"
            } catch (e: Exception) {
                "WhatsApp call nahi laga payi yaar!"
            }
        } else {
            "Yaar '$contactName' ka number nahi mila!"
        }
    }
}