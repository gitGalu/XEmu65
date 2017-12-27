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

package info.galu.dev.xemu65;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Pair;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ViewFlipper;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;

import info.galu.dev.xemu65.about.AboutActivity;
import info.galu.dev.xemu65.filebrowser.FileBrowser;
import info.galu.dev.xemu65.machineconfig.MachineConfig;
import info.galu.dev.xemu65.savebrowser.SaveBrowser;
import info.galu.dev.xemu65.util.AnimUtils;
import info.galu.dev.xemu65.util.FileUtils;
import info.galu.dev.xemu65.util.ConfigUtils;
import info.galu.dev.xemu65.qj.DigitalJoyCallback;
import info.galu.dev.xemu65.qj.DigitalJoyDirection;
import info.galu.dev.xemu65.qj.DigitalJoyState;
import info.galu.dev.xemu65.qj.QuickJoyImpl;
import me.toptas.fancyshowcase.FancyShowCaseQueue;
import me.toptas.fancyshowcase.FancyShowCaseView;

import static info.galu.dev.xemu65.Codes.BUNDLE_EXTRA_CURRENT_FILE;
import static info.galu.dev.xemu65.Codes.BUNDLE_EXTRA_CURRENT_PATH;
import static info.galu.dev.xemu65.Codes.BUNDLE_EXTRA_EMU_VIEW_HEIGHT;
import static info.galu.dev.xemu65.Codes.BUNDLE_EXTRA_EMU_VIEW_WIDTH;

/**
 * Created by gitGalu on 2017-11-07.
 */
public class EmuActivity extends AppCompatActivity implements DigitalJoyCallback {

    private static final String TAG = "XEmu65";

    public static String coreVersion;

    private String currentPath;
    private String currentFile;

    static {
        System.loadLibrary("atari800");
        coreVersion = NativeInit();
    }

    private final int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;

    private EmuView emuView;

    private GestureDetector gestureDetector;

    private ViewFlipper joystickViewFlipper;
    private BottomNavigationView bottombar;
    private ImageButton fsButton;
    private Toolbar toolbar;
    private EmuAudio audioThread;
    private InputMethodManager imeManager;
    private Vibrator hapticFeedback;
    private QuickJoyImpl quickJoy;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        Intent intent = getIntent();

        hapticFeedback = (Vibrator) getSystemService(getApplicationContext().VIBRATOR_SERVICE);

        if (emuView == null) {
            init(intent);
        }
    }

    private void init(Intent intent) {
        Uri uriToFile;

        setContentView(R.layout.emulation);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        joystickViewFlipper = findViewById(R.id.joystickflipper);

        fsButton = findViewById(R.id.fullScreenButton);
        fsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (getSupportActionBar().isShowing()) {
                    fsButton.setImageResource(R.drawable.ic_fullscreen_exit_white_24dp);
                } else {
                    fsButton.setImageResource(R.drawable.ic_fullscreen_white_24dp);
                }
                toggleToolbar();
                dismissOsdKeyboard();
            }
        });

        quickJoy = findViewById(R.id.quickjoy);
        quickJoy.configure(this, hapticFeedback);

        emuView = findViewById(R.id.emuView);
        emuView.setFocusableInTouchMode(true);
        emuView.setFocusable(true);
        emuView.requestFocus();
        emuView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismissOsdKeyboard();
            }
        });

        bottombar = findViewById(R.id.bottombar);

        prepareKbMenu();

        if (!FileUtils.copyAssetFolderToDataPath(getAssets(), "roms",
                getApplicationContext().getFilesDir().getPath())) {
            Log.e(TAG, "Fatal error: Cannot copy ROM files.");
        }

        imeManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        nativeInit();

        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            uriToFile = intent.getData();
            loadGame(uriToFile.getPath());
            quickJoy.attract();
        }
    }

    private static class ButtonSpec {
        private String label;
        private int drawableId;
        private int key;
        private int width;
        private boolean isSinglePressListenerMode;

        public ButtonSpec(String label, int key, int width, boolean isSinglePressListenerMode) {
            this.label = label;
            this.key = key;
            this.width = width;
            this.isSinglePressListenerMode = isSinglePressListenerMode;
        }

        public ButtonSpec(int drawableId, int key, int width) {
            this.drawableId = drawableId;
            this.key = key;
            this.width = width;
        }
    }

    private void nativeInit() {
        NativeSetROMPath(getApplicationContext().getFilesDir().getPath());

        NativePrefMachine(7, false); // default 64KB

        NativePrefSound(44100, 100, true, true, false);

        soundInit(false);

        NativePrefGfx(2, false, 0, 0, true, 336, 240);
    }

    private void loadGame(String path) {
        this.currentPath = new File(path).getParent();
        this.currentFile = new File(path).getName();
        NativeRunAtariProgram(path, 1, 1);
    }


    @Override
    public void onPause() {
        pauseEmulation(true);
        super.onPause();
    }

    @Override
    public void onResume() {
        pauseEmulation(false);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        super.onResume();
        initUiChaingeListener();
    }

    @Override
    public void onDestroy() {
        if (audioThread != null) audioThread.interrupt();
        super.onDestroy();
        if (isFinishing()) {
            NativeExit();
            android.os.Process.killProcess(android.os.Process.myPid());
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
    }

    public void pauseEmulation(boolean pause) {
        if (pause) {
            if (audioThread != null) {
                audioThread.pause(pause);
            }
            if (emuView != null) {
                emuView.pause(pause);
            }
        } else {
            if (emuView != null) {
                emuView.pause(pause);
            }
            if (audioThread != null) {
                audioThread.pause(pause);
            }
        }
    }

    private void soundInit(boolean n) {
        if (audioThread != null) {
            audioThread.interrupt();
        }
        audioThread = new EmuAudio();
        audioThread.start();
    }

    @Override
    public void stateChanged(DigitalJoyDirection direction, DigitalJoyState state) {
        int intState = (state == DigitalJoyState.ENTER) ? 1 : 0;
        int intDirection = -1;
        switch (direction) {
            case START:
                if (state == DigitalJoyState.ENTER) {
                    NativeSpecial(0);
                } else {
                    NativeSpecial(-1);
                }
                return;
            case FIRE_1:
                intDirection = 0;
                break;
            case LEFT:
                intDirection = 1;
                break;
            case RIGHT:
                intDirection = 2;
                break;
            case UP:
                intDirection = 3;
                break;
            case DOWN:
                intDirection = 4;
        }

        if (intDirection >= 0) {
            NativeJoy(intDirection, intState);
        }
    }


    private boolean saveState(boolean quick) {
        if (currentPath == null) {
            return false;
        }

        File cacheDir = getCacheDir();
        String tmpDirName = "" + System.currentTimeMillis();

        File tmpDir = new File(getCacheDir(), tmpDirName);
        tmpDir.mkdir();

        NativeScreenShot(cacheDir.getPath() + '/' + tmpDirName + '/' + "screen.png");
        NativeSaveState(cacheDir.getPath() + '/' + tmpDirName + '/' + "save.sav");

        File[] files = tmpDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.isFile();
            }
        });

        String targetPath = currentPath + "/" + currentFile + "." + tmpDirName + ".a8sav";

        FileUtils.zip(targetPath, files);

        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_emulation, menu);
        return true;
    }


    private Pair<Integer, Integer> measureTargetDimensions() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            float compensation = 10f / 12f;
            return new Pair<>(Math.round(compensation * emuView.getRenderer().w), Math.round(compensation * emuView.getRenderer().h));
        } else {
            float realWidth = 384f;
            float realHeight = 240f;
            int emuViewHeight = emuView.getHeight();
            float scaleY = emuView.getScaleY();
            float targetHeight = scaleY * emuViewHeight; //0.6*720 = 432
            float scaleX = (targetHeight / realHeight);//432/240
            float compensation = 11f / 12f;
            float targetWidth = compensation * scaleX * realWidth;
            return new Pair<>(Math.round(targetWidth), Math.round(targetHeight));
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_file:
                dismissOsdKeyboard();
                fileActionPerformed();
                return true;
            case R.id.action_keys:
                if (bottombar.isShown()) {
                    quickJoy.showJoy(true);
                    bottombar.setVisibility(View.GONE);
                } else {
                    quickJoy.showJoy(false);
                    bottombar.setVisibility(View.VISIBLE);
                }
                return true;
            case R.id.action_history:
                dismissOsdKeyboard();
                historyActionPerformed();
                return false;
            case R.id.action_save:
                dismissOsdKeyboard();
                saveState(false);
                return true;
            case R.id.action_about:
                dismissOsdKeyboard();
                aboutActionPerformed();
                return true;
            case R.id.action_help:
                dismissOsdKeyboard();
                tutorial();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void aboutActionPerformed() {
        Intent aboutIntent = new Intent(this, AboutActivity.class);
        Bundle bundle = AnimUtils.getActivityTransitionParams(this).toBundle();
        startActivity(aboutIntent, bundle);
    }

    private void historyActionPerformed() {
        if (currentPath != null & currentFile != null) {
            File[] saveStateFiles = FileUtils.getSaveStateFiles(currentPath, currentFile);
            if (null != saveStateFiles && (saveStateFiles.length > 0)) {
                Intent intent = new Intent(this, SaveBrowser.class);
                intent.putExtra(BUNDLE_EXTRA_CURRENT_PATH, currentPath);
                intent.putExtra(BUNDLE_EXTRA_CURRENT_FILE, currentFile);
                Pair<Integer, Integer> dimens = measureTargetDimensions();
                intent.putExtra(BUNDLE_EXTRA_EMU_VIEW_WIDTH, dimens.first);
                intent.putExtra(BUNDLE_EXTRA_EMU_VIEW_HEIGHT, dimens.second);
                Bundle bundle = AnimUtils.getActivityTransitionParams(this).toBundle();
                startActivityForResult(intent, Codes.REQUEST_SAVE_BROWSER, bundle);
                return;
            }
        }
    }

    private void fileActionPerformed() {
        Intent fileIntent = new Intent(this, FileBrowser.class);
        Pair<Integer, Integer> dimens = measureTargetDimensions();
        fileIntent.putExtra(BUNDLE_EXTRA_EMU_VIEW_WIDTH, dimens.first);
        fileIntent.putExtra(BUNDLE_EXTRA_EMU_VIEW_HEIGHT, dimens.second);
        Bundle bundle = AnimUtils.getActivityTransitionParams(this).toBundle();
        startActivityForResult(fileIntent, Codes.REQUEST_FILE_BROWSER, bundle);
    }

    private void toggleToolbar() {
        if (!getSupportActionBar().isShowing()) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

            getSupportActionBar().show();
        } else {
            quickJoy.attract();

            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            getSupportActionBar().hide();
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        dismissOsdKeyboard();
        switch (requestCode) {
            case Codes.REQUEST_FILE_BROWSER:
                if (resultCode == Codes.RESULT_FILE_BROWSER_OK) {
                    String filePath = data.getStringExtra(Codes.FILE_PATH);
                    String savePath = data.getStringExtra(Codes.SAVE_STATE_PATH);
                    String fileName = new File(filePath).getName();
                    MachineConfig cfg = ConfigUtils.guessMachineConfig(fileName);
                    NativePrefEmulation(!cfg.getBasicConfig().isBasicRequired(), false, false, false, false);
                    NativePrefMachine(cfg.getMemConfig().getNum(), false);
                    loadGame(filePath);
                    if (savePath != null) {
                        String saveStatePath = FileUtils.formatSaveFileName(savePath);
                        NativeLoadState(saveStatePath);
                    }
                }
                break;
            case Codes.REQUEST_SAVE_BROWSER:
                if (resultCode == Codes.RESULT_SAVE_BROWSER_OK) {
                    String savePath = data.getStringExtra(Codes.SAVE_STATE_PATH);
                    String saveStatePath = FileUtils.formatSaveFileName(savePath);
                    NativeLoadState(saveStatePath);
                }
                break;
        }
    }

    public void initUiChaingeListener() {
        final View decorView = getWindow().getDecorView();
        decorView.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int visibility) {
                decorView.setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            }
        });
    }

    private void tutorial() {
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

        final Animation enterAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_in_top);
        final Animation joyAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_in_top);

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        Point screenSize = new Point();
        getWindowManager().getDefaultDisplay().getRealSize(screenSize);
        int height = metrics.heightPixels;
        int width = screenSize.x;

        final FancyShowCaseView tutorialJoystick = new FancyShowCaseView.Builder(this)
                .focusRectAtPosition(width / 2, 3 * height / 4, (width) - 24, (height / 2) - 24)
                .fitSystemWindows(true)
                .focusBorderColor(Color.RED)
                .enterAnimation(joyAnimation)
                .focusBorderSize(3)
                .titleGravity(Gravity.BOTTOM | Gravity.CENTER)
                .title(getString(R.string.tutorial_caption_joystick))
                .build();

        joyAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                quickJoy.attract();
            }

            @Override
            public void onAnimationEnd(Animation animation) {
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });

        final FancyShowCaseView tutorialFiles = new FancyShowCaseView.Builder(this)
                .focusOn(findViewById(R.id.action_file))
                .fitSystemWindows(true)
                .focusBorderColor(Color.RED)
                .focusBorderSize(3)
                .enterAnimation(enterAnimation)
                .title(getString(R.string.tutorial_caption_load_game))
                .build();

        final FancyShowCaseView tutorialHistory = new FancyShowCaseView.Builder(this)
                .focusOn(findViewById(R.id.action_history))
                .fitSystemWindows(true)
                .focusBorderColor(Color.RED)
                .focusBorderSize(3)
                .title(getString(R.string.tutorial_caption_load_state))
                .build();

        final FancyShowCaseView tutorialSave = new FancyShowCaseView.Builder(this)
                .focusOn(findViewById(R.id.action_save))
                .fitSystemWindows(true)
                .focusBorderColor(Color.RED)
                .focusBorderSize(3)
                .title(getString(R.string.tutorial_caption_save_state))
                .build();

        final FancyShowCaseView tutorialKeys = new FancyShowCaseView.Builder(this)
                .focusOn(findViewById(R.id.action_keys))
                .fitSystemWindows(true)
                .focusBorderColor(Color.RED)
                .focusBorderSize(3)
                .title(getString(R.string.tutorial_caption_keys))
                .build();

        final FancyShowCaseView tutorialFullscreen = new FancyShowCaseView.Builder(this)
                .focusOn(findViewById(R.id.fullScreenButton))
                .fitSystemWindows(true)
                .focusBorderColor(Color.RED)
                .focusBorderSize(3)
                .title(getString(R.string.tutorial_caption_fullscreen))
                .build();

        FancyShowCaseQueue tutorialQueue = new FancyShowCaseQueue()
                .add(tutorialFiles)
                .add(tutorialKeys)
                .add(tutorialHistory)
                .add(tutorialSave)
                .add(tutorialFullscreen)
                .add(tutorialJoystick);

        tutorialQueue.show();
    }

    private class OnScreenKeyboardListener implements View.OnTouchListener {
        private ButtonSpec sp;

        public OnScreenKeyboardListener(ButtonSpec spec) {
            this.sp = spec;
        }

        @Override
        public boolean onTouch(View view, MotionEvent event) {
            if (gestureDetector.onTouchEvent(event)) {
                NativeKeySinglePress(sp.key);
                view.setPressed(true);
                view.setPressed(false);
                return true;
            } else {
                return true;
            }
        }
    }

    private void dismissOsdKeyboard() {
        if (bottombar.isShown()) {
            quickJoy.showJoy(true);
            bottombar.setVisibility(View.GONE);
        }
    }

    private class SingleTapConfirm extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onSingleTapUp(MotionEvent event) {
            return true;
        }
    }

    private void prepareKbMenu() {
        LinearLayout keyboardLl = findViewById(R.id.keyboardLl);

        List<ButtonSpec> buttons = new ArrayList();
        buttons.add(new ButtonSpec("SELECT", 1, 68, false));
        buttons.add(new ButtonSpec("OPTION", 2, 68, false));
        buttons.add(new ButtonSpec("Esc", '~', 68, true));
        buttons.add(new ButtonSpec("SPACE", ' ', 68, true));
        buttons.add(new ButtonSpec("RETURN", '\n', 68, true));
        buttons.add(new ButtonSpec("0", '0', 42, true));
        buttons.add(new ButtonSpec("1", '1', 42, true));
        buttons.add(new ButtonSpec("2", '2', 42, true));
        buttons.add(new ButtonSpec("3", '3', 42, true));
        buttons.add(new ButtonSpec("4", '4', 42, true));
        buttons.add(new ButtonSpec("5", '5', 42, true));
        buttons.add(new ButtonSpec("6", '6', 42, true));
        buttons.add(new ButtonSpec("7", '7', 42, true));
        buttons.add(new ButtonSpec("8", '8', 42, true));
        buttons.add(new ButtonSpec("9", '9', 42, true));
        buttons.add(new ButtonSpec("A", 'A', 42, true));
        buttons.add(new ButtonSpec("B", 'B', 42, true));
        buttons.add(new ButtonSpec("C", 'C', 42, true));
        buttons.add(new ButtonSpec("D", 'D', 42, true));
        buttons.add(new ButtonSpec("E", 'E', 42, true));
        buttons.add(new ButtonSpec("F", 'F', 42, true));
        buttons.add(new ButtonSpec("G", 'G', 42, true));
        buttons.add(new ButtonSpec("H", 'H', 42, true));
        buttons.add(new ButtonSpec("I", 'I', 42, true));
        buttons.add(new ButtonSpec("J", 'J', 42, true));
        buttons.add(new ButtonSpec("K", 'K', 42, true));
        buttons.add(new ButtonSpec("L", 'L', 42, true));
        buttons.add(new ButtonSpec("M", 'M', 42, true));
        buttons.add(new ButtonSpec("N", 'N', 42, true));
        buttons.add(new ButtonSpec("O", 'O', 42, true));
        buttons.add(new ButtonSpec("P", 'P', 42, true));
        buttons.add(new ButtonSpec("Q", 'Q', 42, true));
        buttons.add(new ButtonSpec("R", 'R', 42, true));
        buttons.add(new ButtonSpec("S", 'S', 42, true));
        buttons.add(new ButtonSpec("T", 'T', 42, true));
        buttons.add(new ButtonSpec("U", 'U', 42, true));
        buttons.add(new ButtonSpec("V", 'V', 42, true));
        buttons.add(new ButtonSpec("W", 'W', 42, true));
        buttons.add(new ButtonSpec("X", 'X', 42, true));
        buttons.add(new ButtonSpec("Y", 'Y', 42, true));
        buttons.add(new ButtonSpec("Z", 'Z', 42, true));
        buttons.add(new ButtonSpec(R.drawable.ic_arrow_back_white_24dp, 252, 60));
        buttons.add(new ButtonSpec(R.drawable.ic_arrow_forward_white_24dp, 251, 60));
        buttons.add(new ButtonSpec(R.drawable.ic_arrow_upward_white_24dp, 254, 60));
        buttons.add(new ButtonSpec(R.drawable.ic_arrow_downward_white_24dp, 253, 60));
        TypedValue outValue = new TypedValue();
        getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);

        gestureDetector = new GestureDetector(this, new SingleTapConfirm());

        for (final ButtonSpec sp : buttons) {
            int width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, sp.width, getResources().getDisplayMetrics());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(width, LinearLayout.LayoutParams.WRAP_CONTENT);
            if (sp.label != null) {
                Button b = new Button(this);
                b.setBackgroundResource(outValue.resourceId);
                b.setText(sp.label);
                b.setTypeface(null, Typeface.BOLD);
                b.setPadding(0, 0, 0, 0);
                if (sp.isSinglePressListenerMode) {
                    b.setOnTouchListener(new OnScreenKeyboardListener(sp));
                } else {
                    b.setOnTouchListener(new View.OnTouchListener() {
                        @Override
                        public boolean onTouch(View view, MotionEvent motionEvent) {
                            if (gestureDetector.onTouchEvent(motionEvent)) {
                                view.setPressed(true);
                                NativeSpecial(sp.key);
                                view.setPressed(false);
                                return true;
                            } else {
                                view.setPressed(false);
                                NativeSpecial(-1);
                                return false;
                            }
                        }
                    });
                }
                keyboardLl.addView(b, lp);
            } else {
                ImageButton b = new ImageButton(this);
                b.setBackgroundResource(outValue.resourceId);
                b.setImageResource(sp.drawableId);
                b.setOnTouchListener(new OnScreenKeyboardListener(sp));
                keyboardLl.addView(b, lp);
            }
        }
    }

    private native int NativeRunAtariProgram(String img, int drive, int reboot);

    private native void NativeExit();

    private static native String NativeInit();

    private static native void NativePrefGfx(int aspect, boolean bilinear, int artifact,
                                             int frameskip, boolean collisions, int crophoriz, int cropvert);

    private static native boolean NativePrefMachine(int machine, boolean ntsc);

    private static native void NativePrefEmulation(boolean basic, boolean speed, boolean disk,
                                                   boolean sector, boolean browser);

    private static native void NativePrefSound(int mixrate, int mixbufsizems, boolean sound16bit, boolean hqpokey,
                                               boolean disableOSL);

    private static native boolean NativeSetROMPath(String path);

    private native void NativeSpecial(int key);

    private native void NativeKey(int key, int s);

    private native void NativeKeySinglePress(int key);

    private native void NativeJoy(int direction, int state);

    private native boolean NativeSaveState(String fileName);

    private native boolean NativeLoadState(String fileName);

    private native boolean NativeScreenShot(String fileName);

}
