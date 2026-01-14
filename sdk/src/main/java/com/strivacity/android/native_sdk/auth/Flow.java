package com.strivacity.android.native_sdk.auth;

import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.strivacity.android.native_sdk.auth.config.LoginParameters;
import com.strivacity.android.native_sdk.auth.config.OidcParams;
import com.strivacity.android.native_sdk.auth.config.TenantConfiguration;
import com.strivacity.android.native_sdk.util.HttpClient;
import com.strivacity.android.native_sdk.util.Logging;

import org.json.JSONException;
import org.json.JSONObject;

import lombok.Getter;

import java.net.CookieHandler;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

public class Flow {

    /**
     * JSON Key name for error mnemonic in response object
     */
    private static final String ERROR_KEY = "error";
    /**
     * JSON Key name for error description in response object
     */
    private static final String ERROR_DESCRIPTION_KEY = "error_description";

    private static final int STATUS_CODE_BAD_REQUEST = 400;
    private static final int STATUS_CODE_INTERNAL_SERVER_ERROR = 500;

    private static final String TAG = "Flow";

    private final TenantConfiguration tenantConfiguration;
    private final CookieHandler cookieHandler;

    @Getter
    private final OidcParams oidcParams;

    private String sessionId;

    @NonNull
    private final Logging logging;

    @NonNull
    private final HttpClient httpClient;

    public Flow(
        TenantConfiguration tenantConfiguration,
        CookieHandler cookieHandler,
        @NonNull Logging logging,
        @NonNull HttpClient httpClient
    ) {
        this.tenantConfiguration = tenantConfiguration;
        this.cookieHandler = cookieHandler;
        this.logging = logging;
        this.httpClient = httpClient;

        try {
            oidcParams = new OidcParams();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public Uri startSession(LoginParameters loginParameters) {
        logging.info("Login flow started");
        HttpClient.HttpResponse response = httpClient.followUntil(
            tenantConfiguration.getAuthEndpoint(oidcParams, loginParameters),
            cookieHandler,
            httpResponse -> {
                if (!httpResponse.getHeaders().containsKey("location")) {
                    return true;
                }

                Uri redirectUri = Uri.parse(httpResponse.getHeader("location"));
                return (
                    tenantConfiguration.getRedirectURI().getHost().equals(redirectUri.getHost()) ||
                    (
                        tenantConfiguration.getIssuer().getHost().equals(redirectUri.getHost()) &&
                        "oauth2/error".equals(redirectUri.getPath())
                    )
                );
            }
        );

        if (!response.getHeaders().containsKey("location")) {
            logging.warn("Expected to find location but none were found");
            throw new NativeSDKError.OIDCError("OIDC Error", response.getBody());
        }

        Uri redirectUri = Uri.parse(response.getHeader("location"));
        if (redirectUri.getQueryParameterNames().contains("error")) {
            throw new NativeSDKError.OIDCError(
                redirectUri.getQueryParameter("error"),
                redirectUri.getQueryParameter("error_description")
            );
        }

        if (redirectUri.getQueryParameterNames().contains("code")) {
            return redirectUri;
        }

        if (!redirectUri.getQueryParameterNames().contains("session_id")) {
            throw new NativeSDKError.OIDCError("Failed to start session", "session_id missing");
        }

        sessionId = redirectUri.getQueryParameter("session_id");
        return null;
    }

    public void startWorkflowSession(@NonNull String query) {
        Objects.requireNonNull(query, "query parameter cannot be null");
        final HttpClient.HttpResponse response = follow(tenantConfiguration.getEntryEndpoint(query));
        // validate response and build Error if needed
        validateEntryResponse(response);
        sessionId = extractRequiredSessionId(response);
    }

    private void validateEntryResponse(@NonNull HttpClient.HttpResponse response) {
        final int statusCode = response.getResponseCode();

        if (statusCode == STATUS_CODE_BAD_REQUEST) {
            String body = response.getBody();

            try {
                JSONObject decodedResponse = new JSONObject(body);

                if (!decodedResponse.has(ERROR_KEY)) {
                    final RuntimeException innerException = new RuntimeException(
                        String.format("Workflow error: %s is null", ERROR_KEY)
                    );
                    throw new NativeSDKError.UnknownError(innerException);
                }

                final String error = decodedResponse.getString(ERROR_KEY);
                final String errorDescription = decodedResponse.optString(ERROR_DESCRIPTION_KEY);
                throw new NativeSDKError.WorkflowError(error, errorDescription);
            } catch (JSONException e) {
                final String message = String.format(
                    "Workflow error - could not deserialize response: %s",
                    e.getMessage()
                );
                throw new NativeSDKError.UnknownError(new RuntimeException(message));
            }
        }

        if (statusCode == STATUS_CODE_INTERNAL_SERVER_ERROR) {
            Log.d(TAG, "Ensure that authentication client has entry URL configured.");
            throw new NativeSDKError.UnknownError(
                new RuntimeException("Server failed to answer - 500 status code received")
            );
        }

        if (!response.getHeaders().containsKey("location")) {
            throw new NativeSDKError.UnknownError(
                new RuntimeException("Expected to find Location header but it was not found ")
            );
        }
    }

    @NonNull
    private String extractRequiredSessionId(@NonNull HttpClient.HttpResponse response) {
        final Uri redirectUri = Uri.parse(response.getHeader("location"));
        final String sessionId = redirectUri.getQueryParameter("session_id");
        if (sessionId == null || sessionId.isBlank()) {
            throw new NativeSDKError.UnknownError(
                new RuntimeException("Failed to start session: session_id missing or blank")
            );
        }
        return sessionId;
    }

    public HttpClient.HttpResponse initForm() {
        return httpClient.post(
            tenantConfiguration.getInitEndpoint(),
            cookieHandler,
            httpRequest -> httpRequest.setBearerToken(sessionId)
        );
    }

    public HttpClient.HttpResponse submitForm(String formId, String requestBody) {
        return httpClient.post(
            tenantConfiguration.getFormEndpoint(formId),
            cookieHandler,
            httpRequest -> {
                httpRequest.setContentType("application/json");
                httpRequest.setBearerToken(sessionId);
                httpRequest.setBody(requestBody);
            }
        );
    }

    public Session tokenExchange(String codeToken) {
        HttpClient.HttpResponse response = httpClient.post(
            tenantConfiguration.getTokenEndpoint(),
            cookieHandler,
            httpRequest -> {
                httpRequest.setContentType("application/x-www-form-urlencoded");
                httpRequest.setFollowRedirects(false);
                httpRequest.setBody(
                    tenantConfiguration
                        .getIssuer()
                        .buildUpon()
                        .appendQueryParameter("grant_type", "authorization_code")
                        .appendQueryParameter("client_id", tenantConfiguration.getClientId())
                        .appendQueryParameter("code_verifier", oidcParams.getCodeVerifier())
                        .appendQueryParameter("code", codeToken)
                        .appendQueryParameter("redirect_uri", tenantConfiguration.getRedirectURI().toString())
                        .build()
                        .getQuery()
                );
            }
        );

        if (response.getResponseCode() != 200) {
            JSONObject json;
            try {
                json = new JSONObject(response.getBody());
                throw new RuntimeException(json.getString("error_description"));
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }

        return parseTokens(response, oidcParams);
    }

    public HttpClient.HttpResponse follow(Uri uri) {
        return httpClient.get(uri, cookieHandler, httpRequest -> {});
    }

    public static Session refreshToken(
        TenantConfiguration tenantConfiguration,
        CookieHandler cookieHandler,
        String refreshToken,
        @NonNull HttpClient httpClient
    ) {
        HttpClient.HttpResponse response = httpClient.post(
            tenantConfiguration.getTokenEndpoint(),
            cookieHandler,
            httpRequest -> {
                httpRequest.setContentType("application/x-www-form-urlencoded");
                httpRequest.setFollowRedirects(false);
                httpRequest.setBody(
                    tenantConfiguration
                        .getIssuer()
                        .buildUpon()
                        .appendQueryParameter("grant_type", "refresh_token")
                        .appendQueryParameter("refresh_token", refreshToken)
                        .appendQueryParameter("client_id", tenantConfiguration.getClientId())
                        .appendQueryParameter("redirect_uri", tenantConfiguration.getRedirectURI().toString())
                        .build()
                        .getQuery()
                );
            }
        );

        if (response.getResponseCode() != 200) {
            throw new RuntimeException();
        }

        return parseTokens(response, null);
    }

    private static Session parseTokens(HttpClient.HttpResponse response, @Nullable OidcParams oidcParams) {
        try {
            JSONObject body = new JSONObject(response.getBody());

            Session session = new Session();
            session.setAccessToken(body.getString("access_token"));
            session.setExpiration(Instant.now().plus(body.getInt("expires_in"), ChronoUnit.SECONDS));

            if (!body.isNull("id_token")) {
                session.setIdToken(body.getString("id_token"));

                if (
                    oidcParams != null && !Objects.equals(oidcParams.getNonce(), session.getIdTokenClaims().getNonce())
                ) {
                    throw new RuntimeException("Nonce mismatch");
                }
            }

            if (!body.isNull("refresh_token")) {
                session.setRefreshToken(body.getString("refresh_token"));
            }

            return session;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public static void logout(
        TenantConfiguration tenantConfiguration,
        CookieHandler cookieHandler,
        Session session,
        @NonNull HttpClient httpClient
    ) {
        try {
            httpClient.get(
                tenantConfiguration
                    .getLogoutEndpoint()
                    .buildUpon()
                    .appendQueryParameter("id_token_hint", session.getIdToken())
                    .appendQueryParameter("post_logout_redirect_uri", tenantConfiguration.getPostLogoutURI().toString())
                    .build(),
                cookieHandler,
                httpRequest -> {}
            );
        } catch (Exception ignored) {}
    }
}
