/*
 * Copyright (C) 2018 The Android Open Source Project
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

import static androidx.core.content.ContextCompat.getMainExecutor;

import android.app.Activity;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.hardware.display.DisplayManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.os.SystemProperties;
import android.os.storage.StorageManager;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.View;
import android.view.Display;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceViewHolder;
import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.deviceinfo.BluetoothAddressPreferenceController;
import com.android.settings.deviceinfo.BuildNumberPreferenceController;
import com.android.settings.deviceinfo.DeviceNamePreferenceController;
import com.android.settings.deviceinfo.FccEquipmentIdPreferenceController;
import com.android.settings.deviceinfo.FeedbackPreferenceController;
import com.android.settings.deviceinfo.IpAddressPreferenceController;
import com.android.settings.deviceinfo.ManualPreferenceController;
import com.android.settings.deviceinfo.RegulatoryInfoPreferenceController;
import com.android.settings.deviceinfo.SafetyInfoPreferenceController;
import com.android.settings.deviceinfo.UptimePreferenceController;
import com.android.settings.deviceinfo.WifiMacAddressPreferenceController;
import com.android.settings.deviceinfo.imei.ImeiInfoPreferenceController;
import com.android.settings.deviceinfo.simstatus.EidStatus;
import com.android.settings.deviceinfo.simstatus.SimEidPreferenceController;
import com.android.settings.deviceinfo.simstatus.SimStatusPreferenceController;
import com.android.settings.deviceinfo.simstatus.SlotSimStatus;
import com.android.settings.flags.Flags;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.widget.HighlightablePreferenceGroupAdapter;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.fuelgauge.BatteryUtils;
import com.android.settingslib.metadata.PreferenceSearchIndexablesProvider;
import com.android.settingslib.search.SearchIndexable;
import com.android.settingslib.widget.LayoutPreference;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

@SearchIndexable
public class MyDeviceInfoFragment extends DashboardFragment
        implements DeviceNamePreferenceController.DeviceNamePreferenceHost {

    private static final String LOG_TAG = "MyDeviceInfoFragment";
    private static final String KEY_ABOUT_PHONE_BRANDING = "about_phone_branding";
    private static final String KEY_ABOUT_PHONE_META_PILLS = "about_phone_meta_pills";
    private static final String KEY_ABOUT_PHONE_INFO_CARDS = "about_phone_info_cards";
    static final String KEY_CLOVER_MAINTAINER = "clover_maintainer";
    private static final String KEY_DEVICE_NAME = "device_name";
    private static final String KEY_EID_INFO = "eid_info";
    private static final String PROP_CLOVER_BUILD_VERSION = "ro.clover.build.version";
    private static final String PROP_CLOVER_DISPLAY_VERSION = "ro.clover.display.version";
    private static final String PROP_CLOVER_MAINTAINER = "ro.clover.maintainer";
    private static final String PROP_CLOVER_RELEASE_TYPE = "ro.clover.releasetype";

    private BuildNumberPreferenceController mBuildNumberPreferenceController;

    private DeviceInfoViewModel mDeviceInfoViewModel;

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
        use(DeviceNamePreferenceController.class).setHost(this /* parent */);
        mBuildNumberPreferenceController = use(BuildNumberPreferenceController.class);
        mBuildNumberPreferenceController.setHost(this /* parent */);
    }

    @Override
    public void onCreate(@Nullable Bundle icicle) {
        super.onCreate(icicle);
        mDeviceInfoViewModel = new ViewModelProvider(getActivity()).get(DeviceInfoViewModel.class);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        removeSectionCardDecorations(getListView());
    }

    @Override
    public void onStart() {
        super.onStart();
        bindBrandingCard();
        bindVersionPills();
        bindInfoCards();
        bindMaintainerPreference();
    }

    @Override
    protected String getLogTag() {
        return LOG_TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.my_device_info;
    }

    @Override
    protected RecyclerView.Adapter onCreateAdapter(PreferenceScreen preferenceScreen) {
        final Bundle arguments = getArguments();
        String key = arguments == null ? null
                : arguments.getString(SettingsActivity.EXTRA_FRAGMENT_ARG_KEY);
        if (Flags.catalyst() && key == null) {
            final Activity activity = getActivity();
            final Intent intent = activity != null ? activity.getIntent() : null;
            key = intent != null ? intent.getStringExtra(SettingsActivity.EXTRA_FRAGMENT_ARG_KEY)
                    : null;
        }
        key = PreferenceSearchIndexablesProvider.Companion.getHighlightKey(key);
        mAdapter = new AboutPhonePreferenceGroupAdapter(preferenceScreen, key);
        return mAdapter;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return buildMainPreferenceControllers(context, getSettingsLifecycle());
    }

    private static List<AbstractPreferenceController> buildMainPreferenceControllers(
            Context context, Lifecycle lifecycle) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        controllers.add(new DeviceNamePreferenceController(context, KEY_DEVICE_NAME));
        controllers.add(new UptimePreferenceController(context, lifecycle));
        return controllers;
    }

    static List<AbstractPreferenceController> buildPreferenceControllers(
            Context context, Fragment fragment, Lifecycle lifecycle) {
        // disable catalyst for settings search (i.e. fragment is null)
        boolean isCatalystEnabled = Flags.catalystMyDeviceInfoPrefScreen() && fragment != null;
        final List<AbstractPreferenceController> controllers = new ArrayList<>();

        final Executor executor = (fragment == null) ? getMainExecutor(context) :
                Executors.newSingleThreadExecutor();
        androidx.lifecycle.Lifecycle lifecycleObject = (fragment == null) ? null :
                fragment.getLifecycle();
        final SlotSimStatus slotSimStatus = new SlotSimStatus(context, executor, lifecycleObject);

        controllers.add(new IpAddressPreferenceController(context, lifecycle));
        controllers.add(new WifiMacAddressPreferenceController(context, lifecycle));
        controllers.add(new BluetoothAddressPreferenceController(context, lifecycle));
        controllers.add(new RegulatoryInfoPreferenceController(context));
        controllers.add(new SafetyInfoPreferenceController(context));
        controllers.add(new ManualPreferenceController(context));
        controllers.add(new FeedbackPreferenceController(fragment, context));
        controllers.add(new FccEquipmentIdPreferenceController(context));
        controllers.add(new UptimePreferenceController(context, lifecycle));

        Consumer<String> imeiInfoList = imeiKey -> {
            if (Flags.catalystMyDeviceInfoPrefScreen()) {
                return;
            }
            ImeiInfoPreferenceController imeiRecord =
                    new ImeiInfoPreferenceController(context, imeiKey);
            imeiRecord.init(fragment, slotSimStatus);
            controllers.add(imeiRecord);
        };

        if (fragment != null) {
            imeiInfoList.accept(ImeiInfoPreferenceController.DEFAULT_KEY);
        }

        for (int slotIndex = 0; slotIndex < slotSimStatus.size(); slotIndex++) {
            SimStatusPreferenceController slotRecord =
                    new SimStatusPreferenceController(context,
                            slotSimStatus.getPreferenceKey(slotIndex));
            slotRecord.init(fragment, slotSimStatus);
            controllers.add(slotRecord);

            if (fragment != null) {
                imeiInfoList.accept(ImeiInfoPreferenceController.DEFAULT_KEY + (1 + slotIndex));
            }
        }

        if (!isCatalystEnabled) {
            EidStatus eidStatus = new EidStatus(slotSimStatus, context, executor);
            SimEidPreferenceController simEid = new SimEidPreferenceController(context,
                    KEY_EID_INFO);
            simEid.init(slotSimStatus, eidStatus);
            controllers.add(simEid);
        }

        if (executor instanceof ExecutorService) {
            ((ExecutorService) executor).shutdown();
        }
        return controllers;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (mBuildNumberPreferenceController.onActivityResult(requestCode, resultCode, data)) {
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void bindVersionPills() {
        final LayoutPreference pillsPreference = findPreference(KEY_ABOUT_PHONE_META_PILLS);
        if (pillsPreference == null) {
            return;
        }

        final TextView cloverVersionPill =
                pillsPreference.findViewById(R.id.about_phone_clover_version_pill);
        final TextView androidVersionPill =
                pillsPreference.findViewById(R.id.about_phone_android_version_pill);

        if (cloverVersionPill != null) {
            cloverVersionPill.setText(buildCloverVersionPillText());
        }
        if (androidVersionPill != null) {
            androidVersionPill.setText(buildAndroidVersionPillText());
        }
    }

    private void bindInfoCards() {
        final LayoutPreference infoCardsPreference = findPreference(KEY_ABOUT_PHONE_INFO_CARDS);
        if (infoCardsPreference == null) {
            return;
        }

        bindInfoCardValue(infoCardsPreference, R.id.about_phone_device_name_value,
                buildDeviceNameCardText());
        bindInfoCardValue(infoCardsPreference, R.id.about_phone_memory_value,
                buildMemoryCardText());
        bindInfoCardValue(infoCardsPreference, R.id.about_phone_battery_value,
                buildBatteryCardText());
        bindInfoCardValue(infoCardsPreference, R.id.about_phone_resolution_value,
                buildResolutionCardText());

        final View deviceNameCard = infoCardsPreference.findViewById(
                R.id.about_phone_device_name_card);
        if (deviceNameCard != null) {
            deviceNameCard.setOnClickListener(v -> DeviceNameEditDialog.show(this));
        }
    }

    private void bindInfoCardValue(@NonNull LayoutPreference preference, int viewId,
            @NonNull CharSequence value) {
        final TextView valueView = preference.findViewById(viewId);
        if (valueView != null) {
            valueView.setText(value);
        }
    }

    static void removeSectionCardDecorations(@Nullable RecyclerView listView) {
        if (listView == null) {
            return;
        }
        for (int i = listView.getItemDecorationCount() - 1; i >= 0; i--) {
            listView.removeItemDecorationAt(i);
        }
    }

    private void bindBrandingCard() {
        final LayoutPreference brandingPreference = findPreference(KEY_ABOUT_PHONE_BRANDING);
        if (brandingPreference == null) {
            return;
        }

        final android.widget.ImageView wallpaperView =
                brandingPreference.findViewById(R.id.about_phone_brand_wallpaper);
        if (wallpaperView == null) {
            return;
        }

        final View scrimView = brandingPreference.findViewById(R.id.about_phone_brand_scrim);

        try {
            final android.app.WallpaperManager wm =
                    android.app.WallpaperManager.getInstance(requireContext());
            final android.graphics.drawable.Drawable wallpaper = wm.getDrawable();
            if (wallpaper != null) {
                wallpaperView.setImageDrawable(wallpaper);
                wallpaperView.setRenderEffect(
                        android.graphics.RenderEffect.createBlurEffect(
                                25f, 25f, android.graphics.Shader.TileMode.CLAMP));
            }
        } catch (Exception ignored) {
        }

        if (scrimView != null) {
            final boolean isDark =
                    (requireContext().getResources().getConfiguration().uiMode
                            & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                            == android.content.res.Configuration.UI_MODE_NIGHT_YES;
            scrimView.setBackgroundColor(isDark ? 0x99000000 : 0x66000000);
        }
    }

    static final class AboutPhonePreferenceGroupAdapter
            extends HighlightablePreferenceGroupAdapter {

        AboutPhonePreferenceGroupAdapter(
                @NonNull PreferenceScreen preferenceScreen, @Nullable String key) {
            super(preferenceScreen, key, false);
        }

        @Override
        public void onBindViewHolder(@NonNull PreferenceViewHolder holder, int position) {
            super.onBindViewHolder(holder, position);

            final Preference current = getItem(position);
            if (!isStandardAboutPhoneRow(current)) {
                return;
            }

            final Preference previous = findAdjacentPreference(position, -1);
            final Preference next = findAdjacentPreference(position, 1);
            final boolean isFirst = previous == null
                    || previous.getParent() != current.getParent();
            final boolean isLast = next == null
                    || next.getParent() != current.getParent();

            styleAboutPhoneRow(holder, isFirst, isLast);

            if (Boolean.TRUE.equals(holder.itemView.getTag(R.id.preference_highlighted))) {
                return;
            }

            final int backgroundRes;
            if (isFirst && isLast) {
                backgroundRes = R.drawable.about_phone_round_background;
            } else if (isFirst) {
                backgroundRes = R.drawable.about_phone_round_background_top;
            } else if (isLast) {
                backgroundRes = R.drawable.about_phone_round_background_bottom;
            } else {
                backgroundRes = R.drawable.about_phone_round_background_center;
            }

            final int paddingStart = ViewCompat.getPaddingStart(holder.itemView);
            final int paddingTop = holder.itemView.getPaddingTop();
            final int paddingEnd = ViewCompat.getPaddingEnd(holder.itemView);
            final int paddingBottom = holder.itemView.getPaddingBottom();
            holder.itemView.setBackgroundResource(backgroundRes);
            ViewCompat.setPaddingRelative(holder.itemView, paddingStart, paddingTop,
                    paddingEnd, paddingBottom);
        }

        private void styleAboutPhoneRow(@NonNull PreferenceViewHolder holder,
                boolean isFirst, boolean isLast) {
            final TextView titleView = (TextView) holder.findViewById(android.R.id.title);
            if (titleView != null) {
                titleView.setTypeface(Typeface.create("google-sans-flex", Typeface.BOLD));
                titleView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        titleView.getResources().getDimension(
                                R.dimen.about_phone_info_card_title_text_size));
            }

            final TextView summaryView = (TextView) holder.findViewById(android.R.id.summary);
            if (summaryView != null) {
                summaryView.setTypeface(Typeface.create("google-sans-flex", Typeface.NORMAL));
                summaryView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        summaryView.getResources().getDimension(
                                R.dimen.about_phone_info_card_value_text_size));
            }

            final android.content.res.Resources res = holder.itemView.getResources();
            final int hPad = res.getDimensionPixelSize(R.dimen.about_phone_row_padding_horizontal);
            final int outer = res.getDimensionPixelSize(R.dimen.about_phone_row_padding_outer);
            final int inner = res.getDimensionPixelSize(R.dimen.about_phone_row_padding_inner);
            final int vTop = isFirst ? outer : inner;
            final int vBottom = isLast ? outer : inner;
            ViewCompat.setPaddingRelative(holder.itemView, hPad, vTop, hPad, vBottom);
        }

        private boolean isStandardAboutPhoneRow(@Nullable Preference preference) {
            return preference != null
                    && !(preference instanceof LayoutPreference)
                    && !(preference instanceof PreferenceCategory)
                    && !isGroupDivider(preference);
        }

        @Nullable
        private Preference findAdjacentPreference(int position, int direction) {
            for (int index = position + direction; index >= 0 && index < getItemCount();
                    index += direction) {
                final Preference candidate = getItem(index);
                if (candidate == null || isGroupDivider(candidate)) {
                    return null;
                }
                return candidate;
            }
            return null;
        }
    }

    @NonNull
    private CharSequence buildCloverVersionPillText() {
        final String releaseType = getTitleCaseSystemProperty(PROP_CLOVER_RELEASE_TYPE, "Official");
        final String version = getSystemProperty(PROP_CLOVER_DISPLAY_VERSION,
                getSystemProperty(PROP_CLOVER_BUILD_VERSION, getString(R.string.device_info_default)));
        return getString(R.string.about_phone_clover_version_pill_format, releaseType,
                version.replaceFirst("^[vV]", ""));
    }

    @NonNull
    private CharSequence buildAndroidVersionPillText() {
        return getString(R.string.about_phone_android_version_pill_format,
                Build.VERSION.RELEASE_OR_CODENAME);
    }

    @NonNull
    static CharSequence buildMaintainerCardText(@NonNull Context context) {
        return getSystemProperty(PROP_CLOVER_MAINTAINER,
                context.getString(R.string.device_info_not_available));
    }

    @NonNull
    private CharSequence buildDeviceNameCardText() {
        final String deviceName = Settings.Global.getString(
                requireContext().getContentResolver(), Settings.Global.DEVICE_NAME);
        return deviceName != null ? deviceName : Build.MODEL;
    }

    private void bindMaintainerPreference() {
        bindMaintainerPreference(this);
    }

    static void bindMaintainerPreference(@NonNull DashboardFragment fragment) {
        final Preference maintainerPreference = fragment.findPreference(KEY_CLOVER_MAINTAINER);
        if (maintainerPreference != null) {
            maintainerPreference.setSummary(buildMaintainerCardText(fragment.requireContext()));
        }
    }

    @NonNull
    private CharSequence buildMemoryCardText() {
        final String ramText = formatStorageSize(Process.getAdvertisedMem());

        final StorageManager storageManager = requireContext().getSystemService(StorageManager.class);
        final String romText = storageManager == null
                ? getString(R.string.device_info_not_available)
                : formatStorageSize(storageManager.getPrimaryStorageSize());

        if (isNotAvailable(ramText) || isNotAvailable(romText)) {
            return getString(R.string.device_info_not_available);
        }

        return getString(R.string.about_phone_memory_card_value_format, ramText, romText);
    }

    @NonNull
    private CharSequence buildBatteryCardText() {
        final Intent batteryIntent = BatteryUtils.getBatteryIntent(requireContext());
        final int designCapacityUah = batteryIntent.getIntExtra(
                BatteryManager.EXTRA_DESIGN_CAPACITY, -1);
        if (designCapacityUah <= 0) {
            return getString(R.string.battery_design_capacity_not_available);
        }
        return getString(R.string.battery_design_capacity_summary, designCapacityUah / 1_000);
    }

    @NonNull
    private CharSequence buildResolutionCardText() {
        final DisplayManager displayManager = requireContext().getSystemService(DisplayManager.class);
        final Display display = displayManager == null
                ? null : displayManager.getDisplay(Display.DEFAULT_DISPLAY);
        if (display == null) {
            return getString(R.string.device_info_not_available);
        }

        final Display.Mode mode = display.getMode();
        final int shorterEdge = Math.min(mode.getPhysicalWidth(), mode.getPhysicalHeight());
        final int longerEdge = Math.max(mode.getPhysicalWidth(), mode.getPhysicalHeight());
        return getString(R.string.about_phone_resolution_card_value_format,
                shorterEdge, longerEdge);
    }

    @NonNull
    private String getTitleCaseSystemProperty(@NonNull String key, @NonNull String fallback) {
        final String resolved = getSystemProperty(key, fallback).trim().toLowerCase(Locale.ROOT);
        if (resolved.isEmpty()) {
            return fallback;
        }
        return Character.toUpperCase(resolved.charAt(0)) + resolved.substring(1);
    }

    @NonNull
    private static String getSystemProperty(@NonNull String key, @NonNull String fallback) {
        final String value = SystemProperties.get(key);
        return value == null || value.isBlank() ? fallback : value;
    }

    @NonNull
    private String formatStorageSize(long bytes) {
        if (bytes <= 0) {
            return getString(R.string.device_info_not_available);
        }

        final long terabyte = 1_000_000_000_000L;
        final long gigabyte = 1_000_000_000L;
        if (bytes >= terabyte && bytes % terabyte == 0) {
            return (bytes / terabyte) + " TB";
        }
        return (bytes / gigabyte) + " GB";
    }

    private boolean isNotAvailable(@NonNull String value) {
        return getString(R.string.device_info_not_available).contentEquals(value);
    }

    @Override
    public void showDeviceNameWarningDialog(String deviceName) {
        mDeviceInfoViewModel.setDeviceName(deviceName);
        DeviceNameWarningDialog.show(this);
    }

    CharSequence getCurrentDeviceName() {
        return buildDeviceNameCardText();
    }

    boolean isDeviceNameValid(@NonNull String deviceName) {
        return !deviceName.isBlank()
                && use(DeviceNamePreferenceController.class).isTextValid(deviceName);
    }

    void onDeviceNameEditSubmitted(@NonNull String deviceName) {
        if (!isCatalystEnabled() || !Flags.catalystAboutPhoneDeviceName()) {
            use(DeviceNamePreferenceController.class).setPendingDeviceName(deviceName);
        }
        showDeviceNameWarningDialog(deviceName);
    }

    public void onSetDeviceNameConfirm(boolean confirm) {
        if (!isCatalystEnabled() || !Flags.catalystAboutPhoneDeviceName()) {
            final DeviceNamePreferenceController controller = use(
                    DeviceNamePreferenceController.class);
            controller.updateDeviceName(confirm);
        } else {
            if (confirm) {
                final String deviceName = mDeviceInfoViewModel.getDeviceName();
                if (deviceName != null) {
                    UtilsKt.updateDeviceName(getActivity(), deviceName);
                }
            }
        }
        mDeviceInfoViewModel.clearDeviceNme();
        refreshDeviceNameCard();
    }

    private void refreshDeviceNameCard() {
        final LayoutPreference infoCardsPreference = findPreference(KEY_ABOUT_PHONE_INFO_CARDS);
        if (infoCardsPreference == null) {
            return;
        }
        bindInfoCardValue(infoCardsPreference, R.id.about_phone_device_name_value,
                buildDeviceNameCardText());
    }

    @Override
    public @Nullable String getPreferenceScreenBindingKey(@NonNull Context context) {
        // The redesigned main page is intentionally curated in XML. The existing Catalyst
        // metadata hierarchy still describes the legacy full About phone page and includes
        // moved rows such as IMEI, so hybrid binding would initialize missing preferences.
        return null;
    }

    /**
     * For Search.
     */
    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.my_device_info) {

                @Override
                public List<AbstractPreferenceController> createPreferenceControllers(
                        Context context) {
                    return buildMainPreferenceControllers(context, null /* lifecycle */);
                }
            };
}
