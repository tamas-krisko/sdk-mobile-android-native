package com.strivacity.android.native_sdk.auth;

import java.util.Arrays;

public enum WorkflowError {
    MAGIC_LINK_EXPIRED("magicLinkExpired"),

    CLIENT_MISMATCH("clientMismatch"),
    INVALID_REDIRECT_URI("invalidRedirectUri");

    private final String id;

    WorkflowError(String id) {
        this.id = id;
    }

    public static WorkflowError valueOfId(String id) {
        return Arrays.stream(values()).filter(workflowError -> workflowError.id.equals(id)).findFirst().orElse(null);
    }
}
