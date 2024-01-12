/*
 * temperature-android
 * Copyright (C) 2024  jiwopene
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

package com.gmail.jiwopene.temperature.api.v2;

    public class Informers {

        public Fact fact;

        public class Fact {
            public float temp;

            public float getTemp() {
                return temp;
            }

            public void setTemp(int temp) {
                this.temp = temp;
            }
        }
    }
