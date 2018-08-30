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

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.Switch;

import java.util.Locale;

public class TemperatureLogSettingsActivity extends AppCompatActivity implements View.OnClickListener, AdapterView.OnItemSelectedListener {
    private Switch enableSwitch;
    private GlobalPreferences global;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_temperature_log_settings);

        global = new GlobalPreferences(this);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.activity_temperature_log_settings);
        }

        if (global.getLogEnabled()) {
            startService(new Intent(this, TemperatureLogService.class));
        }

        findViewById(R.id.low_power).setOnClickListener(this);
        findViewById(R.id.wakes_device).setOnClickListener(this);
        findViewById(R.id.exact).setOnClickListener(this);
        findViewById(R.id.log_when_locked).setOnClickListener(this);

        TimeHolder[] presets = new TimeHolder[60];
        for (int mins = 1; mins <= 60; mins++)
            presets[mins-1] = new TimeHolder(mins);
        ((Spinner)findViewById(R.id.log_every)).setAdapter(new ArrayAdapter<Object>(this, android.R.layout.simple_dropdown_item_1line, presets));
        ((Spinner)findViewById(R.id.log_every)).setOnItemSelectedListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem switchItem = menu.add(R.string.enable_log);
        switchItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        enableSwitch = new Switch(this);
        switchItem.setActionView(enableSwitch);

        enableSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                global.setLogEnabled(enableSwitch.isChecked());
                if (enableSwitch.isChecked())
                    startService(new Intent(TemperatureLogSettingsActivity.this, TemperatureLogService.class));
            }
        });

        return super.onCreateOptionsMenu(menu);
    }

    private void refreshUI() {
        enableSwitch.setChecked(global.getLogEnabled());

        ((CheckBox)findViewById(R.id.low_power)).setChecked(global.getLogEnabledInLowPowerState());
        ((CheckBox)findViewById(R.id.low_power)).setEnabled(global.isLogEnabledInLowPowerStateModifiable());

        ((CheckBox)findViewById(R.id.exact)).setChecked(global.getExactLogTimingEnabled());
        ((CheckBox)findViewById(R.id.exact)).setEnabled(global.isExactLogTimingEnabledModifiable());

        ((CheckBox)findViewById(R.id.wakes_device)).setChecked(global.getLogWakesDevice());

        ((CheckBox)findViewById(R.id.log_when_locked)).setChecked(global.getLogWhenLocked());

        if (
                (global.getLogEnabledInLowPowerState() && global.isLogEnabledInLowPowerStateModifiable())
                ||
                (global.getExactLogTimingEnabled() && global.isExactLogTimingEnabledModifiable())
        ) {
            findViewById(R.id.battery_warning_new_android).setVisibility(View.VISIBLE);
        }
        else {
            findViewById(R.id.battery_warning_new_android).setVisibility(View.GONE);
        }

        if (
                global.getLogWakesDevice()
                &&
                        global.getLogInterval() <= (5 * 60_000)
        ) {
            findViewById(R.id.battery_warning_too_fast).setVisibility(View.VISIBLE);
        }
        else {
            findViewById(R.id.battery_warning_too_fast).setVisibility(View.GONE);
        }

        ((Spinner)findViewById(R.id.log_every)).setSelection(((int)Math.max(1, Math.min(60, Math.floor(global.getLogInterval() / 60_000d))))-1);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.low_power:
                global.setLogEnabledInLowPowerState(((CheckBox)view).isChecked());
                break;
            case R.id.exact:
                global.setExactLogTimingEnabled(((CheckBox)view).isChecked());
                break;
            case R.id.wakes_device:
                global.setLogWakesDevice(((CheckBox)view).isChecked());
                break;
            case R.id.log_when_locked:
                global.setLogWhenLocked(((CheckBox)view).isChecked());
                break;
        }
        refreshUI();
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        global.setLogInterval(((TimeHolder)adapterView.getSelectedItem()).mins * 60_000);
        refreshUI();
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }

    class TimeHolder {
        int mins;
        TimeHolder(int mins) {
            this.mins = mins;
        }

        public String toString() {
            return String.format(Locale.getDefault(), "%d min", mins);
        }
    }
}
