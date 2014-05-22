package com.galleryapp.data.model;

/**
 * Created by vovichen on 5/22/14.
 */
public class DocSubmittedObj {
    String Id;
    Integer DocumentCount;
    Integer ErrorCode;
    String ErrorMessage;

    public String getId() {
        return Id;
    }

    public void setId(String id) {
        Id = id;
    }

    public Integer getDocumentCount() {
        return DocumentCount;
    }

    public void setDocumentCount(Integer documentCount) {
        DocumentCount = documentCount;
    }

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
}
