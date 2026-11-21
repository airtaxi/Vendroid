package dev.vendicated.vencord;

import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VendroidNative {
    private static final ExecutorService HTTP_EXECUTOR = Executors.newCachedThreadPool();
    private static final int HTTP_CONNECT_TIMEOUT_MS = 30_000;
    private static final int HTTP_READ_TIMEOUT_MS = 120_000;

    private final WebView wv;
    private final MainActivity activity;

    public VendroidNative(MainActivity activity, WebView wv) {
        this.activity = activity;
        this.wv = wv;
    }

    @JavascriptInterface
    public void goBack() {
        activity.runOnUiThread(() -> {
            if (wv.canGoBack())
                wv.goBack();
            else
                // no idea what i was smoking when I wrote this
                activity.getActionBar();
        });
    }

    @JavascriptInterface
    public void openVendroidSettings() {
        activity.runOnUiThread(activity::openVendroidSettings);
    }

    @JavascriptInterface
    public void openVencordUpdater() {
        activity.runOnUiThread(activity::openUpdaterDialog);
    }

    @JavascriptInterface
    public String getVencordCss() {
        return HttpClient.VencordCss == null ? "" : HttpClient.VencordCss;
    }

    /**
     * Sends an https request from the app instead of the WebView, so the response is not subject to CORS.
     * The result is passed to VencordMobile.onNativeHttpResponse(requestId, { status, data }),
     * where status is -1 if the request could not be completed.
     */
    @JavascriptInterface
    public void httpRequest(String requestId, String method, String url, String headersJson, String body) {
        HTTP_EXECUTOR.execute(() -> {
            String response;
            try {
                response = sendHttpRequest(method, url, headersJson, body);
            } catch (Exception ex) {
                Logger.e("Native http request failed", ex);
                response = httpResponseJson(-1, ex.toString());
            }

            var script = "VencordMobile.onNativeHttpResponse(" + JSONObject.quote(requestId) + "," + response + ")";
            wv.post(() -> wv.evaluateJavascript(script, null));
        });
    }

    private static String sendHttpRequest(String method, String url, String headersJson, String body) throws IOException, JSONException {
        var target = new URL(url);
        if (!"https".equals(target.getProtocol()))
            throw new IOException("Only https URLs are allowed");

        var conn = (HttpURLConnection) target.openConnection();
        try {
            conn.setConnectTimeout(HTTP_CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(HTTP_READ_TIMEOUT_MS);
            conn.setRequestMethod(method);
            // Some API servers reject Android's default Dalvik user agent. Headers from the page override this.
            conn.setRequestProperty("User-Agent", Constants.USER_AGENT);

            var headers = new JSONObject(headersJson);
            for (var keys = headers.keys(); keys.hasNext(); ) {
                var key = keys.next();
                conn.setRequestProperty(key, headers.getString(key));
            }

            if (body != null && !body.isEmpty()) {
                conn.setDoOutput(true);
                try (var os = conn.getOutputStream()) {
                    os.write(body.getBytes(StandardCharsets.UTF_8));
                }
            }

            var status = conn.getResponseCode();
            try (var is = status >= 400 ? conn.getErrorStream() : conn.getInputStream()) {
                return httpResponseJson(status, HttpClient.readAsText(is));
            }
        } finally {
            conn.disconnect();
        }
    }

    private static String httpResponseJson(int status, String data) {
        return "{\"status\":" + status + ",\"data\":" + JSONObject.quote(data) + "}";
    }
}
