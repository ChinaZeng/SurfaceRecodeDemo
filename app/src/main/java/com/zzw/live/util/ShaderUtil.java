package com.zzw.live.util;

import android.content.Context;
import android.opengl.GLES20;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ShaderUtil {
    private static final String TAG = "ShaderUtil";

    public static String readRawTxt(Context context, int rawId) {
        InputStream inputStream = context.getResources().openRawResource(rawId);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuffer sb = new StringBuffer();
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    public static int loadShader(int shaderType, String source) {
        // create a vertex shader type (GLES20.GL_VERTEX_SHADER)
        // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
        int shader = GLES20.glCreateShader(shaderType);
        if (shader != 0) {
            //添加代码到shader
            GLES20.glShaderSource(shader, source);
            //编译shader
            GLES20.glCompileShader(shader);
            int[] compile = new int[1];
            //检测是否编译成功
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compile, 0);
            if (compile[0] != GLES20.GL_TRUE) {
                Log.d(TAG, "shader compile error");
                GLES20.glDeleteShader(shader);
                shader = 0;
            }
        }
        return shader;
    }

    public static int createProgram(String vertexSource, String fragmentSource) {
        //获取vertex shader
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource);
        if (vertexShader == 0) {
            return 0;
        }
        //获取fragment shader
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
        if (fragmentShader == 0) {
            return 0;
        }
        //创建一个空的渲染程序
        int program = GLES20.glCreateProgram();
        if (program != 0) {
            //添加vertexShader到渲染程序
            GLES20.glAttachShader(program, vertexShader);
            //添加fragmentShader到渲染程序
            GLES20.glAttachShader(program, fragmentShader);
            //关联为可执行渲染程序
            GLES20.glLinkProgram(program);
            int[] linsStatus = new int[1];
            //检测是否关联成功
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linsStatus, 0);
            if (linsStatus[0] != GLES20.GL_TRUE) {
                Log.d(TAG, "link program error");
                GLES20.glDeleteProgram(program);
                program = 0;
            }
        }
        return program;

    }

}