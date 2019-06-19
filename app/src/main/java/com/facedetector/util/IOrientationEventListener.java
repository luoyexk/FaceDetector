package com.facedetector.util;

import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.OrientationEventListener;

/**
 * @author zouweilin 2019-06-19
 */
public class IOrientationEventListener extends OrientationEventListener {

    private Camera mCamera;

    public IOrientationEventListener(Context context, Camera mCamera) {
        super(context);
        this.mCamera = mCamera;
    }

    @Override
    public void onOrientationChanged(int orientation) {
        if (ORIENTATION_UNKNOWN == orientation) {
            return;
        }
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(0, info);
        orientation = (orientation + 45) / 90 * 90;
        int rotation = 0;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            rotation = (info.orientation - orientation + 360) % 360;
        } else {
            rotation = (info.orientation + orientation) % 360;
        }
        Log.e("TAG","orientation: " + orientation);
        try {
            if (null != mCamera) {
                Camera.Parameters parameters = mCamera.getParameters();
                parameters.setRotation(rotation);
                mCamera.setParameters(parameters);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
