package com.zzw.live.camera;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.util.Log;

import com.zzw.live.util.DisplayUtil;

import java.io.IOException;
import java.util.List;

public class CameraHelper {
    private SurfaceTexture surfaceTexture;
    private Camera camera;
    private static final String TAG = "CameraHelper";


    private int screenW, screenH;
    private int PreviewWidth;
    private int PreviewHeight;

    public CameraHelper(Context context) {
        screenW = DisplayUtil.getScreenW(context);
        screenH = DisplayUtil.getScreenH(context);
    }

    public void startCamera(SurfaceTexture surfaceTexture, int cameraId) {
        this.surfaceTexture = surfaceTexture;
        startCamera(cameraId);
    }

    private void startCamera(int cameraId) {
        try {
            camera = Camera.open(cameraId);
            camera.setPreviewTexture(surfaceTexture);

            Camera.Parameters parameters = camera.getParameters();
            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            parameters.setPreviewFormat(ImageFormat.NV21);

            //设置对焦模式
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);

            Camera.Size size = findBestSizeValue(parameters.getSupportedPreviewSizes(), screenW, screenH, 0.1f);
            parameters.setPreviewSize(size.width, size.height);
            PreviewWidth = size.width;
            PreviewHeight = size.height;


            size = findBestSizeValue(parameters.getSupportedPictureSizes(), screenW, screenH, 0.1f);
            parameters.setPictureSize(size.width, size.height);

            camera.setParameters(parameters);
            camera.startPreview();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stopPrive() {
        if (camera != null) {
            camera.stopPreview();
            camera.release();
            camera = null;
        }
    }

    public void changeCameraId(int cameraId) {
        stopPrive();
        startCamera(cameraId);
    }

    public void autoFocus() {
        if (camera != null) {
            camera.autoFocus(null);
        }
    }


    /**
     * 照相最佳的分辨率
     *
     * @param sizes
     * @return
     */
    private Camera.Size findBestSizeValue(List<Camera.Size> sizes, int w, int h, double minDiff) {

        //摄像头这个size里面都是w > h
        if (w < h) {
            int t = h;
            h = w;
            w = t;
        }

        double targetRatio = (double) w / h;
        Log.e(TAG, "照相尺寸  w:" + w + "  h:" + h + "  targetRatio:" + targetRatio + "  minDiff:" + minDiff);
        if (sizes == null) {
            return null;
        }
        Camera.Size optimalSize = null;
        // Try to find an size match aspect ratio and size
        for (Camera.Size size : sizes) {
            // 如果有符合的分辨率，则直接返回
//            if (size.width == defaultWidth && size.height == defaultHeight) {
//                Log.e(TAG, "get default preview size!!!");
//                return size;
//            }
//            if (size.width < MIN_PICTURE_WIDTH || size.height < MIN_PICTURE_HEIGHT) {
//                continue;
//            }

            double ratio = (double) size.width / size.height;

            double diff = Math.abs(ratio - targetRatio);

            Log.e(TAG, "照相支持尺寸  width:" + size.width + "  height:" + size.height + "  targetRatio:" + targetRatio + "" +
                    "  ratio:" + ratio + "   diff:" + diff);

            if (diff > minDiff) {
                continue;
            }

            if (optimalSize == null) {
                optimalSize = size;
            } else {
                if (optimalSize.width * optimalSize.height < size.width * size.height) {
                    optimalSize = size;
                }
            }
            minDiff = diff;
        }
        // Cannot find the one match the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff += 0.1f;
            if (minDiff > 1.0f) {
                optimalSize = sizes.get(0);
            } else {
                optimalSize = findBestSizeValue(sizes, w, h, minDiff);
            }
        }
        if (optimalSize != null)
            Log.e(TAG, "照相best尺寸  " + optimalSize.width + "  " + optimalSize.height);
        return optimalSize;

    }


    public int getPreviewWidth() {
        return PreviewWidth;
    }

    public int getPreviewHeight() {
        return PreviewHeight;
    }
}
