package com.galleryapp.data.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

/**
 * Created by pvg on 24.07.14.
 */
public abstract class HttpBaseProvider extends ContentProvider{

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

    protected final void checkForSync(Cursor c, Uri uri){
        c.moveToNext();
        Log.d(TAG, "c.moveToNext()");
        // Do stuff here
        Log.d(TAG, "checkForSync()");
        c.moveToFirst();
        Log.d(TAG, "c.moveToFirst()");
        c.setNotificationUri(getContext().getContentResolver(), uri);
        Log.d(TAG, "c.setNotificationUri()");
    }

}
