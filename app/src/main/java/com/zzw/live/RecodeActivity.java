package com.zzw.live;

import android.media.MediaFormat;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.zzw.live.camera.CameraEglSurfaceView;
import com.zzw.live.encodec.BaseVideoEncoder;
import com.zzw.live.encodec.VideoEncodeRecode;

import java.io.File;


public class RecodeActivity extends AppCompatActivity {

    private CameraEglSurfaceView cameraEglSurfaceView;
    private Button button;

    private VideoEncodeRecode videoEncodeRecode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recode);
        cameraEglSurfaceView = findViewById(R.id.camera_view);
        button = findViewById(R.id.recode);
    }

    public void recode(View view) {
        if(videoEncodeRecode==null){
            videoEncodeRecode = new VideoEncodeRecode(this,cameraEglSurfaceView.getTextureId());
            videoEncodeRecode.initEncoder(cameraEglSurfaceView.getEglContext(),
                    Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator+
                    "testRecode.mp4",
                    MediaFormat.MIMETYPE_VIDEO_AVC,
                    cameraEglSurfaceView.getCameraPrivewHeight(),//相机的宽和高是相反的
                    cameraEglSurfaceView.getCameraPrivewWidth());
            videoEncodeRecode.setOnMediaInfoListener(new BaseVideoEncoder.OnMediaInfoListener() {
                @Override
                public void onMediaTime(int times) {
                    Log.e("zzz","time = "+times);
                }
            });
            videoEncodeRecode.startRecode();

            button.setText("录制中...");

        }else {
            videoEncodeRecode.stopRecode();
            button.setText("开始录制");
            videoEncodeRecode=null;
        }
    }
}
