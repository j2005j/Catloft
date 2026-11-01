package com.catloft.launcher;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

/** Small UI helpers shared across activities. */
final class Ui {

    private Ui() {
    }

    static int dp(Context c, float v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    static void tintImageView(ImageView v, int color) {
        v.setColorFilter(color, PorterDuff.Mode.SRC_IN);
    }

    /** Tint a TextView's compound drawable (used for framework menu icons). */
    static void tintCompound(TextView v, int color) {
        Drawable[] ds = v.getCompoundDrawables();
        for (Drawable d : ds) {
            if (d != null) {
                d.mutate();
                d.setColorFilter(color, PorterDuff.Mode.SRC_IN);
            }
        }
    }

    /** Tint every drawable inside a menu row (icon on the left). */
    static void tintIcon(ImageView v, int color) {
        Drawable d = v.getDrawable();
        if (d != null) {
            d.mutate();
            d.setColorFilter(color, PorterDuff.Mode.SRC_IN);
        }
    }

    static void tintSwitch(android.widget.Switch sw, int color) {
        if (sw.getThumbDrawable() != null) {
            sw.getThumbDrawable().mutate().setTint(color);
        }
        if (sw.getTrackDrawable() != null) {
            sw.getTrackDrawable().mutate().setTint(color);
        }
    }

    static void tintCheckBox(CompoundButton cb, int color) {
        if (cb.getButtonDrawable() != null) {
            cb.getButtonDrawable().mutate().setTint(color);
        }
    }

    static void tintSeekBar(SeekBar bar, int color) {
        bar.getProgressDrawable().mutate().setTint(color);
        if (bar.getThumb() != null) {
            bar.getThumb().mutate().setTint(color);
        }
    }

    static void accentTextView(TextView v, int color) {
        v.setTextColor(color);
    }
}
