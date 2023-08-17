package com.app.tourdairy;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.KeyEvent;
import android.webkit.GeolocationPermissions;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final int FILE_CHOOSER_RESULT_CODE = 1002;
    private static final int CAMERA_CAPTURE_RESULT_CODE = 1003;
    private ActivityResultLauncher<Intent> fileChooserLauncher;
    private String cameraImageFilePath;
    private Context mContext;
    private WebView webView;
    private ProgressBar progressBar;
    private ValueCallback<Uri[]> fileUploadCallback;
    // Create separate launchers for gallery and camera intents
    private ActivityResultLauncher<Intent> galleryChooserLauncher;
    private ActivityResultLauncher<Intent> cameraChooserLauncher;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Initialize the mContext variable
        mContext = this; // Store the context for later use

        galleryChooserLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> handleGalleryChooserResult(result));

        cameraChooserLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> handleCameraChooserResult(result));




        fileChooserLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        if (fileUploadCallback != null) {
                            Uri[] resultUris = WebChromeClient.FileChooserParams.parseResult(result.getResultCode(), result.getData());
                            fileUploadCallback.onReceiveValue(resultUris);
                            fileUploadCallback = null;
                        }
                    } else {
                        if (fileUploadCallback != null) {
                            fileUploadCallback.onReceiveValue(null);
                            fileUploadCallback = null;
                        }
                    }
                });

        webView = findViewById(R.id.webView);
        progressBar = findViewById(R.id.progressBar);

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                view.loadUrl(request.getUrl().toString());
                return true;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                progressBar.setVisibility(ProgressBar.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                progressBar.setVisibility(ProgressBar.GONE);
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {
                    callback.invoke(origin, true, false);
                } else {
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            LOCATION_PERMISSION_REQUEST_CODE);
                }
            }

            @Override
            public void onPermissionRequest(PermissionRequest request) {
                for (String resource : request.getResources()) {
                    if (PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(resource) ||
                            PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(resource)) {
                        request.grant(new String[]{resource});
                    }
                }
            }

            // For handling file chooser dialog
            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                fileUploadCallback = filePathCallback;

                Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
                galleryIntent.addCategory(Intent.CATEGORY_OPENABLE);
                galleryIntent.setType("image/*");

                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                File imageFile = createImageFile();
                if (imageFile != null) {
                    cameraImageFilePath = imageFile.getAbsolutePath();
                    Uri imageUri = FileProvider.getUriForFile(
                            MainActivity.this,
                            "com.app.tourdairy.fileprovider",
                            imageFile
                    );
                    cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                }

                Intent chooserIntent = Intent.createChooser(galleryIntent, "Choose Image Source");
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{cameraIntent});

                try {
                    cameraChooserLauncher.launch(chooserIntent);
                } catch (ActivityNotFoundException e) {
                    fileUploadCallback = null;
                    return false;
                }
                return true;
            }

            private File createImageFile() {
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
                String imageFileName = "JPEG_" + timeStamp + "_";

                // Debug: Log the storage directory path
                File storageDir = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "TourDairy");
                Log.d("FileProvider", "Storage directory: " + storageDir.getAbsolutePath());

                if (!storageDir.exists()) {
                    storageDir.mkdirs(); // Create the directory if it doesn't exist
                }

                try {
                    File image = File.createTempFile(imageFileName, ".jpg", storageDir);
                    Log.d("FileProvider", "Image file path: " + image.getAbsolutePath());
                    return image;
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }

        });

        webView.loadUrl("https://tourdairy.tsngkl.in");
    }
    // Handle the gallery chooser result
    private void handleGalleryChooserResult(ActivityResult result) {
        if (fileUploadCallback != null) {
            Uri[] resultUris = WebChromeClient.FileChooserParams.parseResult(result.getResultCode(), result.getData());
            fileUploadCallback.onReceiveValue(resultUris);
            fileUploadCallback = null;
        }
    }

    private void handleCameraChooserResult(ActivityResult result) {
        if (fileUploadCallback != null) {
            if (cameraImageFilePath != null) {
                Uri imageUri = Uri.fromFile(new File(cameraImageFilePath));
                Uri[] resultUris = new Uri[]{imageUri};
                fileUploadCallback.onReceiveValue(resultUris);
                cameraImageFilePath = null;
            } else {
                fileUploadCallback.onReceiveValue(null);
            }
            fileUploadCallback = null;
        }
    }


    // Handle permissions request results
    @SuppressLint("MissingPermission")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                webView.reload();
            }
        }
    }

    // Handle file chooser result
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        Log.d("CameraResult", "requestCode: " + requestCode + ", resultCode: " + resultCode + ", intent: " + intent + ", fileUploadCallback: " + fileUploadCallback);
        Log.d("CameraResult", "FILE_CHOOSER_RESULT_CODE: " + FILE_CHOOSER_RESULT_CODE + ", CAMERA_CAPTURE_RESULT_CODE: " + CAMERA_CAPTURE_RESULT_CODE);

        if (requestCode == FILE_CHOOSER_RESULT_CODE) {
            if (fileUploadCallback != null) {
                Uri[] result = WebChromeClient.FileChooserParams.parseResult(resultCode, intent);
                Log.d("CameraResult", "result: " + Arrays.toString(result));
                fileUploadCallback.onReceiveValue(result);
                fileUploadCallback = null;
            }
        } else if (requestCode == CAMERA_CAPTURE_RESULT_CODE) {
            if (cameraImageFilePath != null) {
                Uri imageUri = Uri.fromFile(new File(cameraImageFilePath));
                Uri[] result = new Uri[]{imageUri};
                Log.d("CameraResult", "result: " + Arrays.toString(result));
                fileUploadCallback.onReceiveValue(result);
                cameraImageFilePath = null; // Reset the cameraImageFilePath after processing
            } else {
                fileUploadCallback.onReceiveValue(null);
            }
            fileUploadCallback = null;
        }
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK) && webView.canGoBack()) {
            webView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}