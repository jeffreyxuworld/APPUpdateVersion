package com.worldtech.appupdateversion

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Bundle
import android.text.TextUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.tbruyelle.rxpermissions2.RxPermissions
import com.worldtech.appupdateversion.UpdateDialog.OnNormalDialogClickListener
import com.worldtech.appupdateversion.utils.CommonUtil.openBrowser
import com.worldtech.appupdateversion.utils.FileUtils
import com.worldtech.appupdateversion.utils.FileUtils.deleteDownOldFile
import com.worldtech.appupdateversion.utils.FileUtils.getApkDownloadPath
import com.worldtech.appupdateversion.utils.FileUtils.isUpdateFileExist
import com.worldtech.appupdateversion.utils.InstallUtils.*
import com.worldtech.appupdateversion.utils.InstallUtils.Companion.cancleDownload
import com.worldtech.appupdateversion.utils.InstallUtils.Companion.checkInstallPermission
import com.worldtech.appupdateversion.utils.InstallUtils.Companion.installAPK
import com.worldtech.appupdateversion.utils.InstallUtils.Companion.openInstallPermissionSetting
import com.worldtech.appupdateversion.utils.InstallUtils.Companion.with
import com.worldtech.appupdateversion.utils.NetWorkUtils
import java.math.BigDecimal

class MainActivity : AppCompatActivity() {
    private var rxPermissions: RxPermissions? = null
    private var updateApkUrl: String? = null
    private var isInDownload = false
    private var updateDialog: UpdateDialog? = null
    private var apkDownloadPath: String? = null
    private var downloadCallBack: DownloadCallBack? = null
    private var isExsit = false
    private var isMustUpdate = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        rxPermissions = RxPermissions(this)
        initCallback()
        updateApkUrl = "http://www.zhuoyou.com/upload/YouKaPlatForm-youka-Android.apk"
        val versionName = "2.0.0"
        val message = "I'm message"
        val fileSize: Long = 20
        try {
            deleteDownOldFile(this, versionName)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        var apkDownloadPath: String? = null
        try {
            apkDownloadPath = getApkDownloadPath(this, versionName)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            isExsit = isUpdateFileExist(
                apkDownloadPath!!, fileSize
            ) // fileSize单位为b
        } catch (e: Exception) {
            e.printStackTrace()
        }
        isMustUpdate = true
        val internalSetup = 1 //是否为内部更新
        // 上面的参数，可依赖于服务器的接口具体返回
        val positiveText =
            if (isExsit) getString(R.string.click_install) else getString(R.string.dialog_notify_positive)
        updateDialog = UpdateDialog(
            this,
            getString(R.string.have_new_version),
            getString(R.string.version_coode_number) + versionName,
            message,
            positiveText,
            getString(R.string.dialog_notify_negative),
            !isMustUpdate
        )
        updateDialog!!.setListener(object : OnNormalDialogClickListener {
            @SuppressLint("CheckResult")
            override fun onPositive(desc: String) {
                if (internalSetup == 1) {
                    if (!isExsit) {
                        rxPermissions!!
                            .request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            .subscribe { granted: Boolean ->
                                if (granted) {
//                                        LogUtil.d("ttt","onPositive downloadApk updateApkUrl = ");
                                    downloadApk()
                                } else {
//                                        CustomToast.showToast("您需要开启存储权限");
                                }
                            }
                    } else {
                        installApk()
                    }
                } else {
//                    CommonUtil.openBrowser(SplashActivity.this, updateApkUrl);
                }
            }

            override fun onNegative() {
                try {
                    negativeUpdateDialog()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        })
        updateDialog!!.show()
    }

    private fun initCallback() {
        downloadCallBack = object : DownloadCallBack {
            override fun onStart() {
//                LogUtil.d("ttt","InstallUtils onStart");
                if (updateDialog != null) {
                    updateDialog!!.setProgressCount("0")
                    updateDialog!!.setPositiveButtonDownloading()
                    isInDownload = true
                }
            }

            override fun onComplete(path: String?) {
//                LogUtil.d("ttt","InstallUtils onComplete");
                apkDownloadPath = path
                updateDialog!!.setProgressCount("100")
                updateDialog!!.setPositiveButtonDownloadingFinish()
                isExsit = true
                installApk()
                isInDownload = false
            }

            override fun onLoading(total: Long, current: Long) {
                val progress = (div(current, total, 2) * 100).toInt()
                //                LogUtil.d("ttt","InstallUtils onLoading current = " + current + ",total = " + total + ",progress = " + progress);
                updateDialog!!.setProgressCount(progress.toString())
            }

            override fun onFail(e: Exception?) {
                isExsit = false
                //                LogUtil.d("ttt","InstallUtils onFail e" + e.getMessage());
                Toast.makeText(this@MainActivity, R.string.download_fail, Toast.LENGTH_SHORT).show()
                updateDialog!!.setPositiveButtonDownload()
                try {
                    FileUtils.deleteFile(apkDownloadPath!!)
                } catch (exception: Exception) {
                    exception.printStackTrace()
                }
                updateDialog!!.setProgressCount("0")
                isInDownload = false
            }

            override fun cancle() {
                isExsit = false
                //                LogUtil.d("ttt","InstallUtils cancle");
                try {
                    FileUtils.deleteFile(apkDownloadPath!!)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                updateDialog!!.setProgressCount("0")
                isInDownload = false
            }
        }
    }

    @Synchronized
    private fun downloadApk() {
        if (!TextUtils.isEmpty(updateApkUrl)) {
//            LogUtil.d("ttt","downloadApk updateApkUrl = " + updateApkUrl);
            if (NetWorkUtils.isNetworkConnected(this)) {
                if (!isInDownload) {
                    isInDownload = true
                    updateDialog!!.setPositiveButtonDownloading()
                    with(this) //必须-下载地址
                        .setApkUrl(updateApkUrl) //非必须-下载保存的文件的完整路径+name.apk
                        ?.setApkPath(apkDownloadPath) //非必须-下载回调
                        ?.setCallBack(downloadCallBack) //开始下载
                        ?.startDownload()
                }
            } else {
                isInDownload = false
                //                CustomToast.showToast(R.string.net_error);
            }
        }
    }

    private fun installApk() {
        //先判断有没有安装权限
        checkInstallPermission(this, object : InstallPermissionCallBack {
            override fun onGranted() {
                //去安装APK
                installApkFromPath(apkDownloadPath)
            }

            override fun onDenied() {
                //弹出弹框提醒用户
                val alertDialog = AlertDialog.Builder(this@MainActivity)
                    .setTitle("温馨提示")
                    .setMessage("必须授权才能安装APK，请设置允许安装")
                    .setNegativeButton("取消", null)
                    .setPositiveButton("设置") { dialog, which ->
                        //打开设置页面
                        openInstallPermissionSetting(
                            this@MainActivity,
                            object : InstallPermissionCallBack {
                                override fun onGranted() {
                                    //去安装APK
                                    installApkFromPath(apkDownloadPath)
                                }

                                override fun onDenied() {
//                                        LogSaveUtils.addLog("10006","未授权安装apk");
                                    updateDialog!!.dismiss()
                                    //                                        if(isMustUpdate){
//                                            ActivityQueueManager.getInstance().finishAllActivity();
//                                        }else {
//                                            startMainActivity();
//                                        }
                                }
                            })
                    }
                    .create()
                alertDialog.show()
            }
        })
    }

    private fun installApkFromPath(path: String?) {
        installAPK(this, path, object : InstallCallBack {
            override fun onSuccess() {
                //onSuccess：表示系统的安装界面被打开
                //防止用户取消安装，在这里可以关闭当前应用，以免出现安装被取消
//                ActivityQueueManager.getInstance().finishAllActivity();
                finish()
                Toast.makeText(this@MainActivity, R.string.installing_apk, Toast.LENGTH_SHORT)
                    .show()
            }

            override fun onFail(e: Exception?) {
//                LogSaveUtils.addLog("10006","安装失败");
//                LogUtil.d("ttt","installApk onFail e = " + e.getMessage());
                Toast.makeText(this@MainActivity, R.string.install_failed, Toast.LENGTH_SHORT)
                    .show()
                openBrowser(this@MainActivity, updateApkUrl)
            }
        })
    }

    @Throws(Exception::class)
    private fun negativeUpdateDialog() {
        updateDialog!!.dismiss()
        cancleDownload()
        FileUtils.deleteFile(apkDownloadPath!!)
        //根据自己的逻辑，强更：退出应用；非强更：跳主页面
//        if(isMustUpdate){
//            ActivityQueueManager.getInstance().finishAllActivity();
//        }else {
//            startMainActivity();
//        }
    }

    companion object {
        fun div(v1: Long, v2: Long, scale: Int): Float {
            require(scale >= 0) { "The scale must be a positive integer or zero" }
            val b1 = BigDecimal(java.lang.Long.toString(v1))
            val b2 = BigDecimal(java.lang.Long.toString(v2))
            return b1.divide(b2, scale, BigDecimal.ROUND_HALF_UP).toFloat()
        }
    }
}