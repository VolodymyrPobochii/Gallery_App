package com.galleryapp.data.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.util.Log;

import com.galleryapp.syncadapter.SyncAdapter;
import com.galleryapp.syncadapter.SyncUtils;

/**
 * Created by pvg on 24.07.14.
 */
public abstract class HttpBaseProvider extends ContentProvider {

    private static final String TAG = HttpBaseProvider.class.getSimpleName();

    @Override
    public boolean onCreate() {
        return false;
    }

    @Override
    public abstract Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder);

    @Override
    public abstract String getType(Uri uri);

    @Override
    public abstract Uri insert(Uri uri, ContentValues values);

    @Override
    public abstract int delete(Uri uri, String selection, String[] selectionArgs);

    @Override
    public abstract int update(Uri uri, ContentValues values, String selection, String[] selectionArgs);

    protected final void checkForSync(Cursor c, Uri uri) {
        while (c.moveToNext()) {
            int isSynced = c.getInt(GalleryDBContent.GalleryImages.Columns.IS_SYNCED.ordinal());
            int needUpload = c.getInt(GalleryDBContent.GalleryImages.Columns.NEED_UPLOAD.ordinal());
            if (isSynced == 0 && needUpload == 1) {
                c.moveToFirst();
                c.setNotificationUri(getContext().getContentResolver(), uri);
                SyncUtils.TriggerRefresh(SyncAdapter.UPLOAD_FILES);
                break;
            }
        }
        Log.d(TAG, "onPerformSync()::UPLOAD_FILES::c.getCount() = " + c.getCount());
    }

}
