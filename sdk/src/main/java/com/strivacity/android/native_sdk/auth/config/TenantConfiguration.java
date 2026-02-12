package com.strivacity.android.native_sdk.auth.config;

import android.net.Uri;

import com.strivacity.android.native_sdk.NativeSDK;

import lombok.Data;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Data
public class TenantConfiguration {

    private final Uri issuer;
    private final String clientId;
    private final Uri redirectURI;
    private final Uri postLogoutURI;

    public Uri getAuthEndpoint(OidcParams oidcParams, LoginParameters loginParameters, NativeSDK.SdkMode sdkMode) {
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
            .appendQueryParameter("scope", String.join(" ", loginParameters.getScopes()))
            .appendQueryParameter("sdk", sdkMode.value);

        if (loginParameters.getLoginHint() != null) {
            builder.appendQueryParameter("login_hint", loginParameters.getLoginHint());
        }

        if (loginParameters.getAcrValues() != null) {
            builder.appendQueryParameter("acr_values", String.join(" ", loginParameters.getAcrValues()));
        }

        if (loginParameters.getUiLocales() != null) {
            builder.appendQueryParameter("ui_locales", loginParameters.getUiLocales());
        }

        final List<String> audiences = loginParameters.getAudiences();

        Optional
            .ofNullable(audiences)
            .map(strings ->
                strings.stream().filter(s -> s != null && !s.trim().isBlank()).collect(Collectors.joining(" "))
            )
            .filter(p -> !p.isBlank())
            .ifPresent(audiencesParam -> builder.appendQueryParameter("audiences", audiencesParam));

        return builder.build();
    }

    public Uri getTokenEndpoint() {
        return getIssuer().buildUpon().path("/oauth2/token").build();
    }

    public Uri getLogoutEndpoint() {
        return getIssuer().buildUpon().path("/oauth2/sessions/logout").build();
    }

    public Uri getRevokeEndpoint() {
        return getIssuer().buildUpon().path("/oauth2/revoke").build();
    }

    public Uri getInitEndpoint() {
        return getIssuer().buildUpon().path("/flow/api/v1/init").build();
    }

    public Uri getFormEndpoint(String formId) {
        return getIssuer().buildUpon().path("/flow/api/v1/form/" + formId).build();
    }

    public Uri getEntryEndpoint(String query, NativeSDK.SdkMode sdkMode) {
        return getIssuer()
            .buildUpon()
            .path("/provider/flow/entry")
            .encodedQuery(query)
            .appendQueryParameter("client_id", getClientId())
            .appendQueryParameter("redirect_uri", getRedirectURI().toString())
            .appendQueryParameter("sdk", sdkMode.value)
            .build();
    }
}
