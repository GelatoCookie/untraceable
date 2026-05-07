package com.zebra.rfid.demo.sdksample;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.zebra.rfid.api3.TagData;

import java.util.ArrayList;
import java.util.LinkedHashMap;

public class MainActivity extends AppCompatActivity implements RFIDHandler.ResponseHandlerInterface {

    public TextView statusTextViewRFID = null;
    private TextView testStatus;
    private TextView tvTagCount;
    private ListView listViewTags;
    private TagAdapter tagAdapter;
    private final LinkedHashMap<String, TagEntry> tagMap = new LinkedHashMap<>();

    RFIDHandler rfidHandler;
    final static String TAG = "RFID_SAMPLE";
    private static final int BLUETOOTH_PERMISSION_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT}, BLUETOOTH_PERMISSION_REQUEST_CODE);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, BLUETOOTH_PERMISSION_REQUEST_CODE);
            }
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        statusTextViewRFID = findViewById(R.id.textStatus);
        testStatus         = findViewById(R.id.testStatus);
        tvTagCount         = findViewById(R.id.tvTagCount);
        listViewTags       = findViewById(R.id.listViewTags);

        tagAdapter = new TagAdapter(this, new ArrayList<>());
        listViewTags.setAdapter(tagAdapter);

        rfidHandler = new RFIDHandler();
        rfidHandler.onCreate(this);

        findViewById(R.id.button).setOnClickListener(v  -> testStatus.setText(rfidHandler.Test1()));
        findViewById(R.id.button2).setOnClickListener(v -> testStatus.setText(rfidHandler.Test2()));
        findViewById(R.id.button3).setOnClickListener(v -> testStatus.setText(rfidHandler.Defaults()));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) return true;

        if (id == R.id.action_start) {
            clearTagList();
            rfidHandler.performUntraceable();
        }
        if (id == R.id.action_stop)                  rfidHandler.stopInventory();
        if (id == R.id.action_access)                rfidHandler.performUntraceableHideEPCTest();
        if (id == R.id.action_restore_public_access) rfidHandler.restorePublicAccess();
        if (id == R.id.action_clesr)                 clearTagList();


        return super.onOptionsItemSelected(item);
    }

    private void clearTagList() {
        runOnUiThread(() -> {
            tagMap.clear();
            tagAdapter.clear();
            tagAdapter.notifyDataSetChanged();
            tvTagCount.setText("Tags: 0");
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        rfidHandler.onPause();
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        statusTextViewRFID.setText(rfidHandler.onResume());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        rfidHandler.onDestroy();
    }

    // ── RFIDHandler callbacks ─────────────────────────────────────────────────

    @Override
    public void handleTagdata(TagData[] tagData) {
        for (TagData td : tagData) {
            String id = td.getTagID();
            TagEntry existing = tagMap.get(id);
            if (existing != null) {
                existing.readCount++;
                existing.peakRSSI = td.getPeakRSSI();
            } else {
                tagMap.put(id, new TagEntry(id, td.getPeakRSSI(), 1));
            }
        }
        runOnUiThread(() -> {
            tagAdapter.clear();
            tagAdapter.addAll(tagMap.values());
            tagAdapter.notifyDataSetChanged();
            tvTagCount.setText("Tags: " + tagMap.size());
        });
    }

    @Override
    public void handleTriggerPress(boolean pressed) {
        if (pressed) {
            clearTagList();
            rfidHandler.performInventory();
        } else {
            rfidHandler.stopInventory();
        }
    }

    // ── Tag List Adapter ──────────────────────────────────────────────────────

    private static class TagAdapter extends ArrayAdapter<TagEntry> {

        private final LayoutInflater inflater;

        TagAdapter(Context context, ArrayList<TagEntry> items) {
            super(context, R.layout.item_tag, items);
            inflater = LayoutInflater.from(context);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.item_tag, parent, false);
                holder = new ViewHolder();
                holder.tvTagId = convertView.findViewById(R.id.tvTagId);
                holder.tvRssi  = convertView.findViewById(R.id.tvRssi);
                holder.tvCount = convertView.findViewById(R.id.tvCount);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            TagEntry entry = getItem(position);
            if (entry != null) {
                holder.tvTagId.setText(entry.tagId);
                holder.tvRssi.setText(entry.peakRSSI + " dBm");
                holder.tvCount.setText("×" + entry.readCount);
            }
            return convertView;
        }

        static class ViewHolder {
            TextView tvTagId, tvRssi, tvCount;
        }
    }
}

