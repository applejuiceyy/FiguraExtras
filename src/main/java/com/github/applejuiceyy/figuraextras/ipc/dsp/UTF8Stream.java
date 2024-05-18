package com.github.applejuiceyy.figuraextras.ipc.dsp;

import org.luaj.vm2.LuaString;

import java.io.IOException;
import java.io.Reader;

class UTF8Stream extends AbstractBufferedStream {
    private final char[] c = new char[32];
    private final Reader r;

    UTF8Stream(Reader r) {
        super(96);
        this.r = r;
    }

    @Override
    protected int avail() throws IOException {
        if (i < j)
            return j - i;
        int n = r.read(c);
        if (n < 0)
            return -1;
        if (n == 0) {
            int u = r.read();
            if (u < 0)
                return -1;
            c[0] = (char) u;
            n = 1;
        }
        j = LuaString.encodeToUtf8(c, n, b, i = 0);
        return j;
    }

    @Override
    public void close() throws IOException {
        r.close();
    }
}