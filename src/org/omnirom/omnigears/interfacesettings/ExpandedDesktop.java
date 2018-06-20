/*
 * Copyright (C) 2015 The CyanogenMod Project
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

package org.omnirom.omnigears.interfacesettings;

import android.annotation.Nullable;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManagerGlobal;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SectionIndexer;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.view.WindowManagerPolicyControl;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.widget.SwitchBar;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.applications.ApplicationsState.AppEntry;

import com.android.settings.Utils;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExpandedDesktop extends SettingsPreferenceFragment implements
        ApplicationsState.Callbacks, SwitchBar.OnSwitchChangeListener {

    private static final int STATE_DISABLED = 0;
    private static final int STATE_STATUS_HIDDEN = 1;
    private static final int STATE_NAVIGATION_HIDDEN = 2;
    private static final int STATE_BOTH_HIDDEN = 3;

    private static final int STATE_ENABLE_FOR_ALL = 0;
    private static final int STATE_USER_CONFIGURABLE = 1;

    private AllPackagesAdapter mAllPackagesAdapter;
    private ApplicationsState mApplicationsState;
    private View mEmptyView;
    private View mProgressBar;
    private ListView mUserListView;
    private ApplicationsState.Session mSession;
    private ActivityFilter mActivityFilter;
    private Map<String, ApplicationsState.AppEntry> mEntryMap =
            new HashMap<String, ApplicationsState.AppEntry>();
    private int mExpandedDesktopState;
    private SwitchBar mSwitchBar;
    private boolean mOnlyLauncher = true;
    private MenuItem mMenuItem;

    private int getExpandedDesktopState(ContentResolver cr) {
        boolean enableForAll = Settings.Global.getInt(getContentResolver(),
                Settings.Global.OVERRIDE_POLICY_CONTROL, 0) == 1;
        if (enableForAll) {
            return STATE_ENABLE_FOR_ALL;
        }
        String value = Settings.Global.getString(cr, Settings.Global.POLICY_CONTROL);
        if ("immersive.full=*".equals(value)) {
            return STATE_ENABLE_FOR_ALL;
        }
        return STATE_USER_CONFIGURABLE;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mApplicationsState = ApplicationsState.getInstance(getActivity().getApplication());
        mSession = mApplicationsState.newSession(this);
        mSession.resume();
        mActivityFilter = new ActivityFilter(getActivity().getPackageManager());

        mExpandedDesktopState = getExpandedDesktopState(getActivity().getContentResolver());
        if (mExpandedDesktopState == STATE_USER_CONFIGURABLE) {
            WindowManagerPolicyControl.reloadFromSetting(getActivity(),
                    Settings.Global.POLICY_CONTROL);
        }
        mAllPackagesAdapter = new AllPackagesAdapter(getActivity());

        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        rebuild();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.expanded_desktop_prefs, container, false);
        mUserListView = (ListView) view.findViewById(R.id.user_list_view);
        mUserListView.setAdapter(mAllPackagesAdapter);
        mUserListView.setFastScrollEnabled(true);

        mSwitchBar = ((SettingsActivity) getActivity()).getSwitchBar();
        mSwitchBar.setOnStateOffLabel(R.string.expanded_enabled_for_all);
        mSwitchBar.setOnStateOnLabel(R.string.expanded_enabled_for_all);
        mSwitchBar.show();

        mEmptyView = view.findViewById(R.id.nothing_to_show);
        mProgressBar = view.findViewById(R.id.progress_bar);

        if (mExpandedDesktopState == STATE_USER_CONFIGURABLE) {
            mSwitchBar.setChecked(false);
            showListView();
        } else {
            mSwitchBar.setChecked(true);
            hideListView();
        }
        mSwitchBar.addOnSwitchChangeListener(this);
        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        save();
        mSession.pause();
        mSession.release();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mSwitchBar != null) {
            mSwitchBar.removeOnSwitchChangeListener(this);
        }
    }

    private void enableForAll() {
        mExpandedDesktopState = STATE_ENABLE_FOR_ALL;
        //writeValue("immersive.full=*");
        Settings.Global.putInt(getContentResolver(),
                    Settings.Global.OVERRIDE_POLICY_CONTROL, 1);
        mAllPackagesAdapter.notifyDataSetChanged();
        hideListView();
    }

    private void userConfigurableSettings() {
        mExpandedDesktopState = STATE_USER_CONFIGURABLE;
        //writeValue("");
        Settings.Global.putInt(getContentResolver(),
                    Settings.Global.OVERRIDE_POLICY_CONTROL, 0);
        WindowManagerPolicyControl.reloadFromSetting(getActivity());
        mAllPackagesAdapter.notifyDataSetChanged();
        showListView();
    }

    private void hideListView() {
        mUserListView.setVisibility(View.GONE);
        mEmptyView.setVisibility(View.VISIBLE);
    }

    private void showListView() {
        mUserListView.setVisibility(View.VISIBLE);
        mEmptyView.setVisibility(View.GONE);
    }

    /*private void writeValue(String value) {
        Settings.Global.putString(getContentResolver(), Settings.Global.POLICY_CONTROL, value);
    }*/

    private static int getStateForPackage(String packageName) {
        int state = STATE_DISABLED;

        if (WindowManagerPolicyControl.immersiveStatusFilterMatches(packageName)) {
            state = STATE_STATUS_HIDDEN;
        }
        if (WindowManagerPolicyControl.immersiveNavigationFilterMatches(packageName)) {
            if (state == STATE_DISABLED) {
                state = STATE_NAVIGATION_HIDDEN;
            } else {
                state = STATE_BOTH_HIDDEN;
            }
        }

        return state;
    }

    @Override
    public void onRunningStateChanged(boolean running) {
    }

    @Override
    public void onPackageListChanged() {
        mActivityFilter.updateLauncherInfoList();
        rebuild();
    }

    @Override
    public void onRebuildComplete(ArrayList<ApplicationsState.AppEntry> entries) {
        if (entries != null) {
            handleAppEntries(entries);
            mAllPackagesAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onPackageIconChanged() {
    }

    @Override
    public void onPackageSizeChanged(String packageName) {
    }

    @Override
    public void onAllSizesComputed() {
    }

    private void handleAppEntries(List<ApplicationsState.AppEntry> entries) {
        String lastSectionIndex = null;
        ArrayList<String> sections = new ArrayList<String>();
        ArrayList<Integer> positions = new ArrayList<Integer>();
        PackageManager pm = getPackageManager();
        int count = entries.size(), offset = 0;

        for (int i = 0; i < count; i++) {
            ApplicationInfo info = entries.get(i).info;
            String label = (String) info.loadLabel(pm);
            String sectionIndex;

            if (!info.enabled) {
                sectionIndex = "--";
            } else if (TextUtils.isEmpty(label)) {
                sectionIndex = "";
            } else {
                sectionIndex = label.substring(0, 1).toUpperCase();
            }

            if (lastSectionIndex == null ||
                    !TextUtils.equals(sectionIndex, lastSectionIndex)) {
                sections.add(sectionIndex);
                positions.add(offset);
                lastSectionIndex = sectionIndex;
            }
            offset++;
        }

        mAllPackagesAdapter.setEntries(entries, sections, positions);
        mEntryMap.clear();
        for (ApplicationsState.AppEntry e : entries) {
            mEntryMap.put(e.info.packageName, e);
        }
    }

    private void rebuild() {
        mSession.rebuild(mActivityFilter, ApplicationsState.ALPHA_COMPARATOR);
    }

    private void save() {
        if (mExpandedDesktopState == STATE_USER_CONFIGURABLE) {
            WindowManagerPolicyControl.saveToSettings(getActivity(),
                    Settings.Global.POLICY_CONTROL);
        }
    }

    int getStateDrawable(int state) {
        switch (state) {
            case STATE_STATUS_HIDDEN:
                return R.drawable.ic_expanded_desktop_hidestatusbar;
            case STATE_NAVIGATION_HIDDEN:
                return R.drawable.ic_expanded_desktop_hidenavbar;
            case STATE_BOTH_HIDDEN:
                return R.drawable.ic_expanded_desktop_hideboth;
            case STATE_DISABLED:
            default:
                return R.drawable.ic_expanded_desktop_hidenone;
        }
    }

    @Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        if (isChecked) {
            enableForAll();
        } else {
            userConfigurableSettings();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.expanded_desktop_menu, menu);
        mMenuItem = menu.findItem(R.id.show_all_apps);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.show_all_apps:
            mOnlyLauncher = !mOnlyLauncher;
            mActivityFilter.updateLauncherInfoList();
            rebuild();
            break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPrepareOptionsMenu (Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (mMenuItem != null) {
            mMenuItem.setTitle(mOnlyLauncher ? getResources().getString(R.string.show_all_apps_menu)
                    : getResources().getString(R.string.show_only_launcher_menu));
        }
    }

    private class AllPackagesAdapter extends BaseAdapter
            implements AdapterView.OnItemSelectedListener, SectionIndexer {

        private final LayoutInflater inflater;
        private List<ApplicationsState.AppEntry> entries = new ArrayList<>();
        private final ModeAdapter mModesAdapter;
        private String[] mSections;
        private int[] mPositions;

        public AllPackagesAdapter(Context context) {
            this.inflater = LayoutInflater.from(context);
            mModesAdapter = new ModeAdapter(context);
            mActivityFilter = new ActivityFilter(context.getPackageManager());
        }

        @Override
        public int getCount() {
            return entries.size();
        }

        @Override
        public Object getItem(int position) {
            return entries.get(position);
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public long getItemId(int position) {
            return entries.get(position).id;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                holder = new ViewHolder(inflater.inflate(R.layout.expanded_item, parent, false));
                holder.mode.setAdapter(mModesAdapter);
                holder.mode.setOnItemSelectedListener(this);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            ApplicationsState.AppEntry entry = entries.get(position);

            if (entry == null) {
                return holder.rootView;
            }

            holder.title.setText(entry.label);
            mApplicationsState.ensureIcon(entry);
            holder.icon.setImageDrawable(entry.icon);
            holder.mode.setSelection(getStateForPackage(entry.info.packageName), false);
            holder.mode.setTag(entry);
            holder.stateIcon.setImageResource(getStateDrawable(
                    getStateForPackage(entry.info.packageName)));
            return holder.rootView;
        }

        private void setEntries(List<ApplicationsState.AppEntry> entries,
                List<String> sections, List<Integer> positions) {
            this.entries = entries;
            mSections = sections.toArray(new String[sections.size()]);
            mPositions = new int[positions.size()];
            for (int i = 0; i < positions.size(); i++) {
                mPositions[i] = positions.get(i);
            }
            notifyDataSetChanged();
        }


        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            ApplicationsState.AppEntry entry = (ApplicationsState.AppEntry) parent.getTag();

            WindowManagerPolicyControl.removeFromWhiteLists(entry.info.packageName);
            switch (position) {
                case STATE_STATUS_HIDDEN:
                    WindowManagerPolicyControl.addToStatusWhiteList(entry.info.packageName);
                    break;
                case STATE_NAVIGATION_HIDDEN:
                    WindowManagerPolicyControl.addToNavigationWhiteList(entry.info.packageName);
                    break;
                case STATE_BOTH_HIDDEN:
                    WindowManagerPolicyControl.addToStatusWhiteList(entry.info.packageName);
                    WindowManagerPolicyControl.addToNavigationWhiteList(entry.info.packageName);
                    break;
            }
            save();
            notifyDataSetChanged();
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }

        @Override
        public int getPositionForSection(int section) {
            if (section < 0 || section >= mSections.length) {
                return -1;
            }

            return mPositions[section];
        }

        @Override
        public int getSectionForPosition(int position) {
            if (position < 0 || position >= getCount()) {
                return -1;
            }

            int index = Arrays.binarySearch(mPositions, position);

        /*
         * Consider this example: section positions are 0, 3, 5; the supplied
         * position is 4. The section corresponding to position 4 starts at
         * position 3, so the expected return value is 1. Binary search will not
         * find 4 in the array and thus will return -insertPosition-1, i.e. -3.
         * To get from that number to the expected value of 1 we need to negate
         * and subtract 2.
         */
            return index >= 0 ? index : -index - 2;
        }

        @Override
        public Object[] getSections() {
            return mSections;
        }
    }

    private static class ViewHolder {
        private TextView title;
        private Spinner mode;
        private ImageView icon;
        private View rootView;
        private ImageView stateIcon;

        private ViewHolder(View view) {
            this.title = (TextView) view.findViewById(R.id.app_name);
            this.mode = (Spinner) view.findViewById(R.id.app_mode);
            this.icon = (ImageView) view.findViewById(R.id.app_icon);
            this.stateIcon = (ImageView) view.findViewById(R.id.state);
            this.rootView = view;

            view.setTag(this);
        }
    }

    private static class ModeAdapter extends BaseAdapter {

        private final LayoutInflater inflater;
        private boolean hasNavigationBar = true;
        private final int[] items = {R.string.expanded_hide_nothing, R.string.expanded_hide_status,
                R.string.expanded_hide_navigation, R.string.expanded_hide_both};

        private ModeAdapter(Context context) {
            inflater = LayoutInflater.from(context);

            try {
                hasNavigationBar = WindowManagerGlobal.getWindowManagerService().hasNavigationBar();
            } catch (RemoteException e) {
                // Do nothing
            }
        }

        @Override
        public int getCount() {
            return hasNavigationBar ? 4 : 2;
        }

        @Override
        public Object getItem(int position) {
            return items[position];
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView view;
            if (convertView != null) {
                view = (TextView) convertView;
            } else {
                view = (TextView) inflater.inflate(android.R.layout.simple_spinner_dropdown_item,
                        parent, false);
            }

            view.setText(items[position]);

            return view;
        }
    }

    private class ActivityFilter implements ApplicationsState.AppFilter {

        private final PackageManager mPackageManager;
        private final List<String> launcherResolveInfoList = new ArrayList<String>();

        private ActivityFilter(PackageManager packageManager) {
            this.mPackageManager = packageManager;

            updateLauncherInfoList();
        }

        public void updateLauncherInfoList() {
            Intent i = new Intent(Intent.ACTION_MAIN);
            i.addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> resolveInfoList = mPackageManager.queryIntentActivities(i, 0);

            synchronized (launcherResolveInfoList) {
                launcherResolveInfoList.clear();
                for (ResolveInfo ri : resolveInfoList) {
                    launcherResolveInfoList.add(ri.activityInfo.packageName);
                }
            }
        }

        @Override
        public void init() {
        }

        @Override
        public boolean filterApp(AppEntry info) {
            boolean show = !mAllPackagesAdapter.entries.contains(info.info.packageName);
            if (show && mOnlyLauncher) {
                synchronized (launcherResolveInfoList) {
                    show = launcherResolveInfoList.contains(info.info.packageName);
                }
            }
            return show;
        }
    }

    @Override
    public void onLauncherInfoChanged() {
    }

    @Override
    public void onLoadEntriesCompleted() {
        rebuild();
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.OMNI_SETTINGS;
    }

    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                        boolean enabled) {
                    ArrayList<SearchIndexableResource> result =
                            new ArrayList<SearchIndexableResource>();

                    SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.network_traffic;
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
