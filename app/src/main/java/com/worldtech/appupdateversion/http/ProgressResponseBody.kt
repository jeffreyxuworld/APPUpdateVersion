package com.worldtech.appupdateversion.http

import android.os.Handler
import okhttp3.ResponseBody
import android.os.Looper
import okhttp3.MediaType
import okio.*
import kotlin.Throws
import java.io.IOException

/**
 * 下载监听所用
 */
class ProgressResponseBody(
    private val responseBody: ResponseBody,
    private val progressListener: AbsFileProgressCallback
) : ResponseBody() {
    private var bufferedSource: BufferedSource? = null
    private val mUIHandler = Handler(Looper.getMainLooper())
    override fun contentType(): MediaType? {
        return responseBody.contentType()
    }

    override fun contentLength(): Long {
        return responseBody.contentLength()
    }

    override fun source(): BufferedSource {
        if (bufferedSource == null) {
            bufferedSource = Okio.buffer(source(responseBody.source()))
        }
        return bufferedSource!!
    }

    private fun source(source: Source): Source {
        return object : ForwardingSource(source) {
            var totalBytesRead = 0L
            @Throws(IOException::class)
            override fun read(sink: Buffer, byteCount: Long): Long {
                val bytesRead = super.read(sink, byteCount)
                // read() returns the number of bytes read, or -1 if this source is exhausted.
                totalBytesRead += if (bytesRead != -1L) bytesRead else 0
                mUIHandler.post {
                    progressListener.onProgress(
                        totalBytesRead,
                        responseBody.contentLength(),
                        bytesRead == -1L
                    )
                }
                return bytesRead
            }
        }
    }

    interface ProgressListener {
        fun update(bytesRead: Long, contentLength: Long, done: Boolean)
    }
}