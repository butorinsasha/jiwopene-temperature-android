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

import android.net.Uri;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;


public class SysfsSensor implements Sensor {
    private final String path;

    public SysfsSensor(String path) {
        this.path = path;
    }

    @Override
    public float getValue() throws CannotReadSensorException {
        try {
            FileReader fr = new FileReader(new File(path, "temp"));
            BufferedReader br = new BufferedReader(fr);
            return Float.parseFloat(br.readLine()) / 1000;
        }
        catch (Exception e) {
            throw new CannotReadSensorException();
        }
    }

    @Override
    public String getName() throws CannotReadSensorException {
        try {
            FileReader fr = new FileReader(new File(path, "type"));
            BufferedReader br = new BufferedReader(fr);
            return br.readLine();
        }
        catch (Exception e) {
            throw new CannotReadSensorException();
        }
    }

    @Override
    public Uri getIdentifier() {
        return Uri.fromFile(new File(path));
    }
}
