package com.galleryapp.services;

import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.galleryapp.Config;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * Created by user on 10/18/13.
 */
public final class LoginService extends IntentService {

    public static final int CONNECTION_START = 1001;
    public static final int CONNECTION_SUCCESS = 1002;
    public static final int CONNECTION_ERROR = 1003;
    public static final int AUTH_FINISHED = 1004;
    public static final String AUTH_PROBLEM = "auth_problem";
    private String login;
    private String pass;
    private String uuid;
    private String hostName;
    private String port;
    private PendingIntent loginProgressIntent;

    public LoginService() {
        super("Empty constructor");
        Log.d("PostLoginServiceReceiver", "OAuthService()");

    }


    // This method is executed in background when this service is started
    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d("PostLoginServiceReceiver", "PostLogin :: onHandleIntent()");
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

        Log.d("PostLoginServiceReceiver", "PostLogin :: onHandleIntent() :: url = " + url);

        connectionMessage(CONNECTION_START);

        responseToken = DownloadData(url);
        Log.d("PostLoginServiceReceiver", "responseToken = " + responseToken);
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
            HttpClient httpclient = new DefaultHttpClient();
            HttpGet get = new HttpGet(url);
            get.addHeader("Host", hostName + ":" + port);
            Log.d("PostLoginServiceReceiver", "OpenHttpGETConnection::addHeader = " + "Host::" + hostName + ":" + port);
            get.addHeader("Content-Type", "application/json");
            HttpResponse httpResponse = httpclient.execute(get);
            inputStream = httpResponse.getEntity().getContent();
        } catch (Exception e) {
            e.printStackTrace();
            Log.d("PostLoginServiceReceiver", "OpenHttpGETConnection::Exception = " + e.toString());
//            Log.d("InputStream", e.getLocalizedMessage());
        }
        connectionMessage(CONNECTION_SUCCESS);
        return inputStream;
    }

    /**
     * A method to download data from url
     */
    public String DownloadData(String URL) {
        Log.d("PostLoginReceiver", "PostLoginService :: onHandleIntent() :: proccessIntent :: DownloadData()");
        int BUFFER_SIZE = 2000;
        InputStream in = null;
        InputStreamReader isr = null;
        try {
            //in = OpenHttpPOSTConnection(URL);
            in = OpenHttpGETConnection(URL);
        } catch (Exception e) {
            Log.d("PostLoginServiceReceiver", "DownloadData::Exception = " + e.toString());
            e.printStackTrace();
            connectionMessage(CONNECTION_ERROR);
            return "";
        }
        try {
            isr = new InputStreamReader(in);
        } catch (Exception e) {
            Log.d("PostLoginServiceReceiver", "DownloadData::InputStreamReader::Exception = " + e.toString());
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
            Log.d("PostLoginServiceReceiver", "DownloadData::isr.read::IOException = " + e.toString());
            e.printStackTrace();
            connectionMessage(CONNECTION_ERROR);
            return "";
        }
        return str;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //startForeground(1, new Notification());
        Log.d("PostLoginServiceReceiver", "OAuthService :: onCreate()");
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        Log.d("PostLoginServiceReceiver", "OAuthService :: onStart() :: startId = " + Integer.toString(startId));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("PostLoginServiceReceiver", "OAuthService :: onStartCommand() :: startId = " + Integer.toString(startId) + " :: flags = " + Integer.toString(flags));
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Log.d("PostLoginServiceReceiver", "OAuthService :: onDestroy()");
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d("PostLoginServiceReceiver", "OAuthService :: onBind()");
        return super.onBind(intent);
    }

}
