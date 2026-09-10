package com.vikram.lena.automation

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.telecom.TelecomManager
import android.telephony.TelephonyManager

class PhoneCallManager(private val context: Context) {

    /** Contact name se number find karke call karo */
    fun makeCall(contactName: String): String {
        val number = findContactNumber(contactName)
        return if (number != null) {
            val callIntent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$number")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            try {
                context.startActivity(callIntent)
                "Arre Vikram, $contactName ko call laga rahi hu... 📞"
            } catch (e: Exception) {
                "Yaar call nahi laga payi, permission check kar!"
            }
        } else {
            "Yaar '$contactName' naam ka contact nahi mila. Sahi naam bata!"
        }
    }

    /** Incoming call pickup karo */
    fun answerCall(): String {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val telecomManager = context.getSystemService(
                    Context.TELECOM_SERVICE
                ) as TelecomManager
                telecomManager.acceptRingingCall()
            } else {
                // Older devices
                val audioManager = context.getSystemService(
                    Context.AUDIO_SERVICE
                ) as AudioManager
                audioManager.mode = AudioManager.MODE_IN_CALL

                val intent = Intent(Intent.ACTION_MEDIA_BUTTON)
                context.sendBroadcast(intent)
            }
            "Call utha di yaar! Baat kar ab! 📱"
        } catch (e: Exception) {
            "Yaar call uthane mein problem aa gayi: ${e.message}"
        }
    }

    /** Incoming call reject karo */
    fun rejectCall(): String {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val telecomManager = context.getSystemService(
                    Context.TELECOM_SERVICE
                ) as TelecomManager
                telecomManager.endCall()
            }
            "Call reject kar di! 🚫"
        } catch (e: Exception) {
            "Reject karne mein problem aa gayi yaar!"
        }
    }

    /** Contact name se phone number search karo */
    fun findContactNumber(name: String): String? {
        try {
            val cursor: Cursor? = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
                arrayOf("%$name%"),
                null
            )

            cursor?.use {
                if (it.moveToFirst()) {
                    val numberIndex = it.getColumnIndex(
                        ContactsContract.CommonDataKinds.Phone.NUMBER
                    )
                    return it.getString(numberIndex)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    /** Search contact and return info */
    fun searchContact(name: String): String {
        val number = findContactNumber(name)
        return if (number != null) {
            "Mil gaya yaar! $name ka number hai: $number 📋"
        } else {
            "Nahi mila '$name' naam ka koi contact. Sahi naam bata!"
        }
    }
}