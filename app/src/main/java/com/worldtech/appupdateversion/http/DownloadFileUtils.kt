package com.worldtech.appupdateversion.http

import android.os.Handler
import java.lang.NullPointerException
import kotlin.Throws
import java.io.IOException
import android.util.Log
import java.io.InputStream
import java.io.FileOutputStream
import java.io.File
import java.lang.Exception
import java.util.HashMap
import android.os.Looper
import okhttp3.*
import javax.net.ssl.X509TrustManager
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import java.security.SecureRandom
import java.security.NoSuchAlgorithmException
import java.security.KeyManagementException
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.HostnameVerifier

/**
 * 文件上传下载相关的工具
 */
class DownloadFileUtils private constructor() {
    /**
     * 默认AbsFileProgressCallback
     */
    private val defaultFileProgressCallback: AbsFileProgressCallback =
        object : AbsFileProgressCallback() {
            override fun onSuccess(result: String?) {}
            override fun onProgress(bytesRead: Long, contentLength: Long, done: Boolean) {}
            override fun onFailed(errorMsg: String?) {}
            override fun onStart() {}
            override fun onCancle() {}
        }

    /**
     * 请求相关参数
     */
    private val downloadModel: DownloadModel?

    /**
     * 设置请求Url
     *
     * @param url
     * @return
     */
    fun url(url: String?): DownloadFileUtils? {
        downloadModel!!.httpUrl = url
        return instance
    }

    /**
     * 下载文件保存的路径
     *
     * @param filePath
     * @return
     */
    fun downloadPath(filePath: String?): DownloadFileUtils? {
        downloadModel!!.downloadPath = filePath
        return instance
    }

    /**
     * 下载文件保存的路径
     *
     * @param tag
     * @return
     */
    fun tag(tag: Any?): DownloadFileUtils? {
        downloadModel!!.tag = tag
        return instance
    }

    /**
     * 设置请求头
     *
     * @param headersMap
     * @return
     */
//    fun headers(headersMap: Map<String?, String?>?): DownloadFileUtils? {
//        downloadModel!!.headersMap = headersMap
//        return instance
//    }

    /**
     * 设置单个请求头
     *
     * @param headerKey
     * @param headerValue
     * @return
     */
//    fun addHeader(headerKey: String?, headerValue: String?): DownloadFileUtils? {
//        downloadModel!!.headersMap[headerKey] = headerValue
//        return instance
//    }

    /**
     * 上传下载进度回调
     *
     * @param fileProgressCallback
     */
    fun execute(fileProgressCallback: AbsFileProgressCallback?) {
        var fileProgressCallback = fileProgressCallback
        if (fileProgressCallback == null) {
            fileProgressCallback = defaultFileProgressCallback
        }
        downloadModel!!.fileProgressCallback = fileProgressCallback
        //开始请求
        startDonwload()
    }

    private fun startDonwload() {
        if (downloadModel == null) {
            throw NullPointerException("OkhttpRequestModel初始化失败")
        }
        //获取参数
        //请求地址
        val httpUrl = downloadModel.httpUrl
        //请求Tag
        var tag = downloadModel.tag
        if (tag == null) {
            tag = httpUrl
        }
        //请求头
        val headersMap = downloadModel.headersMap
        //下载保存的路径
        val downloadPath = downloadModel.downloadPath
        //文件回调
        val fileProgressCallback = downloadModel.fileProgressCallback

        //获取OkHttpClient
        val okhttpBuilder = okhttpDefaultBuilder
        //初始化请求
        val requestBuild = Request.Builder()
        //添加请求地址
        requestBuild.url(httpUrl)
        //添加请求头
        if (headersMap != null && headersMap.size > 0) {
            for (key in headersMap.keys) {
                requestBuild.addHeader(key, headersMap[key])
            }
        }
        okhttpBuilder.addNetworkInterceptor { chain ->
            val originalResponse = chain.proceed(chain.request())
            originalResponse.newBuilder()
                .body(ProgressResponseBody(originalResponse.body()!!, fileProgressCallback!!))
                .build()
        }
        mUIHandler.post { fileProgressCallback!!.onStart() }
        val call = okhttpBuilder.build().newCall(requestBuild.get().build())
        //添加请求到集合
        mCallHashMap!![tag] = call
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "onFailure:$e")
                mUIHandler.post {
                    if (call.isCanceled) {
                        // 下载取消
                        fileProgressCallback!!.onCancle()
                    } else {
                        // 下载失败
                        fileProgressCallback!!.onFailed(e.toString())
                    }
                }
            }

            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                var `is`: InputStream? = null
                val buf = ByteArray(2048)
                var len = 0
                var fos: FileOutputStream? = null
                // 储存下载文件的目录
                if (downloadPath != null) {
                    checkDownloadFilePath(downloadPath)
                }
                try {
                    `is` = response.body()!!.byteStream()
                    val total = response.body()!!.contentLength()
                    val file = File(downloadPath)
                    fos = FileOutputStream(file)
                    var sum: Long = 0
                    while (`is`.read(buf).also { len = it } != -1) {
                        fos.write(buf, 0, len)
                        sum += len.toLong()
                    }
                    fos.flush()
                    mUIHandler.post { // 下载完成
                        fileProgressCallback!!.onSuccess("")
                    }
                } catch (e: Exception) {
                    mUIHandler.post {
                        Log.e(TAG, "onFailure:" + e.message)
                        if (e.message != null && e.message == "Socket closed") {
                            // 下载失败
                            fileProgressCallback!!.onCancle()
                        } else {
                            // 下载失败
                            fileProgressCallback!!.onFailed(e.toString())
                        }
                    }
                } finally {
                    try {
                        `is`?.close()
                    } catch (e: IOException) {
                    }
                    try {
                        fos?.close()
                    } catch (e: IOException) {
                    }
                }
            }
        })
    }

    companion object {
        private val TAG = DownloadFileUtils::class.java.simpleName

        /**
         * 请求的集合
         */
        private val mCallHashMap: HashMap<Any?, Call>? = HashMap()

        /**
         * 当前实例
         */
        private var instance: DownloadFileUtils? = null

        /**
         * UI线程
         */
        private val mUIHandler = Handler(Looper.getMainLooper())

        /**
         * 回去当前实例
         *
         * @return
         */
        @JvmStatic
        fun with(): DownloadFileUtils? {
            instance = DownloadFileUtils()
            return instance
        }



        private fun checkDownloadFilePath(localFilePath: String) {
            val path = File(
                localFilePath.substring(
                    0,
                    localFilePath.lastIndexOf("/") + 1
                )
            )
            val file = File(localFilePath)
            if (!path.exists()) {
                path.mkdirs()
            }
            if (!file.exists()) {
                try {
                    file.createNewFile()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }//默认信任所有的证书

        /**
         * 获取默认OkHttpClient.Builder
         *
         * @return
         */
        val okhttpDefaultBuilder: OkHttpClient.Builder
            get() {
                //默认信任所有的证书
                val trustManager: X509TrustManager = object : X509TrustManager {
                    override fun checkClientTrusted(
                        chain: Array<X509Certificate>,
                        authType: String
                    ) {
                    }

                    override fun checkServerTrusted(
                        chain: Array<X509Certificate>,
                        authType: String
                    ) {
                    }

                    override fun getAcceptedIssuers(): Array<X509Certificate?> {
                        return arrayOfNulls(0)
                    }
                }
                var sslContext: SSLContext? = null
                try {
                    sslContext = SSLContext.getInstance("SSL")
                    sslContext.init(null, arrayOf<TrustManager>(trustManager), SecureRandom())
                } catch (e: NoSuchAlgorithmException) {
                    e.printStackTrace()
                } catch (e: KeyManagementException) {
                    e.printStackTrace()
                }
                val sslSocketFactory = sslContext!!.socketFactory
                val DO_NOT_VERIFY = HostnameVerifier { hostname, session -> true }
                val builder = OkHttpClient.Builder()
                builder.connectTimeout(30000, TimeUnit.MILLISECONDS)
                builder.readTimeout(30000, TimeUnit.MILLISECONDS)
                builder.writeTimeout(30000, TimeUnit.MILLISECONDS)
                builder.sslSocketFactory(sslSocketFactory, trustManager)
                builder.hostnameVerifier(DO_NOT_VERIFY)
                return builder
            }

        /**
         * 取消一个请求
         *
         * @param tag
         */
        @JvmStatic
        fun cancle(tag: Any?) {
            try {
                if (mCallHashMap != null && mCallHashMap.size > 0) {
                    if (mCallHashMap.containsKey(tag)) {
                        //获取对应的Call
                        val call = mCallHashMap[tag]
                        if (call != null) {
                            //如果没有被取消 执行取消的方法
                            if (!call.isCanceled) {
                                call.cancel()
                            }
                            //移除对应的KEY
                            mCallHashMap.remove(tag)
                        }
                    }
                }
            } catch (e: Exception) {
            }
        }

        /**
         * 取消所有请求
         */
        fun cancleAll() {
            try {
                if (mCallHashMap != null && mCallHashMap.size > 0) {
                    //获取KEY的集合
                    val keyEntries: Set<Map.Entry<Any?, Call>> = mCallHashMap.entries
                    for ((key, call) in keyEntries) {
                        //key
                        //获取对应的Call
                        if (call != null) {
                            //如果没有被取消 执行取消的方法
                            if (!call.isCanceled) {
                                call.cancel()
                            }
                            //移除对应的KEY
                            mCallHashMap.remove(key)
                        }
                    }
                }
            } catch (e: Exception) {
            }
        }
    }

    init {
        downloadModel = DownloadModel()
    }
}