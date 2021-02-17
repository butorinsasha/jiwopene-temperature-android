/*
 * temperature-android
 * Copyright (C) 2021  jiwopene
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

public final class SensorAdjustment {
    private float offset;
    private float multiplier;

    public float getOffset() {
        return offset;
    }

    public void setOffset(float offset) {
        this.offset = offset;
    }

    public float getMultiplier() {
        return multiplier;
    }

    public void setMultiplier(float multiplier) {
        this.multiplier = multiplier;
    }

    public SensorAdjustment(float offset, float multiplier) {
        this.offset = offset;
        this.multiplier = multiplier;
    }

    public double applyTo(double value) {
        return value * multiplier + offset;
    }

    public static SensorAdjustment getDefault() {
        return new SensorAdjustment(0, 1);
    }
}
