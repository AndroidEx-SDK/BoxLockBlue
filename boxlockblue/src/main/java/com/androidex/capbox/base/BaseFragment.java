package com.androidex.capbox.base;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.androidex.capbox.R;
import com.androidex.capbox.data.Event;
import com.androidex.capbox.data.cache.CacheManage;
import com.androidex.capbox.data.cache.SharedPreTool;
import com.androidex.capbox.module.UserModel;
import com.androidex.capbox.ui.activity.LoginActivity;
import com.androidex.capbox.utils.BuildConfig;
import com.androidex.capbox.utils.CommonKit;
import com.androidex.capbox.utils.Constants;
import com.androidex.capbox.utils.Dialog;
import com.androidex.capbox.utils.DialogUtils;
import com.androidex.capbox.utils.SystemUtil;

import butterknife.ButterKnife;
import de.greenrobot.event.EventBus;

/**
 * @author liyp
 * @version 1.0.0
 * @description Fragment基类
 * @createTime 2015/9/19
 * @editTime
 * @editor
 */
public abstract class BaseFragment extends Fragment implements View.OnClickListener {
    protected View rootView;
    protected LayoutInflater layoutInflater;
    protected Activity context;
    private android.app.Dialog dialog;
    private android.app.Dialog mLostAlarmDialog;//防丢弹框Dialog
    protected BluetoothAdapter mBtAdapter;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        layoutInflater = inflater;
        if (rootView == null) {
            context = getActivity();
            rootView = inflater.inflate(getLayoutId(), null);
            ButterKnife.bind(this, rootView);
        } else {
            ViewGroup viewGroup = (ViewGroup) rootView.getParent();
            if (viewGroup != null) {
                viewGroup.removeView(rootView);
            }
        }
        return rootView;
    }


    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setListener();
        initData();
    }

    public abstract void initData();

    public abstract void setListener();

    public abstract int getLayoutId();

    protected void setVisible(View view) {
        view.setVisibility(View.VISIBLE);
    }

    protected void setGone(View view) {
        view.setVisibility(View.GONE);
    }

    protected void setInvisible(View view) {
        view.setVisibility(View.INVISIBLE);
    }

    protected int getColor(int resId) {
        return context.getResources().getColor(resId);
    }

    protected Drawable getDrawable(int resId) {
        return context.getResources().getDrawable(resId);
    }

    protected void setClick(View view) {
        view.setOnClickListener(this);
    }

    protected View getRootView() {
        return rootView;
    }

    protected void registerEventBus() {
        EventBus.getDefault().register(this);
    }

    protected void registerEventBusSticky() {
        EventBus.getDefault().registerSticky(this);
    }

    protected void unregisterEventBus() {
        EventBus.getDefault().unregister(this);
    }

    protected void post(Object event) {
        EventBus.getDefault().post(event);
    }

    protected void postSticky(Object event) {
        EventBus.getDefault().postSticky(event);
    }

    protected String getToken() {
        String token = SharedPreTool.getInstance(context).getStringData(SharedPreTool.TOKEN, null);
        return token;
    }

    protected void setToken(String token) {
        SharedPreTool.getInstance(context).setStringData(SharedPreTool.TOKEN, token);
    }

    public String getUserName() {
        String username = SharedPreTool.getInstance(context).getStringData(SharedPreTool.PHONE, null);
        if (username == null) {
            CommonKit.showErrorShort(context, "账号异常");
            LoginActivity.lauch(context);
            postSticky(new Event.UserLoginEvent());
            return "";
        }
        return username;
    }

    public void setUsername(String username) {
        SharedPreTool.getInstance(context).setStringData(SharedPreTool.PHONE, username);
    }

    /**
     * 设置防丢报警方式
     */
    protected void setLostAlarm(String deviceName) {
        showLostAlarmDialog(deviceName);
        SystemUtil.startVibrate(context, true);//true:循环震动，false:震动一次
    }

    /**
     * 停止显示报警dialog
     */
    protected void closeLostAlarm() {
        SystemUtil.stopVibrate(context);//停止震动
        //SystemUtil.stopPlayMediaPlayer();//停止铃声
        SystemUtil.stopPlayRaw();//停止铃声
        if (mLostAlarmDialog != null && mLostAlarmDialog.isShowing()) {
            mLostAlarmDialog.dismiss();
        }
    }

    /**
     * 防丢报警Dialog显示
     */
    private void showLostAlarmDialog(String deviceName) {
        //就一个确定按钮
        mLostAlarmDialog = Dialog.showRadioDialog(context, deviceName
                + getResources().getString(R.string.itemfragment_dialog_lost), new Dialog.DialogClickListener() {
            @Override
            public void confirm() {
                closeLostAlarm();
            }

            @Override
            public void cancel() {

            }
        });
    }

    /**
     * 获取当前登录的用户
     *
     * @return
     */
    protected UserModel getLoginedUser() {
        return CacheManage.getFastCache().get(Constants.PARAM.CACHE_KEY_CUR_LOGIN_USER, UserModel.class);
    }

    /**
     * 显示等待框
     *
     * @param msg
     */
    protected void showProgress(String msg) {
        if (dialog == null) {
            dialog = DialogUtils.createDialog(context, msg);
        }
        TextView tv = (TextView) dialog.findViewById(R.id.msg);
        tv.setText(msg);
        if (!dialog.isShowing()) {
            dialog.setCancelable(true);
            dialog.show();
        }
    }

    protected void disProgress() {
        if (dialog == null) {
            return;
        }
        if (dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        closeLostAlarm();
        /**
         * 二、3、打开蓝牙
         * 获取到BluetoothAdapter之后，还需要判断蓝牙是否打开。
         * 如果没打开，需要让用户打开蓝牙：
         */
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!mBtAdapter.isEnabled()) {
            mBtAdapter.enable();
        }
        if (!BuildConfig.DEBUG) {

        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (!BuildConfig.DEBUG) {

        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
}
