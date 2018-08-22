package com.zzw.live.encodec;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Build;
import android.view.Surface;

import com.zzw.live.egl.EglHelper;
import com.zzw.live.egl.EglSurfaceView;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;

import javax.microedition.khronos.egl.EGLContext;

public abstract class BaseVideoEncoder {
    private Surface mSurface;
    private EGLContext mEGLContext;
    private EglSurfaceView.Render mRender;

    private MediaMuxer mMediaMuxer;
    private MediaCodec.BufferInfo mBuffInfo;
    private MediaCodec mVideoEncodec;
    private int width, height;

    private VideoEncodecThread mVideoEncodecThread;
    private EGLMediaThread mEGLMediaThread;

    public final static int RENDERMODE_WHEN_DIRTY = 0;
    public final static int RENDERMODE_CONTINUOUSLY = 1;
    private int mRenderMode = RENDERMODE_WHEN_DIRTY;

    public BaseVideoEncoder(Context context) {
    }

    public void setRender(EglSurfaceView.Render wlGLRender) {
        this.mRender = wlGLRender;
    }

    public void setRenderMode(int mRenderMode) {
        if (mRender == null) {
            throw new RuntimeException("must set render before");
        }
        this.mRenderMode = mRenderMode;
    }



    public void startRecode(){
        if (mSurface != null && mEGLContext != null) {
            mVideoEncodecThread = new VideoEncodecThread(new WeakReference<>(this));
            mEGLMediaThread = new EGLMediaThread(new WeakReference<>(this));
            mEGLMediaThread.isCreate = true;
            mEGLMediaThread.isChange = true;
            mEGLMediaThread.start();
            mVideoEncodecThread.start();
        }
    }
    public void stopRecode(){
        if(mEGLMediaThread!=null && mVideoEncodecThread!=null){
            mVideoEncodecThread.exit();

            mEGLMediaThread.onDestroy();
            mVideoEncodecThread =null;
            mEGLMediaThread =null;
        }
    }

    public void initEncoder(EGLContext eglContext,String savePath,String mineType,int width,int height){
        this.width = width;
        this.height = height;
        this.mEGLContext = eglContext;
        initMediaEncoder(savePath,mineType,width,height);
    }

    private void initMediaEncoder(String savePath, String mineType, int width, int height) {
        try {
            mMediaMuxer = new MediaMuxer(savePath,MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            initVideoEncoder(mineType,width,height);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initVideoEncoder(String mineType, int width, int height) {
        try {
            mVideoEncodec= MediaCodec.createEncoderByType(mineType);

            MediaFormat videoFormat = MediaFormat.createVideoFormat(mineType,width,height);
            videoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
            videoFormat.setInteger(MediaFormat.KEY_FRAME_RATE,30);//30帧
            videoFormat.setInteger(MediaFormat.KEY_BIT_RATE,width*height*4);//RGBA
            videoFormat.setInteger(MediaFormat.KEY_BIT_RATE,width*height*4);//RGBA
            //设置压缩等级  默认是baseline
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                videoFormat.setInteger(MediaFormat.KEY_PROFILE,MediaCodecInfo.CodecProfileLevel.AVCProfileMain);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    videoFormat.setInteger(MediaFormat.KEY_LEVEL, MediaCodecInfo.CodecProfileLevel.AVCLevel3);
                }
            }

            mVideoEncodec.configure(videoFormat,null,null,MediaCodec.CONFIGURE_FLAG_ENCODE);

            mBuffInfo = new MediaCodec.BufferInfo();
            mSurface = mVideoEncodec.createInputSurface();
        } catch (IOException e) {
            e.printStackTrace();
            mVideoEncodec=null;
            mBuffInfo=null;
            mSurface=null;
        }
    }

    static class VideoEncodecThread extends Thread{
        private WeakReference<BaseVideoEncoder> encoderWeakReference;
        private boolean isExit;

        private int videoTrackIndex;
        private long pts;

        private MediaCodec videoEncodec;
        private MediaCodec.BufferInfo videoBufferinfo;
        private MediaMuxer mediaMuxer;


        public VideoEncodecThread(WeakReference<BaseVideoEncoder> encoderWeakReference) {
            this.encoderWeakReference = encoderWeakReference;

            videoEncodec = encoderWeakReference.get().mVideoEncodec;
            videoBufferinfo = encoderWeakReference.get().mBuffInfo;
            mediaMuxer = encoderWeakReference.get().mMediaMuxer;
        }

        @Override
        public void run() {
            super.run();
            pts = 0;
            videoTrackIndex = -1;
            isExit = false;
            videoEncodec.start();
            while (true){
                if(isExit){
                    videoEncodec.stop();
                    videoEncodec.release();
                    videoEncodec =null;

                    mediaMuxer.stop();
                    mediaMuxer.release();
                    mediaMuxer = null;
                    break;
                }

                int outputBufferIndex = videoEncodec.dequeueOutputBuffer(videoBufferinfo, 0);
                if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    videoTrackIndex = mediaMuxer.addTrack(videoEncodec.getOutputFormat());
                    mediaMuxer.start();
                }else {
                    while (outputBufferIndex>=0){
                        ByteBuffer outputBuffer= videoEncodec.getOutputBuffers()[outputBufferIndex];
                        outputBuffer.position(videoBufferinfo.offset);
                        outputBuffer.limit(videoBufferinfo.offset + videoBufferinfo.size);

                        //设置时间戳
                        if(pts==0){
                            pts = videoBufferinfo.presentationTimeUs;
                        }
                        videoBufferinfo.presentationTimeUs = videoBufferinfo.presentationTimeUs - pts;
                        //写入数据
                        mediaMuxer.writeSampleData(videoTrackIndex,outputBuffer,videoBufferinfo);
                        if(encoderWeakReference.get().onMediaInfoListener!=null){
                            encoderWeakReference.get().onMediaInfoListener.onMediaTime((int) (videoBufferinfo.presentationTimeUs/1000000));
                        }
                        videoEncodec.releaseOutputBuffer(outputBufferIndex,false);
                        outputBufferIndex = videoEncodec.dequeueOutputBuffer(videoBufferinfo, 0);
                    }
                }
            }
        }

        public void exit() {
            isExit = true;
        }
    }

    static class EGLMediaThread extends Thread {
        private WeakReference<BaseVideoEncoder> encoderWeakReference;
        private EglHelper eglHelper;
        private Object object;
        private boolean isExit = false;
        private boolean isCreate = false;
        private boolean isChange = false;
        private boolean isStart = false;


        public EGLMediaThread(WeakReference<BaseVideoEncoder> encoderWeakReference) {
            this.encoderWeakReference = encoderWeakReference;
        }

        @Override
        public void run() {
            super.run();
            isExit = false;
            isStart = false;
            object = new Object();
            eglHelper = new EglHelper();
            eglHelper.initEgl(encoderWeakReference.get().mSurface,encoderWeakReference.get().mEGLContext);

            while (true){
                try {
                    if(isExit){
                        release();
                        break;
                    }
                    if (isStart) {
                        if (encoderWeakReference.get().mRenderMode == RENDERMODE_WHEN_DIRTY) {
                            synchronized (object) {
                                object.wait();
                            }
                        } else if (encoderWeakReference.get().mRenderMode == RENDERMODE_CONTINUOUSLY) {
                            Thread.sleep(1000 / 60);
                        } else {
                            throw new IllegalArgumentException("renderMode");
                        }
                    }
                    
                    onCreate();
                    onChange(encoderWeakReference.get().width, encoderWeakReference.get().height);
                    onDraw();
                    isStart = true;
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        private void onCreate() {
            if (!isCreate || encoderWeakReference.get().mRender == null)
                return;

            isCreate = false;
            encoderWeakReference.get().mRender.onSurfaceCreated();
        }

        private void onChange(int width, int height) {
            if (!isChange || encoderWeakReference.get().mRender == null)
                return;

            isChange = false;
            encoderWeakReference.get().mRender.onSurfaceChanged(width, height);
        }

        private void onDraw() {
            if (encoderWeakReference.get().mRender == null)
                return;

            encoderWeakReference.get().mRender.onDrawFrame();
            //第一次的时候手动调用一次 不然不会显示ui
            if (!isStart) {
                encoderWeakReference.get().mRender.onDrawFrame();
            }

            eglHelper.swapBuffers();
        }

        void requestRender() {
            if (object != null) {
                synchronized (object) {
                    object.notifyAll();
                }
            }
        }

        void onDestroy() {
            isExit = true;
            //释放锁
            requestRender();
        }


        void release() {
            if (eglHelper != null) {
                eglHelper.destoryEgl();
                eglHelper = null;
                object = null;
                encoderWeakReference = null;
            }
        }

        EGLContext getEglContext() {
            if (eglHelper != null) {
                return eglHelper.getEglContext();
            }
            return null;
        }
    }


    private OnMediaInfoListener onMediaInfoListener;

    public void setOnMediaInfoListener(OnMediaInfoListener onMediaInfoListener) {
        this.onMediaInfoListener = onMediaInfoListener;
    }

    public interface OnMediaInfoListener {
        void onMediaTime(int times);
    }



}
