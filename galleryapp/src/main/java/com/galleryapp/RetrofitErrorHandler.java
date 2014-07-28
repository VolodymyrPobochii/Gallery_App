package com.galleryapp;

import android.util.Log;

import retrofit.ErrorHandler;
import retrofit.RetrofitError;
import retrofit.client.Response;

public final class RetrofitErrorHandler implements ErrorHandler {
    private static final String TAG = RetrofitErrorHandler.class.getSimpleName();

    @Override
    public Throwable handleError(RetrofitError cause) {
        if (cause.isNetworkError()) {
            Log.e(TAG, "Network error");
        }
        Response r = cause.getResponse();
        if (r != null) {
            Log.e(TAG, r.getStatus() + " :: " + r.getReason() + " :: " + cause.getLocalizedMessage());
            return new NewTrowable(cause);
        }
        return cause;
    }

    private class NewTrowable extends Throwable{

        public NewTrowable() {
            super();
        }

        public NewTrowable(String detailMessage) {
            super(detailMessage);
        }

        public NewTrowable(String detailMessage, Throwable cause) {
            super(detailMessage, cause);
        }

        public NewTrowable(Throwable cause) {
            super(cause);
            printStackTrace();
        }
    }
}