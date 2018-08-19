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

import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

/**
 * Created by Jiří on 15.08.2018.
 */

public class TemperatureLog {
    public static final int CURRENT_VERSION = 1;
    private static final String TABLE = "log";
    private static final String COL_ID = "id";
    private static final String COL_URI = "uri";
    private static final String COL_TIMESTAMP = "timestamp";
    private static final String COL_TEMP = "temp";

    private final Context context;

    SQLiteDatabase database;

    public TemperatureLog(Context context) {
        this.context = context;
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
