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
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.RemoteException;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import com.galleryapp.Config;
import com.galleryapp.R;
import com.galleryapp.activities.GalleryActivity;
import com.galleryapp.activities.PrefActivity;
import com.galleryapp.data.model.ImageObj;
import com.galleryapp.data.model.SubmitDocumentObj.CaptureItemObj.BatchObj.Folder.Document;
import com.galleryapp.data.provider.GalleryDBContent;
import com.galleryapp.data.provider.GalleryDBProvider;
import com.galleryapp.fragmernts.GalleryFragment;
import com.galleryapp.syncadapter.SyncAdapter;
import com.galleryapp.syncadapter.SyncUtils;
import com.nostra13.universalimageloader.cache.disc.impl.UnlimitedDiscCache;
import com.nostra13.universalimageloader.cache.memory.impl.LruMemoryCache;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.utils.StorageUtils;

import org.apache.http.protocol.HTTP;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GalleryApp extends Application implements GalleryFragment.OnFragmentInteractionListener {

    public static final String TAG = GalleryApp.class.getSimpleName();
    private static final long TIMER_TICK = 100l;

    private static GalleryApp sInstance;

    private String login;
    private String password;
    private String token;
    private String domain;
    private String captureChannelCode;
    private String indexString;
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

    public static GalleryApp getInstance() {
        return sInstance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (sInstance == null) {
            sInstance = this;
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
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.d(TAG, "width=" + newConfig.screenWidthDp +
                "height=" + newConfig.screenHeightDp);
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

    private void deleteFilesRecursive(final ArrayList<File> images, final ArrayList<File> thumbs) {
        final ExecutorService service = Executors.newFixedThreadPool(1);
        service.submit(new Runnable() {
            @Override
            public void run() {
                for (File image : images) {
                    deleteFileRecursive(image);
                }
                for (File thumb : thumbs) {
                    deleteFileRecursive(thumb);
                }
                service.shutdown();
            }
        });
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
        return customAlertDialog(getApplicationContext(), getString(R.string.no_connection),
                getString(R.string.close), false, null, false, false);
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

    public void prepareFilesForSync(List<Integer> checkedIds) {
        for (Integer id : checkedIds) {
            Log.d(TAG, "prepareFilesForSync()::checkedID = " + Integer.toString(id));
        }

        Cursor cursor = getContentResolver().query(GalleryDBContent.GalleryImages.CONTENT_URI,
                GalleryDBContent.GalleryImages.PROJECTION,
                null, null, null);

        if (cursor.getCount() > 0) {
            Log.d(TAG, "prepareFilesForSync()::ID.ordinal = " +
                    Integer.toString(GalleryDBContent.GalleryImages.Columns.ID.ordinal()));
            while (cursor.moveToNext()) {
                Log.d(TAG, "prepareFilesForSync()::cursorID = " +
                        Integer.toString(cursor.getInt(GalleryDBContent.GalleryImages.Columns.ID.ordinal())));
            }
        }

        ContentValues cv = new ContentValues();
        cv.put(GalleryDBContent.GalleryImages.Columns.IS_SYNCED.getName(), 0);
        cv.put(GalleryDBContent.GalleryImages.Columns.NEED_UPLOAD.getName(), 1);
        cv.put(GalleryDBContent.GalleryImages.Columns.STATUS.getName(), "Not synced");

        ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();
        for (Integer id : checkedIds) {
            operations.add(ContentProviderOperation.newUpdate(GalleryDBContent.GalleryImages.CONTENT_URI)
                    .withValues(cv)
                    .withSelection(GalleryDBContent.GalleryImages.Columns.ID.getName() + "=?", new String[]{String.valueOf(id)})
                    .build());
        }
        if (operations.size() > 0) {
            try {
                int updated = getContentResolver().applyBatch(GalleryDBProvider.AUTHORITY, operations).length;
                Log.d(TAG, "prepareFilesForSync()::applyBatch()::" + updated);
            } catch (RemoteException e) {
                e.printStackTrace();
            } catch (OperationApplicationException e) {
                e.printStackTrace();
            }
            SyncUtils.TriggerRefresh(SyncAdapter.UPLOAD_FILES);
            Log.d(TAG, "prepareFilesForSync()::SyncUtils.TriggerRefresh(UPLOAD_FILES)");
        }
    }

    /*public ArrayList<String> getRunningActivities() {
        ArrayList<String> runningActivities = new ArrayList<String>();
        ActivityManager activityManager = (ActivityManager) getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> services = activityManager.getRunningTasks(Integer.MAX_VALUE);
        for (ActivityManager.RunningTaskInfo service : services) {
            runningActivities.add(0, service.topActivity.toString());
        }
        return runningActivities;
    }*/

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

    public String getIndexString() {
        return indexString;
    }

    public void setIndexString(String indexString) {
        this.indexString = indexString;
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
    }
}