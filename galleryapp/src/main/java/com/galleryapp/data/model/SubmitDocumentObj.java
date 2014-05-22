package com.galleryapp.data.model;

import java.util.ArrayList;

/**
 * Created by vovichen on 5/22/14.
 */
public class SubmitDocumentObj {
    String Domain;
    CaptureItemObj CaptureItem;
    String Token;

    public String getDomain() {
        return Domain;
    }

    public void setDomain(String domain) {
        Domain = domain;
    }

    public CaptureItemObj getCaptureItem() {
        return CaptureItem;
    }

    public void setCaptureItem(CaptureItemObj captureItem) {
        CaptureItem = captureItem;
    }

    public String getToken() {
        return Token;
    }

    public void setToken(String token) {
        Token = token;
    }

    public static class CaptureItemObj {
        String Id;
        String ChannelCode;
        BatchObj Batch;
        String IndexData;
        String Parameters;
        Integer ChannelType;

        public String getId() {
            return Id;
        }

        public void setId(String id) {
            Id = id;
        }

        public String getChannelCode() {
            return ChannelCode;
        }

        public void setChannelCode(String channelCode) {
            ChannelCode = channelCode;
        }

        public BatchObj getBatch() {
            return Batch;
        }

        public void setBatch(BatchObj batch) {
            Batch = batch;
        }

        public String getIndexData() {
            return IndexData;
        }

        public void setIndexData(String indexData) {
            IndexData = indexData;
        }

        public String getParameters() {
            return Parameters;
        }

        public void setParameters(String parameters) {
            Parameters = parameters;
        }

        public Integer getChannelType() {
            return ChannelType;
        }

        public void setChannelType(Integer channelType) {
            ChannelType = channelType;
        }

        public static class BatchObj {
            String IndexSchema;
            String RemovedDocumentUriCSV;
            Integer OperationType;
            ArrayList<Folder> Folders;

            public String getIndexSchema() {
                return IndexSchema;
            }

            public void setIndexSchema(String indexSchema) {
                IndexSchema = indexSchema;
            }

            public String getRemovedDocumentUriCSV() {
                return RemovedDocumentUriCSV;
            }

            public void setRemovedDocumentUriCSV(String removedDocumentUriCSV) {
                RemovedDocumentUriCSV = removedDocumentUriCSV;
            }

            public Integer getOperationType() {
                return OperationType;
            }

            public void setOperationType(Integer operationType) {
                OperationType = operationType;
            }

            public ArrayList<Folder> getFolders() {
                return Folders;
            }

            public void setFolders(ArrayList<Folder> folders) {
                Folders = folders;
            }

            public static class Folder {
                String IndexSchema;
                ArrayList<Document> Documents;
                ArrayList<DocumentError> DocumentErrors;

                public String getIndexSchema() {
                    return IndexSchema;
                }

                public void setIndexSchema(String indexSchema) {
                    IndexSchema = indexSchema;
                }

                public ArrayList<Document> getDocuments() {
                    return Documents;
                }

                public void setDocuments(ArrayList<Document> documents) {
                    Documents = documents;
                }

                public ArrayList<DocumentError> getDocumentErrors() {
                    return DocumentErrors;
                }

                public void setDocumentErrors(ArrayList<DocumentError> documentErrors) {
                    DocumentErrors = documentErrors;
                }

                public static class Document {
                    String IndexSchema;
                    String OriginalFileName;
                    String ContentType;
                    Integer ContentLength;
                    String Uri;
                    String ExistingCMSUri;
                    Boolean IsEmailManifest;
                    String Body;

                    public String getIndexSchema() {
                        return IndexSchema;
                    }

                    public void setIndexSchema(String indexSchema) {
                        IndexSchema = indexSchema;
                    }

                    public String getOriginalFileName() {
                        return OriginalFileName;
                    }

                    public void setOriginalFileName(String originalFileName) {
                        OriginalFileName = originalFileName;
                    }

                    public String getContentType() {
                        return ContentType;
                    }

                    public void setContentType(String contentType) {
                        ContentType = contentType;
                    }

                    public Integer getContentLength() {
                        return ContentLength;
                    }

                    public void setContentLength(Integer contentLength) {
                        ContentLength = contentLength;
                    }

                    public String getUri() {
                        return Uri;
                    }

                    public void setUri(String uri) {
                        Uri = uri;
                    }

                    public String getExistingCMSUri() {
                        return ExistingCMSUri;
                    }

                    public void setExistingCMSUri(String existingCMSUri) {
                        ExistingCMSUri = existingCMSUri;
                    }

                    public boolean getIsEmailManifest() {
                        return IsEmailManifest;
                    }

                    public void setIsEmailManifest(boolean isEmailManifest) {
                        IsEmailManifest = isEmailManifest;
                    }

                    public String getBody() {
                        return Body;
                    }

                    public void setBody(String body) {
                        Body = body;
                    }
                }

                public static class DocumentError {
                }
            }
        }
    }
}
