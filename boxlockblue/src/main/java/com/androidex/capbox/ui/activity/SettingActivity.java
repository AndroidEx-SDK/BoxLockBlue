package com.androidex.capbox.ui.activity;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.androidex.capbox.R;
import com.androidex.capbox.base.UserBaseActivity;
import com.androidex.capbox.base.imageloader.UILKit;
import com.androidex.capbox.callback.ItemClickCallBack;
import com.androidex.capbox.data.Event;
import com.androidex.capbox.data.cache.SharedPreTool;
import com.androidex.capbox.data.net.NetApi;
import com.androidex.capbox.data.net.base.ResultCallBack;
import com.androidex.capbox.module.BaseModel;
import com.androidex.capbox.module.CheckVersionModel;
import com.androidex.capbox.ui.widget.SecondTitleBar;
import com.androidex.capbox.ui.widget.SingleCheckListDialog;
import com.androidex.capbox.utils.CommonKit;
import com.androidex.capbox.utils.Constants;
import com.androidex.capbox.utils.PhotoUtils;
import com.androidex.capbox.utils.RLog;

import java.io.File;

import butterknife.Bind;
import butterknife.OnClick;
import okhttp3.Headers;
import okhttp3.Request;

import static com.androidex.boxlib.utils.ImageUtils.cropImageUri;
import static com.androidex.capbox.data.cache.SharedPreTool.LOGIN_STATUS;
import static com.androidex.capbox.utils.Constants.CODE.CAMERA_PERMISSIONS_REQUEST_CODE;
import static com.androidex.capbox.utils.Constants.CODE.STORAGE_PERMISSIONS_REQUEST_CODE;
import static com.androidex.capbox.utils.Constants.EXTRA_PACKAGE_NAME;
import static com.androidex.capbox.utils.Constants.EXTRA_USER_HEAD;

/**
 * @title 设置界面
 */
public class SettingActivity extends UserBaseActivity {
    @Bind(R.id.title)
    SecondTitleBar title;

    public static final int REQ_SELECT_USER_HEAD = 500; // 选择用户头像
    public static final int REQ_IMAGE_CLIP = 300; // 图片剪裁
    public static final int REQ_CAMERA = 100; // 照相
    private static final int OUTPUT_X = 480;
    private static final int OUTPUT_Y = 480;
    SingleCheckListDialog editHeadDlg;  //修改头像
    private File fileCropUri = new File(Environment.getExternalStorageDirectory().getPath() + "/crop_photo.jpg");
    private Uri photoUri;

    @Override
    public void initData(Bundle savedInstanceState) {

    }

    @Override
    public void setListener() {
        title.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DebugBLEActivity.lauch(context);
            }
        });
    }

    @OnClick({
            R.id.ll_changePassword,
            R.id.ll_changeHead,
    })
    public void clickEvent(View view) {
        switch (view.getId()) {
            case R.id.ll_changePassword:
                ModifiActivtiy.lauch(context);
                break;
            case R.id.ll_changeHead:
                showHeadDlg();
                break;
            default:
                break;
        }
    }

    /**
     * 选择头像对话框
     */
    private void showHeadDlg() {
        if (editHeadDlg == null) {
            editHeadDlg = new SingleCheckListDialog(context);
        }
        editHeadDlg.title(getString(R.string.label_edit_head))
                .data(getResources().getStringArray(R.array.edit_head_opts))
                .setItemClickCallBack(new ItemClickCallBack<String>() {
                    @Override
                    public void onItemClick(int position, String model, int tag) {
                        super.onItemClick(position, model, tag);
                        switch (position) {
                            case 0: //拍照
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                                            || ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                                        if (ActivityCompat.shouldShowRequestPermissionRationale(context, Manifest.permission.CAMERA)) {
                                            CommonKit.showErrorShort(context, "您已经拒绝过一次");
                                        }
                                        requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE}, CAMERA_PERMISSIONS_REQUEST_CODE);
                                    } else {//已经有权限
                                        takePhoto();
                                    }
                                } else {
                                    takePhoto();
                                }
                                break;

                            case 1: //从相册选择
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                                        requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, STORAGE_PERMISSIONS_REQUEST_CODE);
                                    } else {
                                        PhotoUtils.openPic(context, REQ_SELECT_USER_HEAD);//打开系统相册
                                    }
                                } else {
                                    PhotoUtils.openPic(context, REQ_SELECT_USER_HEAD);//打开系统相册
                                }
                                break;

                            case 2://取消
                                editHeadDlg.dismiss();
                                break;
                        }
                    }
                }).show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case REQ_CAMERA://拍照完成回调
                    cropImageUri = Uri.fromFile(fileCropUri);
                    PhotoUtils.cropImageUri(context, photoUri, cropImageUri, 1, 1, OUTPUT_X, OUTPUT_Y, REQ_IMAGE_CLIP);
                    break;
                case REQ_SELECT_USER_HEAD:
                    if (CommonKit.isExitsSdcard()) {
                        cropImageUri = Uri.fromFile(fileCropUri);
                        Uri newUri = Uri.parse(PhotoUtils.getPath(context, data.getData()));
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            newUri = FileProvider.getUriForFile(context, EXTRA_PACKAGE_NAME, new File(newUri.getPath()));
                        }
                        PhotoUtils.cropImageUri(this, newUri, cropImageUri, 1, 1, OUTPUT_X, OUTPUT_Y, REQ_IMAGE_CLIP);
                    } else {
                        CommonKit.showErrorShort(context, "设备没有SD卡！");
                    }
                    break;
                case REQ_IMAGE_CLIP:
                    SharedPreTool.getInstance(context).setStringData(EXTRA_USER_HEAD, cropImageUri.getPath());
//                    Bitmap bitmap = PhotoUtils.getBitmapFromUri(cropImageUri, context);
//                    if (bitmap != null)
//                        iv_head.setImageBitmap(bitmap);
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            //调用系统相机申请拍照权限回调
            case CAMERA_PERMISSIONS_REQUEST_CODE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (CommonKit.isExitsSdcard()) {
                        takePhoto();
                    } else {
                        CommonKit.showErrorShort(context, "设备没有SD卡！");
                    }
                } else {
                    CommonKit.showErrorShort(context, "请允许打开相机！！");
                }
                break;
            }
            //调用系统相册申请Sdcard权限回调
            case STORAGE_PERMISSIONS_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    PhotoUtils.openPic(context, REQ_SELECT_USER_HEAD); //打开系统相册
                } else {
                    CommonKit.showOkShort(context, "请允许打开SD卡");
                }
                break;
            default:
                break;
        }
    }

    /**
     * 拍照
     */
    private void takePhoto() {
        photoUri = CommonKit.getOutputMediaFileUri(context, EXTRA_PACKAGE_NAME);
        Intent intent = new Intent();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            //添加这一句表示对目标应用临时授权该Uri所代表的文件
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
        intent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
        startActivityForResult(intent, REQ_CAMERA);
        context.overridePendingTransition(R.anim.in_from_right, R.anim.out_to_left);
    }

    @Override
    public void onClick(View v) {

    }

    public static void lauch(Activity activity) {
        CommonKit.startActivity(activity, SettingActivity.class, null, false);
    }

    @Override
    public int getLayoutId() {
        return R.layout.activity_setting;
    }

}
