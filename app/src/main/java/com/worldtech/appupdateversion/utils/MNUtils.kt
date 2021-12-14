package com.worldtech.appupdateversion.utils

import android.content.Context
import android.os.Environment
import java.io.File
import java.io.IOException

object MNUtils {
    /**
     * 获取app缓存路径    SDCard/Android/data/你的应用的包名/cache
     *
     * @param context
     */
    fun getCachePath(context: Context): String {
        return if (Environment.MEDIA_MOUNTED == Environment.getExternalStorageState() || !Environment.isExternalStorageRemovable()) {
            //外部存储可用
            context.externalCacheDir!!.path
        } else {
            //外部存储不可用
            context.cacheDir.path
        }
    }

    //参照：APK放到data/data/下面提示解析失败
    fun changeApkFileMode(file: File) {
        try {
            //apk放在缓存目录时，低版本安装提示权限错误，需要对父级目录和apk文件添加权限
            val cmd1 = "chmod 777 " + file.parent
            Runtime.getRuntime().exec(cmd1)
            val cmd = "chmod 777 " + file.absolutePath
            Runtime.getRuntime().exec(cmd)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}