package exception;

/**
 * Copyright 2014 Andrey Medvedev. All rights reserved.
 */


public class CException extends AException {
    public CException(int i) {
        this("Default C exception " + i);
    }

    public CException(String s) {
        super(s);
    }

    public Exception getB() {
        return new BException("This is a B exception.");
    }
}