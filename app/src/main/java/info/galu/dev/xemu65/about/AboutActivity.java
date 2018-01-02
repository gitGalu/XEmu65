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

package info.galu.dev.xemu65.about;

import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import com.mikepenz.aboutlibraries.Libs;
import com.mikepenz.aboutlibraries.LibsBuilder;
import com.mikepenz.aboutlibraries.LibsConfiguration;
import com.mikepenz.aboutlibraries.entity.Library;
import com.mikepenz.aboutlibraries.ui.LibsSupportFragment;
import java.io.Serializable;
import java.util.Comparator;
import info.galu.dev.xemu65.R;
import info.galu.dev.xemu65.util.UIUtils;

/**
 * Created by gitGalu on 2017-12-09.
 */

public class AboutActivity extends AppCompatActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prepareUI();

        prepareAboutInfo();
    }

    @Override
    protected void onResume() {
        super.onResume();

        initUiChaingeListener();
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

    private void prepareAboutInfo() {
        LibsConfiguration.LibsListener libsListener = new LibsConfiguration.LibsListener() {
            @Override
            public void onIconClicked(View v) {
                return;
            }

            @Override
            public boolean onLibraryAuthorClicked(View v, Library library) {
                return false;
            }

            @Override
            public boolean onLibraryContentClicked(View v, Library library) {
                if ("Altirra ROM Set".equals(library.getLibraryName())) {
                    showAltirraLicense();
                }
                return true;
            }

            @Override
            public boolean onLibraryBottomClicked(View v, Library library) {
                return false;
            }

            @Override
            public boolean onExtraClicked(View v, Libs.SpecialButton specialButton) {
                return false;
            }

            @Override
            public boolean onIconLongClicked(View v) {
                return false;
            }

            @Override
            public boolean onLibraryAuthorLongClicked(View v, Library library) {
                return false;
            }

            @Override
            public boolean onLibraryContentLongClicked(View v, Library library) {
                return true;
            }

            @Override
            public boolean onLibraryBottomLongClicked(View v, Library library) {
                return false;
            }
        };

        LibsSupportFragment fragment = new LibsBuilder()
                .withLibraries("atari800", "altirraroms", "appcompat_v7", "design", "support_cardview", "fancyshowcaseview", "flexibleadapter", "android_job")
                .withActivityStyle(Libs.ActivityStyle.DARK)
                .withAboutIconShown(false)
                .withLicenseShown(true)
                .withAutoDetect(false)
                .withSortEnabled(true)
                .withFields(R.string.class.getFields())
                .withLibraryComparator(new A800LibraryComparator())
                .withAboutAppName(getString(R.string.about_app_name))
                .withVersionShown(false)
                .withAboutVersionShown(false)
                .withActivityTitle("XEmu65")
                .withAboutDescription(getString(R.string.about_description))
                .withListener(libsListener)
                .supportFragment();

        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.frame_container, fragment).commit();
    }

    private void showAltirraLicense() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Copyright © 2016 Avery Lee, All Rights Reserved.\n\n" +
                "Copying and distribution of this file, with or without modification, are permitted in any medium without royalty provided the copyright notice and this notice are preserved.  This file is offered as-is, without any warranty.")
                .setTitle(R.string.about_altirra_message_title);

        builder.setPositiveButton("Close", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
            }
        });

        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();

    }

    private void prepareUI() {
        setContentView(R.layout.about);

        UIUtils.disableFullScreen(getWindow());

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_about, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                return true;
            case R.id.action_about_feedback:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(getString(R.string.about_message))
                        .setTitle(R.string.about_message_title);

                builder.setPositiveButton("Rate on Google Play", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        rateOnGooglePlay();
                    }


                });

                builder.setNegativeButton("Contact me", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        contactMe();
                    }
                });

                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialogInterface) {
                        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
                    }
                });

                AlertDialog dialog = builder.create();
                dialog.show();

                break;
        }
        return false;
    }

    private void contactMe() {
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
        emailIntent.setData(Uri.parse(getString(R.string.mailto_developer)));
        try {
            startActivity(emailIntent);
        } catch (ActivityNotFoundException e) {
            // e.g. no email app available
        }

    }

    private void rateOnGooglePlay() {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=" + getPackageName())));
        } catch (android.content.ActivityNotFoundException e) {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("http://play.google.com/store/apps/details?id=" + getPackageName())));
        }
    }

    private static class A800LibraryComparator implements Comparator<Library>, Serializable {
        @Override
        public int compare(Library library, Library t1) {
            if ("Atari800".equals(library.getLibraryName())) {
                return -1;
            }
            if ("Altirra ROM Set".equals(library.getLibraryName())) {
                if ("Atari800".equals(t1.getLibraryName())) {
                    return 1;
                }
                return -1;
            }
            return library.getLibraryName().compareTo(t1.getLibraryName());
        }
    }

}
