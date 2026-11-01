package com.catloft.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Catloft's launcher settings — every knob the spec calls for. */
public class SettingsActivity extends Activity {

    private static final long[] INTERVALS = {
            15L * 60000L, 30L * 60000L, 60L * 60000L, 120L * 60000L,
            240L * 60000L, 360L * 60000L, 720L * 60000L, 1440L * 60000L
    };
    private static final String[] INTERVAL_LABELS = {
            "15 minutes", "30 minutes", "1 hour", "2 hours",
            "4 hours", "6 hours", "12 hours", "24 hours"
    };

    private TextView sumIconScale, sumDim, sumBlur, sumKiller, sumSchedule,
            sumTarget, sumApps, sumSysApps, sumHider, sumTiles, sumColor, sumSlide,
            sumRotate, sumSwipeUp, sumDoubleTap, sumHomeKey, sumBackKey, sumSwipeDown,
            sumOverlay;
    private CheckBox checkBlurHome, checkBlurLock, checkKillerSystem;
    private Switch switchKiller, switchSlide, switchRotate, switchSwipeUp, switchDoubleTap,
            switchSwipeDown;
    private View rowKiller, rowKillerSchedule, rowKillerTarget, rowKillerApps,
            rowKillerSysApps, rowHider, rowRotate, rowSwipeUp, rowDoubleTap,
            rowHomeKey, rowBackKey, rowSwipeDown, rowOverlay;

    private interface StringSetHandler {
        void onSet(Set<String> selected);
    }

    private interface KeyPickHandler {
        void onPicked(String spec); // "" = default / do nothing
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        findViewById(R.id.btnBack).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        sumIconScale = (TextView) findViewById(R.id.sumIconScale);
        sumDim = (TextView) findViewById(R.id.sumDim);
        sumBlur = (TextView) findViewById(R.id.sumBlur);
        sumKiller = (TextView) findViewById(R.id.sumKiller);
        sumSchedule = (TextView) findViewById(R.id.sumSchedule);
        sumTarget = (TextView) findViewById(R.id.sumTarget);
        sumApps = (TextView) findViewById(R.id.sumApps);
        sumSysApps = (TextView) findViewById(R.id.sumSysApps);
        sumHider = (TextView) findViewById(R.id.sumHider);
        sumTiles = (TextView) findViewById(R.id.sumTiles);
        sumColor = (TextView) findViewById(R.id.sumColor);
        sumSlide = (TextView) findViewById(R.id.sumSlide);
        checkBlurHome = (CheckBox) findViewById(R.id.checkBlurHome);
        checkBlurLock = (CheckBox) findViewById(R.id.checkBlurLock);
        checkKillerSystem = (CheckBox) findViewById(R.id.checkKillerSystem);
        switchKiller = (Switch) findViewById(R.id.switchKiller);
        switchSlide = (Switch) findViewById(R.id.switchSlide);
        switchRotate = (Switch) findViewById(R.id.switchRotate);
        switchSwipeUp = (Switch) findViewById(R.id.switchSwipeUp);
        switchDoubleTap = (Switch) findViewById(R.id.switchDoubleTap);
        switchSwipeDown = (Switch) findViewById(R.id.switchSwipeDown);
        sumRotate = (TextView) findViewById(R.id.sumRotate);
        sumSwipeUp = (TextView) findViewById(R.id.sumSwipeUp);
        sumDoubleTap = (TextView) findViewById(R.id.sumDoubleTap);
        sumHomeKey = (TextView) findViewById(R.id.sumHomeKey);
        sumBackKey = (TextView) findViewById(R.id.sumBackKey);
        sumSwipeDown = (TextView) findViewById(R.id.sumSwipeDown);
        sumOverlay = (TextView) findViewById(R.id.sumOverlay);
        rowKiller = findViewById(R.id.rowKiller);
        rowKillerSchedule = findViewById(R.id.rowKillerSchedule);
        rowKillerTarget = findViewById(R.id.rowKillerTarget);
        rowKillerApps = findViewById(R.id.rowKillerApps);
        rowKillerSysApps = findViewById(R.id.rowKillerSysApps);
        rowHider = findViewById(R.id.rowHider);
        rowRotate = findViewById(R.id.rowRotate);
        rowSwipeUp = findViewById(R.id.rowSwipeUp);
        rowDoubleTap = findViewById(R.id.rowDoubleTap);
        rowHomeKey = findViewById(R.id.rowHomeKey);
        rowBackKey = findViewById(R.id.rowBackKey);
        rowSwipeDown = findViewById(R.id.rowSwipeDown);
        rowOverlay = findViewById(R.id.rowOverlay);

        wireRows();
        tintWidgets(Prefs.accent(this));
        refreshSummaries();
        applyOrientation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        tintWidgets(Prefs.accent(this));
        refreshSummaries();
        applyOrientation();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != CatloftDeviceAdmin.REQUEST_ENABLE) {
            return;
        }
        if (resultCode == RESULT_OK) {
            Prefs.setDoubleTapLock(this, true);
        } else {
            Toast.makeText(this, "Double-tap to lock stays off", Toast.LENGTH_SHORT).show();
        }
        refreshSummaries();
    }

    /** Portrait unless the user opts into sensor rotation; applied here and on Home. */
    private void applyOrientation() {
        setRequestedOrientation(Prefs.autoRotate(this)
                ? ActivityInfo.SCREEN_ORIENTATION_SENSOR
                : ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    // --- wiring ---

    private void wireRows() {
        findViewById(R.id.rowIconScale).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int cur = Math.round(Prefs.iconScale(SettingsActivity.this) * 100);
                seek("Icon & title scale", 50, 150, cur, new SeekApply() {
                    @Override
                    public void apply(int value) {
                        Prefs.setIconScale(SettingsActivity.this, value / 100f);
                        refreshSummaries();
                    }
                });
            }
        });

        findViewById(R.id.rowDim).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                seek("Wallpaper dimness", 0, 100, Prefs.dim(SettingsActivity.this), new SeekApply() {
                    @Override
                    public void apply(int value) {
                        Prefs.setDim(SettingsActivity.this, value);
                        refreshSummaries();
                    }
                });
            }
        });

        findViewById(R.id.rowBlur).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                seek("Wallpaper blur", 0, 100, Prefs.blur(SettingsActivity.this), new SeekApply() {
                    @Override
                    public void apply(int value) {
                        Prefs.setBlur(SettingsActivity.this, value);
                        if (value > 0) {
                            WallpaperHelper.refreshBlurred(SettingsActivity.this, new Runnable() {
                                @Override
                                public void run() {
                                    WallpaperHelper.refreshLockWallpaper(SettingsActivity.this);
                                }
                            });
                        } else {
                            WallpaperHelper.refreshLockWallpaper(SettingsActivity.this);
                        }
                        refreshSummaries();
                    }
                });
            }
        });

        checkBlurHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Prefs.setBlurHome(SettingsActivity.this, checkBlurHome.isChecked());
                if (checkBlurHome.isChecked() && Prefs.blur(SettingsActivity.this) > 0) {
                    WallpaperHelper.refreshBlurred(SettingsActivity.this, null);
                }
                refreshSummaries();
            }
        });

        checkBlurLock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Prefs.setBlurLock(SettingsActivity.this, checkBlurLock.isChecked());
                WallpaperHelper.refreshLockWallpaper(SettingsActivity.this);
                refreshSummaries();
            }
        });

        View.OnClickListener killerToggle = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean on = !Prefs.killerOn(SettingsActivity.this);
                Prefs.setKillerOn(SettingsActivity.this, on);
                if (on) {
                    KillScheduler.schedule(SettingsActivity.this);
                } else {
                    KillScheduler.cancel(SettingsActivity.this);
                }
                refreshSummaries();
            }
        };
        rowKiller.setOnClickListener(killerToggle);
        switchKiller.setOnClickListener(killerToggle);

        rowKillerSchedule.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final long cur = Prefs.killerInterval(SettingsActivity.this);
                int checked = 0;
                for (int i = 0; i < INTERVALS.length; i++) {
                    if (INTERVALS[i] == cur) {
                        checked = i;
                    }
                }
                new AlertDialog.Builder(SettingsActivity.this)
                        .setTitle("Run every")
                        .setSingleChoiceItems(INTERVAL_LABELS, checked,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        Prefs.setKillerInterval(SettingsActivity.this,
                                                INTERVALS[which]);
                                        KillScheduler.schedule(SettingsActivity.this);
                                        refreshSummaries();
                                        dialog.dismiss();
                                    }
                                })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });

        rowKillerTarget.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean all = !"selected".equals(Prefs.killerMode(SettingsActivity.this));
                new AlertDialog.Builder(SettingsActivity.this)
                        .setTitle("What to kill")
                        .setSingleChoiceItems(
                                new String[]{"All background apps", "Only selected apps"},
                                all ? 0 : 1,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        Prefs.setKillerMode(SettingsActivity.this,
                                                which == 0 ? "all" : "selected");
                                        refreshSummaries();
                                        dialog.dismiss();
                                    }
                                })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });

        rowKillerApps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickApps("Selected apps", AppResolver.nonSystemApps(SettingsActivity.this),
                        Prefs.killerApps(SettingsActivity.this), false,
                        new StringSetHandler() {
                            @Override
                            public void onSet(Set<String> selected) {
                                Prefs.setKillerApps(SettingsActivity.this, selected);
                                refreshSummaries();
                            }
                        });
            }
        });

        checkKillerSystem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkKillerSystem.isChecked()) {
                    if (Prefs.killerSysApps(SettingsActivity.this).isEmpty()) {
                        // red warning requires an explicit system-app selection first
                        openSystemPickerForEnabling();
                    } else {
                        Prefs.setKillerSysOn(SettingsActivity.this, true);
                        refreshSummaries();
                    }
                } else {
                    Prefs.setKillerSysOn(SettingsActivity.this, false);
                    refreshSummaries();
                }
            }
        });

        rowKillerSysApps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!Prefs.killerSysOn(SettingsActivity.this)) {
                    return;
                }
                pickApps("System apps allowed to be killed",
                        AppResolver.systemApps(SettingsActivity.this),
                        Prefs.killerSysApps(SettingsActivity.this), true,
                        new StringSetHandler() {
                            @Override
                            public void onSet(Set<String> selected) {
                                Prefs.setKillerSysApps(SettingsActivity.this, selected);
                                refreshSummaries();
                            }
                        });
            }
        });

        rowHider.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickApps("Hidden apps (from Catloft's drawer)",
                        AppResolver.launchableApps(SettingsActivity.this),
                        Prefs.hiddenApps(SettingsActivity.this), false,
                        new StringSetHandler() {
                            @Override
                            public void onSet(Set<String> selected) {
                                Prefs.setHiddenApps(SettingsActivity.this, selected);
                                refreshSummaries();
                            }
                        });
            }
        });

        findViewById(R.id.rowTiles).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String[] labels = AppResolver.tileLabels(SettingsActivity.this);
                new AlertDialog.Builder(SettingsActivity.this)
                        .setTitle("Tiles")
                        .setItems(new String[]{
                                        "Tile 1: " + labels[0],
                                        "Tile 2: " + labels[1],
                                        "Tile 3: " + labels[2]
                                },
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        pickTileApp(which);
                                    }
                                })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });

        findViewById(R.id.rowColor).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new ColorPickerDialog(SettingsActivity.this,
                        Prefs.accent(SettingsActivity.this),
                        new ColorPickerDialog.OnColorChosen() {
                            @Override
                            public void onColorChosen(int color) {
                                Prefs.setAccent(SettingsActivity.this, color);
                                tintWidgets(color);
                                refreshSummaries();
                            }
                        }).show();
            }
        });

        View.OnClickListener slideToggle = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean on = !Prefs.slideUnlock(SettingsActivity.this);
                Prefs.setSlideUnlock(SettingsActivity.this, on);
                refreshSummaries();
            }
        };
        findViewById(R.id.rowSlide).setOnClickListener(slideToggle);
        switchSlide.setOnClickListener(slideToggle);

        View.OnClickListener rotateToggle = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean on = !Prefs.autoRotate(SettingsActivity.this);
                Prefs.setAutoRotate(SettingsActivity.this, on);
                applyOrientation();
                refreshSummaries();
            }
        };
        rowRotate.setOnClickListener(rotateToggle);
        switchRotate.setOnClickListener(rotateToggle);

        View.OnClickListener swipeUpToggle = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean on = !Prefs.swipeUpDrawer(SettingsActivity.this);
                Prefs.setSwipeUpDrawer(SettingsActivity.this, on);
                refreshSummaries();
            }
        };
        rowSwipeUp.setOnClickListener(swipeUpToggle);
        switchSwipeUp.setOnClickListener(swipeUpToggle);

        View.OnClickListener doubleTapToggle = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Prefs.doubleTapLock(SettingsActivity.this)) {
                    Prefs.setDoubleTapLock(SettingsActivity.this, false);
                    refreshSummaries();
                } else if (CatloftDeviceAdmin.isActive(SettingsActivity.this)) {
                    Prefs.setDoubleTapLock(SettingsActivity.this, true);
                    refreshSummaries();
                } else {
                    // Ask Android; the preference is stored from onActivityResult only if
                    // the user actually grants the admin — never from here.
                    CatloftDeviceAdmin.request(SettingsActivity.this);
                }
            }
        };
        rowDoubleTap.setOnClickListener(doubleTapToggle);
        switchDoubleTap.setOnClickListener(doubleTapToggle);

        rowHomeKey.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickKeyApp("Home key opens", Prefs.homeKeyApp(SettingsActivity.this),
                        new KeyPickHandler() {
                            @Override
                            public void onPicked(String spec) {
                                Prefs.setHomeKeyApp(SettingsActivity.this, spec);
                                refreshSummaries();
                            }
                        });
            }
        });

        rowBackKey.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickKeyApp("Back key opens", Prefs.backKeyApp(SettingsActivity.this),
                        new KeyPickHandler() {
                            @Override
                            public void onPicked(String spec) {
                                Prefs.setBackKeyApp(SettingsActivity.this, spec);
                                refreshSummaries();
                            }
                        });
            }
        });

        View.OnClickListener swipeDownToggle = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean on = !Prefs.swipeDownShade(SettingsActivity.this);
                Prefs.setSwipeDownShade(SettingsActivity.this, on);
                refreshSummaries();
                // Catloft may switch its own half on, but Android owns the other half:
                // only the user can enable an accessibility service, in Android's screen.
                if (on && !CatloftAccessibilityService.isEnabled(SettingsActivity.this)) {
                    openAccessibilitySettings();
                }
            }
        };
        rowSwipeDown.setOnClickListener(swipeDownToggle);
        switchSwipeDown.setOnClickListener(swipeDownToggle);

        rowOverlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openOverlaySettings();
            }
        });
    }

    /** Settings → Accessibility, where only the user can enable Catloft's service. */
    private void openAccessibilitySettings() {
        try {
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
        } catch (Exception e) {
            Toast.makeText(this, "Open Settings › Accessibility by hand", Toast.LENGTH_SHORT)
                    .show();
        }
    }

    /** Settings → "draw over other apps", the only way to grant SYSTEM_ALERT_WINDOW. */
    private void openOverlaySettings() {
        try {
            startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName())));
        } catch (Exception e) {
            try {
                startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION));
            } catch (Exception e2) {
                Toast.makeText(this,
                        "Open Settings › Apps › Catloft › draw over other apps",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Single-choice picker for a Home/Back mapping: entry 0 is always
     * "Default — do nothing" (the empty spec), then every launchable app.
     */
    private void pickKeyApp(String title, final String current, final KeyPickHandler handler) {
        final List<AppResolver.AppEntry> apps = AppResolver.launchableApps(this);
        final String[] labels = new String[apps.size() + 1];
        labels[0] = "Default — do nothing";
        int checked = 0;
        for (int i = 0; i < apps.size(); i++) {
            labels[i + 1] = apps.get(i).label + (apps.get(i).system ? "  (system)" : "");
            if (("component:" + apps.get(i).component.flattenToString()).equals(current)) {
                checked = i + 1;
            }
        }
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setSingleChoiceItems(labels, checked, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        handler.onPicked(which == 0
                                ? ""
                                : "component:" + apps.get(which - 1).component.flattenToString());
                        dialog.dismiss();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void pickTileApp(final int index) {
        final List<AppResolver.AppEntry> apps = AppResolver.launchableApps(this);
        String[] labels = new String[apps.size()];
        for (int i = 0; i < apps.size(); i++) {
            labels[i] = apps.get(i).label + (apps.get(i).system ? "  (system)" : "");
        }
        new AlertDialog.Builder(this)
                .setTitle("Tile " + (index + 1) + " — pick an app")
                .setItems(labels, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Prefs.setTile(SettingsActivity.this, index,
                                "component:" + apps.get(which).component.flattenToString());
                        refreshSummaries();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /** Multi-select app picker; when requireOne, OK stays disabled-looking until ≥1 is ticked. */
    private void pickApps(String title, final List<AppResolver.AppEntry> apps,
                          Set<String> current, final boolean requireOne,
                          final StringSetHandler handler) {
        final String[] labels = new String[apps.size()];
        final boolean[] checked = new boolean[apps.size()];
        for (int i = 0; i < apps.size(); i++) {
            labels[i] = apps.get(i).label + (apps.get(i).system ? "  (system)" : "");
            checked[i] = current.contains(apps.get(i).pkg);
        }
        AlertDialog.Builder b = new AlertDialog.Builder(this)
                .setTitle(title)
                .setMultiChoiceItems(labels, checked,
                        new DialogInterface.OnMultiChoiceClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which,
                                                boolean isChecked) {
                                checked[which] = isChecked;
                            }
                        })
                .setPositiveButton("OK", null)
                .setNegativeButton("Cancel", null);
        final AlertDialog d = b.create();
        d.setOnShowListener(new android.content.DialogInterface.OnShowListener() {
            @Override
            public void onShow(android.content.DialogInterface dialog) {
                d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Set<String> out = new HashSet<>();
                                for (int i = 0; i < apps.size(); i++) {
                                    if (checked[i]) {
                                        out.add(apps.get(i).pkg);
                                    }
                                }
                                if (requireOne && out.isEmpty()) {
                                    Toast.makeText(SettingsActivity.this,
                                            "Pick at least one — or untick the red warning",
                                            Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                handler.onSet(out);
                                d.dismiss();
                            }
                        });
                d.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                d.dismiss();
                            }
                        });
            }
        });
        d.show();
        tintDialogButtons(d);
    }

    /** Enabling the red system-kill warning demands an explicit selection. */
    private void openSystemPickerForEnabling() {
        final List<AppResolver.AppEntry> apps = AppResolver.systemApps(this);
        final String[] labels = new String[apps.size()];
        final boolean[] checked = new boolean[apps.size()];
        for (int i = 0; i < apps.size(); i++) {
            labels[i] = apps.get(i).label;
        }
        AlertDialog d = new AlertDialog.Builder(this)
                .setTitle("⚠ Which system apps may be killed?")
                .setMultiChoiceItems(labels, checked,
                        new DialogInterface.OnMultiChoiceClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which,
                                                boolean isChecked) {
                                checked[which] = isChecked;
                            }
                        })
                .setPositiveButton("Enable", null)
                .setNegativeButton("Cancel", null)
                .create();
        d.setOnShowListener(new android.content.DialogInterface.OnShowListener() {
            @Override
            public void onShow(android.content.DialogInterface dialog) {
                d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Set<String> out = new HashSet<>();
                                for (int i = 0; i < apps.size(); i++) {
                                    if (checked[i]) {
                                        out.add(apps.get(i).pkg);
                                    }
                                }
                                if (out.isEmpty()) {
                                    Toast.makeText(SettingsActivity.this,
                                            "Select at least one system app",
                                            Toast.LENGTH_LONG).show();
                                    return;
                                }
                                Prefs.setKillerSysApps(SettingsActivity.this, out);
                                Prefs.setKillerSysOn(SettingsActivity.this, true);
                                refreshSummaries();
                                d.dismiss();
                            }
                        });
                d.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                checkKillerSystem.setChecked(false); // declined
                                refreshSummaries();
                                d.dismiss();
                            }
                        });
            }
        });
        d.show();
        tintDialogButtons(d);
    }

    // --- generic dialogs ---

    private interface SeekApply {
        void apply(int value);
    }

    private void seek(String title, int min, int max, int current, final SeekApply apply) {
        View v = getLayoutInflater().inflate(R.layout.seek_dialog, null, false);
        final SeekBar bar = (SeekBar) v.findViewById(R.id.seekBar);
        final TextView val = (TextView) v.findViewById(R.id.seekValue);
        Ui.tintSeekBar(bar, Prefs.accent(this));
        bar.setMax(max - min);
        bar.setProgress(current - min);
        val.setText((min + bar.getProgress()) + "%");
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                val.setText((min + progress) + "%");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        AlertDialog d = new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(v)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        apply.apply(min + bar.getProgress());
                    }
                })
                .setNegativeButton("Cancel", null)
                .create();
        d.show();
        tintDialogButtons(d);
    }

    private void tintDialogButtons(AlertDialog d) {
        int accent = Prefs.accent(this);
        if (d.getButton(AlertDialog.BUTTON_POSITIVE) != null) {
            d.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(accent);
        }
        if (d.getButton(AlertDialog.BUTTON_NEGATIVE) != null) {
            d.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(accent);
        }
        if (d.getButton(AlertDialog.BUTTON_NEUTRAL) != null) {
            d.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(accent);
        }
    }

    // --- state display ---

    private void tintWidgets(int accent) {
        Ui.tintSwitch(switchKiller, accent);
        Ui.tintSwitch(switchSlide, accent);
        Ui.tintSwitch(switchRotate, accent);
        Ui.tintSwitch(switchSwipeUp, accent);
        Ui.tintSwitch(switchDoubleTap, accent);
        Ui.tintSwitch(switchSwipeDown, accent);
        Ui.tintCheckBox(checkBlurHome, accent);
        Ui.tintCheckBox(checkBlurLock, accent);
        // checkKillerSystem stays danger-red on purpose

        tintSections(findViewById(android.R.id.content), accent);
    }

    /**
     * Walk the entire view tree for section headers instead of reaching through
     * {@code btnBack → parent → parent → child 1 → child 0} and hoping the layout still has
     * that shape. The old form survived every layout edit so far, but a silent index
     * assumption is exactly the kind of coupling that turns a cosmetic change into a crash.
     */
    private void tintSections(View v, int accent) {
        if (v instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) v;
            for (int i = 0; i < group.getChildCount(); i++) {
                tintSections(group.getChildAt(i), accent);
            }
        }
        if (v instanceof TextView && "section".equals(v.getTag())) {
            ((TextView) v).setTextColor(accent);
        }
    }

    private void refreshSummaries() {
        sumIconScale.setText(Math.round(Prefs.iconScale(this) * 100) + "%");
        sumDim.setText(Prefs.dim(this) + "%");
        int blur = Prefs.blur(this);
        sumBlur.setText(blur == 0 ? "Off" : blur + "%");
        checkBlurHome.setChecked(Prefs.blurHome(this));
        checkBlurLock.setChecked(Prefs.blurLock(this));

        boolean killerOn = Prefs.killerOn(this);
        switchKiller.setChecked(killerOn);
        String mode = "selected".equals(Prefs.killerMode(this))
                ? "selected apps" : "all background apps";
        String last = "";
        long lastRun = Prefs.killerLast(this);
        if (lastRun > 0) {
            last = " · last " + new SimpleDateFormat("HH:mm", Locale.getDefault())
                    .format(new Date(lastRun));
        }
        sumKiller.setText(killerOn
                ? "Every " + intervalLabel(Prefs.killerInterval(this)) + " · " + mode + last
                : "Off");
        sumSchedule.setText(intervalLabel(Prefs.killerInterval(this)));
        sumTarget.setText("selected".equals(Prefs.killerMode(this))
                ? "Only selected apps" : "All background apps");
        sumApps.setText(Prefs.killerApps(this).size() + " selected");

        boolean sysOn = Prefs.killerSysOn(this);
        checkKillerSystem.setChecked(sysOn);
        int sysCount = Prefs.killerSysApps(this).size();
        sumSysApps.setText(sysCount == 0
                ? "None selected — required" : sysCount + " selected");
        rowKillerSysApps.setAlpha(sysOn ? 1f : 0.45f);
        rowKillerSysApps.setClickable(sysOn);

        float subAlpha = killerOn ? 1f : 0.45f;
        rowKillerSchedule.setAlpha(subAlpha);
        rowKillerSchedule.setClickable(killerOn);
        rowKillerTarget.setAlpha(subAlpha);
        rowKillerTarget.setClickable(killerOn);
        rowKillerApps.setAlpha(subAlpha);
        rowKillerApps.setClickable(killerOn);

        Set<String> hidden = Prefs.hiddenApps(this);
        sumHider.setText(hidden.isEmpty() ? "No apps hidden" : hidden.size() + " hidden");

        String[] tiles = AppResolver.tileLabels(this);
        sumTiles.setText(tiles[0] + " · " + tiles[1] + " · " + tiles[2]);

        sumColor.setText(String.format("#%06X", Prefs.accent(this) & 0xFFFFFF));

        boolean slide = Prefs.slideUnlock(this);
        switchSlide.setChecked(slide);
        sumSlide.setText(slide ? "On — appears when the screen wakes" : "Off");

        boolean rotate = Prefs.autoRotate(this);
        switchRotate.setChecked(rotate);
        sumRotate.setText(rotate ? "On — follows the sensor" : "Off — locked to portrait");

        boolean swipeUp = Prefs.swipeUpDrawer(this);
        switchSwipeUp.setChecked(swipeUp);
        sumSwipeUp.setText(swipeUp ? "On" : "Off");

        // The preference can outlive its grant: Android lets the user revoke device admin
        // behind our back, so show what would actually happen rather than what is stored.
        boolean doubleTap = Prefs.doubleTapLock(this);
        boolean admin = CatloftDeviceAdmin.isActive(this);
        switchDoubleTap.setChecked(doubleTap && admin);
        sumDoubleTap.setText(!doubleTap
                ? "Off"
                : (admin ? "On — double-tap the empty home screen"
                         : "Off — Android revoked device admin"));

        sumHomeKey.setText(keyLabel(Prefs.homeKeyApp(this)));
        sumBackKey.setText(keyLabel(Prefs.backKeyApp(this)));

        // Live service state, not just the stored pref — the two can disagree while the
        // user is halfway through Android's own Accessibility screen.
        boolean shadeWanted = Prefs.swipeDownShade(this);
        boolean shadeService = CatloftAccessibilityService.isEnabled(this);
        switchSwipeDown.setChecked(shadeWanted);
        sumSwipeDown.setText(!shadeWanted
                ? "Off"
                : (shadeService ? "On — swipe down on the home screen"
                                : "On — waiting for Android's accessibility service"));

        boolean overlay = LockOverlayWindow.canDraw(this);
        sumOverlay.setText(overlay ? "On — covers other apps" : "Off — tap to grant");
    }

    /** "Default — do nothing" for the empty key spec, otherwise the mapped app's label. */
    private String keyLabel(String spec) {
        if (spec == null || spec.length() == 0) {
            return "Default — do nothing";
        }
        return AppResolver.label(this, spec);
    }

    private static String intervalLabel(long ms) {
        for (int i = 0; i < INTERVALS.length; i++) {
            if (INTERVALS[i] == ms) {
                return INTERVAL_LABELS[i];
            }
        }
        return (ms / 60000L) + " minutes";
    }
}
