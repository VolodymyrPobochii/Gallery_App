package com.galleryapp.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.galleryapp.R;
import com.galleryapp.application.GalleryApp;
import com.galleryapp.data.model.ImageObj;
import com.galleryapp.utils.AlbumStorageDirFactory;
import com.galleryapp.utils.BaseAlbumDirFactory;
import com.galleryapp.utils.FroyoAlbumDirFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;


public class PhotoIntentActivity extends Activity {

    private static final int ACTION_TAKE_PHOTO_B = 1;
    private static final int ACTION_TAKE_PHOTO_S = 2;
    private static final int ACTION_TAKE_VIDEO = 3;

    private static final String BITMAP_STORAGE_KEY = "viewbitmap";
    private static final String IMAGEVIEW_VISIBILITY_STORAGE_KEY = "imageviewvisibility";
    private ImageView mImageView;
    private Bitmap mImageBitmap;
    private static final String VIDEO_STORAGE_KEY = "viewvideo";

    private static final String VIDEOVIEW_VISIBILITY_STORAGE_KEY = "videoviewvisibility";
    private Uri mVideoUri;
    private String mCurrentPhotoPath;

    private static final String JPEG_FILE_PREFIX = "IMG_";

    private static final String JPEG_FILE_THUMB_PREFIX = "THUMB_";
    private static final String JPEG_FILE_SUFFIX = ".jpg";
    private static final String TIFF_FILE_SUFFIX = ".tiff";

    private AlbumStorageDirFactory mAlbumStorageDirFactory = null;
    private Button picRetakeBtn;
    private Button saveBtn;
    private Button cancelBtn;
    private String currentImageFileName;
    private EditText comments;
    private boolean isEdit;
    private String currentCreateDate;
    private String currentDesc;
    private String selectedPhotoItemId = null;
    private EditText title;
    private String currentImageThumbFileName;
    private String mCurrentThumbPath;
    private Bitmap mCurentThumbBitmap;


    /* Photo album for this application */
    private String getAlbumName() {
        return getString(R.string.album_name);
    }

    /* Photo thumbs album for this application */
    private String getThumbName() {
        return getString(R.string.thumbs_name);
    }

    private File getAlbumDir() {
        File storageDir = null;

        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {

            storageDir = mAlbumStorageDirFactory.getAlbumStorageDir(getAlbumName());

            if (storageDir != null) {
                if (!storageDir.mkdirs()) {
                    if (!storageDir.exists()) {
                        Log.d("CameraSample", "failed to create directory");
                        return null;
                    }
                }
            }

        } else {
            Log.v(getString(R.string.app_name), "External storage is not mounted READ/WRITE.");
        }

        return storageDir;
    }

    private File getThumbDir() {
        File thumbDir = null;

        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {

            thumbDir = mAlbumStorageDirFactory.getAlbumStorageDir(getThumbName());

            if (thumbDir != null) {
                if (!thumbDir.mkdirs()) {
                    if (!thumbDir.exists()) {
                        Log.d("CameraSample", "failed to create directory");
                        return null;
                    }
                }
            }

        } else {
            Log.v(getString(R.string.app_name), "External storage is not mounted READ/WRITE.");
        }

        return thumbDir;
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = JPEG_FILE_PREFIX + timeStamp + "_";
        File albumF = getAlbumDir();
        File imageF = File.createTempFile(imageFileName, JPEG_FILE_SUFFIX, albumF);
        currentImageFileName = imageF.getName();
        return imageF;
    }

    private File createImageThumb() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String thumbFileName = JPEG_FILE_THUMB_PREFIX + timeStamp + "_";
        File thumbAlbumF = getThumbDir();
        File thumbF = File.createTempFile(thumbFileName, JPEG_FILE_SUFFIX, thumbAlbumF);
        currentImageThumbFileName = thumbF.getName();
        return thumbF;
    }

    private File setUpPhotoFile() throws IOException {
        File f = createImageFile();
        mCurrentPhotoPath = f.getAbsolutePath();
        return f;
    }

    private File setUpThumbFile() throws IOException {
        File f = createImageThumb();
        mCurrentThumbPath = f.getAbsolutePath();
        return f;
    }

    private Bitmap setPic(boolean forThumb) {

		/* There isn't enough memory to open up more than a couple camera photos */
        /* So pre-scale the target bitmap into which the file is decoded */

		/* Get the size of the ImageView */
        int targetW = forThumb ? getResources().getInteger(R.integer.grid_thumb_wh) : mImageView.getWidth();
        int targetH = forThumb ? getResources().getInteger(R.integer.grid_thumb_wh) : mImageView.getHeight();

		/* Get the size of the image */
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

		/* Figure out which way needs to be reduced less */
        int scaleFactor = 1;
        if ((targetW > 0) || (targetH > 0)) {
            scaleFactor = Math.min(photoW / targetW, photoH / targetH);
        }

		/* Set bitmap options to scale the image decode target */
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

		/* Decode the JPEG file into a Bitmap */
        Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);

        if (!forThumb) {
        /* Associate the Bitmap to the ImageView */
            mImageView.setImageBitmap(bitmap);
            mVideoUri = null;
            mImageView.setVisibility(View.VISIBLE);
        }
        return bitmap;
    }

    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent("android.intent.action.MEDIA_SCANNER_SCAN_FILE");
        File f = new File(mCurrentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        Log.d("Image", "galleryAddPic:" + contentUri.toString());
        Log.d("MediaStore", "galleryAddPic::path = " + mCurrentPhotoPath);
        Log.d("MediaStore", "galleryAddPic::uri = " + contentUri);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }

    private void saveThumb(Bitmap finalBitmap) {
        File file = null;
        try {
            file = setUpThumbFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        assert file != null;
        if (file.exists()) file.delete();
        try {
            FileOutputStream out = new FileOutputStream(file);
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void dispatchTakePictureIntent(int actionCode) {

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        switch (actionCode) {
            case ACTION_TAKE_PHOTO_B:
                File f = null;
                try {
                    f = setUpPhotoFile();
                    mCurrentPhotoPath = f.getAbsolutePath();
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(f));
                } catch (IOException e) {
                    e.printStackTrace();
                    f = null;
                    mCurrentPhotoPath = null;
                }
                break;

            default:
                break;
        } // switch

        startActivityForResult(takePictureIntent, actionCode);
    }

    private void dispatchTakeVideoIntent() {
        Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        startActivityForResult(takeVideoIntent, ACTION_TAKE_VIDEO);
    }

    private void handleSmallCameraPhoto(Intent intent) {
        Bundle extras = intent.getExtras();
        assert extras != null;
        mImageBitmap = (Bitmap) extras.get("data");
        mImageView.setImageBitmap(mImageBitmap);
        mVideoUri = null;
        mImageView.setVisibility(View.VISIBLE);
    }

    private void handleBigCameraPhoto() {
        if (mCurrentPhotoPath != null) {
//            setPic();
            if (!isEdit) {
                currentCreateDate = new SimpleDateFormat("dd/MM/yyyy'T'HH:mm:ss").format(new Date());
//                galleryAddPic();
                saveThumb(setPic(true));
            }

            ImageObj image = new ImageObj();
            image.setCreateDate(currentCreateDate);
            image.setImageName(currentImageFileName);
            image.setImagePath(mCurrentPhotoPath);
            image.setThumbPath(mCurrentThumbPath);
            image.setImageNotes(comments.getText() != null ? comments.getText().toString() : "-");
            image.setImageTitle(title.getText() != null ? title.getText().toString() : "-");
            image.setIsSynced(0);
            image.setFileUri(null);
            image.setFileId(null);
            image.setStatus("Not synced");

            /*GalleryApp app = (GalleryApp) getApplication();
            app.saveImage(image);*/
            Log.d("Image", "Path:" + mCurrentPhotoPath);

            Intent data = new Intent();
            data.putExtra("selectedPhotoItemId", selectedPhotoItemId);
            data.putExtra("photoPath", mCurrentPhotoPath);
            data.putExtra("thumbPath", mCurrentThumbPath);
            data.putExtra("photo", currentImageFileName);
            data.putExtra("createDate", currentCreateDate);
            data.putExtra("description", comments.getText() != null ? comments.getText().toString() : "-");
            data.putExtra("title", title.getText() != null ? title.getText().toString() : "-");
            data.putExtra("image", image);
            setResult(RESULT_OK, data);
            mCurrentPhotoPath = null;
            finish();
        }

    }

    private void handleCameraVideo(Intent intent) {
        mVideoUri = intent.getData();
        mImageBitmap = null;
        mImageView.setVisibility(View.GONE);
    }

    Button.OnClickListener mTakePicOnClickListener =
            new Button.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dispatchTakePictureIntent(ACTION_TAKE_PHOTO_B);
                }
            };

    Button.OnClickListener mTakePicSOnClickListener =
            new Button.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dispatchTakePictureIntent(ACTION_TAKE_PHOTO_S);
                }
            };

    Button.OnClickListener mTakeVidOnClickListener =
            new Button.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dispatchTakeVideoIntent();
                }
            };

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.photo_activity);


        mImageView = (ImageView) findViewById(R.id.preview);
        mImageBitmap = null;
        mVideoUri = null;

        title = (EditText) findViewById(R.id.title);
        comments = (EditText) findViewById(R.id.comments);

        picRetakeBtn = (Button) findViewById(R.id.btnReIntend);
        picRetakeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePhoto();
            }
        });
        saveBtn = (Button) findViewById(R.id.btnSave);
        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleBigCameraPhoto();
            }
        });
        saveBtn.setEnabled(false);
        cancelBtn = (Button) findViewById(R.id.btnCancel);
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
            mAlbumStorageDirFactory = new FroyoAlbumDirFactory();
        } else {
            mAlbumStorageDirFactory = new BaseAlbumDirFactory();
        }

        isEdit = getIntent().getBooleanExtra("edit", false);
        if (isEdit) {
            saveBtn.setEnabled(true);
            selectedPhotoItemId = getIntent().getStringExtra("selectedPhotoItemId");
            mCurrentPhotoPath = getIntent().getStringExtra("photoPath");
            currentImageFileName = getIntent().getStringExtra("photo");
            currentCreateDate = getIntent().getStringExtra("createDate");
            currentDesc = getIntent().getStringExtra("description");
            comments.setText(currentDesc);
            setPic(false);
        } else {
            takePhoto();
        }
    }

    private void takePhoto() {
        isEdit = false;
        if (isIntentAvailable(this, MediaStore.ACTION_IMAGE_CAPTURE)) {
            dispatchTakePictureIntent(ACTION_TAKE_PHOTO_B);
        } else {
            Toast.makeText(this, "No App could perform this action!", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case ACTION_TAKE_PHOTO_B: {
                if (resultCode == RESULT_OK) {
                    saveBtn.setEnabled(true);
//                    handleBigCameraPhoto();
                    setPic(false);
                }
                break;
            } // ACTION_TAKE_PHOTO_B

            case ACTION_TAKE_PHOTO_S: {
                if (resultCode == RESULT_OK) {
                    handleSmallCameraPhoto(data);
                }
                break;
            } // ACTION_TAKE_PHOTO_S

            case ACTION_TAKE_VIDEO: {
                if (resultCode == RESULT_OK) {
                    handleCameraVideo(data);
                }
                break;
            } // ACTION_TAKE_VIDEO
        } // switch
    }

    // Some lifecycle callbacks so that the image can survive orientation change
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelable(BITMAP_STORAGE_KEY, mImageBitmap);
        outState.putParcelable(VIDEO_STORAGE_KEY, mVideoUri);
        outState.putBoolean(IMAGEVIEW_VISIBILITY_STORAGE_KEY, (mImageBitmap != null));
        outState.putBoolean(VIDEOVIEW_VISIBILITY_STORAGE_KEY, (mVideoUri != null));
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mImageBitmap = savedInstanceState.getParcelable(BITMAP_STORAGE_KEY);
        mVideoUri = savedInstanceState.getParcelable(VIDEO_STORAGE_KEY);
        mImageView.setImageBitmap(mImageBitmap);
        mImageView.setVisibility(
                savedInstanceState.getBoolean(IMAGEVIEW_VISIBILITY_STORAGE_KEY) ?
                        ImageView.VISIBLE : ImageView.GONE
        );
    }

    /**
     * Indicates whether the specified action can be used as an intent. This
     * method queries the package manager for installed packages that can
     * respond to an intent with the specified action. If no suitable package is
     * found, this method returns false.
     * http://android-developers.blogspot.com/2009/01/can-i-use-this-intent.html
     *
     * @param context The application's environment.
     * @param action  The Intent action to check for availability.
     * @return True if an Intent with the specified action can be sent and
     * responded to, false otherwise.
     */
    public static boolean isIntentAvailable(Context context, String action) {
        final PackageManager packageManager = context.getPackageManager();
        final Intent intent = new Intent(action);
        List<ResolveInfo> list =
                packageManager.queryIntentActivities(intent,
                        PackageManager.MATCH_DEFAULT_ONLY);
        return list.size() > 0;
    }

    private void setBtnListenerOrDisable(
            Button btn,
            Button.OnClickListener onClickListener,
            String intentName
    ) {
        if (isIntentAvailable(this, intentName)) {
            btn.setOnClickListener(onClickListener);
        } else {
            btn.setText(
                    getText(R.string.cannot).toString() + " " + btn.getText());
            btn.setClickable(false);
        }
    }
}