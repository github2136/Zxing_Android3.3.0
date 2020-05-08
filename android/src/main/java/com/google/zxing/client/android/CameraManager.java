package com.google.zxing.client.android;

import android.app.Activity;
import android.content.DialogInterface;
import android.hardware.Camera;
import android.view.Gravity;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.FrameLayout;

import androidx.appcompat.app.AlertDialog;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * Created by yb on 2018/8/17.
 */
public class CameraManager {
    private static final int MIN_PREVIEW_PIXELS = 480 * 320; // normal screen
    private static final double MAX_ASPECT_DISTORTION = 0.15;
    private Camera camera;
    //预览尺寸
    private Camera.Size bestSize;
    //预览回调
    private Camera.PreviewCallback previewCallback;
    private SurfaceHolder.Callback callback;
    //预览控件
    private SurfaceView surfaceView;
    //展示框
    private ViewfinderView vView;
    private AutoFocusManager autoFocusManager;
    private boolean hasSurface;
    private Activity activity;

    public CameraManager(Activity activity, SurfaceView surfaceView, ViewfinderView vView,
                         Camera.PreviewCallback previewCallback, SurfaceHolder.Callback callback) {
        this.activity = activity;
        this.surfaceView = surfaceView;
        this.previewCallback = previewCallback;
        this.callback = callback;
        this.vView = vView;

        SurfaceHolder holder = surfaceView.getHolder();
        holder.setKeepScreenOn(true);
        holder.addCallback(callback);
        hasSurface = false;

    }

    /**
     * 打开摄像头
     */
    public void openCamera(SurfaceHolder holder) {
        hasSurface = true;
        try {
            // 打开相机
            camera = Camera.open();
            //设置预览回调
            camera.setPreviewCallback(previewCallback);
            // 设置预览
            camera.setPreviewDisplay(holder);
            int width = activity.getResources().getDisplayMetrics().widthPixels;
            int height = activity.getResources().getDisplayMetrics().heightPixels;
            bestSize = findBestPreviewSizeValue(width, height);//getBestPreviewSize(width, height);

            //预览尺寸与屏幕尺寸都为竖屏或横屏
            if (isVertical(width, height) == isVertical(bestSize.width, bestSize.height)) {
                int[] size = getViewSize(surfaceView.getWidth(), surfaceView.getHeight(), bestSize.width, bestSize.height);
                surfaceView.setLayoutParams(new FrameLayout.LayoutParams(size[0], size[1], Gravity.CENTER));
                vView.setLayoutParams(new FrameLayout.LayoutParams(size[0], size[1], Gravity.CENTER));
            } else {
                int[] size = getViewSize(surfaceView.getWidth(), surfaceView.getHeight(), bestSize.height, bestSize.width);
                surfaceView.setLayoutParams(new FrameLayout.LayoutParams(size[0], size[1], Gravity.CENTER));
                vView.setLayoutParams(new FrameLayout.LayoutParams(size[0], size[1], Gravity.CENTER));
            }
        } catch (Exception e) {
            e.printStackTrace();
            new AlertDialog.Builder(activity)
                    .setTitle("警告")
                    .setMessage("摄像头打开失败")
                    .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            activity.finish();
                        }
                    }).show();
        }
    }

    /**
     * 启动预览
     */
    public void startPreview(SurfaceHolder holder) {
        if (camera != null) {
            Camera.Parameters parameters = camera.getParameters();
            parameters.setPreviewSize(bestSize.width, bestSize.height);
            camera.setParameters(parameters);
            try {
                camera.setPreviewDisplay(holder);
            } catch (IOException e) {
                e.printStackTrace();
            }
            setCameraDisplayOrientation(activity, getDefaultCameraId(), camera);
            camera.startPreview();
            autoFocusManager = new AutoFocusManager(camera);
        }
    }

    public void onResume() {
        if (hasSurface) {
            SurfaceHolder holder = surfaceView.getHolder();
            openCamera(holder);
            startPreview(holder);
        }
    }

    public void onPause() {
        if (camera != null && autoFocusManager != null) {
            autoFocusManager.stop();
            autoFocusManager = null;
        }
        if (camera != null) {
            //停止预览
            camera.stopPreview();
            camera.setPreviewCallback(null);
            camera.release();
            camera = null;
        }
        if (!hasSurface) {
            SurfaceHolder surfaceHolder = surfaceView.getHolder();
            surfaceHolder.removeCallback(callback);
        }
    }

    public void onDestroy() {

    }

    /**
     * 界面销毁
     */
    public void destroyed() {
        hasSurface = false;
    }

    /**
     * 解码失败
     */
    public void setOneShotPreviewCallback() {
        if (camera != null) {
            camera.setOneShotPreviewCallback(previewCallback);
        }
    }

    /**
     * 打开或关闭闪光灯
     */
    public boolean openFlash() {
        boolean b = false;
        if (camera != null) {
            Camera.Parameters parameters = camera.getParameters();
            if (parameters != null) {
                if (parameters.getFlashMode().equals(Camera.Parameters.FLASH_MODE_TORCH)) {
                    parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                    b = false;
                } else {
                    parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                    b = true;
                }
                camera.setParameters(parameters);
            }
        }
        return b;
    }
    ///////////////////////////////////////////////////////////////////////////
    // 私有方法
    ///////////////////////////////////////////////////////////////////////////

    /**
     * 计算出预览控件实际大小
     *
     * @param width
     * @param height
     * @param cameraWidth
     * @param cameraHeight
     * @return
     */
    private int[] getViewSize(int width, int height, int cameraWidth, int cameraHeight) {
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
        int[] size = new int[2];

        size[0] = (int) (cameraWidth * scale);
        size[1] = (int) (cameraHeight * scale);
        return size;
    }

    /**
     * 获取与屏幕比例最接近的预览分辨率
     *
     * @param width
     * @param height
     * @return
     */
    private Camera.Size getBestPreviewSize(int width, int height) {
        Camera.Parameters parameters = camera.getParameters();
        Camera.Size bestSize = null;
        //不同机器 尺寸大小排序方式不一样  有的从小到大有的从大到小
        boolean exchange = false; //交换
        boolean vertical;//是否竖屏
        vertical = isVertical(width, height);
        Boolean verticalPreview = null;
        double scale, scalePreview;
        double diff, bestDiff = -1;
        long bestQuality = 0, quality;//质量
        if (vertical) {
            scale = (double) height / width;
        } else {
            scale = (double) width / height;
        }

        List<Camera.Size> supportedPreviewSizes = parameters.getSupportedPreviewSizes();
        Collections.sort(supportedPreviewSizes, new Comparator<Camera.Size>() {
            @Override
            public int compare(Camera.Size a, Camera.Size b) {
                int aPixels = a.height * a.width;
                int bPixels = b.height * b.width;
                if (bPixels < aPixels) {
                    return 1;
                }
                if (bPixels > aPixels) {
                    return -1;
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
                temp = width;
                width = height;
                height = temp;
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
     * 是否为竖置
     *
     * @param width
     * @param height
     * @return
     */
    private boolean isVertical(int width, int height) {
        return width < height;
    }

    /**
     * 摄像头旋转
     *
     * @param activity
     * @param cameraId
     * @param camera
     */
    private void setCameraDisplayOrientation(Activity activity, int cameraId, Camera camera) {
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

    /**
     * 后置摄像头ID
     *
     * @return
     */
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


    private  Camera.Size findBestPreviewSizeValue(int width, int height) {
        Camera.Parameters parameters = camera.getParameters();
        List<Camera.Size> rawSupportedSizes = parameters.getSupportedPreviewSizes();
        if (rawSupportedSizes == null) {

            Camera.Size defaultSize = parameters.getPreviewSize();
            if (defaultSize == null) {
                throw new IllegalStateException("Parameters contained no preview size!");
            }
            return defaultSize;
        }

        // Sort by size, descending
        List<Camera.Size> supportedPreviewSizes = new ArrayList<>(rawSupportedSizes);
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


        double screenAspectRatio = height / (double) width;

        // Remove sizes that are unsuitable
        Iterator<Camera.Size> it = supportedPreviewSizes.iterator();
        while (it.hasNext()) {
            Camera.Size supportedPreviewSize = it.next();
            int realWidth = supportedPreviewSize.width;
            int realHeight = supportedPreviewSize.height;
            if (realWidth * realHeight < MIN_PREVIEW_PIXELS) {
                it.remove();
                continue;
            }

            boolean isCandidatePortrait = realWidth < realHeight;
            int maybeFlippedWidth = isCandidatePortrait ? realHeight : realWidth;
            int maybeFlippedHeight = isCandidatePortrait ? realWidth : realHeight;
            double aspectRatio = maybeFlippedWidth / (double) maybeFlippedHeight;
            double distortion = Math.abs(aspectRatio - screenAspectRatio);
            if (distortion > MAX_ASPECT_DISTORTION) {
                it.remove();
                continue;
            }

            if (maybeFlippedWidth == height && maybeFlippedHeight == width) {
                Camera.Size size = supportedPreviewSizes.get(0);
                size.width = realWidth;
                size.height = realHeight;

                return size;
            }
        }

        // If no exact match, use largest preview size. This was not a great idea on older devices because
        // of the additional computation needed. We're likely to get here on newer Android 4+ devices, where
        // the CPU is much more powerful.
        if (!supportedPreviewSizes.isEmpty()) {
            Camera.Size largestPreview = supportedPreviewSizes.get(0);
            Camera.Size size = supportedPreviewSizes.get(0);
            size.width = largestPreview.width;
            size.height = largestPreview.height;

            return size;
        }

        // If there is nothing at all suitable, return current preview size
        Camera.Size defaultPreview = parameters.getPreviewSize();
        if (defaultPreview == null) {
            throw new IllegalStateException("Parameters contained no preview size!");
        }
//        Point defaultSize = new Point(defaultPreview.width, defaultPreview.height);

        return defaultPreview;
    }
}