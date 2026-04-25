/*
 * Copyright (C) 2026 The Clover Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.deviceinfo.aboutphone;

import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentManager;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;

/**
 * Dialog for editing the About phone device name from the top summary card.
 */
public class DeviceNameEditDialog extends InstrumentedDialogFragment
        implements TextWatcher, TextView.OnEditorActionListener {

    private static final String KEY_DEVICE_NAME = "device_name";
    private static final String TAG = "DeviceNameEditDialog";

    private AlertDialog mDialog;
    private EditText mDeviceNameView;

    public static void show(MyDeviceInfoFragment host) {
        final FragmentManager manager = host.getActivity().getSupportFragmentManager();
        if (manager.findFragmentByTag(TAG) != null) {
            return;
        }

        final DeviceNameEditDialog dialog = new DeviceNameEditDialog();
        dialog.setTargetFragment(host, 0 /* requestCode */);
        dialog.show(manager, TAG);
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DEVICEINFO;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final MyDeviceInfoFragment host = (MyDeviceInfoFragment) getTargetFragment();
        final String deviceName = savedInstanceState != null
                ? savedInstanceState.getString(KEY_DEVICE_NAME)
                : host.getCurrentDeviceName().toString();

        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final LayoutInflater inflater = LayoutInflater.from(builder.getContext());
        final View view = inflater.inflate(R.layout.dialog_edittext, null, false);
        mDeviceNameView = view.findViewById(R.id.edittext);
        mDeviceNameView.setHint(R.string.my_device_info_device_name_preference_title);
        mDeviceNameView.setText(deviceName);
        Utils.setEditTextCursorPosition(mDeviceNameView);
        mDeviceNameView.setOnEditorActionListener(this);
        mDeviceNameView.addTextChangedListener(this);

        mDialog = builder
                .setTitle(R.string.my_device_info_device_name_preference_title)
                .setView(view)
                .setPositiveButton(R.string.bluetooth_rename_button,
                        (dialog, which) -> submitDeviceName())
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        mDialog.setOnShowListener(dialog -> {
            updatePositiveButtonState();
            showKeyboard();
        });
        return mDialog;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mDeviceNameView != null) {
            outState.putString(KEY_DEVICE_NAME, mDeviceNameView.getText().toString());
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mDialog = null;
        mDeviceNameView = null;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void afterTextChanged(Editable s) {
        updatePositiveButtonState();
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (actionId != EditorInfo.IME_ACTION_DONE && actionId != EditorInfo.IME_NULL) {
            return false;
        }
        if (!isCurrentInputValid()) {
            return true;
        }
        submitDeviceName();
        if (mDialog != null) {
            mDialog.dismiss();
        }
        return true;
    }

    private void updatePositiveButtonState() {
        if (mDialog == null) {
            return;
        }
        final Button positiveButton = mDialog.getButton(AlertDialog.BUTTON_POSITIVE);
        if (positiveButton != null) {
            positiveButton.setEnabled(isCurrentInputValid());
        }
    }

    private boolean isCurrentInputValid() {
        final MyDeviceInfoFragment host = (MyDeviceInfoFragment) getTargetFragment();
        return host != null && host.isDeviceNameValid(getSanitizedDeviceName());
    }

    private String getSanitizedDeviceName() {
        return mDeviceNameView == null ? "" : mDeviceNameView.getText().toString().trim();
    }

    private void submitDeviceName() {
        final MyDeviceInfoFragment host = (MyDeviceInfoFragment) getTargetFragment();
        if (host != null) {
            host.onDeviceNameEditSubmitted(getSanitizedDeviceName());
        }
    }

    private void showKeyboard() {
        if (mDeviceNameView == null || !mDeviceNameView.requestFocus()) {
            return;
        }
        final Context context = getContext();
        if (context == null) {
            return;
        }
        final InputMethodManager imm = context.getSystemService(InputMethodManager.class);
        if (imm != null) {
            imm.showSoftInput(mDeviceNameView, InputMethodManager.SHOW_IMPLICIT);
        }
    }
}
