package com.galleryapp;

import com.galleryapp.data.model.ChannelsObj;

public interface GetChannelsEventListener {
    void onGetChannels(ChannelsObj response);
}