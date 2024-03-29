package com.github.applejuiceyy.figuraextras.util;

public interface SafeCloseable extends AutoCloseable {
    @Override
    void close();
}
