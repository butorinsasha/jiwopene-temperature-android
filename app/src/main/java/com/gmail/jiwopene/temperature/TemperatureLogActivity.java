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

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.graphics.Canvas;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.gmail.jiwopene.temperature.sensors.Sensor;
import com.gmail.jiwopene.temperature.sensors.SensorStorage;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

public class TemperatureLogActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    protected static final int ACTIVITY_RESULT_IMPORT_BACKUP_SELECTED = 0;

    private static final long CHECK_NEW_ITEMS_EVERY = 20000;
    private static final long REFRESH_BUTTON_BLINK_INTERVAL = 500;
    private SensorStorage storage;
    private GlobalPreferences global;
    private Uri selectedSensorIdentifier;
    private MenuItem refreshButton = null;
    private Date lastDate = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_temperature_log);

        storage = new SensorStorage(this);
        global = new GlobalPreferences(this);

        Spinner sensorSelector = findViewById(R.id.sensor_selector);

        sensorSelector.setOnItemSelectedListener(this);

        final ListView recordList = findViewById(R.id.records);

        recordList.setFastScrollAlwaysVisible(true);
        recordList.setScrollBarStyle(View.SCROLLBARS_INSIDE_INSET);
    }

    private boolean refreshButtonDarkened;
    protected void setRefreshButtonBlinkState(boolean darkened) {
        refreshButtonDarkened = darkened;

        if (refreshButton != null) {
            Drawable icon = getResources().getDrawable(R.drawable.round_refresh_white_24);
            icon.setColorFilter(new ColorMatrixColorFilter(new float[]{
                    1, 0, 0, 0, 0,
                    0, 1, 0, 0, 0,
                    0, 0, 1, 0, 0,
                    0, 0, 0, darkened?.75f:1, 0,
            }));
            refreshButton.setIcon(icon);
        }

        invalidateOptionsMenu();
    }

    protected boolean getRefreshButtonBlinkState() {
        return refreshButtonDarkened;
    }

    private boolean newDataAvailable = false;
    protected void setNewDataAvailable(boolean newDataAvailable) {
        if (this.newDataAvailable == newDataAvailable)
            return;

        if (newDataAvailable) {
            refreshButtonBlinkHandler.post(refreshButtonBlink);
            Toast.makeText(this, R.string.new_log_data_available, Toast.LENGTH_SHORT).show();
        }
        else {
            refreshButtonBlinkHandler.removeCallbacks(refreshButtonBlink);
            setRefreshButtonBlinkState(false);
        }
        this.newDataAvailable = newDataAvailable;
    }
    protected boolean isNewDataAvailable() {
        return newDataAvailable;
    }

    private Handler refreshButtonBlinkHandler = new Handler();
    private Runnable refreshButtonBlink = new Runnable() {
        @Override
        public void run() {
            setRefreshButtonBlinkState(!getRefreshButtonBlinkState());
            refreshButtonBlinkHandler.postDelayed(this, REFRESH_BUTTON_BLINK_INTERVAL);
        }
    };

    private Handler checkNewDataHandler = new Handler();
    private Runnable checkNewData = new Runnable() {
        @Override
        public void run() {
            if (selectedSensorIdentifier != null) {
                TemperatureLog.Record[] records = new TemperatureLog(TemperatureLogActivity.this).fetch(1, null, null, selectedSensorIdentifier);
                if (records.length > 0 && lastDate != null) {
                    if (records[0].getDate().after(lastDate)) {
                        if (!isNewDataAvailable()) {
                            setNewDataAvailable(true);
                        }
                    }
                }
                else
                    refreshButtonBlinkHandler.removeCallbacks(refreshButtonBlink);
            }
            checkNewDataHandler.postDelayed(this, CHECK_NEW_ITEMS_EVERY);
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        ((Spinner)findViewById(R.id.sensor_selector)).setAdapter(sensorSelectorAdapterFromArray(storage.getSensors(global.getShowHidden())));

        checkNewDataHandler.post(checkNewData);
    }

    @Override
    protected void onPause() {
        super.onPause();

        checkNewDataHandler.removeCallbacks(checkNewData);
        setNewDataAvailable(false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.temperature_log_activity_menu, menu);
        refreshButton = menu.findItem(R.id.refresh);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settings:
                startActivity(new Intent(this, TemperatureLogSettingsActivity.class));
                return true;
            case R.id.find_by_date:
                openFindByDateDialog();
                return true;
            case R.id.refresh:
                refreshList();
                return true;
            case R.id.delete_all:
                showDeleteLogDialog(null);
                return true;
            case R.id.delete_only_this:
                showDeleteLogDialog(selectedSensorIdentifier);
                return true;
            case R.id.export_csv: {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
                File directory;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "TemperatureLog");
                }
                else {
                    directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "TemperatureLog");
                }
                File file = new File(String.format(Locale.ROOT, "%s/export-%d.csv", directory, new Date().getTime()));
                if (!directory.mkdirs())
                    Log.e("Mkdirs", "Cannot create "+ directory.getPath());
                try {
                    BufferedWriter bw = new BufferedWriter(new FileWriter(file));
                    bw.write(new TemperatureLog(this).getAsCSV(null));
                    bw.close();
                    showShareExportDialog(file);
                }
                catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(this, R.string.log_export_failed, Toast.LENGTH_LONG).show();
                }
                return true;
            }
            case R.id.export_backup: {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
                File directory;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "TemperatureLog");
                }
                else {
                    directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "TemperatureLog");
                }
                File file = new File(String.format(Locale.ROOT, "%s/export-%d.tlb", directory, new Date().getTime()));
                if (!directory.mkdirs())
                    Log.e("Mkdirs", "Cannot create "+ directory.getPath());
                try {
                    BufferedWriter bw = new BufferedWriter(new FileWriter(file));
                    bw.write(new TemperatureLog(this).getBackup());
                    bw.close();
                    showShareExportDialog(file);
                }
                catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(this, R.string.log_export_failed, Toast.LENGTH_LONG).show();
                }
                return true;
            }
            case R.id.import_backup:
                showImportBackupDialog();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    protected void showShareExportDialog(File file) {
        ((ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText(file.getPath(), file.getAbsolutePath()));
        Toast.makeText(this, String.format(Locale.getDefault(), getString(R.string.log_exported_to), file.getPath()), Toast.LENGTH_LONG).show();
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
        shareIntent.setType(URLConnection.guessContentTypeFromName(file.getName()));
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(shareIntent, getString(R.string.log_export_share)));
    }

    protected void showImportBackupDialog() {
        Intent fileIntent = new Intent(Intent.ACTION_GET_CONTENT);
        fileIntent.setType("*/*");
        fileIntent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(Intent.createChooser(fileIntent, getString(R.string.log_import_select_file)), ACTIVITY_RESULT_IMPORT_BACKUP_SELECTED);
        }
        catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, R.string.download_filemanager, Toast.LENGTH_LONG).show();

            // Direct user to Ghost Commander download
            // UNTESTED
            try {
                startActivity(Intent.parseUri("market://details?id=com.ghostsq.commander", 0));
            } catch (URISyntaxException e1) {
                e1.printStackTrace();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
        switch (requestCode) {
            case ACTIVITY_RESULT_IMPORT_BACKUP_SELECTED:
                if (data == null) {
                    Toast.makeText(this, R.string.canceled, Toast.LENGTH_SHORT).show();
                    break;
                }
                try {
                    final AlertDialog dialog = new AlertDialog.Builder(this)
                            .setTitle(R.string.log_import_backup)
                            .create();
                    final LoadBackupInfoAsyncTask task = new LoadBackupInfoAsyncTask(dialog);

                    // Show confirm dialog
                    dialog.setView(dialog.getLayoutInflater().inflate(R.layout.dialog_load_backup, null));
                    final DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            switch (which) {
                                case DialogInterface.BUTTON_POSITIVE:
                                    task.cancel(true);
                                    loadBackup(data.getData());
                                    break;
                                case DialogInterface.BUTTON_NEGATIVE:
                                    task.cancel(true);
                                    break;
                            }
                        }
                    };
                    dialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(android.R.string.cancel), listener);
                    dialog.setButton(DialogInterface.BUTTON_POSITIVE, getString(R.string.log_import_backup_import_button), listener);
                    dialog.show();

                    task.execute(data.getData());
                }
                catch (Exception e) {
                    Toast.makeText(this, R.string.log_import_failed, Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
                break;
        }
    }

    public void loadBackup(Uri backup) {
        new LoadBackupAsyncTask().execute(backup);
    }

    private class LoadBackupInfoAsyncTask extends AsyncTask<Uri, Double, String> {

        Dialog dialog;

        LoadBackupInfoAsyncTask(Dialog dialog) {
            super();

            this.dialog = dialog;
        }

        @Override
        protected String doInBackground(Uri... uris) {
            boolean empty = true;
            try {
                InputStream backup = getContentResolver().openInputStream(uris[0]);
                long size = backup.available(); // To calculate position in stream

                StringBuilder output = new StringBuilder();

                // Get description
                ArrayList<Byte> description_bytes_list = new ArrayList<>();
                while (!isCancelled()) {
                    int b = backup.read();
                    if (b <= 0)
                        break;
                    description_bytes_list.add((byte)b);

                    reportPosition(backup.available(), size-backup.available());
                }
                byte[] description_bytes = new byte[description_bytes_list.size()];
                for (int i = 0; i < description_bytes.length && !isCancelled(); i++)
                    description_bytes[i] = description_bytes_list.get(i);

                output.append(new String(description_bytes));
                output.append("\n");

                // Look for sensors
                int statusCounter = 0;
                final int SHOW_STATUS_EVERY_BYTES = 65536; // To make processing faster report only after some number of bytes
                lookForSensors: while (!isCancelled()) {
                    int byteBuffer;
                    while (!isCancelled()) {
                        statusCounter++;
                        byteBuffer = backup.read();
                        if (byteBuffer <= 0)
                            break lookForSensors;
                        if (byteBuffer == 'S')
                            break;
                        if (statusCounter > SHOW_STATUS_EVERY_BYTES) {
                            reportPosition(backup.available(), size - backup.available());
                            statusCounter = 0;
                        }
                    }
                    for (int i = 0; i < 2; i++)
                        while (!isCancelled()) {
                            statusCounter++;
                            byteBuffer = backup.read();
                            if (byteBuffer <= 0)
                                break lookForSensors;
                            if (byteBuffer == ' ')
                                break;
                            if (statusCounter > SHOW_STATUS_EVERY_BYTES) {
                                reportPosition(backup.available(), size - backup.available());
                                statusCounter = 0;
                            }
                        }

                    while (!isCancelled()) {
                        statusCounter++;
                        byteBuffer = backup.read();
                        if (byteBuffer < 0)
                            break lookForSensors;
                        if (byteBuffer == 0)
                            break;
                        output.append((char)byteBuffer);
                        empty = false;
                        if (statusCounter > SHOW_STATUS_EVERY_BYTES) {
                            reportPosition(backup.available(), size - backup.available());
                            statusCounter = 0;
                        }
                    }
                    output.append("\n");
                }

                if (empty)
                    output.append(getString(R.string.log_import_file_empty));

                return output.toString();
            }
            catch (Exception e) {
                return null;
            }
        }

        private void reportPosition(long size, long position) {
            publishProgress((double) position / (double) size);
        }

        @Override
        protected void onProgressUpdate(Double... values) {
            super.onProgressUpdate(values);

            ((ProgressBar)dialog.findViewById(R.id.progressbar)).setProgress((int)(values[0] * 1000.0d));
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            dialog.findViewById(R.id.progressbar).setVisibility(View.GONE);
            ((TextView)dialog.findViewById(R.id.backup_info)).setText(s);
        }
    }

    private class LoadBackupAsyncTask extends AsyncTask<Uri, Double, Boolean> implements TemperatureLog.LoadFromBackupStatusChangeListener, DialogInterface.OnDismissListener {

        private AlertDialog dialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            dialog = new AlertDialog.Builder(TemperatureLogActivity.this)
                    .setTitle(R.string.log_import_backup)
                    .create();
            dialog.setOnDismissListener(this);
            dialog.setView(dialog.getLayoutInflater().inflate(R.layout.dialog_load_backup_progress, null));
            dialog.show();
        }

        @Override
        protected Boolean doInBackground(Uri... uris) {
            try {
                new TemperatureLog(TemperatureLogActivity.this).loadFromBackup(getContentResolver().openInputStream(uris[0]), this);
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
            return true;
        }

        @Override
        public void statusChanged(double status) {
            publishProgress(status);
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);

            dialog.dismiss();
            Toast.makeText(TemperatureLogActivity.this, R.string.log_import_backup_finished, Toast.LENGTH_LONG).show();

            refreshList();
        }

        @Override
        protected void onProgressUpdate(Double... values) {
            super.onProgressUpdate(values);

            ((ProgressBar)dialog.findViewById(R.id.progressbar)).setProgress((int)(values[0] * 1000));
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            cancel(true);
        }
    }

    private void showDeleteLogDialog(final Uri sensor) {
        final AlertDialog countdownDialog = new AlertDialog(TemperatureLogActivity.this) {
        };
        countdownDialog.setTitle(R.string.log_delete_really_title);
        final TextView countdown = new TextView(TemperatureLogActivity.this);
        countdown.setText(String.format(Locale.getDefault(), getString(R.string.log_delete_really_countdown), 5));
        countdownDialog.setView(countdown);
        int padding = (int)(getResources().getDisplayMetrics().density * 16);
        countdown.setPadding(padding, padding, padding, padding);
        final CountDownTimer countDownTimer = new CountDownTimer(5000, 1000) {
            @Override
            public void onTick(long l) {
                countdown.setText(String.format(Locale.getDefault(), getString(R.string.log_delete_really_countdown), l / 1000 + 1));
            }

            @Override
            public void onFinish() {
                new TemperatureLog(TemperatureLogActivity.this).deleteLog(sensor);
                countdownDialog.cancel();
                refreshList();
            }
        };
        countdownDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                countDownTimer.cancel();
                Toast.makeText(TemperatureLogActivity.this, R.string.canceled, Toast.LENGTH_SHORT).show();
            }
        });

        new AlertDialog.Builder(this)
                .setTitle(R.string.log_delete_really_title)
                .setMessage(sensor == null ? R.string.log_delete_really_message_all :
                    R.string.log_delete_really_message_this)
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Toast.makeText(TemperatureLogActivity.this, R.string.canceled, Toast.LENGTH_SHORT).show();
                    }
                })
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialogInterface, int i) {
                        countdownDialog.show();
                        countDownTimer.start();
                    }
                })
                .show();
    }

    private void openFindByDateDialog() {
        ListView recordList = findViewById(R.id.records);

        final Calendar calendar = new GregorianCalendar();
        calendar.setTimeInMillis(getSelectedDate().getTime());
        Log.d("SelectedDate", String.format(Locale.ROOT, "%d-%d-%d", calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)));
        final DatePickerDialog datePickerDialog = new DatePickerDialog(this, null, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));

        if (recordList.getAdapter().getCount() == 0) {
            Toast.makeText(this, R.string.cannot_find_in_empty_list, Toast.LENGTH_SHORT).show();
        }

        datePickerDialog.getDatePicker().setMaxDate(((TemperatureLog.Record)recordList.getAdapter().getItem(0)).getDate().getTime());
        datePickerDialog.getDatePicker().setMinDate(((TemperatureLog.Record)recordList.getAdapter().getItem(recordList.getAdapter().getCount() - 1)).getDate().getTime());

        Dialog.OnClickListener listener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                switch (i) {
                    case DialogInterface.BUTTON_POSITIVE:
                        Date date = new GregorianCalendar(
                                datePickerDialog.getDatePicker().getYear(),
                                datePickerDialog.getDatePicker().getMonth(),
                                datePickerDialog.getDatePicker().getDayOfMonth()
                        ).getTime();
                        scrollToDate(date);
                        break;
                    case DialogInterface.BUTTON_NEUTRAL:
                        openFindByDateAndTimeDialog(new GregorianCalendar(
                                datePickerDialog.getDatePicker().getYear(),
                                datePickerDialog.getDatePicker().getMonth(),
                                datePickerDialog.getDatePicker().getDayOfMonth(),
                                calendar.get(Calendar.HOUR_OF_DAY),
                                calendar.get(Calendar.MINUTE)
                        ).getTime());
                        break;
                }
            }
        };

        datePickerDialog.setButton(DialogInterface.BUTTON_POSITIVE, getString(R.string.find), listener);
        datePickerDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(android.R.string.cancel), listener);
        datePickerDialog.setButton(DialogInterface.BUTTON_NEUTRAL, getString(R.string.find_by_date_add_time), listener);

        datePickerDialog.show();
    }

    private void openFindByDateAndTimeDialog(final Date initialDate) {
        Calendar selectedDate = new GregorianCalendar();
        selectedDate.setTime(initialDate);

        TimePickerDialog.OnTimeSetListener timeListener = new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker timePicker, int hour, int minute) {
                Calendar calendar = new GregorianCalendar();
                calendar.setTime(initialDate);

                scrollToDate(new GregorianCalendar(
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH),
                        hour,
                        minute
                ).getTime());
            }
        };
        final TimePickerDialog timePickerDialog = new TimePickerDialog(this, timeListener, selectedDate.get(Calendar.HOUR_OF_DAY), selectedDate.get(Calendar.MINUTE), true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            timePickerDialog.create();

            timePickerDialog.getButton(DialogInterface.BUTTON_POSITIVE).setText(R.string.find);
            timePickerDialog.getButton(DialogInterface.BUTTON_NEGATIVE).setText(android.R.string.cancel);
        }

        timePickerDialog.show();
    }

    private Date getSelectedDate() {
        Date date = new Date();

        ListView recordList = findViewById(R.id.records);

        TemperatureLog.Record topRecord = (TemperatureLog.Record) recordList.getAdapter().getItem(recordList.getFirstVisiblePosition());
        if (topRecord != null) {
            date = topRecord.date;
        }

        return date;
    }

    private void scrollToDate(Date date) {
        ListView recordList = findViewById(R.id.records);

        int foundPosition = recordList.getAdapter().getCount() - 1;

        if (foundPosition == -1)
            return;

        for (int i = foundPosition; i >= 0; i--) {
            TemperatureLog.Record item = (TemperatureLog.Record) recordList.getAdapter().getItem(i);
            if (item != null) {
                if (item.getDate().before(date)) {
                    foundPosition = i;
                }
            }
        }

        foundPosition = Math.max(0, foundPosition - 5);
        recordList.setSelection(foundPosition);
    }

    private SensorSelectorAdapter sensorSelectorAdapterFromArray(@NonNull Sensor[] sensors) {
        CachedSensorInfo cachedSensors[] = new CachedSensorInfo[sensors.length];
        for (int i = 0; i < sensors.length; i++) {
            cachedSensors[i] = new CachedSensorInfo();
            cachedSensors[i].original = sensors[i];
            cachedSensors[i].storage = storage;
            cachedSensors[i].refresh();
        }
        return new SensorSelectorAdapter(this, cachedSensors);
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        ((SensorSelectorAdapter)((Spinner)findViewById(R.id.sensor_selector)).getAdapter()).setSelected(i);
        findViewById(R.id.sensor_selector).invalidate();

        Uri newSensorIdentifier = ((CachedSensorInfo) ((Spinner)findViewById(R.id.sensor_selector)).getAdapter().getItem(i)).original.getIdentifier();
        if (selectedSensorIdentifier == null || !newSensorIdentifier.toString().equals(selectedSensorIdentifier.toString())) {
            selectedSensorIdentifier = newSensorIdentifier;

            refreshList();
        }
    }

    private void refreshList() {
        ListView recordListView = findViewById(R.id.records);
        recordListView.setAdapter(new LogAdapter(this, selectedSensorIdentifier));
        TemperatureLog.Record firstItem = (TemperatureLog.Record) recordListView.getAdapter().getItem(0);
        if (firstItem != null)
            lastDate = firstItem.getDate();
        setNewDataAvailable(false);
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {
        ((Spinner)findViewById(R.id.sensor_selector)).setSelection(0);
    }

    private class SensorSelectorAdapter extends ArrayAdapter<Object> implements SpinnerAdapter {
        private SensorSelectorAdapter(@NonNull Context context, @NonNull CachedSensorInfo[] objects) {
            super(context, R.layout.item_sensor_in_spinner, R.id.name, objects);

            for (CachedSensorInfo cachedSensorInfo : objects) {
                cachedSensorInfo.refresh();
            }
        }

        @SuppressLint("ViewHolder")
        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            // I really don't want to use convertView.
            // When I use it, it looks like hidden_flag is INVISIBLE, not GONE.
            convertView = getLayoutInflater().inflate(R.layout.item_sensor_in_spinner, null, true);

            CachedSensorInfo cachedSensorInfo = (CachedSensorInfo) getItem(position);

            if (cachedSensorInfo.name == null) {
                ((TextView)convertView.findViewById(R.id.name)).setText(R.string.unknown_sensor);
            }
            else {
                ((TextView)convertView.findViewById(R.id.name)).setText(cachedSensorInfo.name);
            }

            if (cachedSensorInfo.hidden) {
                convertView.findViewById(R.id.hidden_flag).setVisibility(View.VISIBLE);
            }
            else {
                convertView.findViewById(R.id.hidden_flag).setVisibility(View.GONE);
            }

            if (cachedSensorInfo.description == null) {
                ((TextView)convertView.findViewById(R.id.description)).setText(R.string.no_description);
            }
            else {
                ((TextView)convertView.findViewById(R.id.description)).setText(cachedSensorInfo.description);
            }

            return convertView;
        }

        @Override
        public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
            return getView(position, convertView, parent);
        }

        void setSelected(int position) {
            for (int i = 0; i < getCount(); i++) {
                CachedSensorInfo item = (CachedSensorInfo)getItem(i);
                if (item != null)
                    item.selected = (i == position);
            }
        }
    }
    private class CachedSensorInfo {
        String name;
        String description;
        Boolean hidden;
        Sensor original;
        SensorStorage storage;
        boolean selected = false;

        void refresh() {
            name = null;
            try {
                name = original.getName();
            }
            catch (Exception ignored) {}

            description = null;
            try {
                description = storage.getSensorComment(original.getIdentifier());
            }
            catch (Exception ignored) {}

            hidden = null;
            try {
                hidden = storage.isSensorHidden(original.getIdentifier());
            }
            catch (Exception ignored) {}
        }

        public String toString() {
            return name;
        }
    }

    private class LogAdapter extends BaseAdapter {
        Cursor dataCursor;
        TemperatureLog log;

        LogAdapter(Context context, Uri identifier) {
            log = new TemperatureLog(context);
            dataCursor = log.fetchAsCursor(null, null, null, identifier);
            dataCursor.moveToFirst();
        }

        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        @Override
        public boolean isEnabled(int i) {
            return false;
        }

        @Override
        public void registerDataSetObserver(DataSetObserver dataSetObserver) {
            // List is static
        }

        @Override
        public void unregisterDataSetObserver(DataSetObserver dataSetObserver) {
            // List is static
        }

        @Override
        public int getCount() {
            return dataCursor.getCount();
        }

        @Override
        public Object getItem(int i) {
            if (getCount() > i) {
                dataCursor.moveToPosition(i);
                return log.rowToRecord(dataCursor);
            }
            return null;
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
            if (view == null) {
                view = new TemperatureLogRecordView(viewGroup.getContext());
            }
            if (i > 0) {
                ((TemperatureLogRecordView) view).setNextTemp(((TemperatureLog.Record) getItem(i - 1)).getTemp());
            } else {
                ((TemperatureLogRecordView) view).setNextTemp(((TemperatureLog.Record) getItem(i)).getTemp());
            }
            if (i < (getCount() - 1)) {
                ((TemperatureLogRecordView) view).setPreviousTemp(((TemperatureLog.Record) getItem(i + 1)).getTemp());
            } else {
                ((TemperatureLogRecordView) view).setPreviousTemp(((TemperatureLog.Record) getItem(i)).getTemp());
            }
            ((TemperatureLogRecordView) view).setCurrentTemp(((TemperatureLog.Record) getItem(i)).getTemp());

            ((TemperatureLogRecordView) view).setDate(((TemperatureLog.Record) getItem(i)).getDate());

            ((TemperatureLogRecordView) view).setGapBefore(false);
            if (i == (getCount() - 1)) {
                ((TemperatureLogRecordView) view).setGapBefore(true);
            }
            if (i < (getCount() - 1)) {
                if (Math.abs(((TemperatureLog.Record) getItem(i + 1)).getDate().getTime() - ((TemperatureLog.Record) getItem(i)).getDate().getTime()) > 600_000) {
                    ((TemperatureLogRecordView) view).setGapBefore(true);
                }
            }

            ((TemperatureLogRecordView) view).setGapAfter(false);
            if (i == 0) {
                ((TemperatureLogRecordView) view).setGapAfter(true);
            }
            if (i > 0) {
                if (Math.abs(((TemperatureLog.Record) getItem(i - 1)).getDate().getTime() - ((TemperatureLog.Record) getItem(i)).getDate().getTime()) > 600_000) {
                    ((TemperatureLogRecordView) view).setGapAfter(true);
                }
            }

            return view;
        }

        @Override
        public int getItemViewType(int i) {
            return 0;
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        @Override
        public boolean isEmpty() {
            return getCount() == 0;
        }
    }

    class TemperatureLogRecordView extends FrameLayout {
        public static final float MAXIMAL_TEMP = 110f;

        private final View internalView;
        private double previousTemp;
        private double currentTemp;
        private double nextTemp;
        private Date date;
        private boolean gapBefore = false;
        private boolean gapAfter = false;

        public TemperatureLogRecordView(Context context) {
            super(context);
            internalView = getLayoutInflater().inflate(R.layout.item_log_temperature, null, true);
            addView(internalView);
        }

        @Override
        protected void dispatchDraw(Canvas canvas) {
            double previousTemp = this.previousTemp;
            double nextTemp = this.nextTemp;

            if (gapAfter)
                nextTemp = currentTemp;
            if (gapBefore)
                previousTemp = currentTemp;

            Paint fillStyle = new Paint();
            ColorMatrixColorFilter colorFilter = new ColorMatrixColorFilter(new float[]{
                    1, 0, 0, 0, 0,
                    0, 1, 0, 0, 0,
                    0, 0, 1, 0, 0,
                    0, 0, 0, .5f, 0,
            });
            fillStyle.setColor(getContext().getResources().getColor(R.color.colorPrimary));
            fillStyle.setColorFilter(colorFilter);
            fillStyle.setAntiAlias(true);
            fillStyle.setStyle(Paint.Style.FILL);

            PointF point_middle = new PointF(
                    (float)(((currentTemp)) * (float)canvas.getWidth()) / MAXIMAL_TEMP,
                    canvas.getHeight() / 2
            );

            PolarPointF point_top_pol_rel = PolarPointF.fromPointF(new PointF(
                    (float)(((currentTemp + nextTemp) / 2f) * (float)canvas.getWidth()) / MAXIMAL_TEMP - point_middle.x,
                    0 - point_middle.y
            ));
            point_top_pol_rel.r *= 2;
            PointF point_top = point_top_pol_rel.toPointF();
            point_top.offset(point_middle.x, point_middle.y);

            PolarPointF point_bottom_pol_rel = PolarPointF.fromPointF(new PointF(
                    (float)(((currentTemp + previousTemp) / 2f) * (float)canvas.getWidth()) / MAXIMAL_TEMP - point_middle.x,
                    canvas.getHeight() - point_middle.y
            ));
            point_bottom_pol_rel.r *= 2;
            PointF point_bottom = point_bottom_pol_rel.toPointF();
            point_bottom.offset(point_middle.x, point_middle.y);


            Path fillPath = new Path();
            fillPath.moveTo(0,0);
            fillPath.lineTo(0, canvas.getHeight());
            // Starting from bottom
            fillPath.lineTo(point_bottom.x, point_bottom.y);
            fillPath.lineTo(point_middle.x, point_middle.y);
            fillPath.lineTo(point_top.x, point_top.y);


            Paint lineStyle = new Paint();
            lineStyle.setColor(getContext().getResources().getColor(R.color.colorPrimary));
            lineStyle.setStyle(Paint.Style.STROKE);
            lineStyle.setStrokeCap(Paint.Cap.ROUND);
            lineStyle.setStrokeJoin(Paint.Join.ROUND);
            lineStyle.setAntiAlias(true);
            lineStyle.setStrokeWidth(getResources().getDimensionPixelSize(R.dimen.chart_line_width));

            Path linePath = new Path();
            double averageTemp = (currentTemp + previousTemp) / 2;

            if (gapBefore && gapAfter) {
                linePath.moveTo(point_middle.x, (canvas.getHeight() * 1) / 3);
                linePath.lineTo(point_middle.x, (canvas.getHeight() * 2) / 3);
            }
            else {
                linePath.moveTo(point_bottom.x, point_bottom.y);
                if (gapBefore)
                    linePath.moveTo(point_middle.x, point_middle.y);
                else
                    linePath.lineTo(point_middle.x, point_middle.y);
                if (gapAfter)
                    linePath.moveTo(point_top.x, point_top.y);
                else
                    linePath.lineTo(point_top.x, point_top.y);
            }

            canvas.drawPath(fillPath, fillStyle);
            canvas.drawPath(linePath, lineStyle);
            super.dispatchDraw(canvas);
        }

        public void setPreviousTemp(double previous) {
            this.previousTemp = previous;
            invalidate();
        }

        public void setNextTemp(double next) {
            this.nextTemp = next;
            invalidate();
        }

        public void setCurrentTemp(double currentTemp) {
            this.currentTemp = currentTemp;
            updateText();
            invalidate();
        }

        public void setDate(Date date) {
            this.date = date;
            updateText();
        }

        public void setGapAfter(boolean gapAfter) {
            this.gapAfter = gapAfter;
        }

        public void setGapBefore(boolean gapBefore) {
            this.gapBefore = gapBefore;
        }

        private void updateText() {
            String date;
            if (this.date == null) {
                date = "?";
            }
            else date = DateFormat.getInstance().format(this.date);

            ((TextView)internalView.findViewById(R.id.date)).setText(date);
            ((TextView)internalView.findViewById(R.id.temperature)).setText(String.format(Locale.getDefault(), "%.2f °C", this.currentTemp));
        }
    }
}
