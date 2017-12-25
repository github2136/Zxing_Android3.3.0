package com.google.zxing.client.android;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

/**
 * Created by yb on 2017/12/23.
 */

public class DecodeThread extends Thread {
    private Context mContext;
    private DecodeHandler mDecodeHandler;//解码
    private Handler mResultHandler;//返回结果

    public DecodeThread(Context context, Handler resultHandler) {
        mContext = context;
        mResultHandler = resultHandler;
    }

    public DecodeHandler getHandler() {
        return mDecodeHandler;
    }

    @Override
    public void run() {
        Looper.prepare();
        mDecodeHandler = new DecodeHandler(mContext, mResultHandler);
        Looper.loop();
    }
}
