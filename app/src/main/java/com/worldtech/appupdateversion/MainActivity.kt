package com.worldtech.appupdateversion;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import com.tbruyelle.rxpermissions2.RxPermissions;
import com.worldtech.appupdateversion.utils.CommonUtil;
import com.worldtech.appupdateversion.utils.FileUtils;
import com.worldtech.appupdateversion.utils.InstallUtils;
import com.worldtech.appupdateversion.utils.NetWorkUtils;

import java.math.BigDecimal;

public class MainActivity extends AppCompatActivity {

    private RxPermissions rxPermissions;
    private String updateApkUrl;
    private boolean isInDownload = false;
    private UpdateDialog updateDialog;
    private String apkDownloadPath = null;
    private InstallUtils.DownloadCallBack downloadCallBack;
    private boolean isExsit;
    private boolean isMustUpdate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        rxPermissions = new RxPermissions(this);
        initCallback();
        updateApkUrl = "http://www.zhuoyou.com/upload/YouKaPlatForm-youka-Android.apk";
        String versionName = "2.0.0";
        String message = "I'm message";
        long fileSize = 20;
        try {
            FileUtils.deleteDownOldFile(this, versionName);
        } catch (Exception e) {
            e.printStackTrace();
        }
        String apkDownloadPath = null;
        try {
            apkDownloadPath = FileUtils.getApkDownloadPath(this, versionName);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            isExsit = FileUtils.isUpdateFileExist(apkDownloadPath, fileSize);  // fileSize单位为b
        } catch (Exception e) {
            e.printStackTrace();
        }
        isMustUpdate = true;
        final int internalSetup = 1; //是否为内部更新
        // 上面的参数，可依赖于服务器的接口具体返回

        String positiveText = isExsit?getString(R.string.click_install):getString(R.string.dialog_notify_positive);
        updateDialog = new UpdateDialog(this, getString(R.string.have_new_version) , getString(R.string.version_coode_number) + versionName ,
                message,
                positiveText,
                getString(R.string.dialog_notify_negative),
                !isMustUpdate);
        updateDialog.setListener(new UpdateDialog.OnNormalDialogClickListener() {
            @SuppressLint("CheckResult")
            @Override
            public void onPositive(String desc) {
                if(internalSetup == 1){
                    if(!isExsit){
                        rxPermissions
                                .request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                .subscribe(granted -> {
                                    if (granted) {
//                                        LogUtil.d("ttt","onPositive downloadApk updateApkUrl = ");
                                        downloadApk();
                                    }else{
//                                        CustomToast.showToast("您需要开启存储权限");
                                    }
                                });
                    }else {
                        installApk();
                    }
                }else {
//                    CommonUtil.openBrowser(SplashActivity.this, updateApkUrl);
                }
            }

            @Override
            public void onNegative() {
                try {
                    negativeUpdateDialog();
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        });
        updateDialog.show();



    }

    private void initCallback() {
        downloadCallBack = new InstallUtils.DownloadCallBack() {
            @Override
            public void onStart() {
//                LogUtil.d("ttt","InstallUtils onStart");
                if(updateDialog != null){
                    updateDialog.setProgressCount("0");
                    updateDialog.setPositiveButtonDownloading();
                    isInDownload = true;
                }
            }

            @Override
            public void onComplete(String path) {
//                LogUtil.d("ttt","InstallUtils onComplete");
                apkDownloadPath = path;
                updateDialog.setProgressCount("100");
                updateDialog.setPositiveButtonDownloadingFinish();
                isExsit = true;
                installApk();
                isInDownload = false;
            }

            @Override
            public void onLoading(long total, long current) {
                int progress = (int)(div(current,total,2)*100);
//                LogUtil.d("ttt","InstallUtils onLoading current = " + current + ",total = " + total + ",progress = " + progress);
                updateDialog.setProgressCount(String.valueOf(progress));
            }

            @Override
            public void onFail(Exception e) {
                isExsit = false;
//                LogUtil.d("ttt","InstallUtils onFail e" + e.getMessage());
                Toast.makeText(MainActivity.this, R.string.download_fail, Toast.LENGTH_SHORT).show();
                updateDialog.setPositiveButtonDownload();
                try {
                    FileUtils.deleteFile(apkDownloadPath);
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
                updateDialog.setProgressCount("0");
                isInDownload = false;
            }

            @Override
            public void cancle() {
                isExsit = false;
//                LogUtil.d("ttt","InstallUtils cancle");
                try {
                    FileUtils.deleteFile(apkDownloadPath);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                updateDialog.setProgressCount("0");
                isInDownload = false;
            }
        };
    }

    private synchronized void downloadApk() {
        if(!TextUtils.isEmpty(updateApkUrl)){
//            LogUtil.d("ttt","downloadApk updateApkUrl = " + updateApkUrl);
            if(NetWorkUtils.isNetworkConnected(this)){
                if(!isInDownload){
                    isInDownload = true;
                    updateDialog.setPositiveButtonDownloading();
                    InstallUtils.with(this)
                            //必须-下载地址
                            .setApkUrl(updateApkUrl)
                            //非必须-下载保存的文件的完整路径+name.apk
                            .setApkPath(apkDownloadPath)
                            //非必须-下载回调
                            .setCallBack(downloadCallBack)
                            //开始下载
                            .startDownload();
                }
            }else {
                isInDownload = false;
//                CustomToast.showToast(R.string.net_error);
            }
        }
    }

    private void installApk() {
        //先判断有没有安装权限
        InstallUtils.checkInstallPermission(this, new InstallUtils.InstallPermissionCallBack() {
            @Override
            public void onGranted() {
                //去安装APK
                installApkFromPath(apkDownloadPath);
            }

            @Override
            public void onDenied() {
                //弹出弹框提醒用户
                AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this)
                        .setTitle("温馨提示")
                        .setMessage("必须授权才能安装APK，请设置允许安装")
                        .setNegativeButton("取消", null)
                        .setPositiveButton("设置", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                //打开设置页面
                                InstallUtils.openInstallPermissionSetting(MainActivity.this, new InstallUtils.InstallPermissionCallBack() {
                                    @Override
                                    public void onGranted() {
                                        //去安装APK
                                        installApkFromPath(apkDownloadPath);
                                    }

                                    @Override
                                    public void onDenied() {
//                                        LogSaveUtils.addLog("10006","未授权安装apk");
                                        updateDialog.dismiss();
//                                        if(isMustUpdate){
//                                            ActivityQueueManager.getInstance().finishAllActivity();
//                                        }else {
//                                            startMainActivity();
//                                        }
                                    }
                                });
                            }
                        })
                        .create();
                alertDialog.show();
            }
        });
    }

    public static float div(long v1, long v2, int scale) {
        if (scale < 0) {
            throw new IllegalArgumentException(
                    "The scale must be a positive integer or zero");
        }
        BigDecimal b1 = new BigDecimal(Long.toString(v1));
        BigDecimal b2 = new BigDecimal(Long.toString(v2));
        return b1.divide(b2, scale, BigDecimal.ROUND_HALF_UP).floatValue();
    }

    private void installApkFromPath(String path) {
        InstallUtils.installAPK(this, path, new InstallUtils.InstallCallBack() {
            @Override
            public void onSuccess() {
                //onSuccess：表示系统的安装界面被打开
                //防止用户取消安装，在这里可以关闭当前应用，以免出现安装被取消
//                ActivityQueueManager.getInstance().finishAllActivity();
                finish();
                Toast.makeText(MainActivity.this, R.string.installing_apk, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFail(Exception e) {
//                LogSaveUtils.addLog("10006","安装失败");
//                LogUtil.d("ttt","installApk onFail e = " + e.getMessage());
                Toast.makeText(MainActivity.this, R.string.install_failed, Toast.LENGTH_SHORT).show();
                CommonUtil.openBrowser(MainActivity.this, updateApkUrl);
            }
        });
    }

    private void negativeUpdateDialog() throws Exception {
        updateDialog.dismiss();
        InstallUtils.cancleDownload();
        FileUtils.deleteFile(apkDownloadPath);
        //根据自己的逻辑，强更：退出应用；非强更：跳主页面
//        if(isMustUpdate){
//            ActivityQueueManager.getInstance().finishAllActivity();
//        }else {
//            startMainActivity();
//        }
    }

}
