package com.google.zxing.client.android;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.support.v4.content.res.ResourcesCompat;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

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
    //扫描线位置
    private float scanLine;
    //扫描线高度
    private float scanLineHeight;
    private Paint paint;
    private final int maskColor;
    private String text;
    private Path path = new Path();
    private LinearGradient linearGradient;

    public ViewfinderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        path.setFillType(Path.FillType.EVEN_ODD);
        maskColor = ResourcesCompat.getColor(getResources(), R.color.viewfinder_mask, null);
        scanColor = Color.BLUE;
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
        int right = left + scanWidthPx;
        int top = (int) ((h - scanHeightPx) * heightScale);
        int bottom = top + scanHeightPx;
        scanRet = new RectF(left, top, right, bottom);
        float density = getResources().getDisplayMetrics().density;
        scanLineHeight = density * 20;
        linearGradient = new LinearGradient(scanRet.width() / 2,
                                            0,
                                            scanRet.width() / 2,
                                            scanLineHeight,
                                            new int[]{
                                                    Color.TRANSPARENT,
                                                    Color.parseColor("#cc0000ff"),
                                                    Color.TRANSPARENT
                                            },
                                            null,
                                            Shader.TileMode.CLAMP);
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
        paint.setColor(Color.GREEN);
        paint.setStrokeWidth(2);
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
        paint.setShader(linearGradient);
        canvas.clipRect(scanRet.left,
                        scanRet.top,
                        scanRet.right,
                        scanRet.bottom);
        canvas.save();
        canvas.translate(scanRet.left, scanRet.top + scanRet.height() * scanLine - scanLineHeight / 2);
        canvas.drawRect(0,
                        0,
                        scanRet.width(),
                        scanLineHeight,
                        paint);
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
}
