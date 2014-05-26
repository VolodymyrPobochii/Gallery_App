package com.galleryapp.application;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.app.ProgressDialog;
import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Handler;
import android.os.RemoteException;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import android.widget.Toast;

import com.galleryapp.Config;
import com.galleryapp.DocumentStatusTask;
import com.galleryapp.R;
import com.galleryapp.SubmitDocumentTask;
import com.galleryapp.UploadFileTask2;
import com.galleryapp.activities.GalleryActivity;
import com.galleryapp.activities.PrefActivity;
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
import com.nostra13.universalimageloader.cache.disc.impl.UnlimitedDiscCache;
import com.nostra13.universalimageloader.cache.memory.impl.LruMemoryCache;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.utils.StorageUtils;

import org.apache.http.entity.FileEntity;

import java.io.File;
import java.util.ArrayList;
import java.util.UUID;

public class GalleryApp extends Application {

    private static GalleryApp instance;

    private String token;
    private String domain;
    private SharedPreferences preff;
    private String hostName;
    private String port;
    private String baseUrl;
    private String loginBaseUrl;
    private String cmsBaseUrl;
    private String appVersion;
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

        setPreff(PrefActivity.getPrefs(getApplicationContext()));
        setUpHost();
//        initRestAdapter();
//        initVolley();
    }

    private void initVolley() {
//        ScanVolley.init(this);
    }

    private void initRestAdapter() {
        // Create a very simple REST adapter which points the FileUpload API endpoint.
     /*   mRestAdapter = new RestAdapter.Builder()
                .setClient(new OkClient())
                .setEndpoint(CaptureServiceRestClient.API_URL)
                .build();*/
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
        if (ni == null) {
            // There are no active networks.
            return false;
        } else
            return true;
    }

    public AlertDialog customAlertDialog(final Context activity, String message,
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
//                        android.os.Process.killProcess(Process.myPid());
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

    private void setUpHost() {
        setHostName(getPreff().getString("hostName", Config.DEFAULT_HOST));
        setPort(getPreff().getString("port", Config.DEFAULT_PORT));
        setDomain(getPreff().getString("domain", Config.DEFAULT_DOMAIN));
        setLoginBaseUrl(Config.URL_PREFIX + getHostName() + ":" + getPort());
        setBaseUrl(Config.URL_PREFIX + getHostName() + ":" + getPort() + Config.DEFAULT_URL_BODY + getDomain());
        setCmsBaseUrl(Config.URL_PREFIX + getHostName() + ":" + getPort() + Config.DEFAULT_CSM_URL_BODY);

        Log.d("BaseActivity", "BaseActivity::LoginBaseURL=" + getLoginBaseUrl());
        Log.d("BaseActivity", "BaseActivity::BaseURL=" + getBaseUrl());
    }

//    public static RestAdapter getRestAdapter() {
//        return mRestAdapter;
//    }

    public static GalleryApp getInstance() {
        return instance;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
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


    public void uploadFile(Context context, final Handler uploadHandler,
                           ArrayList<byte[]> fileBytes, ArrayList<String> filePaths,
                           ArrayList<String> fileNames, ArrayList<Integer> ids) {
        //        Retrofit block
       /* RestAdapter restAdapter = GalleryApp.getRestAdapter();

        final Message msg = uploadHandler.obtainMessage();
        // Create an instance of our FileUpload API interface.
        final CaptureServiceRestClient.CaptureServiceRest captureServiceRest = restAdapter.create(CaptureServiceRestClient.CaptureServiceRest.class);
        // Fetch and print a FileUploadObj.
        assert file != null;
        captureServiceRest.uploadFile(
                Config.METHOD_UPLOAD,
                Config.DEFAULT_PARAM_DOMAIN,
                GalleryApp.getInstance().getToken(),
                file,
                String.valueOf(file.length()),
                new Callback<CaptureServiceRestClient.FileUploadObj>() {
                    @Override
                    public void success(CaptureServiceRestClient.FileUploadObj fileUploadObj, Response response) {
                        msg.obj = response.getReason();
                        Log.d("UPLOAD", "success() :: responseReason = " + response.getReason() +
                                "\nresponseBody = " + response.getBody().toString() +
                                "\nurl = " + response.getUrl() +
                                "\nURI = " + fileUploadObj.Url);
                        uploadHandler.sendMessageDelayed(msg, 500);
                    }

                    @Override
                    public void failure(RetrofitError retrofitError) {
                        msg.obj = retrofitError.getResponse().getReason();
                        Log.d("UPLOAD", "failure() :: response : Message = " + retrofitError.getMessage()
                                + "\nLocalizedMessage = " + retrofitError.getLocalizedMessage()
                                + "\nReason = " + retrofitError.getResponse().getReason()
                                + "\nurl=" + retrofitError.getUrl());
                        uploadHandler.sendMessageDelayed(msg, 500);
                    }
                }
        );*/

//        Volley block
//        new UploadFileVolley(context, fileBytes).execute();

        assert filePaths != null;
        if (isNetworkConnected()) {
            for (String filePath : filePaths) {
                File uploadFile = new File(filePath);
                FileEntity fileEntity = new FileEntity(uploadFile, "application/binary");
                int id = ids.get(filePaths.indexOf(filePath));
                String name = fileNames.get(filePaths.indexOf(filePath));
                UploadFileTask2 uploadFileTask = new UploadFileTask2(context, fileEntity, id, name);
                uploadFileTask.execute();
            }
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

    public int updateImageId(String fileId, String id) {
        ContentValues cv = new ContentValues();
        cv.put(GalleryDBContent.GalleryImages.Columns.FILE_ID.getName(), fileId);
        return getContentResolver().update(GalleryDBContent.GalleryImages.CONTENT_URI, cv,
                GalleryDBContent.GalleryImages.Columns.ID.getName() + "=?", new String[]{id});
    }

    public void submitDocs(Context context, FileUploadObj response, String id, String name, long length) {
        ArrayList<DocumentError> documentErrors = new ArrayList<DocumentError>();
        DocumentError documentError = new DocumentError();
        documentErrors.add(documentError);

        ArrayList<Document> documents = new ArrayList<Document>();
        Document document = new Document();
        document.setIndexSchema("");
        document.setOriginalFileName(name);
        document.setContentType("image/jpg");
        document.setContentLength((int) length);
        document.setUri(response.getUrl());
        document.setExistingCMSUri(null);
        document.setIsEmailManifest(false);
        document.setBody(null);
        documents.add(document);

        ArrayList<Folder> folders = new ArrayList<Folder>();
        Folder folder = new Folder();
        folder.setIndexSchema("");
        folder.setDocuments(documents);
        folder.setDocumentErrors(documentErrors);
        folders.add(folder);

        BatchObj batchObj = new BatchObj();
        batchObj.setIndexSchema("");
        batchObj.setRemovedDocumentUriCSV(null);
        batchObj.setOperationType(1);
        batchObj.setFolders(folders);

        CaptureItemObj captureItemObj = new CaptureItemObj();
        captureItemObj.setId(UUID.randomUUID().toString().replace("-", ""));
        captureItemObj.setChannelCode("root_compositescanchannel");
        captureItemObj.setBatch(batchObj);
        captureItemObj.setIndexData("");
        String parameters = new StringBuilder()
                .append("controller=composite").append("&")
                .append("baseuri=").append(Config.DEFAULT_HOST).append(":").append(Config.DEFAULT_PORT).append("&")
                .append("stampsenabled=false").append("&")
                .append("hidescancontrols=false").append("&")
                .append("dataroot=UserStamps").append("&")
                .append("capturechannelcode=root_CompositeScanChannel").append("&")
                .append("uri=").append("&")
                .append("t=").append(getToken()).append("&")
                .append("d=").append(Config.DEFAULT_PARAM_DOMAIN).append("&")
                .append("sync=true").append("&")
                .append("scheme=http")
                .toString();
        captureItemObj.setParameters(parameters);
        captureItemObj.setChannelType(3);

        SubmitDocumentObj submitDocumentObj = new SubmitDocumentObj();
        submitDocumentObj.setDomain(Config.DEFAULT_PARAM_DOMAIN);
        submitDocumentObj.setCaptureItem(captureItemObj);
        submitDocumentObj.setToken(getToken());

        SubmitDocumentTask submitDocumentTask = new SubmitDocumentTask(context, submitDocumentObj, id, name);
        submitDocumentTask.execute();
    }

    public void getDocStatus(Context context, String id, String docId) {
        DocumentStatusTask statusTask = new DocumentStatusTask(context, id, docId);
        statusTask.execute();
    }

    public int updateImageStatus(String status, String id, String docId) {
        ContentValues cv = new ContentValues();
        cv.put(GalleryDBContent.GalleryImages.Columns.STATUS.getName(), status);
//        cv.put(GalleryDBContent.GalleryImages.Columns.IS_SYNCED.getName(), 1);
        return getContentResolver().update(GalleryDBContent.GalleryImages.CONTENT_URI, cv,
                GalleryDBContent.GalleryImages.Columns.ID.getName() + "=?" + " AND " +
                        GalleryDBContent.GalleryImages.Columns.FILE_ID.getName() + "=?",
                new String[]{id, docId}
        );
    }
}
