package com.galleryapp.adapters;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Checkable;
import android.widget.CursorAdapter;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;

import com.galleryapp.R;
import com.galleryapp.fragmernts.GalleryFragment;

import java.io.IOException;

/**
 * Adapter for our image files.
 */
public class ImageAdapter extends CursorAdapter {

    private final ObjectAnimator mObjectAnimator;
    private Context mContext;

    public ImageAdapter(Context context) {
        super(context, null, false);
        mContext = context;
        mObjectAnimator = new ObjectAnimator();
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        ImageView picturesView = new ImageView(context);
        picturesView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        picturesView.setPadding(5, 5, 5, 5);
        GridView.LayoutParams lp = new GridView.LayoutParams(
                (int) context.getResources().getDimension(R.dimen.grid_item),
                (int) context.getResources().getDimension(R.dimen.grid_item));
        picturesView.setLayoutParams(lp);
//        picturesView.setImageBitmap(null);
        CheckableLayout checkableLayout = new CheckableLayout(context);
        checkableLayout.setPadding(8, 8, 8, 8);
        checkableLayout.setLayoutParams(new GridView.LayoutParams(
                GridView.LayoutParams.WRAP_CONTENT,
                GridView.LayoutParams.WRAP_CONTENT));
        checkableLayout.setForegroundGravity(Gravity.CENTER);
        checkableLayout.addView(picturesView);
        return checkableLayout;
    }

    @Override
    public void bindView(final View view, Context context, final Cursor cursor) {
//        ((ImageView)view).setImageBitmap(null);
        final int imageID = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Images.Thumbnails._ID));
//        Uri imageUri = Uri.withAppendedPath(MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI, "" + imageID);
//        ((ImageView) view).setImageURI(imageUri);
        Log.d("URI", "thumbURI = " + Uri.withAppendedPath(MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI, "" + imageID).toString());
        // Set the content of the image based on the provided URI
        new AsyncTask<Integer, Void, Bitmap>() {

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                ((ImageView) ((CheckableLayout) view).getChildAt(0)).setImageBitmap(null);
            }

            @Override
            protected Bitmap doInBackground(Integer... params) {
                Uri imageUri = Uri.withAppendedPath(MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI, "" + params[0]);
                Bitmap bitmap = null;
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(mContext.getContentResolver(), imageUri);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return bitmap;
            }

            @Override
            protected void onPostExecute(Bitmap bitmap) {
                super.onPostExecute(bitmap);
                ((ImageView) ((CheckableLayout) view).getChildAt(0)).setImageBitmap(bitmap);
            }
        }.execute(imageID);
    }

    private class CheckableLayout extends FrameLayout implements Checkable {
        private boolean mChecked;

        public CheckableLayout(Context context) {
            super(context);
        }

        @SuppressWarnings("deprecation")
        public void setChecked(boolean checked) {
            mChecked = checked;
            setBackgroundColor(checked ? getResources().getColor(android.R.color.holo_blue_bright) :
                    getResources().getColor(android.R.color.transparent));
        }

        public boolean isChecked() {
            return mChecked;
        }

        public void toggle() {
            setChecked(!mChecked);
        }

    }
}