package com.galleryapp;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.galleryapp.application.GalleryApp;
import com.galleryapp.data.model.ChannelsObj;
import com.google.gson.Gson;
import com.squareup.okhttp.OkHttpClient;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class GetChannelsTask extends AsyncTask<String, Void, ChannelsObj> {

    private final OkHttpClient client;
    private Context mContext;
    private String url;
    private GetChannelsEventListener mChannelsEventListener;
    private ArrayList<String> mIds;

    public GetChannelsTask(Context context) {
        this.mContext = context;
        this.client = new OkHttpClient();
        setChannelsEventListener((GetChannelsEventListener) context);
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        Log.d("UPLOAD", "onPreExecute()");
    }

    @Override
    protected ChannelsObj doInBackground(String... params) {
        ChannelsObj response = null;
        try {
            response = postFile(params[0]);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return response;
    }

    @Override
    protected void onPostExecute(ChannelsObj response) {
        super.onPostExecute(response);
        Log.d("UPLOAD", "onPostExecute()");
        mChannelsEventListener.onGetChannels(response);
    }

    /*fake*/
    private ChannelsObj postFile(String url) throws IOException {
        GalleryApp app = GalleryApp.getInstance();
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("Host", app.getDomain() + ":" + app.getPort());
        map.put("ContentType", "application/binary");
        map.put("Method", "GET");
        URL parsedUrl = new URL(url);

        HttpURLConnection connection = openConnection(parsedUrl);
        for (String headerName : map.keySet()) {
            connection.addRequestProperty(headerName, map.get(headerName));
        }
        Log.d("UPLOAD", "setConnectionParametersForRequest() :: BEGIN");
        setConnectionParametersForRequest(connection);
        Log.d("UPLOAD", "setConnectionParametersForRequest() :: END");
        // Initialize HttpResponse with data from the HttpURLConnection.
        Log.d("UPLOAD", "ProtocolVersion() :: BEGIN");
        ProtocolVersion protocolVersion = new ProtocolVersion("HTTP", 1, 1);
        int responseCode = connection.getResponseCode();
        if (responseCode == -1) {
            // -1 is returned by getResponseCode() if the response code could not be retrieved.
            // Signal to the caller that something was wrong with the connection.
            throw new IOException("Could not retrieve response code from HttpUrlConnection.");
        }
        Log.d("UPLOAD", "ProtocolVersion() :: END");
        Log.d("UPLOAD", "BasicStatusLine() :: BEGIN");
        StatusLine responseStatus = new BasicStatusLine(protocolVersion, connection.getResponseCode(), connection.getResponseMessage());
        Log.d("UPLOAD", "BasicStatusLine() :: END");
        Log.d("UPLOAD", "BasicHttpResponse() :: BEGIN");
        BasicHttpResponse response = new BasicHttpResponse(responseStatus);
        response.setEntity(entityFromConnection(connection));
        for (Map.Entry<String, List<String>> header : connection.getHeaderFields().entrySet()) {
            if (header.getKey() != null) {
                Header h = new BasicHeader(header.getKey(), header.getValue().get(0));
                response.addHeader(h);
            }
        }
        Log.d("UPLOAD", "BasicHttpResponse() :: END");
        String channels = getContent(response);
        Log.d("UPLOAD", "BasicHttpResponse() :: channels = " + channels);
        channels = "{\"Channels\":[{\"Code\":\"root_ScanCHANNELAAAA\",\"Domain\":\"TestOcrCreateNew_Production.54\",\"Name\":\"ScanCHANNELAAAA\"},{\"Code\":\"root_CompositeScanChannel\",\"Domain\":\"das_Production.54\",\"Name\":\"CompositeScanChannel\"}],\"ErrorCode\":0,\"ErrorMessage\":null}";
        JSONObject jo = null;
        try {
            jo = new JSONObject(channels);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        assert jo != null;
        return new Gson().fromJson(jo.toString(), ChannelsObj.class);
    }

    public String getContent(HttpResponse response) {
        BufferedReader rd = null;
        try {
            rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        String body;
        String content = "";
        try {
            if (rd != null) {
                while ((body = rd.readLine()) != null) {
                    content += body + "\n";
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return content.trim();
    }

    /**
     * Opens an {@link java.net.HttpURLConnection} with parameters.
     *
     * @param url
     * @return an open connection
     * @throws java.io.IOException
     */
    private HttpURLConnection openConnection(URL url) throws IOException {
        HttpURLConnection connection = createConnection(url);

        int timeoutMs = 60 * 1000;
        connection.setConnectTimeout(timeoutMs);
        connection.setReadTimeout(timeoutMs);
        connection.setUseCaches(false);
        connection.setDoInput(true);

        return connection;
    }

    private HttpURLConnection createConnection(URL url) throws IOException {
        return client.open(url);
    }

    private void setConnectionParametersForRequest(HttpURLConnection connection) throws IOException {
        connection.setRequestMethod("GET");
    }

    /**
     * Initializes an {@link org.apache.http.HttpEntity} from the given {@link java.net.HttpURLConnection}.
     *
     * @param connection
     * @return an HttpEntity populated with data from <code>connection</code>.
     */
    private HttpEntity entityFromConnection(HttpURLConnection connection) {
        Log.d("UPLOAD", "entityFromConnection() :: BEGIN");
        BasicHttpEntity entity = new BasicHttpEntity();
        InputStream inputStream;
        try {
            inputStream = connection.getInputStream();
        } catch (IOException ioe) {
            inputStream = connection.getErrorStream();
        }
        entity.setContent(inputStream);
        entity.setContentLength(connection.getContentLength());
        entity.setContentEncoding(connection.getContentEncoding());
        entity.setContentType(connection.getContentType());
        Log.d("UPLOAD", "entityFromConnection() :: END");
        return entity;
    }

    public void setChannelsEventListener(GetChannelsEventListener mChannelsEventListener) {
        this.mChannelsEventListener = mChannelsEventListener;
    }
}