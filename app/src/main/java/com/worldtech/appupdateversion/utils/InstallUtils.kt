package com.worldtech.appupdateversion.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import androidx.core.content.FileProvider
import com.worldtech.appupdateversion.http.AbsFileProgressCallback
import com.worldtech.appupdateversion.http.DownloadFileUtils.Companion.cancle
import com.worldtech.appupdateversion.http.DownloadFileUtils.Companion.with
import java.io.File

/**
 * 安装APK的工具
 */
class InstallUtils
/**
 * 私有构造函数
 */
private constructor() {
    //------------------下载相关---------------------
    private var httpUrl: String? = null
    private var filePath: String? = null

    /**
     * 下载回调监听
     */
    interface DownloadCallBack {
        fun onStart()
        fun onComplete(path: String?)
        fun onLoading(total: Long, current: Long)
        fun onFail(e: Exception?)
        fun cancle()
    }

    /**
     * 设置下载地址
     *
     * @param apkUrl
     * @return
     */
    fun setApkUrl(apkUrl: String?): InstallUtils? {
        httpUrl = apkUrl
        return mInstance
    }

    /**
     * 设置下载后保存的地址,带后缀
     *
     * @param apkPath
     * @return
     */
    fun setApkPath(apkPath: String?): InstallUtils? {
        filePath = apkPath
        return mInstance
    }

    /**
     * 设置回调监听
     *
     * @param downloadCallBack
     * @return
     */
    fun setCallBack(downloadCallBack: DownloadCallBack?): InstallUtils? {
        mDownloadCallBack = downloadCallBack
        return mInstance
    }

    /**
     * 开始下载
     */
    fun startDownload() {
        //先取消之前的下载
        if (isDownloading) {
            cancleDownload()
        }
        //判断下载保存路径是不是空
        if (TextUtils.isEmpty(filePath)) {
            filePath = MNUtils.getCachePath(mContext!!) + "/update.apk"
        }
        //文件权限处理
        MNUtils.changeApkFileMode(File(filePath))
        //下载
        with()
            ?.downloadPath(filePath)
            ?.url(httpUrl)
            ?.tag(InstallUtils::class.java)
            ?.execute(object : AbsFileProgressCallback() {
                var currentProgress = 0
                override fun onSuccess(result: String?) {
                    isDownloading = false
                    if (mDownloadCallBack != null) {
                        mDownloadCallBack!!.onComplete(filePath)
                    }
                }

                override fun onProgress(bytesRead: Long, contentLength: Long, done: Boolean) {
                    isDownloading = true
                    if (mDownloadCallBack != null) {
                        //计算进度
                        val progress = (bytesRead * 100 / contentLength).toInt()
                        //只有进度+1才回调，防止过快
                        if (progress - currentProgress >= 1) {
//                                LogUtil.d("ttt","onProgress progress = " + progress + ",currentProgress = "+ currentProgress);
                            mDownloadCallBack!!.onLoading(contentLength, bytesRead)
                            currentProgress = progress
                        }
                    }
                }

                override fun onFailed(errorMsg: String?) {
                    isDownloading = false
                    if (mDownloadCallBack != null) {
                        mDownloadCallBack!!.onFail(Exception(errorMsg))
                    }
                }

                override fun onStart() {
                    isDownloading = true
                    if (mDownloadCallBack != null) {
                        mDownloadCallBack!!.onStart()
                    }
                }

                override fun onCancle() {
                    isDownloading = false
                    if (mDownloadCallBack != null) {
                        mDownloadCallBack!!.cancle()
                    }
                }
            })
    }
    //------------------安装相关---------------------
    /**
     * 安装回调监听
     */
    interface InstallCallBack {
        fun onSuccess()
        fun onFail(e: Exception?)
    }

    /**
     * 8.0权限检查回调监听
     */
    interface InstallPermissionCallBack {
        fun onGranted()
        fun onDenied()
    }

    companion object {
        private val TAG = InstallUtils::class.java.simpleName
        @SuppressLint("StaticFieldLeak")
        private var mInstance: InstallUtils? = null
        @SuppressLint("StaticFieldLeak")
        private var mContext: Context? = null
        private var mDownloadCallBack: DownloadCallBack? = null
        /**murdermystery
         * 是否正在下载
         */
        /**
         * 是不是在正在下载
         */
        var isDownloading = false
            private set

        /**
         * 设置监听
         *
         * @param downloadCallBack
         */
        @JvmStatic
        fun setDownloadCallBack(downloadCallBack: DownloadCallBack?) {
            //判断有没有开始
            if (isDownloading) {
                mDownloadCallBack = downloadCallBack
            }
        }


        @JvmStatic
        fun with(activity: Activity): InstallUtils {
            mContext = activity.applicationContext
            if (mInstance == null) {
                mInstance = InstallUtils()
            }
            return mInstance as InstallUtils
        }

        @JvmStatic
        fun cancleDownload() {
            cancle(InstallUtils::class.java)
        }

        /**
         * 安装APK工具类
         *
         * @param context  上下文
         * @param filePath 文件路径
         * @param callBack 安装界面成功调起的回调
         */
        @JvmStatic
        fun installAPK(context: Activity, filePath: String?, callBack: InstallCallBack?) {
            try {
                MNUtils.changeApkFileMode(File(filePath))
                val intent = Intent()
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                intent.action = Intent.ACTION_VIEW
                val apkFile = File(filePath)
                val apkUri: Uri
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    // 授予目录临时共享权限
                    val authority = context.packageName + ".updateFileProvider"
                    //android适配android10安装
                    apkUri = FileProvider.getUriForFile(
                        context,
                        authority,
                        apkFile
                    ) //与manifest中定义的provider中的authorities="com.yoka.tablepark.fileProvider"保持一致
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } else {
                    apkUri = Uri.fromFile(apkFile)
                }
                intent.setDataAndType(apkUri, "application/vnd.android.package-archive")
                ActResultRequest(context).startForResult(intent, object : ActForResultCallback {
                    override fun onActivityResult(resultCode: Int, data: Intent?) {
                        Log.i(TAG, "onActivityResult:$resultCode")
                        //调起了系统安装页面
                        callBack?.onSuccess()
                    }
                })
            } catch (e: Exception) {
                callBack?.onFail(e)
            }
        }

        /**
         * 通过浏览器下载APK更新安装
         *
         * @param context    上下文
         * @param httpUrlApk APK下载地址
         */
        @JvmStatic
        fun installAPKWithBrower(context: Context, httpUrlApk: String?) {
            val uri = Uri.parse(httpUrlApk)
            val viewIntent = Intent(Intent.ACTION_VIEW, uri)
            context.startActivity(viewIntent)
        }

        /**
         * 检查有没有安装权限
         * @param activity
         * @param installPermissionCallBack
         */
        @JvmStatic
        fun checkInstallPermission(
            activity: Activity,
            installPermissionCallBack: InstallPermissionCallBack
        ) {
            if (hasInstallPermission(activity)) {
                installPermissionCallBack.onGranted()
            } else {
                openInstallPermissionSetting(activity, installPermissionCallBack)
            }
        }


        /**
         * 判断有没有安装权限
         * @param context
         * @return
         */
        @JvmStatic
        private fun hasInstallPermission(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                //先获取是否有安装未知来源应用的权限
                context.packageManager.canRequestPackageInstalls()
            } else true
        }

        /**
         * 去打开安装权限的页面
         * @param activity
         * @param installPermissionCallBack
         */
        @JvmStatic
        fun openInstallPermissionSetting(
            activity: Activity,
            installPermissionCallBack: InstallPermissionCallBack?
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val packageURI = Uri.parse("package:" + activity.packageName)
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, packageURI)
                ActResultRequest(activity).startForResult(intent, object : ActForResultCallback {
                    override fun onActivityResult(resultCode: Int, data: Intent?) {
                        Log.i(TAG, "onActivityResult:$resultCode")
                        if (resultCode == Activity.RESULT_OK) {
                            //用户授权了
                            installPermissionCallBack?.onGranted()
                        } else {
                            //用户没有授权
                            installPermissionCallBack?.onDenied()
                        }
                    }
                })
            } else {
                //用户授权了
                installPermissionCallBack?.onGranted()
            }
        }


    }
}