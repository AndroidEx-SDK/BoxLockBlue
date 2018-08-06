package com.androidex.capbox.utils;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Environment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import com.androidex.capbox.R;
import com.androidex.capbox.base.BaseActivity;
import com.androidex.capbox.data.cache.SharedPreTool;
import com.androidex.capbox.data.net.NetApi;
import com.androidex.capbox.data.net.base.ResultCallBack;
import com.androidex.capbox.module.BaseModel;
import com.androidex.capbox.module.CheckVersionModel;
import com.androidex.capbox.ui.activity.LoginActivity;

import okhttp3.Headers;
import okhttp3.Request;

import static com.androidex.capbox.data.cache.SharedPreTool.LOGIN_STATUS;

/**
 * @Description:
 * @Author: Liyp
 * @Email: liyp@androidex.cn
 * @Date: 2018/5/26
 */
public class NetApiUtil {
    public static String getToken(BaseActivity context) {
        String token = SharedPreTool.getInstance(context).getStringData(SharedPreTool.TOKEN, null);
        if (token == null) {
            CommonKit.showErrorShort(context, "账号未登录");
            SharedPreTool.getInstance(context).setBoolData(LOGIN_STATUS, false);
            LoginActivity.lauch(context);
            return "";
        }
        return token;
    }


    public static void setToken(String token, BaseActivity context) {
        SharedPreTool.getInstance(context).setStringData(SharedPreTool.TOKEN, token);
    }

    public static String getUserName(BaseActivity context) {
        String username = SharedPreTool.getInstance(context).getStringData(SharedPreTool.PHONE, null);
        if (username == null) {
            CommonKit.showErrorShort(context, "账号未登录");
            SharedPreTool.getInstance(context).setBoolData(LOGIN_STATUS, false);
            LoginActivity.lauch(context);
            return "";
        }
        return username;
    }

    public static void setUsername(String username, BaseActivity context) {
        SharedPreTool.getInstance(context).setStringData(SharedPreTool.PHONE, username);
    }

    public static void userLogout(final BaseActivity context) {
        if (!CommonKit.isNetworkAvailable(context)) {
            CommonKit.showErrorShort(context, "设备未连接网络");
            return;
        }
        NetApi.userLogout(getToken(context), getUserName(context), new ResultCallBack<BaseModel>() {
            @Override
            public void onSuccess(int statusCode, Headers headers, BaseModel model) {
                super.onSuccess(statusCode, headers, model);
                if (model != null) {
                    switch (model.code) {
                        case Constants.API.API_OK:
                            CommonKit.showOkShort(context, context.getString(R.string.hint_logout_ok));
                            break;
                        default:
                            break;
                    }
                }
            }

            @Override
            public void onFailure(int statusCode, Request request, Exception e) {
                super.onFailure(statusCode, request, e);
            }

            @Override
            public void onStart() {
                super.onStart();
            }
        });
    }


    /**
     * 检测版本号，包括APP的，箱体的，腕表的
     * {"appFileName":"boxlock-3.apk",
     * "appVersion":"3","boxFileName":"20171129.hex",
     * "boxVersion":"0.0.1","code":0,"watchFileName":"20171129.hex",
     * "watchVersion":"0.0.2"}
     */
    public static void checkVersion(final BaseActivity context) {
        if (!CommonKit.isNetworkAvailable(context)) {
            CommonKit.showErrorShort(context, "设备未连接网络");
            return;
        }
        NetApi.checkVersion(getToken(context), getUserName(context), new ResultCallBack<CheckVersionModel>() {
            @Override
            public void onStart() {
                super.onStart();
            }

            @Override
            public void onSuccess(int statusCode, Headers headers, CheckVersionModel model) {
                super.onSuccess(statusCode, headers, model);
                if (model != null) {
                    switch (model.code) {
                        case Constants.API.API_OK:
                            RLog.d(model.toString());
                            if (model.appVersion > CommonKit.getAppVersionCode(context)) {
                                RLog.d("发现新版本");
                                downloadAppApk(model.appFileName, context);
                            } else {
                                CommonKit.showOkShort(context, "已经是最新版本");
                            }
                            break;
                        case Constants.API.API_FAIL:
                            RLog.d("网络连接失败");
                            CommonKit.showErrorShort(context, "网络连接失败");
                            break;
                        default:
                            if (model.info != null) {
                                RLog.d(model.info);
                                CommonKit.showErrorShort(context, model.info);
                            } else {
                                RLog.d("网络连接失败");
                                CommonKit.showErrorShort(context, "网络连接失败");
                            }
                            break;
                    }
                }
            }

            @Override
            public void onFailure(int statusCode, Request request, Exception e) {
                super.onFailure(statusCode, request, e);
                RLog.d("网络连接失败");
                CommonKit.showErrorShort(context, "网络连接失败");
            }
        });
    }

    /**
     * 下载Apk
     *
     * @param appFireName
     */
    public static void downloadAppApk(final String appFireName, final BaseActivity context) {
        if (!CommonKit.isNetworkAvailable(context)) {
            CommonKit.showErrorShort(context, "设备未连接网络");
            return;
        }
        final String SDCard = Environment.getExternalStorageDirectory() + "/androidex";
        Log.v("downloadFile", "File path: " + SDCard);
        NetApi.downloadAppApk(getToken(context), SDCard, appFireName, new ResultCallBack() {
            @Override
            public void onStart() {
                super.onStart();
                RLog.d("开始下载新版本");
                CommonKit.showOkShort(context, "开始下载新版本");
            }

            @Override
            public void onSuccess(int statusCode, Headers headers, Object model) {
                super.onSuccess(statusCode, headers, model);
                RLog.d("下载完成");
                CommonKit.installNormal(context, SDCard + "/" + appFireName);
            }

            @Override
            public void onProgress(long bytesWritten, long totalSize) {
                super.onProgress(bytesWritten, totalSize);
                RLog.d("bytesWritten=" + bytesWritten + "\ntotalSize=" + totalSize);
            }

            @Override
            public void onFailure(int statusCode, Request request, Exception e) {
                super.onFailure(statusCode, request, e);
                RLog.d("下载失败" + e.getMessage());
            }
        });
    }
}
