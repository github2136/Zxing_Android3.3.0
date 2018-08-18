package com.google.zxing.client.android;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.CountDownLatch;

/**
 * Created by yb on 2017/12/23.
 */

public class DecodeThread extends Thread {
    private Context mContext;
    private DecodeHandler mDecodeHandler;//解码
    private Handler mResultHandler;//返回结果
  private final CountDownLatch handlerInitLatch;
    public DecodeThread(Context context, Handler resultHandler) {
        mContext = context;
        mResultHandler = resultHandler;
            handlerInitLatch = new CountDownLatch(1);
    }

    public DecodeHandler getHandler() {
        try {
            handlerInitLatch.await();
        } catch (InterruptedException ie) {
            // continue?
        }
        return mDecodeHandler;
    }

    @Override
    public void run() {
        Looper.prepare();
        mDecodeHandler = new DecodeHandler(mContext, mResultHandler);
        handlerInitLatch.countDown();
        Looper.loop();
    }
}
