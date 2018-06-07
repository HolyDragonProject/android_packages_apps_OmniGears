/*
 *  Copyright (C) 2018 The OmniROM Project
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.omnirom.omnigears;

import com.android.settings.SettingsPreferenceFragment;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.provider.Settings;
import android.provider.SearchIndexableResource;

import org.omnirom.omnilib.preference.ColorSelectPreference;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;

import java.util.List;
import java.util.Arrays;

public class OmniClockSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener, Indexable {
    private static final String TAG = "OmniClockSettings";
    private static final String KEY_BG_COLOR = "bg_color";
    private static final String KEY_BORDER_COLOR = "border_color";
    private static final String KEY_HOUR_COLOR = "hour_color";
    private static final String KEY_MINUTE_COLOR = "minute_color";
    private static final String KEY_TEXT_COLOR = "text_color";
    private static final String KEY_ACCENT_COLOR = "accent_color";

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.OMNI_SETTINGS;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.omni_clock_settings);

        PreferenceScreen prefSet = getPreferenceScreen();
        ContentResolver resolver = getContentResolver();
        Resources r = getResources();

        initColorPreference(KEY_BG_COLOR, Settings.System.LOCKSCREEN_OMNI_CLOCK_BG_COLOR,
                r.getColor(R.color.omni_clock_bg_color));
        initColorPreference(KEY_BORDER_COLOR, Settings.System.LOCKSCREEN_OMNI_CLOCK_BORDER_COLOR,
                r.getColor(R.color.omni_clock_primary));
        initColorPreference(KEY_HOUR_COLOR, Settings.System.LOCKSCREEN_OMNI_CLOCK_HOUR_COLOR,
                r.getColor(R.color.omni_clock_hour_hand_color));
        initColorPreference(KEY_MINUTE_COLOR, Settings.System.LOCKSCREEN_OMNI_CLOCK_MINUTE_COLOR,
                r.getColor(R.color.omni_clock_minute_hand_color));
        initColorPreference(KEY_TEXT_COLOR, Settings.System.LOCKSCREEN_OMNI_CLOCK_TEXT_COLOR,
                r.getColor(R.color.omni_clock_text_color));
        initColorPreference(KEY_ACCENT_COLOR, Settings.System.LOCKSCREEN_OMNI_CLOCK_ACCENT_COLOR,
                r.getColor(R.color.omni_clock_accent));
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        return super.onPreferenceTreeClick(preference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        if (preference instanceof ColorSelectPreference) {
            ContentResolver resolver = getContentResolver();
            ColorSelectPreference c = (ColorSelectPreference) preference;
            String hexColor = String.format("#%08X", c.getColor());
            preference.setSummary(hexColor);
            String settingsKey = preference.getKey();
            Settings.System.putInt(resolver, settingsKey, c.getColor());
            return true;
        }
        return true;
    }

    private void initColorPreference(String key, String settingsKey, int defaultValue) {
        ContentResolver resolver = getContentResolver();
        ColorSelectPreference c = (ColorSelectPreference) findPreference(key);
        c.setKey(settingsKey);
        int color = Settings.System.getInt(resolver, settingsKey, defaultValue);
        c.setColor(color);
        String hexColor = String.format("#%08X", color);
        c.setSummary(hexColor);
        c.setOnPreferenceChangeListener(this);
    }

    /**
     * For Search.
     */
    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
        new BaseSearchIndexProvider() {

            @Override
            public List<SearchIndexableResource> getXmlResourcesToIndex(
                    Context context, boolean enabled) {
                final SearchIndexableResource sir = new SearchIndexableResource(context);
                sir.xmlResId = R.xml.omni_clock_settings;
                return Arrays.asList(sir);
            }
	};
}

