package com.facedetector.mqtt;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.util.Log;
import android.widget.Toast;

import com.facedetector.IFaceView;
import com.facedetector.IReceiveView;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;


/**
 * @author zouweilin 2019-06-19
 */
public class MqttPresenter{
    private String tag = "MqttPresenter";
    private MyMqttService mqttService;
    private String ip;
    private String port;
    private String sub ,name ,psd;
    private Context context;

    public void onCreate(Context context) {
        this.context = context;
        psd = App.psd;
        name = App.name;
        sub = App.sub;
        ip = App.ip;
        port = App.port;
//        warn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                if (isConnected()) {
//                    //消息内容
//                    String msg = input.getText().toString();
//                    if (msg.length()== 0){
//                        Toast.makeText(context, "empty msg!", Toast.LENGTH_SHORT).show();
//                        return;
//                    }
//                    //消息主题
//                    String topic = sub;
//                    //消息策略
//                    int qos = 0;
//                    //是否保留
//                    boolean retained = false;
//                    //发布消息
//                    publish(msg, topic, qos, retained);
//                    input.setText("");
//                } else {
//                    Toast.makeText(context, "断开连接", Toast.LENGTH_SHORT).show();
//                }
//            }
//        });

        buildEasyMqttService();
        connect();

    }


    public void onDestroy() {
        disconnect();
        close();
    }


    /**
     * 判断服务是否连接
     */
    private boolean isConnected() {
        return mqttService.isConnected();
    }

    /**
     * 发布消息
     */
    private void publish(String msg, String topic, int qos, boolean retained) {
        mqttService.publish(msg, topic, qos, retained);
    }
    /**
     * 发布消息
     */
    private void publish(byte[] msg, String topic, int qos, boolean retained) {
        mqttService.publish(msg, topic, qos, retained);
    }

    /**
     * 断开连接
     */
    private void disconnect() {
        mqttService.disconnect();
    }

    /**
     * 关闭连接
     */
    private void close() {
        mqttService.close();
    }

    /**
     * 订阅主题
     */
    private void subscribe() {
        String[] topics = new String[]{sub};
        //主题对应的推送策略 分别是0, 1, 2 建议服务端和客户端配置的主题一致
        // 0 表示只会发送一次推送消息 收到不收到都不关心
        // 1 保证能收到消息，但不一定只收到一条
        // 2 保证收到切只能收到一条消息
        int[] qoss = new int[]{0};
        mqttService.subscribe(topics, qoss);
    }

    /**
     * 连接Mqtt服务器
     */
    private void connect() {
        mqttService.connect(new IEasyMqttCallBack() {

            @Override
            public void messageArrived(String topic, String message, int qos) {
                //推送消息到达
                Log.e(tag, "message= " + message);
            }

            @Override
            public void messageArrived(String topic, byte[] message, int qos) {
                if (context instanceof IReceiveView) {
                    ((IReceiveView) context).onReceiveImage(message);
                }
            }

            @Override
            public void connectionLost(Throwable arg0) {
                //连接断开
                try {
                    Log.e(tag + "connectionLost", arg0.toString());
                    Toast.makeText(context, "connectionLost", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {

                } finally {

                }
            }
            @Override
            public void deliveryComplete(IMqttDeliveryToken arg0) {
                Log.e(tag + "@deliveryComplete", "发送完毕" + arg0.toString());
                Toast.makeText(context, "deliveryComplete", Toast.LENGTH_SHORT).show();
            }
            @Override
            public void connectSuccess(IMqttToken arg0) {
                Toast.makeText(context, "连接成功！", Toast.LENGTH_LONG).show();
                Log.e(tag + "@@@@@connectSuccess", "success");
                subscribe();
            }
            @Override
            public void connectFailed(IMqttToken arg0, Throwable arg1) {
                //连接失败
                Log.e(tag + "@@@@@connectFailed", "fail" + arg1.getMessage());
                Toast.makeText(context, "连接失败", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 构建EasyMqttService对象
     */
    private void buildEasyMqttService() {
        String imei = App.imei;
        String name = App.name;
        String psd = App.psd;
        String ip = App.ip;
        String port = App.port;
        mqttService = new MyMqttService.Builder()
                //设置自动重连
                .autoReconnect(true)
                //设置清除回话session  true(false) 不收(收)到服务器之前发出的推送消息
                .cleanSession(true)
                //唯一标示 保证每个设备都唯一就可以 建议 imei
                .clientId(imei)
                .userName(name)
                .passWord(psd)
                //mqtt服务器地址 格式例如：
                //  tcp://iot.eclipse.org:1883
                .serverUrl("tcp://"+ ip +":"+ port)
                //心跳包默认的发送间隔
                .keepAliveInterval(20)
                .timeOut(10)
                //构建出EasyMqttService 建议用application的context
                .bulid(context.getApplicationContext());
    }

    public void takePicture(Camera.Face[] faces, Camera camera) {
        // TODO: 2019-06-19 快速拍照会导致返回空数据，因为上次还没回调完成，所以再调用接口返回空 take picture failed
        camera.takePicture(null, null, new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                Log.e("tag", "thread name: "+Thread.currentThread().getName());
                // 拍照结束后不预览 不卡界面 原文：https://blog.csdn.net/AmazonUnicon/article/details/81364446
                Camera.Parameters params = camera.getParameters();
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                camera.setParameters(params);
                camera.startPreview();

                Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                if (context instanceof IFaceView) {
                    ((IFaceView) context).onTakePictureResult(bitmap);
                }
                publish(data, App.sub, 0, false);
            }
        });
    }

}