package file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class TestFileClass1 {
    private static final int COUNT = 1000;
    private static final boolean sleep = false;
    private final File file1;
    private final FileOutputStream out;
    private final FileInputStream in;
    private final File file2;
    private final FileChannel channel;
    private final File file3;
    private final RandomAccessFile random;
    private final byte[] buffer1 = new byte[10000];
    private final ByteBuffer buffer2 = ByteBuffer.allocate(10000);

    public static void main(String[] args) throws Throwable {
        for (int i = 0; i < 10000; i++) {
            long t = System.currentTimeMillis();
            run(0);
            t = System.currentTimeMillis() - t;
            System.out.println("---------------- " + i + ": " + t);
        }
    }

    public static void run(int i) throws Throwable {
        TestFileClass1 c = new TestFileClass1(i);
        try {
            c.test();
        } finally {
            c.close();
        }
    }

    public TestFileClass1(int i) throws Throwable {
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        File root = new File(tempDir, "files" + i);
        root.mkdirs();

        file1 = new File(root, "file1.tmp");
        file1.delete();
        out = new FileOutputStream(file1);
        in = new FileInputStream(file1);

        file2 = new File(root, "file2.tmp");
        file2.delete();
        channel = new RandomAccessFile(file2, "rw").getChannel();
        file3 = new File(root, "file3.tmp");
        file3.delete();
        random = new RandomAccessFile(file3, "rw");
    }

    public void close() throws Throwable {
        in.close();
        out.close();
        channel.close();
        random.close();
    }

    public void test() throws Throwable {
        writeFileStream();
        readFileStream();
        writeFileChannel();
        readFileChannel();
        writeRandomAccessFile();
        readRandomAccessFile();
    }

    private void writeFileStream() throws Throwable {
        for (int i = 0; i < COUNT; i++) {
            out.write(10);
            out.write(buffer1);
            out.write(buffer1, 0, buffer1.length);
            if (sleep)
                Thread.sleep(1000);
        }
    }

    private void readFileStream() throws Throwable {
        for (int i = 0; i < COUNT; i++) {
            in.read();
            in.read(buffer1);
            in.read(buffer1, 0, buffer1.length);
            if (sleep)
                Thread.sleep(1000);
        }
    }

    private void writeFileChannel() throws Throwable {
        channel.position(0);
        for (int i = 0; i < COUNT; i++) {
            channel.write(buffer2, channel.position());
            channel.position(channel.position() + buffer2.limit());
            buffer2.rewind();
            channel.write(buffer2);
            buffer2.rewind();
            channel.write(new ByteBuffer[]{buffer2});
            buffer2.rewind();
            channel.write(new ByteBuffer[]{buffer2}, 0, 1);
            buffer2.rewind();
            if (sleep)
                Thread.sleep(1000);
        }
    }

    private void readFileChannel() throws Throwable {
        channel.position(0);
        for (int i = 0; i < COUNT; i++) {
            channel.read(buffer2, channel.position());
            channel.position(channel.position() + buffer2.limit());
            buffer2.rewind();
            channel.read(buffer2);
            buffer2.rewind();
            channel.read(new ByteBuffer[]{buffer2});
            buffer2.rewind();
            channel.read(new ByteBuffer[]{buffer2}, 0, 1);
            buffer2.rewind();
            if (sleep)
                Thread.sleep(1000);
        }
    }

    private void writeRandomAccessFile() throws Throwable {
        random.seek(0);
        for (int i = 0; i < COUNT; i++) {
            random.write(10);
            random.write(buffer1);
            random.write(buffer1, 0, buffer1.length);
            if (sleep)
                Thread.sleep(1000);
        }
    }

    private void readRandomAccessFile() throws Throwable {
        random.seek(0);
        for (int i = 0; i < COUNT; i++) {
            random.read();
            random.read(buffer1);
            random.read(buffer1, 0, buffer1.length);
            if (sleep)
                Thread.sleep(1000);
        }
    }
}
