package com.exametrika.impl.boot.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;

/**
 * The {@link JsonReader} is JSON text reader. Reader supports full JSON specification and following extensions:
 * <ul>
 * <li> key can be separated from value by '='
 * <li> values in arrays, and key:value pairs in objects can be separated by ';'
 * <li> strings in keys and values can be enclosed in single quotes
 * <li> key can omit quotes if it does not contain key separator (':' or '=') or whitespace
 * (all whitespaces in unquoted keys considered insignificant and skipped)
 * <li> singleline comments ('//') and multiline comments ('/*') are allowed where insignificant whitespace is allowed
 * <li> non-escaped strings are supported in values (strings enclosed in /../). Non-escaped string can contain any text and don't support
 * escape sequences. They can be multiline, in this case all their content is handled as is.
 * <li> multiline strings are supported, all content between new line and next non-whitespace symbol is skipped. Backslash before newline is also supported.
 * <li> hexadecimal numbers
 * <li> tolerates absence of value separators in arrays and separators between key:value pairs in objects
 * </ul>
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are not thread safe.
 */
public class JsonReader {
    /**
     * JSON reader handler.
     */
    public interface IJsonHandler {
        /**
         * Called when text is started.
         */
        void startText();

        /**
         * Called when text is ended.
         */
        void endText();

        /**
         * Called when object is started.
         */
        void startObject();

        /**
         * Called when object is ended.
         */
        void endObject();

        /**
         * Called when array is started.
         */
        void startArray();

        /**
         * Called when array is ended.
         */
        void endArray();

        /**
         * Called when key has been read.
         *
         * @param key key
         */
        void key(String key);

        /**
         * Called when value has been read.
         *
         * @param value value. Can be null if 'null' literal has been read
         */
        void value(Object value);
    }

    private final Reader reader;
    private final IJsonHandler handler;
    private int[] buffer = new int[10];
    private int index = -1;
    private int pos = 1;
    private int line = 1;

    public JsonReader(Reader reader, IJsonHandler handler) {
        this.reader = reader;
        this.handler = handler;
    }

    public JsonReader(InputStream inputStream, IJsonHandler handler) {
        this(new InputStreamReader(inputStream), handler);
    }

    public JsonReader(String value, IJsonHandler handler) {
        this(new StringReader(value), handler);
    }

    public int getLine() {
        return line;
    }

    public int getPosition() {
        return pos;
    }

    public void read() {
        handler.startText();

        int c = peek(true);

        switch (c) {
            case '[':
                readArray();
                break;
            case '{':
                readObject();
                break;
            default:
                throw new IllegalArgumentException("A JSON text must start with '[' or '{'.");
        }

        handler.endText();
    }

    private void readObject() {
        next(true);

        handler.startObject();

        if (peek(true) == '}') {
            next(true);
            handler.endObject();
        } else {
            while (true) {
                if (peek(true) != '}') {
                    readKey();
                    readValue();
                }

                int c = next(true);
                switch (c) {
                    case '}':
                        handler.endObject();
                        return;
                    case ',':
                    case ';':
                        break;
                    default:
                        back(c);
                }
            }
        }
    }

    private void readArray() {
        next(true);

        handler.startArray();

        if (peek(true) == ']') {
            next(true);
            handler.endArray();
        } else {
            while (true) {
                if (peek(true) != ']')
                    readValue();

                int c = next(true);
                switch (c) {
                    case ']':
                        handler.endArray();
                        return;
                    case ',':
                    case ';':
                        break;
                    default:
                        back(c);
                }
            }
        }
    }

    private void readKey() {
        String key = readString(":=");

        int c = next(true);
        switch (c) {
            case ':':
            case '=':
                break;
            default:
                throw new IllegalArgumentException("Invalid end of key at[ln:" + line + ",col:" + pos + "].");
        }

        handler.key(key);
    }

    private void readValue() {
        int c = peek(true);

        switch (c) {
            case '{':
                readObject();
                break;
            case '[':
                readArray();
                break;
            case '\"':
            case '\'':
                handler.value(readString(""));
                break;
            case '/':
                handler.value(readNonEscapedString());
                break;
            case 't':
                read("true");
                handler.value(true);
                break;
            case 'f':
                read("false");
                handler.value(false);
                break;
            case 'n':
                read("null");
                handler.value(null);
                break;
            default:
                if (Character.isDigit(c) || c == '-' || c == '+')
                    handler.value(readNumber(c));
                else
                    throw new IllegalArgumentException("Invalid value at[ln:" + line + ",col:" + pos + "].");
        }
    }

    private String readString(String delimiters) {
        boolean quoted = false;
        int c = next(true);
        if (c == '\"') {
            delimiters = "\"";
            quoted = true;
            c = next(false);
        } else if (c == '\'') {
            delimiters = "\'";
            quoted = true;
            c = next(false);
        }

        StringBuilder builder = new StringBuilder();
        while (true) {
            if (delimiters.indexOf(c) != -1)
                break;

            if (c == '\\') {
                c = next(false);
                switch (c) {
                    case 'b':
                        builder.append('\b');
                        break;
                    case 'f':
                        builder.append('\f');
                        break;
                    case 'r':
                        builder.append('\r');
                        break;
                    case 'n':
                        builder.append('\n');
                        break;
                    case 't':
                        builder.append('\t');
                        break;
                    case 'u':
                        builder.append(readUnicode());
                        break;
                    case '\r':
                    case '\n':
                        c = next(true);
                        back(c);
                        break;
                    default:
                        builder.append((char) c);
                }
            } else if (c == '\r' || c == '\n') {
                c = next(true);
                back(c);
            } else if (c != -1)
                builder.append((char) c);
            else
                throw new IllegalArgumentException("Invalid value at[ln:" + line + ",col:" + pos + "].");

            c = next(!quoted);
        }

        if (!quoted)
            back(c);

        return builder.toString();
    }

    private String readNonEscapedString() {
        next(false);

        StringBuilder builder = new StringBuilder();
        while (true) {
            int c = next(false);
            if (c == '/')
                break;
            else if (c != -1)
                builder.append((char) c);
            else
                throw new IllegalArgumentException("Invalid value at[ln:" + line + ",col:" + pos + "].");
        }

        return builder.toString();
    }

    private char readUnicode() {
        int value = readHexDigit() << 12;
        value |= readHexDigit() << 8;
        value |= readHexDigit() << 4;
        value |= readHexDigit();

        return (char) value;
    }

    private int readHexDigit() {
        int c = next(false);
        if (c >= '0' && c <= '9')
            return c - '0';
        else if (c >= 'A' && c <= 'F')
            return c - 'A';
        else if (c >= 'a' && c <= 'f')
            return c - 'a';
        else
            throw new IllegalArgumentException("Invalid hex digit at[ln:" + line + ",col:" + pos + "].");

    }

    private void read(String template) {
        for (int i = 0; i < template.length(); i++) {
            if (next(false) != template.charAt(i))
                throw new IllegalArgumentException("Invalid keyword at[ln:" + line + ",col:" + pos + "].");
        }
    }

    private Number readNumber(int c) {
        int sign = 0;
        if (c == '-' || c == '+') {
            sign = c;
            next(false);
            c = peek(false);
        }

        if (c == '0') {
            next(false);
            c = peek(false);
            if (c == 'x' || c == 'X')
                return readHexNumber(sign);
            else
                back('0');
        }

        if (sign != 0)
            back(sign);

        return readDecimalNumber();
    }

    private Number readDecimalNumber() {
        StringBuilder builder = new StringBuilder();
        boolean isDouble = false;
        boolean stop = false;

        while (true) {
            int c = next(false);

            switch (c) {
                case '-':
                case '+':
                    builder.append((char) c);
                    break;
                case 'E':
                case 'e':
                case '.':
                    builder.append((char) c);
                    isDouble = true;
                    break;
                default:
                    if (Character.isDigit(c))
                        builder.append((char) c);
                    else {
                        back(c);
                        stop = true;
                    }
            }

            if (stop)
                break;
        }

        if (isDouble)
            return Double.valueOf(builder.toString());
        else
            return Long.valueOf(builder.toString());
    }

    private Number readHexNumber(int sign) {
        next(false);
        StringBuilder builder = new StringBuilder();

        if (sign == '-')
            builder.append((char) sign);

        while (true) {
            int c = next(false);

            if (c >= '0' && c <= '9' || c >= 'A' && c <= 'F' || c >= 'a' && c <= 'f')
                builder.append((char) c);
            else {
                back(c);
                break;
            }
        }

        return Long.valueOf(builder.toString(), 16);
    }

    private int peek(boolean skipWhitespace) {
        int c = next(skipWhitespace);
        back(c);
        return c;
    }

    private int next(boolean skipWhitespace) {
        if (!skipWhitespace)
            return next();

        while (true) {
            int c = next();

            if (c == ' ' || c == '\r' || c == '\n' || c == '\t')
                continue;

            if (c == '/') {
                c = next();
                if (c == '/') {
                    skipSingleLineComment();
                    continue;
                } else if (c == '*') {
                    skipMultiLineComment();
                    continue;
                }

                back(c);

                return '/';
            }

            return c;
        }
    }

    private void skipSingleLineComment() {
        while (true) {
            int c = next(false);
            if (c == '\r' || c == '\n' || c == -1)
                break;
        }
    }

    private void skipMultiLineComment() {
        while (true) {
            int c = next(false);
            if (c == -1)
                break;
            if (c == '*' && peek(false) == '/') {
                next(false);
                break;
            }
        }
    }

    private int next() {
        int c;
        if (index > -1)
            c = buffer[index--];
        else {
            try {
                c = reader.read();
            } catch (IOException e) {
                throw new IllegalArgumentException(e);
            }
        }

        if (c != '\n')
            pos++;
        else {
            pos = 1;
            line++;
        }

        return c;
    }

    private void back(int c) {
        if (index >= buffer.length - 1)
            throw new IllegalStateException();
        buffer[++index] = c;
    }
}
