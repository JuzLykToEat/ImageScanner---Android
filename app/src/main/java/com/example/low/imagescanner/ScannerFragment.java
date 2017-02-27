package com.example.low.imagescanner;

import android.Manifest;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.inputmethodservice.Keyboard;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import static android.app.Activity.RESULT_OK;

/**
 * Created by low on 07/02/2017.
 */

public class ScannerFragment extends Fragment {

    private ImageView mMRIImageView;
    private ImageView mProcessedImageView;
    private TextView mBoldTextView;
    private TextView mNormalTextView;
    private Button mScanButton;
    private ProgressBar mLoadingProgressBar;

    private final int SELECT_PHOTO = 1;
    CountDownTimer mCountDownTimer;
    int timer = 1;
    String globalpathstorage;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_scanner, container, false);

        mMRIImageView = (ImageView)rootView.findViewById(R.id.imageView);
        mProcessedImageView = (ImageView)rootView.findViewById(R.id.imageView2);
        mBoldTextView = (TextView) rootView.findViewById(R.id.textView3);
        mNormalTextView = (TextView) rootView.findViewById(R.id.textView4);
        mScanButton = (Button) rootView.findViewById(R.id.button_scan);
        mLoadingProgressBar = (ProgressBar) rootView.findViewById(R.id.progressBar);

        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 100);
        }

        return rootView;

    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 100);
        }

        mMRIImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                photoPickerIntent.setType("image/*");
                startActivityForResult(photoPickerIntent, SELECT_PHOTO);
            }
        });

        mScanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLoadingProgressBar.setProgress (timer);
                mCountDownTimer = new CountDownTimer(4000,40) {
                    @Override
                    public void onTick(long millisUntilFinished) {
                        Log.v("Log_tag", "Tick of Progress"+ timer + millisUntilFinished);
                        timer++;
                        mLoadingProgressBar.setProgress(timer);
                    }

                    @Override
                    public void onFinish() {
                        timer = 1;
                        mLoadingProgressBar.setProgress(timer);

                        if(!globalpathstorage.equals("")) {
                            globalpathstorage = new StringBuilder(globalpathstorage).insert(globalpathstorage.length()-4, "processed").toString();
                            File processedMRIimage = new File(globalpathstorage);
                            try {
                                final Uri processedMRIimageUri = Uri.fromFile(processedMRIimage);
                                final InputStream imageStream = getActivity().getContentResolver().openInputStream(processedMRIimageUri);
                                final Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
                                mProcessedImageView.setImageBitmap(selectedImage);
                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                            }
                            String name = processedMRIimage.getName();
                            name = name.replace(".jpg", "");
                            String dir = processedMRIimage.getParent();
                            File form = new File(dir+"/ImageScanner.csv");
                            try {
                                final Uri formUri = Uri.fromFile(form);
                                final InputStream formStream = getActivity().getContentResolver().openInputStream(formUri);
                                BufferedReader reader = new BufferedReader(new InputStreamReader(formStream));
                                try {
                                    String line;
                                    while ((line = reader.readLine()) != null) {
                                        String[] RowData = line.split(",");
                                        if(name.equals(RowData[0])) {
                                            mBoldTextView.setText(RowData[1]);
                                            mNormalTextView.setText(RowData[2]);
                                            break;
                                        }
                                    }
                                }
                                catch (IOException ex) {
                                    // handle exception
                                }
                                finally {
                                    try {
                                        formStream.close();
                                    }
                                    catch (IOException e) {
                                        // handle exception
                                    }
                                }

                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                };
                mCountDownTimer.start();
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);

        switch(requestCode) {
            case SELECT_PHOTO:
                if (resultCode == RESULT_OK) {
                    try {
                        final Uri imageUri = imageReturnedIntent.getData();
                        String MRIimagepath = getPath(getActivity(),imageUri);
                        globalpathstorage = MRIimagepath;
                        final InputStream imageStream = getActivity().getContentResolver().openInputStream(imageUri);
                        final Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
                        mMRIImageView.setImageBitmap(selectedImage);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }

                }
        }
    }

    /**
     * Get a file path from a Uri. This will get the the path for Storage Access
     * Framework Documents, as well as the _data field for the MediaStore and
     * other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @author paulburke
     */
    public static String getPath(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }

                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[] {
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {

            // Return the remote address
            if (isGooglePhotosUri(uri))
                return uri.getLastPathSegment();

            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @param selection (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    public static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }

}
