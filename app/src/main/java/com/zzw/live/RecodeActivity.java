package com.zzw.live;

import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.ywl5320.libmusic.WlMusic;
import com.ywl5320.listener.OnCompleteListener;
import com.ywl5320.listener.OnErrorListener;
import com.ywl5320.listener.OnPreparedListener;
import com.ywl5320.listener.OnShowPcmDataListener;
import com.zzw.live.camera.CameraEglSurfaceView;
import com.zzw.live.encodec.BaseVideoEncoder;
import com.zzw.live.encodec.VideoEncodeRecode;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;


public class RecodeActivity extends AppCompatActivity {

    private CameraEglSurfaceView cameraEglSurfaceView;
    private Button button;

    private VideoEncodeRecode videoEncodeRecode;
    private PutPcmThread putPcmThread;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recode);
        cameraEglSurfaceView = findViewById(R.id.camera_view);
        button = findViewById(R.id.recode);
    }


    public void recode1(View view) {
        if (videoEncodeRecode == null) {
            startRecode(44100, 16, 2);
            button.setText("正在录制");
        } else {
//            putPcmThread.setExit(true);
            videoEncodeRecode.stopRecode();
            button.setText("开始录制");
            videoEncodeRecode = null;
        }
    }

    private void startRecode(int samplerate, int bit, int channels) {
        videoEncodeRecode = new VideoEncodeRecode(this, cameraEglSurfaceView.getTextureId());

        videoEncodeRecode.initEncoder(cameraEglSurfaceView.getEglContext(),
                Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator +
                        "testRecode.mp4",
                cameraEglSurfaceView.getCameraPrivewHeight(),//相机的宽和高是相反的
                cameraEglSurfaceView.getCameraPrivewWidth(),
                samplerate, channels, bit
        );

        videoEncodeRecode.setOnMediaInfoListener(new BaseVideoEncoder.OnMediaInfoListener() {
            @Override
            public void onMediaTime(int times) {
                Log.e("zzz", "time = " + times);
            }
        });

        videoEncodeRecode.setOnStatusChangeListener(new BaseVideoEncoder.OnStatusChangeListener() {
            @Override
            public void onStatusChange(STATUS status) {
                if (status == STATUS.START) {
                    putPcmThread = new PutPcmThread(new WeakReference<RecodeActivity>(RecodeActivity.this));
                    putPcmThread.start();
                }
            }
        });

        videoEncodeRecode.startRecode();
    }

    private static class PutPcmThread extends Thread {

        private boolean isExit;
        private WeakReference<RecodeActivity> reference;

        public PutPcmThread(WeakReference<RecodeActivity> reference) {
            this.reference = reference;
        }

        public void setExit(boolean exit) {
            isExit = exit;
        }

        @Override
        public void run() {
            super.run();
            isExit = false;
            InputStream inputStream = null;
            try {
                // mydream.pcm  44100hz   16bit  立体声

                int s_ = 44100 * 2 * (16 / 2);
                int bufferSize = s_ / 100;

                inputStream = reference.get().getAssets().open("mydream.pcm");
                byte[] buffer = new byte[bufferSize];
                int size = 0;
                while ((size = inputStream.read(buffer, 0, bufferSize)) != -1) {
                    try {
                        Thread.sleep(1000 / 100); // 10毫秒写入一次
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (reference.get().videoEncodeRecode == null || isExit) {
                        Log.e("zzz", "videoEncodeRecode == null or isExit-->  break");
                        break;
                    }
                    reference.get().videoEncodeRecode.putPcmData(buffer, size);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
