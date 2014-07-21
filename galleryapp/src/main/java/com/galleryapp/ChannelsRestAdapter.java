package com.galleryapp;

import com.galleryapp.data.model.ChannelsObj;

import retrofit.Callback;
import retrofit.http.GET;
import retrofit.http.Query;

/**
 * Created by pvg on 21.07.14.
 */
public final class ChannelsRestAdapter extends BaseRestAdapter {

    private final String TAG = this.getClass().getSimpleName();

    private final ChannelsService mCnannelsService;

    private interface ChannelsService {
        @GET(Config.GET_CHANNELS_RULE)
        void getChannels(@Query("t") String t, Callback<ChannelsObj> callback);

        @GET(Config.GET_CHANNELS_RULE)
        ChannelsObj getChannels(@Query("t") String t);
    }

    public ChannelsRestAdapter(String apiUrl) {
        super(apiUrl, ChannelsService.class);
        mCnannelsService = (ChannelsService) mRestService;
    }

    public final void execute(String token, Callback<ChannelsObj> callback) {
        mCnannelsService.getChannels(token, callback);
    }

    public final ChannelsObj execute(String token) {
        return mCnannelsService.getChannels(token);
    }
}
