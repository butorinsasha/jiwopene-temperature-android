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

package com.gmail.jiwopene.temperature;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;


public class GlobalPreferences {
    public static final int UPDATE_INTERVAL_CAN_TAKE_WHILE_LIMIT = 1000;
    protected SharedPreferences preferences;

    public GlobalPreferences(Context context) {
        preferences = context.getSharedPreferences("global", Context.MODE_PRIVATE);
    }

    public static final int UPDATE_INTERVAL_ULTRA_FAST = 100;
    public static final int UPDATE_INTERVAL_FAST = 250;
    public static final int UPDATE_INTERVAL_NORMAL = 500;
    public static final int UPDATE_INTERVAL_SLOW = 1000;

    public int getUpdateInterval() {
        return preferences.getInt("interval", 500);
    }

    public void setUpdateInterval(int interval) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("interval", interval);
        editor.apply();
    }

    public boolean getShowHidden() { return preferences.getBoolean("show_hidden", false); }
    public void setShowHidden(boolean showHidden) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("show_hidden", showHidden);
        editor.apply();
    }

    public boolean getLogEnabled() {
        return preferences.getBoolean("log_enabled", false);
    }
    public void setLogEnabled(boolean enabled) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("log_enabled", enabled);
        editor.apply();
    }

    public long getLogInterval() {
        return preferences.getLong("log_interval", 500);
    }
    public void setLogInterval(int interval) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putLong("log_interval", interval);
        editor.apply();
    }

    public boolean getExactLogTimingEnabled() {
        if (!isExactLogTimingEnabledModifiable()) {
            return true;
        }
        return preferences.getBoolean("log_exact", false);
    }
    public void setExactLogTimingEnabled(boolean exactLogTimingEnabled) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("log_exact", exactLogTimingEnabled);
        editor.apply();
    }
    public boolean isExactLogTimingEnabledModifiable() {
        return !(Build.VERSION.SDK_INT < Build.VERSION_CODES.M);
    }

    public boolean getLogEnabledInLowPowerState() {
        if (!isLogEnabledInLowPowerStateModifiable()) {
            return true;
        }
        return preferences.getBoolean("log_in_low_power", false);
    }
    public void setLogEnabledInLowPowerState(boolean logEnabledInLowPowerState) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("log_in_low_power", logEnabledInLowPowerState);
        editor.apply();
    }
    public boolean isLogEnabledInLowPowerStateModifiable() {
        return !(Build.VERSION.SDK_INT < Build.VERSION_CODES.M);
    }

    public boolean getLogWakesDevice() {
        return preferences.getBoolean("log_wakes_device", false);
    }
    public void setLogWakesDevice(boolean logWakes) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("log_wakes_device", logWakes);
        editor.apply();
    }

    public boolean getLogWhenLocked() {
        return preferences.getBoolean("log_when_locked", true);
    }
    public void setLogWhenLocked(boolean logWhenLocked) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("log_when_locked", logWhenLocked);
        editor.apply();
    }

    public boolean isFirstRun() {
        return preferences.getBoolean("first_run", true);
    }
    public void setFirstRun(boolean firstRun) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("first_run", firstRun);
        editor.apply();
    }
}
