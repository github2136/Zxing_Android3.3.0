/*
 * Copyright (C) 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.zxing.client.android;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

/**
 * This view is overlaid on top of the camera preview. It adds the viewfinder rectangle and partial
 * transparency outside it, as well as the laser scanner animation and result points.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class ViewfinderView extends View {
    private int scanningWidth = 600;
    private int scanningHeight = 600;
    //    int line = 0;
//    private static final long ANIMATION_DELAY = 80L;
    private Paint paint;
    private final int maskColor;

    public int getScanningWidth() {
        return scanningWidth;
    }

    public void setScanningWidth(int scanningWidth) {
        this.scanningWidth = scanningWidth;
    }

    public int getScanningHeight() {
        return scanningHeight;
    }

    public void setScanningHeight(int scanningHeight) {
        this.scanningHeight = scanningHeight;
    }

    public ViewfinderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Resources resources = getResources();
        maskColor = resources.getColor(R.color.viewfinder_mask);
    }


    @Override
    public void onDraw(Canvas canvas) {
        int width = canvas.getWidth();
        int height = canvas.getHeight();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(maskColor);
        int spaceWidth = (width - scanningWidth) / 2;
        int spaceHeight = (height - scanningHeight) / 2;

        canvas.drawRect(0, 0, width, spaceHeight, paint);
        canvas.drawRect(0,
                spaceHeight,
                spaceWidth,
                spaceHeight + scanningHeight + 1,
                paint);
        canvas.drawRect(spaceWidth + scanningWidth + 1,
                spaceHeight,
                width,
                spaceHeight + scanningHeight + 1,
                paint);
        canvas.drawRect(0,
                spaceHeight + scanningHeight + 1,
                width,
                height,
                paint);


        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.BLUE);
        canvas.drawRect((width - scanningWidth) / 2,
                (height - scanningHeight) / 2,
                (width - scanningWidth) / 2 + scanningWidth,
                (height - scanningHeight) / 2 + scanningHeight,
                paint);

        paint.setColor(Color.WHITE);
        paint.setTextAlign(Paint.Align.CENTER);
        int textSize = (int) (getResources().getDisplayMetrics().scaledDensity * 16);
        paint.setTextSize(textSize);
        canvas.drawText("请将二维码置于取景框内扫描", width / 2, spaceHeight + scanningHeight + textSize + 25, paint);
//        paint.setColor(Color.BLUE);
//        canvas.drawLine((width - scanningWidth) / 2 + 1,
//                (height - scanningHeight) / 2 + 1+line,
//                (width - scanningWidth) / 2 + scanningWidth,
//                (height - scanningHeight) / 2 + 1+line, paint);
//        line+=10;
//        if (line > scanningHeight) {
//            line = 0;
//        }
//        postInvalidateDelayed(ANIMATION_DELAY,
//                (width - scanningWidth) / 2 + 1,
//                (height - scanningHeight) / 2,
//                (width - scanningWidth) / 2 + scanningWidth + 1,
//                ((height - scanningHeight) / 2) +
//                        scanningHeight);
    }
}
