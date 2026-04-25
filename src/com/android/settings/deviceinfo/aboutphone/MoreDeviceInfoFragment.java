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

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceScreen;
import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.deviceinfo.BuildNumberPreferenceController;
import com.android.settings.flags.Flags;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.search.SearchIndexable;

import java.util.List;
import java.util.Set;

@SearchIndexable
public class MoreDeviceInfoFragment extends DashboardFragment {

    private static final String LOG_TAG = "MoreDeviceInfoFragment";
    private static final String KEY_EID_INFO = "eid_info";

    private BuildNumberPreferenceController mBuildNumberPreferenceController;

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DEVICEINFO;
    }

    @Override
    public int getHelpResource() {
        return R.string.help_uri_about;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mBuildNumberPreferenceController = use(BuildNumberPreferenceController.class);
        mBuildNumberPreferenceController.setHost(this /* parent */);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        MyDeviceInfoFragment.removeSectionCardDecorations(getListView());
    }

    @Override
    protected @NonNull Set<String> getPreferenceKeysInHierarchy() {
        Set<String> keys = super.getPreferenceKeysInHierarchy();
        keys.add(KEY_EID_INFO);
        return keys;
    }

    @Override
    protected void onPreferenceScreenCreatedFromResource(
            @NonNull PreferenceScreen preferenceScreen) {
        if (isCatalystEnabled()) {
            preferenceScreen.removePreferenceRecursively(KEY_EID_INFO);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        MyDeviceInfoFragment.bindMaintainerPreference(this);
    }

    @Override
    protected String getLogTag() {
        return LOG_TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.more_device_info;
    }

    @Override
    protected RecyclerView.Adapter onCreateAdapter(PreferenceScreen preferenceScreen) {
        mAdapter = new MyDeviceInfoFragment.AboutPhonePreferenceGroupAdapter(
                preferenceScreen, null);
        return mAdapter;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return MyDeviceInfoFragment.buildPreferenceControllers(
                context, this /* fragment */, getSettingsLifecycle());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (mBuildNumberPreferenceController.onActivityResult(requestCode, resultCode, data)) {
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * For Search.
     */
    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.more_device_info) {

                @Override
                public List<AbstractPreferenceController> createPreferenceControllers(
                        Context context) {
                    return MyDeviceInfoFragment.buildPreferenceControllers(
                            context, null /* fragment */, null /* lifecycle */);
                }
            };
}
