package com.benlulud.melophony.webapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.LinearLayout;
import android.widget.Toast;

import 	android.view.LayoutInflater;

import com.benlulud.melophony.api.client.ApiClient;
import com.benlulud.melophony.api.client.Configuration;
import com.benlulud.melophony.database.Database;
import com.benlulud.melophony.server.Router;


public class MainActivity extends Activity {
    private static final String TAG = "MelophonyMainActivity";
    private static final String MELOPHONY_MAIN_URL = "http://localhost:1804";

    private Context context;
    private ApiClient apiClient;
    private Database db;
    private Router router;
    private ViewGroup activityView;
    private WebView webView;
    private boolean isWebViewAttachedToWindow;

    private ValueCallback<Uri> mUploadMessage;
    public ValueCallback<Uri[]> uploadMessage;
    public static final int REQUEST_SELECT_FILE = 100;
    private final static int FILECHOOSER_RESULTCODE = 1;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.context = this.getApplicationContext();
        Log.i(TAG, "onCreate");

        try {
            this.apiClient = new ApiClient();
            this.apiClient.setVerifyingSsl(false);
            this.apiClient.setBasePath("https://melophony.ddns.net:1804/api");
            Configuration.setDefaultApiClient(apiClient);

            this.db = Database.getDatabase(this);
            this.apiClient.addDefaultHeader(Constants.AUTHORIZATION_HEADER, db.getPersistedData(Constants.TOKEN_KEY));

            router = new Router(this);

            setContentView(R.layout.main_activity);
            requestDisableDoze();
            requestEnableDrawOverApps();
            startMediaManagerService();

            webView = (WebView) findViewById(R.id.webView);
            activityView = (ViewGroup) webView.getParent();
            webView.setWebViewClient(new AppWebViewClient());
            loadMelophonyWebApp();
            bindMediaManagerService();
        } catch (Exception e) {
            Log.e(TAG, "Unable to start local server: ", e);
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        );

        params.gravity = Gravity.TOP | Gravity.LEFT;
        params.x = 0;
        params.y = 0;
        params.width = 0;
        params.height = 0;

        activityView.removeView(webView);
        windowManager.addView(webView, params);
        isWebViewAttachedToWindow = true;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isWebViewAttachedToWindow) {
            WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
            windowManager.removeViewImmediate(webView);
            activityView.addView(webView);
            webView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.FILL_PARENT));
        }
        isWebViewAttachedToWindow = false;
    }

    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        webView.saveState(bundle);
    }

    private void requestDisableDoze() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            final Intent intent = new Intent();
            final String packageName = getPackageName();
            final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);

            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + packageName));
                startActivity(intent);
            }
        }
    }

    private void requestEnableDrawOverApps() {
        if (!Settings.canDrawOverlays(this)) {
            new AlertDialog.Builder(this)
                .setTitle("Draw-Over-Apps permission required")
                .setMessage("Melophony requires this setting to keep playing tracks while the phone is locked")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                        intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                        startActivityForResult(intent, 0);
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // Nothing more to do
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
        }
    }

    private void loadMelophonyWebApp() {
        final WebSettings settings = webView.getSettings();

        webView.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
        settings.setJavaScriptEnabled(true);
        settings.setSupportZoom(false);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setDatabaseEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadsImagesAutomatically(true);
        webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        webView.addJavascriptInterface(new JavaScriptInterface(this), "Android");

        webView.setWebChromeClient(new WebChromeClient() {
            // For 3.0+ Devices (Start)
            // onActivityResult attached before constructor
            protected void openFileChooser(ValueCallback uploadMsg, String acceptType) {
                mUploadMessage = uploadMsg;
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("audio/*");
                startActivityForResult(Intent.createChooser(i, "File Browser"), FILECHOOSER_RESULTCODE);
            }

            public boolean onShowFileChooser(WebView mWebView, ValueCallback<Uri[]> filePathCallback, WebChromeClient.FileChooserParams fileChooserParams) {
                if (uploadMessage != null) {
                    uploadMessage.onReceiveValue(null);
                    uploadMessage = null;
                }

                uploadMessage = filePathCallback;

                Intent intent = fileChooserParams.createIntent();
                try {
                    startActivityForResult(intent, REQUEST_SELECT_FILE);
                } catch (ActivityNotFoundException e) {
                    uploadMessage = null;
                    Toast.makeText(context, "Cannot Open File Chooser", Toast.LENGTH_LONG).show();
                    return false;
                }
                return true;
            }

            protected void openFileChooser(ValueCallback<Uri> uploadMsg) {
                mUploadMessage = uploadMsg;
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("audio/*");
                startActivityForResult(Intent.createChooser(i, "File Chooser"), FILECHOOSER_RESULTCODE);
            }

            @Override
            public boolean onConsoleMessage(ConsoleMessage cm) {
                Log.i(TAG, "Js (" + cm.lineNumber() + "): " + cm.message());
                return true;
            }
        });
        webView.loadUrl(MELOPHONY_MAIN_URL);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (requestCode == REQUEST_SELECT_FILE) {
                if (uploadMessage == null)
                    return;
                uploadMessage.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, intent));
                uploadMessage = null;
            }
        } else if (requestCode == FILECHOOSER_RESULTCODE) {
            if (null == mUploadMessage) {
                return;
            }
            Uri result = intent == null || resultCode != RESULT_OK ? null : intent.getData();
            mUploadMessage.onReceiveValue(result);
            mUploadMessage = null;
        } else {
            Toast.makeText(context, "Failed to load audio file", Toast.LENGTH_LONG).show();
        }
    }

    public static class JavaScriptInterface {
        private Context context;

        JavaScriptInterface(final Context context) {
            this.context = context;
        }

        @JavascriptInterface
        public boolean isAndroidWebApp() {
            return true;
        }
    }
}