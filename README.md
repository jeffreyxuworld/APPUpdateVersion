# APPUpdateVersion
[![Platform Android](https://img.shields.io/badge/platform-Android-brightgreen)](https://developer.android.com/)

Android版本更新弹框，内部包括：强制/非强制更新、内部下载（适配8.0以上）/跳转浏览器下载安装，等等相关功能


Android8.0之前
未知应用安装权限默认开启，如下图所示
Android8.0之后
未知应用安装权限默认关闭，且权限入口隐藏。

1.如何开启未知应用安装权限的入口，并设置允许安装？
在AndroidManifest清单文件中添加权限
`<uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES"/>`

2.如果为8.0以上系统，则判断是否有未知应用安装权限
```
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Uri packageURI = Uri.parse("package:" + activity.getPackageName());
            Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, packageURI);
            new ActResultRequest(activity).startForResult(intent, new ActForResultCallback() {
                @Override
                public void onActivityResult(int resultCode, Intent data) {
                    Log.i(TAG, "onActivityResult:" + resultCode);
                    if (resultCode == RESULT_OK) {
                        //用户授权了
                        if (installPermissionCallBack != null) {
                            installPermissionCallBack.onGranted();
                        }
                    } else {
                        //用户没有授权
                        if (installPermissionCallBack != null) {
                            installPermissionCallBack.onDenied();
                        }
                    }
                }
            });
        } else {
            //用户授权了
            if (installPermissionCallBack != null) {
                installPermissionCallBack.onGranted();
            }
        }
```
3.如果没有未知应用安装权限,则需要手动开启
```
    /**
     * 判断有没有安装权限
     * @param context
     * @return
     */
    public static boolean hasInstallPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //先获取是否有安装未知来源应用的权限
            return context.getPackageManager().canRequestPackageInstalls();
        }
        return true;
    }
```
4.然后根据用户是否授权，做对应处理。
```

    /**
     * 安装APK工具类
     *
     * @param context  上下文
     * @param filePath 文件路径
     * @param callBack 安装界面成功调起的回调
     */
    public static void installAPK(Activity context, String filePath, final InstallCallBack callBack) {
        try {
            MNUtils.changeApkFileMode(new File(filePath));
            Intent intent = new Intent();
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setAction(Intent.ACTION_VIEW);
            File apkFile = new File(filePath);
            Uri apkUri;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                // 授予目录临时共享权限
                String authority = context.getPackageName() + ".updateFileProvider";
                //android适配android10安装
                apkUri = FileProvider.getUriForFile(context, authority, apkFile); //与manifest中定义的provider中的authorities="com.yoka.tablepark.fileProvider"保持一致
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } else {
                apkUri = Uri.fromFile(apkFile);
            }
            intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            new ActResultRequest(context).startForResult(intent, new ActForResultCallback() {
                @Override
                public void onActivityResult(int resultCode, Intent data) {
                    Log.i(TAG, "onActivityResult:" + resultCode);
                    //调起了系统安装页面
                    if (callBack != null) {
                        callBack.onSuccess();
                    }
                }
            });

        } catch (Exception e) {
            if (callBack != null) {
                callBack.onFail(e);
            }
        }
    }

    private void installApk(String path) {
        InstallUtils.installAPK(this, path, new InstallUtils.InstallCallBack() {
            @Override
            public void onSuccess() {
                //onSuccess：表示系统的安装界面被打开
                //防止用户取消安装，在这里可以关闭当前应用，以免出现安装被取消
                ActivityQueueManager.getInstance().finishAllActivity();
//                android.os.Process.killProcess(android.os.Process.myPid());
                CustomToast.showToast(R.string.installing_apk);
            }

            @Override
            public void onFail(Exception e) {
//                LogSaveUtils.addLog("10006","安装失败");
                LogUtil.d("ttt","installApk onFail e = " + e.getMessage());
                CustomToast.showToast(R.string.install_failed);
                CommonUtil.openBrowser(SplashActivity.this, updateApkUrl);
            }
        });
    }
```


