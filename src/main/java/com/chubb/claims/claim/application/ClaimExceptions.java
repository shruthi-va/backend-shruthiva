package com.chubb.claims.claim.application;

public final class ClaimExceptions {

    private ClaimExceptions() {
    }

    public static class NotFound extends RuntimeException {
        public NotFound() {
            super("Claim not found.");
        }
    }

    public static class Conflict extends RuntimeException {
        public Conflict(String message) {
            super(message);
        }
    }

    public static class InvalidRequest extends RuntimeException {
        public InvalidRequest(String message) {
            super(message);
        }
    }
}
