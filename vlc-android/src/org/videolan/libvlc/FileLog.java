/*****************************************************************************
 * FileLog.java
 *****************************************************************************
 * Copyright © 2013 VLC authors and VideoLAN
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston MA 02110-1301, USA.
 *****************************************************************************/

package org.videolan.libvlc;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.os.Build;
import android.os.Environment;
import android.util.Log;

/**
 * A log written where the user can fetch it without a computer.
 *
 * The failures worth diagnosing here happen before anything is drawn -- a
 * missing or wrong-ABI libvlcjni kills the process straight from a static
 * initializer -- and reading logcat means plugging in adb, which is precisely
 * what is awkward on a headset. The same lines are therefore appended to
 * Download/vlc-dev.log, which any file manager can open.
 *
 * Nothing here is allowed to throw: a logger that breaks the application it is
 * meant to diagnose is worse than no logger at all.
 */
public class FileLog {

    private static final String TAG = "VLC/FileLog";

    public static final String FILENAME = "vlc-dev.log";

    /** Past this size the log is started over, to stay copyable */
    private static final long MAX_SIZE = 512 * 1024;

    /**
     * Written to when the public Download directory cannot be, which is the
     * case as long as the storage permission has not been granted.
     */
    private static File sFallbackDirectory;

    private static File sFile;
    private static boolean sResolved;

    private FileLog() {}

    /**
     * @param directory a directory owned by the application, needing no
     *                  permission, used when the Download one is not writable
     */
    public static synchronized void setFallbackDirectory(File directory) {
        sFallbackDirectory = directory;
        reset();
    }

    /**
     * Look for the destination again, the Download directory having possibly
     * become writable -- which is what granting the storage permission does.
     */
    public static synchronized void reset() {
        sResolved = false;
        sFile = null;
    }

    public static synchronized File getFile() {
        if (sResolved)
            return sFile;
        sResolved = true;
        sFile = null;

        File directory = null;
        try {
            if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()))
                directory = Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS);
        } catch (Exception e) {
            Log.w(TAG, "Cannot reach the Download directory: " + e);
        }

        if (!isUsable(directory))
            directory = sFallbackDirectory;
        if (!isUsable(directory))
            return null;

        File file = new File(directory, FILENAME);
        if (file.exists() && file.length() > MAX_SIZE)
            file.delete();
        sFile = file;
        return sFile;
    }

    private static boolean isUsable(File directory) {
        if (directory == null)
            return false;
        try {
            if (!directory.exists() && !directory.mkdirs())
                return false;
            return directory.canWrite();
        } catch (Exception e) {
            return false;
        }
    }

    public static void log(String message) {
        log(message, null);
    }

    public static synchronized void log(String message, Throwable throwable) {
        Log.i(TAG, message, throwable);

        File file = getFile();
        if (file == null)
            return;

        PrintWriter writer = null;
        try {
            writer = new PrintWriter(new FileWriter(file, true));
            writer.print(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                    .format(new Date()));
            writer.print("  ");
            writer.println(message);
            if (throwable != null)
                throwable.printStackTrace(writer);
            writer.flush();
        } catch (Exception e) {
            Log.w(TAG, "Cannot write to " + file + ": " + e);
        } finally {
            if (writer != null)
                writer.close();
        }
    }

    /**
     * Open a session, with what is needed to tell why the native library did or
     * did not load: the ABIs of the device and the ABI the build was made for.
     */
    public static void logSession(String application) {
        log("--------------------------------------------------");
        log(application);
        log("device: " + Build.MANUFACTURER + " " + Build.MODEL
                + " (" + Build.DEVICE + ")");
        log("android: " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")");
        log("supported ABIs: " + getSupportedAbis());
    }

    @SuppressWarnings("deprecation")
    private static String getSupportedAbis() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            StringBuilder abis = new StringBuilder();
            for (String abi : Build.SUPPORTED_ABIS) {
                if (abis.length() > 0)
                    abis.append(", ");
                abis.append(abi);
            }
            return abis.toString();
        }
        return Build.CPU_ABI + ", " + Build.CPU_ABI2;
    }
}
