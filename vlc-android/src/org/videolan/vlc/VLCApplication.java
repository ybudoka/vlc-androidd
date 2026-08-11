/*****************************************************************************
 * VLCApplication.java
 *****************************************************************************
 * Copyright © 2010-2012 VLC authors and VideoLAN
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston MA 02110-1301, USA.
 *****************************************************************************/
package org.videolan.vlc;

import java.io.File;
import java.util.Locale;

import org.videolan.libvlc.FileLog;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

public class VLCApplication extends Application {
    public final static String TAG = "VLC/VLCApplication";
    private static VLCApplication instance;

    public final static String SLEEP_INTENT = "org.videolan.vlc.SleepIntent";

    @Override
    public void onCreate() {
        super.onCreate();

        // Are we using advanced debugging - locale?
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        String p = pref.getString("set_locale", "");
        if (p != null && !p.equals("")) {
            Locale locale;
            // workaround due to region code
            if(p.equals("zh-TW")) {
                locale = Locale.TRADITIONAL_CHINESE;
            } else if(p.startsWith("zh")) {
                locale = Locale.CHINA;
            } else if(p.equals("pt-BR")) {
                locale = new Locale("pt", "BR");
            } else if(p.equals("bn-IN") || p.startsWith("bn")) {
                locale = new Locale("bn", "IN");
            } else {
                /**
                 * Avoid a crash of
                 * java.lang.AssertionError: couldn't initialize LocaleData for locale
                 * if the user enters nonsensical region codes.
                 */
                if(p.contains("-"))
                    p = p.substring(0, p.indexOf('-'));
                locale = new Locale(p);
            }
            Locale.setDefault(locale);
            Configuration config = new Configuration();
            config.locale = locale;
            getBaseContext().getResources().updateConfiguration(config,
                    getBaseContext().getResources().getDisplayMetrics());
        }

        instance = this;

        startFileLog();

        // Initialize the database soon enough to avoid any race condition and crash
        MediaDatabase.getInstance(this);
    }

    /**
     * Open the log the user can read from the device itself, and record what
     * tells apart a package that cannot run here from one that can: the native
     * libraries it ships, and the ABIs this device accepts.
     */
    private void startFileLog() {
        /* Used as long as the storage permission has not been granted, since
         * the public Download directory is not writable before that. */
        FileLog.setFallbackDirectory(getExternalFilesDir(null));

        String version = "unknown version";
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            version = info.versionName + " (" + info.versionCode + ")";
        } catch (NameNotFoundException e) {
            Log.e(TAG, "package info not found.");
        }
        FileLog.logSession(getPackageName() + " " + version);

        File libraryDir = new File(getApplicationInfo().nativeLibraryDir);
        String[] libraries = libraryDir.list();
        if (libraries == null || libraries.length == 0)
            FileLog.log("no native library in " + libraryDir
                    + ": libvlc has not been built into this package");
        else
            FileLog.log("native libraries in " + libraryDir + ": "
                    + TextUtils.join(", ", libraries));

        File log = FileLog.getFile();
        Log.i(TAG, "Logging to " + (log != null ? log.getPath() : "logcat only"));
    }

    /**
     * Called when the overall system is running low on memory
     */
    @Override
    public void onLowMemory() {
        super.onLowMemory();
        Log.w(TAG, "System is running low on memory");

        BitmapCache.getInstance().clear();
    }

    /**
     * @return the main context of the Application
     */
    public static Context getAppContext()
    {
        return instance;
    }

    /**
     * @return the main resources from the Application
     */
    public static Resources getAppResources()
    {
        if(instance == null) return null;
        return instance.getResources();
    }
}
