package com.zzw.live.camera;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;

import com.zzw.live.egl.EglSurfaceView;


public class CameraEglSurfaceView extends EglSurfaceView implements CameraFboRender.OnSurfaceListener {

    private CameraHelper cameraHelper;
    private CameraFboRender render;
    private int cameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
    private int textureId;

    public CameraEglSurfaceView(Context context) {
        this(context, null);
    }

    public CameraEglSurfaceView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CameraEglSurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setRenderMode(RENDERMODE_CONTINUOUSLY);

        cameraHelper = new CameraHelper(context);
        render = new CameraFboRender(context);
        render.setOnSurfaceListener(this);
        setRender(render);
        previewAngle(context);
        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (cameraHelper != null) {
                    cameraHelper.autoFocus();
                }
            }
        });
    }

    public int getCameraPrivewWidth(){
        return cameraHelper.getPreviewWidth();
    }

    public int getCameraPrivewHeight(){
        return cameraHelper.getPreviewHeight();
    }

    @Override
    public void onSurfaceCreate(SurfaceTexture surfaceTexture,int textureId) {
        cameraHelper.startCamera(surfaceTexture, cameraId);
        this.textureId = textureId;
    }


    public void onDestroy() {
        if (cameraHelper != null) {
            cameraHelper.stopPrive();
        }
    }


    public void previewAngle(Context context) {
        int angle = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
        render.resetMatirx();
        switch (angle) {
            case Surface.ROTATION_0:
                if (cameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    render.setAngle(90, 0, 0, 1);
                    render.setAngle(180, 1, 0, 0);
                } else {
                    render.setAngle(90f, 0f, 0f, 1f);
                }

                break;
            case Surface.ROTATION_90:
                if (cameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    render.setAngle(180, 0, 0, 1);
                    render.setAngle(180, 0, 1, 0);
                } else {
                    render.setAngle(90f, 0f, 0f, 1f);
                }
                break;
            case Surface.ROTATION_180:
                if (cameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    render.setAngle(90f, 0.0f, 0f, 1f);
                    render.setAngle(180f, 0.0f, 1f, 0f);
                } else {
                    render.setAngle(-90, 0f, 0f, 1f);
                }
                break;
            case Surface.ROTATION_270:
                if (cameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    render.setAngle(180f, 0.0f, 1f, 0f);
                } else {
                    render.setAngle(0f, 0f, 0f, 1f);
                }
                break;
        }
    }

    public int getTextureId() {
        return textureId;
    }
}
