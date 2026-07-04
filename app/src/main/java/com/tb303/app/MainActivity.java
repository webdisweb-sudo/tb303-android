package com.tb303.app;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.view.Window;
import android.view.WindowManager;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.BufferedWriter;

public class MainActivity extends Activity {

    private WebView webView;
    private static final String PATTERNS_DIR = "TB303";

    // JavaScript Interface — called from JS via window.Android.*
    public class TB303Bridge {

        // Save pattern JSON to Downloads/TB303/<name>.tb303
        @JavascriptInterface
        public void savePattern(String name, String json) {
            try {
                File dir = new File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    PATTERNS_DIR
                );
                if (!dir.exists()) dir.mkdirs();
                String filename = name.replaceAll("[^a-zA-Z0-9_\\-]", "_") + ".tb303";
                File file = new File(dir, filename);
                BufferedWriter bw = new BufferedWriter(new FileWriter(file));
                bw.write(json);
                bw.close();
                final String msg = "Сохранено: Downloads/TB303/" + filename;
                runOnUiThread(() -> Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show());
                // Notify JS
                final String jsCall = "onSaveSuccess('" + filename + "')";
                webView.post(() -> webView.evaluateJavascript(jsCall, null));
            } catch (Exception e) {
                final String err = e.getMessage();
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Ошибка: " + err, Toast.LENGTH_LONG).show());
            }
        }

        // Get list of pattern files in Downloads/TB303/
        @JavascriptInterface
        public String listPatterns() {
            try {
                File dir = new File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    PATTERNS_DIR
                );
                if (!dir.exists()) return "[]";
                File[] files = dir.listFiles((d, n) -> n.endsWith(".tb303") || n.endsWith(".json"));
                if (files == null || files.length == 0) return "[]";
                StringBuilder sb = new StringBuilder("[");
                for (int i = 0; i < files.length; i++) {
                    sb.append("\"").append(files[i].getName()).append("\"");
                    if (i < files.length - 1) sb.append(",");
                }
                sb.append("]");
                return sb.toString();
            } catch (Exception e) {
                return "[]";
            }
        }

        // Read pattern file content from Downloads/TB303/<filename>
        @JavascriptInterface
        public String readPattern(String filename) {
            try {
                File dir = new File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    PATTERNS_DIR
                );
                File file = new File(dir, filename);
                if (!file.exists()) return "";
                BufferedReader br = new BufferedReader(new FileReader(file));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();
                return sb.toString();
            } catch (Exception e) {
                return "";
            }
        }

        // Delete pattern file
        @JavascriptInterface
        public boolean deletePattern(String filename) {
            try {
                File dir = new File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    PATTERNS_DIR
                );
                File file = new File(dir, filename);
                return file.delete();
            } catch (Exception e) {
                return false;
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        webView = new WebView(this);
        setContentView(webView);

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setAllowFileAccessFromFileURLs(true);
        s.setAllowUniversalAccessFromFileURLs(true);
        s.setDomStorageEnabled(true);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);

        // Register JavaScript interface
        webView.addJavascriptInterface(new TB303Bridge(), "Android");

        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient());

        webView.setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN |
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );

        webView.loadUrl("file:///android_asset/tb303.html");
    }

    @Override
    protected void onResume() {
        super.onResume();
        webView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        webView.onPause();
    }

    @Override
    public void onBackPressed() {}
}
