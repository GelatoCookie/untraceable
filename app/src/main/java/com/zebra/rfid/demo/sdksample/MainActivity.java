package com.zebra.rfid.demo.sdksample;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
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
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.zebra.rfid.api3.ACCESS_OPERATION_CODE;
import com.zebra.rfid.api3.ACCESS_OPERATION_STATUS;
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
        testStatus = findViewById(R.id.testStatus);
        tvTagCount = findViewById(R.id.tvTagCount);
        listViewTags = findViewById(R.id.listViewTags);

        tagAdapter = new TagAdapter(this, new ArrayList<>());
        listViewTags.setAdapter(tagAdapter);

        // Handle tag selection for Untraceable operation
        listViewTags.setOnItemClickListener((parent, view, position, id) -> {
            TagEntry selectedTag = (TagEntry) parent.getItemAtPosition(position);
            showUntraceableDialog(selectedTag);
        });

        rfidHandler = new RFIDHandler();
        rfidHandler.onCreate(this);

        findViewById(R.id.button).setOnClickListener(v  -> testStatus.setText(rfidHandler.Test1()));
        findViewById(R.id.button2).setOnClickListener(v -> testStatus.setText(rfidHandler.Test2()));
        findViewById(R.id.button3).setOnClickListener(v -> testStatus.setText(rfidHandler.Defaults()));
        findViewById(R.id.buttonRestoreAccess).setOnClickListener(v -> {
            clearTagList();
            rfidHandler.restorePublicAccess();
            testStatus.setText("Restore Public Access triggered");
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            return true;
        }
        if (id == R.id.action_stop) {
            rfidHandler.stopInventory();
            return true;
        }
        if (id == R.id.action_clesr) {
            clearTagList();
            return true;
        }

        if (id == R.id.action_start) {
            clearTagList();
            rfidHandler.performUntraceable();
            return true;
        }
        if (id == R.id.action_access) {
            clearTagList();
            rfidHandler.performAccessTID();
            return true;
        }
        if (id == R.id.action_restore_public_access) {
            clearTagList();
            rfidHandler.restorePublicAccess();
            return true;
        }

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
                existing = new TagEntry(id, td.getPeakRSSI(), 1);
                tagMap.put(id, existing);
            }
            if (td.getOpCode() == ACCESS_OPERATION_CODE.ACCESS_OPERATION_READ &&
                    td.getOpStatus() == ACCESS_OPERATION_STATUS.ACCESS_SUCCESS) {
                if (td.getMemoryBankData() != null && td.getMemoryBankData().length() > 0) {
                    existing.memoryBank = td.getMemoryBank().toString();
                    existing.memoryBankData = td.getMemoryBankData();
                }
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

    // ── Untraceable Operation Dialog ────────────────────────────────────────────

    /**
     * Shows dialog for configuring Untraceable operation on a selected tag.
     */
    private void showUntraceableDialog(TagEntry selectedTag) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Configure Untraceable for " + selectedTag.tagId);

        // Inflate the dialog layout
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_untraceable, null);
        builder.setView(dialogView);

        // Dialog controls
        EditText etPassword = dialogView.findViewById(R.id.etPassword);
        CheckBox cbShowEpc = dialogView.findViewById(R.id.cbShowEpc);
        EditText etEpcLen = dialogView.findViewById(R.id.etEpcLen);
        RadioGroup rgTidOption = dialogView.findViewById(R.id.rgTidOption);
        Spinner spinnerMemoryBank = dialogView.findViewById(R.id.spinnerMemoryBank);
        EditText etTagPattern = dialogView.findViewById(R.id.etTagPattern);
        EditText etTagPatternBitCount = dialogView.findViewById(R.id.etTagPatternBitCount);
        EditText etBitOffset = dialogView.findViewById(R.id.etBitOffset);
        EditText etTagMask = dialogView.findViewById(R.id.etTagMask);
        EditText etTagMaskBitCount = dialogView.findViewById(R.id.etTagMaskBitCount);
        Button btnCancel = dialogView.findViewById(R.id.btnDialogCancel);
        Button btnApply = dialogView.findViewById(R.id.btnDialogApply);

        // Auto-match the tag pattern to the first 2 words (8 hex chars) of the selected EPC.
        String epcPrefixPattern = getFirstTwoWordPattern(selectedTag.tagId);
        if (!epcPrefixPattern.isEmpty()) {
            selectedTag.tagPattern = epcPrefixPattern;
            selectedTag.tagPatternBitCount = epcPrefixPattern.length() * 4;
        }

        // Populate with current tag values
        etPassword.setText(selectedTag.password);
        cbShowEpc.setChecked(selectedTag.showEpc);
        etEpcLen.setText(String.valueOf(selectedTag.epcLen));
        setSpinnerToValue(spinnerMemoryBank, selectedTag.accessFilterMemoryBank);
        etTagPattern.setText(selectedTag.tagPattern);
        etTagPatternBitCount.setText(String.valueOf(selectedTag.tagPatternBitCount));
        etBitOffset.setText(String.valueOf(selectedTag.bitOffset));
        etTagMask.setText(selectedTag.tagMask);
        etTagMaskBitCount.setText(String.valueOf(selectedTag.tagMaskBitCount));

        // Set TID option radio button
        if ("SHOW_TID".equals(selectedTag.tidOption)) {
            rgTidOption.check(R.id.rbShowTid);
        } else if ("SHOW_USER".equals(selectedTag.tidOption)) {
            rgTidOption.check(R.id.rbShowUser);
        } else {
            rgTidOption.check(R.id.rbHideAllTid);
        }

        AlertDialog dialog = builder.create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnApply.setOnClickListener(v -> {
            if (applyUntraceableParameters(selectedTag, etPassword, cbShowEpc, etEpcLen, rgTidOption,
                    spinnerMemoryBank,
                    etTagPattern, etTagPatternBitCount, etBitOffset, etTagMask, etTagMaskBitCount)) {
                Toast.makeText(MainActivity.this, "Applying Untraceable operation...", Toast.LENGTH_SHORT).show();
                rfidHandler.performUntraceableWithParams(selectedTag);
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private String getFirstTwoWordPattern(String epc) {
        if (epc == null) {
            return "";
        }
        String normalized = epc.replaceAll("[^0-9A-Fa-f]", "").toUpperCase();
        int hexCharsForTwoWords = 8;
        if (normalized.length() <= hexCharsForTwoWords) {
            return normalized;
        }
        return normalized.substring(0, hexCharsForTwoWords);
    }

    private void setSpinnerToValue(Spinner spinner, String value) {
        if (spinner.getAdapter() instanceof ArrayAdapter) {
            @SuppressWarnings("unchecked")
            ArrayAdapter<String> adapter = (ArrayAdapter<String>) spinner.getAdapter();
            int position = adapter.getPosition(value);
            if (position >= 0) {
                spinner.setSelection(position);
            }
        }
    }

    private boolean isHex(String value) {
        return value != null && value.matches("^[0-9A-Fa-f]+$");
    }

    /**
     * Validates and applies user input from Untraceable dialog to TagEntry.
     */
    private boolean applyUntraceableParameters(TagEntry tag, EditText etPassword,
            CheckBox cbShowEpc, EditText etEpcLen, RadioGroup rgTidOption, Spinner spinnerMemoryBank,
            EditText etTagPattern, EditText etTagPatternBitCount, EditText etBitOffset,
            EditText etTagMask, EditText etTagMaskBitCount) {

        try {
            String password = etPassword.getText().toString().trim();
            if (password.isEmpty()) {
                Toast.makeText(this, "Password cannot be empty", Toast.LENGTH_SHORT).show();
                return false;
            }
            if (!isHex(password) || password.length() > 8) {
                Toast.makeText(this, "Password must be 1-8 hex chars", Toast.LENGTH_SHORT).show();
                return false;
            }
            tag.password = password;

            tag.showEpc = cbShowEpc.isChecked();

            int epcLen = Integer.parseInt(etEpcLen.getText().toString());
            if (epcLen <= 0 || epcLen > 6) {
                Toast.makeText(this, "EPC Length must be between 1 and 6 words", Toast.LENGTH_SHORT).show();
                return false;
            }
            tag.epcLen = epcLen;

            int selectedTidId = rgTidOption.getCheckedRadioButtonId();
            if (selectedTidId == R.id.rbShowTid) {
                tag.tidOption = "SHOW_TID";
            } else if (selectedTidId == R.id.rbShowUser) {
                tag.tidOption = "SHOW_USER";
            } else {
                tag.tidOption = "HIDE_ALL_TID";
            }

            Object selectedMemoryBank = spinnerMemoryBank.getSelectedItem();
            if (selectedMemoryBank == null) {
                Toast.makeText(this, "Select a memory bank", Toast.LENGTH_SHORT).show();
                return false;
            }
            tag.accessFilterMemoryBank = selectedMemoryBank.toString();

            String pattern = etTagPattern.getText().toString().trim();
            if (pattern.isEmpty()) {
                Toast.makeText(this, "Tag Pattern cannot be empty", Toast.LENGTH_SHORT).show();
                return false;
            }
            if (!isHex(pattern) || (pattern.length() % 2 != 0)) {
                Toast.makeText(this, "Tag Pattern must be even-length hex", Toast.LENGTH_SHORT).show();
                return false;
            }
            tag.tagPattern = pattern;

            int patternBitCount = Integer.parseInt(etTagPatternBitCount.getText().toString());
            if (patternBitCount <= 0 || patternBitCount > 96) {
                Toast.makeText(this, "Tag Pattern Bit Count must be between 1 and 96", Toast.LENGTH_SHORT).show();
                return false;
            }
            if (patternBitCount > pattern.length() * 4) {
                Toast.makeText(this, "Pattern Bit Count exceeds pattern length", Toast.LENGTH_SHORT).show();
                return false;
            }
            tag.tagPatternBitCount = patternBitCount;

            int bitOffset = Integer.parseInt(etBitOffset.getText().toString());
            if (bitOffset < 0) {
                Toast.makeText(this, "Bit Offset cannot be negative", Toast.LENGTH_SHORT).show();
                return false;
            }
            tag.bitOffset = bitOffset;

            String mask = etTagMask.getText().toString().trim();
            if (mask.isEmpty()) {
                Toast.makeText(this, "Tag Mask cannot be empty", Toast.LENGTH_SHORT).show();
                return false;
            }
            if (!isHex(mask) || (mask.length() % 2 != 0)) {
                Toast.makeText(this, "Tag Mask must be even-length hex", Toast.LENGTH_SHORT).show();
                return false;
            }
            tag.tagMask = mask;

            int maskBitCount = Integer.parseInt(etTagMaskBitCount.getText().toString());
            if (maskBitCount <= 0 || maskBitCount > 96) {
                Toast.makeText(this, "Tag Mask Bit Count must be between 1 and 96", Toast.LENGTH_SHORT).show();
                return false;
            }
            if (maskBitCount > mask.length() * 4) {
                Toast.makeText(this, "Mask Bit Count exceeds mask length", Toast.LENGTH_SHORT).show();
                return false;
            }
            tag.tagMaskBitCount = maskBitCount;

            return true;
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid number format", Toast.LENGTH_SHORT).show();
            return false;
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
                holder.tvMemoryInfo = convertView.findViewById(R.id.tvMemoryInfo);
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

                if (entry.memoryBankData != null) {
                    holder.tvMemoryInfo.setVisibility(View.VISIBLE);
                    holder.tvMemoryInfo.setText("Bank: " + entry.memoryBank + " | Data: " + entry.memoryBankData);
                } else {
                    holder.tvMemoryInfo.setVisibility(View.GONE);
                }
            }
            return convertView;
        }

        static class ViewHolder {
            TextView tvTagId, tvMemoryInfo, tvRssi, tvCount;
        }
    }
}

