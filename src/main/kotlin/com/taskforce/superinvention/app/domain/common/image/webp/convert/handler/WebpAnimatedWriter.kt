package com.taskforce.superinvention.app.domain.common.image.webp.convert.handler

import java.io.File

class WebpAnimatedWriter(
    private var q: Int = -1,
    private var m: Int = -1,
    private var mixed: Boolean = true,
    private var lossy: Boolean = true,
    private var multiThread: Boolean = true,
) {

    companion object {
        val DEFAULT: WebpAnimatedWriter = WebpAnimatedWriter()
        val LOSSLESS_COMPRESSION: WebpAnimatedWriter = DEFAULT.withLossless()
    }

    private val handler = Gif2WebpHandler()

    fun withQ(q: Int): WebpAnimatedWriter {
        require(q >= 0) { "q must be between 0 and 100" }
        require(q <= 100) { "q must be between 0 and 100" }
        return WebpAnimatedWriter(q, m, lossy)
    }

    fun withM(m: Int): WebpAnimatedWriter {
        require(m >= 0) { "m must be between 0 and 6" }
        require(m <= 6) { "m must be between 0 and 6" }
        return WebpAnimatedWriter(q, m, lossy)
    }

    fun withLossless(): WebpAnimatedWriter {
        return WebpAnimatedWriter(q, m, false)
    }

    fun writeAsByteArray(gifImage: File): ByteArray {
        return handler.convert(gifImage, q, m, lossy, mixed, multiThread)
    }
}
