package com.worldtech.appupdateversion.http

/**
 * 请求参数相关
 *
 */
class DownloadModel {
    /**
     * 请求地址
     */
    var httpUrl: String? = null

    /**
     * 请求头
     */
    var headersMap: Map<String, String>? = null

    /**
     * 请求Tag
     */
    var tag: Any? = null

    /**
     * 下载文件保存的路径
     */
    var downloadPath: String? = null

    /**
     * 文件下载进度
     */
    var fileProgressCallback: AbsFileProgressCallback? = null
}