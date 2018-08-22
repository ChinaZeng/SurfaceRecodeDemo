package com.zzw.live.encodec;

import android.content.Context;
import android.util.Log;

public class VideoEncodeRecode extends BaseVideoEncoder {

    public VideoEncodeRecode(Context context, int textureId) {
        super(context);
        Log.e("zzz","id = "+textureId);
        setRender(new VideoEncodeRender(context,textureId));
        setRenderMode(RENDERMODE_CONTINUOUSLY);
    }

}
