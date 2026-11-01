package com.catloft.launcher;

import android.app.Application;

/**
 * Installs the crash recorder before any activity is created, so a failure anywhere in the
 * launcher leaves a readable trace behind.
 */
public class CatloftApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        CrashLog.install(this);
    }
}
