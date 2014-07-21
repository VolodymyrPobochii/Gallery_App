package com.galleryapp;

import java.net.MalformedURLException;
import java.net.URL;

import retrofit.RequestInterceptor;
import retrofit.RestAdapter;

/**
 * Created by pvg on 21.07.14.
 */
public abstract class BaseRestAdapter {

    protected final Object mRestService;

    public BaseRestAdapter(String apiUrl, Class<?> clazz) {
        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint(apiUrl)
                .setRequestInterceptor(new BaseIntercaptor(apiUrl))
                .build();

        mRestService = restAdapter.create(clazz);
    }

    private class BaseIntercaptor implements RequestInterceptor {
        private URL parsedUrl;

        public BaseIntercaptor(String apiUrl) {
            try {
                this.parsedUrl = new URL(apiUrl);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void intercept(RequestInterceptor.RequestFacade request) {
            request.addHeader("Host", parsedUrl.getAuthority());
            request.addHeader("ContentType", "application/binary");
            request.addHeader("Method", "GET");
        }
    }
}
