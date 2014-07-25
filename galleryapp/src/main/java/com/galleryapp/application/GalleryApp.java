package com.galleryapp.application;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Application;
import android.app.ProgressDialog;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.CountDownTimer;
import android.os.RemoteException;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import android.widget.Toast;

import com.galleryapp.ChannelsRestAdapter;
import com.galleryapp.Config;
import com.galleryapp.R;
import com.galleryapp.activities.GalleryActivity;
import com.galleryapp.activities.PrefActivity;
import com.galleryapp.asynctasks.DocumentStatusTask;
import com.galleryapp.asynctasks.SubmitDocumentTask;
import com.galleryapp.asynctasks.UploadFileTask2;
import com.galleryapp.data.model.ChannelsObj;
import com.galleryapp.data.model.ChannelsObj.ChannelObj;
import com.galleryapp.data.model.DocStatusObj;
import com.galleryapp.data.model.DocSubmittedObj;
import com.galleryapp.data.model.FileUploadObj;
import com.galleryapp.data.model.ImageObj;
import com.galleryapp.data.model.SubmitDocumentObj;
import com.galleryapp.data.model.SubmitDocumentObj.CaptureItemObj;
import com.galleryapp.data.model.SubmitDocumentObj.CaptureItemObj.BatchObj;
import com.galleryapp.data.model.SubmitDocumentObj.CaptureItemObj.BatchObj.Folder;
import com.galleryapp.data.model.SubmitDocumentObj.CaptureItemObj.BatchObj.Folder.Document;
import com.galleryapp.data.model.SubmitDocumentObj.CaptureItemObj.BatchObj.Folder.DocumentError;
import com.galleryapp.data.provider.GalleryDBContent;
import com.galleryapp.data.provider.GalleryDBProvider;
import com.galleryapp.fragmernts.GalleryFragment;
import com.galleryapp.interfaces.GetChannelsEventListener;
import com.galleryapp.interfaces.ProgressiveEntityListener;
import com.nostra13.universalimageloader.cache.disc.impl.UnlimitedDiscCache;
import com.nostra13.universalimageloader.cache.memory.impl.LruMemoryCache;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.utils.StorageUtils;

import org.apache.http.entity.FileEntity;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.OkClient;
import retrofit.client.Response;

public class GalleryApp extends Application implements ProgressiveEntityListener, GalleryFragment.OnFragmentInteractionListener {

    public static final String TAG = GalleryApp.class.getSimpleName();
    private static final long TIMER_TICK = 100l;

    private static GalleryApp instance;

    private String login;
    private String password;
    private String token;
    private String domain;
    private String captureChannelCode;
    private SharedPreferences preff;
    private String hostName;
    private String port;
    private String baseUrl;
    private String loginBaseUrl;
    private String cmsBaseUrl;
    private String appVersion;
    private ArrayList<Document> mDocuments;
    private ArrayList<String> mIds;
    private int mUpdateTimes;
    private int mUpdateFreq;
    private int mUploadCount;
//    private static RestAdapter mRestAdapter;

    public GalleryApp() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (instance == null) {
            instance = this;
        }
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        display.getMetrics(metrics);
        // Create global configuration and initialize ImageLoader with this configuration
        File cacheDir = StorageUtils.getCacheDirectory(getApplicationContext());
        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(getApplicationContext())
                .memoryCacheExtraOptions(metrics.widthPixels, metrics.heightPixels) // default = device screen dimensions
                .discCacheExtraOptions(metrics.widthPixels, metrics.heightPixels, Bitmap.CompressFormat.JPEG, 75, null)
                .denyCacheImageMultipleSizesInMemory()
                .memoryCache(new LruMemoryCache(2 * 1024 * 1024))
                .memoryCacheSize(2 * 1024 * 1024)
                .discCache(new UnlimitedDiscCache(cacheDir)) // default
                .discCacheSize(50 * 1024 * 1024)
                .discCacheFileCount(100)
                .writeDebugLogs()
                .build();
        ImageLoader.getInstance().init(config);
        preff = PrefActivity.getPrefs(getApplicationContext());
        setUpHost();
//        initRestAdapter();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.d(TAG, "width=" + newConfig.screenWidthDp +
                "height=" + newConfig.screenHeightDp);
    }

    private void initRestAdapter() {
        // Create a very simple REST adapter which points the FileUpload API endpoint.
        RestAdapter mRestAdapter = new RestAdapter.Builder()
                .setClient(new OkClient())
                .setEndpoint("http://")
                .build();
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
    }

    public void deleteImage(ArrayList<String> ids, ArrayList<File> images, ArrayList<File> thumbs) {
        ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();
        for (String id : ids) {
            operations.add(ContentProviderOperation.newDelete(GalleryDBContent.GalleryImages.CONTENT_URI)
                    .withSelection(GalleryDBContent.GalleryImages.Columns.ID.getName() + "=?", new String[]{id})
                    .build());
        }
        try {
            getContentResolver().applyBatch(GalleryDBProvider.AUTHORITY, operations);
            deleteFilesRecursive(images, thumbs);
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (OperationApplicationException e) {
            e.printStackTrace();
        }
    }

    private void deleteFilesRecursive(ArrayList<File> images, ArrayList<File> thumbs) {
        for (File image : images) {
            deleteFileRecursive(image);
        }
        for (File thumb : thumbs) {
            deleteFileRecursive(thumb);
        }
    }

    private void deleteFileRecursive(File file) {
        if (file.isDirectory()) {
            for (File child : file.listFiles()) {
                deleteFileRecursive(child);
            }
        } else {
            boolean result = file.delete();
            Log.d("CHECKED_IDS", "File[deleted] = " + result);
        }
    }

    public Uri saveImage(final ImageObj image) {
        return getContentResolver()
                .insert(GalleryDBContent.GalleryImages.CONTENT_URI, image.toContentValues());
    }

    public boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        return ni != null;
    }

    public final AlertDialog customAlertDialog(final Context activity, String message,
                                               String posText, final boolean posFinish,
                                               String negatText, final boolean negFinish, final boolean logOut) {
        AlertDialog dialog = new AlertDialog.Builder(activity).create();
        dialog.setIcon(R.drawable.ic_launcher);
        dialog.setTitle(activity.getResources().getString(R.string.app_name));
        dialog.setMessage(message);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        if (posText != null) {
            dialog.setButton(AlertDialog.BUTTON_POSITIVE, posText, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (posFinish) {
                        if (activity instanceof GalleryActivity) {
                            if (logOut) setToken(null);
                            ((GalleryActivity) activity).finish();
                        } else {
                            Intent intent = new Intent(activity, GalleryActivity.class);
                            intent.putExtra("logout", logOut);
                            activity.startActivity(intent);
                            ((Activity) activity).finish();
                        }
                    }
                    dialog.dismiss();
                }
            });
        }
        if (negatText != null) {
            dialog.setButton(AlertDialog.BUTTON_NEGATIVE, negatText, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (negFinish) ((Activity) activity).finish();
                    dialog.dismiss();
                }
            });
        }
        return dialog;
    }

    public ProgressDialog customProgressDialog(final Context context, String message) {
        ProgressDialog progressDialog = new ProgressDialog(context);
        progressDialog.setTitle(context.getResources().getString(R.string.app_name));
        progressDialog.setMessage(message);
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        progressDialog.setCanceledOnTouchOutside(false);
        return progressDialog;
    }

    public AlertDialog noConnectionDialog() {
        return customAlertDialog(getApplicationContext(), getString(R.string.no_connection), getString(R.string.close), false, null, false, false);
    }

    public void setUpHost() {
        hostName = preff.getString("hostName", Config.DEFAULT_HOST);
        port = preff.getString("port", Config.DEFAULT_PORT);
        domain = preff.getString("domain", Config.DEFAULT_DOMAIN);
        captureChannelCode = preff.getString("capturechannelcode", Config.DEFAULT_CAPTURE_CHANNEL_CODE);
        login = preff.getString("username", Config.DEFAULT_USERNAME);
        password = preff.getString("password", Config.DEFAULT_PASSWORD);
        loginBaseUrl = hostName + ":" + port;
        baseUrl = hostName + ":" + port + Config.DEFAULT_URL_BODY + domain;
        cmsBaseUrl = hostName + ":" + port + Config.DEFAULT_CSM_URL_BODY;

        Log.d("GalleryApp", "setUpHost()::host=" + hostName);
        Log.d("GalleryApp", "setUpHost()::port=" + port);
        Log.d("GalleryApp", "setUpHost()::domain=" + domain);
        Log.d("GalleryApp", "setUpHost()::channelCode=" + captureChannelCode);
    }

    public static GalleryApp getInstance() {
        return instance;
    }

    public void uploadFile(Context context, List<byte[]> fileBytes, List<String> filePaths, List<String> thumbPaths,
                           List<String> fileNames, List<Integer> ids) {
        String url = hostName + ":" + port + Config.UPLOAD_POST_REQUEST_RULE + domain;
        String query = String.format("%s=%s", "t", token);
        url += "?" + query;
        Log.d("UPLOAD", "url = " + url);
        assert filePaths != null;
        mUploadCount = filePaths.size();
        if (isNetworkConnected()) {
            for (String filePath : filePaths) {
                File uploadFile = new File(filePath);
                FileEntity fileEntity = new FileEntity(uploadFile, "application/binary");
                int id = ids.get(filePaths.indexOf(filePath));
                String thumbPath = thumbPaths.get(filePaths.indexOf(filePath));
                String name = fileNames.get(filePaths.indexOf(filePath));
                UploadFileTask2 uploadFileTask = new UploadFileTask2(getApplicationContext(), fileEntity, thumbPath, id, name);
                uploadFileTask.execute(url);
            }
            mDocuments = new ArrayList<Document>();
            mIds = new ArrayList<String>();
        } else {
            Toast.makeText(context, "No Connection", Toast.LENGTH_SHORT).show();
        }
    }

    public int updateImageUri(String uri, String id) {
        ContentValues cv = new ContentValues();
        cv.put(GalleryDBContent.GalleryImages.Columns.FILE_URI.getName(), uri);
        return getContentResolver().update(GalleryDBContent.GalleryImages.CONTENT_URI, cv,
                GalleryDBContent.GalleryImages.Columns.ID.getName() + "=?", new String[]{id});
    }

    public int updateImageId(String fileId, ArrayList<String> ids) {
        int updatedCount = 0;
        ContentValues cv = new ContentValues();
        cv.put(GalleryDBContent.GalleryImages.Columns.FILE_ID.getName(), fileId);
        for (String id : ids) {
            Log.d("UPLOAD", "updateImageId():: id = " + id);
            updatedCount += getContentResolver().update(GalleryDBContent.GalleryImages.CONTENT_URI, cv,
                    GalleryDBContent.GalleryImages.Columns.ID.getName() + "=?", new String[]{id});
        }
        return updatedCount;
    }

    public void prepareSubmitDocs(Context context, FileUploadObj response, String id, String name, long length, int uploadCount) {
        mIds.add(id);
        Document document = new Document();
        document.setIndexSchema("");
        document.setOriginalFileName(name);
        document.setContentType("image/jpg");
        document.setContentLength((int) length);
        document.setUri(response.getUrl());
        document.setExistingCMSUri(null);
        document.setIsEmailManifest(false);
        document.setBody(null);
        mDocuments.add(document);
        if (uploadCount == 0) {
            submitDocs(context);
        }
    }

    public void submitDocs(Context context) {
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
        captureItemObj.setChannelCode(captureChannelCode);
        captureItemObj.setBatch(batchObj);
        captureItemObj.setIndexData("");
        String parameters = new StringBuilder()
                .append("controller=composite").append("&")
                .append("baseuri=").append(hostUrl != null ? hostUrl.getAuthority() + ":" + port : hostName + ":" + port).append("&")
                .append("stampsenabled=false").append("&")
                .append("hidescancontrols=false").append("&")
                .append("dataroot=UserStamps").append("&")
                .append("capturechannelcode=").append(captureChannelCode).append("&")
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

        SubmitDocumentTask submitDocumentTask = new SubmitDocumentTask(context, submitDocumentObj, mIds);
        submitDocumentTask.execute(url);
    }

    public void getDocStatus(Context context, ArrayList<String> ids, String docId) {
        String url = hostName + ":" + port + Config.STATUS_GET_REQUEST_RULE + domain;
        String query = String.format("%s=%s&%s=%s", "t", getToken(), "id", docId);
        url += "?" + query;
        DocumentStatusTask statusTask = new DocumentStatusTask(context, ids, docId);
        statusTask.execute(url);
    }

    public void getDocStatus(Context context, String id, String docId) {
        ArrayList<String> ids = new ArrayList<String>();
        ids.add(id);
        getDocStatus(context, ids, docId);
    }

    public void getChannels(final Context context) {
        String url = hostName + ":" + port + Config.GET_CHANNELS_RULE;
        String query = String.format("%s=%s", "t", token);
        url += "?" + query;
//        GetChannelsTask statusTask = new GetChannelsTask(context);
//        statusTask.execute(url);
        final GetChannelsEventListener channelsEventListener = (GetChannelsEventListener) context;
        new ChannelsRestAdapter(hostName + ":" + port)
        .execute(token, new Callback<ChannelsObj>() {
            @Override
            public void success(ChannelsObj channelsObj, Response response) {
                Toast.makeText(context, "Channels: " + channelsObj.toString(), Toast.LENGTH_LONG).show();
                channelsEventListener.onGetChannels(channelsObj);
            }

            @Override
            public void failure(RetrofitError error) {
                Toast.makeText(context, "Error: " + error.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    public int updateChannels(ChannelsObj channels) {
        getContentResolver().delete(GalleryDBContent.Channels.CONTENT_URI, null, null);
        ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();
        for (ChannelObj channel : channels.getChannels()) {
            operations.add(ContentProviderOperation.newInsert(GalleryDBContent.Channels.CONTENT_URI)
                    .withValues(channel.toContentValues())
                    .build());
        }
        ContentProviderResult[] results = new ContentProviderResult[0];
        if (operations.size() > 0) {
            try {
                results = getContentResolver().applyBatch(GalleryDBProvider.AUTHORITY, operations);
            } catch (RemoteException e) {
                e.printStackTrace();
            } catch (OperationApplicationException e) {
                e.printStackTrace();
            }
        }
        return results.length;
    }

    public int updateImageStatus(String status, ArrayList<String> ids, String docId) {
        int updatedCount = 0;
        ContentValues cv = new ContentValues();
        cv.put(GalleryDBContent.GalleryImages.Columns.STATUS.getName(), status);
//        cv.put(GalleryDBContent.GalleryImages.Columns.IS_SYNCED.getName(), 1);
        for (String id : ids) {
            updatedCount += getContentResolver().update(GalleryDBContent.GalleryImages.CONTENT_URI, cv,
                    GalleryDBContent.GalleryImages.Columns.ID.getName() + "=?" + " AND " +
                            GalleryDBContent.GalleryImages.Columns.FILE_ID.getName() + "=?",
                    new String[]{id, docId}
            );
        }
        return updatedCount;
    }

    public ArrayList<String> getRunningActivities() {
        ArrayList<String> runningActivities = new ArrayList<String>();
        ActivityManager activityManager = (ActivityManager) getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> services = activityManager.getRunningTasks(Integer.MAX_VALUE);
        for (ActivityManager.RunningTaskInfo service : services) {
            runningActivities.add(0, service.topActivity.toString());
        }
        return runningActivities;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getCaptureChannelCode() {
        return captureChannelCode;
    }

    public void setCaptureChannelCode(String captureChannelCode) {
        this.captureChannelCode = captureChannelCode;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public SharedPreferences getPreff() {
        return preff;
    }

    public void setPreff(SharedPreferences preff) {
        this.preff = preff;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    // TODO: Fix this
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getLoginBaseUrl() {
        return loginBaseUrl;
    }

    public void setLoginBaseUrl(String loginBaseUrl) {
        this.loginBaseUrl = loginBaseUrl;
    }

    public String getCmsBaseUrl() {
        return cmsBaseUrl;
    }

    public void setCmsBaseUrl(String cmsBaseUrl) {
        this.cmsBaseUrl = cmsBaseUrl;
    }

    public String getAppVersion() {
        try {
            return "v." + getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_CONFIGURATIONS).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return getString(R.string.version_unavailable);
        }
    }

    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }

    @Override
    public void onFileUploaded(FileUploadObj response, String id, String name, long length) {
        if (response != null && response.getUrl() != null) {
            Log.d("UPLOAD", "onFileUploaded():: response = " + response.getUrl());
            mUploadCount--;
            if (updateImageUri(response.getUrl(), id) != 0) {
                Log.d("UPLOAD", "updateImageUri():: imageId = " + id + "\nImageName = " + name + "\n" + "FileURI = " + response.getUrl() +
                        "\nmUploadCount = " + mUploadCount);
                prepareSubmitDocs(this, response, id, name, length, mUploadCount);
            }
        }
    }

    @Override
    public void onDocSubmitted(DocSubmittedObj response, ArrayList<String> ids) {
        if (response.getId() != null) {
            Log.d("UPLOAD", "onDocSubmitted():: response = " + response.getId());
            int updatedCount = updateImageId(response.getId(), ids);
            Log.d("UPLOAD", "onDocSubmitted():: updatedCount = " + updatedCount);
            if (updatedCount != 0) {
                mUpdateTimes = Integer.parseInt(preff.getString(getString(R.string.updateTimes), "2"));
                mUpdateFreq = Integer.parseInt(preff.getString(getString(R.string.updateFreq), "10")) * 1000;
                getDocStatus(this, ids, response.getId());
            }
        }
    }

    @Override
    public void onDocStatus(DocStatusObj response, final ArrayList<String> ids, final String docId) {
        if (response.getStatus() != null) {
            Log.d("UPLOAD", "onDocStatus():: response = " + response.getStatus() + " / errorMessage = " + response.getErrorMessage());
            int updatedCount = updateImageStatus(response.getStatus(), ids, docId);
            Log.d("UPLOAD", "onDocStatus():: updatedCount = " + updatedCount);
            if (!response.getStatus().equals("Completed")) {
                if (mUpdateTimes != 0) {
                    new CountDownTimer(mUpdateFreq, TIMER_TICK) {
                        @Override
                        public void onTick(long millisUntilFinished) {
                        }

                        @Override
                        public void onFinish() {
                            getDocStatus(getApplicationContext(), ids, docId);
                        }
                    }.start();
                    mUpdateTimes--;
                } else {
                    AlertDialog dialog = new AlertDialog.Builder(this)
                            .setTitle("File status info")
                            .setMessage("Please check document status manually letter")
                            .setNegativeButton("Close", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            })
                            .create();
                    if (getRunningActivities().contains("ComponentInfo{com.galleryapp.activities/com.galleryapp.activities.GalleryActivity}")) {
                        dialog.show();
                    }
                }
            } else {
                mUpdateTimes = 0;
            }
        }
    }

    @Override
    public void onDeleteItemsOperation(ArrayList<String> ids, ArrayList<File> checkedImages, ArrayList<File> checkedThumbs) {
        if (ids != null && ids.size() > 0) {
            for (String id : ids) {
                Log.d("CHECKED_IDS", "ID[delete] = " + id);
            }
            for (File checkedImage : checkedImages) {
                Log.d("CHECKED_IDS", "checkedImage[delete] = " + checkedImage);
            }
            for (File checkedThumb : checkedThumbs) {
                Log.d("CHECKED_IDS", "checkedThumb[delete] = " + checkedThumb);
            }
            deleteImage(ids, checkedImages, checkedThumbs);
        }
    }

    @Override
    public void onStartUploadImages(int uploadCount) {
//        this.mUploadCount = uploadCount;
    }
}
