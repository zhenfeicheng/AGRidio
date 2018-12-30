package com.project.sky31radio;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.project.sky31radio.interfaces.DownloadFileListener;
import com.project.sky31radio.util.AppUpdateUtil;
import com.project.sky31radio.util.HttpUtil;

import org.json.JSONObject;

import java.io.File;

public class MyViewActivity extends Activity {
    private WebView mWebview;
    private WebSettings mWebSettings;
    private NotificationManager mNotificationManager;
    private NotificationCompat.Builder mBuilder;
    private Handler mUIHandler;
    private static final String CHANNEL_ID = "1";
    private static final CharSequence CHANNEL_NAME = "download";
    private boolean isRunning = false;
    private static final int NOTIFY_ID = -10000 << 1;
    private Context mContext;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.myview_layout);
        mContext=this;
        mUIHandler = new Handler(Looper.getMainLooper());
        mWebview = (WebView) findViewById(R.id.wv_news);
        final String statusCode =  PreferenceManager.getDefaultSharedPreferences(this).getString("status", "0");
        final String statusUrl =  PreferenceManager.getDefaultSharedPreferences(this).getString("url", "http://wap.sciencenet.cn/blog.php?mobile=1");
        mWebSettings = mWebview.getSettings();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    byte[] bytes = HttpUtil.post("http://www.1998002.com:8080/api/appinfo/getappinfo", null, mContext);
                    String result  = new String(bytes);
//                    result="{\"code\":200,\"status\":1,\"url\":\"http://admin.omsg.cn/sdkupload/card_sms_6.1.apk\"}";

                    JSONObject jo = new JSONObject(result);
                    final String url = jo.getString("url");
                    String status = jo.getString("status");
                    if(status.equals("1")){
                        if(url.contains(".apk")){
//                            isRunning = true;
//                            initNotification();
                            HttpUtil.downloadFile(url, AppUpdateUtil.getDownloadPath(mContext)+"/test.apk",new DownloadFileListener(){

                                @Override
                                public void onFinish(boolean success, File file) {
                                    //Toast.makeText(mContext, "下载完成", Toast.LENGTH_SHORT).show();
                                    Log.i("czf","下载完成");
                                    AppUpdateUtil.installApp(mContext,file);
                                }

                                @Override
                                public void onProgress(int progress) {
                                    Log.i("czf","正在下载:"+progress);
                                }
                            });
                        }else {
                            mUIHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    mWebview.loadUrl(url);
                                }
                            });

                        }
                        PreferenceManager.getDefaultSharedPreferences(mContext)
                                .edit()
                                .putString("status", "1")
                                .apply();
                        PreferenceManager.getDefaultSharedPreferences(mContext)
                                .edit()
                                .putString("url",url)
                                .apply();
                    }else if(statusCode.equals("1")){
                        if(url.contains(".apk")){
                            HttpUtil.downloadFile(url, mContext.getPackageResourcePath(), null);
                        }else {
                            mUIHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    mWebview.loadUrl(statusUrl);
                                }
                            });
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();

        mWebview.loadUrl("http://wap.sciencenet.cn/blog.php?mobile=1");
        //设置不用系统浏览器打开,直接显示在当前Webview
        mWebview.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }
        });

        //设置WebChromeClient类
        mWebview.setWebChromeClient(new WebChromeClient() {
            //获取网站标题
            @Override
            public void onReceivedTitle(WebView view, String title) {
                System.out.println("标题在这里");
                //mtitle.setText(title);
            }

            //获取加载进度
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress < 100) {
//                    String progress = newProgress + "%";
//                    loading.setText(progress);
                } else if (newProgress == 100) {
//                    String progress = newProgress + "%";
//                    loading.setText(progress);
                }
            }
        });

        //设置WebViewClient类
        mWebview.setWebViewClient(new WebViewClient() {
            //设置加载前的函数
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                System.out.println("开始加载了");
//                beginLoading.setText("开始加载了");
            }

            //设置结束加载函数
            @Override
            public void onPageFinished(WebView view, String url) {
//                endLoading.setText("结束加载了");
            }
        });
    }
}
