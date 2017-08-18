package com.google.zxing.client.android;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Point;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ZxingActivity extends AppCompatActivity implements SurfaceHolder.Callback, Camera.PreviewCallback {
    public static final String KEY_RESULT = "RESULT";
    public static final String KEY_SCAN_WIDTH_PX = "SCAN_WIDTH_PX";
    public static final String KEY_SCAN_HEIGHT_PX = "SCAN_HEIGHT_PX";
    public static final String KEY_SCAN_WIDTH_DP = "SCAN_WIDTH_DP";
    public static final String KEY_SCAN_HEIGHT_DP = "SCAN_HEIGHT_DP";
    public static final String KEY_SCAN_HEIGHT_SCALE = "SCAN_HEIGHT_SCALE";
    public static final String KEY_SCAN_COLOR = "SCAN_COLOR";
    private SurfaceView surfaceView;
    private ViewfinderView vView;
    private ImageButton ibFlash;
    //    private ImageView iv;
    private Camera camera;
    private Camera.Size bestSize;
    private boolean hasSurface;
    private AutoFocusManager autoFocusManager;//自动对焦类
    private MultiFormatReader multiFormatReader;//解码类
    private BeepManager beepManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_zxing);
        surfaceView = (SurfaceView) findViewById(R.id.preview_view);
        vView = (ViewfinderView) findViewById(R.id.vv_view);
        ibFlash = (ImageButton) findViewById(R.id.ib_flash);
        ibFlash.setOnClickListener(mOnClickListener);
//        iv = (ImageView) findViewById(R.id.iv);

        if (getIntent().hasExtra(KEY_SCAN_WIDTH_PX) && getIntent().hasExtra(KEY_SCAN_HEIGHT_PX)) {
            int width = getIntent().getIntExtra(KEY_SCAN_WIDTH_PX, 600);
            int height = getIntent().getIntExtra(KEY_SCAN_HEIGHT_PX, 600);
            vView.setScanWidthPx(width);
            vView.setScanHeightPx(height);
        } else if (getIntent().hasExtra(KEY_SCAN_WIDTH_DP) && getIntent().hasExtra(KEY_SCAN_HEIGHT_DP)) {
            int width = getIntent().getIntExtra(KEY_SCAN_WIDTH_DP, 200);
            int height = getIntent().getIntExtra(KEY_SCAN_HEIGHT_DP, 200);
            vView.setScanWidthDp(width);
            vView.setScanHeightDp(height);
        }
        if (getIntent().hasExtra(KEY_SCAN_COLOR)) {
            vView.setScanColor(getIntent().getIntExtra(KEY_SCAN_COLOR, Color.BLUE));
        }
        if (getIntent().hasExtra(KEY_SCAN_HEIGHT_SCALE)) {
            vView.setHeightScale(getIntent().getDoubleExtra(KEY_SCAN_HEIGHT_SCALE, 0.5));
        }
        vView.reset();

        hasSurface = false;

        surfaceView = (SurfaceView) findViewById(R.id.preview_view);
        SurfaceHolder holder = surfaceView.getHolder();
        holder.setKeepScreenOn(true);
        holder.addCallback(this);

        multiFormatReader = new MultiFormatReader();
        beepManager = new BeepManager(this);
    }

    private View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (camera != null) {
                Camera.Parameters parameters = camera.getParameters();
                if (parameters != null) {
                    if (parameters.getFlashMode().equals(Camera.Parameters.FLASH_MODE_TORCH)) {
                        parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                        ibFlash.setImageResource(R.drawable.zxing_ic_flash_off);
                    } else {
                        parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                        ibFlash.setImageResource(R.drawable.zxing_ic_flash_on);
                    }
                    camera.setParameters(parameters);
                }
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        if (hasSurface) {
            SurfaceHolder surfaceHolder = surfaceView.getHolder();
            openCamera(surfaceHolder);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        ibFlash.setImageResource(R.drawable.zxing_ic_flash_off);
        if (camera != null) {
            camera.setPreviewCallback(null);
            camera.release();
            camera = null;
        }
        if (!hasSurface) {
            SurfaceView surfaceView = (SurfaceView) findViewById(R.id.preview_view);
            SurfaceHolder surfaceHolder = surfaceView.getHolder();
            surfaceHolder.removeCallback(this);
        }
        if (autoFocusManager != null) {
            autoFocusManager.stop();
            autoFocusManager = null;
        }
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        Camera.Size size = camera.getParameters().getPreviewSize(); //获取预览大小
        final int w = size.width;  //宽度
        final int h = size.height;
        //获取预览图片，预览图片为横向显示
      /*  final YuvImage image = new YuvImage(data, ImageFormat.NV21, scanningWidth, scanningHeight, null);
        ByteArrayOutputStream os = new ByteArrayOutputStream(data.length);
        image.compressToJpeg(new Rect(0, 0, scanningWidth, scanningHeight), 100, os);
        byte[] tmp = os.toByteArray();
        Bitmap bmp = BitmapFactory.decodeByteArray(tmp, 0, tmp.length);*/
        //裁剪框图片
        PlanarYUVLuminanceSource source = new PlanarYUVLuminanceSource(data, w, h,
                (int) ((w - vView.getScanHeightPx()) * vView.getHeightScale()),
                (h - vView.getScanWidthPx()) / 2,
                vView.getScanHeightPx(), vView.getScanWidthPx(), false);
        //显示裁剪框图片
  /*      int[] pixels = source.renderThumbnail();
        int width = source.getThumbnailWidth();
        int height = source.getThumbnailHeight();
        Bitmap bitmap1 = Bitmap.createBitmap(pixels, 0, width, width, height, Bitmap.Config.ARGB_8888);
        iv.setImageBitmap(bitmap1);*/
        Result rawResult = null;
        if (source != null) {
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
            try {
                rawResult = multiFormatReader.decodeWithState(bitmap);
            } catch (ReaderException re) {
            } finally {
                multiFormatReader.reset();
            }
        }
        if (rawResult != null) {
            beepManager.playBeepSoundAndVibrate();
            Intent intent = new Intent();
            intent.putExtra(KEY_RESULT, rawResult.toString());
            setResult(RESULT_OK, intent);
            finish();
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (!hasSurface) {
            hasSurface = true;
            openCamera(holder);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Camera.Parameters parameters = camera.getParameters();
        int[] fps = new int[]{0, 0};
        for (int[] i : parameters.getSupportedPreviewFpsRange()) {
            if (fps[0] <= i[0] && fps[1] <= i[1]) {
                fps = i;
            }
        }
        parameters.setPreviewFpsRange(fps[0], fps[1]);
        parameters.setPreviewSize(bestSize.width, bestSize.height);

        camera.setParameters(parameters);

        try {
            camera.setPreviewDisplay(holder);
        } catch (IOException e) {
            e.printStackTrace();
        }
        setCameraDisplayOrientation(this, getDefaultCameraId(), camera);
        camera.startPreview();
        autoFocusManager = new AutoFocusManager(this, camera);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        hasSurface = false;
    }

    @Override
    protected void onDestroy() {
        if (beepManager != null) {
            beepManager.close();
        }
        super.onDestroy();
    }

    private void openCamera(SurfaceHolder holder) {
        try {
            // 打开相机
            camera = Camera.open();
            camera.setPreviewCallback(this);
            // 设置预览
            camera.setPreviewDisplay(holder);
            Camera.Parameters parameters = camera.getParameters();
            int width = getResources().getDisplayMetrics().widthPixels;
            int height = getResources().getDisplayMetrics().heightPixels;
            bestSize = getBestPreviewSize(width, height, parameters);

            if (isVertical(width, height) == isVertical(bestSize.width, bestSize.height)) {
                Point point = getViewSize(surfaceView.getWidth(), surfaceView.getHeight(), bestSize.width, bestSize.height);
                surfaceView.setLayoutParams(new FrameLayout.LayoutParams(point.x, point.y, Gravity.CENTER));
                vView.setLayoutParams(new FrameLayout.LayoutParams(point.x, point.y, Gravity.CENTER));
            } else {
                Point point = getViewSize(surfaceView.getWidth(), surfaceView.getHeight(), bestSize.height, bestSize.width);
                surfaceView.setLayoutParams(new FrameLayout.LayoutParams(point.x, point.y, Gravity.CENTER));
                vView.setLayoutParams(new FrameLayout.LayoutParams(point.x, point.y, Gravity.CENTER));
            }
        } catch (IOException e) {
            e.printStackTrace();
            new AlertDialog.Builder(this)
                    .setTitle("警告")
                    .setMessage("摄像头打开失败")
                    .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    }).show();
        }
    }

    private Camera.Size getBestPreviewSize(int surfaceViewWidth, int surfaceViewHeight, Camera.Parameters parameters) {
        Camera.Size bestSize = null;
        //不同机器 尺寸大小排序方式不一样  有的从小到大有的从大到小
        boolean exchange = false; //交换
        boolean vertical;//是否竖屏
        vertical = isVertical(surfaceViewWidth, surfaceViewHeight);
        Boolean verticalPreview = null;
        double scale, scalePreview;
        double diff, bestDiff = -1;
        long bestQuality = 0, quality;//质量
        if (vertical) {
            scale = (double) surfaceViewHeight / surfaceViewWidth;
        } else {
            scale = (double) surfaceViewWidth / surfaceViewHeight;
        }

        List<Camera.Size> supportedPreviewSizes = parameters.getSupportedPreviewSizes();
        Collections.sort(supportedPreviewSizes, new Comparator<Camera.Size>() {
            @Override
            public int compare(Camera.Size a, Camera.Size b) {
                int aPixels = a.height * a.width;
                int bPixels = b.height * b.width;
                if (bPixels < aPixels) {
                    return -1;
                }
                if (bPixels > aPixels) {
                    return 1;
                }
                return 0;
            }
        });

        for (Camera.Size size : supportedPreviewSizes) {
            if (verticalPreview == null) {
                verticalPreview = new Boolean(isVertical(size.width, size.height));
            }
            if (!exchange && vertical != verticalPreview) {
                int temp;
                temp = surfaceViewWidth;
                surfaceViewWidth = surfaceViewHeight;
                surfaceViewHeight = temp;
                exchange = true;
            }
            if (verticalPreview) {
                scalePreview = (double) size.height / size.width;
            } else {
                scalePreview = (double) size.width / size.height;
            }
            diff = Math.abs(scale - scalePreview);
            quality = size.width * size.height;
            if (bestDiff == -1) {
                bestDiff = diff;
                bestSize = size;
                bestQuality = quality;
            } else {
                if (diff < bestDiff && quality >= bestQuality) {
                    bestQuality = quality;
                    bestDiff = diff;
                    bestSize = size;
                }
            }
        }
        return bestSize;
    }

    /**
     * 是否为竖屏
     *
     * @param width
     * @param height
     * @return
     */
    private boolean isVertical(int width, int height) {
        return width < height;
    }

    private Point getViewSize(int width, int height, int cameraWidth, int cameraHeight) {
        double scale;
        double widthScale;
        double heightScale;
        widthScale = (double) width / cameraWidth;
        heightScale = (double) height / cameraHeight;
        if (widthScale > heightScale) {
            scale = widthScale;
        } else {
            scale = heightScale;
        }
        Point point = new Point();
        point.x = (int) (cameraWidth * scale);
        point.y = (int) (cameraHeight * scale);
        return point;
    }

    public static void setCameraDisplayOrientation(Activity activity, int cameraId, Camera camera) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }

    private int getDefaultCameraId() {
        int defaultId = -1;
        int mNumberOfCameras = Camera.getNumberOfCameras();

        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        for (int i = 0; i < mNumberOfCameras; i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                defaultId = i;
            }
        }
        if (-1 == defaultId) {
            if (mNumberOfCameras > 0) {
                // 如果没有后向摄像头
                defaultId = 0;
            } else {
                // 没有摄像头
            }
        }
        return defaultId;
    }
}
