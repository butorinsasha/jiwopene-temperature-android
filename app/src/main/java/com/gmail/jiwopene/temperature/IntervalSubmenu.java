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

import android.content.Context;
import android.content.DialogInterface;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.widget.Toast;

import java.util.Locale;

/**
 * Class that maintains submenu with interval selection.
 * When using, implement abstract methods so they run corresponding methods in activity and
 * call methods onPrepareOptionsMenu and onOptionsItemSelected in methods of activity with same name.
 *
 * You must call addToMenu
 * */
public abstract class IntervalSubmenu {
    private Context context;
    private GlobalPreferences globalPreferences;

    public IntervalSubmenu(Context context) {
        this.context = context;

        globalPreferences = new GlobalPreferences(context);
    }

    public void addToMenu(Menu menu, MenuInflater inflater) {
        SubMenu submenu = menu.addSubMenu(R.string.update_interval);
        submenu.getItem().setIcon(R.drawable.baseline_av_timer_white_24);
        submenu.getItem().setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        inflater.inflate(R.menu.interval, submenu);
    }

    public void onPrepareOptionsMenu(Menu menu) {
        updateMenu(menu);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        boolean canTakeWhile = (globalPreferences.getUpdateInterval() > GlobalPreferences.UPDATE_INTERVAL_CAN_TAKE_WHILE_LIMIT);
        switch (item.getItemId()) {
            case R.id.ultra_fast:
                globalPreferences.setUpdateInterval(GlobalPreferences.UPDATE_INTERVAL_ULTRA_FAST);
                invalidateOptionsMenu();
                break;
            case R.id.fast:
                globalPreferences.setUpdateInterval(GlobalPreferences.UPDATE_INTERVAL_FAST);
                invalidateOptionsMenu();
                break;
            case R.id.normal:
                globalPreferences.setUpdateInterval(GlobalPreferences.UPDATE_INTERVAL_NORMAL);
                invalidateOptionsMenu();
                break;
            case R.id.slow:
                globalPreferences.setUpdateInterval(GlobalPreferences.UPDATE_INTERVAL_SLOW);
                invalidateOptionsMenu();
                break;
            case R.id.custom:
                CustomIntervalDialog customIntervalDialog = new CustomIntervalDialog(context);
                customIntervalDialog.show();
                customIntervalDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialogInterface) {
                        invalidateOptionsMenu();
                    }
                });
                break;
            default: return false;
        }
        if (canTakeWhile)
            Toast.makeText(context, R.string.interval_change_can_take_while, Toast.LENGTH_LONG).show();
        return true;
    }

    public abstract void invalidateOptionsMenu();

    private void updateMenu(Menu menu) {
        menu.findItem(R.id.ultra_fast).setChecked(false);
        menu.findItem(R.id.fast).setChecked(false);
        menu.findItem(R.id.normal).setChecked(false);
        menu.findItem(R.id.slow).setChecked(false);
        menu.findItem(R.id.custom).setChecked(false);
        switch (globalPreferences.getUpdateInterval()) {
            case GlobalPreferences.UPDATE_INTERVAL_ULTRA_FAST:
                menu.findItem(R.id.ultra_fast).setChecked(true);
                break;
            case GlobalPreferences.UPDATE_INTERVAL_FAST:
                menu.findItem(R.id.fast).setChecked(true);
                break;
            case GlobalPreferences.UPDATE_INTERVAL_NORMAL:
                menu.findItem(R.id.normal).setChecked(true);
                break;
            case GlobalPreferences.UPDATE_INTERVAL_SLOW:
                menu.findItem(R.id.slow).setChecked(true);
                break;
            default:
                menu.findItem(R.id.custom).setChecked(true);
                break;
        }
        menu.findItem(R.id.interval).setTitle(String.format(Locale.getDefault(), context.getResources().getString(R.string.update_interval_status_fmt), (double)(globalPreferences.getUpdateInterval()) / 1000d));
    }
}
