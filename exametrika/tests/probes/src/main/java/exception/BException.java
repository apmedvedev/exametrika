package exception;

/**
 * Copyright 2014 Andrey Medvedev. All rights reserved.
 */


public class BException extends AException {
    public BException() {
        this("");
    }

    public BException(String s) {
        super(s);
    }

    public String getT() {
        return getMessage();
    }
}