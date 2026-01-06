package com.strivacity.android.native_sdk.auth.config;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Locale;

@Data
@Builder
public class LoginParameters {

    private final String prompt;
    private final String loginHint;
    private final List<String> acrValues;
    private final List<String> scopes;
    private final List<String> audiences;

    @Builder.Default
    private String uiLocales = Locale.getDefault().toLanguageTag();
}
