package com.worldtech.appupdateversion.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Environment;
import android.text.TextUtils;

import java.io.File;
import java.io.IOException;

/**
 * 文件相关工具类
 */
public class FileUtils {
    public static String IMGDir = "Pictures";//图片存储目录

    public static String PHOTODir = "image";//图片存储目录

    public static  File getDiskCacheDir(Context context, String uniqueName) {
        File file = null;
        String cachePath = null;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            cachePath = context.getExternalFilesDir(null).getAbsolutePath();
        }else{
            if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
                    || !Environment.isExternalStorageRemovable()) {
                if(context.getExternalCacheDir() != null){
                    cachePath = context.getExternalCacheDir().getPath(); //路径为:/mnt/sdcard//Android/data/< package name >/cach/…
                }else{
                    try {
                        cachePath  = Environment.getExternalStorageDirectory().getPath();   //SD根目录:/mnt/sdcard/ (6.0后写入需要用户授权)
                    }catch (Exception e){
                        cachePath = context.getCacheDir().getPath();  	//路径是:/data/data/< package name >/cach/…
                    }
                }
            } else {
                cachePath = context.getCacheDir().getPath();  	//路径是:/data/data/< package name >/cach/…
            }
        }
        file = new File(cachePath + File.separator + uniqueName);
        if(file.exists()){
            return file;
        }else {
            if(file.mkdir()){
               return file;
            }
        }
        return file;
    }
    public static File getPhotoPath(Context context, String name) {//拍照文件夹
        File sampleDir = context.getExternalFilesDir(IMGDir);
        if (!sampleDir.exists()) {
            sampleDir.mkdirs();
        }
        File file = new File(sampleDir,name);
        if(!file.exists()){
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return file;
    }

    public static void deleteDir(File folder) {
        if (folder == null || !folder.exists() || !folder.isDirectory())
            return;

        for (File file : folder.listFiles()) {
            if (file.isFile())
                file.delete(); // 删除所有文件
            else if (file.isDirectory())
                deleteDir(file); // 递规的方式删除文件夹
        }
    }

    public static final String VIDEO_PATH_NAME = "videorecord";


    public static void deleteFile(String filePath) {
        File file = new File(filePath);
        if (file.exists()) {
            if (file.isFile()) {
                file.delete();
            } else {
                String[] filePaths = file.list();
                for (String path : filePaths) {
                    deleteFile(filePath + File.separator + path);
                }
                file.delete();
            }
        }
    }


    public static void deleteDownOldFile(Context context, String verson_name) {
        String newApk = "gameplatformupdate" + verson_name + ".apk";
        String name = FileUtils.getDiskCacheDir(context, "update").getAbsolutePath();
        File file = new File(name);
        if (file.exists()) {
            if (!file.isFile()) {
                String[] filePaths = file.list();
                if (filePaths != null && filePaths.length > 0) {
                    for (int i = 0; i < filePaths.length; i++) {
                        if(filePaths[i].equals(newApk)){
                            continue;
                        }else {
                            deleteFile(name+File.separator+filePaths[i]);
                        }
                    }
                }
            }
        }

    }

    public static String getApkDownloadPath(Context context, String versionName) {
        String name = FileUtils.getDiskCacheDir( context,"update").getAbsolutePath() + "/gameplatformupdate" + versionName + ".apk";
        return name;
    }

    public static boolean isUpdateFileExist(String apkDownloadPath,long serverFileSize) {
        File file = new File(apkDownloadPath);
        if(file.exists()){
            try {
                long size = FileSizeUtil.getFileSize(file);
                if(serverFileSize <= size){
                    return true;
                }else {
                    FileUtils.deleteFile(apkDownloadPath);
                    return false;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }else {
            return false;
        }
    }

    public static String getPhotoDir(Context context) {
        File sampleDir = context.getExternalFilesDir(PHOTODir);
        if (!sampleDir.exists()) {
            sampleDir.mkdirs();
        }
        return sampleDir.getAbsolutePath();
    }

}
