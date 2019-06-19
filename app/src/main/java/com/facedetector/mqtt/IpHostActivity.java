package com.facedetector.mqtt;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.facedetector.R;

/**
 * @author zouweilin 2019-06-19
 */
public class IpHostActivity extends AppCompatActivity {

    private EditText ip;
    private EditText port;
    private EditText sub ,name,psd;

    public static void start(Context context) {
        context.startActivity(new Intent(context, IpHostActivity.class));
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ip_host);
        ip = findViewById(R.id.ip);
        port = findViewById(R.id.port);
        sub = findViewById(R.id.sub);
        name = findViewById(R.id.name);
        psd = findViewById(R.id.psd);
        Button connect = findViewById(R.id.connect);
        getIMEI();

        connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ip.getText().toString().length() !=0 && port.getText().toString().length()!=0 &&
                        sub.getText().toString().length()!=0&&name.getText().toString().length()!=0&&
                        psd.getText().toString().length()!=0){
                    App.ip = ip.getText().toString();
                    App.port = port.getText().toString();
                    App.sub = sub.getText().toString();
                    App.name = name.getText().toString();
                    App.psd = psd.getText().toString();
                    finish();
                }
            }
        });

    }

    @SuppressLint("HardwareIds")
    private void getIMEI() {
        String tag = "tag";
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            //申请CAMERA权限
            Log.d(tag, "********没有权限 去申请");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE}, 2);
        } else {
            Log.d(tag, "*********** 已经有权限了");
            TelephonyManager mTelephony = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            assert mTelephony != null;
            String imei;
            if (mTelephony.getDeviceId() != null) {
                imei = mTelephony.getDeviceId();
            } else {
                imei = Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);
            }

            Log.d(tag,"imei "+ imei);
            App.imei = imei;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 2) {
            for (int grantResult : grantResults) {
                if (grantResult == PackageManager.PERMISSION_GRANTED) {
                    getIMEI();
                }
            }
        }
    }
}
