package com.galleryapp.asynctasks;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.galleryapp.Config;
import com.galleryapp.R;
import com.galleryapp.interfaces.ProgressiveEntityListener;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedReader;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

public final class CreateResourceTask extends AsyncTask<Void, Integer, String> {

    private final String responseId;
    private Context mContext;
    private FileEntity fileEntity;
    private final String fileName;
    private final String url;
    private ProgressiveEntityListener mProgressUploadListener;
    private ProgressDialog mProgressDialog;

    public CreateResourceTask(Context context, FileEntity fileEntity, String fileName, String url, String responseId) {
        this.mContext = context;
        this.fileEntity = fileEntity;
        this.fileName = fileName;
        this.url = url;
        this.responseId = responseId;
        setProgressUploadListener((ProgressiveEntityListener) context);
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        Log.d("UPLOAD", "onPreExecute()");
        mProgressDialog = new ProgressDialog(mContext);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setCancelable(false);
        mProgressDialog.setCanceledOnTouchOutside(false);
        mProgressDialog.setTitle(R.string.file_upload);
        mProgressDialog.setIcon(R.drawable.ic_launcher);
        mProgressDialog.setMax(100);
        mProgressDialog.setProgress(0);
        mProgressDialog.show();
    }

    @Override
    protected String doInBackground(Void... params) {
        String response = postFile(fileEntity, fileName, url);
        Log.d("UPLOAD", "doInBackground() :: response:" + response);
        return response;
    }

    @Override
    protected void onProgressUpdate(Integer... progress) {
        super.onProgressUpdate(progress[0]);
        Log.d("UPLOAD", "onProgressUpdate() :: progress:" + progress[0]);
        mProgressDialog.setProgress(progress[0]);
    }

    @Override
    protected void onPostExecute(String response) {
        super.onPostExecute(response);
        Log.d("UPLOAD", "onPostExecute()");

        mProgressUploadListener.onFileUpload(response, fileName, fileEntity, responseId);

        mProgressDialog.setProgress(100);
        mProgressDialog.dismiss();
        mProgressDialog = null;
        fileEntity = null;
    }

    /*fake*/
    private String postFile(final FileEntity fileEntity, String fileName, String url) {

        HttpClient client = new DefaultHttpClient();
        HttpPut put = new HttpPut(url);
        HttpEntity entity = fileEntity;
        put.addHeader("OriginalFileName", fileName);
        put.addHeader("ContentType", Config.CONTENT_TYPE_IMAGE_JPG);
        put.addHeader("ContentLength", String.valueOf(entity.getContentLength()));
        Log.d("UPLOAD", "postFile() :: ContentLength:" + String.valueOf(entity.getContentLength()));

        class ProgressiveEntity implements HttpEntity {
            private final HttpEntity entity;
            private long progress;

            ProgressiveEntity(HttpEntity entity) {
                this.entity = entity;
                Log.d("UPLOAD", "ProgressiveEntity() :: progress:" + progress);
            }

            @Override
            public void consumeContent() throws IOException {
                entity.consumeContent();
            }

            @Override
            public InputStream getContent() throws IOException,
                    IllegalStateException {
                return entity.getContent();
            }

            @Override
            public Header getContentEncoding() {
                return entity.getContentEncoding();
            }

            @Override
            public long getContentLength() {
                return entity.getContentLength();
            }

            @Override
            public Header getContentType() {
                return entity.getContentType();
            }

            @Override
            public boolean isChunked() {
                return entity.isChunked();
            }

            @Override
            public boolean isRepeatable() {
                return entity.isRepeatable();
            }

            @Override
            public boolean isStreaming() {
                return entity.isStreaming();
            } // CONSIDER put a _real_ delegator into here!

            @Override
            public void writeTo(OutputStream outstream) throws IOException {
                class ProxyOutputStream extends FilterOutputStream {
                    /**
                     * @author Stephen Colebourne
                     */
                    public ProxyOutputStream(OutputStream proxy) {
                        super(proxy);
                    }

                    public void write(int idx) throws IOException {
                        out.write(idx);
                    }

                    public void write(byte[] bts) throws IOException {
                        out.write(bts);
                    }

                    public void write(byte[] bts, int st, int end) throws IOException {
                        out.write(bts, st, end);
                    }

                    public void flush() throws IOException {
                        out.flush();
                    }

                    public void close() throws IOException {
                        out.close();
                    }
                } // CONSIDER import this class (and risk more Jar File Hell)

                class ProgressiveOutputStream extends ProxyOutputStream {
                    public ProgressiveOutputStream(OutputStream proxy) {
                        super(proxy);
                    }

                    public void write(byte[] bts, int st, int end) throws IOException {
                        // progress update
                        progress += end;
                        out.write(bts, st, end);
                        publishProgress((int) (100 * progress / getContentLength()));
                    }
                }
                entity.writeTo(new ProgressiveOutputStream(outstream));
            }
        }
        ProgressiveEntity httpEntity = new ProgressiveEntity(entity);
        put.setEntity(httpEntity);
        HttpResponse response = null;
        try {
            response = client.execute(put);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return getContent(response);

    }

    public String getContent(HttpResponse response) {
        if (response != null) {
            BufferedReader rd = null;
            try {
                rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            } catch (IOException e) {
                e.printStackTrace();
            }
            String body;
            String content = "";
            try {
                while ((body = rd.readLine()) != null) {
                    content += body + "\n";
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return content.trim();
        } else {
            return "Response NULL";
        }
    }

    public void setProgressUploadListener(ProgressiveEntityListener mProgressUploadListener) {
        this.mProgressUploadListener = mProgressUploadListener;
    }
}