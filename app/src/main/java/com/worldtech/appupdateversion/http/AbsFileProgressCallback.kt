package com.worldtech.appupdateversion.http

/**
 * 文件下载监听
 */
abstract class AbsFileProgressCallback {
    /**
     * 下载成功
     */
    abstract fun onSuccess(result: String?)

    /**
     */
    abstract fun onProgress(bytesRead: Long, contentLength: Long, done: Boolean)

    /**
     * 下载失败
     */
    abstract fun onFailed(errorMsg: String?)

    /**
     * 下载开始
     */
    abstract fun onStart()

    /**
     * 下载取消
     */
    abstract fun onCancle()
}