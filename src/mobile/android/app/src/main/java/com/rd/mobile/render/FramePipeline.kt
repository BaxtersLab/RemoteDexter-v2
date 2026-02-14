package com.rd.mobile.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.os.Handler
import android.os.HandlerThread
import android.view.SurfaceHolder
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max
import kotlin.math.min

data class DecodedFrame(
    val width: Int,
    val height: Int,
    val rgba8888: ByteArray,
    val timestampNs: Long = System.nanoTime()
)

class SurfaceFramePipeline(
    private val surfaceHolder: SurfaceHolder,
    private val fpsCap: Int = 60
) {
    private var renderThread: HandlerThread? = null
    private var renderHandler: Handler? = null
    private val started = AtomicBoolean(false)
    private val renderPosted = AtomicBoolean(false)
    private val pendingFrameLock = Any()

    @Volatile
    private var pendingFrame: DecodedFrame? = null

    @Volatile
    private var lastRenderNs = 0L

    private var reusableBitmap: Bitmap? = null

    fun start() {
        if (started.getAndSet(true)) return
        val thread = HandlerThread("SurfaceFramePipeline").apply { start() }
        renderThread = thread
        renderHandler = Handler(thread.looper)
    }

    fun stop() {
        if (!started.getAndSet(false)) return
        synchronized(pendingFrameLock) {
            pendingFrame = null
        }
        reusableBitmap?.recycle()
        reusableBitmap = null
        renderHandler = null
        renderThread?.quitSafely()
        renderThread = null
        renderPosted.set(false)
        lastRenderNs = 0L
    }

    fun submitFrame(frame: DecodedFrame) {
        if (!started.get()) return
        synchronized(pendingFrameLock) {
            pendingFrame = frame
        }
        scheduleRender()
    }

    private fun scheduleRender() {
        if (!started.get()) return
        if (!renderPosted.compareAndSet(false, true)) return
        val handler = renderHandler ?: return
        handler.post {
            renderPosted.set(false)
            renderLatestFrame()
            synchronized(pendingFrameLock) {
                if (pendingFrame != null) {
                    scheduleRender()
                }
            }
        }
    }

    private fun renderLatestFrame() {
        val frame = synchronized(pendingFrameLock) {
            val next = pendingFrame
            pendingFrame = null
            next
        } ?: return

        val frameIntervalNs = 1_000_000_000L / fpsCap
        val now = System.nanoTime()
        if (lastRenderNs != 0L && now - lastRenderNs < frameIntervalNs) {
            return
        }
        lastRenderNs = now

        val expectedSize = frame.width * frame.height * 4
        if (frame.rgba8888.size < expectedSize) {
            return
        }

        val bitmap = ensureBitmap(frame.width, frame.height)
        bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(frame.rgba8888, 0, expectedSize))

        val canvas = try {
            surfaceHolder.lockCanvas()
        } catch (_: Throwable) {
            null
        } ?: return

        try {
            drawFixedOrientation(canvas, bitmap)
        } finally {
            surfaceHolder.unlockCanvasAndPost(canvas)
        }
    }

    private fun ensureBitmap(width: Int, height: Int): Bitmap {
        val current = reusableBitmap
        if (current != null && current.width == width && current.height == height) {
            return current
        }
        current?.recycle()
        val created = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        reusableBitmap = created
        return created
    }

    private fun drawFixedOrientation(canvas: Canvas, frameBitmap: Bitmap) {
        canvas.drawColor(Color.BLACK)
        val target = fitCenter(
            canvas.width,
            canvas.height,
            frameBitmap.width,
            frameBitmap.height
        )
        canvas.drawBitmap(frameBitmap, null, target, null)
    }

    private fun fitCenter(
        viewportW: Int,
        viewportH: Int,
        frameW: Int,
        frameH: Int
    ): Rect {
        if (viewportW <= 0 || viewportH <= 0 || frameW <= 0 || frameH <= 0) {
            return Rect(0, 0, max(viewportW, 1), max(viewportH, 1))
        }

        val scale = min(viewportW.toFloat() / frameW.toFloat(), viewportH.toFloat() / frameH.toFloat())
        val scaledW = (frameW * scale).toInt().coerceAtLeast(1)
        val scaledH = (frameH * scale).toInt().coerceAtLeast(1)
        val left = (viewportW - scaledW) / 2
        val top = (viewportH - scaledH) / 2
        return Rect(left, top, left + scaledW, top + scaledH)
    }
}

class RustDeskFrameBridge(
    private val onDecodedFrame: (DecodedFrame) -> Unit
) {
    fun submitDecodedFrame(width: Int, height: Int, rgba8888: ByteArray) {
        if (width <= 0 || height <= 0) return
        onDecodedFrame(
            DecodedFrame(
                width = width,
                height = height,
                rgba8888 = rgba8888
            )
        )
    }
}