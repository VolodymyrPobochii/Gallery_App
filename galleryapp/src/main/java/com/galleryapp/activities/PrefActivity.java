/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.galleryapp.activities;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;

import com.galleryapp.R;
import com.galleryapp.application.GalleryApp;

/**
 * This activity is an example of a simple settings screen that has default
 * values.
 * <p/>
 * In order for the default values to be populated into the
 * {@link android.content.SharedPreferences} (from the preferences XML file), the client must
 * call
 * {@link android.preference.PreferenceManager#setDefaultValues(android.content.Context, int, boolean)}.
 * <p/>
 * This should be called early, typically when the application is first created.
 * An easy way to do this is to have a common function for retrieving the
 * SharedPreferences that takes care of calling it.
 */
public class PrefActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = PrefActivity.class.getSimpleName();
    // This is the global (to the .apk) name under which we store these
    // preferences.  We want this to be unique from other preferences so that
    // we do not have unexpected name conflicts, and the framework can correctly
    // determine whether these preferences' defaults have already been written.
    public static final String PREFS_NAME = "defaults";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getPrefs(this).registerOnSharedPreferenceChangeListener(this);
        getPreferenceManager().setSharedPreferencesName(PREFS_NAME);
        addPreferencesFromResource(R.xml.default_values);

        initViews();
        updatePreffSummaries();
    }


    private void initViews() {
        findPreference("hostName").setTitle(getString(R.string.title_host_preference));
        findPreference("hostName").setDefaultValue(getString(R.string.default_value_host_preference));

        findPreference("port").setTitle(getString(R.string.title_port_preference));
        findPreference("port").setDefaultValue(getString(R.string.default_value_port_preference));

        findPreference("username").setTitle(getString(R.string.title_username_preference));
        findPreference("username").setDefaultValue(getString(R.string.default_value_username_preference));

        findPreference("password").setTitle(getString(R.string.title_password_preference));
        findPreference("password").setDefaultValue(getString(R.string.default_value_password_preference));

        findPreference("updateTimes").setTitle(getString(R.string.update_times_preference));
        findPreference("updateTimes").setDefaultValue(getString(R.string.default_value_update_times_preference));

        findPreference("updateFreq").setTitle(getString(R.string.update_freq_preference));
        findPreference("updateFreq").setDefaultValue(getString(R.string.default_value_update_freq_preference));
    }

    private void updatePreffSummaries() {
        Log.d(TAG, "PreffCount = " + getPreferenceScreen().getPreferenceCount());

        bindPreferenceSummaryToValue(findPreference("hostName"));
        bindPreferenceSummaryToValue(findPreference("port"));
        bindPreferenceSummaryToValue(findPreference("username"));
        bindPreferenceSummaryToValue(findPreference("password"));
        bindPreferenceSummaryToValue(findPreference("updateTimes"));
        bindPreferenceSummaryToValue(findPreference("updateFreq"));
    }

    private void editPreff(final Context context) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                SharedPreferences sp = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                sp.edit()
                        .putString("hostName", findPreference("hostName").getSummary().toString())
                        .putString("port", findPreference("port").getSummary().toString())
                        .putString("username", findPreference("username").getSummary().toString())
                        .putString("password", findPreference("password").getSummary().toString())
                        .putString("updateTimes", findPreference("updateTimes").getSummary().toString())
                        .putString("updateFreq", findPreference("updateFreq").getSummary().toString())
                        .apply();
            }
        }).start();
    }

    public static SharedPreferences getPrefs(Context context) {
        PreferenceManager.setDefaultValues(context, PREFS_NAME, MODE_PRIVATE, R.xml.default_values, false);
        return context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                /*Intent upIntent = NavUtils.getParentActivityIntent(this);
                if (NavUtils.shouldUpRecreateTask(this, upIntent)) {
                    // This activity is NOT part of this app's task, so create a new task
                    // when navigating up, with a synthesized back stack.
                    TaskStackBuilder.create(this)
                            // Add all of this activity's parents to the back stack
                            .addNextIntentWithParentStack(upIntent)
                                    // Navigate up to the closest parent
                            .startActivities();
                } else {
                    // This activity is part of this app's task, so simply
                    // navigate up to the logical parent activity.
                    NavUtils.navigateUpTo(this, upIntent);
                }*/
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.d(TAG, "Changed : preff = " + sharedPreferences.toString() + " key = " + key);
        updatePreffSummaries();
        setResult(RESULT_OK);
    }

    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            Log.d(TAG, TAG + "::onPreferenceChange:preference");
            String stringValue = value.toString();
            if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null
                );

            } else {
                // For all the preferences, set the summary to the value's
                // simple string representation.
                preference.setSummary(stringValue);
            }
            return true;
        }
    };

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     *
     * @see #sBindPreferenceSummaryToValueListener
     */
    private void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        sBindPreferenceSummaryToValueListener.onPreferenceChange(
                preference,
                this.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                        .getString(preference.getKey(), "null")
        );
    }

    private GalleryApp getApp() {
        return (GalleryApp) getApplication();
    }

    @Override
    public void finish() {
        editPreff(this);
        Log.d(TAG, "HostName = " + findPreference("hostName").getSummary());
        setResult(RESULT_OK);
        super.finish();
    }
}
