package com.worldtech.appupdateversion;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

public class UpdateDialog extends AlertDialog {

    private OnNormalDialogClickListener listener;

    private Activity mActivity;

    /**
     * 标题控件
     */
    private TextView tv_title;

    private TextView tv_version;

    /**
     * 内容控件
     */
    private TextView tv_content;

    /**
     * 确定按钮
     */
    private TextView tv_positive;

    /**
     * 取消按钮
     */
    private TextView tv_negative;

    /**
     * 确定取消之间的竖线
     */
    private View view_line;

    /**
     * 标题内容
     */
    private CharSequence strTitle;

    private CharSequence strVersion;

    /**
     * 内容
     */
    private CharSequence strContent;

    /**
     * 确定按钮的名称
     */
    private String strPositive;

    /**
     * 取消按钮的名称
     */
    private String strNegative;

    private boolean cancelOutside;
    private TextView tv_progress;

    /**
     * 右边是确定，左边是取消
     * @param activity
     * @param title
     * @param content
     * @param strPositive
     * @param strNegative
     */
    public UpdateDialog(@NonNull Activity activity , CharSequence title , CharSequence strVersion, CharSequence content , String strPositive , String strNegative, boolean cancelOutside) {
        super(activity,R.style.baseDialog);
        this.mActivity = activity;
        this.strTitle = title;
        this.strVersion = strVersion;
        this.strContent = content;
        this.strPositive = strPositive;
        this.strNegative = strNegative;
        this.cancelOutside = cancelOutside;
        setCanceledOnTouchOutside(false);
        setCancelable(false);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initView();
        initData();
    }

    /**
     * 初始化控件
     */
    private void initView(){
        setContentView(R.layout.dialog_notify);
        tv_title = (TextView) findViewById(R.id.tv_title);
        tv_version = (TextView) findViewById(R.id.tv_version);
        tv_content = (TextView) findViewById(R.id.tv_content);
        tv_positive = (TextView) findViewById(R.id.tv_positive);
        tv_negative = (TextView) findViewById(R.id.tv_negative);
        tv_progress = (TextView) findViewById(R.id.tv_progress);
        view_line = findViewById(R.id.view_line);
    }

    /**
     * 设置确定取消按钮的监听器 ， 右边是确定，左边是取消
     * @param listener
     */
    public void setListener(OnNormalDialogClickListener listener) {
        this.listener = listener;
    }

    /**
     * 初始化数据
     */
    private void initData(){
        setOnCancelListener(new OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
//                LogUtil.i("test" , "通用对话框被取消了");
                if(listener != null){
                    listener.onNegative();
                }
            }
        });
        if(TextUtils.isEmpty(strTitle)){
            tv_title.setVisibility(View.GONE);
        }else{
            tv_title.setVisibility(View.VISIBLE);
            tv_title.setText(strTitle);
        }

        if(TextUtils.isEmpty(strVersion)){   //版本信息
            tv_version.setVisibility(View.GONE);
        }else{
            tv_version.setVisibility(View.VISIBLE);
            tv_version.setText(strVersion);
        }

        if(TextUtils.isEmpty(strContent)){
            tv_content.setVisibility(View.GONE);
        }else{
            tv_content.setVisibility(View.VISIBLE);
            tv_content.setText(strContent);
        }

        if(!TextUtils.isEmpty(strPositive)){
            tv_positive.setText(strPositive);
        }else{
            tv_positive.setVisibility(View.GONE);
        }

        if(!TextUtils.isEmpty(strNegative)){
            tv_negative.setText(strNegative);
        }else{
            tv_negative.setVisibility(View.GONE);
        }

        if(!cancelOutside){
            tv_negative.setVisibility(View.GONE);
            view_line.setVisibility(View.GONE);
        }

        tv_positive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 确定
//                LogUtil.i("test" , "通用对话框确定被点击了");
                if(listener != null){
                    listener.onPositive(tv_positive.getText().toString());
                }
            }
        });

        tv_negative.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 取消
//                LogUtil.i("test" , "通用对话框取消被点击了");
                if(listener != null){
                    listener.onNegative();
                }
            }
        });
    }

    public void setPositiveButtonDownloading(){
        tv_positive.setText(mActivity.getText(R.string.is_downloading));
        tv_positive.setClickable(false);
    }

    public void setPositiveButtonDownload(){
        tv_positive.setText(mActivity.getText(R.string.dialog_notify_positive));
        tv_positive.setClickable(true);
    }

    public void setPositiveButtonDownloadingFinish(){
        tv_positive.setText(mActivity.getText(R.string.click_install));
        tv_positive.setClickable(true);
    }

    public TextView getDoadownLoadingTextView(){
        return tv_progress;
    }

    public void setProgressCount(String count){
        tv_progress.setText(count+"%");
    }
    /**
     * 通用对话框的确定取消按钮点击事件
     */
    public interface OnNormalDialogClickListener{

        /**
         * 确定
         */
        void onPositive(String desc);

        /**
         * 取消
         */
        void onNegative();
    }

    /**
     * 返回内容的控件
     */
    public TextView getContentTextView(){
        return tv_content;
    }

    /**
     * 返回标题的控件
     * @return
     */
    public TextView getTitleTextView(){
        return tv_title;
    }


    @Override
    public boolean onKeyDown(int keyCode, @NonNull KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK){
            return true;
        }
        return false;
    }


}

