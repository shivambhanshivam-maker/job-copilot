package com.shivam.jobcopilot.exception;

import java.io.IOException;

public class GmailReauthenticationRequiredException extends IOException {

    public GmailReauthenticationRequiredException(String message) {
        super(message);
    }

    public GmailReauthenticationRequiredException(String message, Throwable cause) {
        super(message, cause);
    }
}
