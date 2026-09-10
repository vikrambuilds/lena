package com.vikram.lena.automation

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

class AppLauncher(private val context: Context) {

    // Common apps mapping (Hinglish names → package names)
    private val appMap = mapOf(
        // Social Media
        "whatsapp" to "com.whatsapp",
        "instagram" to "com.instagram.android",
        "facebook" to "com.facebook.katana",
        "twitter" to "com.twitter.android",
        "x" to "com.twitter.android",
        "telegram" to "org.telegram.messenger",
        "snapchat" to "com.snapchat.android",
        "linkedin" to "com.linkedin.android",
        "reddit" to "com.reddit.frontpage",
        "pinterest" to "com.pinterest",
        "threads" to "com.instagram.barcelona",
        
        // Google
        "youtube" to "com.google.android.youtube",
        "gmail" to "com.google.android.gm",
        "google" to "com.google.android.googlequicksearchbox",
        "maps" to "com.google.android.apps.maps",
        "chrome" to "com.android.chrome",
        "google pay" to "com.google.android.apps.nbu.paisa.user",
        "gpay" to "com.google.android.apps.nbu.paisa.user",
        "photos" to "com.google.android.apps.photos",
        "drive" to "com.google.android.apps.docs",
        "calendar" to "com.google.android.calendar",
        "meet" to "com.google.android.apps.tachyon",
        "translate" to "com.google.android.apps.translate",
        
        // Entertainment
        "spotify" to "com.spotify.music",
        "netflix" to "com.netflix.mediaclient",
        "amazon prime" to "com.amazon.avod.thirdpartyclient",
        "hotstar" to "in.startv.hotstar",
        "jio cinema" to "com.jio.media.ondemand",
        "sony liv" to "com.sonyliv",
        
        // Utility
        "camera" to "com.android.camera",
        "calculator" to "com.google.android.calculator",
        "clock" to "com.google.android.deskclock",
        "settings" to "com.android.settings",
        "file manager" to "com.google.android.documentsui",
        "phone" to "com.android.dialer",
        "contacts" to "com.android.contacts",
        "messages" to "com.google.android.apps.messaging",
        "notes" to "com.google.android.keep",
        "gallery" to "com.google.android.apps.photos",
        
        // Payment
        "paytm" to "net.one97.paytm",
        "phonepe" to "com.phonepe.app",
        "amazon" to "in.amazon.mShop.android.shopping",
        "flipkart" to "com.flipkart.android",
        "swiggy" to "in.swiggy.android",
        "zomato" to "com.application.zomato",
        
        // Coding / Study
        "github" to "com.github.android",
        "stack overflow" to "com.stackexchange.marvin",
        "udemy" to "com.udemy.android",
        "unacademy" to "com.unacademy.unacademylearningapp",
        "byjus" to "com.byjus.thelearningapp"
    )

    fun openApp(appName: String): String {
        val normalizedName = appName.lowercase().trim()
        
        // First check our map
        val packageName = appMap.entries.find { (key, _) ->
            normalizedName.contains(key)
        }?.value

        if (packageName != null) {
            return launchByPackage(packageName, appName)
        }

        // If not in map, search installed apps
        return searchAndLaunch(normalizedName)
    }

    private fun launchByPackage(packageName: String, displayName: String): String {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
                "Chal $displayName khol rahi hu! 📱"
            } else {
                "$displayName install nahi hai yaar phone mein! 😅"
            }
        } catch (e: Exception) {
            "Arre yaar, $displayName kholne mein problem aa gayi!"
        }
    }

    private fun searchAndLaunch(appName: String): String {
        val pm = context.packageManager
        val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)

        for (appInfo in packages) {
            val label = pm.getApplicationLabel(appInfo).toString().lowercase()
            if (label.contains(appName) || appName.contains(label)) {
                return launchByPackage(appInfo.packageName, label)
            }
        }

        return "Yaar '$appName' nahi mila phone mein. Sahi naam bata!"
    }

    /** Get list of all installed apps */
    fun getInstalledApps(): List<String> {
        val pm = context.packageManager
        val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        return packages
            .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
            .map { pm.getApplicationLabel(it).toString() }
            .sorted()
    }
}