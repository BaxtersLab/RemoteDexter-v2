package com.rd.remotedexter.mobile

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.view.Surface
import java.nio.ByteBuffer

class VideoEncoder(
    private val width: Int,
    private val height: Int,
    private val onFrameEncoded: (ByteArray) -> Unit
) {
    private lateinit var mediaCodec: MediaCodec
    val inputSurface: Surface
    private var isEncoding = false

    init {
        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
            setInteger(MediaFormat.KEY_BIT_RATE, 4_000_000) // 4 Mbps
            setInteger(MediaFormat.KEY_FRAME_RATE, 30)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1) // 1 second
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
        }

        mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        inputSurface = MediaCodec.createPersistentInputSurface()
        mediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        mediaCodec.setInputSurface(inputSurface)
    }

    fun start() {
        if (isEncoding) return

        isEncoding = true
        mediaCodec.start()

        // Start encoding thread
        Thread {
            encodeLoop()
        }.start()
    }

    private fun encodeLoop() {
        val bufferInfo = MediaCodec.BufferInfo()
        val timeoutUs = 10000L

        while (isEncoding) {
            val outputBufferId = mediaCodec.dequeueOutputBuffer(bufferInfo, timeoutUs)
            when {
                outputBufferId >= 0 -> {
                    val outputBuffer = mediaCodec.getOutputBuffer(outputBufferId)
                    if (outputBuffer != null) {
                        val encodedData = ByteArray(bufferInfo.size)
                        outputBuffer.get(encodedData)
                        onFrameEncoded(encodedData)
                    }
                    mediaCodec.releaseOutputBuffer(outputBufferId, false)
                }
                outputBufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    println("Encoder output format changed: ${mediaCodec.outputFormat}")
                }
            }
        }
    }

    fun stop() {
        isEncoding = false
        mediaCodec.stop()
        mediaCodec.release()
    }
}