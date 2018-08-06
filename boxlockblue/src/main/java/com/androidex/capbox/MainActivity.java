package com.androidex.capbox;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.acker.simplezxing.activity.CaptureActivity;
import com.androidex.capbox.base.BaseActivity;
import com.androidex.capbox.base.UserBaseActivity;
import com.androidex.capbox.base.imageloader.UILKit;
import com.androidex.capbox.data.Event;
import com.androidex.capbox.data.cache.SharedPreTool;
import com.androidex.capbox.data.net.NetApi;
import com.androidex.capbox.data.net.base.L;
import com.androidex.capbox.data.net.base.ResultCallBack;
import com.androidex.capbox.module.ActionItem;
import com.androidex.capbox.module.BoxDeviceModel;
import com.androidex.capbox.service.MyBleService;
import com.androidex.capbox.ui.activity.AddDeviceActivity;
import com.androidex.capbox.ui.activity.ConnectDeviceListActivity;
import com.androidex.capbox.ui.activity.SettingActivity;
import com.androidex.capbox.ui.activity.TypeOfAlarmActivity;
import com.androidex.capbox.ui.activity.WebViewActivity;
import com.androidex.capbox.ui.fragment.BoxListFragment;
import com.androidex.capbox.ui.fragment.LockFragment;
import com.androidex.capbox.ui.fragment.MapFragment;
import com.androidex.capbox.ui.fragment.WatchListFragment;
import com.androidex.capbox.ui.view.CircleImageView;
import com.androidex.capbox.ui.view.TitlePopup;
import com.androidex.capbox.ui.widget.ThirdTitleBar;
import com.androidex.capbox.utils.CommonKit;
import com.androidex.capbox.utils.Constants;
import com.androidex.capbox.utils.NetApiUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.Bind;
import butterknife.OnClick;
import okhttp3.Headers;
import okhttp3.Request;

import static com.androidex.capbox.data.cache.SharedPreTool.LOGIN_STATUS;
import static com.androidex.capbox.utils.Constants.EXTRA_BOX_NAME;
import static com.androidex.capbox.utils.Constants.EXTRA_BOX_UUID;
import static com.androidex.capbox.utils.Constants.EXTRA_ITEM_ADDRESS;
import static com.androidex.capbox.utils.Constants.EXTRA_USER_HEAD;
import static com.androidex.boxlib.cache.SharedPreTool.IS_OPEN_LOCKSCREEN;

public class MainActivity extends BaseActivity implements OnClickListener, CompoundButton.OnCheckedChangeListener {
    public static String TAG = "MainActivity";
    @Bind(R.id.titlebar)
    ThirdTitleBar titleBar;
    @Bind(R.id.drawerlayout)
    DrawerLayout drawerlayout;
    @Bind(R.id.tv_versionNum)
    TextView tv_versionNum;
    @Bind(R.id.tv_cacheSize)
    TextView tv_cacheSize;
    @Bind(R.id.iv_head)
    CircleImageView iv_head;
    @Bind(R.id.tv_username)
    TextView tv_username;
    @Bind(R.id.setting_distance)
    Spinner setting_distance;
    @Bind(R.id.tb_alarm)
    ToggleButton tb_alarm;
    @Bind(R.id.tb_lockscreen)
    ToggleButton tb_lockscreen;
    @Bind(R.id.main_viewpager)
    ViewPager viewPager;
    @Bind(R.id.v_boxlist)
    View v_boxlist;
    @Bind(R.id.v_watchlist)
    View v_watchlist;
    @Bind(R.id.v_map)
    View v_map;
    @Bind(R.id.iv_watchlist)
    ImageView iv_watchlist;
    @Bind(R.id.iv_boxlist)
    ImageView iv_boxlist;
    @Bind(R.id.iv_map)
    ImageView iv_map;

    public static String username;
    private BoxListFragment boxListFragment;
    private WatchListFragment watchListFragment;
    private MapFragment mapFragment;
    private TitlePopup titlePopup;
    private boolean isToast = false;
    private boolean isToast_lockscreen = false;
    private MainPagerAdapter mainPagerAdapter;

    @Override
    public void initData(Bundle savedInstanceState) {
        registerEventBusSticky();
        username = SharedPreTool.getInstance(context).getStringData(SharedPreTool.PHONE, null);
        initListView();
        initTitleBar();
        initPager();
    }

    private void initTitleBar() {
        titleBar.getLeftMenu().setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                tv_versionNum.setText(getResources().getString(R.string.about_tv_versionNum) + CommonKit.getVersionName(context));
                tv_username.setText(getUserName());
                updateCache();
                openDrawlayout();
                String head_uri = SharedPreTool.getInstance(context).getStringData(EXTRA_USER_HEAD, null);
                if (head_uri != null) {
                    uploadHead(head_uri);
                }
                if (SharedPreTool.getInstance(context).getBoolData(SharedPreTool.IS_POLICE, true)) {
                    tb_alarm.setChecked(true);
                } else {
                    tb_alarm.setChecked(false);
                }
                if (SharedPreTool.getInstance(context).getBoolData(IS_OPEN_LOCKSCREEN, true)) {
                    tb_lockscreen.setChecked(true);
                } else {
                    tb_lockscreen.setChecked(false);
                }
                int rssiMaxValue = MyBleService.getInstance().getRssiMaxValue();
                settingDistance();//报警距离
                if (rssiMaxValue >= 0) {
                    setting_distance.setSelection(0);
                } else if (rssiMaxValue == -70) {//较近
                    setting_distance.setSelection(1);
                } else if (rssiMaxValue == -80) {//近
                    setting_distance.setSelection(2);
                } else if (rssiMaxValue == -90) {//较远
                    setting_distance.setSelection(3);
                } else if (rssiMaxValue == -98) {//远
                    setting_distance.setSelection(4);
                }
            }
        });
    }

    //报警距离
    private void settingDistance() {
        String[] mItems2 = getResources().getStringArray(R.array.distance);
        ArrayAdapter<String> adapter1 = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, mItems2);
        adapter1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        setting_distance.setAdapter(adapter1);//绑定 Adapter到控件
        setting_distance.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                Log.e(TAG, "pos=" + pos);
                switch (pos) {
                    case 0:
                        MyBleService.getInstance().setRssiMaxValue(0);
                        break;
                    case 1:
                        MyBleService.getInstance().setRssiMaxValue(-70);
                        break;
                    case 2:
                        MyBleService.getInstance().setRssiMaxValue(-80);
                        break;
                    case 3:
                        MyBleService.getInstance().setRssiMaxValue(-90);
                        break;
                    case 4:
                        MyBleService.getInstance().setRssiMaxValue(-98);
                        break;
                    default:
                        MyBleService.getInstance().setRssiMaxValue(0);
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Another interface callback
            }
        });
    }

    /**
     * 上传头像
     *
     * @param filePath 裁剪后的路径
     */
    private void uploadHead(String filePath) {
        if (TextUtils.isEmpty(filePath)) return;
        File file = new File(filePath);
        if (!file.exists()) return;
        SharedPreTool.getInstance(context).setStringData(EXTRA_USER_HEAD, filePath);
        iv_head.setImageURI(Uri.fromFile(file));
    }

    private void initListView() {
        drawerlayout.addDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                Log.i("drawer", slideOffset + "");
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                Log.i("drawer", "抽屉被完全打开了！");
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                Log.i("drawer", "抽屉被完全关闭了！");
            }

            @Override
            public void onDrawerStateChanged(int newState) {
                Log.i("drawer", "drawer的状态：" + newState);
            }
        });
    }

    @OnClick({
            R.id.iv_setting,
            R.id.ll_personelCenter,
            R.id.about_us,
            R.id.ll_connectDevice,
            R.id.message_notification,
            R.id.setting_alarm,
            R.id.ll_searchVersion,
            R.id.ll_clearCache,
            R.id.tv_logout,
    })
    public void clickEvent(View view) {
        closeDrawlayout();
        switch (view.getId()) {
            case R.id.iv_setting://设置
                SettingActivity.lauch(context);
                break;
            case R.id.ll_personelCenter://个人中心
                SettingActivity.lauch(context);
                break;
            case R.id.about_us://关于我们
                String url = "http://www.lockaxial.com/about/aboutandroidex.html";
                if (!TextUtils.isEmpty(url)) {
                    if (!CommonKit.isNetworkAvailable(context)) {
                        CommonKit.showErrorShort(context, "请检查网络");
                        return;
                    }
                    WebViewActivity.lauch(context, url);
                }
                break;
            case R.id.ll_connectDevice://已连接设备
                ConnectDeviceListActivity.lauch(context);
                break;
            case R.id.message_notification://消息通知

                break;
            case R.id.setting_alarm://报警方式
                TypeOfAlarmActivity.lauch(context);
                break;
            case R.id.ll_searchVersion://版本检测
                NetApiUtil.checkVersion(this);
                break;
            case R.id.ll_clearCache://清理缓存
                clearCache();
                break;
            case R.id.tv_logout://退出登陆
                NetApiUtil.userLogout(this);
                removeCacheForSp();//删除缓存
                postSticky(new Event.UserLoginEvent());//登录状态发生改变
                CommonKit.finishActivity(context);
                break;
            default:
                break;
        }
    }

    @OnClick({R.id.ll_watchlist, R.id.ll_boxlist, R.id.ll_map})
    public void onTabChange(View view) {
        switch (view.getId()) {
            case R.id.ll_watchlist:
                viewPager.setCurrentItem(0);
                break;
            case R.id.ll_boxlist:
                viewPager.setCurrentItem(1);
                break;
            case R.id.ll_map:
                viewPager.setCurrentItem(2);
                break;
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
        switch (compoundButton.getId()) {
            case R.id.tb_alarm://报警开关
                if (isChecked) {
                    SharedPreTool.getInstance(context).setBoolData(SharedPreTool.IS_POLICE, true);
                } else {
                    SharedPreTool.getInstance(context).setBoolData(SharedPreTool.IS_POLICE, false);
                    if (!isToast) {
                        isToast = true;
                        return;
                    }
                }
                if (SharedPreTool.getInstance(context).getBoolData(SharedPreTool.IS_POLICE, true)) {
                    CommonKit.showOkShort(context, "打开报警开关成功");
                } else {
                    CommonKit.showOkShort(context, "关闭报警开关成功");
                }
                break;
            case R.id.tb_lockscreen://锁屏开关
                if (isChecked) {
                    SharedPreTool.getInstance(context).setBoolData(IS_OPEN_LOCKSCREEN, true);
                } else {
                    SharedPreTool.getInstance(context).setBoolData(IS_OPEN_LOCKSCREEN, false);
                    if (!isToast_lockscreen) {
                        isToast_lockscreen = true;
                        return;
                    }
                }
                if (SharedPreTool.getInstance(context).getBoolData(IS_OPEN_LOCKSCREEN, true)) {
                    CommonKit.showOkShort(context, "锁屏开关打开");
                } else {
                    CommonKit.showOkShort(context, "锁屏功能关闭");
                }
                break;
        }
    }

    /**
     * 删除缓存本地的账号和密码
     */
    private void removeCacheForSp() {
        SharedPreTool.getInstance(context).setBoolData(LOGIN_STATUS, false);//设置登录标识为false
        //SharedPreTool.getInstance(context).remove(SharedPreTool.TOKEN);
        SharedPreTool.getInstance(context).remove(SharedPreTool.PHONE);
        SharedPreTool.getInstance(context).remove(SharedPreTool.PASSWORD);
        SharedPreTool.getInstance(context).remove(SharedPreTool.AUTOMATIC_LOGIN);
        SharedPreTool.getInstance(context).remove(SharedPreTool.IS_REGISTED);
    }

    /**
     * 清除缓存
     */
    private void clearCache() {
        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... params) {
                UILKit.getLoader().clearDiskCache();
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                super.onPostExecute(result);
                CommonKit.showOkShort(context, "成功清除缓存");
                updateCache();
            }
        }.execute();
    }

    /**
     * 打开左侧侧边栏
     */
    private void openDrawlayout() {
        if (drawerlayout != null && !drawerlayout.isDrawerOpen(Gravity.START)) {
            drawerlayout.openDrawer(Gravity.LEFT);
        }
    }

    /**
     * 关闭左侧侧边栏
     */
    private void closeDrawlayout() {
        if (drawerlayout != null && drawerlayout.isDrawerOpen(Gravity.START)) {
            drawerlayout.closeDrawer(Gravity.START);
        }
    }

    @Override
    public void setListener() {
        tb_alarm.setOnCheckedChangeListener(this);
        tb_lockscreen.setOnCheckedChangeListener(this);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }
            @Override
            public void onPageSelected(int position) {
                switch (position) {
                    case 0:
                        titleBar.setTitle(getString(R.string.main_tv_watchlist));
                        updateImageView(v_watchlist);
                        iv_watchlist.setImageResource(R.mipmap.watch_white);
                        break;
                    case 1:
                        initBoxTitleBar();
                        updateImageView(v_boxlist);
                        iv_boxlist.setImageResource(R.mipmap.box_white);
                        break;
                    case 2:
                        titleBar.setTitle(getString(R.string.main_tv_map));
                        updateImageView(v_map);
                        iv_map.setImageResource(R.mipmap.find_white);
                        break;
                }
            }
            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
    }

    /**
     * 初始化fragment
     */
    private void initPager() {
        if (mainPagerAdapter == null) {
            mainPagerAdapter = new MainPagerAdapter(getSupportFragmentManager());
        }
        viewPager.setAdapter(mainPagerAdapter);
        viewPager.setOffscreenPageLimit(0);
        viewPager.setCurrentItem(1);
    }

    private void updateImageView(View view) {
        v_boxlist.setVisibility(View.GONE);
        v_watchlist.setVisibility(View.GONE);
        v_map.setVisibility(View.GONE);
        view.setVisibility(View.VISIBLE);
        iv_watchlist.setImageResource(R.mipmap.watch_gray);
        iv_boxlist.setImageResource(R.mipmap.box_gray);
        iv_map.setImageResource(R.mipmap.find_gray);
    }

    public class MainPagerAdapter extends FragmentPagerAdapter {

        public MainPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return getFragment(position);
        }

        @Override
        public int getCount() {
            return 3;
        }
    }

    private Fragment getFragment(int pos) {
        Fragment fragment = null;
        switch (pos) {
            case 0:
                if (watchListFragment == null) {
                    watchListFragment = WatchListFragment.getInstance();
                }
                fragment = watchListFragment;
                break;
            case 1:
                if (boxListFragment == null) {
                    boxListFragment = BoxListFragment.getInstance();
                }
                fragment = boxListFragment;
                break;
            case 2:
                if (mapFragment == null) {
                    mapFragment = MapFragment.getInstance();
                }
                fragment = mapFragment;
                break;
        }
        return fragment;
    }

    @Override
    public void onClick(View v) {
    }

    /**
     * 切换到密运箱页面时更改titlebar
     */
    private void initBoxTitleBar() {
        titleBar.setTitle(getString(R.string.main_tv_boxlist));
        titleBar.getRightTv().setVisibility(View.GONE);
        // 实例化标题栏弹窗
        titlePopup = new TitlePopup(context, RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        // 给标题栏弹窗添加子类
        titlePopup.addAction(new ActionItem(context, "添加设备", R.drawable.finish_carry));
        titlePopup.addAction(new ActionItem(context, "扫一扫", R.mipmap.scan_gray));
        titlePopup.addAction(new ActionItem(context, "查找设备", R.mipmap.map_gray));
        titlePopup.setItemOnClickListener(new TitlePopup.OnItemOnClickListener() {
            @Override
            public void onItemClick(ActionItem item, int position) {
                switch (position) {
                    case 0:
                        AddDeviceActivity.lauch(context, null);
                        break;
                    case 1:
                        Intent intent = new Intent(context, CaptureActivity.class);
                        startActivityForResult(intent, CaptureActivity.REQ_CODE);// ,//Activity.RESULT_FIRST_USER
                        break;
                    case 2:
                        viewPager.setCurrentItem(2);
                        break;
                    default:
                        break;
                }
            }
        });
        titleBar.getRightIv().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                titlePopup.show(view);
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent home = new Intent(Intent.ACTION_MAIN);
        home.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        home.addCategory(Intent.CATEGORY_HOME);
        startActivity(home);
    }

    /**
     * 更新缓存
     */
    private void updateCache() {
        getSpaceSize(UILKit.getLoader().getDiskCache().getDirectory());
    }

    private void getSpaceSize(File file) {
        new AsyncTask<File, Void, Float>() {

            @Override
            protected Float doInBackground(File... params) {
                File file = params[0];
                if (file.exists() && file.isDirectory()) {
                    return CommonKit.getFolderSize(file.getAbsolutePath()) / 1024.0f;
                }
                return 0f;
            }

            @Override
            protected void onPostExecute(Float result) {
                if (result != 0) {
                    tv_cacheSize.setText(String.format("%.2f", result) + "MB");
                } else {
                    tv_cacheSize.setText("无缓存");
                }
                super.onPostExecute(result);
            }
        }.execute(file);
    }

    /**
     * 退出主界面
     *
     * @param event
     */
    public void onEvent(Event.UserLoginEvent event) {
        doAfterLogin(new UserBaseActivity.CallBackAction() {
            @Override
            public void action() {

            }
        });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            Intent home = new Intent(Intent.ACTION_MAIN);
            home.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            home.addCategory(Intent.CATEGORY_HOME);
            startActivity(home);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        unregisterEventBus();
    }

    public static void lauch(Activity activity) {
        CommonKit.startActivity(activity, MainActivity.class, null, true);
    }

    @Override
    public int getLayoutId() {
        return R.layout.activity_main;
    }
}
