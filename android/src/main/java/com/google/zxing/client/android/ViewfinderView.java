package com.google.zxing.client.android;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.v4.content.res.ResourcesCompat;
import android.util.AttributeSet;
import android.view.View;

public final class ViewfinderView extends View {
    //空白高度比例0.0-1.0
    private double heightScale = 0.5;
    //扫描框宽度DP
    private int scanWidthDp;
    //扫描框高度DP
    private int scanHeightDp;
    //扫描框宽度PX
    private int scanWidthPx;
    //扫描框高度PX
    private int scanHeightPx;
    //扫描框颜色
    private int scanColor;
    private Paint paint;
    private final int maskColor;

    public int getScanWidthDp() {
        return scanWidthDp;
    }

    public void setScanWidthDp(int scanWidthDp) {
        this.scanWidthDp = scanWidthDp;
    }

    public int getScanHeightDp() {
        return scanHeightDp;
    }

    public void setScanHeightDp(int scanHeightDp) {
        this.scanHeightDp = scanHeightDp;
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

    public double getHeightScale() {
        return heightScale;
    }

    public void setHeightScale(double heightScale) {
        this.heightScale = heightScale;
    }

    public int getScanColor() {
        return scanColor;
    }

    public void setScanColor(int scanColor) {
        this.scanColor = scanColor;
    }

    public ViewfinderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        maskColor = ResourcesCompat.getColor(getResources(), R.color.viewfinder_mask, null);
        scanColor = Color.BLUE;
        reset();
    }

    public void reset() {
        float density = getResources().getDisplayMetrics().density;
        if (scanWidthDp != 0) {
            scanWidthPx = (int) (scanWidthDp * density);
        }
        if (scanWidthDp != 0) {
            scanHeightPx = (int) (scanWidthDp * density);
        }
        invalidate();
    }

    @Override
    public void onDraw(Canvas canvas) {
        int width = canvas.getWidth();
        int height = canvas.getHeight();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(maskColor);
        int spaceWidth = (width - scanWidthPx) / 2;
        int spaceHeight = (int) ((height - scanHeightPx) * heightScale);

        canvas.drawRect(0, 0, width, spaceHeight, paint);
        canvas.drawRect(0,
                spaceHeight,
                spaceWidth,
                spaceHeight + scanHeightPx + 1,
                paint);
        canvas.drawRect(spaceWidth + scanWidthPx + 1,
                spaceHeight,
                width,
                spaceHeight + scanHeightPx + 1,
                paint);
        canvas.drawRect(0,
                spaceHeight + scanHeightPx + 1,
                width,
                height,
                paint);


        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(scanColor);
        canvas.drawRect((width - scanWidthPx) / 2,
                spaceHeight,
                (width - scanWidthPx) / 2 + scanWidthPx,
                spaceHeight + scanHeightPx,
                paint);

        paint.setColor(Color.WHITE);
        paint.setTextAlign(Paint.Align.CENTER);
        int textSize = (int) (getResources().getDisplayMetrics().scaledDensity * 16);
        paint.setTextSize(textSize);
        canvas.drawText("请将二维码置于取景框内扫描", width / 2, spaceHeight + scanHeightPx + textSize + 25, paint);
//        paint.setColor(Color.BLUE);
//        canvas.drawLine((width - scanWidthPx) / 2 + 1,
//                (height - scanHeightPx) / 2 + 1+line,
//                (width - scanWidthPx) / 2 + scanWidthPx,
//                (height - scanHeightPx) / 2 + 1+line, paint);
//        line+=10;
//        if (line > scanHeightPx) {
//            line = 0;
//        }
//        postInvalidateDelayed(ANIMATION_DELAY,
//                (width - scanWidthPx) / 2 + 1,
//                (height - scanHeightPx) / 2,
//                (width - scanWidthPx) / 2 + scanWidthPx + 1,
//                ((height - scanHeightPx) / 2) +
//                        scanHeightPx);
    }
}
