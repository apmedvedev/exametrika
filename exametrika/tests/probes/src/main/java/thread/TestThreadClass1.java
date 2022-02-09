package thread;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * {@link TestThreadClass1} is a complex test modelling multithreaded communication between several consumers and producers in JVM.
 *
 * @author medvedev
 */
public class TestThreadClass1 {
    private static final ThreadPoolExecutor executor = new ThreadPoolExecutor(10, 10, 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue(100),
            new ThreadPoolExecutor.CallerRunsPolicy());
    private static final AtomicLong counter = new AtomicLong();
    private static final AtomicLong callersCounter = new AtomicLong();

    public static void main(String[] args) throws Throwable {
        Thread client1 = new Thread(new ExecutorClient(), "client1");
        Thread client2 = new Thread(new ExecutorClient(), "client2");
        Thread client3 = new Thread(new ExecutorClient(), "client3");
        client1.start();
        client2.start();
        client3.start();
        client1.join();
        client2.join();
        client3.join();
    }

    private static class ExecutorClient implements Runnable {
        @Override
        public void run() {
            try {
                for (int i = 0; i < 10000; i++) {
                    long t = System.currentTimeMillis();

                    test();

                    t = System.currentTimeMillis() - t;

                    StringBuilder builder = new StringBuilder("---------------- " +
                            Thread.currentThread().getName() + ":" + i + ": " + t + ", count: " + counter.get() + ", callers count: " + callersCounter.get());
                    System.out.println(builder);
                }
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }

        private void test() throws Throwable {
            for (int i = 0; i < 1000; i++)
                executor.execute(new ThreadTask(Thread.currentThread()));

            //Thread.sleep(1000);
        }
    }

    private static class ThreadTask implements Runnable {
        private final Thread caller;

        public ThreadTask(Thread caller) {
            this.caller = caller;
        }

        @Override
        public void run() {
            try {
                Thread.sleep(1);

                if (Thread.currentThread() != caller)
                    counter.incrementAndGet();
                else
                    callersCounter.incrementAndGet();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}