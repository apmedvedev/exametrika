package com.exametrika.tests.instrument.instrumentors.data;


@SuppressWarnings("unused")
public class TestInterceptClass2 {
    public TestInterceptClass1 object;
    public String testField1;

    public TestInterceptClass2() {
        this(null);
        testField1 = "test";
    }

    public TestInterceptClass2(TestInterceptClass1 object) {
        this.object = object;

        testField1 = "test2";
    }

    private class InnerClass1 {
        private void privateMethod() {
            testField1 = "innerCalled";
        }
    }

    public static class InnerClass2 {
        private String testField1;

        public void publicMethod() {
            testField1 = "called";
        }
    }
}
