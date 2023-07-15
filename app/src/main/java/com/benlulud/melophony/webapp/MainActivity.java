package com.benlulud.melophony.webapp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.benlulud.melophony.api.client.ApiClient;
import com.benlulud.melophony.api.client.Configuration;
import com.benlulud.melophony.database.Database;
import com.benlulud.melophony.server.Router;


public class MainActivity extends Activity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String MELOPHONY_MAIN_URL = "https://localhost:1804";

    private ApiClient apiClient;
    private Database db;
    private Router router;
    private WebView webView;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate");

        try {
            this.apiClient = new ApiClient();
            this.apiClient.setBasePath("https://melophony.ddns.net:1804");
            Configuration.setDefaultApiClient(apiClient);

            this.db = Database.getDatabase(this);
            this.apiClient.addDefaultHeader(Constants.AUTHORIZATION_HEADER, db.getPersistedData(Constants.TOKEN_KEY));

            router = new Router(this);

            setContentView(R.layout.main_activity);
            requestDisableDoze();

            webView = (WebView) findViewById(R.id.webView);
            webView.setWebViewClient(new AppWebViewClient());
            loadMelophonyWebApp();
        } catch (Exception e) {
            Log.e(TAG, "Unable to start local server: ", e);
        }
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

    private void loadMelophonyWebApp() {
        final WebSettings settings = webView.getSettings();

        webView.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
        settings.setJavaScriptEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadsImagesAutomatically(true);
        webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        webView.addJavascriptInterface(new JavaScriptInterface(this), "Android");

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(ConsoleMessage cm) {
                Log.i(TAG, "Js (" + cm.lineNumber() + "): " + cm.message());
                return true;
            }
        });

        webView.loadUrl(MELOPHONY_MAIN_URL);
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