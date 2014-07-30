package com.galleryapp;

import com.galleryapp.application.GalleryApp;
import com.galleryapp.data.model.ChannelsObj;
import com.galleryapp.data.model.FileUploadObj;

import java.net.MalformedURLException;
import java.net.URL;

import retrofit.Callback;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.Header;
import retrofit.http.Headers;
import retrofit.http.POST;
import retrofit.http.Path;
import retrofit.http.Query;
import retrofit.mime.TypedFile;

/**
 * Created by pvg on 21.07.14.
 */
public final class ScanRestService {

    private final ScanServices mRestService;
    private final GalleryApp mApp;

    public ScanServices getService() {
        return mRestService;
    }

    public interface ScanServices {
        @Headers("Method: GET")
        @GET(Config.GET_CHANNELS_RULE)
        void getChannels(@Query("t") String t, Callback<ChannelsObj> callback);

        @Headers("Method: GET")
        @GET(Config.GET_CHANNELS_RULE)
        ChannelsObj getChannels(@Query("t") String t);

        @Headers("Method: POST")
        @POST(Config.UPLOAD_POST_REQUEST_RULE + "{domain}")
        void uploadFile(@Header("ContentLength") String contentLength, @Body TypedFile typedFile,
                        @Path("domain") String domain, @Query("t") String t, Callback<FileUploadObj> callback);

        @Headers("Method: POST")
        @POST(Config.UPLOAD_POST_REQUEST_RULE + "{domain}")
        FileUploadObj uploadFile(@Header("ContentLength") String contentLength, @Body TypedFile typedFile,
                                 @Path("domain") String domain, @Query("t") String t);
    }

    public static class Builder {

        private final String url;

        public Builder(String apiUrl) {
            url = apiUrl;
        }

        public ScanRestService build(){
            return new ScanRestService(this);
        }
    }

    private ScanRestService(Builder builder) {
        mApp = GalleryApp.getInstance();
        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint(builder.url)
                .setRequestInterceptor(new BaseIntercaptor(builder.url))
                .build();
        mRestService = restAdapter.create(ScanServices.class);
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
        public void intercept(RequestFacade request) {
            request.addHeader("Host", parsedUrl.getAuthority());
            request.addHeader("ContentType", "application/binary");
        }
    }
}
