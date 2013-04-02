package com.vandalsoftware.android.spdyexample;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;

public class LogView extends View {
    private int mY;
    private String mMessage;
    private final TextPaint mPaint;
    private Bitmap mBitmap;
    private Canvas mBmCanvas;
    private boolean mClear;
    private int mBottom;

    public LogView(Context context) {
        this(context, null);
    }

    public LogView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LogView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        mClear = true;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        int h = getMeasuredHeight() - getPaddingBottom() - getPaddingTop();
        mBitmap = Bitmap.createBitmap(getMeasuredWidth(), getMeasuredHeight(),
                Bitmap.Config.ARGB_8888);
        mBmCanvas = new Canvas(mBitmap);
        mBottom = h;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Canvas bc = mBmCanvas;
        if (mClear) {
            Paint.Style oldStyle = mPaint.getStyle();
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setColor(Color.WHITE);
            bc.drawRect(0, 0, getMeasuredWidth(), getMeasuredHeight(), mPaint);
            mPaint.setStyle(oldStyle);
            mClear = false;
        }
        if (mMessage != null) {
            mPaint.setColor(Color.BLACK);
            bc.drawText(mMessage, getPaddingLeft(), getPaddingTop() + mY, mPaint);
            mMessage = null;
        }
        canvas.drawBitmap(mBitmap, 0, 0, mPaint);
    }

    public void log(String message) {
        final Paint.FontMetricsInt metrics = mPaint.getFontMetricsInt();
        int fh = metrics.descent - metrics.ascent;
        if (mY >= mBottom) {
            mY = fh;
            mClear = true;
        } else {
            mY += fh;
            mClear = false;
        }
        mMessage = message;
        invalidate();
    }
}
