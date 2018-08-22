package com.zzw.live.camera;

import android.content.Context;
import android.opengl.GLES20;

import com.zzw.live.R;
import com.zzw.live.egl.EglSurfaceView;
import com.zzw.live.util.ShaderUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class CameraRender implements EglSurfaceView.Render {
    //顶点坐标
    static float vertexData[] = {   // in counterclockwise order:
            -1f, -1f, 0.0f, // bottom left
            1f, -1f, 0.0f, // bottom right
            -1f, 1f, 0.0f, // top left
            1f, 1f, 0.0f,  // top right
    };

    //纹理坐标  对应顶点坐标  与之映射
    static float textureData[] = {   // in counterclockwise order:
            0f, 1f, 0.0f, // bottom left
            1f, 1f, 0.0f, // bottom right
            0f, 0f, 0.0f, // top left
            1f, 0f, 0.0f,  // top right
    };
    //每一次取点的时候取几个点
    static final int COORDS_PER_VERTEX = 3;

    final int vertexCount = vertexData.length / COORDS_PER_VERTEX;
    //每一次取的总的点 大小
    static final int vertexStride = COORDS_PER_VERTEX * 4; // 4 bytes per vertex

    //位置
    protected FloatBuffer vertexBuffer;
    //纹理
    protected FloatBuffer textureBuffer;
    private int program;
    private int avPosition;

    //纹理位置
    private int afPosition;
    //纹理  默认第0个位置 可以不获取
    private int texture;


    //vbo id
    private int vboId;

    private int fboTextureId;

    private Context context;

    public CameraRender(Context context) {
        this.context = context;

        vertexBuffer = ByteBuffer.allocateDirect(vertexData.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(vertexData);
        vertexBuffer.position(0);

        textureBuffer = ByteBuffer.allocateDirect(textureData.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(textureData);
        textureBuffer.position(0);
    }

    @Override
    public void onSurfaceCreated() {
        program = ShaderUtil.createProgram(ShaderUtil.readRawTxt(context, R.raw.vertex_shader_screen),
                ShaderUtil.readRawTxt(context, R.raw.fragment_shader_screen));

        if (program > 0) {
            //获取顶点坐标字段
            avPosition = GLES20.glGetAttribLocation(program, "av_Position");
            //获取纹理坐标字段
            afPosition = GLES20.glGetAttribLocation(program, "af_Position");
            //获取纹理字段
            texture = GLES20.glGetUniformLocation(program, "sTexture");

            //创建vbo
            createVBO();
        }
    }

    @Override
    public void onSurfaceChanged(int width, int height) {
        //宽高
        GLES20.glViewport(0, 0, width, height);
    }

    @Override
    public void onDrawFrame() {
        //清空颜色
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        //设置背景颜色
        GLES20.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);

        //使用程序
        GLES20.glUseProgram(program);

        //设置纹理
        //绑定渲染纹理  默认是第0个位置
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, fboTextureId);

        GLES20.glEnableVertexAttribArray(avPosition);
        GLES20.glEnableVertexAttribArray(afPosition);

        //使用VBO设置纹理和顶点值
        useVboSetVertext();

//        //设置顶点位置值
//        GLES20.glVertexAttribPointer(avPosition, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, vertexStride, vertexBuffer);
//        //设置纹理位置值
//        GLES20.glVertexAttribPointer(afPosition, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, vertexStride, textureBuffer);

        //绘制 GLES20.GL_TRIANGLE_STRIP:复用坐标
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, vertexCount);
        GLES20.glDisableVertexAttribArray(avPosition);
        GLES20.glDisableVertexAttribArray(afPosition);
        //解绑纹理
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
    }


    public void onDraw(int fboTextureId) {
        this.fboTextureId = fboTextureId;
        onDrawFrame();
    }

    /**
     * 创建vbo
     */
    private void createVBO() {
        //1. 创建VBO
        int[] vbos = new int[1];
        GLES20.glGenBuffers(vbos.length, vbos, 0);
        vboId = vbos[0];
        //2. 绑定VBO
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboId);
        //3. 分配VBO需要的缓存大小
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, vertexData.length * 4 + textureData.length * 4, null, GLES20.GL_STATIC_DRAW);
        //4. 为VBO设置顶点数据的值
        GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, 0, vertexData.length * 4, vertexBuffer);
        GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, vertexData.length * 4, textureData.length * 4, textureBuffer);
        //5. 解绑VBO
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
    }


    /**
     * 使用vbo设置顶点位置
     */
    private void useVboSetVertext() {
        //1. 绑定VBO
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboId);
        //2. 设置顶点数据
        GLES20.glVertexAttribPointer(avPosition, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, vertexStride, 0);
        GLES20.glVertexAttribPointer(afPosition, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, vertexStride, vertexData.length * 4);
        //3. 解绑VBO
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
    }

}
