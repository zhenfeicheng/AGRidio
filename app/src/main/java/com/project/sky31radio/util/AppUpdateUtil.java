package com.project.sky31radio.util;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.v4.content.FileProvider;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;

/**
 * Created by lijialong on 2018/2/1.
 */

public class AppUpdateUtil {
    public static boolean installApp(Context context, File appFile) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Uri fileUri = FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".fileProvider", appFile);
                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.setDataAndType(fileUri, "application/vnd.android.package-archive");
                Log.i("czf","开始安装");
            } else {
                intent.setDataAndType(Uri.fromFile(appFile), "application/vnd.android.package-archive");
            }
            if (context.getPackageManager().queryIntentActivities(intent, 0).size() > 0) {
                context.startActivity(intent);
            }
            return true;
        } catch (Exception e) {
            Log.i("czf",""+e.getStackTrace());
        }
        return false;
    }

//    static boolean isAppFileExist(UpdateApp updateApp) {
//        boolean result = false;
//        try {
//            File appFile = updateApp.getAppFile();
//            result = appFile != null && appFile.exists();
//            if (updateApp.isVerifyMD5() && result) {
//                result = TextUtils.isEmpty(updateApp.getMd5()) || SecretManagerUtil.GetMD5Code(appFile).equalsIgnoreCase(updateApp.getMd5());
//                if (!result) {
//                    appFile.delete();
//                }
//            }
//        } catch (Exception e) {
//            result = false;
//        }
//        return result;
//    }

    public static boolean isAppInstalled(Context context, String packageName) {
        if (TextUtils.isEmpty(packageName)) {
            return false;
        }
        try {
            context.getPackageManager().getApplicationInfo(packageName,
                    PackageManager.GET_UNINSTALLED_PACKAGES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public static String getDownloadPath(Context context) {
        String path = null;
        if (PermissionUtil.hasWriteExternalStoragePermission(context) && (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED) || !Environment.isExternalStorageRemovable())) {
            path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
            if (TextUtils.isEmpty(path)) {
                path = context.getExternalCacheDir().getAbsolutePath();
            }
        } else {
            path = context.getCacheDir().getAbsolutePath();
        }
        return path;
    }
}
