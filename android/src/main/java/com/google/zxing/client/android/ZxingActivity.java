package com.google.zxing.client.android;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
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
import android.webkit.MimeTypeMap;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.github2136.util.BitmapUtil;
import com.github2136.util.FileUtil;
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

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ZxingActivity extends AppCompatActivity implements SurfaceHolder.Callback, Camera.PreviewCallback {
    public static final String ARG_RESULT = "RESULT";
    public static final String ARG_SCAN_PIC = "SCAN_PIC";
    public static final String ARG_SCAN_WIDTH_PX = "SCAN_WIDTH_PX";
    public static final String ARG_SCAN_HEIGHT_PX = "SCAN_HEIGHT_PX";
    public static final String ARG_SCAN_WIDTH_DP = "SCAN_WIDTH_DP";
    public static final String ARG_SCAN_HEIGHT_DP = "SCAN_HEIGHT_DP";
    public static final String ARG_SCAN_HEIGHT_SCALE = "SCAN_HEIGHT_SCALE";
    public static final String ARG_SCAN_COLOR = "SCAN_COLOR";
    private static final int REQUEST_SELECT_IMG = 249;
    private Context mContext;
    private SurfaceView surfaceView;
    private ViewfinderView vView;
    private ImageButton ibFlash, ibScanning;
    private ImageView iv;//结果图
    private Camera camera;
    private Camera.Size bestSize;
    private boolean hasSurface;
    private AutoFocusManager autoFocusManager;//自动对焦类
    private MultiFormatReader multiFormatReader;//解码类
    private BeepManager beepManager;
    private Map<DecodeHintType, Object> hints;//解码类型
    private boolean mRotation = false;//结果图旋转
    private Set<String> mMimeType = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_zxing);
        mMimeType.add("image/jpeg");
        mMimeType.add("image/png");
        mMimeType.add("image/gif");
        mContext = this;
        surfaceView = (SurfaceView) findViewById(R.id.preview_view);
        vView = (ViewfinderView) findViewById(R.id.vv_view);
        ibFlash = (ImageButton) findViewById(R.id.ib_flash);
        ibScanning = (ImageButton) findViewById(R.id.ib_scanning);
        ibFlash.setOnClickListener(mOnClickListener);
        ibScanning.setOnClickListener(mOnClickListener);
        iv = (ImageView) findViewById(R.id.iv);

//        initHints();
        if (getIntent().hasExtra(ARG_SCAN_WIDTH_PX) && getIntent().hasExtra(ARG_SCAN_HEIGHT_PX)) {
            int width = getIntent().getIntExtra(ARG_SCAN_WIDTH_PX, 600);
            int height = getIntent().getIntExtra(ARG_SCAN_HEIGHT_PX, 600);
            vView.setScanWidthPx(width);
            vView.setScanHeightPx(height);
        } else if (getIntent().hasExtra(ARG_SCAN_WIDTH_DP) && getIntent().hasExtra(ARG_SCAN_HEIGHT_DP)) {
            int width = getIntent().getIntExtra(ARG_SCAN_WIDTH_DP, 200);
            int height = getIntent().getIntExtra(ARG_SCAN_HEIGHT_DP, 200);
            vView.setScanWidthDp(width);
            vView.setScanHeightDp(height);
        }
        if (getIntent().hasExtra(ARG_SCAN_PIC) && getIntent().getBooleanExtra(ARG_SCAN_PIC, false)) {
            ibScanning.setVisibility(View.VISIBLE);
        }

        if (getIntent().hasExtra(ARG_SCAN_COLOR)) {
            vView.setScanColor(getIntent().getIntExtra(ARG_SCAN_COLOR, Color.BLUE));
        }
        if (getIntent().hasExtra(ARG_SCAN_HEIGHT_SCALE)) {
            vView.setHeightScale(getIntent().getDoubleExtra(ARG_SCAN_HEIGHT_SCALE, 0.5));
        }
        vView.reset();

        hasSurface = false;

        surfaceView = (SurfaceView) findViewById(R.id.preview_view);
        SurfaceHolder holder = surfaceView.getHolder();
        holder.setKeepScreenOn(true);
        holder.addCallback(this);

        multiFormatReader = new MultiFormatReader();
        multiFormatReader.setHints(hints);
        beepManager = new BeepManager(this);
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

    private View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int i = v.getId();
            if (i == R.id.ib_flash) {
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
            } else if (i == R.id.ib_scanning) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(intent, REQUEST_SELECT_IMG);
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        if (hasSurface) {
            SurfaceHolder surfaceHolder = surfaceView.getHolder();
            openCamera(surfaceHolder);
            startPreview(surfaceHolder);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        ibFlash.setImageResource(R.drawable.zxing_ic_flash_off);
        if (camera != null) {
            camera.stopPreview();
            camera.setPreviewCallback(null);
            // 释放相机资源
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

    //预览图与屏幕比例
    private float sourceScale = 0;

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        Camera.Size size = camera.getParameters().getPreviewSize(); //获取预览大小
        int w;  //宽度
        int h;
        byte[] b;
        if (mRotation) {
            w = size.height;  //宽度
            h = size.width;
            b = roate90YUVdata(data, size);
        } else {
            w = size.width;  //宽度
            h = size.height;
            b = data;
        }
        if (sourceScale == 0) {
            sourceScale = w / (float) getResources().getDisplayMetrics().widthPixels;
        }
        //获取预览图片，预览图片为横向显示
      /*  final YuvImage image = new YuvImage(data, ImageFormat.NV21, scanningWidth, scanningHeight, null);
        ByteArrayOutputStream os = new ByteArrayOutputStream(data.length);
        image.compressToJpeg(new Rect(0, 0, scanningWidth, scanningHeight), 100, os);
        byte[] tmp = os.toByteArray();
        Bitmap bmp = BitmapFactory.decodeByteArray(tmp, 0, tmp.length);*/


        //裁剪框图片
        PlanarYUVLuminanceSource source = new PlanarYUVLuminanceSource(b, w, h,
                (int) ((w - vView.getScanWidthPx() * sourceScale) / 2),
                (int) ((h - vView.getScanHeightPx() * sourceScale) * vView.getHeightScale()),
                (int) (vView.getScanWidthPx() * sourceScale),
                (int) (vView.getScanHeightPx() * sourceScale), false);


        if (iv.getVisibility() == View.VISIBLE) {
            //显示裁剪框图片
            int[] pixels = source.renderThumbnail();
            int width = source.getThumbnailWidth();
            int height = source.getThumbnailHeight();
            Bitmap bitmap1 = Bitmap.createBitmap(pixels, 0, width, width, height, Bitmap.Config.ARGB_8888);
            iv.setImageBitmap(bitmap1);
        }
        Result rawResult = null;

        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

        try {
            rawResult = multiFormatReader.decodeWithState(bitmap);
        } catch (ReaderException re) {
        } finally {
            multiFormatReader.reset();
        }

        if (rawResult != null) {
            beepManager.playBeepSoundAndVibrate();
            Intent intent = new Intent();
            intent.putExtra(ARG_RESULT, rawResult.toString());
            setResult(RESULT_OK, intent);
            finish();
        }
    }

    public static byte[] roate90YUVdata(byte[] yuvData, Camera.Size size) {
        byte[] rotatedData = new byte[yuvData.length];
        for (int y = 0; y < size.height; y++) {
            for (int x = 0; x < size.width; x++)
                rotatedData[x * size.height + size.height - y - 1] = yuvData[x + y * size.width];
        }
        int tmp = size.width;
        size.width = size.height;
        size.height = tmp;
        return rotatedData;
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
        startPreview(holder);
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

    private void startPreview(SurfaceHolder holder) {
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

    public void setCameraDisplayOrientation(Activity activity, int cameraId, Camera camera) {
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
        if (result == 90 || result == 270) {
            mRotation = true;
        } else {
            mRotation = false;
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_SELECT_IMG:
                    String p = FileUtil.getFileAbsolutePath(this, data.getData());
                    String suffix = FileUtil.getSuffix(p);
                    //获取文件后缀
                    MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
                    if (mMimeType.contains(mimeTypeMap.getMimeTypeFromExtension(suffix))) {
                        BitmapUtil
                                .getInstance(p)
                                .limit(1080)
                                .get(new BitmapUtil.BitmapGetCallBack() {
                                         @Override
                                         public void callback(Bitmap bitmap) {
                                             scanningImage(bitmap);
                                         }
                                     }
                                );

                    } else {
                        Toast.makeText(this, "非图片类型文件(jpg、png、gif)", Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
        }
    }

    private void scanningImage(Bitmap scanBitmap) {
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
                beepManager.playBeepSoundAndVibrate();
                Intent intent = new Intent();
                intent.putExtra(ARG_RESULT, result.toString());
                setResult(RESULT_OK, intent);
                finish();
            } else {
                Toast.makeText(mContext, "未扫描到数据", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
