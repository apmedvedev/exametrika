package exception;

/**
 * Copyright 2014 Andrey Medvedev. All rights reserved.
 */


public class AException extends RuntimeException {
    public AException() {
    }

    public AException(String s) {
        super(s);
    }
}