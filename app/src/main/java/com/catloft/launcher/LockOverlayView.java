package com.catloft.launcher;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

/**
 * "Slide to unlock >" — a Catloft-level guard against buttdials on the home
 * screen. Deliberately NOT a security feature: it cannot block hardware keys,
 * the notification shade or replace the system keyguard.
 */
public class LockOverlayView extends View {

    public interface Listener {
        void onUnlocked();
    }

    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint thumbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arrowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path arrow = new Path();

    private final RectF track = new RectF();
    private float thumbX;
    private float thumbStart;
    private float thumbEnd;
    private float thumbR;
    private boolean dragging;
    private int accent = Prefs.DEFAULT_ACCENT;
    private Listener listener;

    public LockOverlayView(Context context) {
        this(context, null);
    }

    public LockOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        bgPaint.setColor(0xE6000000);
        trackPaint.setColor(0x2EFFFFFF);
        thumbPaint.setColor(accent);
        textPaint.setColor(0xB3FFFFFF);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(Ui.dp(context, 17));
        arrowPaint.setColor(Color.WHITE);
        arrowPaint.setStyle(Paint.Style.STROKE);
        arrowPaint.setStrokeWidth(Ui.dp(context, 2.5f));
        arrowPaint.setStrokeCap(Paint.Cap.ROUND);
        arrowPaint.setStrokeJoin(Paint.Join.ROUND);
    }

    public void setAccent(int color) {
        accent = color;
        thumbPaint.setColor(color);
        invalidate();
    }

    public void setListener(Listener l) {
        listener = l;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        float m = Ui.dp(getContext(), 28);
        float trackH = Ui.dp(getContext(), 64);
        float cy = h - Ui.dp(getContext(), 120);
        track.set(m, cy - trackH / 2f, w - m, cy + trackH / 2f);
        thumbR = Ui.dp(getContext(), 26);
        thumbStart = track.left + thumbR + Ui.dp(getContext(), 6);
        thumbEnd = track.right - thumbR - Ui.dp(getContext(), 6);
        thumbX = thumbStart;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawRect(0, 0, getWidth(), getHeight(), bgPaint);
        canvas.drawRoundRect(track, track.height() / 2f, track.height() / 2f, trackPaint);

        float cy = track.centerY();
        textPaint.setTextSize(track.height() * 0.38f);
        canvas.drawText("Slide to unlock >", track.centerX(), cy + textPaint.getTextSize() * 0.35f, textPaint);

        canvas.drawCircle(thumbX, cy, thumbR, thumbPaint);

        // ">" chevron inside the thumb
        float a = Ui.dp(getContext(), 5);
        arrow.reset();
        arrow.moveTo(thumbX - a * 0.5f, cy - a);
        arrow.lineTo(thumbX + a * 0.5f, cy);
        arrow.lineTo(thumbX - a * 0.5f, cy + a);
        canvas.drawPath(arrow, arrowPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isShown()) {
            return false;
        }
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                dragging = true;
                setThumbTo(event.getX());
                return true;
            case MotionEvent.ACTION_MOVE:
                if (dragging) {
                    setThumbTo(event.getX());
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (dragging) {
                    dragging = false;
                    if (thumbX >= thumbEnd - 1f) {
                        unlock();
                    } else {
                        animateTo(thumbStart);
                    }
                }
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }

    private void setThumbTo(float x) {
        thumbX = Math.max(thumbStart, Math.min(thumbEnd, x));
        invalidate();
    }

    private void animateTo(float target) {
        ValueAnimator anim = ValueAnimator.ofFloat(thumbX, target);
        anim.setDuration(160);
        anim.setInterpolator(new DecelerateInterpolator());
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                thumbX = (Float) animation.getAnimatedValue();
                invalidate();
            }
        });
        anim.start();
    }

    private void unlock() {
        animateTo(thumbEnd);
        postDelayed(new Runnable() {
            @Override
            public void run() {
                if (listener != null) {
                    listener.onUnlocked();
                }
            }
        }, 140);
    }

    /** Puts the thumb back at the start (called when the screen turns off). */
    public void relock() {
        thumbX = thumbStart;
        invalidate();
    }
}
