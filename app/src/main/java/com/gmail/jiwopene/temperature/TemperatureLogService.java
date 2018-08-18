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

import android.app.AlarmManager;
import android.app.KeyguardManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

import com.gmail.jiwopene.temperature.sensors.Sensor;
import com.gmail.jiwopene.temperature.sensors.SensorStorage;

import java.util.Date;

public class TemperatureLogService extends Service {

    private static final String EXTRA_INSTANCE = "com.gmail.jiwopene.TemperatureLogService.EXTRA_INSTANCE";
    private SensorStorage storage;
    private boolean screenOn = false;
    private AlarmManager alarmManager;
    private GlobalPreferences globalPreferences;
    private BroadcastReceiver receiver;
    private KeyguardManager keyguardManager;
    private TemperatureLog log;

    public TemperatureLogService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        keyguardManager = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        globalPreferences = new GlobalPreferences(this);
        storage = new SensorStorage(this);
        log = new TemperatureLog(this);

        long instance = intent.getLongExtra(EXTRA_INSTANCE, 0);
        Log.d(TemperatureLogService.class.getName(), "Logging");
        if (
                !keyguardManager.inKeyguardRestrictedInputMode() || globalPreferences.getLogWhenLocked()
        ) {
            Date now = new Date();

            for (Sensor sensor : storage.getSensors(true)) {
                double value;

                try {
                    value = sensor.getValue();
                } catch (Exception ignored) {
                    continue;
                }

                log.append(now, sensor.getIdentifier(), value);
            }

            Log.d(TemperatureLogService.class.getName(), "Logging OK");
        }
        else {
            Log.d(TemperatureLogService.class.getName(), "Logging cancelled -- device locked");
        }

        scheduleNext();
        return START_NOT_STICKY;
    }

    private void scheduleNext() {
        Intent intent = new Intent(this, TemperatureLogService.class);
        PendingIntent alarmIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_ONE_SHOT);

        long nextTime = SystemClock.elapsedRealtime() + globalPreferences.getLogInterval();

        int type;
        if (globalPreferences.getLogWakesDevice()) {
            type = AlarmManager.ELAPSED_REALTIME_WAKEUP;
        }
        else {
            type = AlarmManager.ELAPSED_REALTIME;
        }

        if (globalPreferences.getLogEnabledInLowPowerState() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (globalPreferences.getExactLogTimingEnabled() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(type, nextTime, alarmIntent);
            }
            else {
                alarmManager.setAndAllowWhileIdle(type, nextTime, alarmIntent);
            }
        }
        else {
            if (globalPreferences.getExactLogTimingEnabled() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExact(type, nextTime, alarmIntent);
            }
            else {
                alarmManager.set(type, nextTime, alarmIntent);
            }
        }
    }
}
