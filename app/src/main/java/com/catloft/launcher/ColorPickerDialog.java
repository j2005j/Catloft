package com.catloft.launcher;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Accent color picker in the style of the reference screenshot: header with
 * apply/cancel, RECENTS row, PALETTE grid of Material colors, and an
 * Advanced dialog (H/S/V sliders + hex).
 */
public class ColorPickerDialog extends android.app.Dialog {

    public interface OnColorChosen {
        void onColorChosen(int color);
    }

    private static final int[] PALETTE = {
            0xFFFFFFFF, 0xFF607D8B, 0xFF37474F, 0xFF000000,
            0xFF3F51B5, 0xFF2196F3, 0xFF00BCD4, 0xFF009688,
            0xFF4CAF50, 0xFF8BC34A, 0xFFCDDC39, 0xFFFFEB3B,
            0xFFFFC107, 0xFFFF9800, 0xFFFF5722, 0xFFF44336,
            0xFFE91E63, 0xFF9C27B0, 0xFF673AB7, 0xFF5C6BC0
    };

    private final int initialColor;
    private final OnColorChosen callback;
    private int selected;

    private final List<FrameLayout> cells = new ArrayList<>();
    private final List<Integer> cellColors = new ArrayList<>();

    private LinearLayout recentsRow;
    private TextView titleView;

    public ColorPickerDialog(Context context, int current, OnColorChosen callback) {
        super(context, R.style.CatloftDialogTheme);
        this.initialColor = current;
        this.selected = current;
        this.callback = callback;
        requestWindowFeature(Window.FEATURE_NO_TITLE);
    }

    @Override
    protected void onCreate(android.os.Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.color_picker);

        titleView = (TextView) findViewById(R.id.pickerTitle);
        recentsRow = (LinearLayout) findViewById(R.id.recentsRow);
        LinearLayout grid = (LinearLayout) findViewById(R.id.paletteGrid);

        int accent = Prefs.accent(getContext());
        titleView.setTextColor(accent);
        ((TextView) findViewById(R.id.labelRecents)).setTextColor(accent);
        ((TextView) findViewById(R.id.labelPalette)).setTextColor(accent);
        findViewById(R.id.advancedRow).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        openAdvanced();
                    }
                });

        // palette: 4 per row
        LinearLayout row = null;
        for (int i = 0; i < PALETTE.length; i++) {
            if (i % 4 == 0) {
                row = new LinearLayout(getContext());
                row.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                grid.addView(row, rlp);
            }
            FrameLayout cell = makeCell(PALETTE[i]);
            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            cell.setPadding(Ui.dp(getContext(), 6), Ui.dp(getContext(), 6),
                    Ui.dp(getContext(), 6), Ui.dp(getContext(), 6));
            row.addView(cell, clp);
        }

        buildRecents();

        findViewById(R.id.btnOk).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Prefs.setAccent(getContext(), selected);
                addRecent(selected);
                if (callback != null) {
                    callback.onColorChosen(selected);
                }
                dismiss();
            }
        });
        findViewById(R.id.btnCancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss(); // keeps the previous accent
            }
        });

        updateChecks();

        Window w = getWindow();
        if (w != null) {
            int screenW = getContext().getResources().getDisplayMetrics().widthPixels;
            w.setLayout(Math.min(screenW - Ui.dp(getContext(), 32), Ui.dp(getContext(), 360)),
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    private FrameLayout makeCell(final int color) {
        final FrameLayout cell = new FrameLayout(getContext());
        View circle = new View(getContext());
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.OVAL);
        d.setColor(color);
        d.setStroke(Ui.dp(getContext(), 1), 0x59FFFFFF);
        circle.setBackground(d);
        FrameLayout.LayoutParams clp = new FrameLayout.LayoutParams(
                Ui.dp(getContext(), 46), Ui.dp(getContext(), 46));
        clp.gravity = Gravity.CENTER;
        cell.addView(circle, clp);

        TextView check = new TextView(getContext());
        check.setText("✓");
        check.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        check.setTextColor(luminance(color) > 0.5f ? 0xFF616161 : Color.WHITE);
        check.setVisibility(View.GONE);
        FrameLayout.LayoutParams chkLp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        chkLp.gravity = Gravity.CENTER;
        cell.addView(check, chkLp);
        cell.setTag(R.id.tag_check, check);
        cell.setTag(R.id.tag_color, color);

        cell.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selected = color;
                updateChecks();
            }
        });
        cells.add(cell);
        cellColors.add(color);
        return cell;
    }

    private void buildRecents() {
        // drop stale recents cells from the master list before rebuilding
        for (int i = cells.size() - 1; i >= 0; i--) {
            if (cells.get(i).getParent() == recentsRow) {
                cells.remove(i);
                cellColors.remove(i);
            }
        }
        recentsRow.removeAllViews();
        List<Integer> recents = recentList();
        if (recents.isEmpty()) {
            recents.add(initialColor);
        }
        if (!recents.contains(selected)) {
            recents.add(0, selected); // show the in-progress pick too
        }
        for (int color : recents) {
            FrameLayout cell = makeCell(color);
            cell.setPadding(Ui.dp(getContext(), 6), Ui.dp(getContext(), 6),
                    Ui.dp(getContext(), 6), Ui.dp(getContext(), 6));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.rightMargin = Ui.dp(getContext(), 14);
            recentsRow.addView(cell, lp);
        }
    }

    private void updateChecks() {
        for (int i = 0; i < cells.size(); i++) {
            FrameLayout cell = cells.get(i);
            int color = cellColors.get(i);
            TextView check = (TextView) cell.getTag(R.id.tag_check);
            View circle = cell.getChildAt(0);
            GradientDrawable d = new GradientDrawable();
            d.setShape(GradientDrawable.OVAL);
            d.setColor(color);
            boolean sel = color == selected;
            d.setStroke(Ui.dp(getContext(), sel ? 3 : 1),
                    sel ? 0xFFFFFFFF : 0x59FFFFFF);
            circle.setBackground(d);
            check.setVisibility(sel ? View.VISIBLE : View.GONE);
        }
    }

    // --- recents persistence ---

    private List<Integer> recentList() {
        List<Integer> out = new ArrayList<>();
        String csv = Prefs.recentColors(getContext());
        if (csv == null || csv.isEmpty()) {
            return out;
        }
        for (String s : csv.split(",")) {
            try {
                out.add((int) Long.parseLong(s.trim()));
            } catch (NumberFormatException ignored) {
                // skip junk
            }
        }
        return out;
    }

    private void addRecent(int color) {
        List<Integer> list = recentList();
        Integer boxed = color;
        list.remove(boxed);
        list.add(0, boxed);
        while (list.size() > 8) {
            list.remove(list.size() - 1);
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(list.get(i) & 0xFFFFFFFFL);
        }
        Prefs.setRecentColors(getContext(), sb.toString());
    }

    // --- advanced: H/S/V sliders + hex ---

    private void openAdvanced() {
        final float[] hsv = new float[3];
        Color.colorToHSV(selected, hsv);

        LinearLayout box = new LinearLayout(getContext());
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = Ui.dp(getContext(), 20);
        box.setPadding(pad, Ui.dp(getContext(), 8), pad, 0);

        final TextView preview = new TextView(getContext());
        preview.setText("  ");
        LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(
                Ui.dp(getContext(), 40), Ui.dp(getContext(), 40));
        plp.gravity = Gravity.CENTER_HORIZONTAL;
        plp.bottomMargin = Ui.dp(getContext(), 12);
        box.addView(preview, plp);

        final SeekBar hue = slider(box, "Hue", 0, 360, Math.round(hsv[0]));
        final SeekBar sat = slider(box, "Saturation", 0, 100, Math.round(hsv[1]));
        final SeekBar val = slider(box, "Value", 0, 100, Math.round(hsv[2]));

        final EditText hex = new EditText(getContext());
        hex.setHint("#RRGGBB");
        hex.setSingleLine(true);
        hex.setTextColor(Color.WHITE);
        hex.setHintTextColor(0x757575);
        LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        hlp.topMargin = Ui.dp(getContext(), 10);
        box.addView(hex, hlp);

        final Runnable[] render = new Runnable[1];
        render[0] = new Runnable() {
            @Override
            public void run() {
                int c = Color.HSVToColor(hsv);
                GradientDrawable d = new GradientDrawable();
                d.setShape(GradientDrawable.OVAL);
                d.setColor(c);
                d.setStroke(Ui.dp(getContext(), 1), 0x59FFFFFF);
                preview.setBackground(d);
                if (!hex.getText().toString().equalsIgnoreCase(
                        String.format("#%06X", c & 0xFFFFFF))) {
                    hex.setText(String.format("#%06X", c & 0xFFFFFF));
                }
            }
        };

        SeekBar.OnSeekBarChangeListener hsvListener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                hsv[0] = hue.getProgress();
                hsv[1] = sat.getProgress();
                hsv[2] = val.getProgress();
                render[0].run();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        };
        hue.setOnSeekBarChangeListener(hsvListener);
        sat.setOnSeekBarChangeListener(hsvListener);
        val.setOnSeekBarChangeListener(hsvListener);

        hex.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void onTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String t = s.toString().trim();
                if (!t.startsWith("#")) {
                    t = "#" + t;
                }
                if (t.length() == 7) {
                    try {
                        int c = Color.parseColor(t);
                        Color.colorToHSV(c, hsv);
                        hue.setProgress(Math.round(hsv[0]));
                        sat.setProgress(Math.round(hsv[1]));
                        val.setProgress(Math.round(hsv[2]));
                        GradientDrawable d = new GradientDrawable();
                        d.setShape(GradientDrawable.OVAL);
                        d.setColor(c);
                        d.setStroke(Ui.dp(getContext(), 1), 0x59FFFFFF);
                        preview.setBackground(d);
                    } catch (IllegalArgumentException ignored) {
                        // not a valid hex color yet
                    }
                }
            }
        });

        final AlertDialog d = new AlertDialog.Builder(getContext())
                .setTitle("Advanced")
                .setView(box)
                .setPositiveButton("OK", null)
                .setNegativeButton("Cancel", null)
                .create();
        d.setOnShowListener(new android.content.DialogInterface.OnShowListener() {
            @Override
            public void onShow(android.content.DialogInterface dialog) {
                render[0].run();
                d.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Prefs.accent(getContext()));
                d.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Prefs.accent(getContext()));
                d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                selected = Color.HSVToColor(hsv);
                                buildRecents(); // may show the custom pick up front
                                updateChecks();
                                d.dismiss();
                            }
                        });
            }
        });
        d.show();
    }

    private SeekBar slider(LinearLayout box, String label, int min, int max, int cur) {
        TextView t = new TextView(getContext());
        t.setText(label);
        t.setTextColor(0xB3FFFFFF);
        t.setTextSize(13);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        tlp.topMargin = Ui.dp(getContext(), 6);
        box.addView(t, tlp);

        SeekBar bar = new SeekBar(getContext());
        bar.setMax(max - min);
        bar.setProgress(cur - min);
        Ui.tintSeekBar(bar, Prefs.accent(getContext()));
        box.addView(bar, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        return bar;
    }

    private static float luminance(int color) {
        float r = Color.red(color) / 255f;
        float g = Color.green(color) / 255f;
        float b = Color.blue(color) / 255f;
        return 0.299f * r + 0.587f * g + 0.114f * b;
    }
}
