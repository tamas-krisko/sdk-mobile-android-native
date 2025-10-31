package com.strivacity.android.demo;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.strivacity.android.demo.databinding.ActivityMainBinding;
import com.strivacity.android.demo.render.ViewFactory;
import com.strivacity.android.native_sdk.NativeSDK;
import com.strivacity.android.native_sdk.auth.config.TenantConfiguration;

import java.net.CookieManager;

public class MainActivity extends AppCompatActivity {

    private static final String ISSUER = "https://example.org";
    private static final String CLIENT_ID = "";
    private static final String POST_LOGOUT_URI = "android://native-flow";
    public static final String REDIRECT_URI = "android://native-flow";
    public NativeSDK nativeSDK;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TenantConfiguration tenantConfiguration = new TenantConfiguration(
            Uri.parse(ISSUER),
            CLIENT_ID,
            Uri.parse(REDIRECT_URI),
            Uri.parse(POST_LOGOUT_URI)
        );

        nativeSDK =
            new NativeSDK(
                tenantConfiguration,
                new ViewFactory(this),
                new CookieManager(),
                this.getSharedPreferences("test", MODE_PRIVATE)
            );

        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        FloatingActionButton cancelFlowButton = findViewById(R.id.cancelFlowButton);
        cancelFlowButton.setVisibility(View.GONE);
        cancelFlowButton.setOnClickListener(view -> {
            nativeSDK.cancelFlow();
            cancelFlowButton.setVisibility(View.GONE);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (getIntent().getData() != null && getIntent().getData().toString().startsWith(REDIRECT_URI)) {
            /* when a fallback initiated and redirected back to the native application,
               this line will trigger the proper OIDC PKCE flow initialize */
            nativeSDK.continueFlow(getIntent().getData());
        }
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }
}
