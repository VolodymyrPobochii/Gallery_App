package com.galleryapp.services;

import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Intent;
import android.net.http.AndroidHttpClient;
import android.os.IBinder;

import com.galleryapp.Config;
import com.galleryapp.Logger;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;

public final class LoginService extends IntentService {

    private final String TAG = this.getClass().getSimpleName();

    public static final int CONNECTION_START = 1001;
    public static final int CONNECTION_SUCCESS = 1002;
    public static final int CONNECTION_ERROR = 1003;
    public static final int AUTH_FINISHED = 1004;
    public static final String AUTH_PROBLEM = "auth_problem";
    private static final int MAX_SOCKET_TIMEOUT = 60000;
    private String login;
    private String pass;
    private String uuid;
    private String hostName;
    private String port;
    private PendingIntent loginProgressIntent;
    private AndroidHttpClient httpClient;

    public LoginService() {
        super("Empty constructor");
        Logger.d(TAG, "OAuthService()");
    }


    // This method is executed in background when this service is started
    @Override
    protected void onHandleIntent(Intent intent) {
        Logger.d(TAG, "PostLogin :: onHandleIntent()");
        /*Get login progress intent*/
        loginProgressIntent = intent.getParcelableExtra("loginProgressIntent");

        String responseToken = "";
        hostName = intent.getStringExtra("hostName");
        port = intent.getStringExtra("port");
        String baseUrl = intent.getStringExtra("baseUrl");
        login = intent.getStringExtra("login");
        pass = intent.getStringExtra("pass");
        String url = baseUrl + Config.LOGIN_SUFFIX;

        String query = null;
        try {
            query = String.format("%s=%s&%s=%s", Config.USER, URLEncoder.encode(login, "UTF-8"), Config.PASS, pass);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        url += "?" + query;

        Logger.d(TAG, "PostLogin :: onHandleIntent() :: url = " + url);

        connectionMessage(CONNECTION_START);

        responseToken = DownloadData(url);
        Logger.d(TAG, "responseToken = " + responseToken);
        if (!responseToken.isEmpty()) {
            responseToken = responseToken.substring(1);
            responseToken = responseToken.substring(0, responseToken.length() - 1);
            callBackToClient(AUTH_FINISHED, "responseToken", responseToken);
        } else {
            callBackToClient(AUTH_FINISHED, Config.SERVER_CONNECTION, AUTH_PROBLEM);
        }
    }

    private void connectionMessage(int connectionMessage) {
        try {
            loginProgressIntent.send(connectionMessage);
        } catch (PendingIntent.CanceledException e) {
            e.printStackTrace();
        }
    }

    private void callBackToClient(int connectionMessage, String responseName, String responseValue) {
        try {
            Intent intent = new Intent().putExtra(responseName, responseValue);
            loginProgressIntent.send(LoginService.this, connectionMessage, intent);
        } catch (PendingIntent.CanceledException e) {
            e.printStackTrace();
        }
    }

    // ---Connects using HTTP GET---
    public InputStream OpenHttpGETConnection(String url) {
        InputStream inputStream = null;
        try {
            URL parsedUrl = new URL(url);
            httpClient = AndroidHttpClient.newInstance("android", getApplicationContext());
            HttpGet get = new HttpGet(parsedUrl.toURI());
            get.addHeader("Host", parsedUrl.getAuthority());
            Logger.d(TAG, "OpenHttpGETConnection::addHeader = " + "Host::" + parsedUrl.getAuthority());
            get.addHeader("Content-Type", "application/json");
            HttpResponse httpResponse = httpClient.execute(get);
            inputStream = httpResponse.getEntity().getContent();
        } catch (Exception e) {
            e.printStackTrace();
            Logger.d(TAG, "OpenHttpGETConnection::Exception = " + e.toString());
        }
        connectionMessage(CONNECTION_SUCCESS);
        return inputStream;
    }

    /**
     * A method to download data from url
     */
    public String DownloadData(String URL) {
        Logger.d(TAG, "PostLoginService :: onHandleIntent() :: proccessIntent :: DownloadData()");
        int BUFFER_SIZE = 2000;
        InputStream in = null;
        InputStreamReader isr = null;
        try {
            //in = OpenHttpPOSTConnection(URL);
            in = OpenHttpGETConnection(URL);
        } catch (Exception e) {
            Logger.d(TAG, "DownloadData::Exception = " + e.toString());
            e.printStackTrace();
            connectionMessage(CONNECTION_ERROR);
            return "";
        }
        try {
            isr = new InputStreamReader(in);
        } catch (Exception e) {
            Logger.d(TAG, "DownloadData::InputStreamReader::Exception = " + e.toString());
            e.printStackTrace();
            connectionMessage(CONNECTION_ERROR);
            return "";
        }
        int charRead;
        String str = "";
        char[] inputBuffer = new char[BUFFER_SIZE];
        try {
            while ((charRead = isr.read(inputBuffer)) > 0) {
                // ---convert the chars to a String---
                String readString = String.copyValueOf(inputBuffer, 0, charRead);
                str += readString;
                inputBuffer = new char[BUFFER_SIZE];
            }
            in.close();
        } catch (IOException e) {
            Logger.d(TAG, "DownloadData::isr.read::IOException = " + e.toString());
            e.printStackTrace();
            connectionMessage(CONNECTION_ERROR);
            return "";
        }
        httpClient.close();
        return str;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //startForeground(1, new Notification());
        Logger.d(TAG, "OAuthService :: onCreate()");
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        Logger.d(TAG, "OAuthService :: onStart() :: startId = " + Integer.toString(startId));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Logger.d(TAG, "OAuthService :: onStartCommand() :: startId = " + Integer.toString(startId) + " :: flags = " + Integer.toString(flags));
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Logger.d(TAG, "OAuthService :: onDestroy()");
        // Destroy connection mananger
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        Logger.d(TAG, "OAuthService :: onBind()");
        return super.onBind(intent);
    }

}
