package com.galleryapp.asynctasks;

import android.app.NotificationManager;
import android.content.Context;
import android.os.AsyncTask;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.galleryapp.application.GalleryApp;
import com.galleryapp.data.model.FileUploadObj;
import com.galleryapp.interfaces.ProgressiveEntityListener;
import com.google.gson.Gson;
import com.squareup.okhttp.OkHttpClient;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.entity.FileEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class UploadFileTask2 extends AsyncTask<String, Integer, FileUploadObj> {

    private final String TAG = this.getClass().getSimpleName();
    private static final String HEADER_CONTENT_TYPE = "ContentType";
    private final OkHttpClient client;
    private final String mName;
    private final GalleryApp app;
    private Context mContext;
    private FileEntity fileEntity;
    private ProgressiveEntityListener mProgressUploadListener;
    private NotificationManager mNotifyManager;
    private NotificationCompat.Builder mBuilder;
    private int mId;
    private long mLength;

    public UploadFileTask2(Context context, FileEntity fileEntity, int id, String name) {
        this.mContext = context;
        this.fileEntity = fileEntity;
        this.mId = id;
        this.mName = name;
        this.client = new OkHttpClient();
        this.app = GalleryApp.getInstance();
        setProgressUploadListener((ProgressiveEntityListener) context);
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        Log.d(TAG, "onPreExecute()");
        mNotifyManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        mBuilder = new NotificationCompat.Builder(mContext);
        mBuilder.setTicker("Upload begin")
                .setContentTitle("Uploading " + mName)
                .setContentText("Upload in progress")
                .setSmallIcon(android.R.drawable.stat_sys_upload);
        mNotifyManager.notify(mId, mBuilder.build());
    }

    @Override
    protected FileUploadObj doInBackground(String... params) {
        FileUploadObj response = null;
        try {
            response = postFile(fileEntity, params[0]);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return response;
    }

    @Override
    protected void onProgressUpdate(Integer... progress) {
        super.onProgressUpdate(progress[0]);
        Log.d(TAG, "onProgressUpdate() :: progress:" + progress[0]);
        // Sets the progress indicator to a max value, the
        // current completion percentage, and "determinate"
        // state
        mBuilder.setContentText("Uploaded: " + String.valueOf(progress[0]) + "%")
                .setProgress(100, progress[0], false);
        // Displays the progress bar for the first time.
        mNotifyManager.notify(mId, mBuilder.build());
    }

    @Override
    protected void onPostExecute(FileUploadObj response) {
        super.onPostExecute(response);
        Log.d(TAG, "onPostExecute()");

        // When the loop is finished, updates the notification
        mBuilder.setContentText("Upload complete")
                .setSmallIcon(android.R.drawable.stat_sys_upload_done)
                        // Removes the progress bar
                .setProgress(0, 0, false);
        mNotifyManager.notify(mId, mBuilder.build());
        mProgressUploadListener.onFileUploaded(response, String.valueOf(mId), mName, mLength);
        fileEntity = null;
    }

    private FileUploadObj postFile(final FileEntity fileEntity, String url) throws IOException {
        URL parsedUrl = new URL(url);
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("Host", parsedUrl.getAuthority());
        map.put("ContentType", "application/binary");
        map.put("Method", "POST");
        map.put("ContentLength", String.valueOf(fileEntity.getContentLength()));
        Log.d(TAG, "postFile() :: ContentLength:" + String.valueOf(fileEntity.getContentLength()));

        HttpURLConnection connection = openConnection(parsedUrl);
        for (String headerName : map.keySet()) {
            connection.addRequestProperty(headerName, map.get(headerName));
        }
        Log.d(TAG, "setConnectionParametersForRequest() :: BEGIN");
        setConnectionParametersForRequest(connection);
        Log.d(TAG, "setConnectionParametersForRequest() :: END");
        // Initialize HttpResponse with data from the HttpURLConnection.
        Log.d(TAG, "ProtocolVersion() :: BEGIN");
        ProtocolVersion protocolVersion = new ProtocolVersion("HTTP", 1, 1);
        int responseCode = connection.getResponseCode();
        if (responseCode == -1) {
            // -1 is returned by getResponseCode() if the response code could not be retrieved.
            // Signal to the caller that something was wrong with the connection.
            throw new IOException("Could not retrieve response code from HttpUrlConnection.");
        }
        Log.d(TAG, "ProtocolVersion() :: END");
        Log.d(TAG, "BasicStatusLine() :: BEGIN");
        StatusLine responseStatus = new BasicStatusLine(protocolVersion, connection.getResponseCode(), connection.getResponseMessage());
        Log.d(TAG, "BasicStatusLine() :: END");
        Log.d(TAG, "BasicHttpResponse() :: BEGIN");
        BasicHttpResponse response = new BasicHttpResponse(responseStatus);
        response.setEntity(entityFromConnection(connection));
        for (Map.Entry<String, List<String>> header : connection.getHeaderFields().entrySet()) {
            if (header.getKey() != null) {
                Header h = new BasicHeader(header.getKey(), header.getValue().get(0));
                response.addHeader(h);
            }
        }
        Log.d(TAG, "BasicHttpResponse() :: END");
        String uploadedFile = getContent(response);
        Gson gson = new Gson();
        return gson.fromJson(uploadedFile, FileUploadObj.class);
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
        if (fileEntity != null) {
            mLength = fileEntity.getContentLength();
            connection.setDoOutput(true);
            connection.addRequestProperty(HEADER_CONTENT_TYPE, "application/binary");
            DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());
            InputStream fileInputStream = fileEntity.getContent();
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
                publishProgress((int) (progress * 100 / mLength));
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
     * Initializes an {@link HttpEntity} from the given {@link java.net.HttpURLConnection}.
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