package com.galleryapp.asynctasks;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.galleryapp.application.GalleryApp;
import com.galleryapp.data.model.DocSubmittedObj;
import com.galleryapp.data.model.SubmitDocumentObj;
import com.galleryapp.interfaces.ProgressiveEntityListener;
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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class SubmitDocumentTask extends AsyncTask<String, Integer, DocSubmittedObj> {

    private static final String HEADER_CONTENT_TYPE = "ContentType";
    private final OkHttpClient client;
    private final ArrayList<String> mIds;
    private final GalleryApp app;
    private SubmitDocumentObj mPostData;
    private String url;
    private ProgressiveEntityListener mProgressUploadListener;
    private byte[] mPostJson;

    public SubmitDocumentTask(Context context, SubmitDocumentObj postData, ArrayList<String> ids) {
        this.mPostData = postData;
        this.mIds = ids;
        this.client = new OkHttpClient();
        this.app = GalleryApp.getInstance();
        setProgressUploadListener((ProgressiveEntityListener) context);
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        Log.d("UPLOAD", "onPreExecute()");
    }

    @Override
    protected DocSubmittedObj doInBackground(String... params) {
        DocSubmittedObj response = null;
        Gson gson = new Gson();
        try {
            String json = gson.toJson(mPostData);
            Log.d("UPLOAD", "DocSubmittedObj = " + json);
            mPostJson = json.getBytes();
            response = postFile(mPostJson, params[0]);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return response;
    }

    @Override
    protected void onProgressUpdate(Integer... progress) {
        super.onProgressUpdate(progress[0]);
//        Log.d("UPLOAD", "onProgressUpdate() :: progress:" + progress[0]);
    }

    @Override
    protected void onPostExecute(DocSubmittedObj response) {
        super.onPostExecute(response);
        Log.d("UPLOAD", "onPostExecute()");

        mProgressUploadListener.onDocSubmitted(response, mIds);
    }

    /*fake*/
    private DocSubmittedObj postFile(final byte[] postData, String url) throws IOException {
        URL parsedUrl = new URL(url);
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("Host", parsedUrl.getAuthority());
        map.put("ContentType", "application/binary");
        map.put("Method", "POST");
        map.put("ContentLength", String.valueOf(postData.length));
        Log.d("UPLOAD", "postFile() :: ContentLength:" + String.valueOf(postData.length));

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
        String uploadedFile = getContent(response);
        Gson gson = new Gson();
        DocSubmittedObj submittedObj = gson.fromJson(uploadedFile, DocSubmittedObj.class);
        return submittedObj;
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
        connection.setRequestMethod("POST");
        addBodyIfExists(connection);
    }

    private void addBodyIfExists(HttpURLConnection connection) throws IOException {
        Log.d("ProgressEntity", "addBodyIfExists()");
        if (mPostJson != null) {
            final long length = mPostJson.length;
            connection.setDoOutput(true);
            connection.addRequestProperty(HEADER_CONTENT_TYPE, "application/binary");
            DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());
            InputStream fileInputStream = new ByteArrayInputStream(mPostJson);
            int bufferSize = 512;
            byte[] buffer = new byte[bufferSize];
            // Read file
            int bytesRead = fileInputStream.read(buffer, 0, bufferSize);
            int bytesAvailable;
            int maxBufferSize = 512;
            int progress = 0;

            while (bytesRead > 0) {
                progress += bytesRead;
                outputStream.write(buffer, 0, bytesRead);
                bytesAvailable = fileInputStream.available();
                publishProgress((int) (progress * 100 / length));
                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                buffer = new byte[bufferSize];
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);
            }//end of while statement
            fileInputStream.close();
            outputStream.flush();
            outputStream.close();
        }
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

    public void setProgressUploadListener(ProgressiveEntityListener mProgressUploadListener) {
        this.mProgressUploadListener = mProgressUploadListener;
    }
}