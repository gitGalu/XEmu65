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

package info.galu.dev.xemu65.filebrowser;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import java.io.File;
import eu.davidea.fastscroller.FastScroller;
import eu.davidea.flexibleadapter.SelectableAdapter;
import eu.davidea.flexibleadapter.items.IFlexible;
import info.galu.dev.xemu65.Codes;
import info.galu.dev.xemu65.EmuActivity;
import info.galu.dev.xemu65.R;
import info.galu.dev.xemu65.savebrowser.SaveBrowser;
import info.galu.dev.xemu65.util.AnimUtils;
import info.galu.dev.xemu65.util.PermissionUtil;

import static info.galu.dev.xemu65.Codes.BUNDLE_EXTRA_CURRENT_FILE;
import static info.galu.dev.xemu65.Codes.BUNDLE_EXTRA_CURRENT_PATH;
import static info.galu.dev.xemu65.Codes.BUNDLE_EXTRA_EMU_VIEW_HEIGHT;
import static info.galu.dev.xemu65.Codes.BUNDLE_EXTRA_EMU_VIEW_WIDTH;

/**
 * Created by gitGalu on 2017-11-24.
 */

public class FileBrowser extends AppCompatActivity {

    public static final int DEFAULT_WIDTH = 386;
    public static final int DEFAULT_HEIGHT = 240;

    private FlexibleFileAdapter<IFlexible> adapter;

    private int width = DEFAULT_WIDTH;
    private int height = DEFAULT_HEIGHT;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prepareUI();

        PermissionUtil.requestPermissionIfNeeded(this);

        Intent intent = getIntent();
        width = intent.getIntExtra(BUNDLE_EXTRA_EMU_VIEW_WIDTH, DEFAULT_WIDTH);
        height = intent.getIntExtra(BUNDLE_EXTRA_EMU_VIEW_HEIGHT, DEFAULT_HEIGHT);

        prepareBrowser();
    }

    private void prepareUI() {
        setContentView(R.layout.file_browser);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void prepareBrowser() {
        SharedPreferences sp = getSharedPreferences(Codes.SHARED_PREFS_NAME, Context.MODE_PRIVATE);
        String targetDir = sp.getString(Codes.PREF_KEY_LAST_DIR, Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath());

        adapter = new FlexibleFileAdapter<>(this, targetDir);
        adapter.setAnimationOnScrolling(true).setAnimationOnReverseScrolling(true);
        adapter.setMode(SelectableAdapter.Mode.IDLE);

        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.addItemDecoration(new FileDividerItemDecoration(this));
        recyclerView.setAdapter(adapter);

        FastScroller fastScroller = findViewById(R.id.fast_scroller);
        fastScroller.setMinimumScrollThreshold(70);
        fastScroller.setBubbleAndHandleColor(getResources().getColor(R.color.colorPrimary));
        adapter.setFastScroller(fastScroller);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    public void goFile(String filePath) {
        saveCurrentDir(new File(filePath).getParentFile().toString());
        Intent fileIntent = new Intent(this, EmuActivity.class);
        fileIntent.putExtra(Codes.FILE_PATH, filePath);
        setResult(Codes.RESULT_FILE_BROWSER_OK, fileIntent);
        finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    public void goHistory(String path, String file) {
        saveCurrentDir(path);
        Intent intent = new Intent(this, SaveBrowser.class);
        intent.putExtra(BUNDLE_EXTRA_CURRENT_PATH, path);
        intent.putExtra(BUNDLE_EXTRA_CURRENT_FILE, file);
        intent.putExtra(BUNDLE_EXTRA_EMU_VIEW_WIDTH, width);
        intent.putExtra(BUNDLE_EXTRA_EMU_VIEW_HEIGHT, height);
        Bundle bundle = AnimUtils.getActivityTransitionParams(this).toBundle();
        startActivityForResult(intent, Codes.REQUEST_SAVE_BROWSER, bundle);
    }

    private void saveCurrentDir(String filePath) {
        SharedPreferences sp = getSharedPreferences(Codes.SHARED_PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(Codes.PREF_KEY_LAST_DIR, filePath);
        editor.commit();
    }

    @Override
    protected void onResume() {
        super.onResume();
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_files, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case Codes.REQUEST_SAVE_BROWSER:
                if (resultCode == Codes.RESULT_SAVE_BROWSER_OK) {
                    Intent fileIntent = new Intent(this, EmuActivity.class);
                    Bundle resultData = data.getExtras();
                    fileIntent.putExtra(Codes.FILE_PATH, resultData.getString(Codes.FILE_PATH) + "/" + resultData.getString(Codes.FILE_NAME));
                    fileIntent.putExtra(Codes.SAVE_STATE_PATH, resultData.getString(Codes.SAVE_STATE_PATH));
                    setResult(Codes.RESULT_FILE_BROWSER_OK, fileIntent);
                    finish();
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PermissionUtil.REQ_PERM_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result == PackageManager.PERMISSION_DENIED) {
                    allGranted = false;
                    break;
                }
            }

            if (!allGranted) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(R.string.permissions_storage_message)
                        .setTitle(R.string.permissions_storage_title);

                builder.setPositiveButton(R.string.permissions_storage_button_exit, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });

                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialogInterface) {
                        Intent fileIntent = new Intent(getApplicationContext(), EmuActivity.class);
                        setResult(Codes.RESULT_FILE_BROWSER_ERROR, fileIntent);
                        finish();
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                    }
                });


                AlertDialog dialog = builder.create();
                dialog.show();
            } else {
                prepareBrowser();
            }
        }
    }
}
