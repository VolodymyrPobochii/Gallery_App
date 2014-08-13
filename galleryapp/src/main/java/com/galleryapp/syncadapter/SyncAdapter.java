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
import android.content.Intent;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.content.SyncResult;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.text.TextUtils;
import android.util.Log;

import com.galleryapp.Config;
import com.galleryapp.R;
import com.galleryapp.ScanRestService;
import com.galleryapp.application.GalleryApp;
import com.galleryapp.data.model.ChannelsObj;
import com.galleryapp.data.model.DocStatusObj;
import com.galleryapp.data.model.DocSubmittedObj;
import com.galleryapp.data.model.FileUploadObj;
import com.galleryapp.data.model.IndexSchema;
import com.galleryapp.data.model.SchemeElement;
import com.galleryapp.data.model.SubmitDocumentObj;
import com.galleryapp.data.model.SubmitDocumentObj.CaptureItemObj;
import com.galleryapp.data.model.SubmitDocumentObj.CaptureItemObj.BatchObj;
import com.galleryapp.data.model.SubmitDocumentObj.CaptureItemObj.BatchObj.Folder;
import com.galleryapp.data.model.SubmitDocumentObj.CaptureItemObj.BatchObj.Folder.Document;
import com.galleryapp.data.model.SubmitDocumentObj.CaptureItemObj.BatchObj.Folder.DocumentError;
import com.galleryapp.data.provider.GalleryDBContent;
import com.galleryapp.data.provider.GalleryDBContent.GalleryImages;
import com.galleryapp.data.provider.GalleryDBContent.Channels;
import com.galleryapp.data.provider.GalleryDBContent.IndexSchemas;
import com.google.common.base.Utf8;
import com.google.gson.Gson;

import org.apache.http.protocol.HTTP;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.UUID;

import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.mime.TypedByteArray;
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
    public static final int GET_DOC_STATUS = 1002;
    public static final int GET_INDEX_SCHEME = 1003;
    private static final long TIMER_TICK = 100l;
    private static final long TIME_TO_CLOSE_NOTIFICATION = 3000l;

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
    private ScanRestService.ScanServices mRestService;
    private ArrayList<Document> mDocuments;
    private ArrayList<String> mIds;
    private int mUploadCount;
    private int mUpdateTimes;
    private int mUpdateFreq;
    private SharedPreferences mPreff;

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
        mPreff = mApp.getPreff();
        mNotifyManager = NotificationManagerCompat.from(mContext);
        mBuilder = new NotificationCompat.Builder(mContext);
        mBuilder.setTicker("Get Channels")
                .setContentTitle(TAG)
                .setContentText("Channels code request SUCCESS")
                .setSmallIcon(android.R.drawable.ic_dialog_info);

        String baseUrl = mApp.getHostName() + ":" + mApp.getPort();
        Log.d(TAG, "init():: API_Url = " + baseUrl);
        ScanRestService serviceEnum = ScanRestService.INSTANCE.initRestAdapter(baseUrl);
        Log.d(TAG, "init():: Created scanService = " + serviceEnum.toString());
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
            case GET_INDEX_SCHEME:
                String capchcode = mApp.getCaptureChannelCode();
                Log.d(TAG, "onPerformSync() :: GET_INDEX_SCHEME :: capchcode = " + capchcode);
                Cursor channels = localProvider.query(Channels.CONTENT_URI,
                        Channels.PROJECTION,
                        null, null, null
                );
                if (channels != null && channels.getCount() > 0) {
                    localProvider.delete(IndexSchemas.CONTENT_URI, null, null);
                    while (channels.moveToNext()) {
                        String code = channels.getString(Channels.Columns.CODE.getIndex());
                        getIndexScheme(localProvider, syncResult, code);
                    }
                }
                break;
            case UPLOAD_FILES:
                Cursor c = localProvider.query(GalleryImages.CONTENT_URI,
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
            case GET_DOC_STATUS:
                prepareUpdateStatus(localProvider);
                break;
            default:
        }
        Log.i(TAG, "onPerformSync() :: Network synchronization complete");
    }

    private void getIndexScheme(ContentProvider localProvider, SyncResult syncResult, String capchcode) {
        Log.d(TAG, "getIndexScheme() :: BEGIN :: capchcode = " + capchcode);
        IndexSchema indexSchema = null;
        try {
//            mNotifyManager.notify(R.id.get_channels, mBuilder.build());
            indexSchema = mRestService.getIndexScheme(mApp.getDomain(), mApp.getToken(), capchcode);
        } catch (RetrofitError error) {
            Log.d(TAG, "getIndexScheme() :: RetrofitError = " + error.getLocalizedMessage());
           /* mBuilder.setContentText("Channels code request FAILURE")
                    .setSmallIcon(android.R.drawable.stat_notify_error);
            dispatchNotification(R.id.get_channels);*/
        }
        if (indexSchema != null) {
            String scheme = new Gson().toJson(indexSchema);
            Log.d(TAG, "getIndexScheme() :: END :: Scheme = " + scheme);
            List<SchemeElement> elements = indexSchema.getSchema().getDocuemntSchema().getElements();
            ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();
            for (SchemeElement element : elements) {
                element.setChannCode(capchcode);
                operations.add(ContentProviderOperation.newInsert(IndexSchemas.CONTENT_URI).withValues(element.toContentValues()).build());
            }
            if (operations.size() > 0) {
                try {
                    int results = localProvider.applyBatch(operations).length;
                    Log.d(TAG, "getIndexScheme() :: applyBatch() = " + results);
                } catch (OperationApplicationException e) {
                    e.printStackTrace();
                }
            }
        } else {
            Log.d(TAG, "getIndexScheme() :: END :: Scheme = NULL");
        }
    }

    private void getChannelsCodes(ContentProvider localProvider, SyncResult syncResult) {
        Log.d(TAG, "getChannelsCodes() :: START");
        /*mBuilder.setTicker("Channels update")
                .setContentTitle("Channels update")
                .setContentText("Updating channels...")
                .setProgress(100, 0, true)
                .setSmallIcon(android.R.drawable.stat_sys_download);*/

        ChannelsObj channels = null;
        try {
//            mNotifyManager.notify(R.id.get_channels, mBuilder.build());
            channels = mRestService.getChannels(mApp.getToken());
        } catch (RetrofitError error) {
            Log.d(TAG, "getChannelsCodes() :: RetrofitError = " + error.getLocalizedMessage());
           /* mBuilder.setContentText("Channels code request FAILURE")
                    .setSmallIcon(android.R.drawable.stat_notify_error);
            dispatchNotification(R.id.get_channels);*/
        }
        Log.d(TAG, "getChannelsCodes() :: END");
        completeGetChannels(channels, localProvider, syncResult);
    }

    private void uploadFiles(Cursor c, ContentProvider localProvider, SyncResult syncResult) {
        //TODO: refactor later
//        final ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();
        while (c.moveToNext()) {
            Log.d(TAG, "uploadFiles() :: BEGIN");
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
            }
            if (fileUpload != null) {
                Log.d(TAG, "uploadFiles() :: success = " + fileUpload.getUrl());
                mBuilder.setContentText("Upload Successful")
                        .setSmallIcon(android.R.drawable.stat_sys_upload_done);
                dispatchNotification(fileId);
                completeFileUpload(localProvider, syncResult, fileUpload.getUrl(), fileId, fileName, fileLength);
            } else {
                mBuilder.setContentText("Upload Error")
                        .setSmallIcon(android.R.drawable.stat_notify_error);
                dispatchNotification(fileId);
            }
            Log.d(TAG, "uploadFiles() :: END");
        }
        c.close();
    }

    private void completeFileUpload(ContentProvider localProvider, SyncResult syncResult,
                                    String uri, Integer fileId, String fileName, long fileLength) {
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
            prepareSubmitDocs(localProvider, uri, fileId, fileName, fileLength);
        }
    }

    private void prepareSubmitDocs(ContentProvider localProvider, String uri, Integer id, String name, long length) {
        Log.d(TAG, "prepareSubmitDocs() :: START : name = " + name);
        mIds.add(String.valueOf(id));
        Document document = new Document();
        document.setIndexSchema(mApp.getIndexString());
        document.setOriginalFileName(name);
        document.setContentType("image/jpg");
        document.setContentLength((int) length);
        document.setUri(uri);
        document.setExistingCMSUri(null);
        document.setIsEmailManifest(false);
        document.setBody(null);
        mDocuments.add(document);
        Log.d(TAG, "prepareSubmitDocs() :: END : name = " + name + " / schema = " + document.getIndexSchema());
        if (mUploadCount == 0) {
            submitDocs(localProvider);
        }
    }

    private void submitDocs(ContentProvider localProvider) {
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

        /*String url = hostName + ":" + port + Config.SUBMITT_POST_REQUEST_RULE + domain;
        String query = String.format("%s=%s", "t", token);
        url += "?" + query;

        SubmitDocumentTask submitDocumentTask = new SubmitDocumentTask(mContext, submitDocumentObj, mIds);
        submitDocumentTask.execute(url);*/

        String json = new Gson().toJson(submitDocumentObj);
        Log.d("UPLOAD", "DocSubmittedObj = " + json);
        byte[] postJson = json.getBytes();
        TypedByteArray docFile = new TypedByteArray("application/binary", postJson);
        long fileLength = docFile.length();

        DocSubmittedObj submittedDoc = null;
        try {
            mBuilder.setTicker("Submit document")
                    .setContentTitle(submitDocumentObj.getCaptureItem().getId())
                    .setContentText("Submitting document...")
                    .setProgress(100, 0, true)
                    .setSmallIcon(android.R.drawable.stat_sys_upload);
            mNotifyManager.notify(R.id.submit_doc, mBuilder.build());
            submittedDoc = mRestService.submitDoc(String.valueOf(fileLength), docFile, domain, token);
        } catch (RetrofitError error) {
            mBuilder.setContentText("Submit Error")
                    .setSmallIcon(android.R.drawable.stat_notify_error);
            dispatchNotification(R.id.submit_doc);
            Log.d(TAG, "uploadFiles() :: RetrofitError = " + error.getLocalizedMessage());
        }
        if (submittedDoc != null) {
            Log.d(TAG, "uploadFiles() :: success = " + submittedDoc.getId());
            mBuilder.setContentText("Submit Successful")
                    .setSmallIcon(android.R.drawable.stat_sys_upload_done);
            dispatchNotification(R.id.submit_doc);
            completeSubmitDoc(localProvider, submittedDoc, mIds);
        } else {
            mBuilder.setContentText("Submit Error")
                    .setSmallIcon(android.R.drawable.stat_notify_error);
            dispatchNotification(R.id.submit_doc);
        }
        Log.d(TAG, "prepareSubmitDocs() :: END");
    }

    private void completeSubmitDoc(ContentProvider localProvider, DocSubmittedObj submittedDoc, ArrayList<String> ids) {
        String docId = submittedDoc.getId();
        Log.d(TAG, "completeSubmitDoc():: submittedDoc = " + docId);
        int updatedCount = updateImageId(localProvider, docId, ids);
        Log.d(TAG, "completeSubmitDoc():: updatedCount = " + updatedCount);
        if (updatedCount != 0) {
            mUpdateTimes = Integer.parseInt(mPreff.getString(mContext.getString(R.string.updateTimes), "2"));
            mUpdateFreq = Integer.parseInt(mPreff.getString(mContext.getString(R.string.updateFreq), "10")) * 1000;
            Log.d(TAG, "completeSubmitDoc():: mUpdateTimes = " + mUpdateTimes + " mUpdateFreq = " + mUpdateFreq);
            getDocStatus(localProvider, docId);
        }
    }

    private void getDocStatus(ContentProvider localProvider, String docId) {
//        mApp.getDocStatus(mContext, ids, docId);
        DocStatusObj docStatus = null;
        try {
            docStatus = mRestService.getDocStatus(mApp.getDomain(), mApp.getToken(), docId);
        } catch (RetrofitError error) {
            mContext.sendBroadcast(new Intent(Config.ACTION_UPDATE_STATUS));
        }
        if (docStatus != null) {
            if (docStatus.getErrorCode() == 0) {
                String status = docStatus.getStatus();
                Log.d(TAG, "onDocStatus():: docStatus = " + status + " / errorMessage = " + docStatus.getErrorMessage());
                int updatedCount = updateImageStatus(localProvider, status, docId);
                Log.d("UPLOAD", "onDocStatus():: updatedCount = " + updatedCount);
                if (!status.equals("Completed")) {
                    long startTime = System.currentTimeMillis();
                    if (mUpdateTimes != 0) {
                        int dt = (int) (System.currentTimeMillis() - startTime);
                        Log.d(TAG, "Waiting for status (dt:" + dt + ") START");
                        while (dt < mUpdateFreq) {
                            dt = (int) (System.currentTimeMillis() - startTime);
                        }
                        Log.d(TAG, "Waiting for status (dt:" + dt + ") FINISH");
                        mUpdateTimes--;
                        getDocStatus(localProvider, docId);
                    } else {
                        mContext.sendBroadcast(new Intent(Config.ACTION_UPDATE_STATUS));
                    }
                } else {
                    mUpdateTimes = 0;
                    prepareUpdateStatus(localProvider);
                }
            }
        }
    }

    private void prepareUpdateStatus(ContentProvider localProvider) {
        mUpdateTimes = Integer.parseInt(mPreff.getString(mContext.getString(R.string.updateTimes), "2"));
        mUpdateFreq = Integer.parseInt(mPreff.getString(mContext.getString(R.string.updateFreq), "10")) * 1000;

        Cursor cursor = localProvider.query(GalleryImages.CONTENT_URI,
                GalleryImages.PROJECTION,
                GalleryImages.Columns.FILE_ID.getName() + "!=? AND " +
                        GalleryImages.Columns.STATUS.getName() + "!=?",
                new String[]{"", "Completed"}, null
        );
        if (cursor != null && cursor.getCount() > 0) {
            Log.d(TAG, "GET_DOC_STATUS :: c.count = " + cursor.getCount());
            while (cursor.moveToNext()) {
                Log.d(TAG, "GET_DOC_STATUS :: cursor.moveToNext()");
                String id = cursor.getString(GalleryImages.Columns.ID.ordinal());
                String docId = cursor.getString(GalleryImages.Columns.FILE_ID.ordinal());
                String status = cursor.getString(GalleryImages.Columns.STATUS.ordinal());
                if (!TextUtils.isEmpty(docId) && !status.equalsIgnoreCase("Completed")) {
                    Log.d(TAG, "GET_DOC_STATUS :: getDocStatus() : break");
                    getDocStatus(localProvider, docId);
                    break;
                }
            }
            cursor.close();
            Log.d(TAG, "GET_DOC_STATUS ::  cursor.close()");
        } else {
            Log.d(TAG, "GET_DOC_STATUS ::  All Statuses Updated");
        }
    }

    private int updateImageStatus(ContentProvider localProvider, String status, String docId) {
        ContentValues cv = new ContentValues();
        cv.put(GalleryDBContent.GalleryImages.Columns.STATUS.getName(), status);
        if (status.equalsIgnoreCase("Completed")) {
            cv.put(GalleryDBContent.GalleryImages.Columns.IS_SYNCED.getName(), 1);
            cv.put(GalleryDBContent.GalleryImages.Columns.NEED_UPLOAD.getName(), 0);
        }
        return localProvider.update(GalleryImages.CONTENT_URI, cv,
                GalleryImages.Columns.FILE_ID.getName() + "=?",
                new String[]{docId});
    }

    private int updateImageId(ContentProvider localProvider, String fileId, ArrayList<String> ids) {
        int updatedCount = 0;
        ContentValues cv = new ContentValues();
        cv.put(GalleryDBContent.GalleryImages.Columns.FILE_ID.getName(), fileId);
        for (String id : ids) {
            Log.d(TAG, "updateImageId():: id = " + id);
            updatedCount += localProvider.update(GalleryDBContent.GalleryImages.CONTENT_URI, cv,
                    GalleryDBContent.GalleryImages.Columns.ID.getName() + "=?", new String[]{id});
        }
        return updatedCount;
    }

    private void completeGetChannels(ChannelsObj channels, ContentProvider localProvider, SyncResult syncResult) {
        if (channels != null) {
            /*mBuilder.setContentText("Channels update Successful")
                    .setSmallIcon(android.R.drawable.stat_sys_download_done);
            dispatchNotification(R.id.get_channels);*/
            if (channels.getErrorCode() == 0 && TextUtils.isEmpty(channels.getErrorMessage())) {
                if (channels.getChannels().size() > 0) {
                    Log.d(TAG, "getChannelsCodes()" + "ChannelsObj = " + channels.toString());
                    Log.d(TAG, "getChannelsCodes()" + "syncResult.stats.numInserts = " + syncResult.stats.numInserts);
                    int channelsUpdated = updateChannels(channels, localProvider);
                    syncResult.stats.numInserts += channelsUpdated;
//                    dispatchNotification(R.id.get_channels);
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
            /*mBuilder.setContentText("Upload Error")
                    .setSmallIcon(android.R.drawable.stat_notify_error);
            dispatchNotification(R.id.get_channels);*/
        }
        Log.d(TAG, "getChannelsCodes()::END");
    }

    private int updateChannels(ChannelsObj channels, ContentProvider localProvider) {
        //Set first channel as default
        mApp.getPreff().edit()
                .putString("domain", channels.getChannels().get(0).getDomain())
                .putString("capturechannelcode", channels.getChannels().get(0).getCode())
                .apply();
        mApp.setUpHost();
        //Cleare channels
        localProvider.delete(GalleryDBContent.Channels.CONTENT_URI, null, null);
        //Clear IndexSchema
        localProvider.delete(IndexSchemas.CONTENT_URI, null, null);
        //Prepare channels for apply batch
        ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();
        ArrayList<ChannelsObj.ChannelObj> channelObjects = channels.getChannels();
        for (ChannelsObj.ChannelObj channel : channelObjects) {
            operations.add(ContentProviderOperation.newInsert(GalleryDBContent.Channels.CONTENT_URI)
                    .withValues(channel.toContentValues())
                    .build());
        }
        //Channels apply batch
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
        long startTime = System.currentTimeMillis();
        long dt = System.currentTimeMillis() - startTime;
        Log.d(TAG, "Waiting for close notification (dt:" + dt + ") START");
        while (dt < TIME_TO_CLOSE_NOTIFICATION) {
            dt = System.currentTimeMillis() - startTime;
        }
        Log.d(TAG, "Waiting for close notification (dt:" + dt + ") FINISH");
        mNotifyManager.cancel(id);
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
