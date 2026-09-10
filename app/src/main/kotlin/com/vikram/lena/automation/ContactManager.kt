package com.vikram.lena.automation

import android.content.Context
import android.database.Cursor
import android.provider.ContactsContract

data class ContactInfo(
    val name: String,
    val phoneNumber: String,
    val email: String = ""
)

class ContactManager(private val context: Context) {

    /** Search contacts by name */
    fun searchContacts(query: String): List<ContactInfo> {
        val contacts = mutableListOf<ContactInfo>()

        try {
            val cursor: Cursor? = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
                arrayOf("%$query%"),
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )

            cursor?.use {
                while (it.moveToNext()) {
                    val nameIndex = it.getColumnIndex(
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
                    )
                    val numberIndex = it.getColumnIndex(
                        ContactsContract.CommonDataKinds.Phone.NUMBER
                    )

                    if (nameIndex >= 0 && numberIndex >= 0) {
                        contacts.add(
                            ContactInfo(
                                name = it.getString(nameIndex) ?: "",
                                phoneNumber = it.getString(numberIndex) ?: ""
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Remove duplicates by name
        return contacts.distinctBy { it.name }
    }

    /** Find first matching contact number */
    fun findNumber(name: String): String? {
        val contacts = searchContacts(name)
        return contacts.firstOrNull()?.phoneNumber
    }

    /** Get contact info as readable string */
    fun getContactInfo(name: String): String {
        val contacts = searchContacts(name)

        return when {
            contacts.isEmpty() -> {
                "Yaar '$name' naam ka koi contact nahi mila! Sahi naam bata."
            }
            contacts.size == 1 -> {
                val c = contacts[0]
                "Mil gaya! ${c.name} ka number hai: ${c.phoneNumber} 📋"
            }
            else -> {
                val list = contacts.take(5).joinToString("\n") { c ->
                    "• ${c.name}: ${c.phoneNumber}"
                }
                "${contacts.size} contacts mile '$name' naam se:\n$list"
            }
        }
    }

    /** Get total contact count */
    fun getTotalContacts(): Int {
        var count = 0
        try {
            val cursor: Cursor? = context.contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                arrayOf(ContactsContract.Contacts._ID),
                null, null, null
            )
            count = cursor?.count ?: 0
            cursor?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return count
    }
}