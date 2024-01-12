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
import android.content.DialogInterface;
import android.content.Intent;
import android.database.DataSetObserver;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.gmail.jiwopene.temperature.api.v2.Controller;
import com.gmail.jiwopene.temperature.sensors.Sensor;
import com.gmail.jiwopene.temperature.sensors.SensorAdjustment;
import com.gmail.jiwopene.temperature.sensors.SensorStorage;

import java.util.ArrayList;
import java.util.Locale;

public class TemperaturesActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {

    private SensorStorage sensorStorage;
    private GlobalPreferences globalPreferences;

    private ListView sensorList;
    private IntervalSubmenu intervalSubmenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_temperatures);

        sensorStorage = new SensorStorage(this);
        globalPreferences = new GlobalPreferences(this);

        // Rescan sensors on first run
        if (globalPreferences.isFirstRun())
            sensorStorage.rescan();

        sensorList = findViewById(R.id.sensor_list);

        sensorList.setOnItemClickListener(this);

        rebuildSensorList();

        globalPreferences.setFirstRun(false);

        // Retrofit usage training
        Controller controller = new Controller();
        controller.start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        intervalSubmenu = new IntervalSubmenu(this) {
            @Override
            public void invalidateOptionsMenu() {
                TemperaturesActivity.this.invalidateOptionsMenu();
            }
        };
        intervalSubmenu.addToMenu(menu, getMenuInflater());
        getMenuInflater().inflate(R.menu.temperatures_activity_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        intervalSubmenu.onPrepareOptionsMenu(menu);

        menu.findItem(R.id.show_hidden).setChecked(globalPreferences.getShowHidden());

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (intervalSubmenu.onOptionsItemSelected(item))
            return true;

        switch (item.getItemId()) {
            case R.id.refresh_sensors:
                new AlertDialog.Builder(this)
                        .setTitle(android.R.string.dialog_alert_title)
                        .setMessage(R.string.refresh_sensor_alert)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                RefreshTask task = new RefreshTask(sensorStorage, TemperaturesActivity.this);
                                task.execute();
                            }
                        })
                        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {}
                        })
                        .show();
                return true;
            case R.id.show_hidden:
                globalPreferences.setShowHidden(!globalPreferences.getShowHidden());
                invalidateOptionsMenu();
                rebuildSensorList();
                return true;
            case R.id.log:
                startActivity(new Intent(this, TemperatureLogActivity.class));
                return true;
            case R.id.about:
                startActivity(new Intent(this, AboutActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    protected void rebuildSensorList() {
        sensorList.setAdapter(new SensorListAdapter(sensorStorage.getSensors(globalPreferences.getShowHidden()), sensorStorage));
        refreshValues();
    }

    private void refreshValues() {
        ((SensorListAdapter)(sensorList.getAdapter())).refreshValues();
    }

    private final Handler refreshValuesIterationHandler = new Handler();
    private final Runnable refreshValuesIteration = new Runnable() {
        @Override
        public void run() {
            refreshValues();

            refreshValuesIterationHandler.postDelayed(this, globalPreferences.getUpdateInterval());
        }
    };

    public void onPause() {
        super.onPause();
        refreshValuesIterationHandler.removeCallbacks(refreshValuesIteration);
    }

    public void onResume() {
        super.onResume();
        rebuildSensorList();
        refreshValuesIterationHandler.postDelayed(refreshValuesIteration, 0);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        String uri = ((SensorListAdapter.CachedSensor)(sensorList.getAdapter().getItem(i))).original.getIdentifier().toString();
        Intent intent = new Intent(this, SensorActivity.class);
        intent.putExtra(SensorActivity.EXTRA_SENSOR_URI, uri);
        startActivity(intent);
    }


    class RefreshTask extends AsyncTask<Void, Void, Void> {
        private final SensorStorage sensorStorage;
        private final TemperaturesActivity temperaturesActivity;

        RefreshTask(SensorStorage sensorStorage, TemperaturesActivity temperaturesActivity) {
            super();
            this.sensorStorage = sensorStorage;
            this.temperaturesActivity = temperaturesActivity;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            sensorStorage.rescan();
            return null;
        }

        protected void onPostExecute(Void ignored) {
            temperaturesActivity.rebuildSensorList();
        }
    }

    class SensorListAdapter implements ListAdapter {
        private static final int ITEM_TYPE_SENSOR = 0;
        private static final int ITEM_TYPE_MESSAGE = 1;

        private CachedSensor[] sensors;

        SensorListAdapter(Sensor[] sensors, SensorStorage storage) {
            ArrayList<CachedSensor> cachedSensors = new ArrayList<>(sensors.length);
            for (Sensor s : sensors) {
                CachedSensor cachedSensor = new CachedSensor();
                cachedSensor.original = s;
                cachedSensor.storage = storage;
                cachedSensors.add(cachedSensor);
            }
            this.sensors = (cachedSensors.toArray(new CachedSensor[]{}));
        }

        void refreshValues() {
            for (CachedSensor cachedSensor : sensors) {
                cachedSensor.refresh();
            }
            for (DataSetObserver observer : observers) {
                observer.onChanged();
            }
        }

        @Override
        public boolean areAllItemsEnabled() {
            return true;
        }

        @Override
        public boolean isEnabled(int i) {
            return true;
        }

        private ArrayList<DataSetObserver> observers = new ArrayList<>();
        @Override
        public void registerDataSetObserver(DataSetObserver dataSetObserver) {
            observers.add(dataSetObserver);
        }

        @Override
        public void unregisterDataSetObserver(DataSetObserver dataSetObserver) {
            observers.remove(dataSetObserver);
        }

        @Override
        public int getCount() {
            return Math.max(1, sensors.length);
        }

        @Override
        public Object getItem(int i) {
            if (sensors.length > i) {
                return sensors[i];
            }
            else return null;
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            if (sensors.length == 0) {
                TextView message = new TextView(viewGroup.getContext());
                message.setText(R.string.no_sensors);
                return message;
            }
            if (view == null) {
                view = new View(viewGroup.getContext());
                view.setTag(sensors[i].original.getIdentifier().toString());
                view = getLayoutInflater().inflate(R.layout.item_sensor_temperature, viewGroup, false);
            }
            if (sensors[i].name != null)
                ((TextView)(view.findViewById(R.id.name))).setText(sensors[i].name);
            else
                ((TextView)(view.findViewById(R.id.name))).setText(R.string.unknown_sensor);
            if (sensors[i].description != null)
                ((TextView) (view.findViewById(R.id.description))).setText(sensors[i].description);
            else
                ((TextView)(view.findViewById(R.id.description))).setText(R.string.no_description);
            if (sensors[i].value != null)
                ((TextView) (view.findViewById(R.id.temperature))).setText(String.format(Locale.getDefault(), "%.2f °C", sensors[i].adjustment.applyTo(sensors[i].value)));
            else
                ((TextView)(view.findViewById(R.id.temperature))).setText(R.string.no_temperature);
            if (sensors[i].hidden) {
                view.findViewById(R.id.hidden_flag).setVisibility(View.VISIBLE);
            }
            else {
                view.findViewById(R.id.hidden_flag).setVisibility(View.GONE);
            }
            return view;
        }

        @Override
        public int getItemViewType(int i) {
            if (sensors.length == 0)
                return ITEM_TYPE_MESSAGE;
            else
                return ITEM_TYPE_SENSOR;
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        private class CachedSensor {
            String name;
            String description;
            Float value;
            Boolean hidden;
            Sensor original;
            SensorStorage storage;
            SensorAdjustment adjustment;

            void refresh() {
                try {
                    name = original.getName();
                }
                catch (Exception ignored) {
                    name = original.getIdentifier().toString();
                }
                description = storage.getSensorComment(original.getIdentifier());
                try {
                    value = original.getValue();
                }
                catch (Exception ignored) {
                    value = null;
                }
                adjustment = storage.getSensorAdjustment(original.getIdentifier());
                hidden = storage.isSensorHidden(original.getIdentifier());
            }
        }
    }
}
