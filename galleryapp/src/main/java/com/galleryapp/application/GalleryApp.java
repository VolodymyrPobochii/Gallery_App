package com.galleryapp.application;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.app.ProgressDialog;
import android.content.ContentProviderOperation;
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
import com.galleryapp.data.provider.GalleryDBContent;
import com.galleryapp.data.provider.GalleryDBProvider;
import com.nostra13.universalimageloader.cache.disc.impl.UnlimitedDiscCache;
import com.nostra13.universalimageloader.cache.memory.impl.LruMemoryCache;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.utils.StorageUtils;

import java.io.File;
import java.util.ArrayList;

public class GalleryApp extends Application {
    private String token;
    private String domain;
    private SharedPreferences preff;
    private String hostName;
    private String port;
    private String baseUrl;
    private String loginBaseUrl;
    private String cmsBaseUrl;
    private String appVersion;

    public GalleryApp() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
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


}
