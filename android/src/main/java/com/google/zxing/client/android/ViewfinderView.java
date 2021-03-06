package com.google.zxing.client.android;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.animation.LinearInterpolator;

import androidx.core.content.res.ResourcesCompat;

public final class ViewfinderView extends View {
    //空白高度比例0.0-1.0
    private double heightScale = 0.5;
    //扫描框宽度PX
    private int scanWidthPx;
    //扫描框高度PX
    private int scanHeightPx;
    //扫描框位置
    private RectF scanRet;
    //扫描框颜色
    private int scanColor;
    //扫描线颜色
    private int scanLineColor;
    //扫描线位置
    private float scanLine;
    //扫描线高度
    private float scanLineHeight;
    private Paint paint;
    private final int maskColor;
    private String text;
    private Path path = new Path();
    private RadialGradient radialGradient;
    //扫描线椭圆位置
    private RectF ovalRet;
    //屏幕密度
    float density;

    public ViewfinderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        path.setFillType(Path.FillType.EVEN_ODD);
        maskColor = ResourcesCompat.getColor(getResources(), R.color.viewfinder_mask, null);
        DisplayMetrics dm = getResources().getDisplayMetrics();
        density = dm.density;

        ObjectAnimator animator = ObjectAnimator.ofFloat(this, "scanLine", 0, 1);
        animator.setInterpolator(new LinearInterpolator());
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setRepeatMode(ValueAnimator.RESTART);
        animator.setDuration(2000);
        animator.start();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        int left = (w - scanWidthPx) / 2;
        int top = (int) ((h - scanHeightPx) * heightScale);
        int right = left + scanWidthPx;
        int bottom = top + scanHeightPx;
        scanRet = new RectF(left, top, right, bottom);
        radialGradient = new RadialGradient(scanRet.width() / 2,
                                            scanLineHeight / 2,
                                            scanRet.width(),
                                            scanLineColor,
                                            Color.TRANSPARENT,
                                            Shader.TileMode.CLAMP
        );
        ovalRet = new RectF(0, 0, 0, 0);
    }

    @Override
    public void onDraw(Canvas canvas) {
        //灰色四边填充
        path.reset();
        paint.reset();
        path.addRect(0, 0, getWidth(), getHeight(), Path.Direction.CW);
        path.addRect(scanRet, Path.Direction.CCW);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(maskColor);
        canvas.drawPath(path, paint);
        //扫描框
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(scanColor);
        paint.setStrokeWidth(0);
        canvas.drawRect(scanRet, paint);

        //提示文字
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        paint.setTextAlign(Paint.Align.CENTER);
        int textSize = (int) (getResources().getDisplayMetrics().scaledDensity * 16);
        paint.setTextSize(textSize);
        if (text == null) {
            text = "请将二维码置于取景框内扫描";
        }
        canvas.drawText(text, getWidth() / 2, scanRet.bottom + paint.getFontSpacing(), paint);
        //扫描线
        paint.setShader(radialGradient);
        canvas.clipRect(scanRet.left,
                        scanRet.top,
                        scanRet.right,
                        scanRet.bottom);
        canvas.save();
        canvas.translate(scanRet.left, scanRet.top + scanRet.height() * scanLine - scanLineHeight / 2);
        ovalRet.left = 0;
        ovalRet.top = 0;
        ovalRet.right = scanRet.width();
        ovalRet.bottom = scanLineHeight;
        canvas.drawOval(ovalRet, paint);
        canvas.restore();
    }

    public double getHeightScale() {
        return heightScale;
    }

    public void setHeightScale(double heightScale) {
        this.heightScale = heightScale;
    }

    public int getScanWidthPx() {
        return scanWidthPx;
    }

    public void setScanWidthPx(int scanWidthPx) {
        this.scanWidthPx = scanWidthPx;
    }

    public int getScanHeightPx() {
        return scanHeightPx;
    }

    public void setScanHeightPx(int scanHeightPx) {
        this.scanHeightPx = scanHeightPx;
    }

    public int getScanColor() {
        return scanColor;
    }

    public void setScanColor(int scanColor) {
        this.scanColor = scanColor;
    }

    public int getMaskColor() {
        return maskColor;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public RectF getScanRet() {
        return scanRet;
    }

    public float getScanLine() {
        return scanLine;
    }

    public void setScanLine(float scanLine) {
        this.scanLine = scanLine;
        invalidate();
    }

    public int getScanLineColor() {
        return scanLineColor;
    }

    public void setScanLineColor(int scanLineColor) {
        this.scanLineColor = scanLineColor;
    }

    public float getScanLineHeight() {
        return scanLineHeight;
    }

    public void setScanLineHeight(float scanLineHeight) {
        this.scanLineHeight = scanLineHeight * density;
    }
}
