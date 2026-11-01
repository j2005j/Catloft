package com.catloft.launcher;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Wallpaper storage, static blur rendering and lock-screen application.
 *
 * Catloft owns its own wallpaper file (default: none = black). The blurred copy is
 * rendered once, saved to disk and used as a static image — never a live blur —
 * to keep the launcher cheap on battery.
 */
final class WallpaperHelper {

    private static final String ORIGINAL = "wallpaper.jpg";
    private static final String BLURRED = "wallpaper_blurred.jpg";
    private static final String LOCK_BACKUP = "system_wallpaper_backup.jpg";
    private static final int MAX_DIM = 1600;

    static final ExecutorService io = Executors.newSingleThreadExecutor();
    private static final Handler main = new Handler(Looper.getMainLooper());

    private WallpaperHelper() {
    }

    // --- files ---

    private static File file(Context c, String name) {
        return new File(c.getFilesDir(), name);
    }

    static boolean hasWallpaper(Context c) {
        return file(c, ORIGINAL).exists();
    }

    static void clearWallpaper(Context c) {
        file(c, ORIGINAL).delete();
        file(c, BLURRED).delete();
    }

    // --- load / save ---

    /** Decodes a file downsampled to roughly screen size (two-pass, avoids OOM). */
    private static Bitmap decodeScaled(File f, int maxDim) {
        if (!f.exists()) {
            return null;
        }
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        decode(f, bounds);
        int sample = 1;
        int w = bounds.outWidth;
        int h = bounds.outHeight;
        while (w / 2 >= maxDim || h / 2 >= maxDim) {
            sample *= 2;
            w /= 2;
            h /= 2;
        }
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = sample;
        opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
        return decode(f, opts);
    }

    private static Bitmap decode(File f, BitmapFactory.Options opts) {
        try (FileInputStream in = new FileInputStream(f)) {
            return BitmapFactory.decodeStream(in, null, opts);
        } catch (Exception e) {
            return null;
        }
    }

    static Bitmap loadOriginal(Context c) {
        return decodeScaled(file(c, ORIGINAL), MAX_DIM);
    }

    static Bitmap loadBlurred(Context c) {
        return decodeScaled(file(c, BLURRED), MAX_DIM);
    }

    private static void save(Context c, String name, Bitmap bmp, int quality) {
        try (FileOutputStream out = new FileOutputStream(file(c, name))) {
            bmp.compress(Bitmap.CompressFormat.JPEG, quality, out);
        } catch (Exception ignored) {
            // disk full or gone; the feature degrades to "no wallpaper"
        }
    }

    static void saveOriginal(Context c, Bitmap bmp) {
        save(c, ORIGINAL, bmp, 90);
    }

    // --- blur ---

    /**
     * Renders a static blurred copy of the original at the given strength (0-100).
     * Downscale -> blur -> upscale, so the 25px RenderScript limit still yields a
     * heavy blur. Returns null when RenderScript is unavailable on this device.
     */
    static Bitmap renderBlur(Context c, Bitmap src, int percent) {
        if (src == null || percent <= 0) {
            return null;
        }
        int w = Math.max(1, src.getWidth() / 4);
        int h = Math.max(1, src.getHeight() / 4);
        Bitmap small = Bitmap.createScaledBitmap(src, w, h, true);
        Bitmap out = small.copy(Bitmap.Config.ARGB_8888, true);
        RenderScript rs = null;
        try {
            rs = RenderScript.create(c);
            ScriptIntrinsicBlur blur = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
            float radius = Math.max(1f, Math.min(25f, percent * 25f / 100f));
            blur.setRadius(radius);
            Allocation in = Allocation.createFromBitmap(rs, small);
            Allocation o = Allocation.createFromBitmap(rs, out);
            blur.setInput(in);
            blur.forEach(o);
            o.copyTo(out);
            return Bitmap.createScaledBitmap(out, src.getWidth(), src.getHeight(), true);
        } catch (Throwable t) {
            return null;
        } finally {
            if (rs != null) {
                try {
                    rs.destroy();
                } catch (Throwable ignored) {
                    // already gone
                }
            }
        }
    }

    /** Re-renders and stores the blurred copy from the original (on the IO thread). */
    static void refreshBlurred(final Context c, final Runnable after) {
        io.execute(new Runnable() {
            @Override
            public void run() {
                Bitmap orig = loadOriginal(c);
                int percent = Prefs.blur(c);
                if (orig != null && percent > 0) {
                    Bitmap blurred = renderBlur(c, orig, percent);
                    if (blurred != null) {
                        save(c, BLURRED, blurred, 88);
                    }
                }
                if (after != null) {
                    main.post(after);
                }
            }
        });
    }

    // --- home / lock application ---

    /** The bitmap the home screen should draw, or null for plain black. */
    static Bitmap forHome(Context c) {
        if (!hasWallpaper(c)) {
            return null;
        }
        if (Prefs.blur(c) > 0 && Prefs.blurHome(c)) {
            Bitmap b = loadBlurred(c);
            if (b != null) {
                return b;
            }
        }
        return loadOriginal(c);
    }

    /**
     * Keeps the system wallpaper in sync with the lock toggle: applies the blurred
     * (or original) image when "apply to lock" is on, restores the previous system
     * wallpaper when it is off. Android 6 has no separate lock wallpaper — the
     * system wallpaper is what the lock screen shows.
     */
    static void refreshLockWallpaper(final Context c) {
        io.execute(new Runnable() {
            @Override
            public void run() {
                boolean want = Prefs.blurLock(c) && hasWallpaper(c);
                boolean owns = Prefs.lockApplied(c);
                if (want) {
                    Bitmap bmp = Prefs.blur(c) > 0 ? loadBlurred(c) : loadOriginal(c);
                    if (bmp == null) {
                        return;
                    }
                    if (!owns) {
                        // remember what was there so "off" can put it back
                        backupSystemWallpaper(c);
                    }
                    try {
                        android.app.WallpaperManager.getInstance(c).setBitmap(bmp);
                        Prefs.setLockApplied(c, true);
                    } catch (Exception ignored) {
                        // wallpaper service refused; leave prefs as they were
                    }
                } else if (owns) {
                    restoreSystemWallpaper(c);
                }
            }
        });
    }

    private static void backupSystemWallpaper(Context c) {
        try {
            Drawable d = android.app.WallpaperManager.getInstance(c).getDrawable();
            if (d instanceof BitmapDrawable) {
                save(c, LOCK_BACKUP, ((BitmapDrawable) d).getBitmap(), 90);
            } else if (d != null) {
                int w = d.getIntrinsicWidth();
                int h = d.getIntrinsicHeight();
                if (w > 0 && h > 0) {
                    Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                    Canvas canvas = new Canvas(bmp);
                    d.setBounds(0, 0, w, h);
                    d.draw(canvas);
                    save(c, LOCK_BACKUP, bmp, 90);
                }
            }
        } catch (Exception ignored) {
            // nothing to back up
        }
    }

    private static void restoreSystemWallpaper(Context c) {
        Bitmap backup = decodeScaled(file(c, LOCK_BACKUP), MAX_DIM);
        try {
            if (backup != null) {
                android.app.WallpaperManager.getInstance(c).setBitmap(backup);
            }
        } catch (Exception ignored) {
            // best effort
        }
        Prefs.setLockApplied(c, false);
    }

    // --- gallery helpers ---

    /** Decodes a picked image down to a sane wallpaper size. */
    static Bitmap decodeUri(Context c, android.net.Uri uri) {
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            try (java.io.InputStream in = c.getContentResolver().openInputStream(uri)) {
                BitmapFactory.decodeStream(in, null, bounds);
            }
            int sample = 1;
            while (bounds.outWidth / (sample * 2) >= MAX_DIM
                    || bounds.outHeight / (sample * 2) >= MAX_DIM) {
                sample *= 2;
            }
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = sample;
            opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
            try (java.io.InputStream in = c.getContentResolver().openInputStream(uri)) {
                return BitmapFactory.decodeStream(in, null, opts);
            }
        } catch (Exception e) {
            return null;
        }
    }

    static void toast(final Context c, final String msg) {
        main.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(c, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
