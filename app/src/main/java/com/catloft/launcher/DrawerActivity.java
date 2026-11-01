package com.catloft.launcher;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Full-screen list of apps: icon left, label right. Hidden apps filtered out. */
public class DrawerActivity extends Activity {

    private final List<AppResolver.AppEntry> items = new ArrayList<>();
    private AppAdapter adapter;
    private ListView list;
    private TextView empty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drawer);

        list = (ListView) findViewById(R.id.appList);
        empty = (TextView) findViewById(R.id.emptyText);
        adapter = new AppAdapter();
        list.setAdapter(adapter);

        list.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(android.widget.AdapterView<?> parent, View view,
                                    int position, long id) {
                AppResolver.AppEntry e = items.get(position);
                Intent i = new Intent(Intent.ACTION_MAIN);
                i.addCategory(Intent.CATEGORY_LAUNCHER);
                i.setComponent(e.component);
                try {
                    startActivity(i);
                    finish();
                } catch (Exception ex) {
                    android.widget.Toast.makeText(DrawerActivity.this,
                            "Could not open " + e.label, android.widget.Toast.LENGTH_SHORT).show();
                }
            }
        });

        findViewById(R.id.btnBack).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // load off the main thread — icon decoding can be slow on old hardware
        WallpaperHelper.io.execute(new Runnable() {
            @Override
            public void run() {
                final List<AppResolver.AppEntry> all = AppResolver.launchableApps(DrawerActivity.this);
                final Set<String> hidden = Prefs.hiddenApps(DrawerActivity.this);
                final List<AppResolver.AppEntry> shown = new ArrayList<>();
                for (AppResolver.AppEntry e : all) {
                    if (!hidden.contains(e.pkg)) {
                        shown.add(e);
                    }
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        items.clear();
                        items.addAll(shown);
                        adapter.notifyDataSetChanged();
                        empty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
                        list.setVisibility(items.isEmpty() ? View.GONE : View.VISIBLE);
                    }
                });
            }
        });
    }

    private class AppAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public Object getItem(int position) {
            return items.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView;
            if (row == null) {
                row = getLayoutInflater().inflate(R.layout.row_app, parent, false);
            }
            AppResolver.AppEntry e = items.get(position);
            ImageView icon = (ImageView) row.findViewById(R.id.rowIcon);
            TextView label = (TextView) row.findViewById(R.id.rowLabel);

            float scale = Prefs.iconScale(DrawerActivity.this);
            icon.setImageDrawable(e.icon);
            int px = Math.round(Ui.dp(DrawerActivity.this, 40) * scale);
            ViewGroup.LayoutParams lp = icon.getLayoutParams();
            lp.width = px;
            lp.height = px;
            icon.setLayoutParams(lp);
            label.setText(e.label);
            label.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 16 * scale);
            return row;
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed(); // plain back closes the drawer
    }
}
