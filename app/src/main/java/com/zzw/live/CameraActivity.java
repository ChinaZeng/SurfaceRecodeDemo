package com.zzw.live;

import android.content.res.Configuration;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.zzw.live.camera.CameraEglSurfaceView;

public class CameraActivity extends AppCompatActivity {


    private CameraEglSurfaceView cameraEglSurfaceView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        cameraEglSurfaceView = findViewById(R.id.camera);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraEglSurfaceView.onDestroy();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        cameraEglSurfaceView.previewAngle(this);
    }
}
