package com.app.tourdairy;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.webkit.CookieManager;
import android.webkit.GeolocationPermissions;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private WebView webview;
    private ProgressBar progressBar;
    private ProgressDialog pd;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;

    private ActivityResultLauncher<String> fileChooserLauncher;
    private ValueCallback<Uri[]> mFilePathCallback;
    private String mCameraPhotoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webview = findViewById(R.id.webView);
        progressBar = findViewById(R.id.progressBar);

        WebSettings webSettings = webview.getSettings();
        webSettings.setJavaScriptEnabled(true);

        webview.setWebViewClient(new WebViewClient());
        webview.getSettings().setDomStorageEnabled(true);
        webview.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        webview.getSettings().setDatabaseEnabled(true);
        webview.setScrollBarStyle(WebView.SCROLLBARS_INSIDE_OVERLAY);
        webview.getSettings().setEnableSmoothTransition(true);
        webview.getSettings().setSaveFormData(true);
        webview.getSettings().setSavePassword(true);
        webview.getSettings().setUseWideViewPort(true);
        webview.getSettings().setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NARROW_COLUMNS);

        webview.loadUrl("https://tourdairy.tsngkl.in/");

        fileChooserLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (mFilePathCallback != null) {
                        Uri[] results = (uri != null) ? new Uri[]{uri} : null;
                        mFilePathCallback.onReceiveValue(results);
                        mFilePathCallback = null;
                    }
                });

        webview.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(
                    WebView webView, ValueCallback<Uri[]> filePathCallback,
                    WebChromeClient.FileChooserParams fileChooserParams){
                    mFilePathCallback = filePathCallback;
                    Intent contentSelectionIntent = fileChooserParams.createIntent();
                    fileChooserLauncher.launch(String.valueOf(contentSelectionIntent));
                    return true;
                }

            // Handle other WebChromeClient methods...

        });

        // External storage permission for saving file
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
                requestPermissions(permissions, 1);
            }
        }

        // Handle downloading
        webview.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setMimeType(mimeType);
            String cookies = CookieManager.getInstance().getCookie(url);
            request.addRequestHeader("cookie", cookies);
            request.addRequestHeader("User-Agent", userAgent);
            request.setDescription("Downloading file....");
            request.setTitle(URLUtil.guessFileName(url, contentDisposition, mimeType));
            request.allowScanningByMediaScanner();
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, URLUtil.guessFileName(url, contentDisposition, mimeType));
            DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            dm.enqueue(request);
            Toast.makeText(getApplicationContext(), "Downloading File", Toast.LENGTH_SHORT).show();
        });

        pd = new ProgressDialog(MainActivity.this);
        pd.setMessage("Loading Please Wait....");
        webview.setWebViewClient(new MyWebViewClient(pd));
        pd.setCanceledOnTouchOutside(false);

        // Check and request location permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        LOCATION_PERMISSION_REQUEST_CODE);
            } else {
                // Location permissions are already granted, enable location features
                enableLocationFeatures();
            }
        } else {
            // For devices below Android M, no need to request permission, enable location features
            enableLocationFeatures();
        }
    }

    private void enableLocationFeatures() {
        // Enable geolocation support in WebView
        webview.getSettings().setGeolocationEnabled(true);

        // Set up a WebChromeClient to handle geolocation permissions requests
        webview.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                // Check if the app has location permissions
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {
                    // Grant permission for geolocation
                    callback.invoke(origin, true, false);
                } else {
                    // Request location permissions
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            LOCATION_PERMISSION_REQUEST_CODE);
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Location permissions granted, enable location features
                enableLocationFeatures();
            } else {
                // Location permissions denied, handle accordingly (e.g., show a message)
                // You can decide whether to proceed without location features in this case
            }
        }
    }

    public class MyWebViewClient extends WebViewClient {
        ProgressDialog pd;

        public MyWebViewClient(ProgressDialog pd) {
            this.pd = pd;
            pd.show();
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            if (pd.isShowing()) {
                pd.dismiss();
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (webview.canGoBack()) {
            webview.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
