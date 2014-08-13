package com.galleryapp;

import com.galleryapp.application.GalleryApp;
import com.galleryapp.data.model.ChannelsObj;
import com.galleryapp.data.model.DocStatusObj;
import com.galleryapp.data.model.DocSubmittedObj;
import com.galleryapp.data.model.ElementData;
import com.galleryapp.data.model.FileUploadObj;
import com.galleryapp.data.model.IndexSchema;

import java.net.MalformedURLException;
import java.net.URL;

import retrofit.Callback;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.client.Response;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.Header;
import retrofit.http.Headers;
import retrofit.http.POST;
import retrofit.http.Path;
import retrofit.http.Query;
import retrofit.mime.TypedByteArray;
import retrofit.mime.TypedFile;

/**
 * Created by pvg on 21.07.14.
 */
public enum ScanRestService {

    INSTANCE(new RestAdapter.Builder());

    private final RestAdapter.Builder mBuilder;
    private ScanServices mRestService;

    /**
     * Enum constructor
     */
    ScanRestService(RestAdapter.Builder builder) {
        mBuilder = builder;
    }

    public RestAdapter.Builder getBuilder() {
        return mBuilder;
    }

    public ScanRestService initRestAdapter(String apiUrl) {
        if (mRestService == null) {
            mRestService = mBuilder.setEndpoint(apiUrl)
                    .setRequestInterceptor(new BaseInterceptor(apiUrl))
                    .build()
                    .create(ScanServices.class);
        }
        return INSTANCE;
    }

    public ScanServices getService() {
        return mRestService;
    }

    @Override
    public String toString() {
        return "RestService{Builder:" + mBuilder.toString() + "(" + mBuilder.hashCode() + ")" +
                " RestService:" + mRestService.toString() + "(" + mRestService.hashCode() + ")}";
    }


    /**
     * Contains all the requests
     */
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

        @Headers("Method: POST")
        @POST(Config.SUBMITT_POST_REQUEST_RULE + "{domain}")
        void submitDoc(@Header("ContentLength") String contentLength, @Body TypedByteArray typedFile,
                        @Path("domain") String domain, @Query("t") String t, Callback<DocSubmittedObj> callback);

        @Headers("Method: POST")
        @POST(Config.SUBMITT_POST_REQUEST_RULE + "{domain}")
        DocSubmittedObj submitDoc(@Header("ContentLength") String contentLength, @Body TypedByteArray typedFile,
                                 @Path("domain") String domain, @Query("t") String t);

        @Headers("Method: GET")
        @GET(Config.STATUS_GET_REQUEST_RULE + "{domain}")
        void getDocStatus(@Path("domain") String domain, @Query("t") String t, @Query("id") String docId, Callback<DocStatusObj> callback);

        @Headers("Method: GET")
        @GET(Config.STATUS_GET_REQUEST_RULE + "{domain}")
        DocStatusObj getDocStatus(@Path("domain") String domain, @Query("t") String t, @Query("id") String docId);

        @Headers("Method: GET")
        @GET(Config.GET_INDEX_SCHEMA_REQUEST_RULE + "{domain}")
        void getIndexScheme(@Path("domain") String domain, @Query("t") String t, @Query("capchcode") String capchcode, Callback<IndexSchema> callback);

        @Headers("Method: GET")
        @GET(Config.GET_INDEX_SCHEMA_REQUEST_RULE + "{domain}")
        IndexSchema getIndexScheme(@Path("domain") String domain, @Query("t") String t, @Query("capchcode") String capchcode);

        @Headers("Method: POST")
        @POST(Config.POST_GET_ITEMS + "{domain}" + "/{ruleCode}")
        void getItems(@Path("domain") String domain, @Path("ruleCode") String ruleCode,
                      @Query("t") String t, Callback<ElementData> callback);

        @Headers("Method: POST")
        @POST(Config.POST_GET_ITEMS + "{domain}" + "/{ruleCode}")
        ElementData getItems(@Path("domain") String domain, @Path("ruleCode") String ruleCode, @Query("t") String t);
    }

    /**
     * Define RequestInterceptor class with
     * common headers for all requests
     */
    private class BaseInterceptor implements RequestInterceptor {
        private URL parsedUrl;

        public BaseInterceptor(String apiUrl) {
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
