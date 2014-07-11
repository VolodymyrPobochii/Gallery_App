package com.galleryapp.interfaces;

import com.galleryapp.data.model.ChannelsObj;

public interface GetChannelsEventListener {
    void onGetChannels(ChannelsObj response);
}