package mil.nga.mapcache.io.network;

import android.app.Activity;
import android.util.Base64;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import mil.nga.mapcache.auth.Authenticator;
import mil.nga.mapcache.auth.UserLoggerInner;
import mil.nga.mapcache.utils.HttpUtils;

/**
 * An http get request class that performs any specified http get.
 */
public class HttpGetRequest implements Runnable, Authenticator {

    /**
     * The url of the get request.
     */
    private String urlString;

    /**
     * Object that is called when request is completed.
     */
    private IResponseHandler handler;

    /**
     * Used to get the app name and version for the user agent.
     */
    private Activity activity;

    /**
     * Retrieves the user's username and password then calls back for authentication.
     */
    private UserLoggerInner loggerInner = null;

    /**
     * The http connection.
     */
    private HttpURLConnection connection = null;

    /**
     * The authorization string from previous Http request.
     */
    private String authorization = null;

    /**
     * A cookie to set in request if need be.
     */
    private List<String> cookies = null;

    /**
     * Constructs a new HttpGetRequest.
     *
     * @param url      The url of the get request.
     * @param handler  Object this is called when request is completed.
     * @param activity Used to get the app name and version for the user agent.
     */
    public HttpGetRequest(String url, IResponseHandler handler, Activity activity) {
        this.urlString = url;
        this.handler = handler;
        this.activity = activity;
    }

    @Override
    public void run() {
        try {
            authorization = null;
            cookies = null;
            connect(new URL(urlString));

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                addBasicAuth(connection.getURL());
                responseCode = connection.getResponseCode();
            }

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                this.handler.handleResponse(null, responseCode);
            } else {
                InputStream stream = connection.getInputStream();
                String encoding = connection.getHeaderField(HttpUtils.getInstance().getContentEncodingKey());
                if (encoding != null && encoding.equals("gzip")) {
                    stream = new GZIPInputStream(stream);
                }
                this.handler.handleResponse(stream, responseCode);
                if (this.handler instanceof AuthorizationConsumer) {
                    ((AuthorizationConsumer) this.handler).setAuthorizationValue(this.authorization);
                }
            }
        } catch (IOException e) {
            this.handler.handleException(e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * Adds basic auth to the connection.
     *
     * @param url The connection to add basic auth to.
     */
    private void addBasicAuth(URL url) {
        if (loggerInner == null) {
            loggerInner = new UserLoggerInner(activity);
        }
        loggerInner.login(url, this);
    }

    /**
     * Adds the user agent to the http header.
     *
     * @param connection The connection to add the user agent to.
     */
    private void configureRequest(HttpURLConnection connection) {
        connection.addRequestProperty(
                HttpUtils.getInstance().getUserAgentKey(),
                HttpUtils.getInstance().getUserAgentValue(activity));

        // Used for debugging connection issues
        connection.addRequestProperty("Accept", "application/json, text/plain, */*");
        connection.addRequestProperty("Accept-Encoding", "gzip, deflate, br");
        connection.addRequestProperty("Accept-Language", "en-US");
        connection.addRequestProperty("Connection", "keep-alive");
        connection.addRequestProperty("Host", connection.getURL().getHost());
        connection.addRequestProperty("Origin", "null");
        connection.addRequestProperty("Sec-Fetch-Dest", "empty");
        connection.addRequestProperty("Sec-Fetch-Mode", "cors");
        connection.addRequestProperty("Sec-Fetch-Site", "cross-site");

        if (authorization != null) {
            connection.addRequestProperty(HttpUtils.getInstance().getBasicAuthKey(), authorization);
            authorization = null;
        } else if (cookies != null) {
            int index = 0;
            for (String cookie : cookies) {
                if (index == 1)
                    connection.addRequestProperty(HttpUtils.getInstance().getCookieKey(), cookie);
                index++;
            }
        }
    }

    private void connect(URL url) {
        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setInstanceFollowRedirects(false);
            configureRequest(connection);
            Log.i(HttpGetRequest.class.getSimpleName(), " ");
            Log.i(HttpGetRequest.class.getSimpleName(), " ");
            Log.i(HttpGetRequest.class.getSimpleName(), " ");
            Log.i(HttpGetRequest.class.getSimpleName(), "Connecting to " + url);
            for (Map.Entry<String, List<String>> entry : connection.getRequestProperties().entrySet()) {
                Log.i(HttpGetRequest.class.getSimpleName(), entry.getKey() + ": " + entry.getValue());
            }
            connection.connect();

            int responseCode = connection.getResponseCode();

            Log.i(HttpGetRequest.class.getSimpleName(), "Response code " + responseCode + " " + url);
            for (Map.Entry<String, List<String>> entries : connection.getHeaderFields().entrySet()) {
                Log.i(HttpGetRequest.class.getSimpleName(), entries.getKey() + ": " + entries.getValue());
            }
            checkCookie();
            while (responseCode == HttpURLConnection.HTTP_MOVED_PERM
                    || responseCode == HttpURLConnection.HTTP_MOVED_TEMP
                    || responseCode == HttpURLConnection.HTTP_SEE_OTHER) {
                String redirect = connection.getHeaderField(HttpUtils.getInstance().getLocationKey());
                if (!redirect.startsWith("http")) {
                    URL original = new URL(urlString);
                    String hostAndPort = original.getAuthority();
                    String protocol = original.getProtocol();
                    redirect = protocol + "://" + hostAndPort + redirect;
                    authorization = null;
                }
                connection.disconnect();
                url = new URL(redirect);

                if (needsAuthorization()) {
                    addBasicAuth(url);
                    break;
                } else {
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setInstanceFollowRedirects(false);
                    configureRequest(connection);
                    Log.i(HttpGetRequest.class.getSimpleName(), "Redirecting to " + url);
                    for (Map.Entry<String, List<String>> entry : connection.getRequestProperties().entrySet()) {
                        Log.i(HttpGetRequest.class.getSimpleName(), entry.getKey() + ": " + entry.getValue());
                    }
                    connection.connect();
                    responseCode = connection.getResponseCode();
                    Log.i(HttpGetRequest.class.getSimpleName(), "Response code " + responseCode + " " + url);
                    for (Map.Entry<String, List<String>> entries : connection.getHeaderFields().entrySet()) {
                        Log.i(HttpGetRequest.class.getSimpleName(), entries.getKey() + ": " + entries.getValue());
                    }
                    checkCookie();
                }
            }
        } catch (IOException e) {
            Log.e(HttpGetRequest.class.getSimpleName(), e.getMessage(), e);
        }
    }

    /**
     * Checks to see the redirect needs a username and password.
     *
     * @return True if the redirect needs a password, false otherwise.
     */
    private boolean needsAuthorization() {
        boolean needsAuthorizing = false;

        List<String> allowHeaders = connection.getHeaderFields().get(HttpUtils.getInstance().getAllowHeadersKey());
        if (allowHeaders != null) {
            for(String allowHeader : allowHeaders) {
                Log.i(HttpGetRequest.class.getSimpleName(), "Allow header: " + allowHeader);
                if("authorization".equals(allowHeader.toLowerCase(Locale.ROOT))) {
                    needsAuthorizing = true;
                    Log.i(HttpGetRequest.class.getSimpleName(), "Needs authorizing");
                    break;
                }
            }
        }

        return needsAuthorizing;
    }

    /**
     * Checks to see if the response has a cookie.
     */
    private void checkCookie() {
        List<String> cookies = connection.getHeaderFields().get(HttpUtils.getInstance().getSetCookieKey());
        if (cookies != null && !cookies.isEmpty()) {
            this.cookies = cookies;
            Log.i(HttpGetRequest.class.getSimpleName(), "Cookie found: " + cookies);
        }
    }

    @Override
    public boolean authenticate(URL url, String userName, String password) {
        boolean authorized = false;

        try {
            if (connection != null) {
                connection.disconnect();
            }

            String usernamePass = userName + ":" + password;
            authorization = "Basic " + Base64.encodeToString(usernamePass.getBytes(), Base64.NO_WRAP);
            Log.i(HttpGetRequest.class.getSimpleName(), "Authenticating to " + urlString);
            connect(url);
            int responseCode = connection.getResponseCode();
            authorized = responseCode != HttpURLConnection.HTTP_UNAUTHORIZED;
        } catch (IOException e) {
            Log.e(HttpGetRequest.class.getSimpleName(), e.getMessage(), e);
        }

        return authorized;
    }
}
