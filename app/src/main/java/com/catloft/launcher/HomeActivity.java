package com.catloft.launcher;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.DateFormat;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** The launcher home: big date/time/battery, three tiles, hidden status bar. */
public class HomeActivity extends Activity {

    private static final int REQ_PICK_WALLPAPER = 1;

    private ImageView wallpaperView;
    private View dimOverlay;
    private TextView dateText;
    private TextView timeText;
    private TextView batteryText;
    private TextView btnMenu;
    private LinearLayout tilesRow;
    private LinearLayout clockBlock;
    private LockOverlayView lockOverlay;

    /** Smallest the big clock may shrink to before the tiles row would crowd it out. */
    private static final float MIN_CLOCK_SP = 32f;
    private float timeBaseSp;

    private final Handler handler = new Handler();
    private Intent batteryIntent;
    private boolean unlockedThisWake;
    private String[] tileSpecs = new String[3];
    private GestureDetector gesture;
    private PopupWindow currentPopup;

    private final BroadcastReceiver screenReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) {
                return;
            }
            if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                unlockedThisWake = false;
                lockOverlay.setVisibility(View.GONE);
                lockOverlay.relock();
            } else if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
                unlockedThisWake = false;
                if (Prefs.slideUnlock(HomeActivity.this)) {
                    showLock();
                }
            }
        }
    };

    private final BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            batteryIntent = intent;
            updateBattery();
        }
    };

    private final Runnable clockTick = new Runnable() {
        @Override
        public void run() {
            updateClock();
            long now = System.currentTimeMillis();
            long next = 60000L - (now % 60000L) + 60L;
            handler.postDelayed(this, next);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideSystemBars();
        setContentView(R.layout.activity_home);

        wallpaperView = (ImageView) findViewById(R.id.wallpaper);
        dimOverlay = findViewById(R.id.dimOverlay);
        dateText = (TextView) findViewById(R.id.dateText);
        timeText = (TextView) findViewById(R.id.timeText);
        batteryText = (TextView) findViewById(R.id.batteryText);
        btnMenu = (TextView) findViewById(R.id.btnMenu);
        tilesRow = (LinearLayout) findViewById(R.id.tilesRow);
        clockBlock = (LinearLayout) findViewById(R.id.clockBlock);
        lockOverlay = (LockOverlayView) findViewById(R.id.lockOverlay);

        // Remember the intended clock size, then shrink it whenever the middle band is too
        // short to hold date + time + battery (landscape, or a raised system font scale).
        timeBaseSp = timeText.getTextSize() / getResources().getDisplayMetrics().scaledDensity;
        clockBlock.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int l, int t, int r, int b,
                                       int ol, int ot, int or, int ob) {
                fitClock();
            }
        });

        unlockedThisWake = !Prefs.slideUnlock(this);
        lockOverlay.setAccent(Prefs.accent(this));
        lockOverlay.setListener(new LockOverlayView.Listener() {
            @Override
            public void onUnlocked() {
                unlockedThisWake = true;
                lockOverlay.setVisibility(View.GONE);
            }
        });

        btnMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showOverflow();
            }
        });

        gesture = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float vx, float vy) {
                if (e1 != null && !isLocked() && Prefs.swipeUpDrawer(HomeActivity.this)
                        && vy < -900 && e2.getY() < e1.getY() - 80) {
                    openDrawer();
                    return true;
                }
                return false;
            }
        });
        findViewById(R.id.homeRoot).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                gesture.onTouchEvent(event);
                return true;
            }
        });

        registerReceiver(batteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        IntentFilter screenFilter = new IntentFilter();
        screenFilter.addAction(Intent.ACTION_SCREEN_ON);
        screenFilter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(screenReceiver, screenFilter);

        handler.post(clockTick);
        setupTiles();
        reloadWallpaper();
        applyOrientation();
        offerCrashLog();

        if (Prefs.slideUnlock(this)) {
            showLock();
        }
    }

    // --- lifecycle ---

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemBars();
        updateClock();
        refreshAccent();
        setupTiles();
        reloadWallpaper();
        applyOrientation();
        if (Prefs.slideUnlock(this) && !unlockedThisWake) {
            showLock();
        } else if (!Prefs.slideUnlock(this)) {
            lockOverlay.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        try {
            unregisterReceiver(batteryReceiver);
            unregisterReceiver(screenReceiver);
        } catch (Exception ignored) {
            // already gone
        }
        super.onDestroy();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemBars();
        }
    }

    @Override
    public void onBackPressed() {
        // The home screen is the root of the launcher: Back must never close it, whether or
        // not the slide lock is showing. Sub-screens (settings, drawer) finish themselves.
    }

    private boolean isLocked() {
        return lockOverlay.getVisibility() == View.VISIBLE;
    }

    private void showLock() {
        lockOverlay.relock();
        lockOverlay.setVisibility(View.VISIBLE);
        lockOverlay.bringToFront();
    }

    /** Hidden status bar, but deliberately NOT immersive — swipe-down still opens the shade. */
    private void hideSystemBars() {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(
                android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                        | android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }

    /**
     * Portrait unless the user opts into sensor rotation. This runs from both onCreate and
     * onResume, so turning the setting on in the settings screen takes effect on the way back.
     */
    private void applyOrientation() {
        setRequestedOrientation(Prefs.autoRotate(this)
                ? ActivityInfo.SCREEN_ORIENTATION_SENSOR
                : ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    /**
     * Shrink the big clock when the middle band is too short to hold date + time + battery.
     * Landscape is the usual trigger; a raised system font scale or a large icon scale can do
     * it too, because the tiles row below is sized from the icon scale.
     */
    private void fitClock() {
        if (clockBlock == null || timeBaseSp <= 0f) {
            return;
        }
        int avail = clockBlock.getHeight()
                - clockBlock.getPaddingTop() - clockBlock.getPaddingBottom();
        if (avail <= 0) {
            return;
        }
        float scaledDensity = getResources().getDisplayMetrics().scaledDensity;
        int other = dateText.getMeasuredHeight() + batteryText.getMeasuredHeight()
                + Ui.dp(this, 12);
        int maxTimePx = avail - other;
        if (maxTimePx <= 0) {
            return;
        }
        // The sans-serif line box runs a little taller than the nominal point size.
        float maxSp = maxTimePx / (1.25f * scaledDensity);
        float sp = Math.max(MIN_CLOCK_SP, Math.min(timeBaseSp, maxSp));
        float current = timeText.getTextSize() / scaledDensity;
        if (Math.abs(current - sp) >= 1f) {
            timeText.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        }
    }

    /**
     * If a previous run died, show its trace once — the device has no emulator tethered to it,
     * so the stack trace has to be readable straight off the screen.
     */
    private void offerCrashLog() {
        final String trace = CrashLog.take(this);
        if (trace == null || trace.trim().isEmpty()) {
            return;
        }
        TextView body = new TextView(this);
        body.setTypeface(Typeface.MONOSPACE);
        body.setTextSize(11);
        body.setTextColor(0xFFEEEEEE);
        int pad = Ui.dp(this, 16);
        body.setPadding(pad, pad, pad, pad);
        body.setText(trace.length() > 6000 ? trace.substring(0, 6000) : trace);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(body);

        new AlertDialog.Builder(this)
                .setTitle("Catloft crashed")
                .setView(scroll)
                .setPositiveButton("Close", null)
                .setNeutralButton("Copy", new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface dialog, int which) {
                        ClipboardManager cm = (ClipboardManager)
                                getSystemService(Context.CLIPBOARD_SERVICE);
                        if (cm != null) {
                            cm.setPrimaryClip(ClipData.newPlainText("Catloft crash", trace));
                            toast("Crash trace copied");
                        }
                    }
                })
                .show();
    }

    // --- clock / battery ---

    private void updateClock() {
        Date now = new Date();
        boolean h24 = DateFormat.is24HourFormat(this);
        String timePattern = h24 ? "HH:mm" : "h:mm a";
        String datePattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), "EEE, d MMM");
        timeText.setText(new SimpleDateFormat(timePattern, Locale.getDefault()).format(now));
        dateText.setText(new SimpleDateFormat(datePattern, Locale.getDefault()).format(now));
    }

    private void updateBattery() {
        if (batteryIntent == null) {
            return;
        }
        int level = batteryIntent.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryIntent.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, 100);
        if (level < 0 || scale <= 0) {
            return;
        }
        int pct = Math.round(level * 100f / scale);
        batteryText.setText(pct + "%");
        batteryText.setTextColor(pct <= 15 ? 0xFFF44336 : 0xFFFFFFFF);
    }

    // --- wallpaper ---

    private void reloadWallpaper() {
        int dim = Prefs.dim(this);
        dimOverlay.setVisibility(dim > 0 ? View.VISIBLE : View.GONE);
        dimOverlay.setAlpha(dim / 100f);

        if (!WallpaperHelper.hasWallpaper(this)) {
            wallpaperView.setImageDrawable(null);
            wallpaperView.setVisibility(View.GONE);
            return;
        }
        WallpaperHelper.io.execute(new Runnable() {
            @Override
            public void run() {
                final Bitmap bmp = WallpaperHelper.forHome(HomeActivity.this);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (bmp != null) {
                            wallpaperView.setImageBitmap(bmp);
                            wallpaperView.setVisibility(View.VISIBLE);
                        } else {
                            wallpaperView.setVisibility(View.GONE);
                        }
                    }
                });
            }
        });
    }

    private void wallpaperDialog() {
        final String[] items = {
                "Pick from gallery",
                "Use current system wallpaper",
                "Remove wallpaper (black)"
        };
        new AlertDialog.Builder(this)
                .setTitle("Change wallpaper")
                .setItems(items, new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface dialog, int which) {
                        if (which == 0) {
                            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                            i.setType("image/*");
                            try {
                                startActivityForResult(i, REQ_PICK_WALLPAPER);
                            } catch (Exception e) {
                                toast("No app can pick images");
                            }
                        } else if (which == 1) {
                            useSystemWallpaper();
                        } else {
                            WallpaperHelper.io.execute(new Runnable() {
                                @Override
                                public void run() {
                                    WallpaperHelper.clearWallpaper(HomeActivity.this);
                                    WallpaperHelper.refreshLockWallpaper(HomeActivity.this);
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            reloadWallpaper();
                                        }
                                    });
                                }
                            });
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Reading the system wallpaper needs READ_WALLPAPER_INTERNAL, a signature permission this
     * app cannot hold. We try anyway — some builds don't enforce it — and the catch below
     * reports failure to the user rather than crashing.
     */
    @SuppressLint("MissingPermission")
    private void useSystemWallpaper() {
        WallpaperHelper.io.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    android.graphics.drawable.Drawable d =
                            android.app.WallpaperManager.getInstance(HomeActivity.this).getDrawable();
                    Bitmap bmp = null;
                    if (d instanceof android.graphics.drawable.BitmapDrawable) {
                        bmp = ((android.graphics.drawable.BitmapDrawable) d).getBitmap();
                    } else if (d != null && d.getIntrinsicWidth() > 0) {
                        bmp = Bitmap.createBitmap(d.getIntrinsicWidth(),
                                d.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
                        android.graphics.Canvas canvas = new android.graphics.Canvas(bmp);
                        d.setBounds(0, 0, bmp.getWidth(), bmp.getHeight());
                        d.draw(canvas);
                    }
                    if (bmp != null) {
                        WallpaperHelper.saveOriginal(HomeActivity.this, bmp);
                        afterWallpaperChanged();
                    } else {
                        WallpaperHelper.toast(HomeActivity.this, "Could not read system wallpaper");
                    }
                } catch (Exception e) {
                    WallpaperHelper.toast(HomeActivity.this, "Could not read system wallpaper");
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQ_PICK_WALLPAPER || resultCode != RESULT_OK || data == null
                || data.getData() == null) {
            return;
        }
        final Uri uri = data.getData();
        WallpaperHelper.io.execute(new Runnable() {
            @Override
            public void run() {
                Bitmap bmp = WallpaperHelper.decodeUri(HomeActivity.this, uri);
                if (bmp == null) {
                    WallpaperHelper.toast(HomeActivity.this, "Could not load that image");
                    return;
                }
                WallpaperHelper.saveOriginal(HomeActivity.this, bmp);
                afterWallpaperChanged();
            }
        });
    }

    /** Runs on the IO thread after the original was saved. */
    private void afterWallpaperChanged() {
        WallpaperHelper.refreshBlurred(this, new Runnable() {
            @Override
            public void run() {
                WallpaperHelper.refreshLockWallpaper(HomeActivity.this);
                reloadWallpaper();
            }
        });
    }

    // --- tiles ---

    private void setupTiles() {
        final int accent = Prefs.accent(this);
        float scale = Prefs.iconScale(this);
        tilesRow.removeAllViews();
        android.view.LayoutInflater inflater = getLayoutInflater();

        for (int i = 0; i < 3; i++) {
            final int index = i;
            tileSpecs[i] = Prefs.tile(this, i);
            final String spec = tileSpecs[i];

            View tile = inflater.inflate(R.layout.tile, tilesRow, false);
            ImageView icon = (ImageView) tile.findViewById(R.id.tileIcon);
            TextView label = (TextView) tile.findViewById(R.id.tileLabel);

            icon.setImageDrawable(AppResolver.icon(this, spec));
            label.setText(AppResolver.label(this, spec));
            int iconPx = Math.round(Ui.dp(this, 52) * scale);
            ViewGroup.LayoutParams lp = icon.getLayoutParams();
            lp.width = iconPx;
            lp.height = iconPx;
            icon.setLayoutParams(lp);
            label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13 * scale);
            tile.setBackground(tileBackground(accent));

            tile.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!AppResolver.launch(HomeActivity.this, spec)) {
                        toast(AppResolver.label(HomeActivity.this, spec) + " is not installed");
                    }
                }
            });
            tile.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    pickTileApp(index);
                    return true;
                }
            });
            tilesRow.addView(tile);
        }
    }

    private StateListDrawable tileBackground(int accent) {
        StateListDrawable sld = new StateListDrawable();
        GradientDrawable pressed = new GradientDrawable();
        pressed.setColor((accent & 0x00FFFFFF) | 0x33000000);
        pressed.setCornerRadius(Ui.dp(this, 8));
        GradientDrawable normal = new GradientDrawable();
        normal.setColor(Color.TRANSPARENT);
        normal.setCornerRadius(Ui.dp(this, 8));
        sld.addState(new int[]{android.R.attr.state_pressed}, pressed);
        sld.addState(new int[]{}, normal);
        return sld;
    }

    private void pickTileApp(final int index) {
        final java.util.List<AppResolver.AppEntry> apps = AppResolver.launchableApps(this);
        String[] labels = new String[apps.size()];
        for (int i = 0; i < apps.size(); i++) {
            labels[i] = apps.get(i).label + (apps.get(i).system ? "  (system)" : "");
        }
        new AlertDialog.Builder(this)
                .setTitle("Tile " + (index + 1) + " — pick an app")
                .setItems(labels, new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface dialog, int which) {
                        Prefs.setTile(HomeActivity.this, index,
                                "component:" + apps.get(which).component.flattenToString());
                        setupTiles();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // --- overflow menu ---

    private void showOverflow() {
        int accent = Prefs.accent(this);
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xF21A1A1A);
        bg.setCornerRadius(Ui.dp(this, 4));
        panel.setBackground(bg);
        int pad = Ui.dp(this, 4);
        panel.setPadding(pad, pad, pad, pad);

        addMenuRow(panel, android.R.drawable.ic_menu_agenda, "App drawer", accent,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        openDrawer();
                    }
                });
        addMenuRow(panel, android.R.drawable.ic_menu_gallery, "Change wallpaper", accent,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        wallpaperDialog();
                    }
                });
        addMenuRow(panel, android.R.drawable.ic_menu_manage, "Launcher settings", accent,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startActivity(new Intent(HomeActivity.this, SettingsActivity.class));
                    }
                });

        final PopupWindow popup = new PopupWindow(panel,
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        popup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popup.setOutsideTouchable(true);
        popup.setElevation(Ui.dp(this, 8));
        panel.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        int w = Math.max(Ui.dp(this, 240), panel.getMeasuredWidth());
        popup.setWidth(w);
        currentPopup = popup;
        popup.showAsDropDown(btnMenu, -(w - btnMenu.getWidth()), Ui.dp(this, 4));
    }

    private void addMenuRow(LinearLayout panel, int iconRes, String text, final int accent,
                            final View.OnClickListener action) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setPadding(Ui.dp(this, 14), Ui.dp(this, 12), Ui.dp(this, 14), Ui.dp(this, 12));

        ImageView icon = new ImageView(this);
        icon.setImageResource(iconRes);
        Ui.tintIcon(icon, 0xFFFFFFFF);
        int sz = Ui.dp(this, 20);
        row.addView(icon, new LinearLayout.LayoutParams(sz, sz));

        TextView label = new TextView(this);
        label.setText(text);
        label.setTextColor(0xFFFFFFFF);
        label.setTextSize(15);
        LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        llp.leftMargin = Ui.dp(this, 14);
        row.addView(label, llp);

        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                action.onClick(v);
                if (currentPopup != null) {
                    currentPopup.dismiss();
                    currentPopup = null;
                }
            }
        });
        panel.addView(row, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private void openDrawer() {
        startActivity(new Intent(this, DrawerActivity.class));
    }

    private void refreshAccent() {
        int accent = Prefs.accent(this);
        timeText.setTextColor(accent);
        lockOverlay.setAccent(accent);
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
