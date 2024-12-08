package com.github.applejuiceyy.figuraextras.util;

import java.io.IOException;
import java.io.InputStream;

public class NewLineNormaliserInputStream extends InputStream {

    private final InputStream up;
    int prev = -2;

    public NewLineNormaliserInputStream(InputStream up) {
        this.up = up;
    }

    @Override
    public int read() throws IOException {
        if (prev != -2) {
            int t = prev;
            prev = -2;
            return t;
        }
        int i = up.read();
        if (i == -1) {
            return -1;
        }
        if (i == '\r') {
            int ii = up.read();
            if (ii == '\n') {
                return ii;
            }
            prev = ii;
            return i;
        }
        return i;
    }

    @Override
    public void close() throws IOException {
        up.close();
    }
}
