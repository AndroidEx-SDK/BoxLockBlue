package com.androidex.capbox.ui.adapter;

import android.content.Context;
import android.content.Intent;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.TextView;

import com.androidex.boxlib.modules.ServiceBean;
import com.androidex.capbox.R;
import com.androidex.capbox.base.BaseActivity;
import com.androidex.capbox.data.Event;
import com.androidex.capbox.data.cache.SharedPreTool;
import com.androidex.capbox.data.net.NetApi;
import com.androidex.capbox.data.net.base.ResultCallBack;
import com.androidex.capbox.module.BaseModel;
import com.androidex.capbox.service.MyBleService;
import com.androidex.capbox.utils.CalendarUtil;
import com.androidex.capbox.utils.CommonKit;
import com.androidex.capbox.utils.Constants;
import com.androidex.capbox.utils.RLog;

import java.util.List;
import java.util.Map;

import de.greenrobot.event.EventBus;
import okhttp3.Headers;
import okhttp3.Request;

import static com.androidex.capbox.provider.WidgetProvider.ACTION_UPDATE_ALL;
import static com.androidex.capbox.utils.Constants.EXTRA_BOX_NAME;
import static com.androidex.capbox.utils.Constants.EXTRA_BOX_UUID;
import static com.androidex.capbox.utils.Constants.EXTRA_ITEM_ADDRESS;

/**
 * 已绑定设备的适配器
 *
 * @author liyp
 * @editTime 2017/9/30
 */

public class BoxListAdapter2 extends BaseAdapter {
    private static final String TAG = "BoxListAdapter";
    private List<Map<String, String>> mContentList;
    private LayoutInflater mInflater;
    private IClick mListener;
    private final Context mContext;

    public BoxListAdapter2(Context context, List<Map<String, String>> contentList, IClick listener) {
        mContext = context;
        mContentList = contentList;
        mInflater = LayoutInflater.from(context);
        mListener = listener;
    }

    /**
     * 根据位置从数据集中移除一条数据 并更新listview
     *
     * @param position
     */
    public void removeItem(int position) {
        if (position >= 0 && position < mContentList.size()) {
            mContentList.remove(position);
            notifyDataSetChanged();
        }
    }

    @Override
    public int getCount() {
        return mContentList.size();
    }

    @Override
    public Object getItem(int position) {
        return mContentList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.listitem_device2, null);
            if (holder == null) {
                holder = new ViewHolder();
            }
            holder.itemHorizontalScrollView = convertView.findViewById(R.id.hsv);
            /**
             * 操作按钮层
             */
            holder.normalItemContentLayout = convertView.findViewById(R.id.rl_normal);
            holder.deviceName = convertView.findViewById(R.id.device_name);
            holder.tv_unbind = convertView.findViewById(R.id.tv_unbind);
            holder.iv_img = convertView.findViewById(R.id.iv_img);
            holder.iv_lock = convertView.findViewById(R.id.iv_lock);
            holder.iv_ble = convertView.findViewById(R.id.iv_ble);

            ViewGroup.LayoutParams lp = holder.normalItemContentLayout.getLayoutParams();
            WindowManager manager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
            DisplayMetrics dm = new DisplayMetrics();
            manager.getDefaultDisplay().getMetrics(dm);
            int width2 = dm.widthPixels;//宽
            lp.width = width2;
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        final String uuid = mContentList.get(position).get(EXTRA_BOX_UUID);
        final String mac = mContentList.get(position).get(EXTRA_ITEM_ADDRESS);
        String name = mContentList.get(position).get(EXTRA_BOX_NAME);
        String isOnLine = mContentList.get(position).get("isOnLine");
        String deviceStatus = mContentList.get(position).get("deviceStatus");
        String deviceName = CalendarUtil.getName(name, mac);
        holder.deviceName.setText(deviceName);

        holder.normalItemContentLayout.setOnClickListener(mListener);
        holder.normalItemContentLayout.setTag(position);
        if (isOnLine != null&&isOnLine.equals("0")) {
            /**
             {"code":0,"devicelist":[
             {"boxName":"Box66","deviceStatus":"1","isDefault":"0",
             "isOnLine":1,"lat":"22.619786","lon":"114.083282","mac":"B0:91:22:69:41:66","uuid":"B09122694166000000008DD041190000"},
             {"boxName":"Box6E","deviceStatus":"1","isDefault":"0",
             "isOnLine":1,"lat":"22.625753","lon":"114.081701","mac":"B0:91:22:69:43:6E","uuid":"B0912269436E0000000013D143190000"}]}
             */
            holder.iv_img.setImageResource(R.mipmap.ic_box_blue);
            holder.iv_lock.setImageResource(R.mipmap.lock_close1_blue);
            holder.iv_ble.setImageResource(R.mipmap.starts_connect2_blue);
        }else {
            holder.iv_img.setImageResource(R.mipmap.ic_box_gray);
            holder.iv_lock.setImageResource(R.mipmap.lock_close1_gray);
            holder.iv_ble.setImageResource(R.mipmap.starts_connect2_gray);
        }
        // 设置监听事件
        convertView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_UP:
                        // 获得ViewHolder
                        ViewHolder viewHolder = (ViewHolder) v.getTag();
                        // 获得HorizontalScrollView滑动的水平方向值.
                        int scrollX = viewHolder.itemHorizontalScrollView.getScrollX();
                        // 获得操作区域的长度
                        int actionLayoutWidth = viewHolder.tv_unbind.getWidth();
                        // 注意使用smoothScrollTo,这样效果看起来比较圆滑,不生硬
                        // 如果水平方向的移动值<操作区域的长度的一半,就复原
                        if (scrollX < actionLayoutWidth / 2) {
                            viewHolder.itemHorizontalScrollView.smoothScrollTo(0, 0);
                        } else {// 否则的话显示操作区域
                            viewHolder.itemHorizontalScrollView.smoothScrollTo(actionLayoutWidth, 0);
                        }
                        return true;
                }
                return false;
            }
        });
        // 这里防止删除一条item后,ListView处于操作状态,直接还原
        if (holder.itemHorizontalScrollView.getScrollX() != 0) {
            holder.itemHorizontalScrollView.scrollTo(0, 0);
        }

        holder.tv_unbind.setOnClickListener(new View.OnClickListener() {//删除

            @Override
            public void onClick(View v) {
                if (!CommonKit.isNetworkAvailable(mContext)) {
                    CommonKit.showErrorShort(mContext, "设备未连接网络");
                    return;
                }
                String token = SharedPreTool.getInstance(mContext).getStringData(SharedPreTool.TOKEN, null);
                if (token == null) {
                    CommonKit.showErrorShort(mContext, "账号异常");
                    token = "";
                }
                NetApi.relieveBoxBind(token, ((BaseActivity) mContext).getUserName(), uuid, mac, new ResultCallBack<BaseModel>() {
                    @Override
                    public void onStart() {
                        super.onStart();
                    }

                    @Override
                    public void onSuccess(int statusCode, Headers headers, BaseModel model) {
                        super.onSuccess(statusCode, headers, model);
                        if (model != null) {
                            switch (model.code) {
                                case Constants.API.API_OK:
                                    CommonKit.showOkShort(mContext, mContext.getString(R.string.hint_unbind_ok));
                                    removeItem(position);
                                    ServiceBean device = MyBleService.getInstance().getConnectDevice(mac);
                                    if (device != null) {
                                        device.setActiveDisConnect(true);
                                        MyBleService.getInstance().disConnectDevice(mac);
                                    }
                                    SharedPreTool.getInstance(mContext).remove(mac);
                                    EventBus.getDefault().postSticky(new Event.BoxRelieveBind());
                                    mContext.sendBroadcast(new Intent(ACTION_UPDATE_ALL));//发送广播给桌面插件，更新列表
                                    break;
                                case Constants.API.API_FAIL:
                                    CommonKit.showErrorShort(mContext, "解绑失败");
                                    break;
                                case Constants.API.API_NOPERMMISION:
                                    if (model.info != null) {
                                        CommonKit.showErrorShort(mContext, model.info);
                                    } else {
                                        CommonKit.showErrorShort(mContext, "无权限");
                                    }
                                    break;
                                default:
                                    if (model.info != null) {
                                        CommonKit.showErrorShort(mContext, model.info);
                                    }
                                    break;
                            }
                        }
                    }

                    @Override
                    public void onFailure(int statusCode, Request request, Exception e) {
                        super.onFailure(statusCode, request, e);
                    }
                });

            }
        });
        return convertView;
    }

    public class ViewHolder {
        private HorizontalScrollView itemHorizontalScrollView;
        private View normalItemContentLayout;
        public TextView deviceName;
        public ImageView iv_img;
        public ImageView iv_lock;
        public ImageView iv_ble;
        private TextView tv_unbind;
    }

    public abstract static class IClick implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            listViewItemClick((Integer) v.getTag(), v);
        }

        public abstract void listViewItemClick(int position, View v);

    }
}
