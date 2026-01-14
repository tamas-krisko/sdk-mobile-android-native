package com.strivacity.android.demo;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.strivacity.android.demo.databinding.FragmentFirstBinding;
import com.strivacity.android.native_sdk.NativeSDK;
import com.strivacity.android.native_sdk.auth.IdTokenClaims;
import com.strivacity.android.native_sdk.auth.NativeSDKError;
import com.strivacity.android.native_sdk.auth.NativeSDKError.WorkflowError.WorkflowErrorId;
import com.strivacity.android.native_sdk.auth.config.LoginParameters;

import java.util.List;
import java.util.Map;

public class FirstFragment extends Fragment {

    private FragmentFirstBinding binding;
    private MainActivity mainActivity;
    private NativeSDK nativeSDK;
    private FloatingActionButton cancelFlowButton;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentFirstBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mainActivity = ((MainActivity) view.getContext());
        nativeSDK = mainActivity.nativeSDK;
        binding.buttonLogin.setOnClickListener(v -> {
            binding.appLayout.setVisibility(View.GONE);
            binding.appScreenLayoutContainer.setVisibility(View.VISIBLE);
            cancelFlowButton = mainActivity.findViewById(R.id.cancelFlowButton);
            cancelFlowButton.setVisibility(View.VISIBLE);

            nativeSDK.login(
                LoginParameters.builder().scopes(List.of("openid", "email")).build(),
                binding.appScreenLayout,
                this::showProfileScreen,
                throwable -> {
                    Toast.makeText(getContext(), throwable.getMessage(), Toast.LENGTH_SHORT).show();
                    showLoginScreen();
                }
            );
        });

        binding.buttonLogout.setOnClickListener(v -> {
            nativeSDK.logout();
            showLoginScreen();
        });

        binding.buttonRevoke.setOnClickListener(v -> {
            nativeSDK.revoke();
            showLoginScreen();
        });

        nativeSDK.isAuthenticated(authenticated -> {
            if (authenticated) {
                showProfileScreen(nativeSDK.getIdTokenClaims());
            } else {
                showLoginScreen();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        final Uri data = mainActivity.getIntent().getData();
        if (data != null && !data.toString().startsWith(MainActivity.REDIRECT_URI)) {
            mainActivity
                .getApplicationContext()
                .getMainExecutor()
                .execute(() -> {
                    binding.appLayout.setVisibility(View.GONE);
                    binding.appScreenLayoutContainer.setVisibility(View.VISIBLE);
                    cancelFlowButton = mainActivity.findViewById(R.id.cancelFlowButton);
                    cancelFlowButton.setVisibility(View.VISIBLE);
                });

            entry(data);
            mainActivity.getIntent().setData(null);
        }
    }

    private void entry(Uri uri) {
        nativeSDK.entry(
            uri,
            binding.appScreenLayout,
            this::showLoginScreen,
            error -> {
                String mappedErrorMessage = null;
                if (error instanceof NativeSDKError.WorkflowError) {
                    final String idValue = ((NativeSDKError.WorkflowError) error).getError();
                    final WorkflowErrorId errorId = WorkflowErrorId.valueOfId(idValue);
                    mappedErrorMessage = WORKFLOW_ERROR_ID_TO_MESSAGE.get(errorId);
                }
                final String toastText = mappedErrorMessage != null
                    ? mappedErrorMessage
                    : "Something bad happened, please try again";
                Toast.makeText(getContext(), toastText, Toast.LENGTH_SHORT).show();
                showLoginScreen();
            }
        );
    }

    private static final Map<WorkflowErrorId, String> WORKFLOW_ERROR_ID_TO_MESSAGE = Map.of(
        WorkflowErrorId.MAGIC_LINK_EXPIRED,
        "Your link has expired",
        WorkflowErrorId.CLIENT_MISMATCH,
        "An unexpected error occurred, please try again (001)",
        WorkflowErrorId.INVALID_REDIRECT_URI,
        "An unexpected error occurred, please try again (002)"
    );

    private void showLoginScreen() {
        if (cancelFlowButton != null) {
            cancelFlowButton.setVisibility(View.GONE);
        }

        binding.appLayout.setVisibility(View.VISIBLE);
        binding.appScreenLayoutContainer.setVisibility(View.GONE);

        binding.buttonLogin.setVisibility(View.VISIBLE);
        binding.buttonRevoke.setVisibility(View.GONE);
        binding.buttonLogout.setVisibility(View.GONE);

        binding.textviewWelcome.setText("Login required");
    }

    private void showProfileScreen(IdTokenClaims idTokenClaims) {
        if (cancelFlowButton != null) {
            cancelFlowButton.setVisibility(View.GONE);
        }

        binding.appLayout.setVisibility(View.VISIBLE);
        binding.appScreenLayoutContainer.setVisibility(View.GONE);

        binding.buttonLogin.setVisibility(View.GONE);
        binding.buttonRevoke.setVisibility(View.VISIBLE);
        binding.buttonLogout.setVisibility(View.VISIBLE);

        binding.textviewWelcome.setText(idTokenClaims.getString("email"));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
