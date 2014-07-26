/*
 * Copyright 2013 The Android Open Source Project
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

package com.galleryapp.syncadapter;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.galleryapp.ChannelsRestAdapter;
import com.galleryapp.Config;
import com.galleryapp.application.GalleryApp;
import com.galleryapp.data.model.ChannelsObj;
import com.galleryapp.interfaces.GetChannelsEventListener;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Define a sync adapter for the app.
 * <p/>
 * <p>This class is instantiated in {@link SyncService}, which also binds SyncAdapter to the system.
 * SyncAdapter should only be initialized in SyncService, never anywhere else.
 * <p/>
 * <p>The system calls onPerformSync() via an RPC call through the IBinder object supplied by
 * SyncService.
 */
public final class SyncAdapter extends AbstractThreadedSyncAdapter {

    public final String TAG = this.getClass().getSimpleName();
    private static final String PREFS_NAME = "defaults";
    public static final int GET_CHANNELS = 1000;

    /**
     * Network connection timeout, in milliseconds.
     */
    private static final int NET_CONNECT_TIMEOUT_MILLIS = 15000;  // 15 seconds

    /**
     * Network read timeout, in milliseconds.
     */
    private static final int NET_READ_TIMEOUT_MILLIS = 10000;  // 10 seconds

    /**
     * Content resolver, for performing database operations.
     */
    private ContentResolver mContentResolver;
    private GalleryApp mApp;
    private NotificationManagerCompat mNotifyManager;
    private NotificationCompat.Builder mBuilder;
    private Context mContext;

    /**
     * Constructor. Obtains handle to content resolver for later use.
     */
    public SyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        Log.d(TAG, "SyncAdapter :: SyncAdapter()");
        init();
    }

    /**
     * Constructor. Obtains handle to content resolver for later use.
     */
    public SyncAdapter(Context context, boolean autoInitialize, boolean allowParallelSyncs) {
        super(context, autoInitialize, allowParallelSyncs);
        Log.d(TAG, "SyncAdapter :: SyncAdapter()");
        init();
    }

    private void init() {
        mContext = getContext();
        mApp = GalleryApp.getInstance();
        mContentResolver = mContext.getContentResolver();
        mNotifyManager = NotificationManagerCompat.from(mContext);
        mBuilder = new NotificationCompat.Builder(mContext);
        mBuilder.setTicker("Get Channels")
                .setContentTitle(TAG)
                .setContentText("Channels code request SUCCESS")
                .setSmallIcon(android.R.drawable.ic_dialog_info);
    }

    /**
     * Called by the Android system in response to a request to run the sync adapter. The work
     * required to read data from the network, parse it, and store it in the content provider is
     * done here. Extending AbstractThreadedSyncAdapter ensures that all methods within SyncAdapter
     * run on a background thread. For this reason, blocking I/O and other long-running tasks can be
     * run <em>in situ</em>, and you don't have to set up a separate thread for them.
     * .
     * <p/>
     * <p>This is where we actually perform any work required to perform a sync.
     * {@link android.content.AbstractThreadedSyncAdapter} guarantees that this will be called on a non-UI thread,
     * so it is safe to peform blocking I/O here.
     * <p/>
     * <p>The syncResult argument allows you to pass information back to the method that triggered
     * the sync.
     */
    @Override
    public void onPerformSync(Account account, Bundle extras, String authority,
                              ContentProviderClient provider, SyncResult syncResult) {

        Log.d(TAG, "onPerformSync()");
        Log.i(TAG, "Beginning network synchronization");
        int requestType = extras.getInt("requestType");
        Log.d(TAG, "onPerformSync() :: requestType = " + requestType);
        switch (requestType) {
            case GET_CHANNELS:
                getChannelsCodes();
                break;
            default:
        }
        Log.i(TAG, "Network synchronization complete");
    }

    private void getChannelsCodes() {
        Log.d(TAG, "getChannelsCodes()::START");
        String url = mApp.getHostName() + ":" + mApp.getPort();
        Log.d(TAG, "getChannelsCodes()::Url = " + url);
       /* new ChannelsRestAdapter(url)
                .execute(mApp.getToken(), new Callback<ChannelsObj>() {
                    @Override
                    public void success(ChannelsObj channelsObj, Response response) {
                        Log.d(TAG, "onPerformSync()::ChannelsRestAdapter()::success()::channelsObj = " + channelsObj);
                        mNotifyManager.notify(1000, mBuilder.build());
                    }

                    @Override
                    public void failure(RetrofitError error) {
                        Log.d(TAG, "onPerformSync()::ChannelsRestAdapter()::failure()::Error: " + error.getLocalizedMessage());
                        mBuilder.setContentText("Channels code request FAILURE");
                        mNotifyManager.notify(1000, mBuilder.build());
                    }
                });*/
        ChannelsObj channels = new ChannelsRestAdapter(url).execute(mApp.getToken());
        if (channels != null) {
            if (channels.getErrorCode() == 0 && TextUtils.isEmpty(channels.getErrorMessage())) {
                if (channels.getChannels().size() > 0) {
                    Log.d(TAG, "getChannelsCodes()" + "ChannelsObj = " + channels.toString());
                    int channelsUpdated = mApp.updateChannels(channels);
                    Log.d(TAG, "getChannelsCodes()" + "channelsUpdated = " + channelsUpdated);
                } else {
                    Log.d(TAG, "getChannelsCodes():: Channels = 0");
                }
            } else {
                Log.d(TAG, "getChannelsCodes():: Error = " + channels.getErrorMessage());
            }
        } else {
            Log.d(TAG, "getChannelsCodes():: Error: channels = NULL");
        }
        Log.d(TAG, "getChannelsCodes()::END");
    }
}
