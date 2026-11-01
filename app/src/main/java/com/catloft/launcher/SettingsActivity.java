package com.catloft.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
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
            sumRotate, sumSwipeUp;
    private CheckBox checkBlurHome, checkBlurLock, checkKillerSystem;
    private Switch switchKiller, switchSlide, switchRotate, switchSwipeUp;
    private View rowKiller, rowKillerSchedule, rowKillerTarget, rowKillerApps,
            rowKillerSysApps, rowHider, rowRotate, rowSwipeUp;

    private interface StringSetHandler {
        void onSet(Set<String> selected);
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
        sumRotate = (TextView) findViewById(R.id.sumRotate);
        sumSwipeUp = (TextView) findViewById(R.id.sumSwipeUp);
        rowKiller = findViewById(R.id.rowKiller);
        rowKillerSchedule = findViewById(R.id.rowKillerSchedule);
        rowKillerTarget = findViewById(R.id.rowKillerTarget);
        rowKillerApps = findViewById(R.id.rowKillerApps);
        rowKillerSysApps = findViewById(R.id.rowKillerSysApps);
        rowHider = findViewById(R.id.rowHider);
        rowRotate = findViewById(R.id.rowRotate);
        rowSwipeUp = findViewById(R.id.rowSwipeUp);

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
                pickApps("Selected apps", AppResolver.allApps(SettingsActivity.this),
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
