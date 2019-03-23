package com.toufuchew.cardocr.camera.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Region;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

public class ViewShadow extends View {
    private int screenWidth;

    private int screenHeight;

    public final static float ASPECT_RATIO = 1.685f;

    public final static float SCALE_RATIO = 0.9f;

    public ViewShadow(Context context) {
        this(context, null);
    }

    public ViewShadow(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ViewShadow(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // draw scan region border
        Rect shadowRect;
        canvas.drawRect(shadowRect = getShadowRect(), getBorderDrawPaint());

        // draw rect corners
        draw4Corners(canvas);

        // draw scan region
        canvas.clipRect(0, 0, screenWidth, screenHeight);
        canvas.clipRect(shadowRect, Region.Op.DIFFERENCE);
        canvas.drawColor(0x60000000);

        canvas.save();
        canvas.restore();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        screenWidth = getMeasuredWidth();
        screenHeight = getMeasuredHeight();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
    }

    private Rect getShadowRect() {
        int width = (int)(screenWidth * SCALE_RATIO);
        int height = (int)(width / ASPECT_RATIO);

        int x_center = screenWidth >> 1;
        int y_center = screenHeight >> 1;
        int mid_height = height >> 1;
        int mid_width = width >> 1;
        return new Rect(
                x_center - mid_width,
                y_center - mid_height,
                x_center + mid_width,
                y_center + mid_height);
    }

    private Paint getBorderDrawPaint() {
        Paint paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2);
        return paint;
    }

    private void draw4Corners(Canvas canvas) {

    }
}
