package com.catloft.launcher;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

/**
 * Small UI helpers shared across activities.
 *
 * <p>Tinting goes through the {@code set*TintList} APIs (all API 21) rather than
 * {@code getThumbDrawable().setTint()}. That avoids the one framework call in this app
 * that sits exactly on the device's API level ({@code CompoundButton.getButtonDrawable()}
 * is API 23), and it makes the tint follow the widget's pressed/disabled states instead
 * of flattening its state-list drawable.
 */
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
        ColorStateList csl = ColorStateList.valueOf(color);
        sw.setThumbTintList(csl);
        sw.setTrackTintList(csl);
    }

    static void tintCheckBox(CompoundButton cb, int color) {
        cb.setButtonTintList(ColorStateList.valueOf(color));
    }

    static void tintSeekBar(SeekBar bar, int color) {
        ColorStateList csl = ColorStateList.valueOf(color);
        bar.setProgressTintList(csl);
        bar.setThumbTintList(csl);
        if (bar.getProgressDrawable() != null) {
            bar.getProgressDrawable().mutate().setTint(color);
        }
    }

    static void accentTextView(TextView v, int color) {
        v.setTextColor(color);
    }
}
