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

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class Controller implements Callback<Informer> {
    static final String BASE_URL = "https://api.weather.yandex.ru/v2/";
    static final String X_YANDEX_API_KEY = "22494cd9-231c-488e-a636-028fa5cda9d2";

    public void start() {
        Gson gson = new GsonBuilder()
                .setLenient()
                .create();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();


        YandexWeatherAPI yandexWeatherAPI = retrofit.create(YandexWeatherAPI.class);

        Call<Informer> call = yandexWeatherAPI.getWeather(
                X_YANDEX_API_KEY, // curl ... -H "X-Yandex-API-Key:
                59.938675f,                             // curl ... ?lat=59.938675
                30.314447f);                            // curl ... &lon=30.314447

        call.enqueue(this);
    }


    @Override
    public void onResponse(Call<Informer> call, Response<Informer> response) {
        if (response.isSuccessful()) {
            Informer informer = response.body();
            if (informer != null) {
                Log.i(this.getClass().getName(), "Yandex Weather: " + informer.fact.temp);

            }
        } else {
            Log.i(this.getClass().getName(), String.valueOf(response.errorBody()));
        }
    }

    @Override
    public void onFailure(Call<Informer> call, Throwable t) {
        t.printStackTrace();
    }
}
