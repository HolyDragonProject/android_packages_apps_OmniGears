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
 *
*/
package org.omnirom.omnigears.interfacesettings;

import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import android.provider.SearchIndexableResource;
import android.provider.Settings;

import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;

import java.util.List;
import java.util.ArrayList;

public class LockscreenSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener, Indexable {
    private static final String TAG = "LockscreenSettings";
    private static final String LOCKSCREEN_CLOCK_STYLE = "lockscreen_clock_style";

    private ListPreference mLockscreenClockStyle;

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.OMNI_SETTINGS;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.lockscreen_settings);
        final ContentResolver resolver = getContentResolver();

        mLockscreenClockStyle = (ListPreference) findPreference(LOCKSCREEN_CLOCK_STYLE);
        int clockStyle = Settings.System.getInt(resolver,
                Settings.System.LOCKSCREEN_CLOCK_STYLE, 0);

        mLockscreenClockStyle.setValue(Integer.toString(clockStyle));
        mLockscreenClockStyle.setSummary(mLockscreenClockStyle.getEntry());
        mLockscreenClockStyle.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        return super.onPreferenceTreeClick(preference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mLockscreenClockStyle) {
            int value = Integer.valueOf((String) newValue);
            int index = mLockscreenClockStyle.findIndexOfValue((String) newValue);
            mLockscreenClockStyle.setSummary(mLockscreenClockStyle.getEntries()[index]);
            Settings.System.putInt(getContentResolver(), Settings.System.LOCKSCREEN_CLOCK_STYLE, value);
            return true;
        }
        return false;
    }

    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                        boolean enabled) {
                    ArrayList<SearchIndexableResource> result =
                            new ArrayList<SearchIndexableResource>();

                    SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.lockscreen_settings;
                    result.add(sir);

                    return result;
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    ArrayList<String> result = new ArrayList<String>();
                    return result;
                }
            };
}
