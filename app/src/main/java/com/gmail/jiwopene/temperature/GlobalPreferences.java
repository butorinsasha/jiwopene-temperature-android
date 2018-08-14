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

import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.content.Context;


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
}
