package com.catloft.launcher;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Records the last uncaught exception to internal storage.
 *
 * <p>This launcher is tested on hardware rather than in CI, so a crash has to be readable
 * after the fact: {@link #install(Context)} runs from {@link CatloftApp} before any activity
 * starts, and {@link #take(Context)} hands the saved trace to the home screen, so the exact
 * error can be read straight off the device without needing adb.
 */
final class CrashLog {

    private static final String FILE = "crash.txt";
    private static final String TAG = "Catloft";

    private CrashLog() {
    }

    static void install(final Context c) {
        final Thread.UncaughtExceptionHandler previous =
                Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                try {
                    write(c, e);
                } catch (Throwable ignored) {
                    // never let the recorder mask the original failure
                }
                Log.e(TAG, "Uncaught exception", e);
                if (previous != null) {
                    previous.uncaughtException(t, e);
                } else {
                    System.exit(1);
                }
            }
        });
    }

    private static void write(Context c, Throwable e) throws Exception {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        String header = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(new Date()) + "\nCatloft 1.0\n";
        FileOutputStream out = new FileOutputStream(new File(c.getFilesDir(), FILE), false);
        try {
            out.write((header + sw).getBytes("UTF-8"));
        } finally {
            out.close();
        }
    }

    /** Returns the saved trace and clears it, or null when nothing has crashed. */
    static String take(Context c) {
        try {
            File f = new File(c.getFilesDir(), FILE);
            if (!f.exists() || f.length() == 0) {
                return null;
            }
            byte[] buf = new byte[(int) f.length()];
            FileInputStream in = new FileInputStream(f);
            try {
                int off = 0;
                while (off < buf.length) {
                    int n = in.read(buf, off, buf.length - off);
                    if (n < 0) {
                        break;
                    }
                    off += n;
                }
            } finally {
                in.close();
            }
            f.delete();
            return new String(buf, "UTF-8");
        } catch (Exception e) {
            return null;
        }
    }
}
