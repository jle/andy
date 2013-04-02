package com.vandalsoftware.android.spdyexample;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Looper;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;

public class LogView extends View {
    private int mY;
    private ArrayList<String> mPendingMessages = new ArrayList<String>(20);
    private final TextPaint mPaint;
    private Bitmap mBitmap;
    private Canvas mBmCanvas;
    private int mBottom;
    private int mFontHeight;

    public LogView(Context context) {
        this(context, null);
    }

    public LogView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LogView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        int h = getMeasuredHeight() - getPaddingBottom() - getPaddingTop();
        mBitmap = Bitmap.createBitmap(getMeasuredWidth(), getMeasuredHeight(),
                Bitmap.Config.ARGB_8888);
        mBmCanvas = new Canvas(mBitmap);
        mBottom = h;

        final Paint.FontMetricsInt metrics = mPaint.getFontMetricsInt();
        mFontHeight = metrics.descent - metrics.ascent;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Canvas bc = mBmCanvas;
        boolean clear = mY + (mPendingMessages.size() * mFontHeight) >= mBottom || mY == 0;
        if (clear) {
            mY = mFontHeight;
            Paint.Style oldStyle = mPaint.getStyle();
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setColor(Color.BLACK);
            bc.drawRect(0, 0, getMeasuredWidth(), getMeasuredHeight(), mPaint);
            mPaint.setStyle(oldStyle);
        }
        if (!mPendingMessages.isEmpty()) {
            mPaint.setColor(Color.WHITE);
            for (String message : mPendingMessages) {
                bc.drawText(message, getPaddingLeft(), getPaddingTop() + mY, mPaint);
                mY += mFontHeight;
            }
            mPendingMessages.clear();
        }
        canvas.drawBitmap(mBitmap, 0, 0, mPaint);
    }

    public void log(String message) {
        if (Thread.currentThread() != Looper.getMainLooper().getThread()) {
            throw new IllegalThreadStateException("Calling from wrong thread.");
        }
        mPendingMessages.add(message);
        invalidate();
    }
}
