package com.galleryapp.data.model;

import java.util.ArrayList;

public class ChannelsObj {

    private ArrayList<ChannelObj> Channels;
    private Integer ErrorCode;
    private String ErrorMessage;

    public Integer getErrorCode() {
        return ErrorCode;
    }

    public void setErrorCode(Integer errorCode) {
        ErrorCode = errorCode;
    }

    public String getErrorMessage() {
        return ErrorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        ErrorMessage = errorMessage;
    }

    public ArrayList<ChannelObj> getChannels() {
        return Channels;
    }

    public void setChannels(ArrayList<ChannelObj> channels) {
        Channels = channels;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (ChannelObj channel : Channels) {
            sb.append(channel.toString()).append("::");
        }
        return "Channels : " + sb.toString() + "/" + Channels.size()
                + " ErrorCode = " + ErrorCode
                + " ErrorMessage = " + ErrorMessage;
    }

    private static class ChannelObj {

        private String Code;
        private String Domain;
        private String Name;

        public String getCode() {
            return Code;
        }

        public void setCode(String code) {
            Code = code;
        }

        public String getDomain() {
            return Domain;
        }

        public void setDomain(String domain) {
            Domain = domain;
        }

        public String getName() {
            return Name;
        }

        public void setName(String name) {
            Name = name;
        }

        @Override
        public String toString() {
            return "Code=" + Code + " Domain=" + Domain + " Name=" + Name;
        }
    }
}
