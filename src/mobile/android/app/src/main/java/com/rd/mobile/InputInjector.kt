package com.rd.remotedexter.mobile

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.N)
class InputInjector : AccessibilityService() {

    private var currentX = 0f
    private var currentY = 0f
    private var screenWidth = 1080
    private var screenHeight = 1920

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Not used for input injection
    }

    override fun onInterrupt() {
        // Not used
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        println("InputInjector service connected")
    }

    fun setScreenSize(width: Int, height: Int) {
        screenWidth = width
        screenHeight = height
    }

    fun injectTouch(x: Int, y: Int, action: Int) {
        val screenX = x.toFloat()
        val screenY = y.toFloat()

        when (action) {
            ACTION_DOWN -> {
                currentX = screenX
                currentY = screenY
                performTap(screenX, screenY)
            }
            ACTION_MOVE -> {
                currentX = screenX
                currentY = screenY
                performSwipe(currentX, currentY, screenX, screenY)
            }
            ACTION_UP -> {
                // Touch up is handled by gesture completion
            }
        }
    }

    fun injectMouseMove(dx: Int, dy: Int) {
        // Convert deltas to absolute position
        currentX += dx.toFloat()
        currentY += dy.toFloat()

        // Clamp to screen bounds
        currentX = currentX.coerceIn(0f, screenWidth.toFloat())
        currentY = currentY.coerceIn(0f, screenHeight.toFloat())

        // For mouse, we simulate touch move
        performTap(currentX, currentY)
    }

    fun injectMouseClick(button: Int, action: Int) {
        when (action) {
            ACTION_DOWN -> {
                when (button) {
                    BUTTON_LEFT -> performTap(currentX, currentY)
                    BUTTON_RIGHT -> performLongPress(currentX, currentY)
                    BUTTON_MIDDLE -> performTap(currentX, currentY) // Middle click as tap
                }
            }
            ACTION_UP -> {
                // Click up handled by gesture completion
            }
        }
    }

    fun injectKeyEvent(keyCode: Int, action: Int) {
        // Note: Full keyboard injection requires additional permissions
        // This is a simplified implementation
        println("Key event: $keyCode, action: $action")
    }

    fun injectScroll(amount: Int) {
        // Simulate scroll as swipe gesture
        val scrollDistance = amount * 50f // Scale scroll amount
        performSwipe(currentX, currentY, currentX, currentY - scrollDistance)
    }

    private fun performTap(x: Float, y: Float) {
        val path = Path()
        path.moveTo(x, y)

        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 50))
            .build()

        dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                println("Tap gesture completed")
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                println("Tap gesture cancelled")
            }
        }, null)
    }

    private fun performLongPress(x: Float, y: Float) {
        val path = Path()
        path.moveTo(x, y)

        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 500)) // 500ms for long press
            .build()

        dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                println("Long press gesture completed")
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                println("Long press gesture cancelled")
            }
        }, null)
    }

    private fun performSwipe(startX: Float, startY: Float, endX: Float, endY: Float) {
        val path = Path()
        path.moveTo(startX, startY)
        path.lineTo(endX, endY)

        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 100))
            .build()

        dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                println("Swipe gesture completed")
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                println("Swipe gesture cancelled")
            }
        }, null)
    }

    companion object {
        const val ACTION_DOWN = 0
        const val ACTION_UP = 1
        const val ACTION_MOVE = 2

        const val BUTTON_LEFT = 0
        const val BUTTON_RIGHT = 1
        const val BUTTON_MIDDLE = 2
    }
}