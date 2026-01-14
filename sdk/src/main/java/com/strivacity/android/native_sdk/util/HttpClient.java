package com.strivacity.android.native_sdk.util;

import android.net.Uri;

import androidx.annotation.NonNull;

import lombok.Data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.CookieHandler;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.function.Function;

public class HttpClient {

    @NonNull
    private final Logging logging;

    public HttpClient(@NonNull Logging logging) {
        this.logging = logging;
    }

    public HttpResponse get(Uri uri, CookieHandler cookieHandler, Consumer<HttpRequest> httpCustomizer) {
        return send("GET", uri, cookieHandler, httpCustomizer);
    }

    public HttpResponse post(Uri uri, CookieHandler cookieHandler, Consumer<HttpRequest> httpCustomizer) {
        return send("POST", uri, cookieHandler, httpCustomizer);
    }

    public HttpResponse followUntil(Uri uri, CookieHandler cookieHandler, Function<HttpResponse, Boolean> predicate) {
        CookieHandler defaultHandler = CookieHandler.getDefault();

        try {
            CookieHandler.setDefault(cookieHandler);
            int redirectionCounter = 0;
            HttpResponse response;

            do {
                response = get(uri, cookieHandler, httpRequest -> httpRequest.setFollowRedirects(false));
                if (predicate.apply(response)) {
                    return response;
                }

                if (response.getResponseCode() != 301 && response.getResponseCode() != 302) {
                    throw new NoSuchElementException();
                }

                uri = Uri.parse(response.getHeader("location"));
            } while (redirectionCounter++ < 10);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            CookieHandler.setDefault(defaultHandler);
        }

        throw new NoSuchElementException();
    }

    private HttpResponse send(
        String method,
        Uri uri,
        CookieHandler cookieHandler,
        Consumer<HttpRequest> httpCustomizer
    ) {
        logging.debug(String.format("HTTP REQ [%s] %s", method, uri.getPath()));
        CookieHandler defaultHandler = CookieHandler.getDefault();

        try {
            CookieHandler.setDefault(cookieHandler);
            URL url = new URL(uri.toString());
            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
            HttpRequest httpRequest = new HttpRequest(httpURLConnection, method) {
                {
                    setFollowRedirects(false);
                }
            };
            httpCustomizer.accept(httpRequest);
            final HttpResponse response = httpRequest.connect();
            final int statusCode = response.getResponseCode();
            if (statusCode == 301 || statusCode == 302) {
                final Uri locationUri = Uri.parse(response.getHeader("Location"));
                final String scheme = locationUri.getScheme();
                if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) {
                    logging.debug(
                        String.format(
                            "HTTP RES [%d] %s Redirecting -> %s",
                            response.getResponseCode(),
                            uri.getPath(),
                            locationUri.getPath()
                        )
                    );
                } else {
                    logging.debug(
                        String.format(
                            "HTTP RES [%d] %s Redirecting -> %s://%s%s",
                            response.getResponseCode(),
                            uri.getPath(),
                            scheme,
                            locationUri.getAuthority(),
                            locationUri.getPath()
                        )
                    );
                }
            } else {
                logging.debug(String.format("HTTP RES [%d] %s", response.getResponseCode(), uri.getPath()));
            }
            return response;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            CookieHandler.setDefault(defaultHandler);
        }
    }

    @Data
    public static class HttpResponse {

        private String body;
        private int responseCode;
        private Map<String, List<String>> headers;
        private Uri url;

        public String getHeader(String header) {
            if (!headers.containsKey(header) || headers.get(header) == null || headers.get(header).isEmpty()) {
                return null;
            }

            return getHeaders().get(header).get(0);
        }
    }

    public static class HttpRequest {

        private final HttpURLConnection httpURLConnection;

        public HttpRequest(HttpURLConnection httpURLConnection, String method) throws ProtocolException {
            this.httpURLConnection = httpURLConnection;
            httpURLConnection.setRequestMethod(method);
        }

        public void setContentType(String contentType) {
            httpURLConnection.setRequestProperty("Content-Type", contentType);
        }

        public void setFollowRedirects(boolean followRedirects) {
            httpURLConnection.setInstanceFollowRedirects(followRedirects);
        }

        public void setBody(String body) {
            try {
                httpURLConnection.setDoOutput(true);
                OutputStream outputStream = httpURLConnection.getOutputStream();
                OutputStreamWriter outStreamWriter = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
                outStreamWriter.write(body);
                outStreamWriter.flush();
                outStreamWriter.close();
                outputStream.close();
            } catch (Exception e) {
                throw new RuntimeException();
            }
        }

        public void setBearerToken(String bearerToken) {
            httpURLConnection.setRequestProperty("Authorization", "Bearer " + bearerToken);
        }

        public HttpResponse connect() throws IOException {
            HttpResponse response = new HttpResponse();
            response.setHeaders(httpURLConnection.getHeaderFields());
            response.setResponseCode(httpURLConnection.getResponseCode());
            response.setUrl(Uri.parse(httpURLConnection.getURL().toString()));

            InputStream responseStream = null;
            try {
                responseStream = httpURLConnection.getInputStream();
            } catch (Exception e) {
                try {
                    responseStream = httpURLConnection.getErrorStream();
                } catch (Exception ignored) {}
            }

            if (responseStream != null) {
                StringBuilder sb = new StringBuilder();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(responseStream));
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    sb.append(line);
                }
                response.setBody(sb.toString());
            }

            return response;
        }
    }
}
