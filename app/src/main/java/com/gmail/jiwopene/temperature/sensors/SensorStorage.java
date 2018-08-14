/*
 * temperature-android
 * Copyright (C) 2018  jiwopene
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * jiwopene@gmail.com
 * https://gitlab.com/jiwopene/temperature-android
 */

package com.gmail.jiwopene.temperature.sensors;

import android.content.SharedPreferences;
import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.ArraySet;
import android.util.Log;

import java.io.File;
import java.io.FileReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class SensorStorage {
    private static final String PREFERENCES = "sensors";
    private SharedPreferences prefs;

    public SensorStorage(Context context) {
        prefs = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
    }

    public Sensor[] getSensors() {
        ArrayList<Sensor> out = new ArrayList<>();

        for (String sensor : prefs.getStringSet("sensors", new android.support.v4.util.ArraySet<String>())) {
            try {
                URI uri = new URI(sensor);
                switch (uri.getScheme()) {
                    case "file":
                        out.add(new SysfsSensor(uri.getPath()));
                }
            }
            catch (Exception ignored) {
            }
        }

        return out.toArray(new Sensor[]{});
    }

    public @Nullable String getSensorComment(@NonNull Uri uri) {
        return prefs.getString(uri.toString(), null);
    }

    public void setSensorComment(@NonNull Uri uri, @Nullable String comment) {
        SharedPreferences.Editor editor = prefs.edit();
        if (comment == null || comment.trim().equals(""))
            editor.remove(uri.toString());
        else
            editor.putString(uri.toString(), comment);
        editor.apply();
    }

    public void removeSensor(@NonNull Uri uri) {
        ArrayList<String> sensors = new ArrayList<>(prefs.getStringSet("sensors", new ArraySet<String>()));
        for (int i = 0; i < sensors.size();)
            if (sensors.get(i).equals(uri.toString()))
                sensors.remove(i);
            else
                i++;
        SharedPreferences.Editor editor = prefs.edit();
        editor.putStringSet("sensors", new ArraySet<>(sensors));
        editor.apply();
    }

    public void addSensor(@NonNull Uri uri) {
        ArrayList<String> sensors = new ArrayList<>(prefs.getStringSet("sensors", new ArraySet<String>()));
        sensors.add(uri.toString());

        SharedPreferences.Editor editor = prefs.edit();
        editor.putStringSet("sensors", new ArraySet<String>(sensors));
        editor.apply();
    }

    public void clear() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putStringSet("sensors", new ArraySet<String>());
        editor.apply();
    }

    public void rescan() {
        clear();

        Log.i("SensorStorage", "Rescanning sensors");
        ArrayList<String> sensors = new ArrayList<>();

        // Scan for /sys/class/thermal sensors
        {
            int failed = 0; // Because of application does not have the right to list files, check if file "temp" exists
            for (int i = 0; failed < 5; i++) {
                String path = String.format(Locale.ROOT, "/sys/class/thermal/thermal_zone%d", i);
                try {
                    FileReader fr = new FileReader(path + "/temp");
                    if (fr.read() != -1) {
                        sensors.add("file://" + path);
                        Log.d("SensorStorage", "Adding "+path+" as sensor");
                        continue;
                    }
                }
                catch (Exception ignored) {
                }
                Log.d("SensorStorage", "Skipping sensor "+path);
                failed++;
            }
        }

        SharedPreferences.Editor editor = prefs.edit();
        editor.putStringSet("sensors", new ArraySet<String>(sensors));
        editor.apply();
    }
}
