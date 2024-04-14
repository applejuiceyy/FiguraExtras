package com.github.applejuiceyy.figuraextras.vscode.dsp;

import java.io.IOException;
import java.io.InputStream;

abstract class AbstractBufferedStream extends InputStream {
    protected byte[] b;
    protected int i = 0, j = 0;

    protected AbstractBufferedStream(int buflen) {
        this.b = new byte[buflen];
    }

    abstract protected int avail() throws IOException;

    @Override
    public int read() throws IOException {
        int a = avail();
        return a <= 0 ? -1 : 0xff & b[i++];
    }

    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public int read(byte[] b, int i0, int n) throws IOException {
        int a = avail();
        if (a <= 0)
            return -1;
        final int n_read = Math.min(a, n);
        System.arraycopy(this.b, i, b, i0, n_read);
        i += n_read;
        return n_read;
    }

    @Override
    public long skip(long n) throws IOException {
        final long k = Math.min(n, j - i);
        i += k;
        return k;
    }

    @Override
    public int available() throws IOException {
        return j - i;
    }
}