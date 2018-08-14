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
import android.support.v7.app.AlertDialog;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

public class CustomIntervalDialog extends AlertDialog implements TextView.OnEditorActionListener, SeekBar.OnSeekBarChangeListener, View.OnKeyListener {
    private final EditText te_interval;
    private final SeekBar sb_interval;
    private View view;
    private int interval;

    private GlobalPreferences globalPreferences;

    public CustomIntervalDialog(Context context) {
        super(context);

        setTitle(R.string.set_interval);

        view = getLayoutInflater().inflate(R.layout.dialog_custom_interval, null);
        setView(view);

        globalPreferences = new GlobalPreferences(getContext());

        te_interval = view.findViewById(R.id.interval_text);
        sb_interval = view.findViewById(R.id.interval_seek);

        te_interval.setOnEditorActionListener(this);
        sb_interval.setOnSeekBarChangeListener(this);
        te_interval.setOnKeyListener(this);

        interval = globalPreferences.getUpdateInterval();
        refreshBoth();

        setButton(BUTTON_NEGATIVE, getContext().getResources().getText(android.R.string.cancel), new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dismiss();
            }
        });
        setButton(BUTTON_POSITIVE, getContext().getResources().getText(android.R.string.ok), new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (globalPreferences.getUpdateInterval() > GlobalPreferences.UPDATE_INTERVAL_CAN_TAKE_WHILE_LIMIT) {
                    Toast.makeText(getContext(), R.string.interval_change_can_take_while, Toast.LENGTH_LONG).show();
                }
                globalPreferences.setUpdateInterval(interval);
                dismiss();
            }
        });
    }

    @Override
    public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
        refreshIntervalFromTextEditor();
        return false;
    }

    @Override
    public boolean onKey(View view, int i, KeyEvent keyEvent) {
        refreshIntervalFromTextEditor();
        return false;
    }

    private void refreshIntervalFromTextEditor() {
        getButton(BUTTON_POSITIVE).setEnabled(false); // Disable OK button on failure

        try {
            interval = (int)(Double.parseDouble(te_interval.getText().toString()) * 1000d);
        }
        catch (Exception ignored) {
            return;
        }

        if (interval > 10_000) {
            interval = 10_000;
            Toast.makeText(getContext(), R.string.interval_out_of_range, Toast.LENGTH_SHORT).show();
            return;
        }
        else if (interval < 100) {
            interval = 100;
            Toast.makeText(getContext(), R.string.interval_out_of_range, Toast.LENGTH_SHORT).show();
            return;
        }
        else {
            getButton(BUTTON_POSITIVE).setEnabled(true); // If everything is valid, enable OK button
        }
        refreshSeekBar();
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
        if (b) {
            interval = (int) (1000 * (Math.pow(10d, (i / 1000d) * 2d)) / 10d);
            refreshBoth();
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    protected void refreshEditText() {
        te_interval.setText(String.format(Locale.getDefault(), "%.3f", interval / 1000f));
    }

    protected void refreshSeekBar() {
        sb_interval.setProgress((int)((500d*Math.log((double)interval))/(Math.log(2d)+Math.log(5d))-1000d));
    }

    protected void refreshBoth() {
        refreshEditText();
        refreshSeekBar();
    }
}
