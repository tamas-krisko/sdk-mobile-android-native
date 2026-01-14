package com.strivacity.android.native_sdk.auth;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import lombok.Getter;

import java.util.Arrays;
import java.util.function.Predicate;

public class NativeSDKError {

    @Getter
    public static class OIDCError extends RuntimeException {

        private final String error;
        private final String errorDescription;

        public OIDCError(String error, String errorDescription) {
            super(error + " - " + errorDescription);
            this.error = error;
            this.errorDescription = errorDescription;
        }
    }

    @Getter
    public static class WorkflowError extends OIDCError {

        public WorkflowError(@NonNull String error, @Nullable String errorDescription) {
            super(error, errorDescription);
        }

        public enum WorkflowErrorId {
            MAGIC_LINK_EXPIRED("magicLinkExpired"),
            CLIENT_MISMATCH("clientMismatch"),
            INVALID_REDIRECT_URI("invalidRedirectUri");

            private final String id;

            WorkflowErrorId(@NonNull String id) {
                this.id = id;
            }

            public static WorkflowErrorId valueOfId(String id) {
                return Arrays.stream(values()).filter(byId(id)).findFirst().orElse(null);
            }

            /**
             * Matcher used to filter stream by WorkflowError id
             * @param idParam id to match against
             * @return predicate that returns true if workflowError id matches, false otherwise
             */
            private static Predicate<WorkflowErrorId> byId(String idParam) {
                return workflowError -> workflowError.id.equals(idParam);
            }
        }
    }

    public static class HostedFlowCancelled extends RuntimeException {

        public HostedFlowCancelled() {
            super("Hosted flow has been cancelled");
        }
    }

    public static class UnknownError extends RuntimeException {

        public UnknownError(Throwable throwable) {
            super(throwable);
        }
    }
}
