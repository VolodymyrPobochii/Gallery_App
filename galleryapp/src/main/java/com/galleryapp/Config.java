package com.galleryapp;

/**
 * Created by Admin on 25.04.2014.
 */
public class Config {
    public Config() {
    }

    public static final String DEFAULT_HOST = "soldevqa06.eccentex.com";
    public static final String USER = "u";
    public static final String PASS = "p";

    public static final String DEFAULT_DOMAIN = "/103_FixedBoNames_Production.tenant41";
    public static final String DEFAULT_PORT = "9004";

//    public static final String DEFAULT_PORT = "8086";
//    public static final String DEFAULT_DOMAIN = "/UA103_Production.tenant62";

    public static final String DEFAULT_URL_BODY = "/BDS.WebService/DataServiceRest.svc/post";

    public static final String DEFAULT_CSM_URL_BODY = "/CMS.WebService/CMSServiceRest.svc";
    public static final String DEFAULT_CALL_DAYS_HISTORY = "5";
    public static final String DEFAULT_UPDATE_FREQ = "30";
    public static final String DEFAULT_SYNC_PERIOD = "3";

    public static final String URL_PREFIX = "http://";
    public static final String LOGIN_SUFFIX = "/Security.WebService/AuthenticationServiceRest.svc/login";

    public static final String MOBILE_CREATE_RESOURCE_RULE = "/createResource";
    public static final String MOBILE_UPLOAD_DOCUMENT_RULE = "/root_mobile_upload_document";
    public static final String AMBUL_DOCUMENTS = "/ambulDocuments/";

    public static final String CONTENT_TYPE_IMAGE_JPG = "image/jpg";
    public static final String SERVER_CONNECTION = "server_connection";
}
