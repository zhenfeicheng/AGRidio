package com.project.sky31radio.util;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

/**
 * Created by wangyuliang on 2016/2/1.
 */
public class PermissionUtil {
    private static final String TAG = "PermissionUtil";

    public static final int REQUEST_CODE_ASK_PERMISSIONS = 1;

    // MIUI特殊权限处理
    private static final String KEY_MIUI_VERSION_NAME = "ro.miui.ui.version.name";
    private static final int OP_MIUI_READ_NOTIFICATION_SMS = 61; // MIUI通知类短信权限
    private static final int OP_MIUI_FLOAT_WINDOW = 24; // MIUI悬浮窗权限
    private static int MIUI_VERSION_DEFAULT = 0;
    private static int MIUI_VERSION_8 = 8;

    public static void requestPermissions(Context context, String[] permissions, int requestCode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (context instanceof Activity) {
                ((Activity) context).requestPermissions(permissions, requestCode);
            }
        }
    }

    public static boolean hasWriteExternalStoragePermission(Context context) {
        return hasPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }
    /**
     * 判断是否有读短信的权限
     */
    public static boolean hasReadSmsPermission(Context context) {
        return hasPermission(context, Manifest.permission.READ_SMS);
    }



    /**
     * 判断是否有收取短信的权限
     */
    public static boolean hasReceiveSmsPermission(Context context) {
        return hasPermission(context, Manifest.permission.RECEIVE_SMS);
    }

    /**
     * 判断是否有发送短信的权限
     */
    public static boolean hasSendSmsPermission(Context context) {
        return hasPermission(context, Manifest.permission.SEND_SMS);
    }

    /**
     * 判断是否有读取联系人的权限
     */
    public static boolean hasReadContactsPermission(Context context) {
        return hasPermission(context, Manifest.permission.READ_CONTACTS);
    }

    /**
     * 判断是否有拨打电话的权限
     */
    public static boolean hasCallPhonePermission(Context context) {
        return hasPermission(context, Manifest.permission.CALL_PHONE);
    }

    /**
     * 判断是否有读取设备状态的权限
     */
    public static boolean hasReadPhoneStatePermission(Context context) {
        return hasPermission(context, Manifest.permission.READ_PHONE_STATE);
    }

    /**
     * 判断是否有悬浮窗权限
     */
    public static boolean hasSystemAlertWindowPermission(Context context) {
        return hasPermission(context, Manifest.permission.SYSTEM_ALERT_WINDOW);
    }

    /**
     * 判断是否有访问网络状态的权限
     */
    public static boolean hasAccessNetworkStatePermission(Context context) {
        return hasPermission(context, Manifest.permission.ACCESS_NETWORK_STATE);
    }

    private static boolean hasPermission(Context context, String permission) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return PackageManager.PERMISSION_GRANTED == context.checkSelfPermission(permission);
        }
        return true;
    }

    /**
     * 检查手机是否是MIUI
     */
    private static boolean isMiui(){
        String device = Build.MANUFACTURER;
        return "Xiaomi".equals(device);
    }




}