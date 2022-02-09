package com.exametrika.tests.instrument.instrumentors.data;

public class TestCatchClass {
    public int f2;
    public int f3;

    public Exception testCatch(int param) {
        try {
            if (param == 0)
                throw new IllegalArgumentException();
            else
                throw new UnsupportedOperationException();
        } catch (UnsupportedOperationException e) {
            f2 = 123;
            return e;
        } catch (IllegalArgumentException e) {
            f3 = 123;
            return e;
        } catch (RuntimeException e) {
        }

        return null;
    }
}
