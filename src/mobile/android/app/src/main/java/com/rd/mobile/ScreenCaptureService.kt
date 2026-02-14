package com.rd.remotedexter.mobile

import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import java.nio.ByteBuffer

class ScreenCaptureService(
    private val context: Context,
    private val mediaProjectionManager: MediaProjectionManager,
    private val data: Intent
) {
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var videoEncoder: VideoEncoder? = null
    private var isStreaming = false
    private val handlerThread = HandlerThread("ScreenCapture").apply { start() }
    private val handler = Handler(handlerThread.looper)

    init {
        mediaProjection = mediaProjectionManager.getMediaProjection(Activity.RESULT_OK, data)
    }

    fun startStreaming() {
        if (isStreaming) return

        isStreaming = true
        println("Starting screen streaming")

        // Create virtual display
        val displayMetrics = context.resources.displayMetrics
        val width = 1280
        val height = 720

        videoEncoder = VideoEncoder(width, height) { encodedData ->
            // Send encoded data via transport
            // This would integrate with the transport layer
            println("Encoded frame: ${encodedData.size} bytes")
        }

        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "RemoteDexter",
            width,
            height,
            displayMetrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            videoEncoder?.inputSurface,
            null,
            handler
        )

        videoEncoder?.start()
    }

    fun stopStreaming() {
        if (!isStreaming) return

        isStreaming = false
        println("Stopping screen streaming")

        videoEncoder?.stop()
        virtualDisplay?.release()
        virtualDisplay = null
        videoEncoder = null
    }

    fun release() {
        stopStreaming()
        mediaProjection?.stop()
        handlerThread.quitSafely()
    }
}