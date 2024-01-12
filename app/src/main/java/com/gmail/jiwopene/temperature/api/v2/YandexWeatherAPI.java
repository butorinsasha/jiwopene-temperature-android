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

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Query;

public interface YandexWeatherAPI {

    String BASE_URL = "https://api.weather.yandex.ru/v2/";
    String X_YANDEX_API_KEY = "22494cd9-231c-488e-a636-028fa5cda9d2";

    @GET("informers/")
    Call<Informers> getInformers(
            @Header("X-Yandex-API-Key") String apiKey,
            @Query("lat") float lat,
            @Query("lon") float lon
    );
}
