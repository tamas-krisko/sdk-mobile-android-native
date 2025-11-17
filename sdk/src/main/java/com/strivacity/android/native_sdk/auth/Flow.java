package com.strivacity.android.native_sdk.auth;

import android.net.Uri;

import androidx.annotation.Nullable;

import com.strivacity.android.native_sdk.auth.config.LoginParameters;
import com.strivacity.android.native_sdk.auth.config.OidcParams;
import com.strivacity.android.native_sdk.auth.config.TenantConfiguration;
import com.strivacity.android.native_sdk.util.HttpClient;

import org.json.JSONException;
import org.json.JSONObject;

import lombok.Getter;

import java.net.CookieHandler;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

public class Flow {

    private static final String ERROR_KEY = "errorKey";

    private final TenantConfiguration tenantConfiguration;
    private final CookieHandler cookieHandler;

    @Getter
    private final OidcParams oidcParams;

    private String sessionId;

    public Flow(TenantConfiguration tenantConfiguration, CookieHandler cookieHandler) {
        this.tenantConfiguration = tenantConfiguration;
        this.cookieHandler = cookieHandler;

        try {
            oidcParams = new OidcParams();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public Uri startSession(LoginParameters loginParameters) {
        HttpClient.HttpResponse response = HttpClient.followUntil(
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

    public void startWorkflowSession(String query) {
        final HttpClient.HttpResponse response = follow(tenantConfiguration.getEntryEndpoint(query));
        if (response.getResponseCode() == 400) {
            String body = response.getBody();

            try {
                JSONObject root = new JSONObject(body);

                if (!root.has(ERROR_KEY)) {
                    throw new NativeSDKError.UnknownError(new RuntimeException("Workflow error: errorKey is null"));
                }

                throw new NativeSDKError.WorkflowError(root.getString(ERROR_KEY));
            } catch (JSONException e) {
                throw new NativeSDKError.UnknownError(new RuntimeException("Workflow error: " + response.getBody()));
            }
        }

        if (!response.getHeaders().containsKey("location")) {
            throw new NativeSDKError.UnknownError(new RuntimeException("Workflow error: " + response.getBody()));
        }

        final Uri redirectUri = Uri.parse(response.getHeader("location"));
        if (!redirectUri.getQueryParameterNames().contains("session_id")) {
            throw new NativeSDKError.UnknownError(new RuntimeException("Failed to start session: session_id missing"));
        }

        sessionId = redirectUri.getQueryParameter("session_id");
    }

    public HttpClient.HttpResponse initForm() {
        return HttpClient.post(
            tenantConfiguration.getInitEndpoint(),
            cookieHandler,
            httpRequest -> httpRequest.setBearerToken(sessionId)
        );
    }

    public HttpClient.HttpResponse submitForm(String formId, String requestBody) {
        return HttpClient.post(
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
        HttpClient.HttpResponse response = HttpClient.post(
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
        return HttpClient.get(uri, cookieHandler, httpRequest -> {});
    }

    public static Session refreshToken(
        TenantConfiguration tenantConfiguration,
        CookieHandler cookieHandler,
        String refreshToken
    ) {
        HttpClient.HttpResponse response = HttpClient.post(
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

    public static void logout(TenantConfiguration tenantConfiguration, CookieHandler cookieHandler, Session session) {
        try {
            HttpClient.get(
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
