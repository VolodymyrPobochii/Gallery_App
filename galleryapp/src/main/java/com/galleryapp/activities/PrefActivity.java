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

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

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

        updatePreffSummaries();
    }

    private void updatePreffSummaries() {
        getPreferenceScreen().getPreference(0)
                .setSummary(getPrefs(this).getString("hostName", getString(R.string.default_value_host_preference)));
        getPreferenceScreen().getPreference(1)
                .setSummary(getPrefs(this).getString("port", getString(R.string.default_value_port_preference)));
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
                Intent upIntent = NavUtils.getParentActivityIntent(this);
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
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.d("PREFF", "Changed : preff = " + sharedPreferences.toString() + " key = " + key);
        updatePreffSummaries();
    }

    private GalleryApp getApp() {
        return (GalleryApp) getApplication();
    }
}
