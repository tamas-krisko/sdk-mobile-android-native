package com.strivacity.android.native_sdk;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.credentials.CreateCredentialResponse;
import androidx.credentials.CreatePublicKeyCredentialRequest;
import androidx.credentials.CreatePublicKeyCredentialResponse;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.GetPublicKeyCredentialOption;
import androidx.credentials.PublicKeyCredential;

import com.strivacity.android.native_sdk.auth.Flow;
import com.strivacity.android.native_sdk.auth.IdTokenClaims;
import com.strivacity.android.native_sdk.auth.NativeSDKError;
import com.strivacity.android.native_sdk.auth.Session;
import com.strivacity.android.native_sdk.auth.config.LoginParameters;
import com.strivacity.android.native_sdk.auth.config.TenantConfiguration;
import com.strivacity.android.native_sdk.render.Form;
import com.strivacity.android.native_sdk.render.ScreenRenderer;
import com.strivacity.android.native_sdk.render.ViewFactory;
import com.strivacity.android.native_sdk.render.widgets.PasskeyEnrollWidget;
import com.strivacity.android.native_sdk.render.widgets.PasskeyLoginWidget;
import com.strivacity.android.native_sdk.render.widgets.WebauthnEnrollWidget;
import com.strivacity.android.native_sdk.render.widgets.WebauthnLoginWidget;
import com.strivacity.android.native_sdk.util.HttpClient;
import com.strivacity.android.native_sdk.util.Logging;

import org.json.JSONException;
import org.json.JSONObject;

import kotlin.Result;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;

import java.net.CookieHandler;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class NativeSDK {

    private static final String STORE_KEY = "Strivacity";

    // Configuration
    private final TenantConfiguration tenantConfiguration;
    private final Executor backgroundThread;
    private final ViewFactory viewFactory;
    private final CookieHandler cookieHandler;
    private final SharedPreferences sharedPreferences;

    @NonNull
    private final HttpClient httpClient;

    @NonNull
    private final Logging logging;

    // Per-login
    private Flow flow;
    private ScreenRenderer screenRenderer;
    private Consumer<IdTokenClaims> onSuccess;
    private Consumer<Throwable> onError;
    private Runnable onFlowFinish;

    // Session data
    private Session session;

    @NonNull
    private final SdkMode sdkMode;

    public NativeSDK(
        TenantConfiguration tenantConfiguration,
        ViewFactory viewFactory,
        CookieHandler cookieHandler,
        SharedPreferences sharedPreferences,
        @NonNull Logging logging,
        @NonNull HttpClient httpClient,
        SdkMode sdkMode
    ) {
        this.tenantConfiguration = tenantConfiguration;
        this.sharedPreferences = sharedPreferences;
        this.viewFactory = viewFactory;
        this.cookieHandler = cookieHandler;
        this.backgroundThread = Executors.newSingleThreadExecutor();
        this.logging = logging;
        this.httpClient = httpClient;
        this.sdkMode = sdkMode != null ? sdkMode : SdkMode.Android;

        if (sharedPreferences != null) {
            String data = sharedPreferences.getString(STORE_KEY, null);
            if (data != null) {
                logging.info("Session restored");
                this.session = new Session(data);
            }
        } else {
            logging.warn("No shared preference provided - this could lead to unintended behavior.");
        }
    }

    public NativeSDK(
        TenantConfiguration tenantConfiguration,
        ViewFactory viewFactory,
        CookieHandler cookieHandler,
        SharedPreferences sharedPreferences,
        @NonNull Logging logging,
        @NonNull HttpClient httpClient
    ) {
        this(tenantConfiguration, viewFactory, cookieHandler, sharedPreferences, logging, httpClient, SdkMode.Android);
    }

    public NativeSDK(
        TenantConfiguration tenantConfiguration,
        ViewFactory viewFactory,
        CookieHandler cookieHandler,
        SharedPreferences sharedPreferences
    ) {
        this(tenantConfiguration, viewFactory, cookieHandler, sharedPreferences, new Logging.DefaultLogging());
    }

    private NativeSDK(
        TenantConfiguration tenantConfiguration,
        ViewFactory viewFactory,
        CookieHandler cookieHandler,
        SharedPreferences sharedPreferences,
        @NonNull Logging logging
    ) {
        this(tenantConfiguration, viewFactory, cookieHandler, sharedPreferences, logging, new HttpClient(logging));
    }

    public IdTokenClaims getIdTokenClaims() {
        if (session == null) {
            logging.debug("ID token claims requested but no session is available");
            return null;
        }

        logging.debug("ID Token claims retrieved");
        return session.getIdTokenClaims();
    }

    public String getAccessToken() {
        if (session == null) {
            logging.debug("Access token requested but no session is available");
            return null;
        }

        logging.debug("Access token retrieved");
        return session.getAccessToken();
    }

    @MainThread
    public void isAuthenticated(Consumer<Boolean> onResponse) {
        backgroundThread.execute(() -> {
            if (session == null) {
                executeOnMain(() -> onResponse.accept(false));
                return;
            }

            boolean hasValidAccessToken =
                session.getAccessToken() != null && session.getExpiration().isAfter(Instant.now());

            if (!hasValidAccessToken && session.getRefreshToken() != null) {
                try {
                    logging.debug("Authentication check - attempting to refresh token");
                    session =
                        Flow.refreshToken(tenantConfiguration, cookieHandler, session.getRefreshToken(), httpClient);
                    if (sharedPreferences != null) {
                        SharedPreferences.Editor edit = sharedPreferences.edit();
                        edit.putString(STORE_KEY, session.toString());
                        edit.apply();
                    }
                    hasValidAccessToken = true;
                    logging.debug("Authentication check - tokens refreshed, authenticated: $authenticated");
                } catch (Exception ex) {
                    logging.debug("Authentication check failed: " + ex.getMessage(), ex);
                }
            }

            boolean authenticated = hasValidAccessToken;

            if (!authenticated) {
                session = null;
            }
            executeOnMain(() -> {
                logging.debug("Authentication check - authenticated:" + authenticated);
                onResponse.accept(authenticated);
            });
        });
    }

    @MainThread
    public void login(
        LoginParameters loginParameters,
        ViewGroup parentLayout,
        Consumer<IdTokenClaims> onSuccess,
        Consumer<Throwable> onError
    ) {
        login(loginParameters, parentLayout, onSuccess, onError, () -> error(new NativeSDKError.HostedFlowCancelled()));
    }

    @MainThread
    public void login(
        LoginParameters loginParameters,
        ViewGroup parentLayout,
        Consumer<IdTokenClaims> onSuccess,
        Consumer<Throwable> onError,
        Runnable onFlowFinish
    ) {
        backgroundThread.execute(() -> {
            logging.info("Starting login flow");
            try {
                this.onSuccess = onSuccess;
                this.onError = onError;
                this.onFlowFinish = onFlowFinish;

                flow = new Flow(tenantConfiguration, cookieHandler, logging, httpClient, sdkMode);
                screenRenderer =
                    new ScreenRenderer(
                        viewFactory,
                        parentLayout,
                        logging,
                        this::submitForm,
                        finalizeUri -> {
                            HttpClient.HttpResponse finalizeResponse = flow.follow(finalizeUri);
                            continueFlow(Uri.parse(finalizeResponse.getBody()));
                        },
                        this::closeFlow
                    );
                Uri finalizeUri = flow.startSession(loginParameters);
                if (finalizeUri != null) {
                    continueFlow(finalizeUri);
                    return;
                }
            } catch (NativeSDKError.OIDCError oidcError) {
                logging.info("Login flow failed " + oidcError);
                logging.debug(
                    String.format(
                        "OIDC ERROR: %s, Description: %s",
                        oidcError.getError(),
                        oidcError.getErrorDescription()
                    )
                );
                error(oidcError);
                return;
            } catch (Exception e) {
                logging.error("Login flow failed" + e, e);
                error(new NativeSDKError.UnknownError(e));
                return;
            }

            submitForm(null);
        });
    }

    @MainThread
    public void cancelFlow() {
        continueFlow(null);
    }

    @MainThread
    public void continueFlow(Uri redirectUri) {
        if (flow == null) {
            return;
        }

        if (redirectUri == null) {
            error(new NativeSDKError.HostedFlowCancelled());
            return;
        }

        String sessionId = redirectUri.getQueryParameter("session_id");
        if (sessionId != null) {
            this.refreshScreen();
            return;
        }

        backgroundThread.execute(() -> {
            try {
                String codeToken = redirectUri.getQueryParameter("code");
                String state = redirectUri.getQueryParameter("state");
                if (!Objects.equals(state, flow.getOidcParams().getState())) {
                    error(new NativeSDKError.OIDCError("Validation error", "State parameter mismatch"));
                    return;
                }

                session = flow.tokenExchange(codeToken);
                success(session.getIdTokenClaims());
            } catch (Exception e) {
                error(new NativeSDKError.UnknownError(e));
            }
        });
    }

    @MainThread
    public void entry(Uri uri, ViewGroup parentLayout, Runnable onFlowFinish, Consumer<Throwable> onError) {
        backgroundThread.execute(() -> {
            try {
                this.onError = onError;
                this.onFlowFinish = onFlowFinish;

                cleanUp();

                if (uri == null) {
                    error(new NativeSDKError.UnknownError(new RuntimeException("Entry URI is null")));
                    return;
                }

                String challenge = uri.getQueryParameter("challenge");
                if (challenge == null || challenge.trim().isEmpty()) {
                    throw new NativeSDKError.UnknownError(new RuntimeException("Entry challenge parameter is missing"));
                }

                flow = new Flow(tenantConfiguration, cookieHandler, logging, httpClient, sdkMode);
                screenRenderer =
                    new ScreenRenderer(
                        viewFactory,
                        parentLayout,
                        logging,
                        this::submitForm,
                        finalizeUri -> {},
                        this::closeFlow
                    );

                try {
                    flow.startWorkflowSession(uri.getQuery());
                } catch (NativeSDKError.WorkflowError workflowError) {
                    error(workflowError);
                    return;
                }
            } catch (NativeSDKError.OIDCError oidcError) {
                error(oidcError);
                return;
            } catch (Exception e) {
                error(new NativeSDKError.UnknownError(e));
                return;
            }

            submitForm(null);
        });
    }

    @MainThread
    public void logout() {
        logging.debug("Logging user out");
        backgroundThread.execute(() -> {
            Flow.logout(tenantConfiguration, cookieHandler, session, httpClient);
            session = null;
            if (sharedPreferences != null) {
                SharedPreferences.Editor edit = sharedPreferences.edit();
                edit.remove(STORE_KEY);
                edit.apply();
            }
            logging.info("User logged out successfully");
        });
    }

    @MainThread
    public void revoke() {
        backgroundThread.execute(() -> {
            try {
                Flow.revoke(tenantConfiguration, cookieHandler, session, httpClient);
                if (sharedPreferences != null) {
                    SharedPreferences.Editor edit = sharedPreferences.edit();
                    edit.remove(STORE_KEY);
                    edit.apply();
                }
            } catch (Throwable e) {
                Log.e("REVOKE", "Revoke failed", e);
            } finally {
                session = null;
            }
        });
    }

    public void refreshScreen() {
        if (screenRenderer == null || flow == null) {
            return;
        }

        this.submitForm(null);
    }

    private void submitForm(@Nullable Form form) {
        backgroundThread.execute(() -> {
            HttpClient.HttpResponse httpResponse;

            if (form == null) {
                logging.debug("Starting login Journey Flow");
                httpResponse = flow.initForm();
            } else {
                logging.debug(String.format("Submitting form %s", form.getId()));
                httpResponse = flow.submitForm(form.getId(), form.requestBody().toString());

                JSONObject requestBody = form.requestBody();
                if (Set.of("passkeyEnroll", "mfaEnrollWebAuthn").contains(form.getId())) {
                    form
                        .getWidgets()
                        .values()
                        .stream()
                        .filter(widget ->
                            widget instanceof PasskeyEnrollWidget || widget instanceof WebauthnEnrollWidget
                        )
                        .map(widget -> {
                            if (widget instanceof PasskeyEnrollWidget) {
                                return ((PasskeyEnrollWidget) widget).getEnrollOptions();
                            }

                            return ((WebauthnEnrollWidget) widget).getEnrollOptions();
                        })
                        .forEach(enrollOptions ->
                            passkeyEnroll(
                                viewFactory.getContext(),
                                enrollOptions.getJsonObject().toString(),
                                errorMessage ->
                                    viewFactory
                                        .getContext()
                                        .getMainExecutor()
                                        .execute(() -> {
                                            Toast
                                                .makeText(viewFactory.getContext(), errorMessage, Toast.LENGTH_SHORT)
                                                .show();
                                            ScreenRenderer.setEnabled(screenRenderer.getParentLayout(), true);
                                        }),
                                credentialData -> {
                                    try {
                                        requestBody.put("credentialData", new JSONObject(credentialData));
                                    } catch (JSONException e) {
                                        throw new RuntimeException(e);
                                    }

                                    renderScreen(flow.submitForm(form.getId(), requestBody.toString()));
                                }
                            )
                        );
                } else {
                    if (Set.of("passkey", "mfaWebAuthnAssertion").contains(form.getId())) {
                        form
                            .getWidgets()
                            .values()
                            .stream()
                            .filter(widget ->
                                widget instanceof PasskeyLoginWidget || widget instanceof WebauthnLoginWidget
                            )
                            .map(widget -> {
                                if (widget instanceof PasskeyLoginWidget) {
                                    return ((PasskeyLoginWidget) widget).getAssertionOptions();
                                }

                                return ((WebauthnLoginWidget) widget).getAssertionOptions();
                            })
                            .forEach(assertionOptions ->
                                passkeyLogin(
                                    viewFactory.getContext(),
                                    assertionOptions.getJsonObject().toString(),
                                    errorMessage ->
                                        viewFactory
                                            .getContext()
                                            .getMainExecutor()
                                            .execute(() -> {
                                                Toast
                                                    .makeText(
                                                        viewFactory.getContext(),
                                                        errorMessage,
                                                        Toast.LENGTH_SHORT
                                                    )
                                                    .show();
                                                ScreenRenderer.setEnabled(screenRenderer.getParentLayout(), true);
                                            }),
                                    credentialData -> {
                                        try {
                                            requestBody.put(
                                                "passkey".equals(form.getId()) ? "passkey" : "assertion",
                                                new JSONObject(credentialData)
                                            );
                                        } catch (JSONException e) {
                                            throw new RuntimeException(e);
                                        }

                                        renderScreen(flow.submitForm(form.getId(), requestBody.toString()));
                                    }
                                )
                            );
                    } else {
                        renderScreen(flow.submitForm(form.getId(), requestBody.toString()));
                    }
                }
            }

            renderScreen(httpResponse);
        });
    }

    private void closeFlow() {
        cleanUp();
        executeOnMain(() -> onFlowFinish.run());
    }

    private void renderScreen(HttpClient.HttpResponse httpResponse) {
        if (screenRenderer == null) {
            return;
        }

        try {
            this.screenRenderer.showScreen(httpResponse);
        } catch (Exception e) {
            executeOnMain(() -> {
                logging.debug(String.format("%s", e));
                logging.warn("Triggering cloud initiated fallback");
                CustomTabsIntent customTabsIntent = new CustomTabsIntent.Builder().build();
                customTabsIntent.intent.setPackage("com.android.chrome");

                customTabsIntent.intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                customTabsIntent.intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);

                try {
                    customTabsIntent.launchUrl(viewFactory.getContext(), screenRenderer.getFallbackUrl());
                } catch (Exception ex) {
                    executeOnMain(() -> onError.accept(ex));
                }
            });
        }
    }

    private void success(@Nullable IdTokenClaims idTokenClaims) {
        cleanUp();

        if (sharedPreferences != null) {
            SharedPreferences.Editor edit = sharedPreferences.edit();
            edit.putString(STORE_KEY, session.toString());
            edit.apply();
        }

        logging.info("User logged in successfully");

        if (onSuccess != null) {
            executeOnMain(() -> onSuccess.accept(idTokenClaims));
        }
    }

    private void error(Throwable throwable) {
        cleanUp();

        if (onError != null) {
            executeOnMain(() -> onError.accept(throwable));
        }
    }

    private void cleanUp() {
        if (screenRenderer != null) {
            screenRenderer.clear();
            screenRenderer = null;
            flow = null;
        }
    }

    private void executeOnMain(Runnable runnable) {
        viewFactory.getContext().getMainExecutor().execute(runnable);
    }

    /**
     * Defines the mode for determining data content in Journey Flow responses.
     * <p>
     * The mode controls what information is included in screen responses, allowing
     * applications to choose between different levels of layout and branding data.
     *
     * @see <a href="https://docs.strivacity.com/reference/journey-flow-api-for-native-clients#step-1-get-oauth2auth">
     *      Journey Flow API for Native Clients</a>
     */
    public enum SdkMode {
        /**
         * Screen responses include layout information but exclude branding information.
         */
        Android("android"),

        /**
         * Screen responses exclude both layout and branding information (minimal mode).
         */
        AndroidMinimal("android-minimal");

        @NonNull
        public final String value;

        /**
         * Constructs an SdkMode with the specified string value.
         *
         * @param value the string representation of this mode
         */
        SdkMode(@NonNull String value) {
            this.value = value;
        }
    }

    private static void passkeyEnroll(
        Context context,
        String requestJson,
        Consumer<String> onError,
        Consumer<String> onResult
    ) {
        CredentialManager credentialManager = CredentialManager.create(context);
        CreatePublicKeyCredentialRequest createPublicKeyCredentialRequest = new CreatePublicKeyCredentialRequest(
            requestJson
        );
        try {
            credentialManager.createCredential(
                context,
                createPublicKeyCredentialRequest,
                new Continuation<CreateCredentialResponse>() {
                    @NonNull
                    @Override
                    public CoroutineContext getContext() {
                        return EmptyCoroutineContext.INSTANCE;
                    }

                    @Override
                    public void resumeWith(@NonNull Object result) {
                        if (result instanceof CreateCredentialResponse) {
                            CreateCredentialResponse response = (CreateCredentialResponse) result;
                            onResult.accept(
                                ((CreatePublicKeyCredentialResponse) response).getRegistrationResponseJson()
                            );
                            return;
                        } else if (result instanceof Result.Failure) {
                            Result.Failure failure = (Result.Failure) result;
                            onError.accept(failure.exception.getLocalizedMessage());
                            return;
                        }

                        onError.accept("Unknown error during passkey enroll");
                    }
                }
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void passkeyLogin(
        Context context,
        String requestJson,
        Consumer<String> onError,
        Consumer<String> onResult
    ) {
        CredentialManager credentialManager = CredentialManager.create(context);
        GetPublicKeyCredentialOption option = new GetPublicKeyCredentialOption(requestJson);
        GetCredentialRequest credentialRequest = new GetCredentialRequest(List.of(option));

        credentialManager.getCredential(
            context,
            credentialRequest,
            new Continuation<GetCredentialResponse>() {
                @NonNull
                @Override
                public CoroutineContext getContext() {
                    return EmptyCoroutineContext.INSTANCE;
                }

                @Override
                public void resumeWith(@NonNull Object result) {
                    if (result instanceof GetCredentialResponse) {
                        GetCredentialResponse response = (GetCredentialResponse) result;
                        Credential credential = response.getCredential();
                        onResult.accept(((PublicKeyCredential) credential).getAuthenticationResponseJson());
                        return;
                    } else if (result instanceof Result.Failure) {
                        Result.Failure failure = (Result.Failure) result;
                        onError.accept(failure.exception.getLocalizedMessage());
                        return;
                    }

                    onError.accept("Unknown error during passkey login");
                }
            }
        );
    }
}
