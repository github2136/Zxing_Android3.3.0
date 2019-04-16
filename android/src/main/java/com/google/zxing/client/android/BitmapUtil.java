package com.google.zxing.client.android;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Handler;
import android.os.Looper;

import java.io.ByteArrayOutputStream;

public class BitmapUtil {
    //图片路径
    private String mFilePath;
    //旋转角度
    private int mDegree;
    //宽高最大值
    private int mMax;
    //文件最大值
    private int mMaxSize;
    //图片保存质量
    private int mQuality = 100;
    private Handler mHandler;

    private BitmapUtil() {
        mHandler = new Handler(Looper.getMainLooper());
    }

    public static BitmapUtil getInstance(String filePath) {
        BitmapUtil mBitmapUtil = new BitmapUtil();
        mBitmapUtil.init(filePath);
        return mBitmapUtil;
    }

    /**
     * 数据初始化
     */
    private void init(String filePath) {
        mFilePath = filePath;
        mDegree = 0;
        mMax = 0;
        mMaxSize = 0;
    }

    /**
     * 获取图片
     */
    private Bitmap getBitmap(String filePath, int scale) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        options.inSampleSize = scale;
        Bitmap bitmap = BitmapFactory.decodeFile(filePath, options);
        return bitmap;
    }

    /**
     * 按设置获取图片的bitmap
     */
    private Bitmap getBitmap() {
        Bitmap mBitmap;
        if (mMax != 0) {
            int[] values = getBitmapValue(mFilePath);
            int scaleFactor = (int) Math.max(Math.ceil((double) values[0] / mMax), Math.ceil((double) values[1] / mMax));
            int scaleSize;
            if (scaleFactor > 1) {
                int inSampleSize = 1;
                while (inSampleSize << 1 < scaleFactor) {
                    inSampleSize <<= 1;
                }
                scaleSize = inSampleSize;
            } else {
                scaleSize = scaleFactor;
            }
            //使用inSampleSize压缩图片
            mBitmap = getBitmap(mFilePath, scaleSize);
            if (mBitmap != null) {
                //如果图片高宽比限制大则使用Matrix再次缩小
                if (mBitmap.getWidth() > mMax || mBitmap.getHeight() > mMax) {
                    float scaleW = (float) mMax / mBitmap.getWidth();
                    float scaleH = (float) mMax / mBitmap.getHeight();
                    float scale = scaleW > scaleH ? scaleH : scaleW;
                    mBitmap = getBitmap(mBitmap, scale);
                }
            }
        } else {
            mBitmap = getBitmap(mFilePath, 1);
        }
        if (mMaxSize != 0) {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            mQuality = 110;
            do {
                os.reset();
                mQuality -= 10;
                mBitmap.compress(Bitmap.CompressFormat.JPEG, mQuality, os);
            } while (mQuality > 0 && os.toByteArray().length / 1024 > mMaxSize);
        }
        if (mDegree > 0) {
            mBitmap = rotateBitmapByDegree(mBitmap, mDegree);
        }
        return mBitmap;
    }

    /**
     * 获得图片宽高信息 [0]:width [1]:height
     */
    private int[] getBitmapValue(String filePath) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, options);
        int[] value = new int[2];
        value[0] = options.outWidth;
        value[1] = options.outHeight;
        return value;
    }

    /**
     * 将图片按照某个角度进行旋转
     */
    private Bitmap rotateBitmapByDegree(Bitmap sourceBitmap, int degree) {
        Bitmap retBitmap;
        // 根据旋转角度，生成旋转矩阵
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        // 将原始图片按照旋转矩阵进行旋转，并得到新的图片
        retBitmap = Bitmap.createBitmap(sourceBitmap, 0, 0, sourceBitmap.getWidth(), sourceBitmap.getHeight(),
                                        matrix, true);
        sourceBitmap.recycle();
        sourceBitmap = null;
        System.gc();
        return retBitmap;
    }

    /**
     * 压缩至指定尺寸
     *
     * @param sourceBitmap
     * @param scale
     * @return
     */
    private Bitmap getBitmap(Bitmap sourceBitmap, float scale) {
        Bitmap retBitmap;
        // 根据旋转角度，生成旋转矩阵
        Matrix matrix = new Matrix();
        matrix.setScale(scale, scale);
        // 将原始图片按照比例矩阵进行压缩，并得到新的图片
        retBitmap = Bitmap.createBitmap(sourceBitmap, 0, 0, sourceBitmap.getWidth(), sourceBitmap.getHeight(),
                                        matrix, true);
        sourceBitmap.recycle();
        sourceBitmap = null;
        System.gc();
        return retBitmap;
    }

    ///////////////////////////////////////////////////////////////////////////
    // 公开方法
    ///////////////////////////////////////////////////////////////////////////

    /**
     * 获取图片回调
     */
    public interface BitmapGetCallBack {
        void callback(Bitmap bitmap);
    }

    /**
     * 宽高的最大值
     */
    public BitmapUtil limit(int max) {
        mMax = max;
        return this;
    }

    /**
     * 获取图片
     */
    public void get(final BitmapGetCallBack callBack) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final Bitmap mBitmap = getBitmap();
                if (callBack != null) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            callBack.callback(mBitmap);
                        }
                    });
                }
            }
        }).start();
    }
}