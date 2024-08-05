package com.github.applejuiceyy.figuraextras.ipc.underlying;

import com.github.applejuiceyy.figuraextras.util.Util;
import net.minecraft.util.Tuple;
import org.apache.commons.lang3.SystemUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.function.BiConsumer;

public abstract class IPCFactory {
    static public IPCFactory getIPCFactory() throws UnsupportedPlatformException {
        if (SystemUtils.IS_OS_WINDOWS) {
            return new WindowsIPCFactory();
        }
        if (SystemUtils.IS_OS_UNIX) {
            return new UNIXIPCFactory();
        }
        throw new UnsupportedPlatformException();
    }

    public abstract boolean exists(String path);

    public abstract IPC createServer(String path) throws IOException;

    public abstract net.minecraft.util.Tuple<InputStream, OutputStream> connectAsClient(String path) throws IOException;

    public static abstract class IPC implements AutoCloseable {

        public net.minecraft.util.Tuple<InputStream, OutputStream> connect() throws IOException {
            return connect(false);
        }

        public abstract net.minecraft.util.Tuple<InputStream, OutputStream> connect(boolean awaitIfFull) throws IOException;

        public void continuousConnect(BiConsumer<InputStream, OutputStream> consumer) {
            Util.thread(() -> {
                try {
                    _continuousConnect(consumer);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        protected void _continuousConnect(BiConsumer<InputStream, OutputStream> consumer) throws IOException {
            while (true) {
                Tuple<InputStream, OutputStream> connect = connect(true);
                consumer.accept(connect.getA(), connect.getB());
            }
        }

        @Override
        public void close() throws IOException {

        }
    }

    public static class UnsupportedPlatformException extends IOException {
    }
}
