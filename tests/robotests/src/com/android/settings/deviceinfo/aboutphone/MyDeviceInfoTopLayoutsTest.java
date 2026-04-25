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

import static com.google.common.truth.Truth.assertThat;
import static org.robolectric.Shadows.shadowOf;

import android.content.Context;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settingslib.widget.SettingsThemeHelper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.androidx.fragment.FragmentController;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {MyDeviceInfoTopLayoutsTest.ShadowSettingsThemeHelper.class})
public class MyDeviceInfoTopLayoutsTest {

    private static final String KEY_ABOUT_PHONE_BRANDING = "about_phone_branding";
    private static final String KEY_ABOUT_PHONE_META_PILLS = "about_phone_meta_pills";
    private static final String KEY_ABOUT_PHONE_INFO_CARDS = "about_phone_info_cards";
    private static final String KEY_BASIC_INFO_CATEGORY = "basic_info_category";
    private static final String KEY_CLOVER_MAINTAINER = "clover_maintainer";
    private static final String KEY_UP_TIME = "up_time";
    private static final String KEY_BUILD_NUMBER = "build_number";
    private static final String KEY_MORE_DEVICE_INFO = "more_device_info";
    private static final String KEY_LEGAL_CATEGORY = "legal_category";
    private static final String KEY_DEVICE_DETAIL_CATEGORY = "device_detail_category";
    private static final String KEY_DEVICE_IDENTIFIERS_CATEGORY = "device_identifiers_category";
    private static final String KEY_DEVICE_NAME = "device_name";
    private static final String KEY_MY_DEVICE_INFO_HEADER = "my_device_info_header";

    @Before
    public void setUp() {
        ShadowSettingsThemeHelper.setExpressiveTheme(false);
    }

    @Test
    public void onCreate_brandingCardIsFirstPreference() {
        final MyDeviceInfoFragment fragment = FragmentController.of(new MyDeviceInfoFragment())
                .create()
                .start()
                .resume()
                .get();

        final PreferenceScreen screen = fragment.getPreferenceScreen();

        assertThat(screen.getPreference(0).getKey()).isEqualTo(KEY_ABOUT_PHONE_BRANDING);
    }

    @Test
    public void onCreate_versionPillsAreSecondPreference() {
        final MyDeviceInfoFragment fragment = FragmentController.of(new MyDeviceInfoFragment())
                .create()
                .start()
                .resume()
                .get();

        final PreferenceScreen screen = fragment.getPreferenceScreen();

        assertThat(screen.getPreference(1).getKey()).isEqualTo(KEY_ABOUT_PHONE_META_PILLS);
    }

    @Test
    public void onCreate_summaryCardsAreThirdPreference() {
        final MyDeviceInfoFragment fragment = FragmentController.of(new MyDeviceInfoFragment())
                .create()
                .start()
                .resume()
                .get();

        final PreferenceScreen screen = fragment.getPreferenceScreen();

        assertThat(screen.getPreference(2).getKey()).isEqualTo(KEY_ABOUT_PHONE_INFO_CARDS);
    }

    @Test
    public void onCreate_userHeaderPreferenceIsAbsent() {
        final MyDeviceInfoFragment fragment = FragmentController.of(new MyDeviceInfoFragment())
                .create()
                .start()
                .resume()
                .get();

        assertThat((Object) fragment.getPreferenceScreen().findPreference(KEY_MY_DEVICE_INFO_HEADER))
                .isNull();
    }

    @Test
    public void onCreate_basicInfoCategoryDoesNotContainDeviceNamePreference() {
        final MyDeviceInfoFragment fragment = FragmentController.of(new MyDeviceInfoFragment())
                .create()
                .start()
                .resume()
                .get();

        final PreferenceScreen screen = fragment.getPreferenceScreen();
        final PreferenceCategory basicInfoCategory = screen.findPreference(KEY_BASIC_INFO_CATEGORY);

        assertThat(basicInfoCategory).isNotNull();
        assertThat((Object) basicInfoCategory.findPreference(KEY_DEVICE_NAME)).isNull();
    }

    @Test
    public void onCreate_mainPageShowsOnlyCuratedRowsBelowTopCards() {
        final MyDeviceInfoFragment fragment = FragmentController.of(new MyDeviceInfoFragment())
                .create()
                .start()
                .resume()
                .get();

        final PreferenceScreen screen = fragment.getPreferenceScreen();
        final PreferenceCategory basicInfoCategory = screen.findPreference(KEY_BASIC_INFO_CATEGORY);

        assertThat(basicInfoCategory).isNotNull();
        assertThat((Object) basicInfoCategory.findPreference(KEY_CLOVER_MAINTAINER)).isNotNull();
        assertThat((Object) basicInfoCategory.findPreference(KEY_UP_TIME)).isNotNull();
        assertThat((Object) basicInfoCategory.findPreference(KEY_BUILD_NUMBER)).isNotNull();
        assertThat((Object) screen.findPreference(KEY_MORE_DEVICE_INFO)).isNotNull();
        assertThat((Object) screen.findPreference(KEY_LEGAL_CATEGORY)).isNull();
        assertThat((Object) screen.findPreference(KEY_DEVICE_DETAIL_CATEGORY)).isNull();
        assertThat((Object) screen.findPreference(KEY_DEVICE_IDENTIFIERS_CATEGORY)).isNull();
    }

    @Test
    public void moreDeviceInfoFragment_usesDetailedDeviceInfoScreen() {
        final MoreDeviceInfoFragment fragment = FragmentController.of(new MoreDeviceInfoFragment())
                .create()
                .start()
                .resume()
                .get();

        final PreferenceScreen screen = fragment.getPreferenceScreen();

        assertThat(fragment.getPreferenceScreenResId()).isEqualTo(R.xml.more_device_info);
        assertThat((Object) screen.findPreference(KEY_BASIC_INFO_CATEGORY)).isNotNull();
        assertThat((Object) screen.findPreference(KEY_LEGAL_CATEGORY)).isNotNull();
        assertThat((Object) screen.findPreference(KEY_DEVICE_DETAIL_CATEGORY)).isNotNull();
        assertThat((Object) screen.findPreference(KEY_DEVICE_IDENTIFIERS_CATEGORY)).isNotNull();
    }

    @Test
    public void onCreate_expressiveTheme_removesRowSpacingDecoration() {
        ShadowSettingsThemeHelper.setExpressiveTheme(true);

        final MyDeviceInfoFragment fragment = FragmentController.of(new MyDeviceInfoFragment())
                .create()
                .start()
                .resume()
                .get();

        final RecyclerView recyclerView = fragment.getListView();

        assertThat(recyclerView.getItemDecorationCount()).isEqualTo(0);
    }

    @Test
    public void onCreate_expressiveTheme_usesAboutPhoneAdapter() {
        ShadowSettingsThemeHelper.setExpressiveTheme(true);

        final MyDeviceInfoFragment fragment = FragmentController.of(new MyDeviceInfoFragment())
                .create()
                .start()
                .resume()
                .get();

        assertThat(fragment.getListView().getAdapter().getClass().getName())
                .contains("AboutPhonePreferenceGroupAdapter");
    }

    @Test
    public void brandingCardLayout_inflatesWordmarkCard() {
        final View brandingView = LayoutInflater.from(getSubSettingsContext())
                .inflate(R.layout.about_phone_brand_card, null);

        assertThat((Object) brandingView.findViewById(R.id.about_phone_brand_card))
                .isInstanceOf(CardView.class);
        assertThat(((TextView) brandingView.findViewById(R.id.about_phone_brand_prefix))
                .getText()
                .toString()).isEqualTo("CL");
        assertThat(((TextView) brandingView.findViewById(R.id.about_phone_brand_suffix))
                .getText()
                .toString()).isEqualTo("VER PROJECT");
    }

    @Test
    public void versionPillsLayout_inflatesInlineCardViews() {
        final View pillsView = LayoutInflater.from(getSubSettingsContext())
                .inflate(R.layout.about_phone_meta_pills, null);

        assertThat(pillsView).isInstanceOf(LinearLayout.class);
        assertThat(((LinearLayout) pillsView).getOrientation()).isEqualTo(LinearLayout.HORIZONTAL);
        assertThat(((View) pillsView.findViewById(R.id.about_phone_clover_version_pill).getParent()))
                .isInstanceOf(CardView.class);
        assertThat(((View) pillsView.findViewById(R.id.about_phone_android_version_pill).getParent()))
                .isInstanceOf(CardView.class);
    }

    @Test
    public void infoCardsLayout_inflatesFourCards() {
        final View infoCardsView = LayoutInflater.from(getSubSettingsContext())
                .inflate(R.layout.about_phone_info_cards, null);

        assertThat((Object) infoCardsView.findViewById(R.id.about_phone_device_name_card))
                .isInstanceOf(CardView.class);
        assertThat((Object) infoCardsView.findViewById(R.id.about_phone_memory_card))
                .isInstanceOf(CardView.class);
        assertThat((Object) infoCardsView.findViewById(R.id.about_phone_battery_card))
                .isInstanceOf(CardView.class);
        assertThat((Object) infoCardsView.findViewById(R.id.about_phone_resolution_card))
                .isInstanceOf(CardView.class);
    }

    @Test
    public void infoCardsLayout_usesMaterialTonedIcons() {
        final View infoCardsView = LayoutInflater.from(getSubSettingsContext())
                .inflate(R.layout.about_phone_info_cards, null);

        assertInfoCardIcon(infoCardsView, "about_phone_device_name_icon",
                "ic_about_phone_mobile_3");
        assertInfoCardIcon(infoCardsView, "about_phone_memory_icon",
                "ic_about_phone_memory");
        assertInfoCardIcon(infoCardsView, "about_phone_battery_icon",
                "ic_about_phone_battery_android_5");
        assertInfoCardIcon(infoCardsView, "about_phone_resolution_icon",
                "ic_about_phone_aspect_ratio");
    }

    private Context getSubSettingsContext() {
        return new ContextThemeWrapper(
                ApplicationProvider.getApplicationContext(),
                R.style.Theme_SubSettings);
    }

    private void assertInfoCardIcon(@NonNull View root, @NonNull String iconIdName,
            @NonNull String drawableName) {
        final Context context = root.getContext();
        final int iconId = context.getResources().getIdentifier(iconIdName, "id",
                context.getPackageName());
        final int drawableId = context.getResources().getIdentifier(drawableName, "drawable",
                context.getPackageName());

        assertThat(iconId).isNotEqualTo(0);
        assertThat(drawableId).isNotEqualTo(0);

        final ImageView iconView = root.findViewById(iconId);

        assertThat((Object) iconView).isInstanceOf(ImageView.class);
        assertThat(shadowOf(iconView.getDrawable()).getCreatedFromResId()).isEqualTo(drawableId);
        assertThat(iconView.getImageTintList()).isNotNull();
    }

    @Implements(SettingsThemeHelper.class)
    public static class ShadowSettingsThemeHelper {
        private static boolean sIsExpressiveTheme;

        @Implementation
        public static boolean isExpressiveTheme(@NonNull Context context) {
            return sIsExpressiveTheme;
        }

        static void setExpressiveTheme(boolean isExpressiveTheme) {
            sIsExpressiveTheme = isExpressiveTheme;
        }
    }
}
