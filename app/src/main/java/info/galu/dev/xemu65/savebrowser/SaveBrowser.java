/*
 * Copyright (C) 2017 Michal Galinski
 *
 * This file is part of XEmu65, an Atari 8-bit computer emulator for Android.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
*/

package info.galu.dev.xemu65.savebrowser;

import android.content.Intent;
import android.os.Bundle;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import com.google.android.material.snackbar.Snackbar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Gallery;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import info.galu.dev.xemu65.Codes;
import info.galu.dev.xemu65.EmuActivity;
import info.galu.dev.xemu65.R;
import info.galu.dev.xemu65.util.FileUtils;
import info.galu.dev.xemu65.util.UIUtils;

import static info.galu.dev.xemu65.Codes.BUNDLE_EXTRA_CURRENT_FILE;
import static info.galu.dev.xemu65.Codes.BUNDLE_EXTRA_CURRENT_PATH;
import static info.galu.dev.xemu65.Codes.BUNDLE_EXTRA_EMU_VIEW_HEIGHT;
import static info.galu.dev.xemu65.Codes.BUNDLE_EXTRA_EMU_VIEW_WIDTH;

/**
 * Created by gitGalu on 2017-11-23.
 */
//TODO replace Gallery widget with RecyclerView-compatible view
public class SaveBrowser extends AppCompatActivity {

    public static final int DEFAULT_WIDTH = 386;
    public static final int DEFAULT_HEIGHT = 240;

    private CoordinatorLayout coordinatorLayout;
    private SaveAdapter saveAdapter;
    private List<SaveWrapper> items;
    private int selectedItem = 0;

    private String currentPath;
    private String currentFile;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prepareUI();

        Intent intent = getIntent();
        currentPath = intent.getStringExtra(BUNDLE_EXTRA_CURRENT_PATH);
        currentFile = intent.getStringExtra(BUNDLE_EXTRA_CURRENT_FILE);
        int width = intent.getIntExtra(BUNDLE_EXTRA_EMU_VIEW_WIDTH, DEFAULT_WIDTH);
        int height = intent.getIntExtra(BUNDLE_EXTRA_EMU_VIEW_HEIGHT, DEFAULT_HEIGHT);

        prepareFiles(currentPath, currentFile);

        prepareGallery(width, height);
    }

    private void prepareUI() {
        setContentView(R.layout.save_browser);
        UIUtils.disableFullScreen(getWindow());
        coordinatorLayout = findViewById(R.id.coordinator_save_browser);
    }

    private void prepareFiles(String currentPath, String currentFile) {
        File[] foundFiles = FileUtils.getSaveStateFiles(currentPath, currentFile);

        String tmpDirName = "" + System.currentTimeMillis();
        File tmpDir = new File(getCacheDir(), tmpDirName);
        tmpDir.mkdir();

        items = new ArrayList();
        for (File file : foundFiles) {
            String dst = tmpDir + "/" + UUID.randomUUID().toString();
            File tmpDir2 = new File(dst);
            tmpDir2.mkdir();
            FileUtils.unZip(dst, file);
            items.add(new SaveWrapper(dst, "", file));
        }

        Collections.sort(items, new SaveWrapperComparator());
    }

    private void prepareGallery(int width, int height) {
        Gallery gallery = findViewById(R.id.saveGallery);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        saveAdapter = new SaveAdapter(this, items, width, height);
        gallery.setAdapter(saveAdapter);

        gallery.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            public void onItemSelected(AdapterView<?> arg0, View arg1, int pos, long arg3) {
                selectedItem = pos;
            }

            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_saves, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                homeActionPerformed();
                return false;
            case R.id.action_save_load:
                saveLoadActionPerformed();
                return false;
            case R.id.action_save_delete:
                saveDeleteActionPerformed();
                return false;
            default:
                return false;
        }
    }

    private void homeActionPerformed() {
        finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private void saveLoadActionPerformed() {
        if (selectedItem >= 0 && items.size() > selectedItem) {
            SaveWrapper saveWrapper = items.get(selectedItem);
            Intent restoreIntent = new Intent(this, EmuActivity.class);
            restoreIntent.putExtra(Codes.FILE_PATH, currentPath);
            restoreIntent.putExtra(Codes.FILE_NAME, currentFile);
            restoreIntent.putExtra(Codes.SAVE_STATE_PATH, saveWrapper.getBitmapSrc());
            setResult(Codes.RESULT_SAVE_BROWSER_OK, restoreIntent);
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            return;
        }
        setResult(Codes.RESULT_SAVE_BROWSER_CANCEL);
        finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private void saveDeleteActionPerformed() {
        if (selectedItem >= 0 && items.size() > selectedItem) {
            final SaveWrapper saveWrapper = items.get(selectedItem);
            if (!saveWrapper.isToDelete()) {
                saveWrapper.setToDelete(true);
                saveAdapter.notifyDataSetChanged();
                Snackbar snackbar = Snackbar
                        .make(coordinatorLayout, R.string.save_snack_message, Snackbar.LENGTH_LONG)
                        .setAction(R.string.save_snack_undo, new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                saveWrapper.setToDelete(false);
                                saveAdapter.notifyDataSetChanged();
                            }
                        });
                snackbar.setActionTextColor(getResources().getColor(R.color.colorPrimaryDark));
                View sbView = snackbar.getView();
                sbView.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
                snackbar.show();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        pendingDeleteSaves();
    }

    private void pendingDeleteSaves() {
        for (SaveWrapper item : items) {
            if (item.isToDelete()) {
                item.getSourceFile().delete();
            }
        }
    }

    private class SaveWrapperComparator implements Comparator<SaveWrapper> {
        @Override
        public int compare(SaveWrapper t1, SaveWrapper t2) {
            // reverse order
            return t2.getSourceFile().compareTo(t1.getSourceFile());
        }
    }
}