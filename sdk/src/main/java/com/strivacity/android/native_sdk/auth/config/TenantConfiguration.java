package com.strivacity.android.native_sdk.auth.config;

import android.net.Uri;

import lombok.Data;

@Data
public class TenantConfiguration {

    private final Uri issuer;
    private final String clientId;
    private final Uri redirectURI;
    private final Uri postLogoutURI;

    public Uri getAuthEndpoint(OidcParams oidcParams, LoginParameters loginParameters) {
        Uri.Builder builder = issuer
            .buildUpon()
            .path("/oauth2/auth")
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("client_id", clientId)
            .appendQueryParameter("redirect_uri", redirectURI.toString())
            .appendQueryParameter("state", oidcParams.getState())
            .appendQueryParameter("nonce", oidcParams.getNonce())
            .appendQueryParameter("code_challenge", oidcParams.getCodeChallenge())
            .appendQueryParameter("code_challenge_method", "S256")
            .appendQueryParameter("scope", String.join(" ", loginParameters.getScopes()));

        if (loginParameters.getLoginHint() != null) {
            builder = builder.appendQueryParameter("login_hint", loginParameters.getLoginHint());
        }

        if (loginParameters.getAcrValues() != null) {
            builder = builder.appendQueryParameter("acr_values", String.join(" ", loginParameters.getAcrValues()));
        }

        if (loginParameters.getUiLocales() != null) {
            builder = builder.appendQueryParameter("ui_locales", loginParameters.getUiLocales());
        }

        return builder.build();
    }

    public Uri getTokenEndpoint() {
        return getIssuer().buildUpon().path("/oauth2/token").build();
    }

    public Uri getLogoutEndpoint() {
        return getIssuer().buildUpon().path("/oauth2/sessions/logout").build();
    }

    public Uri getInitEndpoint() {
        return getIssuer().buildUpon().path("/flow/api/v1/init").build();
    }

    public Uri getFormEndpoint(String formId) {
        return getIssuer().buildUpon().path("/flow/api/v1/form/" + formId).build();
    }

    public Uri getEntryEndpoint(String query) {
        return getIssuer()
            .buildUpon()
            .path("/provider/flow/entry")
            .encodedQuery(query)
            .appendQueryParameter("client_id", getClientId())
            .appendQueryParameter("redirect_uri", getRedirectURI().toString())
            .build();
    }
}
