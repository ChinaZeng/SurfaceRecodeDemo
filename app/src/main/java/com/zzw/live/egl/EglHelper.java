package com.zzw.live.egl;

import android.opengl.EGL14;
import android.view.Surface;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;

public class EglHelper {
    private static final String TAG = "EglHelper";
    private EGL10 mEgl;
    private EGLDisplay mEglDisplay;
    private EGLContext mEglContext;
    private EGLSurface mEglSurface;


    public void initEgl(Surface surface, EGLContext eglContext) {
        //1. 得到Egl实例
        mEgl = (EGL10) EGLContext.getEGL();

        //2. 得到默认的显示设备（就是窗口）
        mEglDisplay = mEgl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
        if (mEglDisplay == EGL10.EGL_NO_DISPLAY) {
            throw new RuntimeException("eglGetDisplay failed");
        }

        //3. 初始化默认显示设备
        int[] version = new int[2];
        if (!mEgl.eglInitialize(mEglDisplay, version)) {
            throw new RuntimeException("eglInitialize failed");
        }

        //4. 设置显示设备的属性
        int[] attrib_list = new int[]{
                EGL10.EGL_RED_SIZE, mRedSize,
                EGL10.EGL_GREEN_SIZE, mGreenSize,
                EGL10.EGL_BLUE_SIZE, mBlueSize,
                EGL10.EGL_ALPHA_SIZE, mAlphaSize,
                EGL10.EGL_DEPTH_SIZE, mDepthSize,
                EGL10.EGL_STENCIL_SIZE, mStencilSize,
                EGL10.EGL_RENDERABLE_TYPE, 4,//egl版本  2.0
                EGL10.EGL_NONE};


        int[] num_config = new int[1];
        if (!mEgl.eglChooseConfig(mEglDisplay, attrib_list, null, 1,
                num_config)) {
            throw new IllegalArgumentException("eglChooseConfig failed");
        }
        int numConfigs = num_config[0];
        if (numConfigs <= 0) {
            throw new IllegalArgumentException(
                    "No configs match configSpec");
        }

        //5. 从系统中获取对应属性的配置
        EGLConfig[] configs = new EGLConfig[numConfigs];
        if (!mEgl.eglChooseConfig(mEglDisplay, attrib_list, configs, numConfigs,
                num_config)) {
            throw new IllegalArgumentException("eglChooseConfig#2 failed");
        }
        EGLConfig eglConfig = chooseConfig(mEgl, mEglDisplay, configs);
        if (eglConfig == null) {
            eglConfig = configs[0];
        }


        //6. 创建EglContext
        int[] contextAttr = new int[]{
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                EGL10.EGL_NONE
        };

        if (eglContext == null) {
            mEglContext = mEgl.eglCreateContext(mEglDisplay, eglConfig, EGL10.EGL_NO_CONTEXT, contextAttr);
        } else {
            mEglContext = mEgl.eglCreateContext(mEglDisplay, eglConfig, eglContext, contextAttr);
        }

        //7. 创建渲染的Surface
        mEglSurface = mEgl.eglCreateWindowSurface(mEglDisplay, eglConfig, surface, null);

        //8. 绑定EglContext和Surface到显示设备中
        if (!mEgl.eglMakeCurrent(mEglDisplay, mEglSurface, mEglSurface, mEglContext)) {
            throw new RuntimeException("eglMakeCurrent fail");
        }
    }


    //9. 刷新数据，显示渲染场景
    public boolean swapBuffers() {
        if (mEgl != null) {
            return mEgl.eglSwapBuffers(mEglDisplay, mEglSurface);
        } else {
            throw new RuntimeException("egl is null");
        }
    }

    public void destoryEgl() {
        if (mEgl != null) {
            if (mEglSurface != null && mEglSurface != EGL10.EGL_NO_SURFACE) {
                mEgl.eglMakeCurrent(mEglDisplay, EGL10.EGL_NO_SURFACE,
                        EGL10.EGL_NO_SURFACE,
                        EGL10.EGL_NO_CONTEXT);

                mEgl.eglDestroySurface(mEglDisplay, mEglSurface);
                mEglSurface = null;
            }


            if (mEglContext != null) {
                mEgl.eglDestroyContext(mEglDisplay, mEglContext);
                mEglContext = null;
            }


            if (mEglDisplay != null) {
                mEgl.eglTerminate(mEglDisplay);
                mEglDisplay = null;
            }

            mEgl = null;
        }


    }


    public EGLContext getEglContext() {
        return mEglContext;
    }

    private final int mRedSize = 8;
    private final int mGreenSize = 8;
    private final int mBlueSize = 8;
    private final int mAlphaSize = 8;
    private final int mDepthSize = 8;
    private final int mStencilSize = 8;


    private EGLConfig chooseConfig(EGL10 egl, EGLDisplay display,
                                   EGLConfig[] configs) {
        for (EGLConfig config : configs) {
            int d = findConfigAttrib(egl, display, config,
                    EGL10.EGL_DEPTH_SIZE, 0);
            int s = findConfigAttrib(egl, display, config,
                    EGL10.EGL_STENCIL_SIZE, 0);
            if ((d >= mDepthSize) && (s >= mStencilSize)) {
                int r = findConfigAttrib(egl, display, config,
                        EGL10.EGL_RED_SIZE, 0);
                int g = findConfigAttrib(egl, display, config,
                        EGL10.EGL_GREEN_SIZE, 0);
                int b = findConfigAttrib(egl, display, config,
                        EGL10.EGL_BLUE_SIZE, 0);
                int a = findConfigAttrib(egl, display, config,
                        EGL10.EGL_ALPHA_SIZE, 0);
                if ((r == mRedSize) && (g == mGreenSize)
                        && (b == mBlueSize) && (a == mAlphaSize)) {
                    return config;
                }
            }
        }
        return null;
    }

    private int findConfigAttrib(EGL10 egl, EGLDisplay display,
                                 EGLConfig config, int attribute, int defaultValue) {
        int[] value = new int[1];
        if (egl.eglGetConfigAttrib(display, config, attribute, value)) {
            return value[0];
        }
        return defaultValue;
    }


}
