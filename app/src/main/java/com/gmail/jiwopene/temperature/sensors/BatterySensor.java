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

import android.content.Context;

import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.BatteryManager;

public class BatterySensor implements Sensor {
    private final Context context;

    public BatterySensor(Context context) {
        this.context = context;
    }

    @Override
    public float getValue() throws CannotReadSensorException {
        Intent intent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int temperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -300);
        if (temperature == -300) {
            throw new CannotReadSensorException();
        }
        return (float)(temperature) / 10f;
    }

    @Override
    public String getName() throws CannotReadSensorException {
        return "Battery";
    }

    @Override
    public Uri getIdentifier() {
        return Uri.parse("android:///battery");
    }
}
