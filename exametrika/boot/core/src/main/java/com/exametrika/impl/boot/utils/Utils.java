/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.boot.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * The {@link Utils} contains different utility methods.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class Utils {
    public static final boolean IS_64_BIT = is64bit();
    private static char[] hexDigits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    public static String escape(String string) {
        if (string == null || string.length() == 0)
            return "";

        char c = 0;
        String hhhh;

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < string.length(); i++) {
            c = string.charAt(i);
            switch (c) {
                case '\\':
                case '"':
                case '\'':
                case '{':
                case '}':
                case '[':
                case ']':
                case ':':
                case '=':
                case ',':
                case ';':
                    builder.append('\\');
                    builder.append(c);
                    break;
                case '\b':
                    builder.append("\\b");
                    break;
                case '\t':
                    builder.append("\\t");
                    break;
                case '\n':
                    builder.append("\\n");
                    break;
                case '\f':
                    builder.append("\\f");
                    break;
                case '\r':
                    builder.append("\\r");
                    break;
                default:
                    if (c < ' ' || (c >= '\u0080' && c < '\u00a0') || (c >= '\u2000' && c < '\u2100')) {
                        hhhh = "000" + Integer.toHexString(c);
                        builder.append("\\u" + hhhh.substring(hhhh.length() - 4));
                    } else
                        builder.append(c);
            }
        }
        return builder.toString();
    }

    public static String expandProperties(String str, boolean escape) {
        StringBuilder builder = new StringBuilder();

        boolean expanded = false;
        int curPos = 0;

        while (curPos < str.length()) {
            int startPos = str.indexOf("${", curPos);
            if (startPos == -1) {
                if (expanded)
                    builder.append(str.substring(curPos));
                break;
            }
            int endPos = str.indexOf('}', startPos + 2);
            if (endPos == -1) {
                if (expanded)
                    builder.append(str.substring(curPos));
                break;
            }

            String propertyName = str.substring(startPos + 2, endPos);

            String propertyValue = System.getProperty(propertyName);
            if (propertyValue == null) {
                propertyValue = System.getenv(propertyName);
                if (propertyValue == null)
                    throw new IllegalArgumentException("Property '" + propertyName + "' is not found.");
            }

            builder.append(str.substring(curPos, startPos));
            if (escape)
                builder.append(escape(propertyValue));
            else
                builder.append(propertyValue);
            expanded = true;
            curPos = endPos + 1;
        }

        if (expanded)
            return builder.toString();

        return str;
    }

    public static void move(File source, File destination) {
        if (source == null)
            throw new IllegalArgumentException();
        if (!source.exists())
            throw new IllegalArgumentException();
        if (destination == null)
            throw new IllegalArgumentException();

        if (destination.exists()) {
            if (destination.isDirectory())
                emptyDir(destination);

            if (!destination.delete())
                throw new IllegalStateException();
        }

        copy(source, destination);
        emptyDir(source);
        source.delete();
//        if (!source.renameTo(destination))
//            throw new IllegalStateException();
    }

    public static void emptyDir(File dir) {
        if (dir == null)
            throw new IllegalArgumentException();

        if (!dir.isDirectory())
            return;

        for (File file : dir.listFiles()) {
            if (file.isDirectory())
                emptyDir(file);

            file.delete();
        }
    }

    public static void delete(File file) {
        if (file.isDirectory())
            emptyDir(file);

        file.delete();
    }

    public static void copy(File source, File destination) {
        if (source == null)
            throw new IllegalArgumentException();
        if (destination == null)
            throw new IllegalArgumentException();
        if (!source.exists())
            throw new IllegalArgumentException(source.getPath());

        if (source.isFile())
            copyFile(source, destination);
        else
            copyDir(source, destination);
    }

    public static void copyDir(File sourceDir, File destinationDir) {
        if (!sourceDir.isDirectory())
            throw new IllegalArgumentException();

        destinationDir.mkdirs();
        for (File sourceFile : sourceDir.listFiles()) {
            File destinationFile = new File(destinationDir, sourceFile.getName());
            if (sourceFile.isDirectory())
                copyDir(sourceFile, destinationFile);
            else
                copyFile(sourceFile, destinationFile);
        }
    }

    public static void copyFile(File source, File destination) {
        if (!source.isFile())
            throw new IllegalArgumentException();

        destination.getParentFile().mkdirs();

        InputStream in = null;
        OutputStream out = null;

        try {
            in = new BufferedInputStream(new FileInputStream(source));
            out = new BufferedOutputStream(new FileOutputStream(destination));

            copy(in, out);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            close(in);
            close(out);
        }
    }

    public static void copy(InputStream source, OutputStream destination) throws IOException {
        if (source == null)
            throw new IllegalArgumentException();
        if (destination == null)
            throw new IllegalArgumentException();
        int bufferSize = 8192;

        byte[] buffer = new byte[bufferSize];

        while (true) {
            int length = source.read(buffer);
            if (length == -1)
                break;

            destination.write(buffer, 0, length);
        }
    }

    public static void close(Closeable closeable) {
        if (closeable == null)
            return;

        try {
            closeable.close();
        } catch (IOException e) {
            Loggers.logError(Utils.class.getName(), e);
        }
    }

    public static String md5Hash(File file) {
        if (!file.exists())
            throw new IllegalArgumentException();

        InputStream stream = null;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            stream = new FileInputStream(file);
            stream = new DigestInputStream(new BufferedInputStream(stream), md);

            byte[] buffer = new byte[1000];
            while (stream.read(buffer) != -1)
                ;

            return Utils.digestToString(md.digest());
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            close(stream);
        }
    }

    public static String digestToString(byte[] digest) {
        StringBuffer builder = new StringBuffer();
        for (int i = 0; i < digest.length; i++) {
            builder.append(hexDigits[(digest[i] >>> 4) & 0xF]);
            builder.append(hexDigits[digest[i] & 0xF]);
        }

        return builder.toString();
    }

    public static void unzip(File source, File destination) {
        if (source == null)
            throw new IllegalArgumentException();
        if (!source.isFile())
            throw new IllegalArgumentException();
        if (destination == null)
            throw new IllegalArgumentException();
        if (!source.exists())
            throw new IllegalArgumentException();
        if (!destination.exists())
            destination.mkdirs();
        if (!destination.isDirectory())
            throw new IllegalArgumentException();

        FileInputStream fileStream = null;

        try {
            fileStream = new FileInputStream(source);
            ZipInputStream stream = new ZipInputStream(new BufferedInputStream(fileStream));
            while (true) {
                ZipEntry entry = stream.getNextEntry();
                if (entry == null)
                    break;

                if (entry.isDirectory())
                    new File(destination, entry.getName()).mkdirs();
                else {
                    FileOutputStream fileOut = null;
                    try {
                        fileOut = new FileOutputStream(new File(destination, entry.getName()));
                        copy(stream, fileOut);
                    } finally {
                        close(fileOut);
                    }
                }
            }
            close(stream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            close(fileStream);
        }
    }

    private static boolean is64bit() {
        String value = System.getProperty("sun.arch.data.model");
        if (value != null)
            return value.contains("64");
        else
            return System.getProperty("os.arch").contains("64");
    }

    private Utils() {
    }
}
