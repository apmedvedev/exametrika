package com.exametrika.tests.instrument.instrumentors.data;

public class TestThrowClass {
    public int f1;

    public Exception testThrow() {
        try {
            f1 = 123;
            throw new IllegalArgumentException();
        } catch (IllegalArgumentException e) {
            return e;
        } catch (RuntimeException e) {
        }

        return null;
    }
}
