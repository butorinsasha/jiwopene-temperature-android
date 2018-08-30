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

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.gmail.jiwopene.temperature.sensors.Sensor;
import com.gmail.jiwopene.temperature.sensors.SensorStorage;

import java.util.Locale;

public class SensorActivity extends AppCompatActivity {

    public static final String EXTRA_SENSOR_URI = "com.gmail.jiwopene.temperature.SensorActivity.SENSOR";
    private Sensor sensor;
    private SensorStorage storage;
    private GlobalPreferences globalPreferences;
    private TextView tv_name;
    private TextView tv_description;
    private TextView tv_temperature;
    private IntervalSubmenu intervalSubmenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.globalPreferences = new GlobalPreferences(this);

        storage = new SensorStorage(this);
        for (Sensor sensor : storage.getSensors(true)) {
            if (sensor.getIdentifier().toString().equals(getIntent().getStringExtra(EXTRA_SENSOR_URI))) {
                this.sensor = sensor;
                break;
            }
        }

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.activity_sensor_details);
        }

        if (sensor != null) {
            setContentView(R.layout.activity_sensor);

            this.tv_name = findViewById(R.id.name);
            this.tv_description = findViewById(R.id.description);
            this.tv_temperature = findViewById(R.id.temperature);
        }
        else {
            TextView message = new TextView(this);
            message.setText(R.string.sensor_not_found);
            setContentView(message);
        }
    }

    private Handler updateTimerHandler = new Handler();
    private Runnable updateTimer = new Runnable() {
        @Override
        public void run() {
            update();

            updateTimerHandler.postDelayed(this, globalPreferences.getUpdateInterval());
        }
    };

    @Override
    protected void onPause() {
        super.onPause();
        updateTimerHandler.removeCallbacks(updateTimer);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateTimerHandler.postDelayed(updateTimer, 0);
    }

    private void update() {
        try {
            tv_name.setText(sensor.getName());
        }
        catch (Exception e) {
            tv_name.setText(R.string.unknown_sensor);
        }

        tv_description.setText(R.string.no_description);
        try {
            String text = storage.getSensorComment(sensor.getIdentifier());
            if (text != null)
                tv_description.setText(text);
        }
        catch (Exception e) {
        }

        try {
            tv_temperature.setText(String.format(Locale.getDefault(), "%.2f °C", sensor.getValue()));
        }
        catch (Exception e) {
            tv_temperature.setText(R.string.no_temperature);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        intervalSubmenu = new IntervalSubmenu(this) {
            @Override
            public void invalidateOptionsMenu() {
                SensorActivity.this.invalidateOptionsMenu();
            }
        };
        intervalSubmenu.addToMenu(menu, getMenuInflater());
        getMenuInflater().inflate(R.menu.sensor_activity_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        intervalSubmenu.onPrepareOptionsMenu(menu);

        menu.findItem(R.id.is_hidden).setChecked(storage.isSensorHidden(sensor.getIdentifier()));

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (intervalSubmenu.onOptionsItemSelected(item))
            return true;
        switch (item.getItemId()) {
            case R.id.edit_description:
                final EditText editText = new EditText(this);
                editText.setText(storage.getSensorComment(sensor.getIdentifier()));
                new AlertDialog.Builder(this)
                        .setTitle(R.string.edit_description)
                        .setView(editText)
                        .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.cancel();
                            }
                        })
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //dialogInterface.dismiss();
                                storage.setSensorComment(sensor.getIdentifier(), editText.getText().toString());
                                update();
                            }
                        })
                        .setNeutralButton(R.string.delete_description, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //dialogInterface.dismiss();
                                storage.setSensorComment(sensor.getIdentifier(), null);
                                update();
                            }
                        })
                        .show();
                return true;
            case R.id.is_hidden:
                storage.setSensorHidden(sensor.getIdentifier(), !storage.isSensorHidden(sensor.getIdentifier()));
                invalidateOptionsMenu();
                return true;
            case R.id.show_uri:
                new AlertDialog.Builder(this)
                        .setTitle(R.string.sensor_uri)
                        .setNeutralButton(android.R.string.copy, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                ((ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("Sensor", sensor.getIdentifier().toString()));
                                Toast.makeText(SensorActivity.this, R.string.copied, Toast.LENGTH_LONG).show();
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }
                        })
                        .setMessage(sensor.getIdentifier().toString())
                        .show();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
