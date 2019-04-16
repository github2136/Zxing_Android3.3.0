package com.google.zxing.client.android;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.common.GlobalHistogramBinarizer;
import com.google.zxing.common.HybridBinarizer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by yb on 2017/12/23.
 */

public class DecodeHandler extends Handler {
    public static final int MSG_DECODE = 272;//解码
    public static final int MSG_SCANNINGIMAGE = 565;//图片扫描
    private MultiFormatReader multiFormatReader;//解码类
    private Map<DecodeHintType, Object> hints;//解码类型
    private Context mContext;
    private Handler mResultHandler;
    private Point mSize;//裁剪后的尺寸
    private Point mPosition;//裁剪后的尺寸
    private Point mScanSize;//屏幕尺寸

    public DecodeHandler(Context context, Handler resultHandler) {
        mContext = context;
        mResultHandler = resultHandler;
        initHints();
        multiFormatReader = new MultiFormatReader();
        multiFormatReader.setHints(hints);
    }

    public void setSize(Point resultSize, Point position, Point scanSize) {
        mSize = resultSize;
        mPosition = position;
        mScanSize = scanSize;
    }

    private void initHints() {
        hints = new HashMap<>();
        Set<BarcodeFormat> decodeFormats = new HashSet<>();

        decodeFormats.add(BarcodeFormat.UPC_A);
        decodeFormats.add(BarcodeFormat.UPC_E);
        decodeFormats.add(BarcodeFormat.EAN_13);
        decodeFormats.add(BarcodeFormat.EAN_8);
        decodeFormats.add(BarcodeFormat.RSS_14);
        decodeFormats.add(BarcodeFormat.RSS_EXPANDED);

        decodeFormats.add(BarcodeFormat.CODE_39);
        decodeFormats.add(BarcodeFormat.CODE_93);
        decodeFormats.add(BarcodeFormat.CODE_128);
        decodeFormats.add(BarcodeFormat.ITF);
        decodeFormats.add(BarcodeFormat.CODABAR);

        decodeFormats.add(BarcodeFormat.QR_CODE);

        decodeFormats.add(BarcodeFormat.DATA_MATRIX);

        hints.put(DecodeHintType.POSSIBLE_FORMATS, decodeFormats);
//        是否使用HARDER模式来解析数据，如果启用，则会花费更多的时间去解析二维码，对精度有优化，对速度则没有。
//        hints.put(DecodeHintType.TRY_HARDER, true);
//        解析的字符集。这个对解析也比较关键，最好定义需要解析数据对应的字符集。
//        hints.put(DecodeHintType.CHARACTER_SET, "");
    }

    //预览图与控件比例
    private float sourceScale = 0;

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_DECODE:
                decode((byte[]) msg.obj, msg.arg1, msg.arg2);
                break;
            case MSG_SCANNINGIMAGE: {
                Bitmap scanBitmap = (Bitmap) msg.obj;
                Result result = null;
                RGBLuminanceSource source = null;
                try {
                    int width = scanBitmap.getWidth();
                    int height = scanBitmap.getHeight();
                    int[] pixels = new int[width * height];
                    scanBitmap.getPixels(pixels, 0, width, 0, 0, width, height);
                    source = new RGBLuminanceSource(width, height, pixels);
                    result = multiFormatReader.decode(new BinaryBitmap(new HybridBinarizer(source)));
                } catch (Exception e) {
                    e.printStackTrace();
                    if (source != null) {
                        try {
                            result = multiFormatReader.decode(new BinaryBitmap(new GlobalHistogramBinarizer(source)));
                        } catch (Throwable e2) {
                            e2.printStackTrace();
                        }
                    }

                } finally {
                    multiFormatReader.reset();
                    if (result != null) {
                        Message message = mResultHandler.obtainMessage(ZxingActivity.MSG_RESULT);
                        message.obj = result.toString();
                        message.sendToTarget();
                        getLooper().quit();
                    } else {
                        Message message = mResultHandler.obtainMessage(ZxingActivity.MSG_RESULT);
                        message.sendToTarget();
                    }
                }
            }
            break;
        }
    }

    private void decode(byte[] data, int width, int height) {
        long start = System.currentTimeMillis();
        if (sourceScale == 0) {
            sourceScale = height / Float.valueOf(mScanSize.x);
        }
        //获取预览图片，预览图片为横向显示
      /*  final YuvImage image = new YuvImage(data, ImageFormat.NV21, scanningWidth, scanningHeight, null);
        ByteArrayOutputStream os = new ByteArrayOutputStream(data.length);
        image.compressToJpeg(new Rect(0, 0, scanningWidth, scanningHeight), 100, os);
        byte[] tmp = os.toByteArray();
        Bitmap bmp = BitmapFactory.decodeByteArray(tmp, 0, tmp.length);*/
        PlanarYUVLuminanceSource source=null;
//        if (isVertical(width,height)==isVertical(mScanSize.x,mScanSize.y)) {
//        data = roate90YUVdata(data, width, height);
//        if (data != null) {
//            source = new PlanarYUVLuminanceSource(data, height, width,
//                    (int) (mPosition.x * sourceScale),
//                    (int) (mPosition.y * sourceScale),
//                    (int) (mSize.x * sourceScale),
//                    (int) (mSize.y * sourceScale),
//                    false);
//        }
//        }else{
            source = new PlanarYUVLuminanceSource(data, width, height,
                    (int) (mPosition.y * sourceScale),
                    (int) (mPosition.x * sourceScale),
                    (int) (mSize.y * sourceScale),
                    (int) (mSize.x * sourceScale),
                    false);
//        }

        //裁剪框图片
        if (false) {
            //显示裁剪框图片
            int[] pixels = source.renderThumbnail();
            int width1 = source.getThumbnailWidth();
            int height1 = source.getThumbnailHeight();
            Bitmap bitmap1 = Bitmap.createBitmap(pixels, 0, width1, width1, height1, Bitmap.Config.ARGB_8888);
            Message message = mResultHandler.obtainMessage(ZxingActivity.MSG_RESULT_IMG);
            message.obj = bitmap1;
            message.sendToTarget();
        }
        Result rawResult = null;
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
        try {
            rawResult = multiFormatReader.decodeWithState(bitmap);
        } catch (ReaderException re) {
            // continue
        } finally {
            multiFormatReader.reset();
        }
        if (rawResult != null) {
            long end = System.currentTimeMillis();
            Log.d("zxing", "Found barcode in " + (end - start) + " ms");
            Message message = mResultHandler.obtainMessage(ZxingActivity.MSG_RESULT);
            message.obj = rawResult.toString();
            message.sendToTarget();
            getLooper().quit();
        } else {
            if (mResultHandler != null) {
                Message message = Message.obtain(mResultHandler, ZxingActivity.MSG_DECODE_FAIL);
                message.sendToTarget();
            }
        }
    }

    public byte[] roate90YUVdata(byte[] yuvData, int width, int height) {
        byte[] rotatedData = null;
        if (yuvData != null) {
            rotatedData = new byte[yuvData.length];
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++)
                    rotatedData[x * height + height - y - 1] = yuvData[x + y * width];
            }
        }
        return rotatedData;
    }

    /**
     * 是否为竖置
     *
     * @param width
     * @param height
     * @return
     */
    private boolean isVertical(int width, int height) {
        return width < height;
    }
}
