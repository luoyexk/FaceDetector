package com.facedetector;

import android.Manifest;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.Toolbar;

import com.facedetector.mqtt.App;
import com.facedetector.mqtt.IpHostActivity;
import com.facedetector.mqtt.ReceiveActivity;
import com.facedetector.util.FaceSDK;

import java.lang.ref.WeakReference;

/**
 * 检测图片中脸部的数量，同时检测至少27张脸
 * 检测视频中脸部的数量，同时检测至少10张脸，根据手机性能来决定
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int PICK_IMAGE_ALBUM = 0x12;
    private static final int REQUEST_PERMISSION = 0x1000;
    private ImageView iv;
    private HandlerThread handlerThread;
    private FaceHandler faceHandler;
    private View btnGallery;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_PERMISSION);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (faceHandler != null) {
            faceHandler.removeCallbacksAndMessages(null);
        }
        if (handlerThread != null) {
            handlerThread.quit();
        }
    }

    private void initViews() {
        btnGallery = findViewById(R.id.btn_de_face_image);
        btnGallery.setOnClickListener(this);
        findViewById(R.id.btn1).setOnClickListener(this);
        iv = findViewById(R.id.iv);
        getImei();
    }

    private void getImei() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        TelephonyManager mTelephony = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        assert mTelephony != null;
        String imei;
        if (mTelephony.getDeviceId() != null) {
            imei = mTelephony.getDeviceId();
        } else {
            imei = Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);
        }
        App.imei = imei;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) {
            return;
        }
        if (requestCode == PICK_IMAGE_ALBUM) {
            Uri uri = data.getData();
            if (handlerThread == null) {
                handlerThread = new HandlerThread("face");
                handlerThread.start();
                faceHandler = new FaceHandler(handlerThread.getLooper(), this);
            }
            faceHandler.detect(uri);
            btnGallery.setEnabled(false);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn1:
                FaceDetectorActivity.start(this);
                break;
            case R.id.btn_de_face_image:
                openAlbum();
                break;
            case R.id.btnReceive:
                ReceiveActivity.start(this);
                break;
            case R.id.btnSetHost:
                IpHostActivity.start(this);
                    break;
            default:
        }
    }

    private void openAlbum() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_ALBUM);
    }

    private void showResult(final Bitmap bitmap) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                iv.setImageBitmap(bitmap);
                btnGallery.setEnabled(true);
            }
        });
    }

    private void readGalleryError() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, "Something wrong happened when read gallery.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void beginDetect() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
            }
        });
    }


    private static class FaceHandler extends Handler {
        private WeakReference<MainActivity> viewWeakReference;

        private WeakReference<Uri> uriWeakReference;

        FaceHandler(Looper looper, MainActivity activity) {
            super(looper);
            this.viewWeakReference = new WeakReference<>(activity);
        }

        void detect(Uri uri) {
            this.uriWeakReference = new WeakReference<>(uri);
            sendEmptyMessage(0);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            MainActivity activity = viewWeakReference.get();
            Uri uri = uriWeakReference.get();
            if (activity == null || uri == null) {
                return;
            }
            // get image path from gallery
            String imagePath = getPath(uri, activity.getContentResolver());
            if (imagePath == null) {
                activity.readGalleryError();
                return;
            }
            activity.beginDetect();
            Log.e("tag", " imagePath :" + imagePath);
            // begin detect
            Bitmap bitmap = new FaceSDK().DetectionBitmap(BitmapFactory.decodeFile(imagePath));
            if (viewWeakReference.get() != null) {
                activity.showResult(bitmap);
            }
        }

        private String getPath(Uri uri, ContentResolver provider) {
            String[] projection = {MediaStore.Images.Media.DATA};
            Cursor cursor = provider.query(uri, projection, null, null, null);
            if (cursor == null || !cursor.moveToFirst()) {
                return null;
            }
            int columnIndex = cursor.getColumnIndex(projection[0]);
            String imagePath = cursor.getString(columnIndex);
            cursor.close();
            return imagePath;
        }

    }

}