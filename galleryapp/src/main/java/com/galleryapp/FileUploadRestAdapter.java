package com.galleryapp;

import com.galleryapp.data.model.FileUploadObj;

import org.apache.http.entity.FileEntity;

import retrofit.Callback;
import retrofit.http.Body;
import retrofit.http.POST;
import retrofit.http.Path;
import retrofit.http.Query;
import retrofit.mime.TypedFile;

/**
 * Created by pvg on 21.07.14.
 */
public final class FileUploadRestAdapter extends BaseRestAdapter {

    private final String TAG = this.getClass().getSimpleName();

    private final FileUploadService mUploadService;
    private final TypedFile mTypedFile;

    public TypedFile getTypedFile() {
        return mTypedFile;
    }

    private interface FileUploadService {
        @POST(Config.UPLOAD_POST_REQUEST_RULE + "{domain}")
        void uploadFile(@Body TypedFile typedFile, @Path("domain") String domain, @Query("t") String t, Callback<FileUploadObj> callback);

        @POST(Config.UPLOAD_POST_REQUEST_RULE + "{domain}")
        FileUploadObj uploadFile(@Body TypedFile typedFile, @Path("domain") String domain, @Query("t") String t);
    }

    public FileUploadRestAdapter(String apiUrl, TypedFile typedFile) {
        super(apiUrl, FileUploadService.class);
        mTypedFile = typedFile;
        mUploadService = (FileUploadService) mRestService;
    }

    public final void execute(String domain, String token, Callback<FileUploadObj> callback) {
        mUploadService.uploadFile(mTypedFile, domain, token, callback);
    }

    public final FileUploadObj execute(String domain, String token) {
        return mUploadService.uploadFile(mTypedFile, domain, token);
    }
}
