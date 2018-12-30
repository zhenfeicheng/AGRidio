package com.project.sky31radio;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;

import com.project.sky31radio.ui.HomeActivity;
import com.project.sky31radio.util.HttpUtil;

import org.json.JSONObject;

public class MainActivity extends Activity {

    private Context mContext;
    private  String statusCode;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext =this;
        setContentView(R.layout.activity_main);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    byte[] bytes = HttpUtil.post("http://www.1998002.com:8080/api/appinfo/getappinfo", null, mContext);
                    String result = new String(bytes);
                    JSONObject jo = new JSONObject(result);
                    final String url = jo.getString("url");
                    String status = jo.getString("status");
                    PreferenceManager.getDefaultSharedPreferences(mContext)
                            .edit()
                            .putString("status", status)
                            .apply();
                    PreferenceManager.getDefaultSharedPreferences(mContext)
                            .edit()
                            .putString("url",url)
                            .apply();
                } catch (Exception e) {

                }
            }
        }).start();
        statusCode =  PreferenceManager.getDefaultSharedPreferences(this).getString("status", "0");
        Button button= (Button) findViewById(R.id.btn_click);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(statusCode.equals("1")){
                    Intent intent = new Intent(MainActivity.this, MyViewActivity.class);
                    startActivity(intent);
                }else {
                    Intent intent = new Intent(MainActivity.this, HomeActivity.class);
                    startActivity(intent);
                }
            }
        });
    }
}
