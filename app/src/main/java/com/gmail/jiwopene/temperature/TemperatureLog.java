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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.gmail.jiwopene.temperature.sensors.SensorStorage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Locale;

public class TemperatureLog {
    public static final int CURRENT_VERSION = 1;
    private static final String TABLE = "log";
    private static final String COL_ID = "id";
    private static final String COL_URI = "uri";
    private static final String COL_TIMESTAMP = "timestamp";
    private static final String COL_TEMP = "temp";

    private final Context context;

    SQLiteDatabase database;
    SensorStorage sensorStorage;

    public TemperatureLog(Context context) {
        this.context = context;
        
        sensorStorage = new SensorStorage(context);
        
        database = new SQLiteOpenHelper(context, "log", null, CURRENT_VERSION) {

            @Override
            public void onCreate(SQLiteDatabase sqLiteDatabase) {
                onUpgrade(sqLiteDatabase, 0, CURRENT_VERSION);
            }

            @Override
            public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
                switch (newVersion) {
                    case 1:
                        switch (oldVersion) {
                            case 0:
                                database.execSQL("create table "+TABLE+" ("+COL_ID+" integer primary key autoincrement not null, "+COL_URI+" text not null, "+COL_TIMESTAMP+" integer not null, "+COL_TEMP+" integer)");
                                return;
                        }
                        return;
                }
                throw new SQLException(String.format(Locale.ROOT, "Cannot upgrade log database ver. %d to %d", oldVersion, newVersion));
            }

            @Override
            public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
                super.onDowngrade(db, oldVersion, newVersion);
            }
        }.getWritableDatabase();
    }

    public void append(@NonNull Date date, @NonNull Uri identifier, double temp) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL_URI, identifier.toString());
        contentValues.put(COL_TEMP, (int)(temp * 1000));
        contentValues.put(COL_TIMESTAMP, date.getTime());
        database.insert(TABLE, null, contentValues);
    }

    public Cursor fetchAsCursor(@Nullable Integer last, @Nullable Date from, @Nullable Date to, @Nullable Uri sensor) {
        String limit = null;
        if (last != null)
            limit = Integer.toString(last);

        ArrayList<String> args = new ArrayList<>(4);
        if (from == null) {
            args.add("1");
            args.add("");
        }
        else {
            args.add("0");
            args.add(Integer.toString((int)(from.getTime()/1000)));
        }
        if (to == null) {
            args.add("1");
            args.add("");
        }
        else {
            args.add("0");
            args.add(Integer.toString((int)(to.getTime()/1000)));
        }
        if (sensor == null) {
            args.add("1");
            args.add("");
        }
        else {
            args.add("0");
            args.add(sensor.toString());
        }

        return database.query(TABLE, new String[]{COL_TEMP, COL_TIMESTAMP, COL_URI}, "(? or " + COL_TIMESTAMP + "> ?) and (? or " + COL_TIMESTAMP + "> ?) and (? or "+COL_URI+" = ?)", args.toArray(new String[6]), null, null, COL_ID + " desc", limit);
    }

    public TemperatureLog.Record[] fetch(@Nullable Integer last, @Nullable Date from, @Nullable Date to, @Nullable Uri sensor) {
        Cursor records = fetchAsCursor(last, from, to, sensor);

        records.moveToFirst();

        Record[] out = new Record[records.getCount()];

        for (int i = 0; i < out.length; i++, records.moveToNext()) {
            out[i] = new Record(new Date(records.getLong(1)), Uri.parse(records.getString(2)), (double)records.getInt(0) / 1000d);
        }

        records.close();

        return out;
    }

    public void deleteLog(@Nullable Uri sensor) {
        if (sensor == null) {
            database.delete(TABLE, null, null);
        }
        else {
            database.delete(TABLE, COL_URI + " = ?", new String[]{ sensor.toString() });
        }
    }

    public String getAsCSV(@Nullable Uri[] sensors) {
        if (sensors == null) {
            sensors = getSensorList();
        }

        HashMap<Long, HashMap<String, Double>> records = new HashMap<>();

        for (Uri sensor : sensors) {
            Cursor sensorRecords = fetchAsCursor(null, null, null, sensor);
            sensorRecords.moveToNext();

            while (!sensorRecords.isAfterLast()) {
                if (!records.containsKey(sensorRecords.getLong(1))) {
                    HashMap<String, Double> newLine = new HashMap<>();
                    for (Uri newLineSensor : sensors) {
                        newLine.put(newLineSensor.toString(), null);
                    }
                    records.put(sensorRecords.getLong(1), newLine);
                }

                records.get(sensorRecords.getLong(1)).put(sensorRecords.getString(2), ((double)sensorRecords.getInt(0)) / 1000d);
                sensorRecords.moveToNext();
            }

            sensorRecords.close();
        }

        StringBuffer csv = new StringBuffer();

        // Write header
        csv.append("\"TIME\",");
        for (int i = 0; i < sensors.length; i++) {
            csv.append("\"");
            csv.append(sensors[i].toString().replace("\"", "\"\""));
            csv.append("\"");
            if (i < (sensors.length - 1))
                csv.append(",");
        }
        csv.append("\n");

        ArrayList<Long> sortedTime = new ArrayList<>(records.keySet());
        Collections.sort(sortedTime);

        for (Long time : sortedTime) {
            csv.append("\"");
            DateFormat dateFormat = new SimpleDateFormat("y-M-d H:m Z", Locale.ROOT);
            csv.append(dateFormat.format(new Date(time)).replace("\"", "\"\""));
            csv.append("\",");

            for (int i = 0; i < sensors.length; i++) {
                csv.append("\"");
                Double temp = records.get(time).get(sensors[i].toString());
                if (temp != null)
                    csv.append(temp.toString());
                csv.append("\"");
                if (i < (sensors.length - 1))
                    csv.append(",");
            }
            csv.append("\n");
        }

        return csv.toString();
    }

    public Uri[] getSensorList() {
        Cursor uris = database.query(TABLE, new String[]{COL_URI}, null, null, COL_URI, null, null);
        uris.moveToFirst();
        Uri[] sensors = new Uri[uris.getCount()];
        for (int i = 0; i < sensors.length; i++, uris.moveToNext())
            sensors[i] = Uri.parse(uris.getString(0));
        uris.close();
        return sensors;
    }

    /**
     * Returns content of database as the "backup" file
     * @return The backup
     * 
     * <h2>Format description</h2>
     * <h3>File header</h3>
     * The file starts with header. It can be any text,
     * this field is terminated with NUL character ('\0')
     * 
     * <h3>Sensor definitions</h3>
     * Sensors must be defined before first use. The definition
     * is a line (terminated with '\0') in format
     * <code>SENSOR id uri desc</code> where <code>id</code> is
     * the sensor number (sensors can be replaced using multiple
     * definitions) and <code>uri</code> is sensor URI. The last
     * field (<code>desc</code>) can contain anything including
     * newlines. The NUL character is used to terminate the
     * field.
     * 
     * <h3>Records in log</h3>
     * Every record is stored as one line (terminated with '\n'
     * in format <code>ts sensor temp</code>. <code>ts</code>
     * is timestamp (milliseconds from Jan 1 1970 UTC),
     * <code>sensor</code> is the sensor number (the last sensor definition
     * <em>above</em> the record) and <code>temp</code> is
     * temperature with the dot ('.') character used as decimal
     * point.
     */
    public String getBackup() {
        StringBuffer stringBuffer = new StringBuffer();

        stringBuffer.append("Temperature log backup from "+context.getPackageName()+"\n");
        stringBuffer.append("Version "+BuildConfig.VERSION_NAME+" (#"+BuildConfig.VERSION_CODE+")\n");
        stringBuffer.append("\0"); // Terminate header

        Hashtable<String, Long> sensorTable = new Hashtable<>();

        {
            long lastSensorId = 0;

            for (Uri sensor : getSensorList()) {
                sensorTable.put(sensor.toString(), lastSensorId);

                stringBuffer.append("SENSOR ");
                stringBuffer.append(lastSensorId);
                stringBuffer.append(" ");
                stringBuffer.append(sensor.toString());
                stringBuffer.append(" ");
                if (sensorStorage.getSensorComment(sensor) != null)
                    stringBuffer.append(sensorStorage.getSensorComment(sensor));
                stringBuffer.append("\0");

                lastSensorId++;
            }
        }

        for (Record record : fetch(null, null, null, null)) {
            stringBuffer.append(record.date.getTime());
            stringBuffer.append(" ");
            stringBuffer.append(sensorTable.get(record.getIdentifier().toString()));
            stringBuffer.append(" ");
            stringBuffer.append(String.format(Locale.ROOT, "%.3f", record.temp));
            stringBuffer.append("\n");
        }

        return stringBuffer.toString();
    }

    public void loadFromBackup(InputStream backup, LoadFromBackupStatusChangeListener statusChangeListener) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(backup));
        long size = backup.available(); // Initial size

        // Skip header
        while (reader.read() > 0);

        StringBuffer lineBuffer = new StringBuffer();
        int byteBuffer;

        Hashtable<Long, String> sensors = new Hashtable<>(1);

        lineLoop: for (;; lineBuffer.delete(0, lineBuffer.length())) {
            byteBuffer = reader.read();
            if (statusChangeListener != null && size != 0)
                statusChangeListener.statusChanged(1.0d - (double)(backup.available()) / (double)size);
            if (byteBuffer == -1)
                break;
            lineBuffer.append((char)byteBuffer);

            if (byteBuffer == (int)'S') { // Is sensor definition
                for (byteBuffer = 0; byteBuffer >= 0 && byteBuffer != ' ';) {
                    byteBuffer = reader.read();
                    if (statusChangeListener != null && size != 0)
                        statusChangeListener.statusChanged(1.0d - (double)(backup.available()) / (double)size);
                    if (byteBuffer == -1)
                        continue lineLoop;
                }

                long id = 0;
                for (byteBuffer = '0'; byteBuffer >= '0' && byteBuffer <= '9'; byteBuffer = reader.read())
                    id = id * 10 + byteBuffer - '0';

                lineBuffer.deleteCharAt(0);
                while (true) {
                    byteBuffer = reader.read();
                    if (statusChangeListener != null && size != 0)
                        statusChangeListener.statusChanged(1.0d - (double)(backup.available()) / (double)size);
                    if (byteBuffer < 0)
                        break lineLoop;
                    if (byteBuffer == ' ')
                        break;
                    lineBuffer.append((char)byteBuffer);
                }

                String uri = lineBuffer.toString();
                sensors.put(id, uri);

                while (true) {
                    byteBuffer = reader.read();
                    if (statusChangeListener != null && size != 0)
                        statusChangeListener.statusChanged(1.0d - (double)(backup.available()) / (double)size);
                    if (byteBuffer < 0)
                        break lineLoop;
                    if (byteBuffer == 0)
                        break;
                    lineBuffer.append((char)byteBuffer);
                }

                new SensorStorage(context).setSensorComment(Uri.parse(uri), lineBuffer.toString());
            }
            else { // Is temperature
                lineBuffer.append(reader.readLine());

                String[] fields = lineBuffer.toString().split(" ");
                if (fields.length < 3)
                    throw new NullPointerException();

                append(new Date(Long.parseLong(fields[0])), Uri.parse(sensors.get(Long.parseLong(fields[1]))), Double.parseDouble(fields[2]));
            }
        }
    }

    public interface LoadFromBackupStatusChangeListener {
        void statusChanged(double status);
    }

    /**
     * Get estimated backup size.
     * @return Size in bytes
     */
    public long getBackupSize() {
        Cursor items = fetchAsCursor(null, null, null,null);
        long size = items.getCount() * (10);
        items.close();
        return size;
    }

    public static class Record {
        Date date;
        double temp;
        Uri identifier;

        public Record(@NonNull Date date, @NonNull Uri identifier, double temp) {
            this.date = date;
            this.temp = temp;
            this.identifier = identifier;
        }

        public Date getDate() {
            return date;
        }

        public double getTemp() {
            return temp;
        }

        public Uri getIdentifier() {
            return identifier;
        }
    }
}
