package com.schibsted.android.sdk.exceptions;

/**
 * Created with IntelliJ IDEA.
 * User: mikaellindstrom
 * Date: 10/31/12
 * Time: 1:44 PM
 * To change this template use File | Settings | File Templates.
 */
public class SPiDKeychainException extends SPiDException {
    public SPiDKeychainException(String errorMessage) {
        super(errorMessage);
    }

    public SPiDKeychainException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
