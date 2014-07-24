package com.syncadapter.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import com.syncadapter.SyncUtils;


public abstract class BaseProvider extends ContentProvider {

    @Override
    public boolean onCreate() {
        return true;
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

    protected void refreshCurrencyRates(){
        SyncUtils.TriggerRefresh(0);
    }
}
