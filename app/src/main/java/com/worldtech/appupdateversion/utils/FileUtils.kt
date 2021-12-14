package com.worldtech.appupdateversion.utils

import com.worldtech.appupdateversion.utils.FileSizeUtil.getFileSize
import android.content.Context
import java.io.File
import android.os.Build
import android.os.Environment
import java.lang.Exception
import java.io.IOException

/**
 * 文件相关工具类
 */
object FileUtils {
    var IMGDir = "Pictures" //图片存储目录
    var PHOTODir = "image" //图片存储目录
    @JvmStatic
    @Throws(Exception::class)
    fun getDiskCacheDir(context: Context, uniqueName: String): File {
        var file: File? = null
        var cachePath: String? = null
        cachePath = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.getExternalFilesDir(null)!!.absolutePath
        } else {
            if (Environment.MEDIA_MOUNTED == Environment.getExternalStorageState() || !Environment.isExternalStorageRemovable()) {
                if (context.externalCacheDir != null) {
                    context.externalCacheDir!!.path //路径为:/mnt/sdcard//Android/data/< package name >/cach/…
                } else {
                    try {
                        Environment.getExternalStorageDirectory().path //SD根目录:/mnt/sdcard/ (6.0后写入需要用户授权)
                    } catch (e: Exception) {
                        context.cacheDir.path //路径是:/data/data/< package name >/cach/…
                    }
                }
            } else {
                context.cacheDir.path //路径是:/data/data/< package name >/cach/…
            }
        }
        file = File(cachePath + File.separator + uniqueName)
        if (file.exists()) {
            return file
        } else {
            if (file.mkdir()) {
                return file
            }
        }
        return file
    }

    @JvmStatic
    @Throws(Exception::class)
    fun getPhotoPath(context: Context, name: String?): File { //拍照文件夹
        val sampleDir = context.getExternalFilesDir(IMGDir)
        if (!sampleDir!!.exists()) {
            sampleDir.mkdirs()
        }
        val file = File(sampleDir, name)
        if (!file.exists()) {
            try {
                file.createNewFile()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return file
    }

    @JvmStatic
    @Throws(Exception::class)
    fun deleteDir(folder: File?) {
        if (folder == null || !folder.exists() || !folder.isDirectory) return
        for (file in folder.listFiles()) {
            if (file.isFile) file.delete() // 删除所有文件
            else if (file.isDirectory) deleteDir(file) // 递规的方式删除文件夹
        }
    }

    const val VIDEO_PATH_NAME = "videorecord"
    @JvmStatic
    @Throws(Exception::class)
    fun deleteFile(filePath: String) {
        val file = File(filePath)
        if (file.exists()) {
            if (file.isFile) {
                file.delete()
            } else {
                val filePaths = file.list()
                for (path in filePaths) {
                    deleteFile(filePath + File.separator + path)
                }
                file.delete()
            }
        }
    }

    @JvmStatic
    @Throws(Exception::class)
    fun deleteDownOldFile(context: Context, verson_name: String) {
        val newApk = "gameplatformupdate$verson_name.apk"
        val name = getDiskCacheDir(context, "update").absolutePath
        val file = File(name)
        if (file.exists()) {
            if (!file.isFile) {
                val filePaths = file.list()
                if (filePaths != null && filePaths.size > 0) {
                    for (i in filePaths.indices) {
                        if (filePaths[i] == newApk) {
                            continue
                        } else {
                            deleteFile(name + File.separator + filePaths[i])
                        }
                    }
                }
            }
        }
    }

    @JvmStatic
    @Throws(Exception::class)
    fun getApkDownloadPath(context: Context, versionName: String): String {
        return getDiskCacheDir(
            context,
            "update"
        ).absolutePath + "/gameplatformupdate" + versionName + ".apk"
    }

    @JvmStatic
    @Throws(Exception::class)
    fun isUpdateFileExist(apkDownloadPath: String, serverFileSize: Long): Boolean {
        val file = File(apkDownloadPath)
        return if (file.exists()) {
            try {
                val size = getFileSize(file)
                if (serverFileSize <= size) {
                    true
                } else {
                    deleteFile(apkDownloadPath)
                    false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        } else {
            false
        }
    }
    @JvmStatic
    @Throws(Exception::class)
    fun getPhotoDir(context: Context): String {
        val sampleDir = context.getExternalFilesDir(PHOTODir)
        if (!sampleDir!!.exists()) {
            sampleDir.mkdirs()
        }
        return sampleDir.absolutePath
    }
}