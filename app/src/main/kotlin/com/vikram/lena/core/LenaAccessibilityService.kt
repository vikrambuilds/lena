package com.vikram.lena.core

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Build
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class LenaAccessibilityService : AccessibilityService() {

    companion object {
        var instance: LenaAccessibilityService? = null
            private set

        fun isServiceRunning(): Boolean = instance != null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event?.let {
            when (it.eventType) {
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                    val packageName = it.packageName?.toString() ?: ""
                    val className = it.className?.toString() ?: ""
                    onAppChanged(packageName, className)
                }
                AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED -> {
                    val text = it.text?.joinToString(" ") ?: ""
                    onNotificationReceived(it.packageName?.toString() ?: "", text)
                }
                else -> {}
            }
        }
    }

    override fun onInterrupt() {
        instance = null
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    // ========== PHONE CONTROL METHODS ==========

    /** Answer incoming call by UI node search or Swipe Gesture */
    fun answerCall(): Boolean {
        return try {
            val rootNode = rootInActiveWindow
            if (rootNode != null) {
                val answered = findAndClickNode(rootNode, "answer") || 
                               findAndClickNode(rootNode, "accept") ||
                               findAndClickNode(rootNode, "receive")
                if (answered) return true
            }
            // Fallback gesture
            performSwipeUp()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /** Reject/End call by UI node click */
    fun endCall(): Boolean {
        return try {
            val rootNode = rootInActiveWindow
            if (rootNode != null) {
                findAndClickNode(rootNode, "decline") || 
                findAndClickNode(rootNode, "reject") ||
                findAndClickNode(rootNode, "end") ||
                findAndClickNode(rootNode, "dismiss")
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    /** Go back */
    fun goBack(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_BACK)
    }

    /** Go home */
    fun goHome(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_HOME)
    }

    /** Open recent apps */
    fun openRecents(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_RECENTS)
    }

    /** Open notifications panel */
    fun openNotifications(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
    }

    /** Open quick settings */
    fun openQuickSettings(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
    }

    /** Take screenshot */
    fun takeScreenshot(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
        } else {
            false
        }
    }

    /** Lock screen */
    fun lockScreen(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
        } else {
            false
        }
    }

    /** Click on a UI element by text */
    fun clickByText(text: String): Boolean {
        val rootNode = rootInActiveWindow ?: return false
        return findAndClickNode(rootNode, text)
    }

    /** Type text into focused field */
    fun typeText(text: String): Boolean {
        val rootNode = rootInActiveWindow ?: return false
        val focusedNode = rootNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        
        return if (focusedNode != null) {
            val bundle = Bundle()
            bundle.putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                text
            )
            focusedNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, bundle)
        } else {
            false
        }
    }

    /** Perform swipe gesture */
    fun performSwipe(
        startX: Float, startY: Float,
        endX: Float, endY: Float,
        duration: Long = 300
    ): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false

        val path = Path()
        path.moveTo(startX, startY)
        path.lineTo(endX, endY)

        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, duration))
            .build()

        return dispatchGesture(gesture, null, null)
    }

    private fun performSwipeUp(): Boolean {
        val displayMetrics = resources.displayMetrics
        val centerX = displayMetrics.widthPixels / 2f
        val startY = displayMetrics.heightPixels * 0.8f
        val endY = displayMetrics.heightPixels * 0.2f
        return performSwipe(centerX, startY, centerX, endY)
    }

    private fun findAndClickNode(node: AccessibilityNodeInfo, text: String): Boolean {
        val nodeText = node.text?.toString()?.lowercase() ?: ""
        val nodeDesc = node.contentDescription?.toString()?.lowercase() ?: ""

        if (nodeText.contains(text.lowercase()) || 
            nodeDesc.contains(text.lowercase())) {
            return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (findAndClickNode(child, text)) return true
        }

        return false
    }

    private fun onAppChanged(packageName: String, className: String) {}

    private fun onNotificationReceived(packageName: String, text: String) {}
}