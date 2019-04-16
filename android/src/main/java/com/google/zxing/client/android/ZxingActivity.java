package com.google.zxing.client.android;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.widget.Space;
import android.support.v7.app.AppCompatActivity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.github2136.util.BitmapUtil;
import com.github2136.util.FileUtil;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Set;

public class ZxingActivity extends AppCompatActivity implements SurfaceHolder.Callback, Camera.PreviewCallback {
    public static final String ARG_RESULT = "RESULT";
    public static final String ARG_SCAN_PIC = "SCAN_PIC";
    public static final String ARG_SCAN_FLASH = "SCAN_FLASH";
    public static final String ARG_SCAN_TEXT = "SCAN_TEXT";
    public static final String ARG_SCAN_WIDTH_DP = "SCAN_WIDTH_DP";
    public static final String ARG_SCAN_HEIGHT_DP = "SCAN_HEIGHT_DP";
    public static final String ARG_SCAN_HEIGHT_SCALE = "SCAN_HEIGHT_SCALE";
    public static final String ARG_SCAN_COLOR = "SCAN_COLOR";
    private static final int REQUEST_SELECT_IMG = 249;
    public static final int MSG_RESULT = 869;
    public static final int MSG_RESULT_IMG = 464;//图片结果
    public static final int MSG_DECODE_FAIL = 415;//解码失败
    private Context mContext;
    private SurfaceView surfaceView;
    private ViewfinderView vView;
    private ImageButton ibFlash, ibScanning;
    private Space space;
    private ImageView iv;//结果图
    private Set<String> mMimeType = new HashSet<>();
    private DecodeThread mDecodeThread;
    private ResultHandler mResultHandler;
    private CameraManager cameraManager;
    private BeepManager beepManager;

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
        space = (Space) findViewById(R.id.spacer);
        ibFlash.setOnClickListener(mOnClickListener);
        ibScanning.setOnClickListener(mOnClickListener);
        iv = (ImageView) findViewById(R.id.iv);

        if (getIntent().hasExtra(ARG_SCAN_WIDTH_DP) && getIntent().hasExtra(ARG_SCAN_HEIGHT_DP)) {
            float density = getResources().getDisplayMetrics().density;
            int width = getIntent().getIntExtra(ARG_SCAN_WIDTH_DP, 200);
            int height = getIntent().getIntExtra(ARG_SCAN_HEIGHT_DP, 200);
            vView.setScanWidthPx((int) (width * density));
            vView.setScanHeightPx((int) (height * density));
        }
        if (getIntent().hasExtra(ARG_SCAN_PIC) && getIntent().getBooleanExtra(ARG_SCAN_PIC, false)) {
            ibScanning.setVisibility(View.VISIBLE);
        }
        if (getIntent().hasExtra(ARG_SCAN_FLASH) && getIntent().getBooleanExtra(ARG_SCAN_FLASH, false)) {
            ibFlash.setVisibility(View.VISIBLE);
        }
        if (ibScanning.getVisibility() == View.VISIBLE && ibFlash.getVisibility() == View.VISIBLE) {
            //两个按钮都显示
            space.setVisibility(View.VISIBLE);
        }
        if (getIntent().hasExtra(ARG_SCAN_TEXT)) {
            vView.setText(getIntent().getStringExtra(ARG_SCAN_TEXT));
        }

        if (getIntent().hasExtra(ARG_SCAN_COLOR)) {
            vView.setScanColor(getIntent().getIntExtra(ARG_SCAN_COLOR, Color.BLUE));
        }
        if (getIntent().hasExtra(ARG_SCAN_HEIGHT_SCALE)) {
            vView.setHeightScale(getIntent().getDoubleExtra(ARG_SCAN_HEIGHT_SCALE, 0.5));
        }
        beepManager = new BeepManager(this);
        mResultHandler = new ResultHandler(this);
        cameraManager = new CameraManager(this, surfaceView, vView, this, this);
        mDecodeThread = new DecodeThread(this, mResultHandler);
        mDecodeThread.start();
    }

    private View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int i = v.getId();
            if (i == R.id.ib_flash) {
                if (cameraManager.openFlash()) {
                    ibFlash.setImageResource(R.drawable.zxing_ic_flash_on);
                } else {
                    ibFlash.setImageResource(R.drawable.zxing_ic_flash_off);
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
        cameraManager.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        ibFlash.setImageResource(R.drawable.zxing_ic_flash_off);
        cameraManager.onPause();
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        Camera.Size size = camera.getParameters().getPreviewSize(); //获取预览大小
        DecodeHandler decodeHandler = mDecodeThread.getHandler();
        if (decodeHandler != null) {
            Point resultSize = new Point(vView.getScanWidthPx(), vView.getScanHeightPx());
            Point position = new Point((int) vView.getScanRet().left, (int) vView.getScanRet().top);
            Point scanSize = new Point(vView.getWidth(), vView.getHeight());
            decodeHandler.setSize(resultSize, position, scanSize);
            Message message = decodeHandler.obtainMessage(DecodeHandler.MSG_DECODE, size.width, size.height, data);
            message.sendToTarget();
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        cameraManager.openCamera(holder);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        cameraManager.startPreview(holder);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        cameraManager.destroyed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (beepManager != null) {
            beepManager.close();
        }
        cameraManager.onDestroy();
    }


    static class ResultHandler extends Handler {
        WeakReference<ZxingActivity> weakReference;

        public ResultHandler(ZxingActivity activity) {
            weakReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            ZxingActivity activity = weakReference.get();
            if (activity != null) {
                switch (msg.what) {
                    case MSG_RESULT:
                        if (msg.obj != null) {
                            Intent intent = new Intent();
                            activity.beepManager.playBeepSoundAndVibrate();
                            intent.putExtra(ARG_RESULT, msg.obj.toString());
                            activity.setResult(RESULT_OK, intent);
                            activity.finish();
                        } else {
                            Toast.makeText(activity, "未扫描到数据", Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case MSG_DECODE_FAIL:
                        activity.cameraManager.setOneShotPreviewCallback();
                        break;
                    case MSG_RESULT_IMG: {
                        if (msg.obj != null) {
                            Bitmap bitmap = (Bitmap) msg.obj;
                            if (activity.iv.getVisibility() == View.VISIBLE) {
                                activity.iv.setImageBitmap(bitmap);
                            }
                        }
                    }
                    break;
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_SELECT_IMG:
                    try {
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
                                                 DecodeHandler decodeHandler = mDecodeThread.getHandler();
                                                 if (decodeHandler != null) {
                                                     Message message = decodeHandler.obtainMessage(DecodeHandler.MSG_SCANNINGIMAGE, bitmap);
                                                     message.sendToTarget();
                                                 }
                                             }
                                         }
                                    );
                        } else {
                            Toast.makeText(this, "非图片类型文件(jpg、png、gif)", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(this, "没有文件读取权限", Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
        }
    }
}