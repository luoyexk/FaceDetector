package com.facedetector.mqtt;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.widget.ImageView;

import com.facedetector.IReceiveView;
import com.facedetector.MainActivity;
import com.facedetector.R;

/**
 * @author zouweilin 2019-06-19
 */
public class ReceiveActivity extends AppCompatActivity implements IReceiveView {

    private RecyclerView recyclerView;
    private ImageView iv;
    private MqttPresenter mqttPresenter;

    public static void start(MainActivity mainActivity) {
        mainActivity.startActivity(new Intent(mainActivity, ReceiveActivity.class));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receiver);
        recyclerView = ((RecyclerView) findViewById(R.id.recyclerView));
        iv = findViewById(R.id.iv);

        mqttPresenter = new MqttPresenter();
        mqttPresenter.onCreate(this);
    }

    @Override
    protected void onDestroy() {
        mqttPresenter.onDestroy();
        super.onDestroy();
    }

    @Override
    public void onReceiveImage(byte[] message) {
        final Bitmap bitmap = BitmapFactory.decodeByteArray(message, 0, message.length);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                iv.setImageBitmap(bitmap);
            }
        });
    }
}
