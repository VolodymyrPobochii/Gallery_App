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
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SyncResult;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.text.TextUtils;
import android.util.Log;

import com.galleryapp.ChannelsRestAdapter;
import com.galleryapp.FileUploadRestAdapter;
import com.galleryapp.application.GalleryApp;
import com.galleryapp.data.model.ChannelsObj;
import com.galleryapp.data.model.FileUploadObj;
import com.galleryapp.data.provider.GalleryDBContent.GalleryImages;

import java.io.File;
import java.util.ArrayList;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.mime.TypedFile;

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
    public static final String NOT_SYNCED = "0";
    public static final String NEED_UPLOAD = "1";
    public static final int GET_CHANNELS = 1000;
    public static final int UPLOAD_FILES = 1001;

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
            case UPLOAD_FILES:
                Cursor c = null;
                try {
                    c = provider.query(GalleryImages.CONTENT_URI,
                            GalleryImages.PROJECTION,
                            GalleryImages.Columns.IS_SYNCED.getName() + "=? AND " +
                                    GalleryImages.Columns.NEED_UPLOAD.getName() + "=?",
                            new String[]{NOT_SYNCED, NEED_UPLOAD}, null
                    );
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                if (c != null && c.getCount() > 0) {
                    Log.d(TAG, "onPerformSync()::UPLOAD_FILES::c.getCount() = " + c.getCount());
                    uploadFiles(c, provider);
                }
                break;
            default:
        }
        Log.i(TAG, "Network synchronization complete");
    }

    private void getChannelsCodes() {
        Log.d(TAG, "getChannelsCodes()::START");
        String url = mApp.getHostName() + ":" + mApp.getPort();
        Log.d(TAG, "getChannelsCodes()::Url = " + url);
        new ChannelsRestAdapter(url)
                .execute(mApp.getToken(), new Callback<ChannelsObj>() {
                    @Override
                    public void success(ChannelsObj channelsObj, Response response) {
                        Log.d(TAG, "onPerformSync()::ChannelsRestAdapter()::success()::channelsObj = " + channelsObj);
                        dispatchNotification(100);
                        completeGetChannels(channelsObj);
                    }

                    @Override
                    public void failure(RetrofitError error) {
                        Log.d(TAG, "onPerformSync()::ChannelsRestAdapter()::failure()::Error: " + error.getLocalizedMessage());
                        mBuilder.setContentText("Channels code request FAILURE");
                        dispatchNotification(100);
                    }
                });
//        ChannelsObj channels = new ChannelsRestAdapter(url).execute(mApp.getToken());
        //        completeGetChannels(channels);
    }

    private void uploadFiles(Cursor c, ContentProviderClient provider) {
//        final ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();
        String url = mApp.getHostName() + ":" + mApp.getPort();
        while (c.moveToNext()) {
            Integer fileId = c.getInt(GalleryImages.Columns.ID.ordinal());
            String fileName = c.getString(GalleryImages.Columns.IMAGE_NAME.ordinal());
            String filePath = c.getString(GalleryImages.Columns.IMAGE_PATH.ordinal());
            String fileThumbPath = c.getString(GalleryImages.Columns.THUMB_PATH.ordinal());
            Integer needUpload = c.getInt(GalleryImages.Columns.NEED_UPLOAD.ordinal());
            Integer isSynced = c.getInt(GalleryImages.Columns.IS_SYNCED.ordinal());
            Log.d(TAG, "fileId=" + fileId + "|fileName=" + fileName + "|filePath=" +
                    filePath + "|fileThumbPath=" + fileThumbPath + "|needUpload=" + needUpload + "|isSynced=" + isSynced);

            TypedFile typedFile = new TypedFile("application/binary", new File(filePath));
            new FileUploadRestAdapter(url, typedFile)
                    .execute(mApp.getDomain(), mApp.getToken(), new UploadCallback(provider, fileId, fileName));
        }
    }

    private void completeFileUpload(ContentProviderClient providerClient, FileUploadObj fileUploadObj, Integer mFileId) {
        ContentValues cv = new ContentValues();
        cv.put(GalleryImages.Columns.FILE_URI.getName(), fileUploadObj.getUrl());

        try {
            int updated = providerClient.update(GalleryImages.CONTENT_URI, cv,
                    GalleryImages.Columns.ID.getName() + "=?", new String[]{String.valueOf(mFileId)});
            Log.d(TAG, "updated=" + updated);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void completeGetChannels(ChannelsObj channels) {
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

    private void dispatchNotification(final int id) {
        mNotifyManager.notify(id, mBuilder.build());
        new Handler().postDelayed(
                new Runnable() {
                    @Override
                    public void run() {
                        mNotifyManager.cancel(id);
                    }
                }, 3000
        );
    }

    private class UploadCallback implements Callback<FileUploadObj> {

        private final ContentProviderClient mProvider;
        private final Integer mFileId;
        private final String mFileName;

        public UploadCallback(ContentProviderClient operations, Integer fileId, String fileName) {
            this.mProvider = operations;
            this.mFileId = fileId;
            this.mFileName = fileName;
        }

        @Override
        public void success(FileUploadObj fileUploadObj, Response response) {
            completeFileUpload(mProvider, fileUploadObj, mFileId);
            Log.d(TAG, "UploadCallback()::success=" + fileUploadObj.getUrl());
        }

        @Override
        public void failure(RetrofitError error) {
            Log.d(TAG, "UploadCallback()::error=" + error.getLocalizedMessage());
        }
    }
}
