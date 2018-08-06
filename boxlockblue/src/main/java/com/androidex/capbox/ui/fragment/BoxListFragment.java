package com.androidex.capbox.ui.fragment;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.androidex.boxlib.service.BleService;
import com.androidex.capbox.MainActivity;
import com.androidex.capbox.R;
import com.androidex.capbox.base.BaseFragment;
import com.androidex.capbox.data.net.NetApi;
import com.androidex.capbox.data.net.base.L;
import com.androidex.capbox.data.net.base.ResultCallBack;
import com.androidex.capbox.module.BoxDeviceModel;
import com.androidex.capbox.service.MyBleService;
import com.androidex.capbox.ui.activity.BoxDetailActivity;
import com.androidex.capbox.ui.activity.LoginActivity;
import com.androidex.capbox.ui.adapter.BoxListAdapter;
import com.androidex.capbox.ui.adapter.BoxListAdapter2;
import com.androidex.capbox.ui.widget.ThirdTitleBar;
import com.androidex.capbox.utils.CommonKit;
import com.androidex.capbox.utils.Constants;
import com.androidex.capbox.utils.RLog;
import com.e.ble.bean.BLEDevice;
import com.e.ble.scan.BLEScanCfg;
import com.e.ble.scan.BLEScanListener;
import com.e.ble.util.BLEError;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.Bind;
import okhttp3.Headers;
import okhttp3.Request;

import static com.androidex.boxlib.utils.BleConstants.BLE.BLE_CONN_DIS;
import static com.androidex.boxlib.utils.BleConstants.BLE.BLE_CONN_FAIL;
import static com.androidex.boxlib.utils.BleConstants.BLE.BLE_CONN_SUCCESS;
import static com.androidex.boxlib.utils.BleConstants.BLE.BLE_CONN_SUCCESS_ALLCONNECTED;
import static com.androidex.boxlib.utils.BleConstants.BLECONSTANTS.BLECONSTANTS_ADDRESS;
import static com.androidex.capbox.utils.Constants.EXTRA_BOX_NAME;
import static com.androidex.capbox.utils.Constants.EXTRA_BOX_UUID;
import static com.androidex.capbox.utils.Constants.EXTRA_ITEM_ADDRESS;

/**
 * 箱体列表
 *
 * @author liyp
 * @editTime 2017/9/27
 */

public class BoxListFragment extends BaseFragment {
    private static final String TAG = "BoxListFragment";
    @Bind(R.id.deviceListView)
    ListView deviceList;
    @Bind(R.id.swipe_container)
    SwipeRefreshLayout swipe_container;

    private BleBroadCast bleBroadCast;
    List<Map<String, String>> mylist = new ArrayList<>();
    private static final long SCAN_PERIOD = 10000;
    private boolean mScanning = false;//控制蓝牙扫描
    private BoxListAdapter2 boxListAdapter;
    private FragmentManager fragmentManager;
    private FragmentTransaction transaction;
    private static BoxListFragment fragment;

    @Override
    public void initData() {
        iniRefreshView();
        initFragmentManager();
        initListView();
        initBle();//蓝牙连接
    }

    private void initFragmentManager() {
        fragmentManager = getChildFragmentManager();
    }

    /**
     * 初始化两个列表的ListView
     */
    private void initListView() {
        //已绑定设备的适配器
        boxListAdapter = new BoxListAdapter2(context, mylist, new BoxListAdapter2.IClick() {

            @Override
            public void listViewItemClick(int position, View v) {
                switch (v.getId()) {
                    case R.id.rl_normal:
                        Bundle bundle = new Bundle();
                        bundle.putString(EXTRA_ITEM_ADDRESS, mylist.get(position).get(EXTRA_ITEM_ADDRESS));
                        bundle.putString(EXTRA_BOX_NAME,  mylist.get(position).get(EXTRA_BOX_NAME));
                        bundle.putString(EXTRA_BOX_UUID,  mylist.get(position).get(EXTRA_BOX_UUID));
                        LockFragment lockFragment = new LockFragment();
                        lockFragment.setArguments(bundle);
                        transaction = fragmentManager.beginTransaction();
                        transaction.replace(R.id.ll_boxlist, lockFragment);
                        transaction.commit();
                        break;
                    default:
                        break;
                }
            }
        });
        //设置已绑定列表的适配器
        deviceList.setAdapter(boxListAdapter);
    }

    private void iniRefreshView() {
        swipe_container.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                new Handler().postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        boxlist();
                        swipe_container.setRefreshing(false);
                    }
                }, 1000);
            }
        });
    }

    /**
     * 蓝牙初始化
     */
    private void initBle() {
        initBleReceiver();//初始化蓝牙广播
        // 初始化蓝牙adapter
        if (!mBtAdapter.isEnabled()) {
            mBtAdapter.enable();//打开蓝牙
            RLog.d("打开蓝牙");
        }
    }

    /**
     * 开始扫描
     */
    private void scanLeDevice(final String address, final String deviceUUID) {
        showProgress("搜索设备...");
        BLEScanCfg scanCfg = new BLEScanCfg.ScanCfgBuilder(SCAN_PERIOD).builder();
        MyBleService.get().startScanner(scanCfg, new BLEScanListener() {
            boolean isScanDevice;//是否扫描到设备

            @Override
            public void onScannerStart() {
                isScanDevice = false;
            }

            @Override
            public void onScanning(BLEDevice device) {
                if (device.getMac().equals(address)) {
                    showProgress("搜索到设备...");
                    stopScanLe();
                    isScanDevice = true;
                    synchronized (this) {
                        RLog.d("搜索到设备mScanning=" + mScanning);
                        Bundle bundle = new Bundle();
                        bundle.putString("name", device.getName());
                        bundle.putString("uuid", deviceUUID);
                        bundle.putString("address", address);
                        BoxDetailActivity.lauch(getActivity(), bundle);
                    }
                }
            }

            @Override
            public void onScannerStop() {
                stopScanLe();
                RLog.e("扫描结束");
                if (!isScanDevice) {
                    CommonKit.showErrorShort(context, "未搜索到设备");
                }
            }

            @Override
            public void onScannerError(int errorCode) {
                stopScanLe();
                if (errorCode == BLEError.BLE_CLOSE) {
                    CommonKit.showErrorShort(context, "蓝牙未打开，请打开蓝牙后重试");
                } else {
                    CommonKit.showErrorShort(context, "扫描出现异常");
                }
            }
        });
    }

    /**
     * 注册设备连接广播
     */
    private void initBleReceiver() {
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BLE_CONN_SUCCESS);
        intentFilter.addAction(BLE_CONN_SUCCESS_ALLCONNECTED);
        intentFilter.addAction(BLE_CONN_FAIL);
        intentFilter.addAction(BLE_CONN_DIS);
        intentFilter.addAction(Constants.BLE.ACTION_UUID);//获取UUID
        bleBroadCast = new BleBroadCast();
        context.registerReceiver(bleBroadCast, intentFilter);
    }

    private void stopScanLe() {
        disProgress();
    }

    public void boxlist() {
        NetApi.boxlist(getToken(), ((MainActivity) context).username, new ResultCallBack<BoxDeviceModel>() {
            @Override
            public void onSuccess(int statusCode, Headers headers, BoxDeviceModel model) {
                super.onSuccess(statusCode, headers, model);
                if (model != null) {
                    switch (model.code) {
                        case Constants.API.API_OK:
                            mylist.clear();
                            for (BoxDeviceModel.device device : model.devicelist) {
                                Map<String, String> map = new HashMap<>();
                                map.put(EXTRA_BOX_NAME, device.boxName);
                                map.put(EXTRA_BOX_UUID, device.uuid);
                                map.put(EXTRA_ITEM_ADDRESS, device.mac);
                                map.put("deviceStatus", "" + device.deviceStatus);
                                map.put("isdefault", "" + device.isDefault);
                                map.put("isOnLine", "" + device.isOnLine);
                                mylist.add(map);
                            }
                            if (model.devicelist.size() > 0) {
                                L.e(TAG + "刷新列表");
                                boxListAdapter.notifyDataSetChanged();
                            } else {
                                CommonKit.showErrorShort(context, "请绑定箱体");
                                L.e(TAG + "刷新列表无数据");
                            }
                            showProgress("刷新完成");
                            break;
                        case Constants.API.API_FAIL:
                            CommonKit.showErrorShort(context, "账号在其他地方登录");
                            LoginActivity.lauch(context);
                            showProgress("刷新失败");
                            break;
                        case Constants.API.API_NOPERMMISION:
                            CommonKit.showErrorShort(context, "获取设备列表失败");
                            showProgress("刷新失败");
                            break;
                        default:
                            disProgress();
                            if (model.info != null) {
                                CommonKit.showErrorShort(context, model.info);
                            }
                            break;
                    }
                }
                disProgress();
            }

            @Override
            public void onFailure(int statusCode, Request request, Exception e) {
                super.onFailure(statusCode, request, e);
                if (context != null) {
                    showProgress("刷新列表失败");
                }
                disProgress();
                CommonKit.showErrorShort(context, "网络连接异常");
            }

            @Override
            public void onStart() {
                super.onStart();
            }
        });
    }

    @Override
    public void setListener() {
    }

    @Override
    public void onClick(View view) {

    }

    /**
     * 接收广播
     */
    public class BleBroadCast extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String mac = intent.getStringExtra(BLECONSTANTS_ADDRESS);
            switch (intent.getAction()) {
                case BLE_CONN_SUCCESS://连接成功
                case BLE_CONN_SUCCESS_ALLCONNECTED://重复连接
                    BleService.get().enableNotify(mac);
                    showProgress("连接成功...");
                    break;

                case BLE_CONN_DIS://断开连接
                    disProgress();
                    setLostAlarm("");//防丢报警设置
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        boxlist();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (bleBroadCast != null) {
            context.unregisterReceiver(bleBroadCast);
        }
    }

    public static BoxListFragment getInstance() {
        if (fragment ==null){
            fragment = new BoxListFragment();
        }
        return fragment;
    }

    @Override
    public int getLayoutId() {
        return R.layout.fragment_boxlist;
    }
}
