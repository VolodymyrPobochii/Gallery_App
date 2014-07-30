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
import android.content.ContentProvider;
import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.SyncResult;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.text.TextUtils;
import android.util.Log;

import com.galleryapp.Config;
import com.galleryapp.ScanRestServiceEnum;
import com.galleryapp.application.GalleryApp;
import com.galleryapp.asynctasks.SubmitDocumentTask;
import com.galleryapp.data.model.ChannelsObj;
import com.galleryapp.data.model.FileUploadObj;
import com.galleryapp.data.model.SubmitDocumentObj;
import com.galleryapp.data.model.SubmitDocumentObj.CaptureItemObj;
import com.galleryapp.data.model.SubmitDocumentObj.CaptureItemObj.BatchObj;
import com.galleryapp.data.model.SubmitDocumentObj.CaptureItemObj.BatchObj.Folder;
import com.galleryapp.data.model.SubmitDocumentObj.CaptureItemObj.BatchObj.Folder.Document;
import com.galleryapp.data.model.SubmitDocumentObj.CaptureItemObj.BatchObj.Folder.DocumentError;
import com.galleryapp.data.provider.GalleryDBContent;
import com.galleryapp.data.provider.GalleryDBContent.GalleryImages;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.UUID;

import retrofit.RetrofitError;
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
    private GalleryApp mApp;
    private NotificationManagerCompat mNotifyManager;
    private NotificationCompat.Builder mBuilder;
    private Context mContext;
    private ScanRestServiceEnum.ScanServices mRestService;
    private ArrayList<Document> mDocuments;
    private ArrayList<String> mIds;
    private int mUploadCount;

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
        mNotifyManager = NotificationManagerCompat.from(mContext);
        mBuilder = new NotificationCompat.Builder(mContext);
        mBuilder.setTicker("Get Channels")
                .setContentTitle(TAG)
                .setContentText("Channels code request SUCCESS")
                .setSmallIcon(android.R.drawable.ic_dialog_info);

        String baseUrl = mApp.getHostName() + ":" + mApp.getPort();
        Log.d(TAG, "init():: API_Url = " + baseUrl);
        //TODO: refactor later
//        ScanRestService scanService = new ScanRestService.Builder(baseUrl).build();
        ScanRestServiceEnum serviceEnum = ScanRestServiceEnum.INSTANCE.initRestAdapter(baseUrl);
//        serviceEnum.initRestAdapter(baseUrl);
//        Log.d(TAG, "init():: Created scanService = " + scanService.toString());
        Log.d(TAG, "init():: Created scanService = " + serviceEnum.name());
        //TODO: refactor later
//        mRestService = scanService.getService();
        mRestService = serviceEnum.getService();
        Log.d(TAG, "init():: Created mRestService = " + mRestService.toString());
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
        ContentProvider localProvider = provider.getLocalContentProvider();
        int requestType = extras.getInt("requestType");
        Log.d(TAG, "onPerformSync() :: requestType = " + requestType);
        Log.i(TAG, "onPerformSync() :: Beginning network synchronization");
        switch (requestType) {
            case GET_CHANNELS:
                getChannelsCodes(localProvider, syncResult);
                break;
            case UPLOAD_FILES:
                Cursor c = null;
                c = localProvider.query(GalleryImages.CONTENT_URI,
                        GalleryImages.PROJECTION,
                        GalleryImages.Columns.IS_SYNCED.getName() + "=? AND " +
                                GalleryImages.Columns.NEED_UPLOAD.getName() + "=?",
                        new String[]{NOT_SYNCED, NEED_UPLOAD}, null
                );
                if (c != null && c.getCount() > 0) {
                    Log.d(TAG, "onPerformSync() :: UPLOAD_FILES :: Count = " + c.getCount());
                    mUploadCount = c.getCount();
                    mDocuments = new ArrayList<Document>();
                    mIds = new ArrayList<String>();
                    uploadFiles(c, localProvider, syncResult);
                }
                break;
            default:
        }
        Log.i(TAG, "onPerformSync() :: Network synchronization complete");
    }

    private void getChannelsCodes(ContentProvider localProvider, SyncResult syncResult) {
        Log.d(TAG, "getChannelsCodes() :: START");
        //TODO: refactor later
       /* mRestService.getChannels(mApp.getToken(), new Callback<ChannelsObj>() {
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
        });*/
        mBuilder.setTicker("Channels update")
                .setContentTitle("Channels update")
                .setContentText("Updating channels...")
                .setProgress(100, 0, true)
                .setSmallIcon(android.R.drawable.stat_sys_download);

        ChannelsObj channels = null;
        try {
            mNotifyManager.notify(100, mBuilder.build());
            channels = mRestService.getChannels(mApp.getToken());
        } catch (RetrofitError error) {
            Log.d(TAG, "getChannelsCodes() :: RetrofitError = " + error.getLocalizedMessage());
            mBuilder.setContentText("Channels code request FAILURE")
                    .setSmallIcon(android.R.drawable.stat_notify_error);
            dispatchNotification(100);
        } finally {
            Log.d(TAG, "getChannelsCodes() :: END");
            completeGetChannels(channels, localProvider, syncResult);
        }
    }

    private void uploadFiles(Cursor c, ContentProvider localProvider, SyncResult syncResult) {
        //TODO: refactor later
//        final ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();
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
            long fileLength = typedFile.length();
            //TODO: refactor later
            /*mRestService.uploadFile(String.valueOf(typedFile.length()), typedFile,
                    mApp.getDomain(), mApp.getToken(),
                    new UploadCallback(provider, fileId, fileName));*/
            mBuilder.setTicker("File upload")
                    .setContentTitle(fileName)
                    .setContentText("Uploading...")
                    .setProgress(100, 0, true)
                    .setSmallIcon(android.R.drawable.stat_sys_upload);

            FileUploadObj fileUpload = null;
            try {
                mNotifyManager.notify(fileId, mBuilder.build());
                fileUpload = mRestService.uploadFile(String.valueOf(fileLength), typedFile,
                        mApp.getDomain(), mApp.getToken());
            } catch (RetrofitError error) {
                mBuilder.setContentText("Upload Error")
                        .setSmallIcon(android.R.drawable.stat_notify_error);
                dispatchNotification(fileId);
                Log.d(TAG, "uploadFiles() :: RetrofitError = " + error.getLocalizedMessage());
            } finally {
                if (fileUpload != null) {
                    Log.d(TAG, "uploadFiles() :: success = " + fileUpload.getUrl());
                    mBuilder.setTicker("File upload")
                            .setContentText("Upload Successful")
                            .setSmallIcon(android.R.drawable.stat_sys_upload_done);
                    dispatchNotification(fileId);
                    completeFileUpload(localProvider, syncResult, fileUpload.getUrl(), fileId, fileName, fileLength);
                } else {
                    mBuilder.setTicker("File upload")
                            .setContentText("Upload Error")
                            .setSmallIcon(android.R.drawable.stat_notify_error);
                    dispatchNotification(fileId);
                }
            }
        }
        c.close();
    }

    private void completeFileUpload(ContentProvider localProvider, SyncResult syncResult, String uri, Integer fileId, String fileName, long fileLength) {
        Log.d(TAG, "completeFileUpload() :: START : fileName = " + fileName);
        mNotifyManager.cancel(fileId);
        ContentValues cv = new ContentValues();
        cv.put(GalleryImages.Columns.FILE_URI.getName(), uri);
        int updated = localProvider.update(GalleryImages.CONTENT_URI, cv,
                GalleryImages.Columns.ID.getName() + "=?", new String[]{String.valueOf(fileId)});
        Log.d(TAG, "updated=" + updated);
        if (updated > 0) {
            Log.d(TAG, "completeFileUpload() :: END : updated = " + updated);
            Log.d(TAG, "completeFileUpload() :: syncResult.stats.numInserts = " + syncResult.stats.numUpdates);
            syncResult.stats.numUpdates += updated;
            Log.d(TAG, "completeFileUpload() :: syncResult.stats.numInserts = " + syncResult.stats.numUpdates);
            mUploadCount--;
            prepareSubmitDocs(uri, fileId, fileName, fileLength);
        }
    }

    private void prepareSubmitDocs(String uri, Integer id, String name, long length) {
        Log.d(TAG, "prepareSubmitDocs() :: START : name = " + name);
        mIds.add(String.valueOf(id));
        Document document = new Document();
        document.setIndexSchema("");
        document.setOriginalFileName(name);
        document.setContentType("image/jpg");
        document.setContentLength((int) length);
        document.setUri(uri);
        document.setExistingCMSUri(null);
        document.setIsEmailManifest(false);
        document.setBody(null);
        mDocuments.add(document);
        Log.d(TAG, "prepareSubmitDocs() :: END : name = " + name);
        if (mUploadCount == 0) {
            submitDocs();
        }
    }

    private void submitDocs() {
        Log.d(TAG, "prepareSubmitDocs() :: START");
        String token = mApp.getToken();
        String domain = mApp.getDomain();
        String hostName = mApp.getHostName();
        String port = mApp.getPort();
        String channelCode = mApp.getCaptureChannelCode();

        ArrayList<DocumentError> documentErrors = new ArrayList<DocumentError>();
        DocumentError documentError = new DocumentError();
        documentErrors.add(documentError);

        ArrayList<Folder> folders = new ArrayList<Folder>();
        Folder folder = new Folder();
        folder.setIndexSchema("");
        folder.setDocuments(mDocuments);
        folder.setDocumentErrors(documentErrors);
        folders.add(folder);

        BatchObj batchObj = new BatchObj();
        batchObj.setIndexSchema("");
        batchObj.setRemovedDocumentUriCSV(null);
        batchObj.setOperationType(1);
        batchObj.setFolders(folders);

        URL hostUrl = null;
        try {
            hostUrl = new URL(hostName);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        CaptureItemObj captureItemObj = new CaptureItemObj();
        captureItemObj.setId(UUID.randomUUID().toString().replace("-", ""));
        captureItemObj.setChannelCode(channelCode);
        captureItemObj.setBatch(batchObj);
        captureItemObj.setIndexData("");
        String parameters = new StringBuilder()
                .append("controller=composite").append("&")
                .append("baseuri=").append(hostUrl != null ? hostUrl.getAuthority() + ":" + port : hostName + ":" + port).append("&")
                .append("stampsenabled=false").append("&")
                .append("hidescancontrols=false").append("&")
                .append("dataroot=UserStamps").append("&")
                .append("capturechannelcode=").append(channelCode).append("&")
                .append("uri=").append("&")
                .append("t=").append(token).append("&")
                .append("d=").append(domain).append("&")
                .append("sync=true").append("&")
                .append("scheme=").append(hostUrl != null ? hostUrl.getProtocol() : "http")
                .toString();
        Log.d(TAG, "PARAMETERS: " + parameters);
        captureItemObj.setParameters(parameters);
        captureItemObj.setChannelType(3);

        SubmitDocumentObj submitDocumentObj = new SubmitDocumentObj();
        submitDocumentObj.setDomain(domain);
        submitDocumentObj.setCaptureItem(captureItemObj);
        submitDocumentObj.setToken(token);

        String url = hostName + ":" + port + Config.SUBMITT_POST_REQUEST_RULE + domain;
        String query = String.format("%s=%s", "t", token);
        url += "?" + query;

        SubmitDocumentTask submitDocumentTask = new SubmitDocumentTask(mContext, submitDocumentObj, mIds);
        submitDocumentTask.execute(url);
        Log.d(TAG, "prepareSubmitDocs() :: END");
    }

    private void completeGetChannels(ChannelsObj channels, ContentProvider localProvider, SyncResult syncResult) {
        if (channels != null) {
            mBuilder.setContentText("Channels update Successful")
                    .setSmallIcon(android.R.drawable.stat_sys_download_done);
            dispatchNotification(100);
            if (channels.getErrorCode() == 0 && TextUtils.isEmpty(channels.getErrorMessage())) {
                if (channels.getChannels().size() > 0) {
                    Log.d(TAG, "getChannelsCodes()" + "ChannelsObj = " + channels.toString());
                    Log.d(TAG, "getChannelsCodes()" + "syncResult.stats.numInserts = " + syncResult.stats.numInserts);
                    int channelsUpdated = updateChannels(channels, localProvider);
                    syncResult.stats.numInserts += channelsUpdated;
                    dispatchNotification(100);
                    Log.d(TAG, "getChannelsCodes()" + "channelsUpdated = " + channelsUpdated);
                    Log.d(TAG, "getChannelsCodes()" + "syncResult.stats.numInserts = " + syncResult.stats.numInserts);
                } else {
                    Log.d(TAG, "getChannelsCodes():: Channels = 0");
                }
            } else {
                Log.d(TAG, "getChannelsCodes():: Error = " + channels.getErrorMessage());
            }
        } else {
            Log.d(TAG, "getChannelsCodes():: Error: channels = NULL");
            mBuilder.setContentText("Upload Error")
                    .setSmallIcon(android.R.drawable.stat_notify_error);
            dispatchNotification(100);
        }
        Log.d(TAG, "getChannelsCodes()::END");
    }

    private int updateChannels(ChannelsObj channels, ContentProvider localProvider) {
        localProvider.delete(GalleryDBContent.Channels.CONTENT_URI, null, null);

        ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();
        for (ChannelsObj.ChannelObj channel : channels.getChannels()) {
            operations.add(ContentProviderOperation.newInsert(GalleryDBContent.Channels.CONTENT_URI)
                    .withValues(channel.toContentValues())
                    .build());
        }

        ContentProviderResult[] results = new ContentProviderResult[0];
        if (operations.size() > 0) {
            try {
                results = localProvider.applyBatch(operations);
            } catch (OperationApplicationException e) {
                e.printStackTrace();
            }
        }
        return results.length;
    }

    private void dispatchNotification(final int id) {
        mNotifyManager.notify(id, mBuilder.build());
        mNotifyManager.cancel(id);
        //TODO: refactor later
        /*new Handler().postDelayed(
                new Runnable() {
                    @Override
                    public void run() {
                        mNotifyManager.cancel(id);
                        Looper.myLooper().quit();
                    }
                }, 3000
        );*/
    }


    //TODO: refactor later
    /*private class UploadCallback implements Callback<FileUploadObj> {

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
//            completeFileUpload(mProvider, fileUploadObj.getUrl(), mFileId, fileName, fileLength);
            Log.d(TAG, "UploadCallback()::success=" + fileUploadObj.getUrl());
        }

        @Override
        public void failure(RetrofitError error) {
            Log.d(TAG, "UploadCallback()::error=" + error.getLocalizedMessage());
        }
    }*/
}
